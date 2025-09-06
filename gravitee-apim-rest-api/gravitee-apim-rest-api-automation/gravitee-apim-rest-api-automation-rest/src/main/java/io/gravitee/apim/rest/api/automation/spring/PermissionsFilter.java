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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
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
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
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
    private static final String LEGACY_PARAM = "legacy";

    @Context
    protected ResourceInfo resourceInfo;

    @Inject
    private SecurityContext securityContext;

    @Inject
    private PermissionService permissionService;

    private static final JsonFactory JSON_FACTORY = new JsonFactory();

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
            case ENVIRONMENT -> hasPermission(executionContext, permission, executionContext.getEnvironmentId());
            case API, APPLICATION -> hasPermission(executionContext, permission, getHrid(requestContext));
            default -> false;
        };
    }

    private boolean hasPermission(final ExecutionContext executionContext, Permission permission, String referenceId) {
        if (referenceId == null) {
            return false;
        }

        return permissionService.hasPermission(executionContext, permission.value(), referenceId, permission.acls());
    }

    private String getHrid(ContainerRequestContext requestContext) {
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        boolean legacy = false;
        MultivaluedMap<String, String> queryParameters = requestContext.getUriInfo().getQueryParameters();
        if (queryParameters != null && queryParameters.get(LEGACY_PARAM) != null) {
            legacy = Boolean.parseBoolean(queryParameters.get(LEGACY_PARAM).getFirst());
        }

        if ("GET".equals(requestContext.getMethod()) || "DELETE".equals(requestContext.getMethod())) {
            MultivaluedMap<String, String> pathParameters = requestContext.getUriInfo().getPathParameters();
            if (pathParameters != null && pathParameters.get(HRID_PARAM) != null) {
                return legacy
                    ? pathParameters.get(HRID_PARAM).getFirst()
                    : IdBuilder.builder(executionContext, pathParameters.get(HRID_PARAM).getFirst()).buildId();
            }
        } else if ("PUT".equals(requestContext.getMethod()) && requestContext.hasEntity()) {
            try {
                String hrid = null;
                InputStream inputStream = new BufferedInputStream(requestContext.getEntityStream(), 1024);
                inputStream.mark(Integer.MAX_VALUE);

                JsonParser parser = JSON_FACTORY.createParser(inputStream);
                while (!parser.isClosed()) {
                    JsonToken token = parser.nextToken();
                    if (token == JsonToken.FIELD_NAME && "hrid".equals(parser.currentName())) {
                        parser.nextToken();
                        hrid = parser.getValueAsString();
                        break;
                    }
                }

                inputStream.reset();
                requestContext.setEntityStream(inputStream);

                if (hrid != null) {
                    return legacy ? hrid : IdBuilder.builder(executionContext, hrid).buildId();
                }
            } catch (IOException e) {
                log.debug("Error reading request body", e);
                return null;
            }
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
