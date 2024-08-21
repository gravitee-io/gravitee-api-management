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
package io.gravitee.rest.api.rest.filter;

import io.gravitee.rest.api.rest.annotation.Permission;
import io.gravitee.rest.api.rest.annotation.Permissions;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
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

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Provider
@Priority(200)
public class PermissionsFilter implements ContainerRequestFilter {

    private static final String GROUP_ID_PARAM_V2 = "groupId";
    private static final String API_ID_PARAM_V2 = "apiId";
    private static final String APPLICATION_ID_PARAM_V2 = "applicationId";
    private static final String GROUP_ID_PARAM_V1 = "group";
    private static final String API_ID_PARAM_V1 = "api";
    private static final String APPLICATION_ID_PARAM_V1 = "application";
    private static final String INTEGRATION_ID_PARAM = "integrationId";

    @Context
    protected ResourceInfo resourceInfo;

    @Inject
    private SecurityContext securityContext;

    @Inject
    private PermissionService permissionService;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        findRequiredPermissions()
            .ifPresent(requiredPermissions -> {
                mustBeAuthenticated();
                filter(requiredPermissions, requestContext, GraviteeContext.getExecutionContext());
            });
    }

    protected void filter(Permissions permissions, ContainerRequestContext requestContext, ExecutionContext executionContext) {
        Stream
            .of(permissions.value())
            .filter(permission -> hasPermission(permission, requestContext, executionContext))
            .findAny()
            .orElseThrow(ForbiddenAccessException::new);
    }

    private boolean hasPermission(Permission permission, ContainerRequestContext requestContext, ExecutionContext executionContext) {
        return switch (permission.value().getScope()) {
            case ORGANIZATION -> hasPermission(executionContext, permission, executionContext.getOrganizationId());
            case ENVIRONMENT -> (
                executionContext.hasEnvironmentId() && hasPermission(executionContext, permission, executionContext.getEnvironmentId())
            );
            case APPLICATION -> hasPermission(executionContext, permission, getApplicationId(requestContext));
            case API -> hasPermission(executionContext, permission, getApiId(requestContext));
            case GROUP -> hasPermission(executionContext, permission, getGroupId(requestContext));
            case INTEGRATION -> hasPermission(executionContext, permission, getId(INTEGRATION_ID_PARAM, requestContext));
            case PLATFORM -> false;
        };
    }

    private boolean hasPermission(final ExecutionContext executionContext, Permission permission, String referenceId) {
        return permissionService.hasPermission(executionContext, permission.value(), referenceId, permission.acls());
    }

    private String getGroupId(ContainerRequestContext requestContext) {
        String groupId = getId(GROUP_ID_PARAM_V1, requestContext);
        return groupId == null ? getId(GROUP_ID_PARAM_V2, requestContext) : groupId;
    }

    private String getApiId(ContainerRequestContext requestContext) {
        String apiId = getId(API_ID_PARAM_V1, requestContext);
        return apiId == null ? getId(API_ID_PARAM_V2, requestContext) : apiId;
    }

    private String getApplicationId(ContainerRequestContext requestContext) {
        String applicationId = getId(APPLICATION_ID_PARAM_V1, requestContext);
        return applicationId == null ? getId(APPLICATION_ID_PARAM_V2, requestContext) : applicationId;
    }

    private String getId(String key, ContainerRequestContext requestContext) {
        List<String> pathParams = requestContext.getUriInfo().getPathParameters().get(key);
        if (pathParams != null) {
            return pathParams.iterator().next();
        }
        List<String> queryParams = requestContext.getUriInfo().getQueryParameters().get(key);
        if (queryParams != null) {
            return queryParams.iterator().next();
        }
        return null;
    }

    private Optional<Permissions> findRequiredPermissions() {
        return Optional
            .ofNullable(resourceInfo.getResourceMethod().getDeclaredAnnotation(Permissions.class))
            .or(() -> Optional.ofNullable(resourceInfo.getResourceClass().getDeclaredAnnotation(Permissions.class)));
    }

    private void mustBeAuthenticated() {
        Principal principal = securityContext.getUserPrincipal();
        if (principal == null) {
            throw new UnauthorizedAccessException();
        }
    }
}
