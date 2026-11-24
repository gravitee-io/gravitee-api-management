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

import io.gravitee.apim.common.secret.SecretChangedEvent;
import io.gravitee.apim.common.secret.StaticKeyEncryptionKeyProvider;
import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.api.genericevent.GenericEventRegistry;
import io.gravitee.node.secrets.plugins.SecretProviderPluginManager;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.OrganizationKeyRepository;
import io.gravitee.repository.management.api.SecretRepository;
import io.gravitee.repository.management.api.SecretValueRepository;
import java.util.Optional;
import lombok.CustomLog;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Registration must happen while the gateway context starts: the secrets service starts afterwards and deploys the
 * provider from its {@code api.secrets.providers} entry.
 */
@CustomLog
@Configuration
public class GraviteeSecretProviderConfiguration {

    private static final String NO_MANAGEMENT_REPOSITORY = "none";
    private static final String BRIDGE_CLIENT = "http";

    @Bean
    public GraviteeSecretProviderFactory graviteeSecretProviderFactory(
        SecretProviderPluginManager pluginManager,
        ConfigurableEnvironment environment,
        ObjectProvider<SecretRepository> secretRepository,
        ObjectProvider<OrganizationKeyRepository> organizationKeyRepository,
        ObjectProvider<EnvironmentRepository> environmentRepository,
        ObjectProvider<SecretValueRepository> secretValueRepository
    ) {
        pluginManager.register(new GraviteeSecretProviderPlugin());
        GraviteeSecretProviderFactory factory = pluginManager.getFactoryById(GraviteeSecretProvider.ID);
        factory.connect(
            secretRepository,
            organizationKeyRepository,
            environmentRepository,
            secretValueRepository,
            BRIDGE_CLIENT.equalsIgnoreCase(managementType(environment)),
            () -> environment.getProperty(StaticKeyEncryptionKeyProvider.KEY_PROPERTY),
            GraviteeSecretProviderEntries.misconfiguration(environment).or(() -> withoutManagementRepository(environment))
        );
        log.info("Gravitee secret provider [{}]: REGISTERED", GraviteeSecretProvider.ID);
        return factory;
    }

    // Lazy: the management repository plugin registers the repositories after this context starts.
    @Bean
    @Lazy
    @Conditional(WithManagementDatabase.class)
    public SecretValueRepository graviteeSecretValueRepository(
        ConfigurableEnvironment environment,
        ObjectProvider<SecretRepository> secretRepository,
        ObjectProvider<OrganizationKeyRepository> organizationKeyRepository,
        ObjectProvider<EnvironmentRepository> environmentRepository
    ) {
        return new DecryptingSecretValueRepository(
            environment.getProperty(StaticKeyEncryptionKeyProvider.KEY_PROPERTY),
            secretRepository.getIfAvailable(),
            organizationKeyRepository.getIfAvailable(),
            environmentRepository.getIfAvailable()
        );
    }

    @Bean(destroyMethod = "cancel")
    public GenericEventRegistry.Subscription graviteeSecretChangeSubscription(GenericEventRegistry registry, EventManager eventManager) {
        return registry.subscribe(SecretChangedEvent.KIND, new GraviteeSecretChangeSubscriber(eventManager));
    }

    // The none repository registers secret repositories that find nothing, so the provider cannot tell it apart by its beans.
    private static Optional<String> withoutManagementRepository(ConfigurableEnvironment environment) {
        return NO_MANAGEMENT_REPOSITORY.equalsIgnoreCase(managementType(environment))
            ? Optional.of(GraviteeSecretProvider.NO_SECRET_STORE)
            : Optional.empty();
    }

    private static String managementType(Environment environment) {
        return environment.getProperty("management.type", environment.getProperty("repositories.management.type", ""));
    }

    // A bridge server in this gateway serves these values; a bridge client registers its own.
    static class WithManagementDatabase implements Condition {

        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            String type = managementType(context.getEnvironment());
            return !BRIDGE_CLIENT.equalsIgnoreCase(type) && !NO_MANAGEMENT_REPOSITORY.equalsIgnoreCase(type);
        }
    }
}
