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

import inmemory.AimCatalogQueryServiceInMemory;
import inmemory.PortalNavigationItemsQueryServiceInMemory;
import io.gravitee.apim.core.agent.model.PortalAgentCard;
import io.gravitee.apim.core.portal.model.PortalArea;
import io.gravitee.apim.core.portal_page.model.PortalNavigationAgent;
import io.gravitee.apim.core.portal_page.model.PortalNavigationItemId;
import io.gravitee.apim.core.portal_page.model.PortalVisibility;
import io.gravitee.rest.api.portal.rest.model.AgentCard;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AgentsResourceTest extends AbstractResourceTest {

    private static final String ENVIRONMENT_ID = "DEFAULT";
    private static final String AGENT_ID = "catalog-agent-id";
    private static final Instant CREATED_AT = Instant.parse("2026-04-20T10:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-04-22T11:00:00Z");

    @Autowired
    private AimCatalogQueryServiceInMemory aimCatalogQueryService;

    @Autowired
    private PortalNavigationItemsQueryServiceInMemory navigationItemsQueryService;

    @Override
    protected String contextPath() {
        return "agents/";
    }

    @BeforeEach
    void setUp() {
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
        aimCatalogQueryService.reset();
        navigationItemsQueryService.reset();
    }

    @Test
    void should_return_agent_card_when_exposed_in_navigation() {
        navigationItemsQueryService.initWith(List.of(publicAgentNavigationItem()));
        aimCatalogQueryService.initWith(List.of(agentCard()));

        Response response = target(AGENT_ID).request().get();

        assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
        var result = response.readEntity(AgentCard.class);
        assertThat(result.getId()).isEqualTo(AGENT_ID);
        assertThat(result.getKind()).isEqualTo(AgentCard.KindEnum.AGENT);
        assertThat(result.getEntityId()).isEqualTo("agent.weather-agent");
        assertThat(result.getDefinition().getName()).isEqualTo("Weather Agent");
        assertThat(result.getDefinition().getSkills()).extracting(skill -> skill.getName()).containsExactly("Forecast");
        assertThat(result.getMetadata()).containsEntry("protocol", "A2A");
        assertThat(result.getCreationDate()).isEqualTo(OffsetDateTime.ofInstant(CREATED_AT, ZoneOffset.UTC));
        assertThat(result.getUpdateDate()).isEqualTo(OffsetDateTime.ofInstant(UPDATED_AT, ZoneOffset.UTC));
    }

    @Test
    void should_return_not_found_when_agent_is_not_exposed_in_navigation() {
        aimCatalogQueryService.initWith(List.of(agentCard()));

        Response response = target(AGENT_ID).request().get();

        assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    }

    private static PortalNavigationAgent publicAgentNavigationItem() {
        return PortalNavigationAgent.builder()
            .id(PortalNavigationItemId.of("00000000-0000-0000-0000-000000000201"))
            .organizationId("organization-id")
            .environmentId(ENVIRONMENT_ID)
            .title("Weather Agent")
            .segment("weather-agent")
            .area(PortalArea.TOP_NAVBAR)
            .order(0)
            .apiId("a2a-proxy-api-id")
            .agentId(AGENT_ID)
            .published(true)
            .visibility(PortalVisibility.PUBLIC)
            .build();
    }

    private static PortalAgentCard agentCard() {
        return new PortalAgentCard(
            AGENT_ID,
            PortalAgentCard.KIND_AGENT,
            "agent.weather-agent",
            "weather-agent",
            "source-id",
            "manual",
            ENVIRONMENT_ID,
            "organization-id",
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
