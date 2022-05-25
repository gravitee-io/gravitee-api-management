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
package io.gravitee.apim.gateway.tests.sdk.connector;

import io.gravitee.connector.api.ConnectorConfiguration;
import io.gravitee.plugin.connector.ConnectorPlugin;
import io.gravitee.plugin.core.api.PluginManifest;
import java.net.URL;
import java.nio.file.Path;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ConnectorBuilder {

    private ConnectorBuilder() {
        throw new IllegalStateException("Utility class");
    }

    public static ConnectorPlugin<?> build(String id, Class<?> connector) {
        return build(id, connector, null);
    }

    public static ConnectorPlugin<?> build(String id, Class<?> connector, Class<? extends ConnectorConfiguration> connectorConfiguration) {
        return new ConnectorPlugin<>() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public String clazz() {
                return connector.getName();
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
            public Class configuration() {
                return connectorConfiguration;
            }

            @Override
            public Class<?> connector() {
                return connector;
            }
        };
    }
}
