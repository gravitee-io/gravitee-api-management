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
package io.gravitee.apim.plugin.secrets;

import io.gravitee.apim.common.secret.StaticKeyEncryptionKeyProvider;
import io.gravitee.node.secrets.plugins.SecretProviderPluginManager;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.OrganizationKeyRepository;
import io.gravitee.repository.management.api.SecretRepository;
import lombok.CustomLog;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Registration must happen while the gateway context starts: the secrets service starts afterwards and deploys the
 * provider from its {@code api.secrets.providers} entry.
 */
@CustomLog
@Configuration
public class GraviteeSecretProviderConfiguration {

    @Bean
    public GraviteeSecretProviderFactory graviteeSecretProviderFactory(
        SecretProviderPluginManager pluginManager,
        ConfigurableEnvironment environment,
        ObjectProvider<SecretRepository> secretRepository,
        ObjectProvider<OrganizationKeyRepository> organizationKeyRepository,
        ObjectProvider<EnvironmentRepository> environmentRepository
    ) {
        pluginManager.register(new GraviteeSecretProviderPlugin());
        GraviteeSecretProviderFactory factory = pluginManager.getFactoryById(GraviteeSecretProvider.ID);
        factory.connect(secretRepository, organizationKeyRepository, environmentRepository, () ->
            environment.getProperty(StaticKeyEncryptionKeyProvider.KEY_PROPERTY)
        );
        log.info("Gravitee secret provider [{}]: REGISTERED", GraviteeSecretProvider.ID);
        return factory;
    }
}
