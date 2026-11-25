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
import io.gravitee.repository.management.api.SecretValueRepository;
import io.gravitee.secrets.api.errors.SecretManagerException;
import io.gravitee.secrets.api.plugin.SecretProvider;
import io.gravitee.secrets.api.plugin.SecretProviderFactory;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;

/**
 * The plugin manager instantiates this factory reflectively, so the repositories cannot be constructor arguments. They
 * are looked up lazily because the management repository plugin registers them after the factory exists.
 */
public class GraviteeSecretProviderFactory implements SecretProviderFactory<GraviteeSecretManagerConfiguration> {

    static final String NO_BRIDGE_SECRETS =
        "Secret provider [" +
        GraviteeSecretProvider.ID +
        "] cannot read secrets: the bridge client does not serve secrets, it must be version 10.1.0 or later";

    private final AtomicReference<Connection> connection = new AtomicReference<>();

    void connect(
        ObjectProvider<SecretRepository> secretRepository,
        ObjectProvider<OrganizationKeyRepository> organizationKeyRepository,
        ObjectProvider<EnvironmentRepository> environmentRepository,
        ObjectProvider<SecretValueRepository> secretValueRepository,
        boolean throughBridge,
        Supplier<String> sharedEncryptionKey,
        Optional<String> misconfiguration
    ) {
        connection.set(
            new Connection(
                secretRepository,
                organizationKeyRepository,
                environmentRepository,
                secretValueRepository,
                throughBridge,
                sharedEncryptionKey,
                misconfiguration
            )
        );
    }

    @Override
    public SecretProvider create(GraviteeSecretManagerConfiguration configuration) {
        Connection connected = connection.get();
        if (connected == null) {
            throw new SecretManagerException("Secret provider [" + GraviteeSecretProvider.ID + "] is not connected to the gateway");
        }
        if (connected.misconfiguration().isPresent()) {
            return GraviteeSecretProvider.refusing(connected.misconfiguration().get());
        }
        if (connected.throughBridge()) {
            SecretValueRepository bridge = connected.secretValueRepository().getIfAvailable();
            return bridge == null ? GraviteeSecretProvider.refusing(NO_BRIDGE_SECRETS) : GraviteeSecretProvider.throughBridge(bridge);
        }
        if (connected.secretValueRepository().getIfAvailable() instanceof DecryptingSecretValueRepository secretValues) {
            return GraviteeSecretProvider.decrypting(secretValues);
        }
        return GraviteeSecretProvider.decrypting(
            encryptionKey(configuration, connected.sharedEncryptionKey()),
            connected.secretRepository().getIfAvailable(),
            connected.organizationKeyRepository().getIfAvailable(),
            connected.environmentRepository().getIfAvailable()
        );
    }

    private static String encryptionKey(GraviteeSecretManagerConfiguration configuration, Supplier<String> sharedEncryptionKey) {
        String key = configuration.encryptionKey();
        return key == null || key.isBlank() ? sharedEncryptionKey.get() : key;
    }

    private record Connection(
        ObjectProvider<SecretRepository> secretRepository,
        ObjectProvider<OrganizationKeyRepository> organizationKeyRepository,
        ObjectProvider<EnvironmentRepository> environmentRepository,
        ObjectProvider<SecretValueRepository> secretValueRepository,
        boolean throughBridge,
        Supplier<String> sharedEncryptionKey,
        Optional<String> misconfiguration
    ) {}
}
