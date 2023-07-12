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
package io.gravitee.plugin.entrypoint.webhook.configuration;

import io.gravitee.gateway.jupiter.api.connector.entrypoint.EntrypointConnectorConfiguration;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
public class WebhookEntrypointConnectorConfiguration implements EntrypointConnectorConfiguration {

    /**
     *
     */
    private String type;

    /**
     * The callback URL called by the entrypoint on a message.
     */
    private String callbackUrl;

    /**
     * The list of headers to add to the request to the callback URL.
     */
    private Map<String, String> headers;

    /**
     * The authentication method to reach the callback URL.
     */
    private Authentication authentication;

    /**
     * The retry capabilities to use in case of failure.
     */
    private Retry retry;
}
