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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.gravitee.definition.model.v4.http.HttpClientOptions;
import io.gravitee.definition.model.v4.http.HttpProxyOptions;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.jupiter.api.connector.entrypoint.EntrypointConnectorConfiguration;
import io.gravitee.gateway.jupiter.api.context.DeploymentContext;
import lombok.Getter;
import lombok.Setter;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
public class WebhookEntrypointConnectorConfiguration implements EntrypointConnectorConfiguration {

    @JsonProperty("proxy")
    private HttpProxyOptions proxyOptions;

    @JsonProperty("http")
    private HttpClientOptions httpOptions;

    /**
     * Evaluate the Connector configuration using the EL TemplateEngine
     *
     * @param deploymentContext Context that provides the TemplateEngine
     * @param configuration Connector configuration to evaluate
     * @return the configuration bean updated with evaluated settings
     * @param <T>
     */
    public static <T extends WebhookEntrypointConnectorConfiguration> T eval(
        final DeploymentContext deploymentContext,
        final T configuration
    ) {
        final TemplateEngine templateEngine = deploymentContext.getTemplateEngine();
        final HttpProxyOptions proxyOptions = configuration.getProxyOptions();

        if (proxyOptions != null) {
            proxyOptions.setHost(eval(templateEngine, proxyOptions.getHost()));
            proxyOptions.setUsername(eval(templateEngine, proxyOptions.getUsername()));
            proxyOptions.setPassword(eval(templateEngine, proxyOptions.getPassword()));
        }

        return configuration;
    }

    private static String eval(TemplateEngine templateEngine, String value) {
        if (value != null && !value.isEmpty() && templateEngine != null) {
            return templateEngine.getValue(value, String.class);
        }

        return value;
    }
}
