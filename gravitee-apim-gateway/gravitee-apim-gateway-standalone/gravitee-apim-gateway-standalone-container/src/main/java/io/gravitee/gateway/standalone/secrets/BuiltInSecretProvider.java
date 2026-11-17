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

import static io.gravitee.apim.common.secret.StaticKeyEncryptionKeyProvider.KEY_PROPERTY;

import io.gravitee.apim.common.secret.DataKeyResolver;
import io.gravitee.apim.common.secret.InvalidKeyEncryptionKeyException;
import io.gravitee.apim.common.secret.KeyEncryptionKeyProvider;
import io.gravitee.apim.common.secret.SecretCryptoException;
import io.gravitee.apim.common.secret.SecretEnvelope;
import io.gravitee.apim.common.secret.SecretScope;
import io.gravitee.apim.common.secret.StaticKeyEncryptionKeyProvider;
import io.gravitee.apim.common.secret.StoredDataKey;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EnvironmentRepository;
import io.gravitee.repository.management.api.OrganizationKeyRepository;
import io.gravitee.repository.management.api.SecretRepository;
import io.gravitee.repository.management.model.Environment;
import io.gravitee.repository.management.model.Secret;
import io.gravitee.secrets.api.core.SecretMap;
import io.gravitee.secrets.api.core.SecretURL;
import io.gravitee.secrets.api.errors.SecretManagerException;
import io.gravitee.secrets.api.plugin.EnvironmentAwareSecretProvider;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.CustomLog;

@CustomLog
public class BuiltInSecretProvider implements EnvironmentAwareSecretProvider {

    public static final String ID = "gravitee";
    private static final String VALUE_KEY = "value";

    private final String base64Kek;
    private final SecretRepository secretRepository;
    private final OrganizationKeyRepository organizationKeyRepository;
    private final EnvironmentRepository environmentRepository;
    private final Map<String, String> organizationOfEnvironment = new ConcurrentHashMap<>();
    private DataKeyResolver dataKeys;

    public BuiltInSecretProvider(
        String base64Kek,
        SecretRepository secretRepository,
        OrganizationKeyRepository organizationKeyRepository,
        EnvironmentRepository environmentRepository
    ) {
        this.base64Kek = base64Kek;
        this.secretRepository = secretRepository;
        this.organizationKeyRepository = organizationKeyRepository;
        this.environmentRepository = environmentRepository;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public EnvironmentAwareSecretProvider start() {
        KeyEncryptionKeyProvider kek;
        try {
            kek = StaticKeyEncryptionKeyProvider.fromBase64(base64Kek);
        } catch (InvalidKeyEncryptionKeyException e) {
            throw new SecretManagerException(KEY_PROPERTY + " " + e.getMessage());
        }
        Set<String> storedKekIds;
        try {
            storedKekIds = organizationKeyRepository.findKekIds();
        } catch (TechnicalException e) {
            throw new SecretManagerException("Failed to read the stored data keys", e);
        }
        if (!storedKekIds.isEmpty() && storedKekIds.stream().noneMatch(kek::canUnwrap)) {
            throw new SecretManagerException(KEY_PROPERTY + " does not match the key that encrypted the stored secrets");
        }
        if (!storedKekIds.stream().allMatch(kek::canUnwrap)) {
            log.warn(
                "{} did not wrap the data key of every organization: the organizations it did not wrap cannot read their secrets",
                KEY_PROPERTY
            );
        }
        dataKeys = new DataKeyResolver(kek, this::findStoredDataKey);
        return this;
    }

    @Override
    public Maybe<SecretMap> resolve(String environmentId, SecretURL secretURL) {
        return Maybe.fromCallable(() -> lookup(environmentId, secretURL.path())).onErrorResumeNext(e ->
            Maybe.error(
                e instanceof SecretManagerException
                    ? e
                    : new SecretManagerException(
                        "Failed to resolve secret [" + secretURL.path() + "] of environment [" + environmentId + "]",
                        e
                    )
            )
        );
    }

    private SecretMap lookup(String environmentId, String key) throws TechnicalException {
        Optional<String> organizationId = organizationOf(environmentId);
        if (organizationId.isEmpty()) {
            return null;
        }
        Optional<Secret> secret = secretRepository.findByKey(organizationId.get(), environmentId, key);
        if (secret.isEmpty()) {
            return null;
        }
        return SecretMap.of(Map.of(VALUE_KEY, decrypt(secret.get())));
    }

    private String decrypt(Secret secret) {
        try {
            var dataKey = dataKeys
                .resolve(secret.getOrganizationId())
                .orElseThrow(() ->
                    new SecretManagerException(
                        "Organization [" + secret.getOrganizationId() + "] has no data key to decrypt secret [" + secret.getId() + "]"
                    )
                );
            var scope = new SecretScope(secret.getOrganizationId(), secret.getEnvironmentId(), secret.getId());
            return SecretEnvelope.open(dataKey, scope, secret.getEncryptedValue());
        } catch (SecretCryptoException e) {
            throw new SecretManagerException(e.getMessage(), e);
        }
    }

    private Optional<String> organizationOf(String environmentId) throws TechnicalException {
        String cached = organizationOfEnvironment.get(environmentId);
        if (cached != null) {
            return Optional.of(cached);
        }
        Optional<String> organizationId = environmentRepository.findById(environmentId).map(Environment::getOrganizationId);
        organizationId.ifPresent(id -> organizationOfEnvironment.put(environmentId, id));
        return organizationId;
    }

    private Optional<StoredDataKey> findStoredDataKey(String organizationId) {
        try {
            return organizationKeyRepository
                .findByOrganizationId(organizationId)
                .map(key -> new StoredDataKey(key.getId(), key.getKekId(), key.getWrappedKey()));
        } catch (TechnicalException e) {
            throw new SecretManagerException("Failed to read the data key of organization [" + organizationId + "]", e);
        }
    }
}
