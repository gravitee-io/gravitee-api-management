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
package io.gravitee.apim.core.agent.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.PortalNavigationItemFixtures;
import inmemory.AimCatalogQueryServiceInMemory;
import inmemory.ApiQueryServiceInMemory;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import inmemory.SubscriptionQueryServiceInMemory;
import io.gravitee.apim.core.agent.exception.PortalAgentNotFoundException;
import io.gravitee.apim.core.agent.model.PortalAgentCard;
import io.gravitee.apim.core.membership.domain_service.ApiPortalMembershipDomainService;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.portal_page.domain_service.PortalAgentAccessDomainService;
import io.gravitee.apim.core.portal_page.domain_service.PortalNavigationApiVisibilityDomainService;
import io.gravitee.apim.core.portal_page.model.PortalNavigationAgent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemViewerContext;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetPortalAgentUseCaseTest {

    private static final String ENVIRONMENT_ID = PortalNavigationItemFixtures.ENV_ID;
    private static final String AGENT_ID = "catalog-agent-id";
    private static final String API_ID = "a2a-proxy-api-id";
    private static final String USER_ID = "user-id";
    private static final Instant CREATED_AT = Instant.parse("2026-04-20T10:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-04-22T11:00:00Z");

    private AimCatalogQueryServiceInMemory aimCatalogQueryService;
    private MembershipQueryServiceInMemory membershipQueryService;
    private PortalNavigationItemsQueryServiceInMemory navigationItemsQueryService;
    private GetPortalAgentUseCase useCase;

    @BeforeEach
    void setUp() {
        aimCatalogQueryService = new AimCatalogQueryServiceInMemory();
        membershipQueryService = new MembershipQueryServiceInMemory();
        navigationItemsQueryService = new PortalNavigationItemsQueryServiceInMemory();

        var apiMembershipDomainService = new ApiPortalMembershipDomainService(
            membershipQueryService,
            new SubscriptionQueryServiceInMemory(),
            new ApiQueryServiceInMemory()
        );
        var apiVisibilityDomainService = new PortalNavigationApiVisibilityDomainService(
            navigationItemsQueryService,
            apiMembershipDomainService
        );
        useCase = new GetPortalAgentUseCase(
            new PortalAgentAccessDomainService(navigationItemsQueryService, apiVisibilityDomainService),
            aimCatalogQueryService
        );
    }

    @Test
    void should_return_public_agent_card_when_exposed_in_navigation() {
        givenPublicAgent();
        aimCatalogQueryService.initWith(List.of(agentCard()));

        var result = useCase.execute(input(null)).agent();

        assertThat(result.id()).isEqualTo(AGENT_ID);
        assertThat(result.kind()).isEqualTo(PortalAgentCard.KIND_AGENT);
        assertThat(result.definition().name()).isEqualTo("Weather Agent");
        assertThat(result.definition().skills()).extracting(PortalAgentCard.Skill::name).containsExactly("Forecast");
        assertThat(result.metadata()).containsEntry("protocol", "A2A");
    }

    @Test
    void should_return_private_agent_to_direct_member() {
        var navigationItem = publicAgentNavigationItem();
        navigationItem.setVisibility(PortalVisibility.PRIVATE);
        navigationItemsQueryService.initWith(List.of(navigationItem));
        membershipQueryService.initWith(List.of(apiMembership(USER_ID)));
        aimCatalogQueryService.initWith(List.of(agentCard()));

        var result = useCase.execute(input(USER_ID));

        assertThat(result.agent().id()).isEqualTo(AGENT_ID);
    }

    @Test
    void should_return_not_found_for_private_agent_without_access() {
        var navigationItem = publicAgentNavigationItem();
        navigationItem.setVisibility(PortalVisibility.PRIVATE);
        navigationItemsQueryService.initWith(List.of(navigationItem));
        aimCatalogQueryService.initWith(List.of(agentCard()));

        assertThatThrownBy(() -> useCase.execute(input(USER_ID))).isInstanceOf(PortalAgentNotFoundException.class);
    }

    @Test
    void should_return_not_found_when_agent_is_not_exposed_in_navigation() {
        aimCatalogQueryService.initWith(List.of(agentCard()));

        assertThatThrownBy(() -> useCase.execute(input(null))).isInstanceOf(PortalAgentNotFoundException.class);
    }

    @Test
    void should_return_not_found_when_navigation_item_is_unpublished() {
        var navigationItem = publicAgentNavigationItem();
        navigationItem.setPublished(false);
        navigationItemsQueryService.initWith(List.of(navigationItem));
        aimCatalogQueryService.initWith(List.of(agentCard()));

        assertThatThrownBy(() -> useCase.execute(input(null))).isInstanceOf(PortalAgentNotFoundException.class);
    }

    @Test
    void should_return_not_found_when_navigation_item_has_no_catalog_agent_id() {
        var navigationItem = publicAgentNavigationItem();
        navigationItem.setAgentId(null);
        navigationItemsQueryService.initWith(List.of(navigationItem));
        aimCatalogQueryService.initWith(List.of(agentCard()));

        assertThatThrownBy(() -> useCase.execute(input(null))).isInstanceOf(PortalAgentNotFoundException.class);
    }

    @Test
    void should_return_not_found_when_catalog_agent_is_missing() {
        givenPublicAgent();

        assertThatThrownBy(() -> useCase.execute(input(null))).isInstanceOf(PortalAgentNotFoundException.class);
    }

    @Test
    void should_return_not_found_when_catalog_agent_belongs_to_another_environment() {
        givenPublicAgent();
        aimCatalogQueryService.initWith(List.of(agentCard("other-environment-id")));

        assertThatThrownBy(() -> useCase.execute(input(null))).isInstanceOf(PortalAgentNotFoundException.class);
    }

    private void givenPublicAgent() {
        navigationItemsQueryService.initWith(List.of(publicAgentNavigationItem()));
    }

    private static GetPortalAgentUseCase.Input input(String userId) {
        return new GetPortalAgentUseCase.Input(ENVIRONMENT_ID, AGENT_ID, PortalNavigationItemViewerContext.forPortal(userId));
    }

    private static PortalNavigationAgent publicAgentNavigationItem() {
        return PortalNavigationItemFixtures.anAgent("nav-agent-id", "Weather Agent", null, API_ID)
            .toBuilder()
            .agentId(AGENT_ID)
            .build();
    }

    private static Membership apiMembership(String userId) {
        return Membership.builder()
            .id("api-membership")
            .memberId(userId)
            .memberType(Membership.Type.USER)
            .referenceType(Membership.ReferenceType.API)
            .referenceId(API_ID)
            .build();
    }

    private static PortalAgentCard agentCard() {
        return agentCard(ENVIRONMENT_ID);
    }

    private static PortalAgentCard agentCard(String environmentId) {
        return new PortalAgentCard(
            AGENT_ID,
            PortalAgentCard.KIND_AGENT,
            "agent.weather-agent",
            "weather-agent",
            "source-id",
            "manual",
            environmentId,
            "org-id",
            CREATED_AT,
            UPDATED_AT,
            Map.of("protocol", "A2A"),
            new PortalAgentCard.Definition(
                "Weather Agent",
                "Forecasts the weather",
                "https://agents.example/a2a",
                new PortalAgentCard.Provider("Gravitee", "https://gravitee.io"),
                "1.0.0",
                "https://docs.example",
                new PortalAgentCard.Capabilities(true, false, true),
                List.of("text"),
                List.of("text"),
                List.of(new PortalAgentCard.Skill("skill-forecast", "Forecast", "Tell the weather", List.of("weather"), List.of(), List.of(), List.of()))
            )
        );
    }
}
