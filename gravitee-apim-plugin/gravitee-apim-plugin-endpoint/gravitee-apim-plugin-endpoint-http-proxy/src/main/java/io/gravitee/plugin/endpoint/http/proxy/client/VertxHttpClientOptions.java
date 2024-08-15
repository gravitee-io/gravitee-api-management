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
package io.gravitee.plugin.endpoint.http.proxy.client;

import io.gravitee.node.vertx.client.http.VertxHttpProtocolVersion;
import java.io.Serial;
import java.io.Serializable;
import lombok.*;

/**
 * This is a terrible hack that must not be committed!
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VertxHttpClientOptions {

    @Serial
    private static final long serialVersionUID = -6047350282117115405L;

    public static final int DEFAULT_HTTP2_MULTIPLEXING_LIMIT = -1;
    public static final long DEFAULT_IDLE_TIMEOUT = 60000;
    public static final long DEFAULT_KEEP_ALIVE_TIMEOUT = 30000;
    public static final long DEFAULT_CONNECT_TIMEOUT = 5000;
    public static final long DEFAULT_READ_TIMEOUT = 10000;
    public static final int DEFAULT_MAX_CONCURRENT_CONNECTIONS = 100;
    public static final boolean DEFAULT_KEEP_ALIVE = true;
    public static final boolean DEFAULT_PIPELINING = false;
    public static final boolean DEFAULT_USE_COMPRESSION = true;
    public static final boolean DEFAULT_PROPAGATE_CLIENT_ACCEPT_ENCODING = false;
    public static final boolean DEFAULT_FOLLOW_REDIRECTS = false;
    public static final boolean DEFAULT_CLEAR_TEXT_UPGRADE = true;
    public static final VertxHttpProtocolVersion DEFAULT_PROTOCOL_VERSION = VertxHttpProtocolVersion.HTTP_1_1;

    private int http2MultiplexingLimit = DEFAULT_HTTP2_MULTIPLEXING_LIMIT;
    private long idleTimeout = DEFAULT_IDLE_TIMEOUT;
    private long keepAliveTimeout = DEFAULT_KEEP_ALIVE_TIMEOUT;
    private long connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    private boolean keepAlive = DEFAULT_KEEP_ALIVE;
    private long readTimeout = DEFAULT_READ_TIMEOUT;
    private boolean pipelining = DEFAULT_PIPELINING;
    private int maxConcurrentConnections = DEFAULT_MAX_CONCURRENT_CONNECTIONS;
    private boolean useCompression = DEFAULT_USE_COMPRESSION;
    private boolean clearTextUpgrade = DEFAULT_CLEAR_TEXT_UPGRADE;
    private VertxHttpProtocolVersion version = DEFAULT_PROTOCOL_VERSION;
}
