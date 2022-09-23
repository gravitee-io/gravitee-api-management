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
package io.gravitee.plugin.entrypoint.http.get.configuration;

import io.gravitee.gateway.jupiter.api.connector.entrypoint.EntrypointConnectorConfiguration;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
public class HttpGetEntrypointConnectorConfiguration implements EntrypointConnectorConfiguration {

    /**
     * Maximum number of messages to retrieve. <br/>
     * Default is 500.
     */
    private int messagesLimitCount = 500;

    /**
     * Maximum duration in milliseconds to wait to retrieve messages. <br/>
     * Default is 5_000.
     */
    private long messagesLimitDurationMs = 5_000;

    /**
     * Allow sending messages headers to client in payload. Each header will be sent as extra field in payload. <br/>
     * For JSON and XML, in a dedicated headers object. For plain text, following 'key=value' format. <br/>
     * Default is false.
     */
    private boolean headersInPayload = false;

    /**
     * Allow sending messages metadata to client in payload. Each metadata will be sent as extra field in payload. <br/>
     * For JSON and XML, in a dedicated metadata object. For plain text, following 'key=value' format. <br/>
     * Default is false.
     */
    private boolean metadataInPayload = false;
}
