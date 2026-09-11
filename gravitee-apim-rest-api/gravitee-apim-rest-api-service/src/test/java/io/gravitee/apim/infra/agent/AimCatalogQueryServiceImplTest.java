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
package io.gravitee.apim.infra.agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.agent.model.PortalAgentCard;
import io.gravitee.apim.core.agent.service_provider.AimCatalogQueryService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AimCatalogQueryServiceImplTest {

    @Test
    void should_return_empty_when_aim_catalog_is_not_registered() {
        var applicationContext = mock(ApplicationContext.class);
        var queryService = new AimCatalogQueryServiceImpl(applicationContext);
        when(applicationContext.getBeansOfType(AimCatalogQueryService.class)).thenReturn(Map.of("self", queryService));

        assertThat(queryService.findById("env", "agent-id")).isEmpty();
    }

    @Test
    void should_delegate_to_the_aim_catalog_when_registered() {
        var applicationContext = mock(ApplicationContext.class);
        var queryService = new AimCatalogQueryServiceImpl(applicationContext);
        var aimCatalog = mock(AimCatalogQueryService.class);
        var agent = agentCard();
        when(applicationContext.getBeansOfType(AimCatalogQueryService.class)).thenReturn(
            Map.of("self", queryService, "aim", aimCatalog)
        );
        when(aimCatalog.findById("env", "agent-id")).thenReturn(Optional.of(agent));

        assertThat(queryService.findById("env", "agent-id")).contains(agent);
    }

    private static PortalAgentCard agentCard() {
        return new PortalAgentCard(
            "agent-id",
            PortalAgentCard.KIND_AGENT,
            "agent.weather",
            "weather",
            "source-id",
            "manual",
            "env",
            "org",
            Instant.parse("2026-04-20T10:00:00Z"),
            Instant.parse("2026-04-22T11:00:00Z"),
            Map.of(),
            new PortalAgentCard.Definition("Weather", null, null, null, "1.0.0", null, null, List.of(), List.of(), List.of())
        );
    }
}
