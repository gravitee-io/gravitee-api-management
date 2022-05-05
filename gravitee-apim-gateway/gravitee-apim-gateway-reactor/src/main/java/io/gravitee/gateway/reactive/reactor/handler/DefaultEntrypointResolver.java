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
package io.gravitee.gateway.reactive.reactor.handler;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpVersion;
import io.gravitee.common.util.MultiValueMap;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.api.http2.HttpFrame;
import io.gravitee.gateway.api.stream.ReadStream;
import io.gravitee.gateway.api.ws.WebSocket;
import io.gravitee.gateway.reactive.reactor.handler.EntrypointResolver;
import io.gravitee.gateway.reactor.handler.HandlerEntrypoint;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;
import io.gravitee.reporter.api.http.Metrics;
import javax.net.ssl.SSLSession;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultEntrypointResolver implements EntrypointResolver {

    public static final String ATTR_ENTRYPOINT = ExecutionContext.ATTR_PREFIX + "entrypoint";

    private final ReactorHandlerRegistry handlerRegistry;

    public DefaultEntrypointResolver(ReactorHandlerRegistry handlerRegistry) {
        this.handlerRegistry = handlerRegistry;
    }

    @Override
    public HandlerEntrypoint resolve(String host, String path) {
        Request request = new EntrypointRequest(path, host);
        for (HandlerEntrypoint entrypoint : handlerRegistry.getEntrypoints()) {
            if (entrypoint.accept(request)) {
                return entrypoint;
            }
        }

        return null;
    }

    /**
     * FIXME: quick & dirty solution to make current entry point resolution working.
     */
    private static class EntrypointRequest implements Request {

        private final String path;
        private final String host;

        public EntrypointRequest(String path, String host) {
            this.path = path;
            this.host = host;
        }

        @Override
        public String id() {
            return null;
        }

        @Override
        public String transactionId() {
            return null;
        }

        @Override
        public String uri() {
            return null;
        }

        @Override
        public String path() {
            return path;
        }

        @Override
        public String pathInfo() {
            return null;
        }

        @Override
        public String contextPath() {
            return null;
        }

        @Override
        public MultiValueMap<String, String> parameters() {
            return null;
        }

        @Override
        public MultiValueMap<String, String> pathParameters() {
            return null;
        }

        @Override
        public HttpHeaders headers() {
            return null;
        }

        @Override
        public HttpMethod method() {
            return null;
        }

        @Override
        public String scheme() {
            return null;
        }

        @Override
        public HttpVersion version() {
            return null;
        }

        @Override
        public long timestamp() {
            return 0;
        }

        @Override
        public String remoteAddress() {
            return null;
        }

        @Override
        public String localAddress() {
            return null;
        }

        @Override
        public SSLSession sslSession() {
            return null;
        }

        @Override
        public Metrics metrics() {
            return null;
        }

        @Override
        public boolean ended() {
            return false;
        }

        @Override
        public Request timeoutHandler(Handler<Long> timeoutHandler) {
            return null;
        }

        @Override
        public Handler<Long> timeoutHandler() {
            return null;
        }

        @Override
        public boolean isWebSocket() {
            return false;
        }

        @Override
        public WebSocket websocket() {
            return null;
        }

        @Override
        public Request customFrameHandler(Handler<HttpFrame> frameHandler) {
            return null;
        }

        @Override
        public Request closeHandler(Handler<Void> closeHandler) {
            return null;
        }

        @Override
        public String host() {
            return host;
        }

        @Override
        public ReadStream<Buffer> bodyHandler(Handler<Buffer> bodyHandler) {
            return null;
        }

        @Override
        public ReadStream<Buffer> endHandler(Handler<Void> endHandler) {
            return null;
        }
    }
}
