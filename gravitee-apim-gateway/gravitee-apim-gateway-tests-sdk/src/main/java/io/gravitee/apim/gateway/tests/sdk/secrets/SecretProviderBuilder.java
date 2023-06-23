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
package io.gravitee.apim.gateway.tests.sdk.secrets;

import io.gravitee.node.api.secrets.SecretManagerConfiguration;
import io.gravitee.node.api.secrets.SecretProviderFactory;
import io.gravitee.node.secrets.plugins.SecretProviderPlugin;
import io.gravitee.node.secrets.plugins.internal.DefaultSecretProviderPlugin;
import io.gravitee.plugin.core.api.PluginManifest;
import java.net.URL;
import java.nio.file.Path;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SecretProviderBuilder {

    public static <T extends SecretProviderFactory<C>, C extends SecretManagerConfiguration> SecretProviderPlugin<T, C> build(
        String id,
        Class<T> factoryClass,
        Class<C> configurationClass
    ) {
        return new DefaultSecretProviderPlugin<>(
            new SecretProviderPlugin<>() {
                @Override
                public Class<SecretManagerConfiguration> configuration() {
                    return (Class<SecretManagerConfiguration>) configurationClass;
                }

                @Override
                public String id() {
                    return id;
                }

                @Override
                public Class<SecretProviderFactory> secretProviderFactory() {
                    return null;
                }

                @Override
                public String clazz() {
                    return factoryClass.getName();
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
                public boolean deployed() {
                    return true;
                }
            },
            factoryClass,
            configurationClass
        );
    }
}
