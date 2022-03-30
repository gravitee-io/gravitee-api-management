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
package io.gravitee.rest.api.management.rest.resource;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.ApplicationEntity;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.io.IOException;
import java.security.Principal;
import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApplicationResourceNotAdminTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "applications/my-app/";
    }

    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        resourceConfig.register(ApiPagesResourceNotAdminTest.AuthenticationFilter.class);
    }

    @Test
    public void shouldNotRestoreApplication() {
        reset(applicationService);

        ApplicationEntity restored = new ApplicationEntity();
        restored.setId("my-beautiful-application");
        doReturn(restored).when(applicationService).restore(eq(GraviteeContext.getExecutionContext()), any());

        final Response response = envTarget("_restore").request().post(null);
        assertEquals(HttpStatusCode.FORBIDDEN_403, response.getStatus());
    }

    @Priority(50)
    public static class AuthenticationFilter implements ContainerRequestFilter {

        @Override
        public void filter(final ContainerRequestContext requestContext) throws IOException {
            requestContext.setSecurityContext(
                new SecurityContext() {
                    @Override
                    public Principal getUserPrincipal() {
                        return () -> USER_NAME;
                    }

                    @Override
                    public boolean isUserInRole(String string) {
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
                }
            );
        }
    }
}
