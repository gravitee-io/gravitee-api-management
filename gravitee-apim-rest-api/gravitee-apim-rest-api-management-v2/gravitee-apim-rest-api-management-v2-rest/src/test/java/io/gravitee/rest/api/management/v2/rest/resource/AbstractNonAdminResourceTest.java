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
package io.gravitee.rest.api.management.v2.rest.resource;

import io.gravitee.rest.api.management.v2.rest.UserDetails;
import io.gravitee.rest.api.management.v2.rest.resource.api.ApiResourceTest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.List;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Jersey harness for resource tests that must run as a plain user: the default {@code AuthenticationFilter} answers
 * {@code true} to every {@code isUserInRole} check, which makes the caller an org admin and short-circuits the
 * permission and membership gates such a test exists to exercise.
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public abstract class AbstractNonAdminResourceTest extends ApiResourceTest {

    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        resourceConfig.register((ContainerRequestFilter) requestContext -> requestContext.setSecurityContext(nonAdminSecurityContext()), 5);
        resourceConfig.register(GraviteeContextRequestFilter.class);
        var mockResponse = Mockito.mock(HttpServletResponse.class);
        resourceConfig.register(
            new AbstractBinder() {
                @Override
                protected void configure() {
                    bind(mockResponse).to(HttpServletResponse.class);
                }
            }
        );
    }

    private static SecurityContext nonAdminSecurityContext() {
        return new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                var userDetails = new UserDetails(USER_NAME, "", List.of());
                userDetails.setOrganizationId(ORGANIZATION);
                var principal = new UsernamePasswordAuthenticationToken(userDetails, new Object());
                SecurityContextHolder.getContext().setAuthentication(principal);
                return principal;
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
                return "BASIC";
            }
        };
    }
}
