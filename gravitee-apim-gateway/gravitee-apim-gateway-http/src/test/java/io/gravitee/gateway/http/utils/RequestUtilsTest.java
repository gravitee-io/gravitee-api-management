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
package io.gravitee.gateway.http.utils;

import static io.gravitee.common.http.MediaType.APPLICATION_GRPC;
import static io.gravitee.common.http.MediaType.APPLICATION_OCTET_STREAM;
import static io.gravitee.common.http.MediaType.TEXT_EVENT_STREAM;
import static io.vertx.core.http.HttpHeaders.CONNECTION;
import static io.vertx.core.http.HttpHeaders.CONTENT_LENGTH;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static io.vertx.core.http.HttpHeaders.UPGRADE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.reactive.api.context.Request;
import io.gravitee.gateway.reactive.api.context.Response;
import io.gravitee.gateway.reactive.http.vertx.VertxHttpServerRequest;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RequestUtilsTest {

    @Nested
    class WebSocketRequestTest {

        @Mock
        HttpServerRequest httpServerRequest;

        @BeforeEach
        public void beforeEach() {
            reset(httpServerRequest);
            lenient().when(httpServerRequest.headers()).thenReturn(HttpHeaders.headers());
        }

        @Test
        void should_not_be_websocket_when_http2() {
            lenient().when(httpServerRequest.getHeader(CONNECTION)).thenReturn("Upgrade");
            lenient().when(httpServerRequest.getHeader(UPGRADE)).thenReturn("websocket");
            when(httpServerRequest.version()).thenReturn(HttpVersion.HTTP_2);
            assertFalse(RequestUtils.isWebSocket(httpServerRequest));
        }

        @Test
        void should_not_be_websocket_when_no_connection_header() {
            lenient().when(httpServerRequest.getHeader(CONNECTION)).thenReturn(null);
            lenient().when(httpServerRequest.getHeader(UPGRADE)).thenReturn("websocket");
            when(httpServerRequest.method()).thenReturn(HttpMethod.GET);
            when(httpServerRequest.version()).thenReturn(HttpVersion.HTTP_1_1);

            assertFalse(RequestUtils.isWebSocket(httpServerRequest));
        }

        @Test
        void should_not_be_web_socket_when_no_upgrade_header() {
            lenient().when(httpServerRequest.getHeader(CONNECTION)).thenReturn("Upgrade");
            lenient().when(httpServerRequest.getHeader(UPGRADE)).thenReturn(null);
            when(httpServerRequest.method()).thenReturn(HttpMethod.GET);
            when(httpServerRequest.version()).thenReturn(HttpVersion.HTTP_1_1);

            assertFalse(RequestUtils.isWebSocket(httpServerRequest));
        }

        @Test
        void should_not_be_web_socket_when_not_get_method() {
            lenient().when(httpServerRequest.getHeader(CONNECTION)).thenReturn("Upgrade");
            lenient().when(httpServerRequest.getHeader(UPGRADE)).thenReturn("websocket");
            when(httpServerRequest.method()).thenReturn(HttpMethod.POST);
            when(httpServerRequest.version()).thenReturn(HttpVersion.HTTP_1_1);

            assertFalse(RequestUtils.isWebSocket(httpServerRequest));
        }

        @Test
        void should_not_be_web_socket_when_not_upgrade_web_socket() {
            lenient().when(httpServerRequest.getHeader(CONNECTION)).thenReturn("Upgrade");
            lenient().when(httpServerRequest.getHeader(UPGRADE)).thenReturn("something else");
            when(httpServerRequest.method()).thenReturn(HttpMethod.GET);
            when(httpServerRequest.version()).thenReturn(HttpVersion.HTTP_1_1);

            assertFalse(RequestUtils.isWebSocket(httpServerRequest));
        }

        @Test
        void should_be_web_socket() {
            lenient().when(httpServerRequest.getHeader(CONNECTION)).thenReturn("Upgrade");
            lenient().when(httpServerRequest.getHeader(UPGRADE)).thenReturn("websocket");
            when(httpServerRequest.method()).thenReturn(HttpMethod.GET);
            when(httpServerRequest.version()).thenReturn(HttpVersion.HTTP_1_1);

            assertTrue(RequestUtils.isWebSocket(httpServerRequest));
        }

        @Test
        void should_be_web_socket_connection_header_multivalues() {
            lenient().when(httpServerRequest.getHeader(CONNECTION)).thenReturn("keep-alive, Upgrade");
            lenient().when(httpServerRequest.getHeader(UPGRADE)).thenReturn("websocket");
            when(httpServerRequest.method()).thenReturn(HttpMethod.GET);
            when(httpServerRequest.version()).thenReturn(HttpVersion.HTTP_1_1);

            assertTrue(RequestUtils.isWebSocket(httpServerRequest));
        }
    }

    @Nested
    class StreamingRequestTest {

        @Mock
        Request request;

        private io.gravitee.gateway.api.http.HttpHeaders headers;

        @BeforeEach
        public void beforeEach() {
            headers = spy(io.gravitee.gateway.api.http.HttpHeaders.create());
            lenient().when(request.headers()).thenReturn(headers);
        }

        @Test
        void should_not_be_streaming_request_when_content_length_not_null() {
            headers.add(CONTENT_LENGTH, "1");
            assertFalse(RequestUtils.isStreaming(request));
        }

        @Test
        void should_not_be_streaming_when_no_headers_nor_no_websocket() {
            assertFalse(RequestUtils.isStreaming(request));
        }

        @Test
        void should_be_streaming_request_when_content_type_grpc() {
            headers.add(CONTENT_TYPE, APPLICATION_GRPC);
            assertTrue(RequestUtils.isStreaming(request));
        }

        @Test
        void should_be_streaming_request_when_content_type_text_event_stream() {
            headers.add(CONTENT_TYPE, TEXT_EVENT_STREAM);
            assertTrue(RequestUtils.isStreaming(request));
        }

        @Test
        void should_be_streaming_request_when_content_type_octet_stream() {
            headers.add(CONTENT_TYPE, APPLICATION_OCTET_STREAM);
            assertTrue(RequestUtils.isStreaming(request));
        }

        @Test
        void should_be_streaming_request_when_websocket() {
            lenient().when(request.isWebSocket()).thenReturn(true);
            assertTrue(RequestUtils.isStreaming(request));
        }
    }

    @Nested
    class StreamingResponseTest {

        @Mock
        VertxHttpServerRequest request;

        @Mock
        Response response;

        private io.gravitee.gateway.api.http.HttpHeaders headers;

        @BeforeEach
        public void beforeEach() {
            headers = io.gravitee.gateway.api.http.HttpHeaders.create();
            lenient().when(response.headers()).thenReturn(headers);
        }

        @Test
        void should_not_be_streaming_when_no_headers_nor_no_websocket() {
            lenient().when(request.isWebSocketUpgraded()).thenReturn(false);
            assertFalse(RequestUtils.isStreaming(request, response));
        }

        @Test
        void should_be_streaming_request_when_content_type_grpc() {
            headers.add(CONTENT_TYPE, APPLICATION_GRPC);
            assertTrue(headers.get(CONTENT_TYPE) != null);
            assertTrue(RequestUtils.isStreaming(request, response));
        }

        @Test
        void should_be_streaming_request_when_content_type_text_event_stream() {
            headers.add(CONTENT_TYPE, TEXT_EVENT_STREAM);
            assertTrue(RequestUtils.isStreaming(request, response));
        }

        @Test
        void should_be_streaming_request_when_content_type_octet_stream() {
            headers.add(CONTENT_TYPE, APPLICATION_OCTET_STREAM);
            assertTrue(RequestUtils.isStreaming(request, response));
        }

        @Test
        void should_be_streaming_request_when_websocket() {
            lenient().when(request.isWebSocketUpgraded()).thenReturn(true);
            assertTrue(RequestUtils.isStreaming(request, response));
        }
    }
}
