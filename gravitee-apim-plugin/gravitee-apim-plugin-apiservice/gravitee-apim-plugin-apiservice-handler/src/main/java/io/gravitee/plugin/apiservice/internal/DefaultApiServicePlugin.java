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
package io.gravitee.plugin.apiservice.internal;

import io.gravitee.gateway.reactive.api.apiservice.ApiServiceConfiguration;
import io.gravitee.gateway.reactive.api.apiservice.ApiServiceFactory;
import io.gravitee.plugin.apiservice.ApiServicePlugin;
import io.gravitee.plugin.core.api.Plugin;
import io.gravitee.plugin.core.api.PluginManifest;
import java.net.URL;
import java.nio.file.Path;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class DefaultApiServicePlugin<T extends ApiServiceFactory, C extends ApiServiceConfiguration> implements ApiServicePlugin<T, C> {

    private final Plugin plugin;
    private final Class<T> apiServiceClass;
    private final Class<C> apiServiceConfigurationClass;

    DefaultApiServicePlugin(final Plugin plugin, final Class<T> ApiServiceFactoryClass, final Class<C> apiServiceConfigurationClass) {
        this.plugin = plugin;
        this.apiServiceClass = ApiServiceFactoryClass;
        this.apiServiceConfigurationClass = apiServiceConfigurationClass;
    }

    @Override
    public Class<T> connectorFactory() {
        return apiServiceClass;
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
    public Class<C> configuration() {
        return apiServiceConfigurationClass;
    }

    @Override
    public boolean deployed() {
        return plugin.deployed();
    }
}
