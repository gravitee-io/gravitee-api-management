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
package io.gravitee.plugin.entrypoint.webhook;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.jupiter.api.ListenerType;
import io.gravitee.gateway.jupiter.api.context.*;
import io.gravitee.gateway.jupiter.api.message.DefaultMessage;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.plugin.entrypoint.webhook.configuration.WebhookEntrypointConnectorConfiguration;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.vertx.core.Vertx;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@WireMockTest
class WebhookEntrypointConnectorTest {

    @Mock
    private ExecutionContext ctx;

    @Mock
    private Request mockMessageRequest;

    @Mock
    private Response mockMessageResponse;

    @Mock
    private WebhookEntrypointConnectorConfiguration webhookEntrypointConnectorConfiguration;

    private final Vertx vertx = Vertx.vertx();

    private WebhookEntrypointConnector webhookEntrypointConnector;

    @BeforeEach
    void beforeEach() {
        lenient().when(ctx.request()).thenReturn(mockMessageRequest);
        lenient().when(ctx.response()).thenReturn(mockMessageResponse);
        lenient().when(mockMessageResponse.end()).thenReturn(Completable.complete());
        webhookEntrypointConnector = new WebhookEntrypointConnector(webhookEntrypointConnectorConfiguration);
    }

    @Test
    void shouldMCheckConnector() {
        assertThat(webhookEntrypointConnector.supportedApi()).isEqualTo(WebhookEntrypointConnector.SUPPORTED_API);
        assertThat(webhookEntrypointConnector.supportedListenerType()).isEqualTo(ListenerType.SUBSCRIPTION);
        assertThat(webhookEntrypointConnector.supportedModes()).isEqualTo(WebhookEntrypointConnector.SUPPORTED_MODES);
    }

    @Test
    void shouldMatchesCriteriaReturnValidCount() {
        assertThat(webhookEntrypointConnector.matchCriteriaCount()).isEqualTo(0);
    }

    @Test
    void shouldMatchesWithValidContext() {
        when(ctx.getInternalAttribute(ContextAttributes.ATTR_SUBSCRIPTION_TYPE)).thenReturn("webhook");

        boolean matches = webhookEntrypointConnector.matches(ctx);

        assertThat(matches).isTrue();
    }

    @Test
    void shouldNotMatchesWithNoSubscriptionType() {
        boolean matches = webhookEntrypointConnector.matches(ctx);

        assertThat(matches).isFalse();
    }

    @Test
    void shouldNotCompleteInvalidCallback() {
        when(webhookEntrypointConnectorConfiguration.getCallbackUrl()).thenReturn("unknown_url");

        webhookEntrypointConnector.handleRequest(ctx).test().assertError(IllegalStateException.class);

        verify(ctx, times(1)).getComponent(Vertx.class);
    }

    @Test
    void shouldCompleteHandleRequestWithValidCallback() {
        when(webhookEntrypointConnectorConfiguration.getCallbackUrl()).thenReturn("http://callbackserver/endpoint");
        when(ctx.getComponent(Vertx.class)).thenReturn(vertx);

        webhookEntrypointConnector.handleRequest(ctx).test().assertComplete();

        verify(ctx, times(1)).getComponent(Vertx.class);
    }

    @Test
    void shouldCompleteAndEndWhenNoMessage(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(post("/callback").willReturn(ok()));

        when(webhookEntrypointConnectorConfiguration.getCallbackUrl())
            .thenReturn("http://localhost:" + wmRuntimeInfo.getHttpPort() + "/callback");
        when(ctx.getComponent(Vertx.class)).thenReturn(vertx);

        // Prepare the client
        webhookEntrypointConnector.handleRequest(ctx).test().assertComplete();

        // Process response messages
        Flowable<Message> empty = Flowable.empty();
        when(mockMessageResponse.messages()).thenReturn(empty);
        when(ctx.response()).thenReturn(mockMessageResponse);
        webhookEntrypointConnector.handleResponse(ctx).test().awaitTerminalEvent(10, TimeUnit.SECONDS);

        wmRuntimeInfo.getWireMock().verifyThat(0, postRequestedFor(urlPathEqualTo("/callback")));
    }

    @Test
    void shouldCompleteAndEndWhenMessages(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(post("/callback").willReturn(ok()));

        when(webhookEntrypointConnectorConfiguration.getCallbackUrl())
            .thenReturn("http://localhost:" + wmRuntimeInfo.getHttpPort() + "/callback");
        when(ctx.getComponent(Vertx.class)).thenReturn(vertx);

        // Prepare the client
        webhookEntrypointConnector.handleRequest(ctx).test().assertComplete();

        // Prepare response messages
        DefaultMessage message1 = DefaultMessage
            .builder()
            .content(Buffer.buffer("message1"))
            .headers(HttpHeaders.create().set("my-header", "my-value"))
            .build();

        DefaultMessage message2 = DefaultMessage
            .builder()
            .content(Buffer.buffer("message2"))
            .headers(HttpHeaders.create().set("my-header", "my-value"))
            .build();

        DefaultMessage messageNoHeader = DefaultMessage.builder().content(Buffer.buffer("message3")).build();

        DefaultMessage messageNoPayload = DefaultMessage.builder().build();

        Flowable<Message> messages = Flowable.just(message1, message2, messageNoHeader, messageNoPayload);

        when(mockMessageResponse.messages()).thenReturn(messages);
        when(ctx.response()).thenReturn(mockMessageResponse);

        // Process response messages
        webhookEntrypointConnector.handleResponse(ctx).test().awaitTerminalEvent(10, TimeUnit.SECONDS);

        wmRuntimeInfo.getWireMock().verifyThat(4, postRequestedFor(urlPathEqualTo("/callback")));
    }
    /*
    @Test
    void shouldWriteSseEventAndCompleteAndEndWhenResponseMessagesComplete() {
        Flowable<Message> messages = Flowable.just(new DummyMessage("1"), new DummyMessage("2"), new DummyMessage("3"));
        when(mockMessageResponse.messages()).thenReturn(messages);
        HttpHeaders httpHeaders = HttpHeaders.create();
        when(mockMessageResponse.headers()).thenReturn(httpHeaders);
        when(mockMessageResponse.write(any())).thenReturn(Completable.complete());
        when(mockMessageResponse.end()).thenReturn(Completable.complete());
        when(mockMessageExecutionContext.response()).thenReturn(mockMessageResponse);
        boolean b = webhookEntrypointConnector.handleResponse(mockMessageExecutionContext).test().awaitTerminalEvent(10, TimeUnit.SECONDS);
        assertThat(httpHeaders.contains(HttpHeaderNames.CONTENT_TYPE)).isTrue();
        assertThat(httpHeaders.contains(HttpHeaderNames.CONNECTION)).isTrue();
        assertThat(httpHeaders.contains(HttpHeaderNames.CACHE_CONTROL)).isTrue();
        assertThat(httpHeaders.contains(HttpHeaderNames.CACHE_CONTROL)).isTrue();
        verify(mockMessageResponse, times(8)).write(any());
    }

    @Test
    void shouldWriteErrorSseEventAndCompleteAndEndWhenResponseMessagesFail() {
        Flowable<Message> messages = Flowable.error(new RuntimeException("error"));
        when(mockMessageResponse.messages()).thenReturn(messages);
        HttpHeaders httpHeaders = HttpHeaders.create();
        when(mockMessageResponse.headers()).thenReturn(httpHeaders);
        when(mockMessageResponse.end()).thenReturn(Completable.complete());
        when(mockMessageExecutionContext.response()).thenReturn(mockMessageResponse);
        boolean b = webhookEntrypointConnector.handleResponse(mockMessageExecutionContext).test().awaitTerminalEvent(10, TimeUnit.SECONDS);
        assertThat(httpHeaders.contains(HttpHeaderNames.CONTENT_TYPE)).isTrue();
        assertThat(httpHeaders.contains(HttpHeaderNames.CONNECTION)).isTrue();
        assertThat(httpHeaders.contains(HttpHeaderNames.CACHE_CONTROL)).isTrue();
        verify(mockMessageResponse, times(1)).end(any());
    }
 */
}
