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
package io.gravitee.rest.api.portal.rest.resource;

import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.gravitee.rest.api.model.ViewEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.portal.rest.model.View;

/**
 * @author Florent CHAMFROY (forent.chamfroy at graviteesource.com)
 */
public class ViewResourceNotAuthenticatedTest extends AbstractResourceTest {

    private static final String VIEW_ID = "my-view-id";

    @Override
    protected String contextPath() {
        return "views/";
    }
    
    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        resourceConfig.register(AuthenticationFilter.class);
    }
    
    @Priority(50)
    public static class AuthenticationFilter implements ContainerRequestFilter {
        @Override
        public void filter(final ContainerRequestContext requestContext) throws IOException {
            requestContext.setSecurityContext(new SecurityContext() {
                @Override
                public Principal getUserPrincipal() {
                    return null;
                }
                @Override
                public boolean isUserInRole(String string) {
                    return false;
                }
                @Override
                public boolean isSecure() { return false; }
                
                @Override
                public String getAuthenticationScheme() { return "BASIC"; }
            });
        }
    }

    @Before
    public void init() throws IOException, URISyntaxException {
        resetAllMocks();
        
        ViewEntity viewEntity = new ViewEntity();
        viewEntity.setId(VIEW_ID);
        viewEntity.setHidden(false);
        doReturn(viewEntity).when(viewService).findNotHiddenById(VIEW_ID);
        
        Set<ApiEntity> mockApis = new HashSet<>();
        doReturn(mockApis).when(apiService).findByVisibility(any());
        
        Function<ViewEntity, ViewEntity> identity = (v) -> v;
        doReturn(identity).when(viewEnhancer).enhance(any());
        
        Mockito.when(viewMapper.convert(any(), any())).thenCallRealMethod();

    }

    @Test
    public void shouldGetView() {
        final Response response = target(VIEW_ID).request().get();
        assertEquals(OK_200, response.getStatus());

        Mockito.verify(viewService).findNotHiddenById(VIEW_ID);
        Mockito.verify(apiService).findByVisibility(any());
        Mockito.verify(viewEnhancer).enhance(any());
        Mockito.verify(viewMapper).convert(any(), any());

        final View responseView = response.readEntity(View.class);
        assertNotNull(responseView);
        
    }
}
