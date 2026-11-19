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
import io.gravitee.secrets.api.core.SecretEvent;
import io.gravitee.secrets.api.core.SecretMap;
import io.gravitee.secrets.api.core.SecretURL;
import io.gravitee.secrets.api.errors.SecretManagerException;
import io.gravitee.secrets.api.plugin.SecretProvider;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.CustomLog;

@CustomLog
public class GraviteeSecretProvider implements SecretProvider {

    public static final String ID = "gravitee";
    private static final String KEY_SETTING =
        "Secret provider [" +
        ID +
        "] " +
        GraviteeSecretManagerConfiguration.ENCRYPTION_KEY_PROPERTY +
        " (or " +
        StaticKeyEncryptionKeyProvider.KEY_PROPERTY +
        ")";
    private static final String VALUE_KEY = "value";

    private final String base64Kek;
    private final String environmentId;
    private final SecretRepository secretRepository;
    private final OrganizationKeyRepository organizationKeyRepository;
    private final EnvironmentRepository environmentRepository;
    private final Map<String, String> organizationOfEnvironment = new ConcurrentHashMap<>();
    private volatile DataKeyResolver dataKeys;
    private volatile String refusal;

    public GraviteeSecretProvider(
        String base64Kek,
        String environmentId,
        SecretRepository secretRepository,
        OrganizationKeyRepository organizationKeyRepository,
        EnvironmentRepository environmentRepository
    ) {
        this.base64Kek = base64Kek;
        this.environmentId = environmentId;
        this.secretRepository = secretRepository;
        this.organizationKeyRepository = organizationKeyRepository;
        this.environmentRepository = environmentRepository;
    }

    static GraviteeSecretProvider refusing(String reason) {
        var provider = new GraviteeSecretProvider(null, null, null, null, null);
        provider.refusal = reason;
        return provider;
    }

    /**
     * Never throws: the secrets service stops, with every other secret provider, when a provider fails to start. A
     * provider that cannot serve logs why once and refuses every secret instead.
     */
    @Override
    public SecretProvider start() {
        if (refusal != null) {
            log.error("{}: it refuses every secret until this is fixed", refusal);
            return this;
        }
        try {
            dataKeys = new DataKeyResolver(verifiedKeyEncryptionKey(), this::findStoredDataKey);
        } catch (SecretManagerException e) {
            refusal = e.getMessage();
            log.error("{}: it refuses every secret until this is fixed", refusal, e.getCause());
        }
        return this;
    }

    private KeyEncryptionKeyProvider verifiedKeyEncryptionKey() {
        if (secretRepository == null || organizationKeyRepository == null || environmentRepository == null) {
            throw new SecretManagerException(
                "Secret provider [" + ID + "] cannot read secrets: the management repository does not store secrets"
            );
        }
        KeyEncryptionKeyProvider kek;
        try {
            kek = StaticKeyEncryptionKeyProvider.fromBase64(base64Kek);
        } catch (InvalidKeyEncryptionKeyException e) {
            throw new SecretManagerException(KEY_SETTING + " " + e.getMessage());
        }
        Set<String> storedKekIds;
        try {
            storedKekIds = organizationKeyRepository.findKekIds();
        } catch (TechnicalException | RuntimeException e) {
            throw new SecretManagerException("Secret provider [" + ID + "] failed to read the stored data keys", e);
        }
        if (!storedKekIds.isEmpty() && storedKekIds.stream().noneMatch(kek::canUnwrap)) {
            throw new SecretManagerException(KEY_SETTING + " does not match the key that encrypted the stored secrets");
        }
        if (!storedKekIds.stream().allMatch(kek::canUnwrap)) {
            log.warn(
                "{} did not wrap the data key of every organization: the organizations it did not wrap cannot read their secrets",
                KEY_SETTING
            );
        }
        return kek;
    }

    @Override
    public Maybe<SecretMap> resolve(SecretURL secretURL) {
        String reason = refusal;
        if (reason != null) {
            return Maybe.error(new SecretManagerException(reason));
        }
        return Maybe.fromCallable(() -> {
            String path = secretURL.path();
            int separator = path.indexOf('/');
            if (separator <= 0 || separator == path.length() - 1) {
                throw new SecretManagerException("Secret reference [" + path + "] must have the form /" + ID + "/{envId}/{key}");
            }
            String referencedEnvironmentId = path.substring(0, separator);
            if (!referencedEnvironmentId.equals(environmentId)) {
                throw new SecretManagerException(
                    "Secret reference [" +
                        path +
                        "] names environment [" +
                        referencedEnvironmentId +
                        "], but secret provider [" +
                        ID +
                        "] serves environment [" +
                        environmentId +
                        "] only"
                );
            }
            String key = path.substring(separator + 1);
            try {
                return lookup(environmentId, key);
            } catch (SecretManagerException e) {
                throw e;
            } catch (Exception e) {
                throw new SecretManagerException("Failed to resolve secret [" + key + "] of environment [" + environmentId + "]", e);
            }
        });
    }

    @Override
    public Flowable<SecretEvent> watch(SecretURL secretURL) {
        return Flowable.error(new SecretManagerException("The " + ID + " secret provider does not support watching secrets"));
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
