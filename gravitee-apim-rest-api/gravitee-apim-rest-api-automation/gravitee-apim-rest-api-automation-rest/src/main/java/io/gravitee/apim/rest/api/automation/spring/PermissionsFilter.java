/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.apim.rest.api.automation.spring;

import io.gravitee.apim.core.utils.CollectionUtils;
import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.IdBuilder;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.gravitee.rest.api.service.exceptions.UnauthorizedAccessException;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@Provider
@Priority(200)
@Slf4j
public class PermissionsFilter implements ContainerRequestFilter {

    private static final String HRID_PARAM = "hrid";
    private static final String API_HRID = "apiHrid";
    private static final String LEGACY_PARAM = "legacy";

    @Context
    protected ResourceInfo resourceInfo;

    @Inject
    private SecurityContext securityContext;

    @Inject
    private PermissionService permissionService;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        findRequiredPermissions().ifPresent(requiredPermissions -> {
            mustBeAuthenticated();
            filter(requiredPermissions, requestContext, GraviteeContext.getExecutionContext());
        });
    }

    protected void filter(Permissions permissions, ContainerRequestContext requestContext, ExecutionContext executionContext) {
        Stream.of(permissions.value())
            .filter(permission -> hasPermission(permission, requestContext, executionContext))
            .findAny()
            .orElseThrow(ForbiddenAccessException::new);
    }

    private boolean hasPermission(Permission permission, ContainerRequestContext requestContext, ExecutionContext executionContext) {
        return switch (permission.value().getScope()) {
            case ENVIRONMENT -> hasPermission(executionContext, permission, executionContext.getEnvironmentId());
            case API -> hasPermission(executionContext, permission, getApiHrid(requestContext));
            case APPLICATION -> hasPermission(executionContext, permission, getPathParameter(requestContext, HRID_PARAM));
            default -> false;
        };
    }

    private boolean hasPermission(final ExecutionContext executionContext, Permission permission, String referenceId) {
        if (referenceId == null) {
            return false;
        }

        return permissionService.hasPermission(executionContext, permission.value(), referenceId, permission.acls());
    }

    private String getPathParameter(ContainerRequestContext requestContext, String key) {
        return getPathParameter(requestContext, key, isLegacy(requestContext));
    }

    private String getPathParameter(ContainerRequestContext requestContext, String key, boolean isLegacy) {
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        List<String> pathParams = requestContext.getUriInfo().getPathParameters().get(key);
        if (CollectionUtils.isEmpty(pathParams)) {
            return null;
        }

        return isLegacy ? pathParams.getFirst() : IdBuilder.builder(executionContext, pathParams.getFirst()).buildId();
    }

    private String getApiHrid(ContainerRequestContext requestContext) {
        boolean legacy = isLegacy(requestContext);
        String apiHrid = getPathParameter(requestContext, API_HRID, legacy);
        return apiHrid != null ? apiHrid : getPathParameter(requestContext, HRID_PARAM, legacy);
    }

    private static boolean isLegacy(ContainerRequestContext requestContext) {
        List<String> queryParams = requestContext.getUriInfo().getQueryParameters().get(LEGACY_PARAM);
        return !CollectionUtils.isEmpty(queryParams) && Boolean.parseBoolean(queryParams.getFirst());
    }

    private Optional<Permissions> findRequiredPermissions() {
        return Optional.ofNullable(resourceInfo.getResourceMethod().getDeclaredAnnotation(Permissions.class)).or(() ->
            Optional.ofNullable(resourceInfo.getResourceClass().getDeclaredAnnotation(Permissions.class))
        );
    }

    private void mustBeAuthenticated() {
        Principal principal = securityContext.getUserPrincipal();
        if (principal == null) {
            throw new UnauthorizedAccessException();
        }
    }
}
