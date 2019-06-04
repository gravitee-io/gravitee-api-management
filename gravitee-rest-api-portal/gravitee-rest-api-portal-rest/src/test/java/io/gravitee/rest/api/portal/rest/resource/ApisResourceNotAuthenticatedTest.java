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
import static org.mockito.Mockito.reset;

import java.io.IOException;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashSet;
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
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.portal.rest.model.Api;
import io.gravitee.rest.api.portal.rest.model.ApisResponse;

/**
 * @author Florent CHAMFROY (forent.chamfroy at graviteesource.com)
 */
public class ApisResourceNotAuthenticatedTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "apis";
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
        ApiEntity publishedApi = new ApiEntity();
        publishedApi.setLifecycleState(ApiLifecycleState.PUBLISHED);
        publishedApi.setName("A");
        publishedApi.setId("A");
        
        ApiEntity unpublishedApi = new ApiEntity();
        unpublishedApi.setLifecycleState(ApiLifecycleState.UNPUBLISHED);
        unpublishedApi.setName("B");
        unpublishedApi.setId("B");
        
        ApiEntity anotherPublishedApi = new ApiEntity();
        anotherPublishedApi.setLifecycleState(ApiLifecycleState.PUBLISHED);
        anotherPublishedApi.setName("C");
        anotherPublishedApi.setId("C");
        
        reset(apiService);
        reset(ratingService);
        reset(apiMapper);
        
        Set<ApiEntity> mockApis = new HashSet<>(Arrays.asList(publishedApi, anotherPublishedApi));
        doReturn(mockApis).when(apiService).findByUser(any(), any());
        doReturn(mockApis).when(apiService).search(any());
        
        doReturn(false).when(ratingService).isEnabled();
        
        doReturn(new Api().name("A").id("A")).when(apiMapper).convert(publishedApi);
        doReturn(new Api().name("B").id("B")).when(apiMapper).convert(unpublishedApi);
        doReturn(new Api().name("C").id("C")).when(apiMapper).convert(anotherPublishedApi);

    }
    
    @Test
    public void shouldGetpublishedApi() {
        final Response response = target().request().get();
        assertEquals(HttpStatusCode.OK_200, response.getStatus());
        
        Mockito.verify(apiService).search(any());
        
        ApisResponse apiResponse = response.readEntity(ApisResponse.class);
        assertEquals(2, apiResponse.getData().size());
        assertEquals("A", apiResponse.getData().get(0).getName());
        assertEquals("C", apiResponse.getData().get(1).getName());
    }
}
