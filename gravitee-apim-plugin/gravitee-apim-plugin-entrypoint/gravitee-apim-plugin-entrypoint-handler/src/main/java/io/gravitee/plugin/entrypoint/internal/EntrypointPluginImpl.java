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
package io.gravitee.plugin.entrypoint.internal;

import io.gravitee.gateway.jupiter.api.entrypoint.EntrypointConnectorConfiguration;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.core.api.PluginManifest;
import io.gravitee.plugin.entrypoint.EntrypointPlugin;
import java.net.URL;
import java.nio.file.Path;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
class EntrypointPluginImpl implements EntrypointPlugin {

    private final Plugin plugin;
    private final Class<?> entryPointConnectorClass;
    private final Class<? extends EntrypointConnectorConfiguration> entrypointConnectorConfigurationClass;

    EntrypointPluginImpl(
        final Plugin plugin,
        final Class<?> entrypointConnectorClass,
        final Class<? extends EntrypointConnectorConfiguration> entryPointConnectorConfigurationClass
    ) {
        this.plugin = plugin;
        this.entryPointConnectorClass = entrypointConnectorClass;
        this.entrypointConnectorConfigurationClass = entryPointConnectorConfigurationClass;
    }

    @Override
    public Class<?> entrypointConnectorFactory() {
        return entryPointConnectorClass;
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
    public Class<? extends EntrypointConnectorConfiguration> configuration() {
        return entrypointConnectorConfigurationClass;
    }
}
