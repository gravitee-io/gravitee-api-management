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
import io.gravitee.plugin.entrypoint.webhook.configuration.WebhookEntrypointConnectorSubscriptionConfiguration;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@GatewayTest
@DeployApi({ "/apis/webhook-entrypoint.json" })
class WebhookEntrypointMockEndpointIntegrationTest extends AbstractGatewayTest {

    private static final String API_ID = "my-api";
    private static final String SUBSCRIPTION_ID = "my-subscription";
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
    void shouldSendMessagesFromMockEndpointToWebhookEntrypoint() throws JsonProcessingException {
        WebhookEntrypointConnectorSubscriptionConfiguration configuration = new WebhookEntrypointConnectorSubscriptionConfiguration();
        configuration.setCallbackUrl(String.format("http://localhost:%s%s", wiremock.port(), WEBHOOK_URL_PATH));

        Subscription subscription = new Subscription();
        subscription.setApi(API_ID);
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setStatus("ACCEPTED");
        subscription.setConfiguration(MAPPER.writeValueAsString(configuration));

        getBean(SubscriptionDispatcher.class).dispatch(subscription);

        // wait for callback wiremock to receive 7 requests (timeouts after 1 sec)
        interval(50, MILLISECONDS)
            .takeWhile(i -> wiremock.countRequestsMatching(anyRequestedFor(anyUrl()).build()).getCount() < 7)
            .test()
            .awaitDone(1, SECONDS)
            .assertComplete();

        // verify requests received by wiremock
        wiremock.verify(7, postRequestedFor(urlPathEqualTo(WEBHOOK_URL_PATH)).withRequestBody(equalTo("Mock data")));
    }
}
