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
package io.gravitee.plugin.endpoint.http.proxy;

import io.gravitee.common.http.HttpHeader;
import io.gravitee.definition.model.v4.http.HttpProxyOptions;
import io.gravitee.el.TemplateEngine;
import io.gravitee.gateway.jupiter.api.ConnectorMode;
import io.gravitee.gateway.jupiter.api.connector.ConnectorHelper;
import io.gravitee.gateway.jupiter.api.connector.endpoint.sync.EndpointSyncConnectorFactory;
import io.gravitee.gateway.jupiter.api.context.DeploymentContext;
import io.gravitee.gateway.jupiter.api.exception.PluginConfigurationException;
import io.gravitee.gateway.jupiter.reactor.handler.context.DefaultDeploymentContext;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorConfiguration;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@AllArgsConstructor
public class HttpProxyEndpointConnectorFactory implements EndpointSyncConnectorFactory<HttpProxyEndpointConnector> {

    private final ConnectorHelper connectorFactoryHelper;

    @Override
    public Set<ConnectorMode> supportedModes() {
        return HttpProxyEndpointConnector.SUPPORTED_MODES;
    }

    public HttpProxyEndpointConnector createConnector(final DeploymentContext deploymentContext, final String configuration) {
        try {
            return new HttpProxyEndpointConnector(
                eval(
                    deploymentContext,
                    connectorFactoryHelper.readConfiguration(HttpProxyEndpointConnectorConfiguration.class, configuration)
                )
            );
        } catch (Exception e) {
            log.error("Can't create connector cause no valid configuration", e);
            return null;
        }
    }

    private HttpProxyEndpointConnectorConfiguration eval(
        final DeploymentContext deploymentContext,
        final HttpProxyEndpointConnectorConfiguration configuration
    ) {
        final TemplateEngine templateEngine = deploymentContext.getTemplateEngine();
        final HttpProxyOptions proxyOptions = configuration.getProxyOptions();
        final List<HttpHeader> headers = configuration.getHeaders();

        configuration.setTarget(eval(templateEngine, configuration.getTarget()));

        if (proxyOptions != null) {
            proxyOptions.setHost(eval(templateEngine, proxyOptions.getHost()));
            proxyOptions.setUsername(eval(templateEngine, proxyOptions.getUsername()));
            proxyOptions.setPassword(eval(templateEngine, proxyOptions.getPassword()));
        }

        if (headers != null && !headers.isEmpty()) {
            headers.forEach(httpHeader -> httpHeader.setValue(eval(templateEngine, httpHeader.getValue())));
        }

        return configuration;
    }

    private String eval(TemplateEngine templateEngine, String value) {
        if (value != null && !value.isEmpty()) {
            return templateEngine.convert(value);
        }

        return value;
    }
}
