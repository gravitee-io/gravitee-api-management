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

import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.INTERNAL_SERVER_ERROR_500;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class PortalPagesResourceTest extends AbstractResourceTest {

    @Autowired
    private PortalPageQueryServiceInMemory portalPageQueryServiceInMemory;

    @Autowired
    private PortalPageContextCrudServiceInMemory portalPageContextCrudServiceInMemory;

    protected String contextPath() {
        return "portal-pages";
    }

    @BeforeEach
    void setUp() {
        resetAllMocks();
        portalPageQueryServiceInMemory.reset();
        portalPageContextCrudServiceInMemory.reset();
    }

    @Test
    void should_return_400_when_type_is_missing() {
        Response response = target().queryParam("type", "UNKNOWN").request().get();
        assertEquals(BAD_REQUEST_400, response.getStatus());
    }

    @Test
    void should_return_400_when_type_is_invalid() {
        Response response = target().queryParam("type", "UNKNOWN").request().get();
        assertEquals(BAD_REQUEST_400, response.getStatus());
    }

    @Test
    void should_return_500_when_no_homepage_configured() {
        Response response = target().queryParam("type", "HOMEPAGE").request().get();
        assertEquals(INTERNAL_SERVER_ERROR_500, response.getStatus());
    }

    @Test
    void should_return_200_and_only_published_pages() {
        // Given a context exists
        var envId = "DEFAULT";
        var pageId1 = PageId.random();
        var pageId2 = PageId.random();
        var context = PortalPageContext.builder()
            .pageId(pageId1.toString())
            .contextType(PortalPageContextType.HOMEPAGE)
            .environmentId(envId)
            .published(true)
            .build();
        portalPageContextCrudServiceInMemory.initWith(List.of(context));

        // And we have both published and unpublished pages in storage
        PortalPage page1 = new PortalPage(pageId1, new GraviteeMarkdown("content1"));
        PortalPage page2 = new PortalPage(pageId2, new GraviteeMarkdown("content2"));
        var pages = List.of(
            new PortalPageWithViewDetails(page1, new PortalPageView(PortalViewContext.HOMEPAGE, true)),
            new PortalPageWithViewDetails(page2, new PortalPageView(PortalViewContext.HOMEPAGE, false))
        );
        portalPageQueryServiceInMemory.initWith(pages);

        // When
        Response response = target().queryParam("type", "HOMEPAGE").request().get();

        // Then
        assertEquals(OK_200, response.getStatus());
        List<?> returned = response.readEntity(new GenericType<>() {});
        assertThat(returned).hasSize(1);
    }
}
