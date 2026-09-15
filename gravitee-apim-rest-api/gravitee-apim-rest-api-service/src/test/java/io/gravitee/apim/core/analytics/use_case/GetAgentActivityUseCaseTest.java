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
package io.gravitee.apim.core.analytics.use_case;

import static org.assertj.core.api.Assertions.assertThat;

import fakes.FakeAnalyticsQueryService;
import io.gravitee.apim.core.analytics.model.AgentActivityResult;
import io.gravitee.apim.core.analytics.model.AgentActivityRun;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GetAgentActivityUseCaseTest {

    private final FakeAnalyticsQueryService analyticsQueryService = new FakeAnalyticsQueryService();
    private GetAgentActivityUseCase cut;

    @BeforeEach
    void setUp() {
        cut = new GetAgentActivityUseCase(analyticsQueryService);
    }

    @AfterEach
    void tearDown() {
        GraviteeContext.cleanContext();
        analyticsQueryService.agentActivityResult = null;
    }

    @Test
    void should_return_empty_feed_when_query_service_has_no_runs() {
        var result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new GetAgentActivityUseCase.Input("a2a-1", List.of("app-1"), "actor-1", 0L, 0L, 0, 25)
        );

        assertThat(result.runs()).isEmpty();
        assertThat(result.total()).isZero();
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(25);
    }

    @Test
    void should_return_runs_from_query_service() {
        analyticsQueryService.agentActivityResult = new AgentActivityResult(
            List.of(AgentActivityRun.builder().conversationId("conv-1").outcome("done").build()),
            1,
            0,
            25
        );

        var result = cut.execute(
            GraviteeContext.getExecutionContext(),
            new GetAgentActivityUseCase.Input("a2a-1", List.of("app-1"), "actor-1", 1L, 2L, 0, 25)
        );

        assertThat(result.runs()).hasSize(1);
        assertThat(result.runs().getFirst().getConversationId()).isEqualTo("conv-1");
        assertThat(result.total()).isEqualTo(1);
    }
}
