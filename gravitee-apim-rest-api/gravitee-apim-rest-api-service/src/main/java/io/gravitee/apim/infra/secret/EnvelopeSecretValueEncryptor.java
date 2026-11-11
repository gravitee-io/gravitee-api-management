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

import io.gravitee.apim.common.secret.DataKey;
import io.gravitee.apim.common.secret.DataKeyResolver;
import io.gravitee.apim.common.secret.InvalidKeyEncryptionKeyException;
import io.gravitee.apim.common.secret.KeyEncryptionKeyMismatchException;
import io.gravitee.apim.common.secret.KeyEncryptionKeyProvider;
import io.gravitee.apim.common.secret.SecretCryptoException;
import io.gravitee.apim.common.secret.SecretEnvelope;
import io.gravitee.apim.common.secret.SecretScope;
import io.gravitee.apim.common.secret.StaticKeyEncryptionKeyProvider;
import io.gravitee.apim.common.secret.StoredDataKey;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.secret.exception.SecretEncryptionUnavailableException;
import io.gravitee.apim.core.secret.model.Secret;
import io.gravitee.apim.core.secret.service_provider.SecretValueEncryptor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.repository.exceptions.DuplicateKeyException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.OrganizationKeyRepository;
import io.gravitee.repository.management.api.SecretRepository;
import io.gravitee.repository.management.model.OrganizationKey;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import lombok.CustomLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Envelope encryption: each organization's secret values are encrypted under its own data key, stored wrapped by the
 * key encryption key. A missing or invalid key encryption key never stops the Management API: it disables the secrets
 * feature, and every encrypt or decrypt fails with the reason. A data key wrapped by another key encryption key fails
 * only the calls of its organization.
 */
@CustomLog
@Service
public class EnvelopeSecretValueEncryptor implements SecretValueEncryptor {

    public static final String KEY_PROPERTY = "api.secrets.encryption.key";

    private static final String MISMATCH = "does not match the key that encrypted the stored secrets";
    private static final String KEY_HINT = "Set it to a base64-encoded 256-bit key.";

    private final OrganizationKeyRepository organizationKeyRepository;
    private final SecretRepository secretRepository;
    private final KeyEncryptionKeyProvider kek;
    private final DataKeyResolver dataKeys;
    private final String unavailableReason;

    public EnvelopeSecretValueEncryptor(
        @Value("${" + KEY_PROPERTY + ":#{null}}") String base64Kek,
        @Lazy OrganizationKeyRepository organizationKeyRepository,
        @Lazy SecretRepository secretRepository
    ) {
        this.organizationKeyRepository = organizationKeyRepository;
        this.secretRepository = secretRepository;
        KeyEncryptionKeyProvider provider = null;
        String reason = null;
        try {
            provider = StaticKeyEncryptionKeyProvider.fromBase64(base64Kek);
        } catch (InvalidKeyEncryptionKeyException e) {
            reason = e.getMessage();
            logUnavailable(base64Kek, reason);
        }
        this.kek = provider;
        this.unavailableReason = reason;
        this.dataKeys = provider == null ? null : new DataKeyResolver(provider, this::findStored);
    }

    @Override
    public String encrypt(Secret secret, String plaintext) {
        try {
            return SecretEnvelope.seal(dataKeyFor(secret.organizationId()), scopeOf(secret), plaintext);
        } catch (SecretCryptoException e) {
            throw translate(secret.organizationId(), e);
        }
    }

    @Override
    public String decrypt(Secret secret, String ciphertext) {
        ensureAvailable();
        try {
            var dataKey = dataKeys
                .resolve(secret.organizationId())
                .orElseThrow(() ->
                    new TechnicalDomainException(
                        "Organization " + secret.organizationId() + " has no data key to decrypt secret " + secret.id()
                    )
                );
            return SecretEnvelope.open(dataKey, scopeOf(secret), ciphertext);
        } catch (SecretCryptoException e) {
            throw translate(secret.organizationId(), e);
        }
    }

    DataKey dataKeyFor(String organizationId) {
        ensureAvailable();
        return dataKeys.resolve(organizationId).orElseGet(() -> createDataKey(organizationId));
    }

    private DataKey createDataKey(String organizationId) {
        refuseToMixKeyEncryptionKeys();
        refuseToOrphanSecrets(organizationId);
        var dataKey = DataKey.generate();
        var organizationKey = OrganizationKey.builder()
            .id(dataKey.id())
            .organizationId(organizationId)
            .wrappedKey(Base64.getEncoder().encodeToString(kek.wrap(organizationId, dataKey.material())))
            .kekId(kek.id())
            .createdAt(Date.from(TimeProvider.instantNow()))
            .build();
        try {
            organizationKeyRepository.create(organizationKey);
            return dataKey;
        } catch (DuplicateKeyException e) {
            // Another node created the organization's data key first: use that one.
            return dataKeys
                .resolve(organizationId)
                .orElseThrow(() ->
                    new TechnicalDomainException("The data key of organization " + organizationId + " could not be read", e)
                );
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("Failed to store the data key of organization " + organizationId, e);
        }
    }

    // Checked on each new data key, not only at startup: a node with another key could otherwise write the first data keys.
    private void refuseToMixKeyEncryptionKeys() {
        if (!storedKeysMatch()) {
            throw new SecretEncryptionUnavailableException(KEY_PROPERTY, MISMATCH);
        }
    }

    // A new data key could never decrypt the secrets already stored under the lost one.
    private void refuseToOrphanSecrets(String organizationId) {
        boolean hasSecrets;
        try {
            hasSecrets = secretRepository.existsByOrganizationId(organizationId);
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("Failed to check for the secrets of organization " + organizationId, e);
        }
        if (hasSecrets) {
            throw new TechnicalDomainException(
                "Organization " +
                    organizationId +
                    " has secrets but no data key to decrypt them: restore its data key or delete its secrets"
            );
        }
    }

    private Optional<StoredDataKey> findStored(String organizationId) {
        try {
            return organizationKeyRepository
                .findByOrganizationId(organizationId)
                .map(key -> new StoredDataKey(key.getId(), key.getKekId(), key.getWrappedKey()));
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("Failed to read the data key of organization " + organizationId, e);
        }
    }

    /**
     * Logs at ERROR when the key encryption key did not wrap every stored data key. It does not disable the secrets
     * feature: only the organizations whose data key it did not wrap fail.
     *
     * @throws TechnicalDomainException when the stored key encryption key ids cannot be read
     */
    public void verifyStoredKeys() {
        if (kek != null && !storedKeysMatch()) {
            log.error(
                "{} {}: the organizations whose data key it did not wrap cannot store or read secrets until it is fixed.",
                KEY_PROPERTY,
                MISMATCH
            );
        }
    }

    private boolean storedKeysMatch() {
        Set<String> kekIds;
        try {
            kekIds = organizationKeyRepository.findKekIds();
        } catch (TechnicalException e) {
            throw new TechnicalDomainException("Failed to read the key encryption key ids of the stored secrets", e);
        }
        return kekIds.stream().allMatch(kek::canUnwrap);
    }

    private void ensureAvailable() {
        if (kek == null) {
            throw new SecretEncryptionUnavailableException(KEY_PROPERTY, unavailableReason);
        }
    }

    private static TechnicalDomainException translate(String organizationId, SecretCryptoException e) {
        if (e instanceof KeyEncryptionKeyMismatchException) {
            return new SecretEncryptionUnavailableException(
                KEY_PROPERTY,
                "does not match the key that wrapped the data key of organization " + organizationId
            );
        }
        return new TechnicalDomainException(e.getMessage(), e);
    }

    private static void logUnavailable(String base64Kek, String reason) {
        if (base64Kek == null || base64Kek.isBlank()) {
            log.warn("{} {}: secrets cannot be stored or read. {}", KEY_PROPERTY, reason, KEY_HINT);
        } else {
            log.error("{} {}: secrets cannot be stored or read until it is fixed. {}", KEY_PROPERTY, reason, KEY_HINT);
        }
    }

    private static SecretScope scopeOf(Secret secret) {
        return new SecretScope(secret.organizationId(), secret.environmentId(), secret.id());
    }
}
