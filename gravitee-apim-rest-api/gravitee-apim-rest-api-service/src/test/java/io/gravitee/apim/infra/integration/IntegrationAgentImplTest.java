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
package io.gravitee.apim.infra.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.in;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import fixtures.core.model.IntegrationFixture;
import io.gravitee.apim.core.integration.exception.IntegrationIngestionException;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.integration.model.IntegrationApi;
import io.gravitee.exchange.api.command.Command;
import io.gravitee.exchange.api.controller.ExchangeController;
import io.gravitee.integration.api.command.ingest.IngestCommand;
import io.gravitee.integration.api.command.ingest.IngestCommandPayload;
import io.gravitee.integration.api.command.ingest.IngestReply;
import io.gravitee.integration.api.command.ingest.IngestReplyPayload;
import io.gravitee.integration.api.model.Plan;
import io.gravitee.integration.api.model.PlanSecurityType;
import io.reactivex.rxjava3.core.Single;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IntegrationAgentImplTest {

    private static final String INTEGRATION_ID = "integration-id";
    private static final Integration INTEGRATION = IntegrationFixture.anIntegration().withId(INTEGRATION_ID);

    @Mock
    ExchangeController controller;

    IntegrationAgentImpl agent;

    @BeforeEach
    void setUp() {
        agent = new IntegrationAgentImpl(controller);
    }

    @Nested
    class FetchAllAssets {

        @BeforeEach
        void setUp() {
            when(controller.sendCommand(any(), any()))
                .thenReturn(Single.just(new IngestReply("command-id", new IngestReplyPayload(List.of(buildApi(1), buildApi(2))))));
        }

        @Test
        void should_send_command_to_fetch_all_assets() {
            agent.fetchAllApis(INTEGRATION).test().awaitDone(10, TimeUnit.SECONDS);

            var captor = ArgumentCaptor.forClass(Command.class);
            Mockito.verify(controller).sendCommand(captor.capture(), Mockito.eq(INTEGRATION_ID));

            assertThat(captor.getValue())
                .isInstanceOf(IngestCommand.class)
                .extracting(Command::getPayload)
                .isEqualTo(new IngestCommandPayload(List.of()));
        }

        @Test
        void should_return_all_assets() {
            var result = agent.fetchAllApis(INTEGRATION).test().awaitDone(10, TimeUnit.SECONDS).values();

            assertThat(result)
                .containsExactly(
                    new IntegrationApi(
                        INTEGRATION_ID,
                        "asset-uid-1",
                        "asset-1",
                        "asset-name-1",
                        "asset-description-1",
                        "asset-version-1",
                        Map.of("url", "https://example.com/1"),
                        List.of(new IntegrationApi.Plan("plan-id-1", "Gold 1", "Gold description 1", IntegrationApi.PlanType.API_KEY))
                    ),
                    new IntegrationApi(
                        INTEGRATION_ID,
                        "asset-uid-2",
                        "asset-2",
                        "asset-name-2",
                        "asset-description-2",
                        "asset-version-2",
                        Map.of("url", "https://example.com/2"),
                        List.of(new IntegrationApi.Plan("plan-id-2", "Gold 2", "Gold description 2", IntegrationApi.PlanType.API_KEY))
                    )
                );
        }

        @Test
        void should_return_empty_when_not_asset() {
            when(controller.sendCommand(any(), any()))
                .thenReturn(Single.just(new IngestReply("command-id", new IngestReplyPayload(List.of()))));

            agent.fetchAllApis(INTEGRATION).test().awaitDone(10, TimeUnit.SECONDS).assertNoValues();
        }

        @Test
        void should_throw_when_command_fails() {
            when(controller.sendCommand(any(), any())).thenReturn(Single.just(new IngestReply("command-id", "Fail to fetch assets")));

            agent
                .fetchAllApis(INTEGRATION)
                .doOnNext(asset -> System.out.println("OK: " + asset))
                .doOnError(th -> System.err.println("ERROR: " + th))
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .assertError(error -> {
                    assertThat(error).isInstanceOf(IntegrationIngestionException.class).hasMessage("Fail to fetch assets");
                    return true;
                });
        }
    }

    @NotNull
    private static io.gravitee.integration.api.model.Api buildApi(int index) {
        return io.gravitee.integration.api.model.Api
            .builder()
            .uniqueId("asset-uid-" + index)
            .id("asset-" + index)
            .name("asset-name-" + index)
            .version("asset-version-" + index)
            .description("asset-description-" + index)
            .connectionDetails(Map.of("url", "https://example.com/" + index))
            .plans(
                List.of(
                    Plan
                        .builder()
                        .id("plan-id-" + index)
                        .name("Gold " + index)
                        .description("Gold description " + index)
                        .planSecurityType(PlanSecurityType.API_KEY)
                        .build()
                )
            )
            .build();
    }
}
