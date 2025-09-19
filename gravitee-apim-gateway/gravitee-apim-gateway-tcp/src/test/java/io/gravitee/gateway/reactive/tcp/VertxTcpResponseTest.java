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
package io.gravitee.gateway.reactive.tcp;

import static io.gravitee.gateway.reactive.tcp.VertxReadStreamUtil.toVertxRxReadStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.IdGenerator;
import io.gravitee.common.utils.UUID;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.tcp.vertx.RecordingWriteStream;
import io.reactivex.rxjava3.core.Flowable;
import io.vertx.rxjava3.core.net.NetSocket;
import io.vertx.rxjava3.core.streams.WriteStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class VertxTcpResponseTest {

    static final String SENT_BUFFER_1 = "client_buf_1";
    static final String SENT_BUFFER_2 = "client_buf_2";
    static final String RECEIVED_BUFFER_1 = "backend_buf_1";
    static final String RECEIVED_BUFFER_2 = "backend_buf_2";

    @Mock
    NetSocket socket;

    @Mock
    VertxTcpRequest mockRequest;

    @Mock
    ExecutionContext executionContext;

    VertxTcpResponse cut;

    IdGenerator idGenerator = new UUID();
    IOException err = new IOException("simulated");

    @BeforeEach
    void createClassUnderTest() {
        cut = new VertxTcpResponse(mockRequest);
    }

    @Test
    void should_create_a_response() {
        when(socket.toFlowable()).thenReturn(Flowable.empty());
        VertxTcpRequest tcpRequest = new VertxTcpRequest(socket, idGenerator);
        cut = new VertxTcpResponse(tcpRequest);
        assertThat(cut.isStreaming()).isTrue();
        assertThat(cut.ended()).isFalse();
        assertThat(cut.status()).isZero();
        assertThat(cut.headers()).isEmpty();
        assertThat(cut.trailers()).isEmpty();
        assertThat(cut.reason()).isNull();
        cut.chunks().test().assertValueCount(0).assertComplete();
        assertThat(cut.ended()).isFalse();
    }

    static Stream<Arguments> buffers() {
        return Stream.of(
            arguments(List.of(), List.of()),
            arguments(List.of(), List.of(RECEIVED_BUFFER_1)),
            arguments(List.of(SENT_BUFFER_1), List.of()),
            arguments(List.of(SENT_BUFFER_1), List.of(RECEIVED_BUFFER_1, RECEIVED_BUFFER_2)),
            arguments(List.of(SENT_BUFFER_1, SENT_BUFFER_2), List.of(RECEIVED_BUFFER_1)),
            arguments(List.of(SENT_BUFFER_1, SENT_BUFFER_2), List.of(RECEIVED_BUFFER_1, RECEIVED_BUFFER_2))
        );
    }

    @ParameterizedTest
    @MethodSource("buffers")
    void should_end_a_response(List<String> sent, List<String> received) {
        RecordingWriteStream clientResponseStream = new RecordingWriteStream();
        when(mockRequest.getWriteStream()).thenReturn(WriteStream.newInstance(clientResponseStream));

        RecordingWriteStream backendRequestStream = new RecordingWriteStream();
        pipeUpstream(Flowable.fromIterable(sent.stream().map(Buffer::buffer).toList()), backendRequestStream);

        cut.chunks(Flowable.fromIterable(received.stream().map(Buffer::buffer).toList()));
        cut.pipeDownstream();

        cut.end(executionContext).test().awaitDone(200, TimeUnit.MILLISECONDS).assertComplete();
        assertResponseEnded();
        // backed stream sends client buffers
        assertThat(backendRequestStream.getRecordedBuffers()).containsExactlyElementsOf(sent);
        // client stream returns backend buffers
        assertThat(clientResponseStream.getRecordedBuffers()).containsExactlyElementsOf(received);
    }

    @Test
    void should_end_on_backend_write_error() {
        RecordingWriteStream clientResponseStream = new RecordingWriteStream();
        when(mockRequest.getWriteStream()).thenReturn(WriteStream.newInstance(clientResponseStream));

        Flowable<Buffer> sent = Flowable.just(Buffer.buffer(SENT_BUFFER_1));
        RecordingWriteStream backendRequestStream = new RecordingWriteStream();
        // will simulate a write error when writing backend buffers to the client
        backendRequestStream.errOnNextWrite(err);
        pipeUpstream(sent, backendRequestStream);

        cut.chunks(Flowable.just(Buffer.buffer(RECEIVED_BUFFER_1)));
        cut.pipeDownstream();

        cut.end(executionContext).test().awaitDone(200, TimeUnit.MILLISECONDS).assertError(err);
        assertResponseEnded();
        assertThat(backendRequestStream.getRecordedBuffers()).isEmpty();
    }

    @Test
    void should_end_on_client_stream_error() {
        // will simulate a write error when writing backend buffers to the client
        RecordingWriteStream clientResponseStream = new RecordingWriteStream();
        clientResponseStream.errOnNextWrite(err);
        when(mockRequest.getWriteStream()).thenReturn(WriteStream.newInstance(clientResponseStream));

        Flowable<Buffer> sent = Flowable.just(Buffer.buffer(SENT_BUFFER_1));
        RecordingWriteStream backendRequestStream = new RecordingWriteStream();
        pipeUpstream(sent, backendRequestStream);

        cut.chunks(Flowable.just(Buffer.buffer(RECEIVED_BUFFER_1)));
        cut.pipeDownstream();

        cut.end(executionContext).test().awaitDone(200, TimeUnit.MILLISECONDS).assertError(err);
        assertResponseEnded();
        assertThat(clientResponseStream.getRecordedBuffers()).isEmpty();
    }

    private void pipeUpstream(Flowable<Buffer> sent, RecordingWriteStream backendRequestStream) {
        when(mockRequest.upstreamPipe()).thenReturn(toVertxRxReadStream(sent).rxPipeTo(WriteStream.newInstance(backendRequestStream)));
    }

    @Test
    void should_end_flow_when_error_in_request_flow() {
        RecordingWriteStream clientResponseStream = new RecordingWriteStream();
        when(mockRequest.getWriteStream()).thenReturn(WriteStream.newInstance(clientResponseStream));

        // send one buffer and then an error
        Flowable<Buffer> sent = Flowable.concat(List.of(Flowable.just(Buffer.buffer(SENT_BUFFER_1)), Flowable.error(err)));
        RecordingWriteStream backendRequestStream = new RecordingWriteStream();
        pipeUpstream(sent, backendRequestStream);

        cut.chunks(Flowable.just(Buffer.buffer(RECEIVED_BUFFER_1)));
        cut.pipeDownstream();

        cut.end(executionContext).test().awaitDone(200, TimeUnit.MILLISECONDS).assertError(err);
        assertResponseEnded();
        assertThat(backendRequestStream.getRecordedBuffers()).containsExactly(SENT_BUFFER_1);
    }

    @Test
    void should_end_flow_when_error_in_response_flow() {
        RecordingWriteStream clientResponseStream = new RecordingWriteStream();
        when(mockRequest.getWriteStream()).thenReturn(WriteStream.newInstance(clientResponseStream));

        Flowable<Buffer> sent = Flowable.just(Buffer.buffer(SENT_BUFFER_1));
        RecordingWriteStream backendRequestStream = new RecordingWriteStream();
        pipeUpstream(sent, backendRequestStream);

        // respond a buffer and an error
        cut.chunks(Flowable.concat(List.of(Flowable.just(Buffer.buffer(RECEIVED_BUFFER_1)), Flowable.error(err))));
        cut.pipeDownstream();

        cut.end(executionContext).test().awaitDone(200, TimeUnit.MILLISECONDS).assertError(err);
        assertResponseEnded();
        assertThat(clientResponseStream.getRecordedBuffers()).containsExactly(RECEIVED_BUFFER_1);
    }

    private void assertResponseEnded() {
        assertThat(cut.ended())
            .withFailMessage(() -> "response is not ended")
            .isTrue();
    }
}
