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
import io.gravitee.apim.core.credential.model.Credential;
import io.gravitee.apim.core.credential.model.SecretReference;
import io.gravitee.apim.core.secret.crud_service.SecretCrudService;
import io.gravitee.apim.core.secret.domain_service.SecretDomainService;
import io.gravitee.apim.core.secret.model.Secret;
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

    public void rotate(Credential credential, Map<String, String> values, AuditInfo auditInfo) {
        var rotated = new ArrayList<String>();
        for (var field : credential.strategy().secretFields()) {
            if (!values.containsKey(field)) {
                continue;
            }
            try {
                var secret = secretCrudService.get(
                    auditInfo.organizationId(),
                    auditInfo.environmentId(),
                    credential.secretReferences().get(field).secretId()
                );
                secretDomainService.update(secret, new SecretValue(values.get(field)), auditInfo);
            } catch (RuntimeException e) {
                if (rotated.isEmpty()) {
                    throw e;
                }
                throw new CredentialRotationIncompleteException(credential.id(), rotated, field, e);
            }
            rotated.add(field);
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
