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
package io.gravitee.plugin.entrypoint.websocket;

import static io.gravitee.plugin.entrypoint.websocket.WebSocketCloseStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.reactive.api.ApiType;
import io.gravitee.gateway.reactive.api.ConnectorMode;
import io.gravitee.gateway.reactive.api.ListenerType;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.Request;
import io.gravitee.gateway.reactive.api.context.Response;
import io.gravitee.gateway.reactive.api.message.DefaultMessage;
import io.gravitee.gateway.reactive.api.message.Message;
import io.gravitee.gateway.reactive.api.qos.Qos;
import io.gravitee.gateway.reactive.api.qos.QosCapability;
import io.gravitee.gateway.reactive.api.qos.QosRequirement;
import io.gravitee.gateway.reactive.api.ws.WebSocket;
import io.gravitee.plugin.entrypoint.websocket.configuration.WebSocketEntrypointConnectorConfiguration;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class WebSocketEntrypointConnectorTest {

    protected static final String MOCK_EXCEPTION = "Mock exception";

    @Mock
    private WebSocketEntrypointConnectorConfiguration configuration;

    @Mock
    private ExecutionContext ctx;

    @Mock
    private Request request;

    @Mock
    private Response response;

    @Mock
    private WebSocket webSocket;

    @Captor
    private ArgumentCaptor<Flowable<Buffer>> chunkCaptor;

    private WebSocketEntrypointConnector cut;

    @BeforeEach
    void init() {
        lenient().when(ctx.request()).thenReturn(request);
        lenient().when(ctx.response()).thenReturn(response);

        cut = new WebSocketEntrypointConnector(Qos.NONE, configuration);
    }

    @Test
    void shouldIdReturnWebsocket() {
        assertThat(cut.id()).isEqualTo("websocket");
    }

    @Test
    void shouldSupportAsyncApi() {
        assertThat(cut.supportedApi()).isEqualTo(ApiType.MESSAGE);
    }

    @Test
    void shouldSupportHttpListener() {
        assertThat(cut.supportedListenerType()).isEqualTo(ListenerType.HTTP);
    }

    @Test
    void shouldSupportPublishAndSubscribeModes() {
        assertThat(cut.supportedModes()).containsOnly(ConnectorMode.PUBLISH, ConnectorMode.SUBSCRIBE);
    }

    @Test
    void shouldReturnQosRequirement() {
        QosRequirement qosRequirement = cut.qosRequirement();
        assertThat(qosRequirement.getQos()).isEqualTo(Qos.NONE);
        assertThat(qosRequirement.getCapabilities()).isEmpty();
    }

    @Test
    void shouldReturnQosRequirementWithCapabilities() {
        WebSocketEntrypointConnector cut = new WebSocketEntrypointConnector(Qos.AUTO, configuration);
        QosRequirement qosRequirement = cut.qosRequirement();
        assertThat(qosRequirement.getQos()).isEqualTo(Qos.AUTO);
        assertThat(qosRequirement.getCapabilities()).containsOnly(QosCapability.AUTO_ACK);
    }

    @Test
    void shouldMatch4Criteria() {
        assertEquals(4, cut.matchCriteriaCount());
    }

    @Test
    void shouldMatch() {
        when(request.isWebSocket()).thenReturn(true);
        assertTrue(cut.matches(ctx));
    }

    @Test
    void shouldNotMatch() {
        when(request.isWebSocket()).thenReturn(false);
        assertFalse(cut.matches(ctx));
    }

    @Test
    void shouldHandleRequest() {
        final Buffer message1 = Buffer.buffer("message1");
        final Buffer message2 = Buffer.buffer("message2");
        final Buffer message3 = Buffer.buffer("message3");

        when(configuration.getPublisher()).thenReturn(new WebSocketEntrypointConnectorConfiguration.Publisher(true));
        when(request.webSocket()).thenReturn(webSocket);
        when(webSocket.upgrade()).thenReturn(Single.just(webSocket));
        when(webSocket.read()).thenReturn(Flowable.just(message1, message2, message3));

        final TestObserver<Void> obs = cut.handleRequest(ctx).test();

        obs.assertNoValues();

        final ArgumentCaptor<Flowable<Message>> messageCaptor = ArgumentCaptor.forClass(Flowable.class);
        verify(request).messages(messageCaptor.capture());

        final TestSubscriber<Message> messageObs = messageCaptor.getValue().test();

        messageObs.assertValueCount(3);
        messageObs.assertValueAt(0, message -> message.content().equals(message1));
        messageObs.assertValueAt(1, message -> message.content().equals(message2));
        messageObs.assertValueAt(2, message -> message.content().equals(message3));
        messageObs.assertComplete();
    }

    @Test
    void shouldHandleRequestWithEmptyWhenPublisherIsDisabled() {
        when(configuration.getPublisher()).thenReturn(new WebSocketEntrypointConnectorConfiguration.Publisher(false));
        when(request.webSocket()).thenReturn(webSocket);
        when(webSocket.upgrade()).thenReturn(Single.just(webSocket));

        final TestObserver<Void> obs = cut.handleRequest(ctx).test();

        obs.assertNoValues();

        final ArgumentCaptor<Flowable<Message>> messageCaptor = ArgumentCaptor.forClass(Flowable.class);
        verify(request).messages(messageCaptor.capture());

        final TestSubscriber<Message> messageObs = messageCaptor.getValue().test();

        messageObs.assertNoValues();
    }

    @Test
    void shouldHandleResponse() {
        final Message requestMessage1 = new DefaultMessage("Request message1");
        final Message requestMessage2 = new DefaultMessage("Request message2");
        final Message requestMessage3 = new DefaultMessage("Request message3");

        final Message responseMessage1 = new DefaultMessage("Response message1");
        final Message responseMessage2 = new DefaultMessage("Response message2");
        final Message responseMessage3 = new DefaultMessage("Response message3");

        when(configuration.getSubscriber()).thenReturn(new WebSocketEntrypointConnectorConfiguration.Subscriber(true));
        when(request.webSocket()).thenReturn(webSocket);
        when(request.messages()).thenReturn(Flowable.just(requestMessage1, requestMessage2, requestMessage3));
        when(response.messages()).thenReturn(Flowable.just(responseMessage1, responseMessage2, responseMessage3));
        when(webSocket.write(any(Buffer.class))).thenReturn(Completable.complete());
        when(webSocket.close(anyInt(), anyString())).thenReturn(Completable.complete());

        final TestObserver<Void> obs = cut.handleResponse(ctx).test();

        obs.assertNoValues();

        verify(response).chunks(chunkCaptor.capture());
        chunkCaptor.getValue().test().assertComplete();

        verify(webSocket).write(argThat(buffer -> buffer.toString().equals("Response message1")));
        verify(webSocket).write(argThat(buffer -> buffer.toString().equals("Response message2")));
        verify(webSocket).write(argThat(buffer -> buffer.toString().equals("Response message3")));
        verify(webSocket).close(NORMAL_CLOSURE.code(), NORMAL_CLOSURE.reasonText());
        verifyNoMoreInteractions(webSocket);
    }

    @Test
    void shouldHandleResponseWhenRequestMessageIsEmpty() {
        final Message responseMessage1 = new DefaultMessage("Response message1");
        final Message responseMessage2 = new DefaultMessage("Response message2");
        final Message responseMessage3 = new DefaultMessage("Response message3");

        when(configuration.getSubscriber()).thenReturn(new WebSocketEntrypointConnectorConfiguration.Subscriber(true));
        when(request.webSocket()).thenReturn(webSocket);
        when(request.messages()).thenReturn(Flowable.empty());
        when(response.messages()).thenReturn(Flowable.just(responseMessage1, responseMessage2, responseMessage3));
        when(webSocket.write(any(Buffer.class))).thenReturn(Completable.complete());
        when(webSocket.close(anyInt(), anyString())).thenReturn(Completable.complete());

        final TestObserver<Void> obs = cut.handleResponse(ctx).test();

        obs.assertNoValues();

        verify(response).chunks(chunkCaptor.capture());
        chunkCaptor.getValue().test().assertComplete();

        verify(webSocket, times(3)).write(any(Buffer.class));
        verify(webSocket).close(NORMAL_CLOSURE.code(), NORMAL_CLOSURE.reasonText());
        verifyNoMoreInteractions(webSocket);
    }

    @Test
    void shouldCloseWithErrorWhenErrorOccursOnRequestMessage() {
        final Message message = new DefaultMessage("Response message");

        when(configuration.getSubscriber()).thenReturn(new WebSocketEntrypointConnectorConfiguration.Subscriber(true));
        when(request.webSocket()).thenReturn(webSocket);
        when(request.messages()).thenReturn(Flowable.error(new RuntimeException(MOCK_EXCEPTION)));
        when(response.messages()).thenReturn(Flowable.just(message, message, message));
        when(webSocket.close(anyInt(), anyString())).thenReturn(Completable.complete());

        final TestObserver<Void> obs = cut.handleResponse(ctx).test();

        obs.assertNoValues();

        verify(response).chunks(chunkCaptor.capture());
        chunkCaptor.getValue().test().assertComplete();

        verify(webSocket, never()).write(any(Buffer.class));
        verify(webSocket).close(SERVER_ERROR.code(), SERVER_ERROR.reasonText());
        verifyNoMoreInteractions(webSocket);
    }

    @Test
    void shouldCloseWithErrorWhenErrorOccursOnResponseMessage() {
        final Message message = new DefaultMessage("Request message");

        when(configuration.getSubscriber()).thenReturn(new WebSocketEntrypointConnectorConfiguration.Subscriber(true));
        when(request.webSocket()).thenReturn(webSocket);
        when(request.messages()).thenReturn(Flowable.just(message, message, message));
        when(response.messages()).thenReturn(Flowable.error(new RuntimeException(MOCK_EXCEPTION)));
        when(webSocket.close(anyInt(), anyString())).thenReturn(Completable.complete());

        final TestObserver<Void> obs = cut.handleResponse(ctx).test();

        obs.assertNoValues();

        verify(response).chunks(chunkCaptor.capture());
        chunkCaptor.getValue().test().assertComplete();

        verify(webSocket, never()).write(any(Buffer.class));
        verify(webSocket).close(SERVER_ERROR.code(), SERVER_ERROR.reasonText());
        verifyNoMoreInteractions(webSocket);
    }

    @Test
    void shouldCloseWithErrorWhenMessageErrorOccursOnResponseMessage() {
        final Message message = new DefaultMessage("Request message");

        when(configuration.getSubscriber()).thenReturn(new WebSocketEntrypointConnectorConfiguration.Subscriber(true));
        when(request.webSocket()).thenReturn(webSocket);
        when(request.messages()).thenReturn(Flowable.just(message, message, message));
        when(response.messages()).thenReturn(Flowable.just(new DefaultMessage("error").error(true)));
        when(webSocket.close(anyInt(), anyString())).thenReturn(Completable.complete());

        final TestObserver<Void> obs = cut.handleResponse(ctx).test();

        obs.assertNoValues();

        verify(response).chunks(chunkCaptor.capture());
        chunkCaptor.getValue().test().assertComplete();

        verify(webSocket, never()).write(any(Buffer.class));
        verify(webSocket).close(SERVER_ERROR.code(), SERVER_ERROR.reasonText());
        verifyNoMoreInteractions(webSocket);
    }

    @Test
    void shouldHandleResponseWithEmptyWhenSubscriberIsDisabled() {
        final Message message = new DefaultMessage("Response message");

        when(configuration.getSubscriber()).thenReturn(new WebSocketEntrypointConnectorConfiguration.Subscriber(false));
        when(request.webSocket()).thenReturn(webSocket);
        when(request.messages()).thenReturn(Flowable.just(message, message, message));
        when(webSocket.close(anyInt(), anyString())).thenReturn(Completable.complete());

        final TestObserver<Void> obs = cut.handleResponse(ctx).test();

        obs.assertNoValues();

        verify(response).chunks(chunkCaptor.capture());
        chunkCaptor.getValue().test().assertComplete();

        verify(webSocket, never()).write(any(Buffer.class));
        verify(webSocket).close(NORMAL_CLOSURE.code(), NORMAL_CLOSURE.reasonText());

        verifyNoMoreInteractions(webSocket);
    }

    @Test
    void shouldCloseWithTryAgainLaterWhenStopping() throws Exception {
        final TestScheduler testScheduler = new TestScheduler();

        final Message message = new DefaultMessage("Message");
        final Flowable<Message> messageFlow = Flowable
            .just(message, message, message)
            .zipWith(Flowable.interval(1000, TimeUnit.MILLISECONDS, testScheduler), (m, aLong) -> m);

        when(configuration.getSubscriber()).thenReturn(new WebSocketEntrypointConnectorConfiguration.Subscriber(true));
        when(request.webSocket()).thenReturn(webSocket);
        when(request.messages()).thenReturn(Flowable.empty());
        when(response.messages()).thenReturn(messageFlow);
        when(webSocket.write(any(Buffer.class))).thenReturn(Completable.complete());
        when(webSocket.close(anyInt(), anyString())).thenReturn(Completable.complete());

        final TestObserver<Void> obs = cut.handleResponse(ctx).test();

        obs.assertNoValues();

        verify(response).chunks(chunkCaptor.capture());
        final TestSubscriber<Buffer> chunkObs = chunkCaptor.getValue().test();
        chunkObs.assertNotComplete();

        // Advance time by 2 seconds should produce 2 messages en request and 2 on response.
        testScheduler.advanceTimeBy(2000, TimeUnit.MILLISECONDS);
        testScheduler.triggerActions();

        // Trigger stop.
        cut.preStop();

        // Should have completed.
        chunkObs.assertComplete();
        verify(webSocket, times(2)).write(any(Buffer.class));
        verify(webSocket).close(TRY_AGAIN_LATER.code(), TRY_AGAIN_LATER.reasonText());
        verifyNoMoreInteractions(webSocket);
    }
}
