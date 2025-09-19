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

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.graviteesource.entrypoint.webhook.WebhookEntrypointConnectorFactory;
import com.graviteesource.entrypoint.webhook.configuration.HttpHeader;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.integration.tests.messages.AbstractRabbitMQEndpointIntegrationTest;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.reactive.reactor.v4.subscription.SubscriptionDispatcher;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.reactivex.rxjava3.disposables.Disposable;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.rabbitmq.OutboundMessage;

@GatewayTest
class WebhookEntrypointRabbitMQEndpointIntegrationTest extends AbstractRabbitMQEndpointIntegrationTest {

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

    @Test
    @DeployApi({ "/apis/v4/messages/webhook/webhook-entrypoint-rabbitmq-endpoint.json" })
    void should_receive_all_messages_without_header() throws JsonProcessingException {
        final String callbackPath = WEBHOOK_URL_PATH + "/without-header";

        Subscription subscription = webhookActions.configureSubscriptionAndCallback("webhook-entrypoint-rabbitmq-endpoint", callbackPath);

        final Disposable disposableSubscription = webhookActions.dispatchSubscription(subscription).subscribe();

        // Publish several messages after a delay, to be sure the subscription has been fully dispatched and consumes message
        publishToRabbitMQ(
            Flux.just(
                new OutboundMessage(exchange, routingKey, "message1".getBytes()),
                new OutboundMessage(exchange, routingKey, "message2".getBytes()),
                new OutboundMessage(exchange, routingKey, "message3".getBytes())
            ),
            5000
        ).blockingAwait();

        // Wait for the webhook to have received 3 requests (message1, message2 and message3)
        webhookActions.waitForRequestsOnCallbackBlocking(3, callbackPath, disposableSubscription);

        // verify requests received by wiremock
        wiremock.verify(1, postRequestedFor(urlPathEqualTo(callbackPath)).withRequestBody(equalTo("message1")));
        wiremock.verify(1, postRequestedFor(urlPathEqualTo(callbackPath)).withRequestBody(equalTo("message2")));
        wiremock.verify(1, postRequestedFor(urlPathEqualTo(callbackPath)).withRequestBody(equalTo("message3")));

        // close the subscription to avoid maintaining it between test methods
        webhookActions.closeSubscription(subscription);
    }

    @Test
    @DeployApi({ "/apis/v4/messages/webhook/webhook-entrypoint-rabbitmq-endpoint.json" })
    void should_receive_all_messages_with_additional_headers() throws JsonProcessingException {
        final String callbackPath = WEBHOOK_URL_PATH + "/with-header";

        Subscription subscription = webhookActions.configureSubscriptionAndCallback(
            "webhook-entrypoint-rabbitmq-endpoint",
            callbackPath,
            List.of(new HttpHeader("Header1", "my-header-1-value"), new HttpHeader("Header2", "my-header-2-value"))
        );

        // Publish a first message that is retained before subscribing
        final Disposable disposableSubscription = webhookActions.dispatchSubscription(subscription).subscribe();

        // Publish several messages after a delay, to be sure the subscription has been fully dispatched and consumes message
        publishToRabbitMQ(
            Flux.just(
                new OutboundMessage(exchange, routingKey, "message1".getBytes()),
                new OutboundMessage(exchange, routingKey, "message2".getBytes()),
                new OutboundMessage(exchange, routingKey, "message3".getBytes())
            ),
            5000
        ).blockingAwait();

        // Wait for the webhook to have received 4 requests (message, message1, message2 and message3)
        webhookActions.waitForRequestsOnCallbackBlocking(3, callbackPath, disposableSubscription);

        // verify requests received by wiremock
        wiremock.verify(
            1,
            postRequestedFor(urlPathEqualTo(callbackPath))
                .withRequestBody(equalTo("message1"))
                .withHeader("Header1", equalTo("my-header-1-value"))
                .withHeader("Header2", equalTo("my-header-2-value"))
        );
        wiremock.verify(
            1,
            postRequestedFor(urlPathEqualTo(callbackPath))
                .withRequestBody(equalTo("message2"))
                .withHeader("Header1", equalTo("my-header-1-value"))
                .withHeader("Header2", equalTo("my-header-2-value"))
        );
        wiremock.verify(
            1,
            postRequestedFor(urlPathEqualTo(callbackPath))
                .withRequestBody(equalTo("message3"))
                .withHeader("Header1", equalTo("my-header-1-value"))
                .withHeader("Header2", equalTo("my-header-2-value"))
        );

        // close the subscription to avoid maintaining it between test methods
        webhookActions.closeSubscription(subscription);
    }
}
