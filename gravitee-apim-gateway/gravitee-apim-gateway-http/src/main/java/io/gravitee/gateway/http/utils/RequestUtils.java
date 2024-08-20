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

import static io.vertx.core.http.HttpHeaders.CONNECTION;
import static io.vertx.core.http.HttpHeaders.CONTENT_LENGTH;
import static io.vertx.core.http.HttpHeaders.CONTENT_TYPE;
import static io.vertx.core.http.HttpHeaders.UPGRADE;

import io.gravitee.common.http.MediaType;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.reactive.api.context.Request;
import io.gravitee.gateway.reactive.api.context.Response;
import io.gravitee.gateway.reactive.http.vertx.VertxHttpServerRequest;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpVersion;
import io.vertx.rxjava3.core.http.HttpServerRequest;
import java.util.Arrays;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RequestUtils {

    /**
     * We are only considering HTTP_1.x requests for now.
     * There is a dedicated RFC to support WebSockets over HTTP2: <a href="https://tools.ietf.org/html/rfc8441">RFC 8441</a>
     *
     * @return <code>true</code> if the given request is websocket one, <code>false</code> otherwise.
     */
    public static boolean isWebSocket(final HttpServerRequest nativeRequest) {
        return isWebSocket(nativeRequest.getDelegate());
    }

    public static boolean isWebSocket(final io.vertx.core.http.HttpServerRequest nativeRequest) {
        HttpVersion httpVersion = nativeRequest.version();
        if (HttpVersion.HTTP_1_0 == httpVersion || HttpVersion.HTTP_1_1 == httpVersion) {
            boolean isUpgrade = false;
            String connectionHeader = nativeRequest.getHeader(CONNECTION);
            if (connectionHeader != null) {
                String upgradeHeader = nativeRequest.getHeader(UPGRADE);
                isUpgrade =
                    Arrays
                        .stream(connectionHeader.split(","))
                        .map(String::trim)
                        .anyMatch(HttpHeaderValues.UPGRADE::contentEqualsIgnoreCase) &&
                    HttpHeaderValues.WEBSOCKET.contentEqualsIgnoreCase(upgradeHeader);
            }
            return HttpMethod.GET.name().equals(nativeRequest.method().name()) && isUpgrade;
        }
        return false;
    }

    /**
     * Check if the given request is streaming
     *
     * @param request the request to test
     * @return <code>true</code> if the given request is streaming, <code>false</code> otherwise.
     */
    public static boolean isStreaming(final Request request) {
        return hasStreamingContentType(request.headers()) || request.isWebSocket();
    }

    /**
     * Check if the given response is streaming
     *
     * @param request  the vertx http request
     * @param response the http response
     * @return <code>true</code> if the given response is streaming (websocket/gRPC/SEE...), <code>false</code> otherwise.
     */
    public static boolean isStreaming(VertxHttpServerRequest request, final Response response) {
        return hasStreamingContentType(response.headers()) || request.isWebSocketUpgraded();
    }

    /**
     * Check if the given headers contains streaming information
     *
     * @param httpHeaders header to test
     * @return true for gRPC, SSE, octet-stream
     */
    private static boolean hasStreamingContentType(final HttpHeaders httpHeaders) {
        String contentLengthHeaderValue = httpHeaders.get(CONTENT_LENGTH);
        if (contentLengthHeaderValue == null) {
            String contentTypeHeaderValue = httpHeaders.get(CONTENT_TYPE);
            MediaType contentTypeMedia = MediaType.parseMediaType(contentTypeHeaderValue);
            return (
                MediaType.MEDIA_APPLICATION_GRPC.equals(contentTypeMedia) ||
                MediaType.MEDIA_APPLICATION_OCTET_STREAM.equals(contentTypeMedia) ||
                MediaType.MEDIA_TEXT_EVENT_STREAM.equals(contentTypeMedia)
            );
        }
        return false;
    }
}
