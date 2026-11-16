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
package io.gravitee.apim.core.credential.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.credential.exception.CredentialRotationIncompleteException;
import io.gravitee.apim.core.credential.exception.CredentialSecretMissingException;
import io.gravitee.apim.core.credential.model.Credential;
import io.gravitee.apim.core.credential.model.SecretReference;
import io.gravitee.apim.core.secret.crud_service.SecretCrudService;
import io.gravitee.apim.core.secret.domain_service.SecretDomainService;
import io.gravitee.apim.core.secret.model.Secret;
import io.gravitee.apim.core.secret.model.SecretSnapshot;
import io.gravitee.apim.core.secret.model.SecretValue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/** Writes the values of the credentials Gravitee stores, one secret per secret field. */
@DomainService
@RequiredArgsConstructor
public class CredentialSecretsDomainService {

    private final SecretDomainService secretDomainService;
    private final SecretCrudService secretCrudService;

    /** Deletes the secrets already written when one write fails, so a failed creation leaves no secret behind. */
    public Map<String, SecretReference> store(Credential credential, Map<String, String> values, AuditInfo auditInfo) {
        var stored = new LinkedHashMap<String, SecretReference>();
        try {
            for (var field : credential.strategy().secretFields()) {
                var secret = secretDomainService.create(key(credential, field), new SecretValue(values.get(field)), auditInfo);
                stored.put(field, SecretReference.managed(secret.path(), secret.id()));
            }
        } catch (RuntimeException e) {
            discard(stored.values(), auditInfo, e);
            throw e;
        }
        return stored;
    }

    /** Deletes the secrets of a credential that could not be saved; a failed delete is added to {@code cause}. */
    public void discard(Collection<SecretReference> references, AuditInfo auditInfo, RuntimeException cause) {
        for (var reference : references) {
            try {
                deleteIfPresent(reference.secretId(), auditInfo);
            } catch (RuntimeException e) {
                cause.addSuppressed(e);
            }
        }
    }

    /** Secrets whose new value is written, with the value each held before. */
    public record Rotation(String credentialId, Map<String, SecretSnapshot> previous) {}

    /**
     * All or nothing: when a field fails, the fields already written get their old values back before the failure is rethrown.
     * Nothing locks the secrets, so the undo of one rotation can overwrite a value a concurrent rotation of the same credential wrote.
     */
    public Rotation rotate(Credential credential, Map<String, String> values, AuditInfo auditInfo) {
        var snapshots = new LinkedHashMap<String, SecretSnapshot>();
        for (var field : credential.strategy().secretFields()) {
            if (!values.containsKey(field)) {
                continue;
            }
            var secretId = credential.secretReferences().get(field).secretId();
            var secret = secretCrudService
                .findById(auditInfo.organizationId(), auditInfo.environmentId(), secretId)
                .orElseThrow(() -> new CredentialSecretMissingException(credential.id(), field));
            snapshots.put(field, secretDomainService.snapshot(secret));
        }
        var rotation = new Rotation(credential.id(), new LinkedHashMap<>());
        for (var snapshot : snapshots.entrySet()) {
            try {
                secretDomainService.update(snapshot.getValue().secret(), new SecretValue(values.get(snapshot.getKey())), auditInfo);
            } catch (RuntimeException e) {
                undo(rotation, auditInfo, e);
                throw e;
            }
            rotation.previous().put(snapshot.getKey(), snapshot.getValue());
        }
        return rotation;
    }

    /** Puts back the old values of a rotation that cannot complete; a value that cannot be put back is added to {@code cause}. */
    public void undo(Rotation rotation, AuditInfo auditInfo, RuntimeException cause) {
        var notPutBack = new ArrayList<String>();
        rotation
            .previous()
            .forEach((field, snapshot) -> {
                try {
                    secretDomainService.restore(snapshot, auditInfo);
                } catch (RuntimeException e) {
                    cause.addSuppressed(e);
                    notPutBack.add(field);
                }
            });
        if (!notPutBack.isEmpty()) {
            throw new CredentialRotationIncompleteException(rotation.credentialId(), notPutBack, cause);
        }
    }

    public void deleteAll(Credential credential, AuditInfo auditInfo) {
        credential
            .secretReferences()
            .values()
            .stream()
            .filter(SecretReference::isManaged)
            .forEach(reference -> deleteIfPresent(reference.secretId(), auditInfo));
    }

    /** A delete that failed after removing some secrets leaves them gone, so the retry must still go through. */
    private void deleteIfPresent(String secretId, AuditInfo auditInfo) {
        secretCrudService
            .findById(auditInfo.organizationId(), auditInfo.environmentId(), secretId)
            .ifPresent(secret -> secretDomainService.delete(secret, auditInfo));
    }

    private static String key(Credential credential, String field) {
        return Secret.CREDENTIAL_KEY_PREFIX + credential.id() + "." + field;
    }
}
