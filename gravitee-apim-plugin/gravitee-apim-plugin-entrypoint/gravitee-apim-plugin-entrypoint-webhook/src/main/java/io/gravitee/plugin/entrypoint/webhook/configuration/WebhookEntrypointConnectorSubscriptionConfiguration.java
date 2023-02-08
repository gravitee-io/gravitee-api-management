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
package io.gravitee.plugin.entrypoint.webhook.configuration;

import io.gravitee.definition.model.v4.ssl.SslOptions;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author GraviteeSource Team
 */
@Getter
@Setter
@ToString
public class WebhookEntrypointConnectorSubscriptionConfiguration {

    /**
     * The callback URL called by the entrypoint on a message.
     */
    private String callbackUrl;

    /**
     * The list of headers to add to the request to the callback URL.
     */
    private List<HttpHeader> headers;

    /**
     * Ssl options.
     */
    private SslOptions ssl = new SslOptions();
}
