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
package io.gravitee.apim.core.integration.use_case;

import static org.assertj.core.api.Assertions.assertThat;

import fixtures.core.model.IntegrationFixture;
import inmemory.IntegrationCrudServiceInMemory;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.integration.use_case.UpdateAgentStatusUseCase.Input;
import io.gravitee.apim.core.integration.use_case.UpdateAgentStatusUseCase.Output;
import io.gravitee.common.utils.TimeProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UpdateAgentStatusUseCaseTest {

    private static final Instant INSTANT_NOW = Instant.parse("2023-10-22T10:15:30Z");
    private static final String INTEGRATION_ID = "my-integration-id";
    private static final String PROVIDER = "aws-api-gateway";
    private static final Integration INTEGRATION = IntegrationFixture
        .anIntegration()
        .toBuilder()
        .id(INTEGRATION_ID)
        .provider(PROVIDER)
        .build();

    IntegrationCrudServiceInMemory integrationCrudService = new IntegrationCrudServiceInMemory();

    UpdateAgentStatusUseCase useCase;

    @BeforeAll
    static void beforeAll() {
        TimeProvider.overrideClock(Clock.fixed(INSTANT_NOW, ZoneId.systemDefault()));
    }

    @AfterAll
    static void afterAll() {
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @BeforeEach
    void setUp() {
        useCase = new UpdateAgentStatusUseCase(integrationCrudService);
    }

    @Nested
    class AgentConnected {

        @Test
        void should_fail_when_integration_does_not_exist() {
            // When
            var result = useCase.execute(new Input("unknown", PROVIDER, Integration.AgentStatus.CONNECTED));

            // Then
            assertThat(result).extracting(Output::success, Output::message).containsExactly(false, "Integration [id=unknown] not found");
        }

        @Test
        void should_fail_when_integration_does_not_match_with_agent_connected() {
            // Given
            givenExistingIntegration(INTEGRATION);

            // When
            var result = useCase.execute(new Input(INTEGRATION_ID, "other", Integration.AgentStatus.CONNECTED));

            // Then
            assertThat(result)
                .extracting(Output::success, Output::message)
                .containsExactly(false, "Integration [id=my-integration-id] does not match. Expected provider [provider=aws-api-gateway]");
        }

        @Test
        void should_update_agent_status() {
            // Given
            givenExistingIntegration(INTEGRATION);

            var result = useCase.execute(new Input(INTEGRATION_ID, PROVIDER, Integration.AgentStatus.CONNECTED));

            SoftAssertions.assertSoftly(soft -> {
                soft
                    .assertThat(integrationCrudService.storage())
                    .contains(
                        INTEGRATION
                            .toBuilder()
                            .agentStatus(Integration.AgentStatus.CONNECTED)
                            .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                            .build()
                    );
                soft.assertThat(result).extracting(Output::success).isEqualTo(true);
            });
        }
    }

    @Nested
    class AgentDisconnected {

        @Test
        void should_fail_when_integration_does_not_exist() {
            // When
            var result = useCase.execute(new Input("unknown", PROVIDER, Integration.AgentStatus.DISCONNECTED));

            // Then
            assertThat(result).extracting(Output::success, Output::message).containsExactly(false, "Integration [id=unknown] not found");
        }

        @Test
        void should_not_check_provider_input() {
            // Given
            givenExistingIntegration(INTEGRATION.toBuilder().agentStatus(Integration.AgentStatus.CONNECTED).build());

            var result = useCase.execute(new Input(INTEGRATION_ID, "unknown", Integration.AgentStatus.DISCONNECTED));

            assertThat(result).extracting(Output::success).isEqualTo(true);
        }

        @Test
        void should_update_agent_status() {
            // Given
            givenExistingIntegration(INTEGRATION.toBuilder().agentStatus(Integration.AgentStatus.CONNECTED).build());

            var result = useCase.execute(new Input(INTEGRATION_ID, PROVIDER, Integration.AgentStatus.DISCONNECTED));

            SoftAssertions.assertSoftly(soft -> {
                soft
                    .assertThat(integrationCrudService.storage())
                    .contains(
                        INTEGRATION
                            .toBuilder()
                            .agentStatus(Integration.AgentStatus.DISCONNECTED)
                            .updatedAt(INSTANT_NOW.atZone(ZoneId.systemDefault()))
                            .build()
                    );
                soft.assertThat(result).extracting(Output::success).isEqualTo(true);
            });
        }
    }

    private void givenExistingIntegration(Integration... integration) {
        integrationCrudService.initWith(Arrays.asList(integration));
    }
}
