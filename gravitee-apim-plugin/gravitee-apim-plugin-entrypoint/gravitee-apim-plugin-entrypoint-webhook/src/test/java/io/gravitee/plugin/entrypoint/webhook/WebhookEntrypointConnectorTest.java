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

import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.gravitee.plugin.entrypoint.webhook.WebhookEntrypointConnector.INTERNAL_ATTR_WEBHOOK_HTTP_CLIENT;
import static io.gravitee.plugin.entrypoint.webhook.WebhookEntrypointConnector.INTERNAL_ATTR_WEBHOOK_REQUEST_URI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.jupiter.api.ApiType;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.ListenerType;
import io.gravitee.gateway.jupiter.api.connector.ConnectorFactoryHelper;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.InternalContextAttributes;
import io.gravitee.gateway.jupiter.api.context.Request;
import io.gravitee.gateway.jupiter.api.context.Response;
import io.gravitee.gateway.jupiter.api.exception.PluginConfigurationException;
import io.gravitee.gateway.jupiter.api.message.DefaultMessage;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.plugin.entrypoint.webhook.configuration.WebhookEntrypointConnectorConfiguration;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.observers.TestObserver;
import io.reactivex.subscribers.TestSubscriber;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpClient;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@WireMockTest
class WebhookEntrypointConnectorTest {

    private final Vertx vertx = Vertx.vertx();

    protected static final String SUBSCRIPTION_CONFIGURATION = "subscription configuration";
    private final Vertx vertx = Vertx.vertx();

    @Mock
    private ExecutionContext ctx;

    @Mock
    private Request mockMessageRequest;

    @Mock
    private Response response;

    @Mock
    private WebhookEntrypointConnectorConfiguration configuration;

    @Mock
    private ConnectorFactoryHelper connectorFactoryHelper;

    @Mock
    private Subscription subscription;

    @Captor
    private ArgumentCaptor<Flowable<Buffer>> chunksCaptor;

    @Captor
    private ArgumentCaptor<HttpClient> httpClientCaptor;

    private WebhookEntrypointConnector cut;

    @BeforeEach
    void beforeEach() throws PluginConfigurationException {
        lenient().when(ctx.request()).thenReturn(mockMessageRequest);
        lenient().when(ctx.response()).thenReturn(response);
        lenient().when(response.end()).thenReturn(Completable.complete());
        lenient().when(ctx.getComponent(io.vertx.reactivex.core.Vertx.class)).thenReturn(vertx);
        lenient().when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION)).thenReturn(subscription);
        lenient().when(ctx.getComponent(ConnectorFactoryHelper.class)).thenReturn(connectorFactoryHelper);
        lenient()
            .when(
                connectorFactoryHelper.getConnectorConfiguration(
                    eq(WebhookEntrypointConnectorConfiguration.class),
                    eq(SUBSCRIPTION_CONFIGURATION)
                )
            )
            .thenReturn(configuration);
        lenient().when(subscription.getConfiguration()).thenReturn(SUBSCRIPTION_CONFIGURATION);

        cut = new WebhookEntrypointConnector(configuration);
    }

    @Test
    void shouldIdReturnWebhook() {
        assertThat(cut.id()).isEqualTo("webhook");
    }

    @Test
    void shouldSupportAsyncApi() {
        assertThat(cut.supportedApi()).isEqualTo(ApiType.ASYNC);
    }

    @Test
    void shouldSupportSubscriptionListener() {
        assertThat(cut.supportedListenerType()).isEqualTo(ListenerType.SUBSCRIPTION);
    }

    @Test
    void shouldSupportSubscribeModeOnly() {
        assertThat(cut.supportedModes()).containsOnly(ConnectorMode.SUBSCRIBE);
    }

    @Test
    void shouldMatchesCriteriaReturnValidCount() {
        assertThat(cut.matchCriteriaCount()).isZero();
    }

    @Test
    void shouldMatchesWithValidContext() {
        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION_TYPE)).thenReturn("webhook");

        boolean matches = cut.matches(ctx);

        assertThat(matches).isTrue();
    }

    @Test
    void shouldNotMatchesWithNoSubscriptionType() {
        boolean matches = cut.matches(ctx);

        assertThat(matches).isFalse();
    }

    @Test
    void shouldHandlingRequestInErrorWithInvalidCallbackUrl() {
        when(configuration.getCallbackUrl()).thenReturn("unknown_url");

        final TestObserver<Void> obs = cut.handleRequest(ctx).test();
        obs.assertError(IllegalArgumentException.class);
    }

    @Test
    void shouldHandleRequestWithValidCallback() {
        when(configuration.getCallbackUrl()).thenReturn("http://callbackserver/endpoint");

        final TestObserver<Void> obs = cut.handleRequest(ctx).test();
        obs.assertNoValues();

        verify(ctx).setInternalAttribute(INTERNAL_ATTR_WEBHOOK_REQUEST_URI, "/endpoint");
        verify(ctx).setInternalAttribute(eq(INTERNAL_ATTR_WEBHOOK_HTTP_CLIENT), any(HttpClient.class));
    }

    @Test
    void shouldNotCallWebHookWhenNoMessage(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(post("/callback").willReturn(ok()));

        when(configuration.getCallbackUrl()).thenReturn("http://localhost:" + wmRuntimeInfo.getHttpPort() + "/callback");
        doNothing().when(ctx).setInternalAttribute(INTERNAL_ATTR_WEBHOOK_REQUEST_URI, "/callback");
        doNothing().when(ctx).setInternalAttribute(eq(INTERNAL_ATTR_WEBHOOK_HTTP_CLIENT), httpClientCaptor.capture());
        when(ctx.getInternalAttribute(INTERNAL_ATTR_WEBHOOK_REQUEST_URI)).thenReturn("/callback");
        doAnswer(i -> httpClientCaptor.getValue()).when(ctx).getInternalAttribute(INTERNAL_ATTR_WEBHOOK_HTTP_CLIENT);

        cut.handleRequest(ctx).test().assertComplete();

        when(response.messages()).thenReturn(Flowable.empty());
        when(ctx.response()).thenReturn(response);

        final TestObserver<Void> obs = cut.handleResponse(ctx).test();
        obs.awaitTerminalEvent(10, TimeUnit.SECONDS);

        verify(response).chunks(chunksCaptor.capture());

        final TestSubscriber<Buffer> chunksObs = chunksCaptor.getValue().test();

        chunksObs.awaitTerminalEvent();

        wmRuntimeInfo.getWireMock().verifyThat(0, postRequestedFor(urlPathEqualTo("/callback")));
    }

    @Test
    void shouldCallWebhookWhenMessages(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(post("/callback").willReturn(ok()));

        when(configuration.getCallbackUrl()).thenReturn("http://localhost:" + wmRuntimeInfo.getHttpPort() + "/callback");
        doNothing().when(ctx).setInternalAttribute(INTERNAL_ATTR_WEBHOOK_REQUEST_URI, "/callback");
        doNothing().when(ctx).setInternalAttribute(eq(INTERNAL_ATTR_WEBHOOK_HTTP_CLIENT), httpClientCaptor.capture());
        when(ctx.getInternalAttribute(INTERNAL_ATTR_WEBHOOK_REQUEST_URI)).thenReturn("/callback");
        doAnswer(i -> httpClientCaptor.getValue()).when(ctx).getInternalAttribute(INTERNAL_ATTR_WEBHOOK_HTTP_CLIENT);

        // Prepare the client
        cut.handleRequest(ctx).test().assertComplete();

        // Prepare response messages
        final DefaultMessage message1 = DefaultMessage
            .builder()
            .content(Buffer.buffer("message1"))
            .headers(HttpHeaders.create().set("my-header", "my-value"))
            .build();

        final DefaultMessage message2 = DefaultMessage
            .builder()
            .content(Buffer.buffer("message2"))
            .headers(HttpHeaders.create().set("my-header", "my-value"))
            .build();

        final DefaultMessage messageNoHeader = DefaultMessage.builder().content(Buffer.buffer("message3")).build();
        final DefaultMessage messageNoPayload = DefaultMessage.builder().build();
        final Flowable<Message> messages = Flowable.just(message1, message2, messageNoHeader, messageNoPayload);

        when(response.messages()).thenReturn(messages);
        when(ctx.response()).thenReturn(response);

        final TestObserver<Void> obs = cut.handleResponse(ctx).test();
        obs.awaitTerminalEvent(10, TimeUnit.SECONDS);

        verify(response).chunks(chunksCaptor.capture());

        final TestSubscriber<Buffer> chunksObs = chunksCaptor.getValue().test();

        chunksObs.awaitTerminalEvent();

        wmRuntimeInfo.getWireMock().verifyThat(4, postRequestedFor(urlPathEqualTo("/callback")));
    }
}
