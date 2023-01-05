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
package io.gravitee.plugin.entrypoint.webhook;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.reactivex.rxjava3.core.Observable.interval;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.gateway.tests.sdk.AbstractGatewayTest;
import io.gravitee.apim.gateway.tests.sdk.annotations.DeployApi;
import io.gravitee.apim.gateway.tests.sdk.annotations.GatewayTest;
import io.gravitee.apim.gateway.tests.sdk.configuration.GatewayConfigurationBuilder;
import io.gravitee.apim.gateway.tests.sdk.connector.EntrypointBuilder;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.jupiter.reactor.v4.subscription.SubscriptionDispatcher;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import io.gravitee.plugin.entrypoint.webhook.configuration.HttpHeader;
import io.gravitee.plugin.entrypoint.webhook.configuration.WebhookEntrypointConnectorSubscriptionConfiguration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@Disabled
@GatewayTest
@DeployApi({ "/apis/webhook-entrypoint.json" })
class WebhookEntrypointMockEndpointIntegrationTest extends AbstractGatewayTest {

    private static final String API_ID = "my-api";
    private static final String WEBHOOK_URL_PATH = "/webhook";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public void configureEntrypoints(Map<String, EntrypointConnectorPlugin<?, ?>> entrypoints) {
        entrypoints.putIfAbsent("webhook", EntrypointBuilder.build("webhook", WebhookEntrypointConnectorFactory.class));
    }

    @Override
    protected void configureGateway(GatewayConfigurationBuilder gatewayConfigurationBuilder) {
        gatewayConfigurationBuilder.set("api.jupiterMode.enabled", "true");
        gatewayConfigurationBuilder.set("api.jupiterMode.default", "always");
    }

    @Test
    @DisplayName("Should send messages from mock endpoint to webhook entrypoint callback URL")
    @Disabled("Disabled for now as it is flaky on CI")
    void shouldSendMessagesFromMockEndpointToWebhookEntrypoint() throws JsonProcessingException {
        wiremock.stubFor(post(WEBHOOK_URL_PATH).willReturn(ok()));
        WebhookEntrypointConnectorSubscriptionConfiguration configuration = new WebhookEntrypointConnectorSubscriptionConfiguration();
        configuration.setCallbackUrl(String.format("http://localhost:%s%s", wiremock.port(), WEBHOOK_URL_PATH));

        Subscription subscription = buildTestSubscription(configuration);

        getBean(SubscriptionDispatcher.class).dispatch(subscription);

        // wait for callback wiremock to receive 7 requests (timeouts after 1 sec)
        interval(50, MILLISECONDS)
            .takeWhile(i -> wiremock.countRequestsMatching(anyRequestedFor(anyUrl()).build()).getCount() < 7)
            .test()
            .awaitDone(60, SECONDS)
            .assertComplete();

        // verify requests received by wiremock
        wiremock.verify(7, postRequestedFor(urlPathEqualTo(WEBHOOK_URL_PATH)).withRequestBody(equalTo("Mock data")));
    }

    @Test
    @DisplayName("Should send messages from mock endpoint to webhook entrypoint callback URL, with additional headers")
    @Disabled("Disabled for now as it is flaky on CI")
    void shouldSendMessagesFromMockEndpointToWebhookEntrypointWithHeaders() throws JsonProcessingException {
        wiremock.stubFor(post(WEBHOOK_URL_PATH).willReturn(ok()));
        WebhookEntrypointConnectorSubscriptionConfiguration configuration = new WebhookEntrypointConnectorSubscriptionConfiguration();
        configuration.setCallbackUrl(String.format("http://localhost:%s%s", wiremock.port(), WEBHOOK_URL_PATH));
        configuration.setHeaders(List.of(new HttpHeader("Header1", "my-header-1-value"), new HttpHeader("Header2", "my-header-2-value")));

        Subscription subscription = buildTestSubscription(configuration);

        getBean(SubscriptionDispatcher.class).dispatch(subscription);

        // wait for callback wiremock to receive 7 requests (timeouts after 1 sec)
        interval(50, MILLISECONDS)
            .takeWhile(i -> wiremock.countRequestsMatching(anyRequestedFor(anyUrl()).build()).getCount() < 7)
            .test()
            .awaitDone(60, SECONDS)
            .assertComplete();

        // verify requests received by wiremock
        wiremock.verify(
            7,
            postRequestedFor(urlPathEqualTo(WEBHOOK_URL_PATH))
                .withRequestBody(equalTo("Mock data"))
                .withHeader("Header1", equalTo("my-header-1-value"))
                .withHeader("Header2", equalTo("my-header-2-value"))
        );
    }

    private Subscription buildTestSubscription(WebhookEntrypointConnectorSubscriptionConfiguration configuration)
        throws JsonProcessingException {
        Subscription subscription = new Subscription();
        subscription.setApi(API_ID);
        subscription.setId(UUID.randomUUID().toString());
        subscription.setStatus("ACCEPTED");
        subscription.setConsumerStatus(Subscription.ConsumerStatus.STARTED);
        subscription.setConfiguration(MAPPER.writeValueAsString(configuration));
        return subscription;
    }
}
