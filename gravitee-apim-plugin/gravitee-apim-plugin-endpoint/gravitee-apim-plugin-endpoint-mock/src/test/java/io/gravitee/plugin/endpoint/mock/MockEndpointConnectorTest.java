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
package io.gravitee.plugin.endpoint.mock;

import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_ENTRYPOINT_CONNECTOR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpHeader;
import io.gravitee.gateway.reactive.api.ApiType;
import io.gravitee.gateway.reactive.api.ConnectorMode;
import io.gravitee.gateway.reactive.api.connector.entrypoint.EntrypointConnector;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.context.Request;
import io.gravitee.gateway.reactive.api.context.Response;
import io.gravitee.gateway.reactive.api.message.DefaultMessage;
import io.gravitee.gateway.reactive.api.message.Message;
import io.gravitee.plugin.endpoint.mock.configuration.MockEndpointConnectorConfiguration;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.assertj.core.data.MapEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

/**
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class MockEndpointConnectorTest {

    protected static final String MESSAGE_CONTENT = "test mock endpoint";
    private static final String MESSAGE_TO_LOG = "message to log";
    private MockEndpointConnectorConfiguration configuration;

    @Mock(name = "io.gravitee.plugin.endpoint.mock.MockEndpointConnector")
    Logger log;

    @InjectMocks
    private MockEndpointConnector cut;

    @Mock
    private ExecutionContext ctx;

    @Mock
    private Request request;

    @Mock
    private Response response;

    @Mock
    private EntrypointConnector entrypointConnector;

    @BeforeEach
    public void setup() {
        configuration = new MockEndpointConnectorConfiguration();
        configuration.setMessageInterval(100L);
        configuration.setMessageContent(MESSAGE_CONTENT);
        cut = new MockEndpointConnector(configuration);
        lenient().when(request.onMessage(any())).thenReturn(Completable.complete());

        lenient().when(ctx.request()).thenReturn(request);
        lenient().when(ctx.response()).thenReturn(response);
        lenient().when(entrypointConnector.supportedModes()).thenReturn(Set.of(ConnectorMode.SUBSCRIBE, ConnectorMode.PUBLISH));
        lenient().when(ctx.getInternalAttribute(ATTR_INTERNAL_ENTRYPOINT_CONNECTOR)).thenReturn(entrypointConnector);
    }

    @Test
    void shouldIdReturnMock() {
        assertThat(cut.id()).isEqualTo("mock");
    }

    @Test
    void shouldSupportAsyncApi() {
        assertThat(cut.supportedApi()).isEqualTo(ApiType.MESSAGE);
    }

    @Test
    void shouldSupportPublishAndSubscribeModes() {
        assertThat(cut.supportedModes()).containsOnly(ConnectorMode.PUBLISH, ConnectorMode.SUBSCRIBE);
    }

    @Test
    @DisplayName("Should receive messages")
    void shouldLogRequestMessagesFlow() {
        cut.connect(ctx).test().assertComplete();

        ArgumentCaptor<Function<Message, Maybe<Message>>> messagesCaptor = ArgumentCaptor.forClass(Function.class);

        verify(request).onMessage(messagesCaptor.capture());
        messagesCaptor.getValue().apply(DefaultMessage.builder().content(MESSAGE_TO_LOG).build()).test().assertComplete();
        verify(log).info("Received message: {}", MESSAGE_TO_LOG);
    }

    @Test
    @DisplayName("Should generate messages flow")
    void shouldGenerateMessagesFlow() {
        cut.connect(ctx).test().assertComplete();

        ArgumentCaptor<Flowable<Message>> messagesCaptor = ArgumentCaptor.forClass(Flowable.class);
        verify(response).messages(messagesCaptor.capture());
        final TestScheduler testScheduler = new TestScheduler();
        final TestSubscriber<Message> obs = messagesCaptor.getValue().test(3);

        testScheduler.triggerActions();
        obs
            .awaitCount(3)
            .assertValueCount(3)
            .assertValueAt(0, message -> message.content().toString().equals(MESSAGE_CONTENT) && message.id().equals("0"))
            .assertValueAt(1, message -> message.content().toString().equals(MESSAGE_CONTENT) && message.id().equals("1"))
            .assertValueAt(2, message -> message.content().toString().equals(MESSAGE_CONTENT) && message.id().equals("2"));
    }

    @Test
    @DisplayName("Should generate messages flow with a limited count of messages from the configuration")
    void shouldGenerateLimitedMessagesFlowFromConfiguration() throws InterruptedException {
        configuration.setMessageCount(5);

        cut.connect(ctx).test().assertComplete();

        ArgumentCaptor<Flowable<Message>> messagesCaptor = ArgumentCaptor.forClass(Flowable.class);
        verify(response).messages(messagesCaptor.capture());

        messagesCaptor.getValue().test().await().assertValueCount(5);
    }

    @Test
    @DisplayName("Should generate messages flow with a limited count of messages from the client request")
    void shouldGenerateLimitedMessagesFlowFromClientRequest() throws InterruptedException {
        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_MESSAGES_LIMIT_COUNT)).thenReturn(3);
        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_MESSAGES_LIMIT_DURATION_MS)).thenReturn(null);
        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_MESSAGES_RECOVERY_LAST_ID)).thenReturn(null);
        when(request.onMessage(any())).thenReturn(Completable.complete());

        cut.connect(ctx).test().assertComplete();

        ArgumentCaptor<Flowable<Message>> messagesCaptor = ArgumentCaptor.forClass(Flowable.class);
        verify(response).messages(messagesCaptor.capture());

        messagesCaptor.getValue().test().await().assertValueCount(3);
    }

    @ParameterizedTest(name = "configurationLimitCount: {0}, ctxLimitCount: {1}")
    @CsvSource({ "3,4", "4,2" })
    @DisplayName("Should generate messages flow with a limited count of messages from the client request and the configuration")
    void shouldGenerateLimitedMessagesFlowFromClientRequestAndConfiguration(int configurationLimitCount, int ctxLimitCount)
        throws InterruptedException {
        configuration.setMessageCount(configurationLimitCount);

        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_MESSAGES_LIMIT_COUNT)).thenReturn(ctxLimitCount);
        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_MESSAGES_LIMIT_DURATION_MS)).thenReturn(null);
        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_MESSAGES_RECOVERY_LAST_ID)).thenReturn(null);
        when(request.onMessage(any())).thenReturn(Completable.complete());

        cut.connect(ctx).test().assertComplete();

        ArgumentCaptor<Flowable<Message>> messagesCaptor = ArgumentCaptor.forClass(Flowable.class);
        verify(response).messages(messagesCaptor.capture());

        messagesCaptor.getValue().test().await().assertValueCount(Math.min(configurationLimitCount, ctxLimitCount));
    }

    @Test
    @DisplayName("Should generate messages flow during a specific amount of time requested by the client")
    void shouldGenerateLimitedInTimeMessagesFlowFromClientRequest() throws InterruptedException {
        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_MESSAGES_LIMIT_COUNT)).thenReturn(null);
        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_MESSAGES_LIMIT_DURATION_MS)).thenReturn(1_000L);
        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_MESSAGES_RECOVERY_LAST_ID)).thenReturn(null);
        when(request.onMessage(any())).thenReturn(Completable.complete());

        cut.connect(ctx).test().assertComplete();

        ArgumentCaptor<Flowable<Message>> messagesCaptor = ArgumentCaptor.forClass(Flowable.class);
        verify(response).messages(messagesCaptor.capture());

        messagesCaptor.getValue().test().awaitDone(1_500L, TimeUnit.MILLISECONDS).assertComplete();
    }

    @Test
    @DisplayName("Should generate messages flow from lastId")
    void shouldGenerateMessagesFlowFromLastId() throws InterruptedException {
        configuration.setMessageCount(4);
        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_MESSAGES_LIMIT_COUNT)).thenReturn(null);
        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_MESSAGES_LIMIT_DURATION_MS)).thenReturn(null);
        when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_MESSAGES_RECOVERY_LAST_ID)).thenReturn("1");
        when(request.onMessage(any())).thenReturn(Completable.complete());

        cut.connect(ctx).test().assertComplete();

        ArgumentCaptor<Flowable<Message>> messagesCaptor = ArgumentCaptor.forClass(Flowable.class);
        verify(response).messages(messagesCaptor.capture());

        TestSubscriber<Message> messages = messagesCaptor.getValue().test();
        messages.await();
        messages.assertValueCount(2);
        messages.assertValueAt(0, message -> message.id().equals("2"));
        messages.assertValueAt(1, message -> message.id().equals("3"));
    }

    @Test
    @DisplayName("Should generate message with metadata")
    void shouldGenerateMessageWithMetadata() throws InterruptedException {
        configuration.setMessageCount(1);
        configuration.setMetadata(List.of(new HttpHeader("mock-metadata1", "foo"), new HttpHeader("mock-metadata2", "bar")));

        cut.connect(ctx).test().assertComplete();

        ArgumentCaptor<Flowable<Message>> messagesCaptor = ArgumentCaptor.forClass(Flowable.class);
        verify(response).messages(messagesCaptor.capture());

        messagesCaptor
            .getValue()
            .test()
            .await()
            .assertValue(message -> {
                assertThat(message.metadata()).containsOnlyKeys("sourceTimestamp", "mock-metadata1", "mock-metadata2");
                assertThat(message.metadata()).containsAllEntriesOf(Map.of("mock-metadata1", "foo", "mock-metadata2", "bar"));
                assertThat(message.headers()).isEmpty();
                return true;
            });
    }

    @Test
    @DisplayName("Should generate message with headers")
    void shouldGenerateMessageWithHeaders() throws InterruptedException {
        configuration.setMessageCount(1);
        configuration.setHeaders(List.of(new HttpHeader("header1", "foo"), new HttpHeader("header2", "bar")));

        cut.connect(ctx).test().assertComplete();

        ArgumentCaptor<Flowable<Message>> messagesCaptor = ArgumentCaptor.forClass(Flowable.class);
        verify(response).messages(messagesCaptor.capture());

        messagesCaptor
            .getValue()
            .test()
            .await()
            .assertValue(message -> {
                assertThat(message.headers().toSingleValueMap())
                    .containsExactlyInAnyOrderEntriesOf(Map.of("header1", "foo", "header2", "bar"));
                assertThat(message.metadata()).hasEntrySatisfying("sourceTimestamp", value -> assertThat(value).isInstanceOf(Long.class));
                return true;
            });
    }

    @Test
    @DisplayName("Should generate message with headers and metadata")
    void shouldGenerateMessageWithHeadersAndMetadata() throws InterruptedException {
        configuration.setMessageCount(1);
        configuration.setMetadata(List.of(new HttpHeader("mock-metadata1", "foo"), new HttpHeader("mock-metadata2", "bar")));
        configuration.setHeaders(List.of(new HttpHeader("header1", "foo"), new HttpHeader("header2", "bar")));
        cut.connect(ctx).test().assertComplete();

        ArgumentCaptor<Flowable<Message>> messagesCaptor = ArgumentCaptor.forClass(Flowable.class);
        verify(response).messages(messagesCaptor.capture());

        messagesCaptor
            .getValue()
            .test()
            .await()
            .assertValue(message -> {
                assertThat(message.metadata()).containsOnlyKeys("sourceTimestamp", "mock-metadata1", "mock-metadata2");
                assertThat(message.metadata()).containsAllEntriesOf(Map.of("mock-metadata1", "foo", "mock-metadata2", "bar"));
                assertThat(message.headers().toSingleValueMap())
                    .containsExactlyInAnyOrderEntriesOf(Map.of("header1", "foo", "header2", "bar"));
                return true;
            });
    }
}
