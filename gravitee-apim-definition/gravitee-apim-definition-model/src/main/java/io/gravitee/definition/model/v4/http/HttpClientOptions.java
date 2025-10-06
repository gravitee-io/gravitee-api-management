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
package io.gravitee.definition.model.v4.http;

import static io.gravitee.node.vertx.client.http.VertxHttpClientOptions.*;

import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HttpClientOptions implements Serializable {

    @Serial
    private static final long serialVersionUID = -7061411805967594667L;

    private static final boolean DEFAULT_PROPAGATE_CLIENT_HOST = false;

    @Builder.Default
    private int http2MultiplexingLimit = -1;

    @Builder.Default
    private int http2ConnectionWindowSize = -1;

    @Builder.Default
    private int http2StreamWindowSize = -1;

    @Builder.Default
    private int http2MaxFrameSize = DEFAULT_MAX_FRAME_SIZE;

    @Builder.Default
    private long idleTimeout = DEFAULT_IDLE_TIMEOUT;

    @Builder.Default
    private long keepAliveTimeout = DEFAULT_KEEP_ALIVE_TIMEOUT;

    @Builder.Default
    private long connectTimeout = DEFAULT_CONNECT_TIMEOUT;

    @Builder.Default
    private boolean keepAlive = DEFAULT_KEEP_ALIVE;

    @Builder.Default
    private long readTimeout = DEFAULT_READ_TIMEOUT;

    @Builder.Default
    private boolean pipelining = DEFAULT_PIPELINING;

    @Builder.Default
    private int maxConcurrentConnections = DEFAULT_MAX_CONCURRENT_CONNECTIONS;

    @Builder.Default
    private boolean useCompression = DEFAULT_USE_COMPRESSION;

    @Builder.Default
    private boolean propagateClientAcceptEncoding = DEFAULT_PROPAGATE_CLIENT_ACCEPT_ENCODING;

    @Builder.Default
    private boolean propagateClientHost = DEFAULT_PROPAGATE_CLIENT_HOST;

    @Builder.Default
    private boolean followRedirects = DEFAULT_FOLLOW_REDIRECTS;

    @Builder.Default
    private boolean clearTextUpgrade = DEFAULT_CLEAR_TEXT_UPGRADE;

    @Builder.Default
    private ProtocolVersion version = ProtocolVersion.valueOf(DEFAULT_PROTOCOL_VERSION.name());

    public boolean isPropagateClientAcceptEncoding() {
        // Propagate Accept-Encoding can only be made if useCompression is disabled.
        return !useCompression && propagateClientAcceptEncoding;
    }
}
