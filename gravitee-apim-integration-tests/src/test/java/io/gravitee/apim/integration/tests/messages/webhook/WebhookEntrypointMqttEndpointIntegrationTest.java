/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.apim.integration.tests.messages.webhook;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import com.graviteesource.entrypoint.webhook.WebhookEntrypointConnectorFactory;
import com.graviteesource.entrypoint.webhook.configuration.HttpHeader;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.integration.tests.fake.MessageFlowReadyPolicy;
import io.gravitee.apim.integration.tests.messages.AbstractMqtt5EndpointIntegrationTest;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.reactive.api.qos.Qos;
import io.gravitee.gateway.reactive.reactor.v4.subscription.SubscriptionDispatcher;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.observers.TestObserver;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class WebhookEntrypointMqttEndpointIntegrationTest extends AbstractMqtt5EndpointIntegrationTest {

    private static final String WEBHOOK_URL_PATH = "/webhook";

    private WebhookTestingActions webhookActions;

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("webhook", EntrypointBuilder.build("webhook", WebhookEntrypointConnectorFactory.class));
    }

    @BeforeEach
    void setUp() {
        webhookActions = new WebhookTestingActions(wiremock, getBean(SubscriptionDispatcher.class));
    }

    @ParameterizedTest
    @MethodSource("allQosParameters")
    @DeployApi(
        {
            "/apis/v4/messages/mqtt5/mqtt5-endpoint-qos-auto.json",
            "/apis/v4/messages/mqtt5/mqtt5-endpoint-qos-none.json",
            "/apis/v4/messages/mqtt5/mqtt5-endpoint-qos-at-most-once.json",
            "/apis/v4/messages/mqtt5/mqtt5-endpoint-qos-at-least-once.json",
        }
    )
    void should_receive_messages_single(Qos qos, MqttQos publishQos) throws JsonProcessingException {
        final int messageCount = 10;
        final String callbackPath = WEBHOOK_URL_PATH + "/without-header";
        final List<Completable> readyObs = new ArrayList<>();

        final Subscription subscription = createSubscription(qos, callbackPath, readyObs);

        final TestObserver<Void> obs = Completable
            .mergeArray(
                webhookActions.dispatchSubscription(subscription),
                publishMessagesWhenReady(readyObs, TEST_TOPIC + "-qos-" + qos.getLabel(), publishQos)
            )
            .takeUntil(webhookActions.waitForRequestsOnCallback(messageCount, callbackPath))
            .test();

        obs.awaitDone(30, TimeUnit.SECONDS).assertComplete();

        // Verify requests received by wiremock
        verifyMessages(messageCount, callbackPath);
    }

    @ParameterizedTest
    @MethodSource("allQosParameters")
    @DeployApi(
        {
            "/apis/v4/messages/mqtt5/mqtt5-endpoint-qos-auto.json",
            "/apis/v4/messages/mqtt5/mqtt5-endpoint-qos-none.json",
            "/apis/v4/messages/mqtt5/mqtt5-endpoint-qos-at-most-once.json",
            "/apis/v4/messages/mqtt5/mqtt5-endpoint-qos-at-least-once.json",
        }
    )
    void should_receive_all_messages_with_additional_headers(Qos qos, MqttQos publishQos) throws JsonProcessingException {
        final int messageCount = 10;
        final String callbackPath = WEBHOOK_URL_PATH + "/with-header";
        final List<Completable> readyObs = new ArrayList<>();

        final List<HttpHeader> headers = List.of(
            new HttpHeader("Header1", "my-header-1-value"),
            new HttpHeader("Header2", "my-header-2-value")
        );

        final Subscription subscription = createSubscription(qos, callbackPath, headers, readyObs);

        final TestObserver<Void> obs = Completable
            .mergeArray(
                webhookActions.dispatchSubscription(subscription),
                publishMessagesWhenReady(readyObs, TEST_TOPIC + "-qos-" + qos.getLabel(), publishQos)
            )
            .takeUntil(webhookActions.waitForRequestsOnCallback(messageCount, callbackPath))
            .test();

        obs.awaitDone(30, TimeUnit.SECONDS).assertComplete();

        // Verify requests received by wiremock
        verifyMessagesWithHeaders(messageCount, callbackPath, headers);
    }

    @ParameterizedTest
    @MethodSource("allQosParameters")
    @DeployApi(
        {
            "/apis/v4/messages/mqtt5/mqtt5-endpoint-qos-auto.json",
            "/apis/v4/messages/mqtt5/mqtt5-endpoint-qos-none.json",
            "/apis/v4/messages/mqtt5/mqtt5-endpoint-qos-at-most-once.json",
            "/apis/v4/messages/mqtt5/mqtt5-endpoint-qos-at-least-once.json",
        }
    )
    void should_receive_messages_parallel(Qos qos, MqttQos publishQos) throws JsonProcessingException {
        final int messageCount = 40;
        final String callbackPath = WEBHOOK_URL_PATH + "/without-header";
        final List<Completable> readyObs = new ArrayList<>();

        // Simulate the same subscription running on 2 different instances.
        final Subscription subscriptionInstance1 = createSubscription(qos, callbackPath, readyObs);
        final Subscription subscriptionInstance2 = createSubscription(qos, callbackPath, readyObs);
        subscriptionInstance2.setId(subscriptionInstance1.getId());

        final TestObserver<Void> obs = Completable
            .mergeArray(
                webhookActions.dispatchSubscription(subscriptionInstance1),
                webhookActions.dispatchSubscription(subscriptionInstance2),
                publishMessagesWhenReady(readyObs, TEST_TOPIC + "-qos-" + qos.getLabel(), publishQos)
            )
            .takeUntil(webhookActions.waitForRequestsOnCallback(messageCount, callbackPath))
            .test();

        obs.awaitDone(30, TimeUnit.SECONDS).assertComplete();

        // Verify requests received by wiremock has no duplicates.
        verifyMessages(messageCount, callbackPath);
    }

    private void verifyMessages(int messageCount, String callbackPath) {
        for (int i = 0; i < messageCount; i++) {
            wiremock.verify(1, postRequestedFor(urlPathEqualTo(callbackPath)).withRequestBody(equalTo("message-" + i)));
        }
    }

    private void verifyMessagesWithHeaders(int messageCount, String callbackPath, List<HttpHeader> headers) {
        for (int i = 0; i < messageCount; i++) {
            final RequestPatternBuilder requestPatternBuilder = postRequestedFor(urlPathEqualTo(callbackPath))
                .withRequestBody(equalTo("message-" + i));

            for (HttpHeader header : headers) {
                requestPatternBuilder.withHeader(header.getName(), equalTo(header.getValue()));
            }

            wiremock.verify(1, requestPatternBuilder);
        }
    }

    private Subscription createSubscription(Qos qos, String callbackPath, List<Completable> readyObs) throws JsonProcessingException {
        return this.createSubscription(qos, callbackPath, null, readyObs);
    }

    private Subscription createSubscription(Qos qos, String callbackPath, List<HttpHeader> headers, List<Completable> readyObs)
        throws JsonProcessingException {
        final Subscription subscription = webhookActions.configureSubscriptionAndCallback(
            "mqtt5-endpoint-qos-" + qos.getLabel(),
            callbackPath,
            null,
            headers
        );
        readyObs.add(MessageFlowReadyPolicy.readyObs(subscription));

        return subscription;
    }
}
