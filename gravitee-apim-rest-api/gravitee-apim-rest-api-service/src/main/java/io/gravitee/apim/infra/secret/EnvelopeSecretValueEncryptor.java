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
package io.gravitee.apim.infra.secret;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.gravitee.apim.common.secret.DataKey;
import io.gravitee.apim.common.secret.InvalidKeyEncryptionKeyException;
import io.gravitee.apim.common.secret.KeyEncryptionKeyProvider;
import io.gravitee.apim.common.secret.SecretCryptoException;
import io.gravitee.apim.common.secret.SecretEnvelope;
import io.gravitee.apim.common.secret.SecretScope;
import io.gravitee.apim.common.secret.StaticKeyEncryptionKeyProvider;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.secret.exception.SecretEncryptionUnavailableException;
import io.gravitee.apim.core.secret.model.Secret;
import io.gravitee.apim.core.secret.service_provider.SecretValueEncryptor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.OrganizationKeyRepository;
import io.gravitee.repository.management.model.OrganizationKey;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Envelope encryption: each organization's secret values are encrypted under its own data key, stored wrapped by the
 * key encryption key. A missing, invalid or mismatched key encryption key never stops the Management API: it disables
 * the secrets feature and every secret call fails with the reason.
 */
@CustomLog
@Service
public class EnvelopeSecretValueEncryptor implements SecretValueEncryptor {

    public static final String KEY_PROPERTY = "api.secrets.encryption.key";

    private static final String MISMATCH = "does not match the key that encrypted the stored secrets";
    // A data key never changes for its id, so the cache cannot go stale: the TTL only bounds how long a deleted organization's key stays in memory.
    private static final Duration DATA_KEY_TTL = Duration.ofHours(1);
    private static final long DATA_KEY_CACHE_SIZE = 10_000;

    private final KeyEncryptionKeyProvider kek;
    private final OrganizationKeyRepository organizationKeyRepository;
    private final Cache<String, DataKey> dataKeys = CacheBuilder.newBuilder()
        .expireAfterAccess(DATA_KEY_TTL)
        .maximumSize(DATA_KEY_CACHE_SIZE)
        .build();

    private volatile String unavailableReason;
    private volatile boolean storedKeysChecked;

    public EnvelopeSecretValueEncryptor(
        @Value("${" + KEY_PROPERTY + ":#{null}}") String base64Kek,
        @Lazy OrganizationKeyRepository organizationKeyRepository
    ) {
        this.organizationKeyRepository = organizationKeyRepository;
        this.kek = toProvider(base64Kek);
    }

    @Override
    public String encrypt(Secret secret, String plaintext) {
        var dataKey = dataKeyOf(secret.organizationId());
        try {
            return SecretEnvelope.seal(dataKey, scopeOf(secret), plaintext);
        } catch (SecretCryptoException e) {
            throw new TechnicalDomainException("Failed to encrypt the value of secret " + secret.id(), e);
        }
    }

    @Override
    public String decrypt(Secret secret, String ciphertext) {
        ensureAvailable();
        var dataKey = storedDataKeyOf(secret.organizationId()).orElseThrow(() ->
            new TechnicalDomainException("Organization " + secret.organizationId() + " has no data key to decrypt secret " + secret.id())
        );
        try {
            return SecretEnvelope.open(dataKey, scopeOf(secret), ciphertext);
        } catch (SecretCryptoException e) {
            throw new TechnicalDomainException("Failed to decrypt the value of secret " + secret.id(), e);
        }
    }

    DataKey dataKeyOf(String organizationId) {
        ensureAvailable();
        return storedDataKeyOf(organizationId).orElseGet(() -> createDataKey(organizationId));
    }

    private Optional<DataKey> storedDataKeyOf(String organizationId) {
        var cached = dataKeys.getIfPresent(organizationId);
        if (cached != null) {
            return Optional.of(cached);
        }
        return findStored(organizationId).map(this::unwrap);
    }

    private DataKey createDataKey(String organizationId) {
        var dataKey = DataKey.generate();
        var organizationKey = OrganizationKey.builder()
            .id(dataKey.id())
            .organizationId(organizationId)
            .wrappedKey(Base64.getEncoder().encodeToString(wrap(organizationId, dataKey)))
            .kekId(kek.id())
            .createdAt(Date.from(TimeProvider.instantNow()))
            .build();
        try {
            organizationKeyRepository.create(organizationKey);
            dataKeys.put(organizationId, dataKey);
            return dataKey;
        } catch (DuplicateKeyException e) {
            // Another node created the organization's data key first: use that one.
            return findStored(organizationId)
                .map(this::unwrap)
                .orElseThrow(() ->
                    new TechnicalDomainException("The data key of organization " + organizationId + " could not be read", e)
                );
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("Failed to store the data key of organization " + organizationId, e);
        }
    }

    private Optional<OrganizationKey> findStored(String organizationId) {
        try {
            return organizationKeyRepository.findByOrganizationId(organizationId);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("Failed to read the data key of organization " + organizationId, e);
        }
    }

    private byte[] wrap(String organizationId, DataKey dataKey) {
        try {
            return kek.wrap(organizationId, dataKey.material());
        } catch (SecretCryptoException e) {
            throw new TechnicalDomainException("Failed to wrap the data key of organization " + organizationId, e);
        }
    }

    private DataKey unwrap(OrganizationKey organizationKey) {
        if (!kek.id().equals(organizationKey.getKekId())) {
            disable(MISMATCH);
            throw unavailable();
        }
        try {
            var material = kek.unwrap(organizationKey.getOrganizationId(), Base64.getDecoder().decode(organizationKey.getWrappedKey()));
            var dataKey = new DataKey(organizationKey.getId(), material);
            dataKeys.put(organizationKey.getOrganizationId(), dataKey);
            return dataKey;
        } catch (SecretCryptoException | IllegalArgumentException e) {
            throw new TechnicalDomainException("Failed to unwrap the data key of organization " + organizationKey.getOrganizationId(), e);
        }
    }

    /**
     * Checks the key encryption key against the ids stored beside the wrapped data keys. A mismatch disables the
     * secrets feature and logs why; it never throws.
     *
     * @throws TechnicalDomainException when the stored ids cannot be read
     */
    public void verifyStoredKeys() {
        if (unavailableReason == null && !storedKeysChecked) {
            checkStoredKeys();
        }
    }

    private void ensureAvailable() {
        verifyStoredKeys();
        if (unavailableReason != null) {
            throw unavailable();
        }
    }

    private void checkStoredKeys() {
        try {
            var kekIds = organizationKeyRepository.findKekIds();
            if (!kekIds.isEmpty() && !kekIds.contains(kek.id())) {
                disable(MISMATCH);
            }
            storedKeysChecked = true;
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("Failed to read the key encryption key ids of the stored secrets", e);
        }
    }

    private synchronized void disable(String reason) {
        if (unavailableReason == null) {
            unavailableReason = reason;
            log.error("{} {}: secrets cannot be stored or read until it is fixed.", KEY_PROPERTY, reason);
        }
    }

    private SecretEncryptionUnavailableException unavailable() {
        return new SecretEncryptionUnavailableException(KEY_PROPERTY, unavailableReason);
    }

    private KeyEncryptionKeyProvider toProvider(String base64Kek) {
        try {
            return StaticKeyEncryptionKeyProvider.fromBase64(base64Kek);
        } catch (InvalidKeyEncryptionKeyException e) {
            disable(e.getMessage());
            return null;
        }
    }

    private static SecretScope scopeOf(Secret secret) {
        return new SecretScope(secret.organizationId(), secret.environmentId(), secret.id());
    }
}
