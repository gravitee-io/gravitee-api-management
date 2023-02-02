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
import static io.gravitee.gateway.jupiter.api.context.InternalContextAttributes.ATTR_INTERNAL_EXECUTION_FAILURE;
import static io.gravitee.plugin.entrypoint.webhook.WebhookEntrypointConnector.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.v4.ssl.SslOptions;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.service.Subscription;
import io.gravitee.gateway.jupiter.api.ApiType;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.ListenerType;
import io.gravitee.gateway.jupiter.api.connector.ConnectorHelper;
import io.gravitee.gateway.jupiter.api.context.ExecutionContext;
import io.gravitee.gateway.jupiter.api.context.InternalContextAttributes;
import io.gravitee.gateway.jupiter.api.context.Request;
import io.gravitee.gateway.jupiter.api.context.Response;
import io.gravitee.gateway.jupiter.api.exception.PluginConfigurationException;
import io.gravitee.gateway.jupiter.api.message.DefaultMessage;
import io.gravitee.gateway.jupiter.api.message.Message;
import io.gravitee.gateway.jupiter.api.qos.Qos;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.container.spring.SpringEnvironmentConfiguration;
import io.gravitee.plugin.entrypoint.webhook.configuration.WebhookEntrypointConnectorConfiguration;
import io.gravitee.plugin.entrypoint.webhook.configuration.WebhookEntrypointConnectorSubscriptionConfiguration;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.impl.HttpClientImpl;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpClient;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.StandardEnvironment;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@WireMockTest
class WebhookEntrypointConnectorTest {

    protected static final String SUBSCRIPTION_CONFIGURATION = "subscription configuration";
    protected static final String MOCK_ERROR = "Mock error";
    public static final int AWAIT_SECONDS = 60;
    private final Vertx vertx = Vertx.vertx();
    private final Configuration nodeConfiguration = new SpringEnvironmentConfiguration(new StandardEnvironment());

    @Mock
    private ExecutionContext ctx;

    @Mock
    private Request mockMessageRequest;

    @Mock
    private Response response;

    @Mock
    private WebhookEntrypointConnectorConfiguration configuration;

    @Mock
    private WebhookEntrypointConnectorSubscriptionConfiguration subscriptionConfiguration;

    @Mock
    private ConnectorHelper connectorHelper;

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
        lenient().when(response.end(ctx)).thenReturn(Completable.complete());
        lenient().when(ctx.getComponent(io.vertx.rxjava3.core.Vertx.class)).thenReturn(vertx);
        lenient().when(ctx.getComponent(Configuration.class)).thenReturn(nodeConfiguration);
        lenient().when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_SUBSCRIPTION)).thenReturn(subscription);
        lenient().when(ctx.getComponent(ConnectorHelper.class)).thenReturn(connectorHelper);
        lenient()
            .when(
                connectorHelper.readConfiguration(
                    eq(WebhookEntrypointConnectorSubscriptionConfiguration.class),
                    eq(SUBSCRIPTION_CONFIGURATION)
                )
            )
            .thenReturn(subscriptionConfiguration);
        lenient().when(subscription.getConfiguration()).thenReturn(SUBSCRIPTION_CONFIGURATION);
        lenient().when(subscriptionConfiguration.getCallbackUrl()).thenReturn("http://localhost:8080/callback");

        cut = new WebhookEntrypointConnector(connectorHelper, Qos.NONE, configuration);
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
        when(subscriptionConfiguration.getCallbackUrl()).thenReturn("unknown_url");

        final TestObserver<Void> obs = cut.handleRequest(ctx).test();
        obs.assertError(IllegalArgumentException.class);
    }

    @Test
    void shouldHandleRequestWithValidCallback() {
        when(subscriptionConfiguration.getCallbackUrl()).thenReturn("http://callbackserver/endpoint");

        final TestObserver<Void> obs = cut.handleRequest(ctx).test();
        obs.assertNoValues();

        verify(ctx).setInternalAttribute(eq(INTERNAL_ATTR_WEBHOOK_HTTP_CLIENT), any(HttpClient.class));
    }

    @Test
    void shouldNotCallWebHookWhenNoMessage(WireMockRuntimeInfo wmRuntimeInfo) throws InterruptedException {
        stubFor(post("/callback").willReturn(ok()));

        when(subscriptionConfiguration.getCallbackUrl()).thenReturn("http://localhost:" + wmRuntimeInfo.getHttpPort() + "/callback");
        doNothing().when(ctx).setInternalAttribute(eq(INTERNAL_ATTR_WEBHOOK_HTTP_CLIENT), httpClientCaptor.capture());
        doNothing().when(ctx).setInternalAttribute(eq(INTERNAL_ATTR_WEBHOOK_SUBSCRIPTION_CONFIG), any());
        doAnswer(i -> httpClientCaptor.getValue()).when(ctx).getInternalAttribute(INTERNAL_ATTR_WEBHOOK_HTTP_CLIENT);

        cut.handleRequest(ctx).test().assertComplete();

        when(response.messages()).thenReturn(Flowable.empty());
        when(ctx.response()).thenReturn(response);

        final TestObserver<Void> obs = cut.handleResponse(ctx).test();
        obs.await(AWAIT_SECONDS, TimeUnit.SECONDS);

        verify(response).chunks(chunksCaptor.capture());

        final TestSubscriber<Buffer> chunksObs = chunksCaptor.getValue().test();

        chunksObs.await();

        wmRuntimeInfo.getWireMock().verifyThat(0, postRequestedFor(urlPathEqualTo("/callback")));
    }

    @Test
    void shouldCallWebhookWhenMessages(WireMockRuntimeInfo wmRuntimeInfo) throws InterruptedException {
        stubFor(post("/callback").willReturn(ok()));

        when(subscriptionConfiguration.getCallbackUrl()).thenReturn("http://localhost:" + wmRuntimeInfo.getHttpPort() + "/callback");
        doNothing().when(ctx).setInternalAttribute(eq(INTERNAL_ATTR_WEBHOOK_HTTP_CLIENT), httpClientCaptor.capture());
        doNothing().when(ctx).setInternalAttribute(eq(INTERNAL_ATTR_WEBHOOK_SUBSCRIPTION_CONFIG), any());
        when(ctx.getInternalAttribute(INTERNAL_ATTR_WEBHOOK_SUBSCRIPTION_CONFIG)).thenReturn(subscriptionConfiguration);
        doAnswer(i -> httpClientCaptor.getValue()).when(ctx).getInternalAttribute(INTERNAL_ATTR_WEBHOOK_HTTP_CLIENT);

        // Prepare the client
        cut.handleRequest(ctx).test().assertComplete();

        // Prepare response messages
        final DefaultMessage message1 = spy(
            DefaultMessage.builder().content(Buffer.buffer("message1")).headers(HttpHeaders.create().set("my-header", "my-value")).build()
        );

        final DefaultMessage message2 = spy(
            DefaultMessage.builder().content(Buffer.buffer("message2")).headers(HttpHeaders.create().set("my-header", "my-value")).build()
        );

        final DefaultMessage messageNoHeader = spy(DefaultMessage.builder().content(Buffer.buffer("message3")).build());
        final DefaultMessage messageNoPayload = spy(DefaultMessage.builder().build());
        final Flowable<Message> messages = Flowable.just(message1, message2, messageNoHeader, messageNoPayload);

        when(response.messages()).thenReturn(messages);
        when(ctx.response()).thenReturn(response);

        final TestObserver<Void> obs = cut.handleResponse(ctx).test();
        obs.await(AWAIT_SECONDS, TimeUnit.SECONDS);

        verify(response).chunks(chunksCaptor.capture());

        final TestSubscriber<Buffer> chunksObs = chunksCaptor.getValue().test();

        chunksObs.await();

        wmRuntimeInfo.getWireMock().verifyThat(4, postRequestedFor(urlPathEqualTo("/callback")));
        verify(message1).ack();
        verify(message2).ack();
        verify(messageNoHeader).ack();
        verify(messageNoPayload).ack();
    }

    @Test
    void shouldStopSendingMessagesToWebhookWhenStopping(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
        stubFor(post("/callback").willReturn(ok()));

        when(subscriptionConfiguration.getCallbackUrl()).thenReturn("http://localhost:" + wmRuntimeInfo.getHttpPort() + "/callback");
        doNothing().when(ctx).setInternalAttribute(eq(INTERNAL_ATTR_WEBHOOK_HTTP_CLIENT), httpClientCaptor.capture());
        doNothing().when(ctx).setInternalAttribute(eq(INTERNAL_ATTR_WEBHOOK_SUBSCRIPTION_CONFIG), any());
        when(ctx.getInternalAttribute(INTERNAL_ATTR_WEBHOOK_SUBSCRIPTION_CONFIG)).thenReturn(subscriptionConfiguration);
        doAnswer(i -> httpClientCaptor.getValue()).when(ctx).getInternalAttribute(INTERNAL_ATTR_WEBHOOK_HTTP_CLIENT);

        // Prepare the client
        cut.handleRequest(ctx).test().assertComplete();

        final TestScheduler testScheduler = new TestScheduler();

        // Prepare response messages
        final DefaultMessage message = DefaultMessage
            .builder()
            .content(Buffer.buffer("message"))
            .headers(HttpHeaders.create().set("my-header", "my-value"))
            .build();

        final Flowable<Message> messages = Flowable
            .just(message, message, message)
            .zipWith(Flowable.interval(1000, TimeUnit.MILLISECONDS, testScheduler), (m, aLong) -> m);

        when(response.messages()).thenReturn(messages);
        when(ctx.response()).thenReturn(response);

        final TestObserver<Void> obs = cut.handleResponse(ctx).test();
        obs.assertComplete();

        verify(response).chunks(chunksCaptor.capture());

        final TestSubscriber<Buffer> chunksObs = chunksCaptor.getValue().test();

        chunksObs.assertNotComplete();

        testScheduler.advanceTimeBy(2000, TimeUnit.MILLISECONDS);
        testScheduler.triggerActions();

        cut.preStop();

        chunksObs.assertComplete();
        wmRuntimeInfo.getWireMock().verifyThat(lessThanOrExactly(2), postRequestedFor(urlPathEqualTo("/callback")));
    }

    @Test
    void shouldCallWebhookAndHandleHeadersWithPrecedence(WireMockRuntimeInfo wmRuntimeInfo) throws InterruptedException {
        stubFor(post("/callback").willReturn(ok()));

        when(subscriptionConfiguration.getCallbackUrl()).thenReturn("http://localhost:" + wmRuntimeInfo.getHttpPort() + "/callback");
        doNothing().when(ctx).setInternalAttribute(eq(INTERNAL_ATTR_WEBHOOK_HTTP_CLIENT), httpClientCaptor.capture());
        doNothing().when(ctx).setInternalAttribute(eq(INTERNAL_ATTR_WEBHOOK_SUBSCRIPTION_CONFIG), any());
        when(ctx.getInternalAttribute(INTERNAL_ATTR_WEBHOOK_SUBSCRIPTION_CONFIG)).thenReturn(subscriptionConfiguration);
        doAnswer(i -> httpClientCaptor.getValue()).when(ctx).getInternalAttribute(INTERNAL_ATTR_WEBHOOK_HTTP_CLIENT);

        // Prepare the client
        cut.handleRequest(ctx).test().assertComplete();

        // Prepare response messages : 1 message with 2 headers : header1 and header2
        final DefaultMessage message = spy(
            DefaultMessage
                .builder()
                .content(Buffer.buffer("message1"))
                .headers(HttpHeaders.create().set("my-header1", "XXX").set("my-header2", "YYY"))
                .build()
        );

        when(response.messages()).thenReturn(Flowable.just(message));

        // Prepare response headers : header2 and header3
        when(response.headers()).thenReturn(HttpHeaders.create().set("my-header2", "AAA").set("my-header3", "ZZZ"));
        when(ctx.response()).thenReturn(response);

        final TestObserver<Void> obs = cut.handleResponse(ctx).test();
        obs.await(AWAIT_SECONDS, TimeUnit.SECONDS);

        verify(response).chunks(chunksCaptor.capture());

        final TestSubscriber<Buffer> chunksObs = chunksCaptor.getValue().test();

        chunksObs.await();

        wmRuntimeInfo
            .getWireMock()
            .verifyThat(
                1,
                postRequestedFor(urlPathEqualTo("/callback"))
                    .withHeader("my-header1", equalTo("XXX"))
                    .withHeader("my-header2", equalTo("AAA"))
                    .withHeader("my-header3", equalTo("ZZZ"))
            );
        verify(message).ack();
    }

    @Test
    void shouldErrorWhenMessageFlaggedInErrorIsReceived() {
        final DefaultMessage message = spy(new DefaultMessage(MOCK_ERROR).error(true));
        when(response.messages()).thenReturn(Flowable.just(message));
        when(ctx.response()).thenReturn(response);

        final HttpClient httpClient = mock(HttpClient.class);
        when(ctx.getInternalAttribute(INTERNAL_ATTR_WEBHOOK_HTTP_CLIENT)).thenReturn(httpClient);
        when(ctx.getInternalAttribute(INTERNAL_ATTR_WEBHOOK_SUBSCRIPTION_CONFIG)).thenReturn(subscriptionConfiguration);
        when(ctx.interruptMessageWith(any())).thenReturn(Maybe.error(new Exception(MOCK_ERROR)));

        final TestObserver<Void> obs = cut.handleResponse(ctx).test();
        obs.assertComplete();

        verify(response).chunks(chunksCaptor.capture());

        final TestSubscriber<Buffer> chunksObs = chunksCaptor.getValue().test();

        chunksObs.assertError(Throwable.class);
        verify(ctx)
            .interruptMessageWith(
                argThat(
                    executionFailure ->
                        executionFailure.statusCode() == HttpStatusCode.INTERNAL_SERVER_ERROR_500 &&
                        executionFailure.message().equals(MOCK_ERROR)
                )
            );
        verify(message, never()).ack();
        verify(httpClient).close();
    }

    @Test
    void shouldErrorWithoutAckWhenCallbackReturns5xx(WireMockRuntimeInfo wmRuntimeInfo) throws InterruptedException {
        stubFor(post("/callback").willReturn(serverError()));

        when(subscriptionConfiguration.getCallbackUrl()).thenReturn("http://localhost:" + wmRuntimeInfo.getHttpPort() + "/callback");
        doNothing().when(ctx).setInternalAttribute(eq(INTERNAL_ATTR_WEBHOOK_HTTP_CLIENT), httpClientCaptor.capture());
        doNothing().when(ctx).setInternalAttribute(eq(INTERNAL_ATTR_WEBHOOK_SUBSCRIPTION_CONFIG), any());
        when(ctx.getInternalAttribute(INTERNAL_ATTR_WEBHOOK_SUBSCRIPTION_CONFIG)).thenReturn(subscriptionConfiguration);
        doAnswer(i -> httpClientCaptor.getValue()).when(ctx).getInternalAttribute(INTERNAL_ATTR_WEBHOOK_HTTP_CLIENT);
        when(ctx.interruptMessageWith(any())).thenReturn(Maybe.error(new Exception(MOCK_ERROR)));

        // Prepare the client
        cut.handleRequest(ctx).test().assertComplete();

        // Prepare response messages
        final DefaultMessage message = spy(
            DefaultMessage.builder().content(Buffer.buffer("message1")).headers(HttpHeaders.create().set("my-header", "my-value")).build()
        );

        final Flowable<Message> messages = Flowable.just(message);

        when(response.messages()).thenReturn(messages);
        when(ctx.response()).thenReturn(response);

        final TestObserver<Void> obs = cut.handleResponse(ctx).test();
        obs.await(AWAIT_SECONDS, TimeUnit.SECONDS);

        verify(response).chunks(chunksCaptor.capture());

        final TestSubscriber<Buffer> chunksObs = chunksCaptor.getValue().test();

        chunksObs.await();
        chunksObs.assertError(Throwable.class);
        wmRuntimeInfo.getWireMock().verifyThat(1, postRequestedFor(urlPathEqualTo("/callback")));

        verify(ctx)
            .interruptMessageWith(
                argThat(
                    executionFailure ->
                        executionFailure.statusCode() == HttpStatusCode.INTERNAL_SERVER_ERROR_500 &&
                        executionFailure.key().equals(WEBHOOK_UNREACHABLE_KEY) &&
                        executionFailure.message().equals(WEBHOOK_UNREACHABLE_MESSAGE)
                )
            );

        verify(message, never()).ack();
    }

    @Test
    void shouldFlagMessageInErrorAndErrorWithoutAckWhenCallbackReturns4xx(WireMockRuntimeInfo wmRuntimeInfo) throws InterruptedException {
        stubFor(post("/callback").willReturn(badRequest()));

        when(subscriptionConfiguration.getCallbackUrl()).thenReturn("http://localhost:" + wmRuntimeInfo.getHttpPort() + "/callback");
        doNothing().when(ctx).setInternalAttribute(eq(INTERNAL_ATTR_WEBHOOK_HTTP_CLIENT), httpClientCaptor.capture());
        doNothing().when(ctx).setInternalAttribute(eq(INTERNAL_ATTR_WEBHOOK_SUBSCRIPTION_CONFIG), any());
        when(ctx.getInternalAttribute(INTERNAL_ATTR_WEBHOOK_SUBSCRIPTION_CONFIG)).thenReturn(subscriptionConfiguration);
        doAnswer(i -> httpClientCaptor.getValue()).when(ctx).getInternalAttribute(INTERNAL_ATTR_WEBHOOK_HTTP_CLIENT);
        when(ctx.interruptMessageWith(any())).thenReturn(Maybe.error(new Exception(MOCK_ERROR)));

        // Prepare the client
        cut.handleRequest(ctx).test().assertComplete();

        // Prepare response messages
        final DefaultMessage message = spy(
            DefaultMessage.builder().content(Buffer.buffer("message1")).headers(HttpHeaders.create().set("my-header", "my-value")).build()
        );

        final Flowable<Message> messages = Flowable.just(message);

        when(response.messages()).thenReturn(messages);
        when(ctx.response()).thenReturn(response);

        final TestObserver<Void> obs = cut.handleResponse(ctx).test();
        obs.await(AWAIT_SECONDS, TimeUnit.SECONDS);

        verify(response).chunks(chunksCaptor.capture());

        final TestSubscriber<Buffer> chunksObs = chunksCaptor.getValue().test();

        chunksObs.await();
        chunksObs.assertError(Throwable.class);
        wmRuntimeInfo.getWireMock().verifyThat(1, postRequestedFor(urlPathEqualTo("/callback")));

        verify(ctx)
            .interruptMessageWith(
                argThat(
                    executionFailure ->
                        executionFailure.statusCode() == HttpStatusCode.INTERNAL_SERVER_ERROR_500 &&
                        executionFailure.key().equals(MESSAGE_PROCESSING_FAILED_KEY) &&
                        executionFailure.message().equals(MESSAGE_PROCESSING_FAILED_MESSAGE)
                )
            );
        verify(message).error(true);
        verify(message, never()).ack();
    }

    @Test
    void shouldErrorExistingExecutionFailureWithoutAckWhenCallbackReturns4xx(WireMockRuntimeInfo wmRuntimeInfo)
        throws InterruptedException {
        stubFor(post("/callback").willReturn(badRequest()));

        when(subscriptionConfiguration.getCallbackUrl()).thenReturn("http://localhost:" + wmRuntimeInfo.getHttpPort() + "/callback");
        doNothing().when(ctx).setInternalAttribute(eq(INTERNAL_ATTR_WEBHOOK_HTTP_CLIENT), httpClientCaptor.capture());
        doNothing().when(ctx).setInternalAttribute(eq(INTERNAL_ATTR_WEBHOOK_SUBSCRIPTION_CONFIG), any());
        when(ctx.getInternalAttribute(INTERNAL_ATTR_WEBHOOK_SUBSCRIPTION_CONFIG)).thenReturn(subscriptionConfiguration);
        doAnswer(i -> httpClientCaptor.getValue()).when(ctx).getInternalAttribute(INTERNAL_ATTR_WEBHOOK_HTTP_CLIENT);
        when(ctx.interruptMessageWith(any())).thenReturn(Maybe.error(new Exception(MOCK_ERROR)));

        final ExecutionFailure failure = new ExecutionFailure(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
        when(ctx.getInternalAttribute(ATTR_INTERNAL_EXECUTION_FAILURE)).thenReturn(failure);

        // Prepare the client
        cut.handleRequest(ctx).test().assertComplete();

        // Prepare response messages
        final DefaultMessage message = spy(
            DefaultMessage.builder().content(Buffer.buffer("message1")).headers(HttpHeaders.create().set("my-header", "my-value")).build()
        );

        final Flowable<Message> messages = Flowable.just(message);

        when(response.messages()).thenReturn(messages);
        when(ctx.response()).thenReturn(response);

        final TestObserver<Void> obs = cut.handleResponse(ctx).test();
        obs.await(AWAIT_SECONDS, TimeUnit.SECONDS);

        verify(response).chunks(chunksCaptor.capture());

        final TestSubscriber<Buffer> chunksObs = chunksCaptor.getValue().test();

        chunksObs.await();
        chunksObs.assertError(Throwable.class);
        wmRuntimeInfo.getWireMock().verifyThat(1, postRequestedFor(urlPathEqualTo("/callback")));

        verify(ctx).interruptMessageWith(failure);
        verify(message).error(true);
        verify(message, never()).ack();
    }

    @Test
    public void shouldBuildHttpClientWithSsl() {
        when(subscriptionConfiguration.getCallbackUrl()).thenReturn("https://localhost:473/test");

        SslOptions sslOptions = new SslOptions();
        sslOptions.setTrustAll(true);
        sslOptions.setHostnameVerifier(false);
        when(subscriptionConfiguration.getSsl()).thenReturn(sslOptions);

        cut.computeSubscriptionContextAttributes(ctx, subscriptionConfiguration);

        verify(ctx).setInternalAttribute(INTERNAL_ATTR_WEBHOOK_SUBSCRIPTION_CONFIG, subscriptionConfiguration);
        verify(ctx).setInternalAttribute(eq(INTERNAL_ATTR_WEBHOOK_HTTP_CLIENT), httpClientCaptor.capture());

        HttpClientOptions options = ((HttpClientImpl) httpClientCaptor.getValue().getDelegate()).options();
        assertThat(options.isSsl()).isTrue();
        assertThat(options.getDefaultHost()).isEqualTo("localhost");
        assertThat(options.getDefaultPort()).isEqualTo(473);
        assertThat(options.isVerifyHost()).isFalse();
        assertThat(options.isTrustAll()).isTrue();
    }

    @Test
    public void shouldBuildHttpClientWithoutSsl() {
        when(subscriptionConfiguration.getCallbackUrl()).thenReturn("http://localhost:473/test");

        cut.computeSubscriptionContextAttributes(ctx, subscriptionConfiguration);

        verify(ctx).setInternalAttribute(INTERNAL_ATTR_WEBHOOK_SUBSCRIPTION_CONFIG, subscriptionConfiguration);
        verify(ctx).setInternalAttribute(eq(INTERNAL_ATTR_WEBHOOK_HTTP_CLIENT), httpClientCaptor.capture());

        HttpClientOptions options = ((HttpClientImpl) httpClientCaptor.getValue().getDelegate()).options();
        assertThat(options.isSsl()).isFalse();
        assertThat(options.getDefaultHost()).isEqualTo("localhost");
        assertThat(options.getDefaultPort()).isEqualTo(473);
    }
}
