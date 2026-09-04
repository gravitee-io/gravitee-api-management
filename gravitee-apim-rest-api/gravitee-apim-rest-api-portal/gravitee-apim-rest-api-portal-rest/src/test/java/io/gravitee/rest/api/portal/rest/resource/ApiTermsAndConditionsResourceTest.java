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

import static org.assertj.core.api.Assertions.assertThat;

import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationAgent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.rest.api.portal.rest.model.PortalPageContent;
import io.gravitee.rest.api.portal.rest.model.PortalPageContentType;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ApiTermsAndConditionsResourceTest extends AbstractResourceTest {

    private static final String ENV_ID = "DEFAULT";
    private static final String API_ID = "my-agent-api-id";

    @Autowired
    private PortalNavigationItemsQueryServiceInMemory portalNavigationItemsQueryService;

    @Autowired
    private PortalPageContentQueryServiceInMemory portalPageContentQueryService;

    @Override
    protected String contextPath() {
        return "apis/";
    }

    @BeforeEach
    void init() {
        GraviteeContext.setCurrentEnvironment(ENV_ID);
    }

    @AfterEach
    void cleanUp() {
        GraviteeContext.cleanContext();
        portalNavigationItemsQueryService.reset();
        portalPageContentQueryService.reset();
    }

    @Test
    void should_return_200_with_agent_terms_and_conditions() {
        var contentId = PortalPageContentId.random();
        portalNavigationItemsQueryService.initWith(List.of(publishedAgent(API_ID, contentId)));
        portalPageContentQueryService.initWith(
            List.of(new GraviteeMarkdownPageContent(contentId, "DEFAULT", ENV_ID, GraviteeMarkdown.of("# Agent usage terms")))
        );

        Response response = target(API_ID + "/terms-and-conditions").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.OK_200);
        var result = response.readEntity(PortalPageContent.class);
        assertThat(result.getType()).isEqualTo(PortalPageContentType.GRAVITEE_MARKDOWN);
        assertThat(result.getContent()).isEqualTo("# Agent usage terms");
    }

    @Test
    void should_return_404_when_no_agent_terms_are_configured() {
        portalNavigationItemsQueryService.initWith(List.of(publishedAgent(API_ID, null)));

        Response response = target(API_ID + "/terms-and-conditions").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
    }

    @Test
    void should_return_404_when_terms_content_is_empty() {
        var contentId = PortalPageContentId.random();
        portalNavigationItemsQueryService.initWith(List.of(publishedAgent(API_ID, contentId)));
        portalPageContentQueryService.initWith(
            List.of(new GraviteeMarkdownPageContent(contentId, "DEFAULT", ENV_ID, GraviteeMarkdown.of("")))
        );

        Response response = target(API_ID + "/terms-and-conditions").request().get();

        assertThat(response.getStatus()).isEqualTo(HttpStatusCode.NOT_FOUND_404);
    }

    private PortalNavigationAgent publishedAgent(String agentId, PortalPageContentId termsContentId) {
        return PortalNavigationAgent.builder()
            .id(PortalNavigationItemId.random())
            .organizationId("DEFAULT")
            .environmentId(ENV_ID)
            .title("Nav for " + agentId)
            .segment(PortalNavigationItem.slugify("Nav for " + agentId).value())
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .agentId(agentId)
            .termsAndConditionsPageContentId(termsContentId)
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .parentId(PortalNavigationItemId.random())
            .build();
    }
}
