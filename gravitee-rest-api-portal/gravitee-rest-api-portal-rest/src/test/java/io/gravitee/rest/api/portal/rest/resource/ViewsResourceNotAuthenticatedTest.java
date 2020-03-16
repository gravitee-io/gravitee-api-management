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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.model.ViewEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.portal.rest.model.ViewsResponse;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 */
public class ViewsResourceNotAuthenticatedTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "views";
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
    public void init() {
        resetAllMocks();
        
        Set<ApiEntity> mockApis = new HashSet<>();
        doReturn(mockApis).when(apiService).findPublishedByUser(any());
        
        ViewEntity view1 = new ViewEntity();
        view1.setId("1");
        view1.setHidden(false);
        view1.setOrder(2);
        
        ViewEntity view2 = new ViewEntity();
        view2.setId("2");
        view2.setHidden(false);
        view2.setOrder(3);
        
        ViewEntity view3 = new ViewEntity();
        view3.setId("3");
        view3.setHidden(true);
        view3.setOrder(1);
        
        List<ViewEntity> mockViews = Arrays.asList(view1, view2, view3);
        doReturn(mockViews).when(viewService).findAll();

        doReturn(false).when(ratingService).isEnabled();

        Mockito.when(viewMapper.convert(any(), any())).thenCallRealMethod();

    }
    
    @Test
    public void shouldGetNotHiddenViews() {
        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        Mockito.verify(apiService).findPublishedByUser(any());
        ViewsResponse viewsResponse = response.readEntity(ViewsResponse.class);
        assertEquals(2, viewsResponse.getData().size());
        
    }
}
