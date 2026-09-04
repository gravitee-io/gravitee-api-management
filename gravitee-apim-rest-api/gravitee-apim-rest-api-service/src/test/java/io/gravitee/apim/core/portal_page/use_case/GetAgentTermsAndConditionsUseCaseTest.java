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
package io.gravitee.apim.core.portal_page.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import inmemory.ApiQueryServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.PortalPageContentQueryServiceInMemory;
import inmemory.SubscriptionQueryServiceInMemory;
import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.gravitee_markdown.GraviteeMarkdown;
import io.gravitee.apim.core.membership.domain_service.ApiPortalMembershipDomainService;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationApiVisibilityDomainService;
import io.gravitee.apim.core.portal_page.exception.AgentTermsAndConditionsNotFoundException;
import io.gravitee.apim.core.portal_page.model.GraviteeMarkdownPageContent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationAgent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItem;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalPageContentId;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetAgentTermsAndConditionsUseCaseTest {

    private static final String ENV_ID = "env-id";
    private static final String ORG_ID = "org-id";
    private static final String API_ID = "agent-api-1";
    private static final String USER_ID = "user-1";

    private final PortalNavigationItemsQueryServiceInMemory navQueryService = new PortalNavigationItemsQueryServiceInMemory();
    private final PortalPageContentQueryServiceInMemory pageContentQueryService = new PortalPageContentQueryServiceInMemory();
    private final MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory();
    private final SubscriptionQueryServiceInMemory subscriptionQueryService = new SubscriptionQueryServiceInMemory();
    private final ApiQueryServiceInMemory apiQueryService = new ApiQueryServiceInMemory();
    private GetAgentTermsAndConditionsUseCase useCase;

    @BeforeEach
    void setUp() {
        navQueryService.reset();
        pageContentQueryService.reset();
        membershipQueryService.reset();
        subscriptionQueryService.reset();
        apiQueryService.reset();

        var visibility = new PortalNavigationApiVisibilityDomainService(
            navQueryService,
            new ApiPortalMembershipDomainService(membershipQueryService, subscriptionQueryService, apiQueryService)
        );
        useCase = new GetAgentTermsAndConditionsUseCase(visibility, navQueryService, pageContentQueryService);
    }

    @Test
    void should_return_terms_content_when_agent_is_public_and_content_is_present() {
        var contentId = PortalPageContentId.random();
        navQueryService.initWith(List.of(publishedAgent(API_ID, PortalVisibility.PUBLIC, contentId)));
        var content = new GraviteeMarkdownPageContent(contentId, ORG_ID, ENV_ID, GraviteeMarkdown.of("# Agent terms"));
        pageContentQueryService.initWith(List.of(content));

        var result = useCase.execute(new GetAgentTermsAndConditionsUseCase.Input(ENV_ID, API_ID, USER_ID));

        assertThat(result.content()).isEqualTo(content);
    }

    @Test
    void should_throw_api_not_found_when_agent_is_not_visible() {
        var contentId = PortalPageContentId.random();
        navQueryService.initWith(List.of(publishedAgent(API_ID, PortalVisibility.PRIVATE, contentId)));

        assertThatThrownBy(() -> useCase.execute(new GetAgentTermsAndConditionsUseCase.Input(ENV_ID, API_ID, USER_ID))).isInstanceOf(
            ApiNotFoundException.class
        );
    }

    @Test
    void should_throw_not_found_when_agent_has_no_terms_content_id() {
        navQueryService.initWith(List.of(publishedAgent(API_ID, PortalVisibility.PUBLIC, null)));

        assertThatThrownBy(() -> useCase.execute(new GetAgentTermsAndConditionsUseCase.Input(ENV_ID, API_ID, USER_ID))).isInstanceOf(
            AgentTermsAndConditionsNotFoundException.class
        );
    }

    @Test
    void should_throw_not_found_when_terms_content_is_empty() {
        var contentId = PortalPageContentId.random();
        navQueryService.initWith(List.of(publishedAgent(API_ID, PortalVisibility.PUBLIC, contentId)));
        pageContentQueryService.initWith(List.of(new GraviteeMarkdownPageContent(contentId, ORG_ID, ENV_ID, GraviteeMarkdown.of("   "))));

        assertThatThrownBy(() -> useCase.execute(new GetAgentTermsAndConditionsUseCase.Input(ENV_ID, API_ID, USER_ID))).isInstanceOf(
            AgentTermsAndConditionsNotFoundException.class
        );
    }

    @Test
    void should_throw_not_found_when_terms_content_is_missing() {
        var contentId = PortalPageContentId.random();
        navQueryService.initWith(List.of(publishedAgent(API_ID, PortalVisibility.PUBLIC, contentId)));

        assertThatThrownBy(() -> useCase.execute(new GetAgentTermsAndConditionsUseCase.Input(ENV_ID, API_ID, USER_ID))).isInstanceOf(
            AgentTermsAndConditionsNotFoundException.class
        );
    }

    @Test
    void should_throw_not_found_when_terms_disabled() {
        var contentId = PortalPageContentId.random();
        navQueryService.initWith(
            List.of(publishedAgent(API_ID, PortalVisibility.PUBLIC, contentId).toBuilder().termsAndConditionsEnabled(false).build())
        );
        pageContentQueryService.initWith(
            List.of(new GraviteeMarkdownPageContent(contentId, ORG_ID, ENV_ID, GraviteeMarkdown.of("# Agent terms")))
        );

        assertThatThrownBy(() -> useCase.execute(new GetAgentTermsAndConditionsUseCase.Input(ENV_ID, API_ID, USER_ID))).isInstanceOf(
            AgentTermsAndConditionsNotFoundException.class
        );
    }

    private PortalNavigationAgent publishedAgent(String agentId, PortalVisibility visibility, PortalPageContentId termsContentId) {
        return PortalNavigationAgent.builder()
            .id(PortalNavigationItemId.random())
            .organizationId(ORG_ID)
            .environmentId(ENV_ID)
            .title("Nav for " + agentId)
            .segment(PortalNavigationItem.slugify("Nav for " + agentId).value())
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .agentId(agentId)
            .termsAndConditionsPageContentId(termsContentId)
            .termsAndConditionsEnabled(true)
            .published(true)
            .visibility(visibility)
            .parentId(PortalNavigationItemId.random())
            .build();
    }
}
