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
import static org.mockito.Mockito.doReturn;

import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.portal.rest.model.Page;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 */
public class PageResourceNotAuthenticatedTest extends AbstractResourceTest {

    @Override
    protected String contextPath() {
        return "pages/";
    }

    @Override
    protected void decorate(ResourceConfig resourceConfig) {
        resourceConfig.register(NotAuthenticatedAuthenticationFilter.class);
    }

    private static final String ANOTHER_PAGE = "another-page";

    private PageEntity mockAnotherPage;

    @BeforeEach
    public void init() {
        resetAllMocks();

        mockAnotherPage = new PageEntity();
        mockAnotherPage.setPublished(true);
        mockAnotherPage.setVisibility(Visibility.PUBLIC);
        Map<String, String> metadataMap = new HashMap<>();
        metadataMap.put(ANOTHER_PAGE, ANOTHER_PAGE);
        mockAnotherPage.setMetadata(metadataMap);
        mockAnotherPage.setReferenceType("ENVIRONMENT");
        mockAnotherPage.setReferenceId(GraviteeContext.getCurrentEnvironment());
        doReturn(mockAnotherPage).when(pageService).findById(ANOTHER_PAGE, null);

        doReturn(new Page()).when(pageMapper).convert(any(), any(), any());
    }

    @Test
    public void shouldHaveMetadataCleared() {
        doReturn(true).when(accessControlService).canAccessApiFromPortal(eq(GraviteeContext.getExecutionContext()), anyString());
        doReturn(true).when(accessControlService).canAccessPageFromPortal(eq(GraviteeContext.getExecutionContext()), any(PageEntity.class));

        Response anotherResponse = target(ANOTHER_PAGE).request().get();
        assertEquals(OK_200, anotherResponse.getStatus());

        assertTrue(mockAnotherPage.getMetadata().isEmpty());
    }
}
