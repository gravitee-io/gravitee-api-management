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
package io.gravitee.gamma.authz.rest.resource;

import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;
import java.security.Principal;

@Provider
@Priority(Priorities.AUTHENTICATION)
public final class TestGraviteeContextFilter implements ContainerRequestFilter, ContainerResponseFilter {

    static final String TEST_ORG = "test-org";
    static final String TEST_USER = "test-user";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        MultivaluedMap<String, String> pathParams = requestContext.getUriInfo().getPathParameters();
        String env = firstOrDefault(pathParams, "environmentId", "test-env");
        GraviteeContext.setCurrentOrganization(TEST_ORG);
        GraviteeContext.setCurrentEnvironment(env);
        requestContext.setSecurityContext(
            new SecurityContext() {
                @Override
                public Principal getUserPrincipal() {
                    return () -> TEST_USER;
                }

                @Override
                public boolean isUserInRole(String role) {
                    return false;
                }

                @Override
                public boolean isSecure() {
                    return true;
                }

                @Override
                public String getAuthenticationScheme() {
                    return "TEST";
                }
            }
        );
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        GraviteeContext.cleanContext();
    }

    private static String firstOrDefault(MultivaluedMap<String, String> map, String key, String defaultValue) {
        String value = map.getFirst(key);
        return value == null ? defaultValue : value;
    }
}
