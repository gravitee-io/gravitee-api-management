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
package io.gravitee.apim.integration.tests.messages.webhook;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.graviteesource.entrypoint.webhook.WebhookEntrypointConnectorFactory;
import com.graviteesource.entrypoint.webhook.configuration.HttpHeader;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.InjectApi;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.policy.PolicyBuilder;
import io.gravitee.apim.integration.tests.fake.ForceClientIdentifierPolicy;
import io.gravitee.apim.integration.tests.messages.AbstractKafkaEndpointIntegrationTest;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.reactive.api.qos.Qos;
import io.gravitee.gateway.reactive.reactor.v4.subscription.SubscriptionDispatcher;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.policy.PolicyPlugin;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
class WebhookEntrypointKafkaEndpointIntegrationTest extends AbstractKafkaEndpointIntegrationTest {

    private static final String WEBHOOK_URL_PATH = "/webhook";

    private WebhookTestingActions webhookActions;

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("webhook", EntrypointBuilder.build("webhook", WebhookEntrypointConnectorFactory.class));
    }

    @Override
    public void configurePolicies(Map<String, PolicyPlugin> policies) {
        super.configurePolicies(policies);
        policies.putIfAbsent("force-client-identifier", PolicyBuilder.build("force-client-identifier", ForceClientIdentifierPolicy.class));
    }

    @BeforeEach
    public void prepare() {
        webhookActions = new WebhookTestingActions(wiremock, getBean(SubscriptionDispatcher.class));
    }

    @ParameterizedTest
    @MethodSource("allQosParameters")
    @DeployApi(
        {
            "/apis/v4/messages/kafka/kafka-endpoint-qos-auto.json",
            "/apis/v4/messages/kafka/kafka-endpoint-qos-none.json",
            "/apis/v4/messages/kafka/kafka-endpoint-qos-at-most-once.json",
            "/apis/v4/messages/kafka/kafka-endpoint-qos-at-least-once.json",
        }
    )
    void should_receive_messages_single(Qos qos) throws JsonProcessingException {
        final int messageCount = 10;
        final String callbackPath = WEBHOOK_URL_PATH + "/without-header";
        final List<Completable> readyObs = new ArrayList<>();

        final Subscription subscription = webhookActions.createSubscription("kafka-endpoint-qos-" + qos.getLabel(), callbackPath, readyObs);

        final TestObserver<Void> obs = Completable.mergeArray(
            webhookActions.dispatchSubscription(subscription),
            publishMessagesWhenReady(readyObs, TEST_TOPIC + "-qos-" + qos.getLabel())
        )
            .takeUntil(webhookActions.waitForRequestsOnCallback(messageCount, callbackPath))
            .test();

        obs.awaitDone(30, TimeUnit.SECONDS).assertComplete();

        // Verify requests received by wiremock
        webhookActions.verifyMessages(messageCount, callbackPath);
    }

    @ParameterizedTest
    @MethodSource("allQosParameters")
    @DeployApi(
        {
            "/apis/v4/messages/kafka/kafka-endpoint-qos-auto.json",
            "/apis/v4/messages/kafka/kafka-endpoint-qos-none.json",
            "/apis/v4/messages/kafka/kafka-endpoint-qos-at-most-once.json",
            "/apis/v4/messages/kafka/kafka-endpoint-qos-at-least-once.json",
        }
    )
    void should_receive_all_messages_with_additional_headers(Qos qos) throws JsonProcessingException {
        final int messageCount = 10;
        final String callbackPath = WEBHOOK_URL_PATH + "/with-header";
        final List<Completable> readyObs = new ArrayList<>();

        final List<HttpHeader> headers = List.of(
            new HttpHeader("Header1", "my-header-1-value"),
            new HttpHeader("Header2", "my-header-2-value")
        );

        final Subscription subscription = webhookActions.createSubscription(
            "kafka-endpoint-qos-" + qos.getLabel(),
            callbackPath,
            headers,
            readyObs
        );

        final TestObserver<Void> obs = Completable.mergeArray(
            webhookActions.dispatchSubscription(subscription),
            publishMessagesWhenReady(readyObs, TEST_TOPIC + "-qos-" + qos.getLabel())
        )
            .takeUntil(webhookActions.waitForRequestsOnCallback(messageCount, callbackPath))
            .test();

        obs.awaitDone(30, TimeUnit.SECONDS).assertComplete();

        // Verify requests received by wiremock
        webhookActions.verifyMessagesWithHeaders(messageCount, callbackPath, headers);
    }

    @ParameterizedTest
    @MethodSource("allQosParameters")
    @DeployApi(
        {
            "/apis/v4/messages/kafka/kafka-endpoint-qos-auto.json",
            "/apis/v4/messages/kafka/kafka-endpoint-qos-none.json",
            "/apis/v4/messages/kafka/kafka-endpoint-qos-at-most-once.json",
            "/apis/v4/messages/kafka/kafka-endpoint-qos-at-least-once.json",
        }
    )
    void should_receive_messages_parallel(Qos qos, boolean unused, @InjectApi Map<String, ReactableApi<?>> reactableApis)
        throws JsonProcessingException {
        final int messageCount = 40;
        final String callbackPath = WEBHOOK_URL_PATH + "/without-header";
        final List<Completable> readyObs = new ArrayList<>();

        // Simulate the same subscription running on 2 different instances.
        final String apiId = "kafka-endpoint-qos-" + qos.getLabel();

        // Reconfigure the api to add a special policy that forces the client identifier since it is not possible to set it when using webhook.
        final ReactableApi<?> api = reactableApis.get(apiId);
        ((Api) api.getDefinition()).getFlows()
            .get(0)
            .setRequest(List.of(Step.builder().name("Force client identifier").policy("force-client-identifier").enabled(true).build()));

        redeploy(api);

        final Subscription subscriptionInstance1 = webhookActions.createSubscription(apiId, callbackPath, readyObs);
        final Subscription subscriptionInstance2 = webhookActions.createSubscription(apiId, callbackPath, readyObs);

        final TestObserver<Void> obs = Completable.mergeArray(
            webhookActions.dispatchSubscription(subscriptionInstance1),
            webhookActions.dispatchSubscription(subscriptionInstance2),
            publishMessagesWhenReady(readyObs, TEST_TOPIC + "-qos-" + qos.getLabel())
        )
            .takeUntil(webhookActions.waitForRequestsOnCallback(messageCount, callbackPath))
            .test();

        obs.awaitDone(30, TimeUnit.SECONDS).assertComplete();

        // Verify requests received by wiremock has no duplicates.
        webhookActions.verifyMessages(messageCount, callbackPath);
    }
}
