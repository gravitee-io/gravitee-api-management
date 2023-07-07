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
import io.gravitee.apim.integration.tests.messages.AbstractKafkaEndpointIntegrationTest;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.reactive.reactor.v4.subscription.SubscriptionDispatcher;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.reactivex.rxjava3.core.Single;
import io.vertx.rxjava3.core.Vertx;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    @BeforeEach
    public void prepare() {
        webhookActions = new WebhookTestingActions(wiremock, getBean(SubscriptionDispatcher.class));
    }

    @Test
    @DeployApi({ "/apis/v4/messages/webhook/webhook-entrypoint-kafka-endpoint.json" })
    void should_receive_all_messages_without_header(Vertx vertx) throws JsonProcessingException {
        final String callbackPath = WEBHOOK_URL_PATH + "/without-header";

        // In order to simplify the test, Kafka endpoint's consumer is configured with "autoOffsetReset": "earliest"
        // It allows us to publish the messages in the topic before opening the api connection.
        Single
            .fromCallable(() -> getKafkaProducer(vertx))
            .flatMapCompletable(producer ->
                publishToKafka(producer, "message1")
                    .andThen(publishToKafka(producer, "message2"))
                    .andThen(publishToKafka(producer, "message3"))
                    .doFinally(producer::close)
            )
            .blockingAwait();

        Subscription subscription = webhookActions.configureSubscriptionAndCallback("webhook-entrypoint-kafka-endpoint", callbackPath);

        webhookActions.dispatchSubscriptionAndWaitForRequestsOnCallback(subscription, 3, callbackPath);

        // verify requests received by wiremock
        wiremock.verify(1, postRequestedFor(urlPathEqualTo(callbackPath)).withRequestBody(equalTo("message1")));
        wiremock.verify(1, postRequestedFor(urlPathEqualTo(callbackPath)).withRequestBody(equalTo("message2")));
        wiremock.verify(1, postRequestedFor(urlPathEqualTo(callbackPath)).withRequestBody(equalTo("message3")));

        // close the subscription to avoid maintaining it between test methods
        webhookActions.closeSubscription(subscription);
    }

    @Test
    @DeployApi({ "/apis/v4/messages/webhook/webhook-entrypoint-kafka-endpoint.json" })
    void should_receive_all_messages_with_additional_headers(Vertx vertx) throws JsonProcessingException {
        final String callbackPath = WEBHOOK_URL_PATH + "/with-header";

        // In order to simplify the test, Kafka endpoint's consumer is configured with "autoOffsetReset": "earliest"
        // It allows us to publish the messages in the topic before opening the api connection.
        Single
            .fromCallable(() -> getKafkaProducer(vertx))
            .flatMapCompletable(producer ->
                publishToKafka(producer, "message1")
                    .andThen(publishToKafka(producer, "message2"))
                    .andThen(publishToKafka(producer, "message3"))
                    .doFinally(producer::close)
            )
            .blockingAwait();

        Subscription subscription = webhookActions.configureSubscriptionAndCallback(
            "webhook-entrypoint-kafka-endpoint",
            callbackPath,
            null,
            List.of(new HttpHeader("Header1", "my-header-1-value"), new HttpHeader("Header2", "my-header-2-value"))
        );

        webhookActions.dispatchSubscriptionAndWaitForRequestsOnCallback(subscription, 3, callbackPath);
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
