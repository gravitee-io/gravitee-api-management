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
import java.util.Optional;
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

    private static final String NO_MANAGEMENT_REPOSITORY = "none";

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
        factory.connect(
            secretRepository,
            organizationKeyRepository,
            environmentRepository,
            () -> environment.getProperty(StaticKeyEncryptionKeyProvider.KEY_PROPERTY),
            GraviteeSecretProviderEntries.misconfiguration(environment).or(() -> withoutManagementRepository(environment))
        );
        log.info("Gravitee secret provider [{}]: REGISTERED", GraviteeSecretProvider.ID);
        return factory;
    }

    // The none repository registers secret repositories that find nothing, so the provider cannot tell it apart by its beans.
    private static Optional<String> withoutManagementRepository(ConfigurableEnvironment environment) {
        String type = environment.getProperty("management.type", environment.getProperty("repositories.management.type", ""));
        return NO_MANAGEMENT_REPOSITORY.equalsIgnoreCase(type) ? Optional.of(GraviteeSecretProvider.NO_SECRET_STORE) : Optional.empty();
    }
}
