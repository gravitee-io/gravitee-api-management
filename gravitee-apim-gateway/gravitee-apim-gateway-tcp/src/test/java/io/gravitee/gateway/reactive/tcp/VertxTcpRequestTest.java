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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.IdGenerator;
import io.gravitee.common.utils.UUID;
import io.gravitee.gateway.reactive.tcp.VertxTcpRequest;
import io.reactivex.rxjava3.core.Flowable;
import io.vertx.rxjava3.core.net.NetSocket;
import io.vertx.rxjava3.core.net.SocketAddress;
import java.time.Instant;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class VertxTcpRequestTest {

    @Mock
    NetSocket socket;

    IdGenerator idGenerator = new UUID();

    @Test
    void should_create_a_request() {
        when(socket.toFlowable()).thenReturn(Flowable.just(io.vertx.rxjava3.core.buffer.Buffer.buffer("foo")));
        when(socket.remoteAddress()).thenReturn(SocketAddress.inetSocketAddress(4092, "www.acme.com"));
        when(socket.localAddress()).thenReturn(SocketAddress.inetSocketAddress(55468, "127.0.0.1"));
        when(socket.indicatedServerName()).thenReturn("www.acme.com");

        VertxTcpRequest tcpRequest = new VertxTcpRequest(socket, idGenerator);
        assertThat(tcpRequest.id()).isNotEmpty();
        assertThat(tcpRequest.transactionId()).isNotEmpty();
        assertThat(tcpRequest.timestamp()).isCloseTo(Instant.now().toEpochMilli(), Percentage.withPercentage(0.01));
        assertThat(tcpRequest.remoteAddress()).isEqualTo("www.acme.com:4092");
        assertThat(tcpRequest.originalHost()).isEqualTo("www.acme.com");
        assertThat(tcpRequest.host()).isNotEmpty();
        assertThat(tcpRequest.localAddress()).isEqualTo("127.0.0.1");
        assertThat(tcpRequest.isStreaming()).isTrue();
        assertThat(tcpRequest.ended()).isFalse();
        assertThat(tcpRequest.getWriteStream()).isEqualTo(socket);

        assertBlankedValues(tcpRequest);

        tcpRequest
            .chunks()
            .doOnNext(buffer -> assertThat(buffer).hasToString("foo"))
            .test()
            .awaitCount(1)
            .assertComplete();
    }

    @Test
    void should_create_request_without_network_data() {
        when(socket.toFlowable()).thenReturn(Flowable.empty());
        VertxTcpRequest tcpRequest = new VertxTcpRequest(socket, idGenerator);
        assertThat(tcpRequest.remoteAddress()).isNull();
        assertThat(tcpRequest.originalHost()).isNull();
        assertThat(tcpRequest.host()).isNull();
        assertThat(tcpRequest.localAddress()).isNull();
        assertThat(tcpRequest.isStreaming()).isTrue();

        assertBlankedValues(tcpRequest);
    }

    private static void assertBlankedValues(VertxTcpRequest tcpRequest) {
        assertThat(tcpRequest.headers()).isEmpty();
        assertThat(tcpRequest.body()).isNotNull();
        assertThat(tcpRequest.body().blockingGet()).isNull();
        assertThat(tcpRequest.parameters()).isEmpty();
        assertThat(tcpRequest.pathParameters()).isEmpty();
        assertThat(tcpRequest.messages()).isNotNull();
        tcpRequest.messages().test().assertValueCount(0).assertComplete();
        assertThat(tcpRequest.contextPath()).isEmpty();
        assertThat(tcpRequest.path()).isEmpty();
        assertThat(tcpRequest.pathInfo()).isEmpty();
        assertThat(tcpRequest.scheme()).isEmpty();
        assertThat(tcpRequest.uri()).isEmpty();
    }
}
