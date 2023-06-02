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
import static io.reactivex.rxjava3.core.Observable.interval;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.graviteesource.entrypoint.webhook.WebhookEntrypointConnectorFactory;
import com.graviteesource.entrypoint.webhook.configuration.HttpHeader;
import com.graviteesource.entrypoint.webhook.configuration.WebhookEntrypointConnectorSubscriptionConfiguration;
import com.graviteesource.reactor.message.MessageApiReactorFactory;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.apim.gateway.tests.sdk.reactor.ReactorBuilder;
import io.gravitee.apim.plugin.reactor.ReactorPlugin;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.api.service.SubscriptionConfiguration;
import io.gravitee.gateway.reactive.reactor.v4.reactor.ReactorFactory;
import io.gravitee.gateway.reactive.reactor.v4.subscription.SubscriptionDispatcher;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.reactivex.rxjava3.core.Completable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@GatewayTest
class WebhookEntrypointMockEndpointIntegrationTest extends AbstractGatewayTest {

    private static final String API_ID = "my-api";
    private static final String WEBHOOK_URL_PATH = "/webhook";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void configureReactors(Set<ReactorPlugin<? extends ReactorFactory<?>>> reactors) {
        reactors.add(ReactorBuilder.build(MessageApiReactorFactory.class));
    }

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("webhook", EntrypointBuilder.build("webhook", WebhookEntrypointConnectorFactory.class));
    }

    @Test
    @DisplayName("Should send messages from mock endpoint to webhook entrypoint callback URL")
    @DeployApi({ "/apis/v4/messages/webhook/api.json" })
    void shouldSendMessagesFromMockEndpointToWebhookEntrypoint() throws JsonProcessingException {
        WebhookEntrypointConnectorSubscriptionConfiguration configuration = new WebhookEntrypointConnectorSubscriptionConfiguration();
        final String callbackPath = WEBHOOK_URL_PATH + "/without-header";
        configuration.setCallbackUrl(String.format("http://localhost:%s%s", wiremock.port(), callbackPath));
        wiremock.stubFor(post(callbackPath).willReturn(ok()));

        Subscription subscription = buildTestSubscription(configuration);

        dispatchSubscription(subscription)
            .andThen(
                interval(50, MILLISECONDS)
                    .takeWhile(i -> wiremock.countRequestsMatching(anyRequestedFor(urlPathEqualTo(callbackPath)).build()).getCount() < 7)
            )
            .test()
            .awaitDone(10, SECONDS)
            .assertComplete();

        // verify requests received by wiremock
        wiremock.verify(7, postRequestedFor(urlPathEqualTo(callbackPath)).withRequestBody(equalTo("Mock data")));

        // close the subscription to avoid maintaining it between test methods
        subscription.setStatus("CLOSED");
        dispatchSubscription(subscription).blockingAwait();
    }

    @Test
    @DisplayName("Should send messages from mock endpoint to webhook entrypoint callback URL, with additional headers")
    @DeployApi({ "/apis/v4/messages/webhook/api.json" })
    void shouldSendMessagesFromMockEndpointToWebhookEntrypointWithHeaders() throws JsonProcessingException {
        WebhookEntrypointConnectorSubscriptionConfiguration configuration = new WebhookEntrypointConnectorSubscriptionConfiguration();
        final String callbackPath = WEBHOOK_URL_PATH + "/with-headers";
        configuration.setCallbackUrl(String.format("http://localhost:%s%s", wiremock.port(), callbackPath));
        configuration.setHeaders(List.of(new HttpHeader("Header1", "my-header-1-value"), new HttpHeader("Header2", "my-header-2-value")));
        wiremock.stubFor(post(callbackPath).willReturn(ok()));

        Subscription subscription = buildTestSubscription(configuration);
        subscription.setApi(API_ID);

        dispatchSubscription(subscription)
            .andThen(
                // wait for callback wiremock to receive 7 requests (timeouts after 1 sec)
                interval(50, MILLISECONDS)
                    .takeWhile(i -> wiremock.countRequestsMatching(anyRequestedFor(urlPathEqualTo(callbackPath)).build()).getCount() < 7)
            )
            .test()
            .awaitDone(10, SECONDS)
            .assertComplete();

        // verify requests received by wiremock
        wiremock.verify(
            7,
            postRequestedFor(urlPathEqualTo(callbackPath))
                .withRequestBody(equalTo("Mock data"))
                .withHeader("Header1", equalTo("my-header-1-value"))
                .withHeader("Header2", equalTo("my-header-2-value"))
        );

        // close the subscription to avoid maintaining it between test methods
        subscription.setStatus("CLOSED");
        dispatchSubscription(subscription).blockingAwait();
    }

    private Completable dispatchSubscription(Subscription subscription) {
        return getBean(SubscriptionDispatcher.class).dispatch(subscription);
    }

    private Subscription buildTestSubscription(WebhookEntrypointConnectorSubscriptionConfiguration configuration)
        throws JsonProcessingException {
        Subscription subscription = new Subscription();
        subscription.setApi(API_ID);
        subscription.setId(UUID.randomUUID().toString());
        subscription.setStatus("ACCEPTED");
        SubscriptionConfiguration subscriptionConfiguration = new SubscriptionConfiguration(
            "subscribe",
            "webhook",
            MAPPER.writeValueAsString(configuration)
        );
        subscription.setConfiguration(subscriptionConfiguration);
        return subscription;
    }
}
