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
package io.gravitee.rest.api.portal.rest.security;

import io.gravitee.rest.api.service.ConfigService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.UnauthorizedAccessException;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * Rejects anonymous requests on resources annotated with {@link RequirePortalAuth} when the portal forces login.
 */
@Provider
@RequirePortalAuth
@Priority(200)
public class RequirePortalAuthFilter implements ContainerRequestFilter {

    @Inject
    private ConfigService configService;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (
            requestContext.getSecurityContext().getUserPrincipal() == null &&
            configService.portalLoginForced(GraviteeContext.getExecutionContext())
        ) {
            throw new UnauthorizedAccessException();
        }
    }
}
