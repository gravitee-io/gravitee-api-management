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

import io.vertx.core.VertxException;

/**
 * Raised when the gateway closes an upstream (gateway→backend) connection because it stayed idle longer than the
 * endpoint's configured {@code idleTimeout}. Vert.x's {@code HttpClient} would otherwise close an idle connection
 * silently, surfacing only a generic "Connection was closed" to the in-flight request — indistinguishable from a
 * genuine upstream close. {@code HttpClientFactory} installs a handler that converts the Netty
 * {@code IdleStateEvent} into this typed exception so the connector can classify it as
 * {@code UPSTREAM_IDLE_TIMEOUT} rather than {@code UPSTREAM_CONNECTION_CLOSED} (APIM-12769).
 */
public class UpstreamIdleTimeoutException extends VertxException {

    public UpstreamIdleTimeoutException(final String message) {
        super(message, true);
    }
}
