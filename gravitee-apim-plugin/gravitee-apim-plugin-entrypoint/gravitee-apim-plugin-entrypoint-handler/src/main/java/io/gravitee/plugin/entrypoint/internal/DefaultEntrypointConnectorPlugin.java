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
package io.gravitee.plugin.entrypoint.internal;

import io.gravitee.gateway.reactive.api.connector.entrypoint.EntrypointConnectorConfiguration;
import io.gravitee.gateway.reactive.api.connector.entrypoint.EntrypointConnectorFactory;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.core.api.PluginManifest;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import java.net.URL;
import java.nio.file.Path;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
class DefaultEntrypointConnectorPlugin<T extends EntrypointConnectorFactory<?>, U extends EntrypointConnectorConfiguration>
    implements EntrypointConnectorPlugin<T, U> {

    private final Plugin plugin;
    private final Class<T> entrypointConnectorFactoryClass;
    private final Class<U> entrypointConnectorConfigurationClass;

    DefaultEntrypointConnectorPlugin(
        final Plugin plugin,
        final Class<T> entrypointConnectorFactoryClass,
        final Class<U> entryPointConnectorConfigurationClass
    ) {
        this.plugin = plugin;
        this.entrypointConnectorFactoryClass = entrypointConnectorFactoryClass;
        this.entrypointConnectorConfigurationClass = entryPointConnectorConfigurationClass;
    }

    @Override
    public Class<T> connectorFactory() {
        return entrypointConnectorFactoryClass;
    }

    @Override
    public String clazz() {
        return plugin.clazz();
    }

    @Override
    public URL[] dependencies() {
        return plugin.dependencies();
    }

    @Override
    public String id() {
        return plugin.id();
    }

    @Override
    public PluginManifest manifest() {
        return plugin.manifest();
    }

    @Override
    public Path path() {
        return plugin.path();
    }

    @Override
    public Class<U> configuration() {
        return entrypointConnectorConfigurationClass;
    }

    @Override
    public boolean deployed() {
        return plugin.deployed();
    }
}
