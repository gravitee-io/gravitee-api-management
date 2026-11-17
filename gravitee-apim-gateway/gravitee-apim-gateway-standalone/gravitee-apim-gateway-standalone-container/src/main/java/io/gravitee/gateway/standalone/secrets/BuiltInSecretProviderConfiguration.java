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
package io.gravitee.gateway.standalone.secrets;

import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.OrganizationKeyRepository;
import io.gravitee.repository.management.api.SecretRepository;
import io.gravitee.secrets.api.plugin.EnvironmentAwareSecretProvider;
import lombok.CustomLog;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@CustomLog
@Configuration
public class BuiltInSecretProviderConfiguration {

    @Bean
    @Lazy
    public EnvironmentAwareSecretProvider builtInSecretProvider(
        @Value("${" + BuiltInSecretProvider.KEY_PROPERTY + ":#{null}}") String base64Kek,
        ObjectProvider<SecretRepository> secretRepository,
        ObjectProvider<OrganizationKeyRepository> organizationKeyRepository,
        ObjectProvider<EnvironmentRepository> environmentRepository
    ) {
        SecretRepository secrets = secretRepository.getIfAvailable();
        OrganizationKeyRepository organizationKeys = organizationKeyRepository.getIfAvailable();
        if (secrets == null || organizationKeys == null) {
            log.info("Built-in secret provider [{}]: SKIPPED, the management repository does not store secrets", BuiltInSecretProvider.ID);
            return null;
        }
        return new BuiltInSecretProvider(base64Kek, secrets, organizationKeys, environmentRepository.getObject());
    }
}
