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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import fixtures.core.model.IntegrationFixture;
import fixtures.definition.ApiDefinitionFixtures;
import fixtures.definition.PlanFixtures;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.integration.exception.IntegrationDiscoveryException;
import io.gravitee.apim.core.integration.exception.IntegrationIngestionException;
import io.gravitee.apim.core.integration.exception.IntegrationSubscriptionException;
import io.gravitee.apim.core.integration.model.IngestStarted;
import io.gravitee.apim.core.integration.model.Integration;
import io.gravitee.apim.core.integration.model.IntegrationApi;
import io.gravitee.apim.core.integration.model.IntegrationSubscription;
import io.gravitee.apim.core.integration.service_provider.IntegrationAgent;
import io.gravitee.apim.core.subscription.model.SubscriptionEntity;
import io.gravitee.definition.model.federation.SubscriptionParameter;
import io.gravitee.definition.model.v4.plan.PlanSecurity;
import io.gravitee.exchange.api.command.Command;
import io.gravitee.exchange.api.controller.ExchangeController;
import io.gravitee.exchange.api.controller.metrics.ChannelMetric;
import io.gravitee.integration.api.command.discover.DiscoverCommand;
import io.gravitee.integration.api.command.discover.DiscoverCommandPayload;
import io.gravitee.integration.api.command.discover.DiscoverReply;
import io.gravitee.integration.api.command.discover.DiscoverReplyPayload;
import io.gravitee.integration.api.command.ingest.StartIngestCommand;
import io.gravitee.integration.api.command.ingest.StartIngestCommandPayload;
import io.gravitee.integration.api.command.ingest.StartIngestReply;
import io.gravitee.integration.api.command.ingest.StartIngestReplyPayload;
import io.gravitee.integration.api.command.subscribe.SubscribeCommand;
import io.gravitee.integration.api.command.subscribe.SubscribeCommandPayload;
import io.gravitee.integration.api.command.subscribe.SubscribeReply;
import io.gravitee.integration.api.command.subscribe.SubscribeReplyPayload;
import io.gravitee.integration.api.command.unsubscribe.UnsubscribeCommand;
import io.gravitee.integration.api.command.unsubscribe.UnsubscribeCommandPayload;
import io.gravitee.integration.api.command.unsubscribe.UnsubscribeReply;
import io.gravitee.integration.api.command.unsubscribe.UnsubscribeReplyPayload;
import io.gravitee.integration.api.model.Page;
import io.gravitee.integration.api.model.PageType;
import io.gravitee.integration.api.model.Plan;
import io.gravitee.integration.api.model.PlanSecurityType;
import io.gravitee.integration.api.model.Subscription;
import io.gravitee.integration.api.model.SubscriptionResult;
import io.gravitee.integration.api.model.SubscriptionType;
import io.gravitee.rest.api.model.BaseApplicationEntity;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private static final String JOB_ID = "job-id";

    @Mock
    ExchangeController controller;

    IntegrationAgentImpl agent;

    @BeforeEach
    void setUp() {
        agent = new IntegrationAgentImpl(Optional.of(controller));
    }

    @Nested
    class GetAgentStatus {

        @Test
        void should_return_connected_when_an_active_channel_exist() {
            when(controller.channelsMetricsForTarget(INTEGRATION_ID))
                .thenReturn(Flowable.just(ChannelMetric.builder().id("c1").targetId(INTEGRATION_ID).active(true).primary(true).build()));

            agent.getAgentStatusFor(INTEGRATION_ID).test().awaitDone(10, TimeUnit.SECONDS).assertValue(IntegrationAgent.Status.CONNECTED);
        }

        @Test
        void should_return_disconnected_when_no_active_channel_exist() {
            when(controller.channelsMetricsForTarget(INTEGRATION_ID))
                .thenReturn(
                    Flowable.just(
                        ChannelMetric.builder().id("c1").targetId(INTEGRATION_ID).active(false).primary(false).build(),
                        ChannelMetric.builder().id("c2").targetId(INTEGRATION_ID).active(false).primary(false).build()
                    )
                );

            agent
                .getAgentStatusFor(INTEGRATION_ID)
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .assertValue(IntegrationAgent.Status.DISCONNECTED);
        }

        @Test
        void should_return_disconnected_when_no_channel_metrics_exist() {
            when(controller.channelsMetricsForTarget(INTEGRATION_ID)).thenReturn(Flowable.empty());

            agent
                .getAgentStatusFor(INTEGRATION_ID)
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .assertValue(IntegrationAgent.Status.DISCONNECTED);
        }
    }

    @Nested
    class StartIngest {

        @BeforeEach
        void setUp() {
            lenient()
                .when(controller.sendCommand(any(), any()))
                .thenReturn(Single.just(new StartIngestReply("command-id", new StartIngestReplyPayload(JOB_ID, 10L))));
        }

        @Test
        void should_send_command_to_fetch_all_assets() {
            agent.startIngest(INTEGRATION_ID, JOB_ID).test().awaitDone(10, TimeUnit.SECONDS);

            var captor = ArgumentCaptor.forClass(Command.class);
            Mockito.verify(controller).sendCommand(captor.capture(), Mockito.eq(INTEGRATION_ID));

            assertThat(captor.getValue())
                .isInstanceOf(StartIngestCommand.class)
                .extracting(Command::getPayload)
                .isEqualTo(new StartIngestCommandPayload(JOB_ID, List.of()));
        }

        @Test
        void should_return_ingest_started() {
            agent
                .startIngest(INTEGRATION_ID, JOB_ID)
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .assertValue(result -> {
                    assertThat(result).isEqualTo(new IngestStarted(JOB_ID, 10L));
                    return true;
                });
        }

        @Test
        void should_throw_when_command_fails() {
            when(controller.sendCommand(any(), any())).thenReturn(Single.just(new StartIngestReply("command-id", "Fail to start ingest")));

            agent
                .startIngest(INTEGRATION_ID, JOB_ID)
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .assertError(error -> {
                    assertThat(error).isInstanceOf(IntegrationIngestionException.class).hasMessage("Fail to start ingest");
                    return true;
                });
        }

        @Test
        void should_throw_when_no_controller() {
            agent = new IntegrationAgentImpl(Optional.empty());
            agent
                .startIngest(INTEGRATION_ID, JOB_ID)
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .assertError(error -> {
                    assertThat(error).isInstanceOf(TechnicalDomainException.class).hasMessage("Federation feature not enabled");
                    return true;
                });
        }
    }

    @Nested
    class Subscribe {

        static final String SUBSCRIPTION_ID = "subscription-id";
        static final String APPLICATION_ID = "application-id";
        static final String APPLICATION_NAME = "application-name";
        static final BaseApplicationEntity APPLICATION = BaseApplicationEntity.builder().id(APPLICATION_ID).name(APPLICATION_NAME).build();

        @BeforeEach
        void setUp() {
            lenient()
                .when(controller.sendCommand(any(), any()))
                .thenReturn(
                    Single.just(
                        new SubscribeReply(
                            "command-id",
                            new SubscribeReplyPayload(
                                SubscriptionResult.builder().apiKey("my-api-key").metadata(Map.of("key", "value")).build()
                            )
                        )
                    )
                );
        }

        @Test
        void should_send_command_to_subscribe_for_apikey() {
            var subscriptionParameter = new SubscriptionParameter.ApiKey(
                PlanFixtures.aFederatedPlan().toBuilder().providerId("plan-provider-id").build()
            );
            agent
                .subscribe(
                    INTEGRATION_ID,
                    ApiDefinitionFixtures.aFederatedApi().toBuilder().id("gravitee-api-id").providerId("api-provider-id").build(),
                    subscriptionParameter,
                    SUBSCRIPTION_ID,
                    APPLICATION
                )
                .test()
                .awaitDone(10, TimeUnit.SECONDS);

            var captor = ArgumentCaptor.forClass(Command.class);
            Mockito.verify(controller).sendCommand(captor.capture(), Mockito.eq(INTEGRATION_ID));
            assertThat(captor.getValue())
                .isInstanceOf(SubscribeCommand.class)
                .extracting(Command::getPayload)
                .isEqualTo(
                    new SubscribeCommandPayload(
                        "api-provider-id",
                        new Subscription(
                            SUBSCRIPTION_ID,
                            APPLICATION_ID,
                            APPLICATION_NAME,
                            SubscriptionType.API_KEY,
                            Map.of(Subscription.METADATA_PLAN_ID, "plan-provider-id")
                        )
                    )
                );
        }

        @Test
        void should_send_command_to_subscribe_for_oauth() {
            String oAuthClientId = "client-id";
            String planProviderId = "plan-provider-id";
            var subscriptionParameter = new SubscriptionParameter.OAuth(
                oAuthClientId,
                PlanFixtures
                    .aFederatedPlan()
                    .toBuilder()
                    .security(PlanSecurity.builder().type("oauth2").build())
                    .providerId(planProviderId)
                    .build()
            );
            agent
                .subscribe(
                    INTEGRATION_ID,
                    ApiDefinitionFixtures.aFederatedApi().toBuilder().id("gravitee-api-id").providerId("api-provider-id").build(),
                    subscriptionParameter,
                    SUBSCRIPTION_ID,
                    APPLICATION
                )
                .test()
                .awaitDone(10, TimeUnit.SECONDS);

            var captor = ArgumentCaptor.forClass(Command.class);
            Mockito.verify(controller).sendCommand(captor.capture(), Mockito.eq(INTEGRATION_ID));
            assertThat(captor.getValue())
                .isInstanceOf(SubscribeCommand.class)
                .extracting(Command::getPayload)
                .isEqualTo(
                    new SubscribeCommandPayload(
                        "api-provider-id",
                        new Subscription(
                            SUBSCRIPTION_ID,
                            APPLICATION_ID,
                            APPLICATION_NAME,
                            SubscriptionType.OAUTH,
                            Map.of(Subscription.METADATA_PLAN_ID, planProviderId, Subscription.METADATA_CONSUMER_KEY, oAuthClientId)
                        )
                    )
                );
        }

        @Test
        void should_return_the_API_Key() {
            var result = agent
                .subscribe(
                    INTEGRATION_ID,
                    ApiDefinitionFixtures.aFederatedApi(),
                    PlanFixtures.subscriptionParameter(),
                    SUBSCRIPTION_ID,
                    APPLICATION
                )
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .values();

            assertThat(result)
                .hasSize(1)
                .containsExactly(
                    new IntegrationSubscription(INTEGRATION_ID, IntegrationSubscription.Type.API_KEY, "my-api-key", Map.of("key", "value"))
                );
        }

        @Test
        void should_throw_when_command_fails() {
            when(controller.sendCommand(any(), any())).thenReturn(Single.just(new SubscribeReply("command-id", "Fail to subscribe")));

            agent
                .subscribe(
                    INTEGRATION_ID,
                    ApiDefinitionFixtures.aFederatedApi(),
                    PlanFixtures.subscriptionParameter(),
                    SUBSCRIPTION_ID,
                    APPLICATION
                )
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .assertError(error -> {
                    assertThat(error).isInstanceOf(IntegrationSubscriptionException.class).hasMessage("Fail to subscribe");
                    return true;
                });
        }

        @Test
        void should_throw_when_no_controller() {
            agent = new IntegrationAgentImpl(Optional.empty());
            agent
                .subscribe(
                    INTEGRATION_ID,
                    ApiDefinitionFixtures.aFederatedApi(),
                    PlanFixtures.subscriptionParameter(),
                    SUBSCRIPTION_ID,
                    APPLICATION
                )
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .assertError(error -> {
                    assertThat(error).isInstanceOf(TechnicalDomainException.class).hasMessage("Federation feature not enabled");
                    return true;
                });
        }
    }

    @Nested
    class Unsubscribe {

        @BeforeEach
        void setUp() {
            lenient()
                .when(controller.sendCommand(any(), any()))
                .thenReturn(Single.just(new UnsubscribeReply("command-id", new UnsubscribeReplyPayload())));
        }

        @Test
        void should_send_command_to_unsubscribe() {
            agent
                .unsubscribe(
                    INTEGRATION_ID,
                    ApiDefinitionFixtures.aFederatedApi().toBuilder().id("gravitee-api-id").providerId("api-provider-id").build(),
                    SubscriptionEntity.builder().id("subscription-id").metadata(Map.of("aws-api-key-id", "apikey-123")).build()
                )
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .assertComplete();

            var captor = ArgumentCaptor.forClass(Command.class);
            Mockito.verify(controller).sendCommand(captor.capture(), Mockito.eq(INTEGRATION_ID));
            assertThat(captor.getValue())
                .isInstanceOf(UnsubscribeCommand.class)
                .extracting(Command::getPayload)
                .isEqualTo(
                    new UnsubscribeCommandPayload(
                        "api-provider-id",
                        new Subscription("subscription-id", null, null, null, Map.of("aws-api-key-id", "apikey-123"))
                    )
                );
        }

        @Test
        void should_throw_when_command_fails() {
            when(controller.sendCommand(any(), any())).thenReturn(Single.just(new UnsubscribeReply("command-id", "Fail to unsubscribe")));

            agent
                .unsubscribe(
                    INTEGRATION_ID,
                    ApiDefinitionFixtures.aFederatedApi().toBuilder().id("gravitee-api-id").providerId("api-provider-id").build(),
                    SubscriptionEntity.builder().id("subscription-id").metadata(Map.of("aws-api-key-id", "apikey-123")).build()
                )
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .assertError(error -> {
                    assertThat(error).isInstanceOf(IntegrationSubscriptionException.class).hasMessage("Fail to unsubscribe");
                    return true;
                });
        }

        @Test
        void should_throw_when_metadata_is_null() {
            agent
                .unsubscribe(
                    INTEGRATION_ID,
                    ApiDefinitionFixtures.aFederatedApi().toBuilder().id("gravitee-api-id").providerId("api-provider-id").build(),
                    SubscriptionEntity.builder().id("subscription-id").build()
                )
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .assertComplete();

            var captor = ArgumentCaptor.forClass(Command.class);
            Mockito.verify(controller).sendCommand(captor.capture(), Mockito.eq(INTEGRATION_ID));
            assertThat(captor.getValue())
                .isInstanceOf(UnsubscribeCommand.class)
                .extracting(Command::getPayload)
                .isEqualTo(
                    new UnsubscribeCommandPayload("api-provider-id", new Subscription("subscription-id", null, null, null, Map.of()))
                );
        }

        @Test
        void should_throw_when_no_controller() {
            agent = new IntegrationAgentImpl(Optional.empty());
            agent
                .unsubscribe(
                    INTEGRATION_ID,
                    ApiDefinitionFixtures.aFederatedApi().toBuilder().id("gravitee-api-id").providerId("api-provider-id").build(),
                    SubscriptionEntity.builder().id("subscription-id").build()
                )
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .assertError(error -> {
                    assertThat(error).isInstanceOf(TechnicalDomainException.class).hasMessage("Federation feature not enabled");
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
                        .characteristics(List.of())
                        .build()
                )
            )
            .pages(List.of(new Page(PageType.SWAGGER, "swaggerDoc")))
            .build();
    }

    @Nested
    class Discover {

        @BeforeEach
        void setUp() {
            lenient()
                .when(controller.sendCommand(any(), any()))
                .thenReturn(Single.just(new DiscoverReply("command-id", new DiscoverReplyPayload(List.of(buildApi(1), buildApi(2))))));
        }

        @Test
        void should_send_command_to_discover() {
            agent.discoverApis(INTEGRATION_ID).test().awaitDone(10, TimeUnit.SECONDS);

            var captor = ArgumentCaptor.forClass(Command.class);
            Mockito.verify(controller).sendCommand(captor.capture(), Mockito.eq(INTEGRATION_ID));
            assertThat(captor.getValue())
                .isInstanceOf(DiscoverCommand.class)
                .extracting(Command::getPayload)
                .isEqualTo(new DiscoverCommandPayload());
        }

        @Test
        void should_discover_apis() {
            var result = agent.discoverApis(INTEGRATION_ID).test().awaitDone(10, TimeUnit.SECONDS).values();

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
                        List.of(
                            new IntegrationApi.Plan("plan-id-1", "Gold 1", "Gold description 1", IntegrationApi.PlanType.API_KEY, List.of())
                        ),
                        List.of(new IntegrationApi.Page(IntegrationApi.PageType.SWAGGER, "swaggerDoc")),
                        null
                    ),
                    new IntegrationApi(
                        INTEGRATION_ID,
                        "asset-uid-2",
                        "asset-2",
                        "asset-name-2",
                        "asset-description-2",
                        "asset-version-2",
                        Map.of("url", "https://example.com/2"),
                        List.of(
                            new IntegrationApi.Plan("plan-id-2", "Gold 2", "Gold description 2", IntegrationApi.PlanType.API_KEY, List.of())
                        ),
                        List.of(new IntegrationApi.Page(IntegrationApi.PageType.SWAGGER, "swaggerDoc")),
                        null
                    )
                );
        }

        @Test
        void should_return_empty_when_nothing_discovered() {
            when(controller.sendCommand(any(), any()))
                .thenReturn(Single.just(new DiscoverReply("command-id", new DiscoverReplyPayload(List.of()))));

            agent.discoverApis(INTEGRATION_ID).test().awaitDone(10, TimeUnit.SECONDS).assertNoValues();
        }

        @Test
        void should_throw_exception_when_command_fails() {
            when(controller.sendCommand(any(), any())).thenReturn(Single.just(new DiscoverReply("command-id", "Fail to discover")));

            agent
                .discoverApis(INTEGRATION_ID)
                .test()
                .awaitDone(10, TimeUnit.SECONDS)
                .assertError(error -> {
                    assertThat(error).isInstanceOf(IntegrationDiscoveryException.class).hasMessage("Fail to discover");
                    return true;
                });
        }
    }

    @Test
    void should_throw_when_no_controller() {
        agent = new IntegrationAgentImpl(Optional.empty());
        agent
            .discoverApis(INTEGRATION_ID)
            .test()
            .awaitDone(10, TimeUnit.SECONDS)
            .assertError(error -> {
                assertThat(error).isInstanceOf(TechnicalDomainException.class).hasMessage("Federation feature not enabled");
                return true;
            });
    }
}
