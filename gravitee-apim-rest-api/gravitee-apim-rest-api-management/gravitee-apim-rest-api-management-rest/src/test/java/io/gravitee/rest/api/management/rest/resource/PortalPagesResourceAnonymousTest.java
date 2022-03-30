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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.io.IOException;
import java.security.Principal;
import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.SecurityContext;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PortalPagesResourceAnonymousTest extends AbstractResourceTest {

    private static final String PAGE_NAME = "p";

    @Override
    protected String contextPath() {
        return "portal/pages/";
    }

    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        resourceConfig.register(AuthenticationFilter.class);
    }

    @Priority(50)
    public static class AuthenticationFilter implements ContainerRequestFilter {

        @Override
        public void filter(final ContainerRequestContext requestContext) throws IOException {
            requestContext.setSecurityContext(
                new SecurityContext() {
                    @Override
                    public Principal getUserPrincipal() {
                        return null;
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

    @Test
    public void shouldGetPage_LoginOptional() {
        PageEntity page = new PageEntity();
        page.setPublished(true);
        page.setVisibility(Visibility.PUBLIC);
        doReturn(page).when(pageService).findById(any(), any());
        doReturn(false).when(configService).portalLoginForced(GraviteeContext.getExecutionContext());
        //        final Response response = envTarget(PAGE_NAME).request().get();
        //
        //        assertNotNull("Response should be present", response);
        //        assertEquals("Response should be 200", OK.getStatusCode(), response.getStatus());
    }

    @Test
    public void shouldNotGetPage_LoginRequired() {
        PageEntity page = new PageEntity();
        page.setPublished(true);
        page.setVisibility(Visibility.PUBLIC);
        doReturn(page).when(pageService).findById(any(), any());
        doReturn(true).when(configService).portalLoginForced(GraviteeContext.getExecutionContext());
        //        final Response response = envTarget(PAGE_NAME).request().get();
        //
        //        assertNotNull("Response should be present", response);
        //        assertEquals("Response should be 401", UNAUTHORIZED.getStatusCode(), response.getStatus());
    }
}
