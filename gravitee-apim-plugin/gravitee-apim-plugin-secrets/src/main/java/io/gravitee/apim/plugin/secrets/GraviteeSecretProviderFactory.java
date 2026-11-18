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

import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.OrganizationKeyRepository;
import io.gravitee.repository.management.api.SecretRepository;
import io.gravitee.secrets.api.errors.SecretManagerException;
import io.gravitee.secrets.api.plugin.SecretProvider;
import io.gravitee.secrets.api.plugin.SecretProviderFactory;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;

/**
 * The plugin manager instantiates this factory reflectively, so the repositories cannot be constructor arguments. They
 * are looked up lazily because the management repository plugin registers them after the factory exists.
 */
public class GraviteeSecretProviderFactory implements SecretProviderFactory<GraviteeSecretManagerConfiguration> {

    private volatile ObjectProvider<SecretRepository> secretRepository;
    private volatile ObjectProvider<OrganizationKeyRepository> organizationKeyRepository;
    private volatile ObjectProvider<EnvironmentRepository> environmentRepository;
    private volatile Supplier<String> sharedEncryptionKey;

    void connect(
        ObjectProvider<SecretRepository> secretRepository,
        ObjectProvider<OrganizationKeyRepository> organizationKeyRepository,
        ObjectProvider<EnvironmentRepository> environmentRepository,
        Supplier<String> sharedEncryptionKey
    ) {
        this.secretRepository = secretRepository;
        this.organizationKeyRepository = organizationKeyRepository;
        this.environmentRepository = environmentRepository;
        this.sharedEncryptionKey = sharedEncryptionKey;
    }

    @Override
    public SecretProvider create(GraviteeSecretManagerConfiguration configuration) {
        if (secretRepository == null) {
            throw new SecretManagerException("Secret provider [" + GraviteeSecretProvider.ID + "] is not connected to the gateway");
        }
        return new GraviteeSecretProvider(
            encryptionKey(configuration),
            secretRepository.getIfAvailable(),
            organizationKeyRepository.getIfAvailable(),
            environmentRepository.getIfAvailable()
        );
    }

    private String encryptionKey(GraviteeSecretManagerConfiguration configuration) {
        String key = configuration.encryptionKey();
        return key == null || key.isBlank() ? sharedEncryptionKey.get() : key;
    }
}
