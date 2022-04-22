/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.portal.rest.filter;

import io.gravitee.rest.api.portal.rest.security.Permission;
import io.gravitee.rest.api.portal.rest.security.Permissions;
import io.gravitee.rest.api.portal.rest.security.RequirePortalAuth;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.ForbiddenAccessException;
import io.gravitee.rest.api.service.exceptions.UnauthorizedAccessException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Priority;
import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Provider
@Priority(200)
public class PermissionsFilter implements ContainerRequestFilter {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Context
    protected ResourceInfo resourceInfo;

    @Inject
    private SecurityContext securityContext;

    @Inject
    private PermissionService permissionService;

    @Inject
    private ConfigService configService;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        final ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        checkAuthenticationConditions(executionContext);
        findRequiredPermissions().ifPresent(requiredPermissions -> filter(requiredPermissions, requestContext, executionContext));
    }

    protected void filter(Permissions permissions, ContainerRequestContext requestContext, final ExecutionContext executionContext) {
        Stream
            .of(permissions.value())
            .filter(permission -> hasPermission(permission, requestContext, executionContext))
            .findAny()
            .orElseThrow(ForbiddenAccessException::new);
    }

    private void checkAuthenticationConditions(final ExecutionContext executionContext) {
        if (requiresPortalAuth(executionContext) && securityContext.getUserPrincipal() == null) {
            throw new UnauthorizedAccessException();
        }
    }

    private boolean hasPermission(Permission permission, ContainerRequestContext requestContext, final ExecutionContext executionContext) {
        switch (permission.value().getScope()) {
            case ORGANIZATION:
                return hasPermission(executionContext, permission, executionContext.getOrganizationId());
            case ENVIRONMENT:
                return (
                    executionContext.hasEnvironmentId() && hasPermission(executionContext, permission, executionContext.getEnvironmentId())
                );
            case APPLICATION:
                return hasPermission(executionContext, permission, getApplicationId(requestContext));
            case API:
                return hasPermission(executionContext, permission, getApiId(requestContext));
            default:
                return false;
        }
    }

    private boolean hasPermission(final ExecutionContext executionContext, Permission permission, String referenceId) {
        return permissionService.hasPermission(executionContext, permission.value(), referenceId, permission.acls());
    }

    private boolean requiresPortalAuth(final ExecutionContext executionContext) {
        return findRequirePortalAuthAnnotation().isPresent() && configService.portalLoginForced(executionContext);
    }

    private String getApiId(ContainerRequestContext requestContext) {
        return getId("apiId", requestContext);
    }

    private String getApplicationId(ContainerRequestContext requestContext) {
        return getId("applicationId", requestContext);
    }

    private String getId(String key, ContainerRequestContext requestContext) {
        List<String> pathParams = requestContext.getUriInfo().getPathParameters().get(key);
        if (pathParams != null) {
            return pathParams.iterator().next();
        } else {
            List<String> queryParams = requestContext.getUriInfo().getQueryParameters().get(key);
            if (queryParams != null) {
                return queryParams.iterator().next();
            }
        }
        return null;
    }

    private Optional<Permissions> findRequiredPermissions() {
        return Optional
            .ofNullable(resourceInfo.getResourceMethod().getDeclaredAnnotation(Permissions.class))
            .or(() -> Optional.ofNullable(resourceInfo.getResourceClass().getDeclaredAnnotation(Permissions.class)));
    }

    private Optional<RequirePortalAuth> findRequirePortalAuthAnnotation() {
        return Optional
            .ofNullable(resourceInfo.getResourceMethod().getDeclaredAnnotation(RequirePortalAuth.class))
            .or(() -> Optional.ofNullable(resourceInfo.getResourceClass().getDeclaredAnnotation(RequirePortalAuth.class)));
    }
}
