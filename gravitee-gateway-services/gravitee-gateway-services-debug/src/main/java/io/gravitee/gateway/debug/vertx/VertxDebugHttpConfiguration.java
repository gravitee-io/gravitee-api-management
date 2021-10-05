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
package io.gravitee.gateway.debug.vertx;

import io.gravitee.gateway.http.vertx.VertxHttpServerConfiguration;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author Guillaume Cusnieux (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class VertxDebugHttpConfiguration extends VertxHttpServerConfiguration {

    private static final int MAX_CONNECTION_TIMEOUT = 5000;
    private static final int MAX_REQUEST_TIMEOUT = 10000;

    @Value("${debug.timeout.connect:5000}")
    private int connectTimeout;

    @Value("${debug.timeout.request:10000}")
    private int requestTimeout;

    @Value("${debug.port:8482}")
    private int port;

    @Value("${debug.host:localhost}")
    private String host;

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getHost() {
        return host;
    }

    public int getConnectTimeout() {
        return connectTimeout < MAX_CONNECTION_TIMEOUT ? connectTimeout : MAX_CONNECTION_TIMEOUT;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getRequestTimeout() {
        return requestTimeout < MAX_REQUEST_TIMEOUT ? requestTimeout : MAX_REQUEST_TIMEOUT;
    }

    public void setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }
}
