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
import io.gravitee.gateway.reactive.api.ConnectorMode;
import io.gravitee.gateway.reactive.api.connector.endpoint.sync.HttpEndpointSyncConnectorFactory;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.helper.PluginConfigurationHelper;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorConfiguration;
import io.gravitee.plugin.endpoint.http.proxy.configuration.HttpProxyEndpointConnectorSharedConfiguration;
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
public class HttpProxyEndpointConnectorFactory implements HttpEndpointSyncConnectorFactory {

    private final PluginConfigurationHelper connectorFactoryHelper;

    @Override
    public Set<ConnectorMode> supportedModes() {
        return HttpProxyEndpointConnector.SUPPORTED_MODES;
    }

    @Override
    public HttpProxyEndpointConnector createConnector(
        final DeploymentContext deploymentContext,
        final String configuration,
        final String sharedConfiguration
    ) {
        try {
            return new HttpProxyEndpointConnector(
                eval(
                    deploymentContext,
                    connectorFactoryHelper.readConfiguration(HttpProxyEndpointConnectorConfiguration.class, configuration)
                ),
                eval(
                    deploymentContext,
                    connectorFactoryHelper.readConfiguration(HttpProxyEndpointConnectorSharedConfiguration.class, sharedConfiguration)
                )
            );
        } catch (Exception e) {
            log.error("Can't create connector because no valid configuration", e);
            return null;
        }
    }

    private HttpProxyEndpointConnectorConfiguration eval(
        final DeploymentContext deploymentContext,
        final HttpProxyEndpointConnectorConfiguration configuration
    ) {
        configuration.setTarget(eval(deploymentContext.getTemplateEngine(), configuration.getTarget()));
        return configuration;
    }

    private HttpProxyEndpointConnectorSharedConfiguration eval(
        final DeploymentContext deploymentContext,
        final HttpProxyEndpointConnectorSharedConfiguration groupConfiguration
    ) {
        final TemplateEngine templateEngine = deploymentContext.getTemplateEngine();
        final HttpProxyOptions proxyOptions = groupConfiguration.getProxyOptions();
        final List<HttpHeader> headers = groupConfiguration.getHeaders();

        if (proxyOptions != null) {
            proxyOptions.setHost(eval(templateEngine, proxyOptions.getHost()));
            proxyOptions.setUsername(eval(templateEngine, proxyOptions.getUsername()));
            proxyOptions.setPassword(eval(templateEngine, proxyOptions.getPassword()));
        }

        if (headers != null && !headers.isEmpty()) {
            headers.forEach(httpHeader -> httpHeader.setValue(eval(templateEngine, httpHeader.getValue())));
        }

        return groupConfiguration;
    }

    private String eval(TemplateEngine templateEngine, String value) {
        if (value != null && !value.isEmpty()) {
            return templateEngine.convert(value);
        }

        return value;
    }
}
