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

import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import inmemory.PortalPageContextCrudServiceInMemory;
import inmemory.PortalPageQueryServiceInMemory;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdown;
import io.gravitee.apim.core.portal_page.model.PageId;
import io.gravitee.apim.core.portal_page.model.PortalPage;
import io.gravitee.apim.core.portal_page.model.PortalPageView;
import io.gravitee.apim.core.portal_page.model.PortalPageWithViewDetails;
import io.gravitee.apim.core.portal_page.model.PortalViewContext;
import io.gravitee.repository.management.model.PortalPageContext;
import io.gravitee.repository.management.model.PortalPageContextType;
import io.gravitee.rest.api.portal.rest.model.PortalHomepage;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Tests for GET /pages/_homepage
 */
public class PagesResourceHomepageTest extends AbstractResourceTest {

    @Autowired
    private io.gravitee.apim.core.portal_page.query_service.PortalPageQueryService portalPageQueryService;

    @Autowired
    private io.gravitee.apim.core.portal_page.crud_service.PortalPageContextCrudService portalPageContextCrudService;

    @Autowired
    private io.gravitee.repository.management.api.PortalPageRepository portalPageRepository;

    protected String contextPath() {
        return "pages";
    }

    @BeforeEach
    void setUp() {
        resetAllMocks();
        ((PortalPageQueryServiceInMemory) portalPageQueryService).reset();
        ((PortalPageContextCrudServiceInMemory) portalPageContextCrudService).reset();
    }

    @Test
    void shouldReturnHomepageWithEnrichedFields() throws Exception {
        // Given
        var envId = GraviteeContext.getCurrentEnvironment();
        var pageId = "11111111-1111-1111-1111-111111111111";
        var content = "Hello Gravitee";

        // Provide context linking page to HOMEPAGE
        ((PortalPageContextCrudServiceInMemory) portalPageContextCrudService).initWith(
                List.of(
                    io.gravitee.repository.management.model.PortalPageContext
                        .builder()
                        .contextType(PortalPageContextType.HOMEPAGE)
                        .environmentId(envId)
                        .pageId(pageId)
                        .build()
                )
            );
        var page = PortalPage.of(PageId.of(pageId), new GraviteeMarkdown(content));
        var view = new PortalPageView(PortalViewContext.HOMEPAGE, true);
        ((PortalPageQueryServiceInMemory) portalPageQueryService).initWith(List.of(new PortalPageWithViewDetails(page, view)));

        var repoPage = io.gravitee.repository.management.model.PortalPage
            .builder()
            .id(pageId)
            .environmentId(envId)
            .name("Home")
            .content(content)
            .createdAt(new Date(1690000000000L))
            .updatedAt(new Date(1695000000000L))
            .build();
        when(portalPageRepository.findById(pageId)).thenReturn(Optional.of(repoPage));

        // When
        Response response = target("/_homepage").request().get();

        // Then
        assertEquals(OK_200, response.getStatus());
        var body = response.readEntity(PortalHomepage.class);
        assertNotNull(body);
        assertEquals(pageId, body.getId());
        assertEquals("Home", body.getName());
        assertEquals(content, body.getContent());
        assertEquals("GRAVITEE_MARKDOWN", body.getType());
        assertNotNull(body.getCreatedAt());
        assertNotNull(body.getUpdatedAt());
    }

    @Test
    void shouldReturnHomepageWithMinimalFieldsWhenNoRepoData() throws Exception {
        // Given
        var envId = GraviteeContext.getCurrentEnvironment();
        var pageId = "22222222-2222-2222-2222-222222222222";
        var content = "Hello without repo";

        ((PortalPageContextCrudServiceInMemory) portalPageContextCrudService).initWith(
                List.of(PortalPageContext.builder().contextType(PortalPageContextType.HOMEPAGE).environmentId(envId).pageId(pageId).build())
            );
        var page = PortalPage.of(PageId.of(pageId), new GraviteeMarkdown(content));
        var view = new PortalPageView(PortalViewContext.HOMEPAGE, true);
        ((PortalPageQueryServiceInMemory) portalPageQueryService).initWith(List.of(new PortalPageWithViewDetails(page, view)));

        when(portalPageRepository.findById(pageId)).thenReturn(Optional.empty());

        // When
        Response response = target("/_homepage").request().get();

        // Then
        assertEquals(OK_200, response.getStatus());
        var body = response.readEntity(PortalHomepage.class);
        assertNotNull(body);
        assertEquals(pageId, body.getId());
        assertNull(body.getName());
        assertEquals(content, body.getContent());
        assertEquals("GRAVITEE_MARKDOWN", body.getType());
    }

    @Test
    void shouldReturn404WhenHomepageNotPublished() {
        var envId = GraviteeContext.getCurrentEnvironment();
        var pageId = "33333333-3333-3333-3333-333333333333";
        var content = "Hidden homepage";

        ((PortalPageContextCrudServiceInMemory) portalPageContextCrudService).initWith(
                List.of(PortalPageContext.builder().contextType(PortalPageContextType.HOMEPAGE).environmentId(envId).pageId(pageId).build())
            );
        var page = PortalPage.of(PageId.of(pageId), new GraviteeMarkdown(content));
        var view = new PortalPageView(PortalViewContext.HOMEPAGE, false);
        ((PortalPageQueryServiceInMemory) portalPageQueryService).initWith(List.of(new PortalPageWithViewDetails(page, view)));

        Response response = target("/_homepage").request().get();
        assertEquals(NOT_FOUND_404, response.getStatus());
    }

    @Test
    void shouldReturn404WhenNoHomepageConfigured() {
        Response response = target("/_homepage").request().get();
        assertEquals(NOT_FOUND_404, response.getStatus());
    }

    @Test
    void shouldReturnHomepageEvenWhenRepositoryThrows() throws Exception {
        var envId = GraviteeContext.getCurrentEnvironment();
        var pageId = "44444444-4444-4444-4444-444444444444";
        var content = "Repo exception path";

        ((PortalPageContextCrudServiceInMemory) portalPageContextCrudService).initWith(
                List.of(PortalPageContext.builder().contextType(PortalPageContextType.HOMEPAGE).environmentId(envId).pageId(pageId).build())
            );
        var page = PortalPage.of(PageId.of(pageId), new GraviteeMarkdown(content));
        var view = new PortalPageView(PortalViewContext.HOMEPAGE, true);
        ((PortalPageQueryServiceInMemory) portalPageQueryService).initWith(List.of(new PortalPageWithViewDetails(page, view)));

        when(portalPageRepository.findById(pageId)).thenThrow(new RuntimeException("DB down"));

        Response response = target("/_homepage").request().get();
        assertEquals(OK_200, response.getStatus());
        var body = response.readEntity(PortalHomepage.class);
        assertNotNull(body);
        assertEquals(pageId, body.getId());
        assertNull(body.getName());
        assertEquals(content, body.getContent());
        assertEquals("GRAVITEE_MARKDOWN", body.getType());
    }
}
