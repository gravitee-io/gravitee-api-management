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
package io.gravitee.gateway.debug.vertx;

import lombok.Builder;

/**
 * @author Guillaume Cusnieux (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
@Builder
public class VertxDebugHttpClientConfiguration {

    public static final int MAX_CONNECTION_TIMEOUT = 5000;
    public static final int MAX_REQUEST_TIMEOUT = 30000;

    private boolean compressionSupported;

    private boolean alpn;

    private boolean secured;

    private boolean openssl;

    private int connectTimeout;

    private int requestTimeout;

    private int port;

    private String host;

    public boolean isCompressionSupported() {
        return compressionSupported;
    }

    public boolean isAlpn() {
        return alpn;
    }

    public boolean isSecured() {
        return secured;
    }

    public boolean isOpenssl() {
        return openssl;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public int getConnectTimeout() {
        return Math.min(connectTimeout, MAX_CONNECTION_TIMEOUT);
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getRequestTimeout() {
        return Math.min(requestTimeout, MAX_REQUEST_TIMEOUT);
    }

    public void setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }
}
