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
package io.gravitee.rest.api.portal.rest.resource;

import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.portal.rest.model.Page;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.util.*;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 */
public class ApiPageResourceNotAuthenticatedTest extends AbstractResourceTest {

    private static final String API = "my-api";
    private static final String PAGE = "my-page";
    private static final String ANOTHER_PAGE = "another-page";
    private ApiEntity mockApi;
    private PageEntity mockPage;
    private PageEntity mockAnotherPage;

    @Override
    protected String contextPath() {
        return "apis/";
    }

    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        resourceConfig.register(NotAuthenticatedAuthenticationFilter.class);
    }

    @BeforeEach
    public void init() {
        resetAllMocks();

        mockApi = new ApiEntity();
        mockApi.setId(API);
        doReturn(mockApi).when(apiService).findById(GraviteeContext.getExecutionContext(), API);
        when(accessControlService.canAccessApiFromPortal(GraviteeContext.getExecutionContext(), API)).thenReturn(true);

        mockPage = new PageEntity();
        mockPage.setPublished(true);
        mockPage.setVisibility(Visibility.PUBLIC);
        doReturn(mockPage).when(pageService).findById(PAGE, null);

        mockAnotherPage = new PageEntity();
        mockAnotherPage.setPublished(true);
        mockAnotherPage.setVisibility(Visibility.PUBLIC);
        mockAnotherPage.setReferenceType("API");
        mockAnotherPage.setReferenceId(API);
        Map<String, String> metadataMap = new HashMap<>();
        metadataMap.put(ANOTHER_PAGE, ANOTHER_PAGE);
        mockAnotherPage.setMetadata(metadataMap);
        doReturn(mockAnotherPage).when(pageService).findById(ANOTHER_PAGE, null);

        doReturn(new Page()).when(pageMapper).convert(any(), any(), any());
    }

    @Test
    public void shouldHaveMetadataCleared() {
        when(apiSearchService.findGenericById(GraviteeContext.getExecutionContext(), API)).thenReturn(mock(GenericApiEntity.class));
        when(accessControlService.canAccessApiFromPortal(eq(GraviteeContext.getExecutionContext()), any(GenericApiEntity.class)))
            .thenReturn(true);
        when(
            accessControlService.canAccessApiPageFromPortal(
                eq(GraviteeContext.getExecutionContext()),
                any(GenericApiEntity.class),
                any(PageEntity.class)
            )
        )
            .thenReturn(true);

        Response anotherResponse = target(API).path("pages").path(ANOTHER_PAGE).request().get();
        assertEquals(OK_200, anotherResponse.getStatus());

        assertTrue(mockAnotherPage.getMetadata().isEmpty());
    }
}
