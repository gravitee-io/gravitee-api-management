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
package io.gravitee.gamma.module.platform.core.credential.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.gamma.module.platform.core.credential.exception.CredentialRotationIncompleteException;
import io.gravitee.gamma.module.platform.core.credential.model.Credential;
import io.gravitee.gamma.module.platform.core.credential.model.SecretReference;
import io.gravitee.gamma.module.platform.core.credential.port.service_provider.CredentialSecretStore;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/** Writes the values of the credentials Gravitee stores, one secret per secret field. */
@DomainService
@RequiredArgsConstructor
public class CredentialSecretsDomainService {

    private final CredentialSecretStore secretStore;

    /** Deletes the secrets already written when one write fails, so a failed creation leaves no secret behind. */
    public Map<String, SecretReference> store(Credential credential, Map<String, String> values, AuditInfo auditInfo) {
        var stored = new LinkedHashMap<String, SecretReference>();
        try {
            for (var field : credential.strategy().secretFields()) {
                var secret = secretStore.create(key(credential, field), values.get(field), auditInfo);
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
                secretStore.delete(reference.secretId(), auditInfo);
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
                secretStore.update(credential.secretReferences().get(field).secretId(), values.get(field), auditInfo);
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
            .forEach(reference -> secretStore.delete(reference.secretId(), auditInfo));
    }

    private static String key(Credential credential, String field) {
        return "credential." + credential.id() + "." + field;
    }
}
