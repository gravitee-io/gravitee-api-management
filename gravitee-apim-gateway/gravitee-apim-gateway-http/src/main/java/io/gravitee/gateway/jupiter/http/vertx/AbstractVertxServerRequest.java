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
package io.gravitee.gateway.jupiter.http.vertx;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.common.http.HttpVersion;
import io.gravitee.common.http.IdGenerator;
import io.gravitee.common.util.LinkedMultiValueMap;
import io.gravitee.common.util.MultiValueMap;
import io.gravitee.common.util.URIUtils;
import io.gravitee.gateway.http.vertx.VertxHttpHeaders;
import io.gravitee.gateway.jupiter.core.context.AbstractRequest;
import io.vertx.rxjava3.core.http.HttpServerRequest;
import io.vertx.rxjava3.core.net.SocketAddress;
import javax.net.ssl.SSLSession;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
abstract class AbstractVertxServerRequest extends AbstractRequest {

    protected final HttpServerRequest nativeRequest;

    AbstractVertxServerRequest(HttpServerRequest nativeRequest, IdGenerator idGenerator) {
        this.nativeRequest = nativeRequest;
        this.originalHost = this.nativeRequest.host();
        this.timestamp = System.currentTimeMillis();
        this.id = idGenerator.randomString();
        this.headers = new VertxHttpHeaders(nativeRequest.headers().getDelegate());
    }

    public HttpServerRequest getNativeRequest() {
        return nativeRequest;
    }

    @Override
    public String uri() {
        if (uri == null) {
            uri = nativeRequest.uri();
        }

        return uri;
    }

    @Override
    public String path() {
        if (path == null) {
            path = nativeRequest.path();
        }

        return path;
    }

    @Override
    public String contextPath() {
        return contextPath;
    }

    @Override
    public MultiValueMap<String, String> parameters() {
        if (parameters == null) {
            parameters = URIUtils.parameters(nativeRequest.uri());
        }

        return parameters;
    }

    @Override
    public MultiValueMap<String, String> pathParameters() {
        if (pathParameters == null) {
            pathParameters = new LinkedMultiValueMap<>();
        }

        return pathParameters;
    }

    @Override
    public HttpMethod method() {
        if (method == null) {
            try {
                method = HttpMethod.valueOf(nativeRequest.method().name());
            } catch (IllegalArgumentException iae) {
                method = HttpMethod.OTHER;
            }
        }

        return method;
    }

    @Override
    public String scheme() {
        if (scheme == null) {
            scheme = nativeRequest.scheme();
        }

        return scheme;
    }

    @Override
    public HttpVersion version() {
        if (version == null) {
            version = HttpVersion.valueOf(nativeRequest.version().name());
        }

        return version;
    }

    @Override
    public String remoteAddress() {
        if (remoteAddress == null) {
            SocketAddress nativeRemoteAddress = nativeRequest.remoteAddress();
            this.remoteAddress = extractAddress(nativeRemoteAddress);
        }
        return remoteAddress;
    }

    @Override
    public String localAddress() {
        if (localAddress == null) {
            this.localAddress = extractAddress(nativeRequest.localAddress());
        }
        return localAddress;
    }

    private String extractAddress(SocketAddress address) {
        if (address != null) {
            //TODO Could be improve to a better compatibility with geoIP
            int ipv6Idx = address.host().indexOf("%");
            return (ipv6Idx != -1) ? address.host().substring(0, ipv6Idx) : address.host();
        }
        return null;
    }

    @Override
    public SSLSession sslSession() {
        if (sslSession == null) {
            sslSession = nativeRequest.sslSession();
        }

        return sslSession;
    }

    @Override
    public boolean ended() {
        return nativeRequest.isEnded();
    }

    @Override
    public String host() {
        return this.nativeRequest.host();
    }

    /**
     * Pauses the current request.
     * <b>WARN: use with caution</b>
     */
    public void pause() {
        this.nativeRequest.pause();
    }

    /**
     * Resumes the current request.
     * <b>WARN: use with caution</b>
     */
    public void resume() {
        this.nativeRequest.resume();
    }
}
