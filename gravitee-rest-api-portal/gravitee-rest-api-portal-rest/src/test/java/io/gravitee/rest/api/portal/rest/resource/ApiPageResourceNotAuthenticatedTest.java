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

import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.portal.rest.model.Page;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.io.IOException;
import java.security.Principal;
import java.util.*;

import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doReturn;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 */
public class ApiPageResourceNotAuthenticatedTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "apis/";
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

    private static final String API = "my-api";
    private static final String PAGE = "my-page";
    private static final String ANOTHER_PAGE = "another-page";

    private ApiEntity mockApi;
    private PageEntity mockPage;
    private PageEntity mockAnotherPage;

    
    @Before
    public void init() {
        resetAllMocks();
        
        mockApi = new ApiEntity();
        mockApi.setId(API);
        doReturn(mockApi).when(apiService).findById(API);
        Set<ApiEntity> mockApis = new HashSet<>(Arrays.asList(mockApi));
        doReturn(mockApis).when(apiService).findPublishedByUser(isNull(), argThat(q -> singletonList(API).equals(q.getIds())));
        
        mockPage = new PageEntity();
        mockPage.setPublished(true);
        mockPage.setExcludedGroups(new ArrayList<String>());
        doReturn(mockPage).when(pageService).findById(PAGE, null);
        
        mockAnotherPage = new PageEntity();
        mockAnotherPage.setPublished(true);
        mockAnotherPage.setExcludedGroups(new ArrayList<String>());
        Map<String, String> metadataMap = new HashMap<>();
        metadataMap.put(ANOTHER_PAGE, ANOTHER_PAGE);
        mockAnotherPage.setMetadata(metadataMap);
        doReturn(mockAnotherPage).when(pageService).findById(ANOTHER_PAGE, null);

        doReturn(new Page()).when(pageMapper).convert(any(), any(), any());
    }

    @Test
    public void shouldHaveMetadataCleared() {
        
        doReturn(true).when(groupService).isUserAuthorizedToAccessApiData(any(), any(), any());
        
        Response anotherResponse = target(API).path("pages").path(ANOTHER_PAGE).request().get();
        assertEquals(OK_200, anotherResponse.getStatus());

        assertTrue(mockAnotherPage.getMetadata().isEmpty());

    }
}
