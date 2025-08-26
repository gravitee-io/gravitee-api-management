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
package io.gravitee.gateway.reactive.reactor.processor.connection;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.common.http.HttpHeadersValues;
import io.gravitee.common.http.HttpVersion;
import io.gravitee.gateway.reactive.core.connection.DefaultConnectionDrainManager;
import io.gravitee.gateway.reactive.reactor.processor.AbstractProcessorTest;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class ConnectionDrainProcessorTest extends AbstractProcessorTest {

    DefaultConnectionDrainManager connectionDrainManager;

    ConnectionDrainProcessor cut;

    @BeforeEach
    void setUp() {
        connectionDrainManager = new DefaultConnectionDrainManager();
        cut = new ConnectionDrainProcessor(connectionDrainManager);
    }

    @Test
    void should_not_drain_connection_when_no_drain_requested() {
        cut.execute(ctx).test().assertComplete();
        verifyNoInteractions(mockResponse);
    }

    @Test
    void should_not_drain_connection_when_connection_created_after_drain_request() {
        connectionDrainManager.requestDrain();
        when(mockRequest.connectionTimestamp()).thenReturn(System.currentTimeMillis() + 1000);

        cut.execute(ctx).test().assertComplete();
        verifyNoInteractions(mockResponse);
    }

    @Test
    void should_add_connection_close_response_header_when_draining_http1() {
        when(mockRequest.version()).thenReturn(HttpVersion.HTTP_1_1);
        when(mockRequest.connectionTimestamp()).thenReturn(System.currentTimeMillis() - 1000);
        connectionDrainManager.requestDrain();
        cut.execute(ctx).test().assertComplete();

        assertThat(spyResponseHeaders.get(HttpHeaderNames.CONNECTION)).isEqualTo(HttpHeadersValues.CONNECTION_CLOSE);
    }

    @Test
    void should_add_connection_close_response_header_when_draining_http2() {
        when(mockRequest.version()).thenReturn(HttpVersion.HTTP_2);
        when(mockRequest.connectionTimestamp()).thenReturn(System.currentTimeMillis() - 1000);
        connectionDrainManager.requestDrain();
        cut.execute(ctx).test().assertComplete();

        assertThat(spyResponseHeaders.get(HttpHeaderNames.CONNECTION)).isEqualTo(HttpHeadersValues.CONNECTION_GO_AWAY);
    }

    @Test
    void should_return_processor_id() {
        assertThat(cut.getId()).isEqualTo("processor-connection-drain");
    }
}
