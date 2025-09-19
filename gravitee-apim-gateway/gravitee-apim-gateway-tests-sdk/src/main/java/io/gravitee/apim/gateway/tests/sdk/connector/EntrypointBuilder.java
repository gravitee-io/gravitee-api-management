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
package io.gravitee.apim.gateway.tests.sdk.connector;

import io.gravitee.gateway.reactive.api.connector.entrypoint.EntrypointConnectorConfiguration;
import io.gravitee.gateway.reactive.api.connector.entrypoint.EntrypointConnectorFactory;
import io.gravitee.plugin.core.api.PluginManifest;
import io.gravitee.plugin.entrypoint.EntrypointConnectorPlugin;
import java.net.URL;
import java.nio.file.Path;

public class EntrypointBuilder {

    private EntrypointBuilder() {
        throw new IllegalStateException("Utility class");
    }

    public static <T extends EntrypointConnectorFactory<?>, U extends EntrypointConnectorConfiguration> EntrypointConnectorPlugin<
        T,
        U
    > build(String id, Class<T> entrypointConnectorFactory) {
        return build(id, entrypointConnectorFactory, null);
    }

    public static <T extends EntrypointConnectorFactory<?>, U extends EntrypointConnectorConfiguration> EntrypointConnectorPlugin<
        T,
        U
    > build(String id, Class<T> entrypointConnectorFactory, Class<U> entrypointConfiguration) {
        return new EntrypointConnectorPlugin<>() {
            @Override
            public Class<T> connectorFactory() {
                return entrypointConnectorFactory;
            }

            @Override
            public String id() {
                return id;
            }

            @Override
            public String clazz() {
                return entrypointConnectorFactory.getName();
            }

            @Override
            public Path path() {
                return null;
            }

            @Override
            public PluginManifest manifest() {
                return null;
            }

            @Override
            public URL[] dependencies() {
                return new URL[0];
            }

            @Override
            public Class<U> configuration() {
                return entrypointConfiguration;
            }

            @Override
            public boolean deployed() {
                return true;
            }
        };
    }
}
