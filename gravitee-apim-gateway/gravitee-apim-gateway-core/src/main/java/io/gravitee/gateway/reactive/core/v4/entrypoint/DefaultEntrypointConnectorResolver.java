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
package io.gravitee.gateway.reactive.core.v4.entrypoint;

import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_LISTENER_TYPE;

import io.gravitee.common.service.AbstractService;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.gateway.reactive.api.ApiType;
import io.gravitee.gateway.reactive.api.connector.entrypoint.BaseEntrypointConnector;
import io.gravitee.gateway.reactive.api.connector.entrypoint.EntrypointConnector;
import io.gravitee.gateway.reactive.api.connector.entrypoint.EntrypointConnectorFactory;
import io.gravitee.gateway.reactive.api.connector.entrypoint.HttpEntrypointConnector;
import io.gravitee.gateway.reactive.api.connector.entrypoint.async.EntrypointAsyncConnector;
import io.gravitee.gateway.reactive.api.connector.entrypoint.async.EntrypointAsyncConnectorFactory;
import io.gravitee.gateway.reactive.api.connector.entrypoint.async.HttpEntrypointAsyncConnector;
import io.gravitee.gateway.reactive.api.connector.entrypoint.async.HttpEntrypointAsyncConnectorFactory;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.api.qos.Qos;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPluginManager;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@SuppressWarnings("unchecked")
public class DefaultEntrypointConnectorResolver extends AbstractService<DefaultEntrypointConnectorResolver> {

    private static final Logger log = LoggerFactory.getLogger(DefaultEntrypointConnectorResolver.class);
    private final List<BaseEntrypointConnector> entrypointConnectors;

    public DefaultEntrypointConnectorResolver(
        final Api api,
        final DeploymentContext deploymentContext,
        final EntrypointConnectorPluginManager entrypointConnectorPluginManager
    ) {
        entrypointConnectors =
            api
                .getListeners()
                .stream()
                .flatMap(listener -> listener.getEntrypoints().stream())
                .map(entrypoint ->
                    this.<BaseEntrypointConnector<?>>createConnector(deploymentContext, entrypointConnectorPluginManager, entrypoint)
                )
                .filter(Objects::nonNull)
                .sorted(Comparator.<BaseEntrypointConnector<?>>comparingInt(BaseEntrypointConnector::matchCriteriaCount).reversed())
                .collect(Collectors.toList());
    }

    private <T extends BaseEntrypointConnector<?>> T createConnector(
        final DeploymentContext deploymentContext,
        final EntrypointConnectorPluginManager entrypointConnectorPluginManager,
        final Entrypoint entrypoint
    ) {
        EntrypointConnectorFactory<?> connectorFactory = entrypointConnectorPluginManager.getFactoryById(entrypoint.getType());

        if (connectorFactory != null) {
            if (connectorFactory.supportedApi() == ApiType.MESSAGE) {
                HttpEntrypointAsyncConnectorFactory<HttpEntrypointAsyncConnector> entrypointAsyncConnectorFactory =
                    (HttpEntrypointAsyncConnectorFactory<HttpEntrypointAsyncConnector>) connectorFactory;
                Qos qos = Qos.AUTO;
                if (entrypoint.getQos() != null) {
                    qos = Qos.fromLabel(entrypoint.getQos().getLabel());
                }
                return (T) entrypointAsyncConnectorFactory.createConnector(deploymentContext, qos, entrypoint.getConfiguration());
            }
            return (T) connectorFactory.createConnector(deploymentContext, entrypoint.getConfiguration());
        }
        return null;
    }

    public <C extends BaseExecutionContext, T extends BaseEntrypointConnector<C>> T resolve(final C ctx) {
        for (BaseEntrypointConnector<C> connector : entrypointConnectors) {
            if (connector.supportedListenerType() == ctx.getInternalAttribute(ATTR_INTERNAL_LISTENER_TYPE) && connector.matches(ctx)) {
                return (T) connector;
            }
        }
        return null;
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();

        for (BaseEntrypointConnector<?> connector : entrypointConnectors) {
            try {
                connector.stop();
            } catch (Exception e) {
                log.warn("An error occurred when stopping entrypoint connector [{}].", connector.id());
            }
        }
    }

    @Override
    public DefaultEntrypointConnectorResolver preStop() throws Exception {
        super.preStop();

        for (BaseEntrypointConnector<?> connector : entrypointConnectors) {
            try {
                connector.preStop();
            } catch (Exception e) {
                log.warn("An error occurred when pre-stopping entrypoint connector [{}].", connector.id());
            }
        }

        return this;
    }
}
