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
package io.gravitee.gateway.http.connector.http;

import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.gateway.api.proxy.ProxyRequest;
import io.gravitee.gateway.http.connector.AbstractConnector;
import io.gravitee.gateway.http.connector.AbstractHttpProxyConnection;
import io.netty.handler.codec.http.HttpHeaderNames;
import java.net.URL;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author GraviteeSource Team
 */
public class Http2Connector<T extends HttpEndpoint> extends AbstractConnector<T> {

    protected static final Set<CharSequence> HTTP2_ILLEGAL_HEADERS;

    static {
        HTTP2_ILLEGAL_HEADERS =
            Set.of(
                HttpHeaderNames.CONNECTION,
                HttpHeaderNames.HOST,
                HttpHeaderNames.PROXY_CONNECTION,
                HttpHeaderNames.TRANSFER_ENCODING,
                HttpHeaderNames.UPGRADE
            );
    }

    @Autowired
    public Http2Connector(T endpoint) {
        super(endpoint);
    }

    @Override
    protected void convertHeadersForHttpVersion(URL url, ProxyRequest request, String host) {
        // TODO: support HTTP2 headers, something like:
        //        request.headers().set(Http2PseudoHeaderNames.AUTHORITY, host);
        //
        //        if (!request.headers().contains(Http2PseudoHeaderNames.SCHEME)) {
        //            String scheme = request.headers().contains(HttpHeaderNames.X_FORWARDED_PROTO)
        //                ? request.headers().get(HttpHeaderNames.X_FORWARDED_PROTO)
        //                : url.getProtocol();
        //            request.headers().set(Http2PseudoHeaderNames.SCHEME, scheme);
        //        }
        //        if (!request.headers().contains(Http2PseudoHeaderNames.PATH)) {
        //            request.headers().set(Http2PseudoHeaderNames.PATH, url.getFile());
        //        }
        // Strip all headers made illegal in HTTP/2 messages:
        HTTP2_ILLEGAL_HEADERS.forEach(name -> request.headers().remove(name));
    }

    @Override
    protected AbstractHttpProxyConnection create(ProxyRequest proxyRequest) {
        return new HttpProxyConnection<>(endpoint, proxyRequest);
    }
}
