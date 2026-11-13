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
package io.gravitee.apim.core.secret.domain_service;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.EnvironmentAuditLogEntity;
import io.gravitee.apim.core.secret.crud_service.SecretCrudService;
import io.gravitee.apim.core.secret.exception.SecretKeyAlreadyExistsException;
import io.gravitee.apim.core.secret.model.Secret;
import io.gravitee.apim.core.secret.model.SecretAuditEvent;
import io.gravitee.apim.core.secret.model.SecretSnapshot;
import io.gravitee.apim.core.secret.model.SecretValue;
import io.gravitee.apim.core.secret.service_provider.SecretValueEncryptor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.ZonedDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * Writes secrets: encrypted and audited, whoever asks. It does not check who may change a key, so the Secrets API
 * refuses the keys kept for credentials before calling it.
 */
@DomainService
@RequiredArgsConstructor
public class SecretDomainService {

    private final SecretCrudService secretCrudService;
    private final SecretValueEncryptor secretValueEncryptor;
    private final AuditDomainService auditService;

    public Secret create(String key, SecretValue value, AuditInfo auditInfo) {
        var secret = Secret.create(
            UuidString.generateRandom(),
            auditInfo.organizationId(),
            auditInfo.environmentId(),
            key,
            TimeProvider.instantNow()
        );
        if (secretCrudService.findByKey(secret.organizationId(), secret.environmentId(), secret.key()).isPresent()) {
            throw new SecretKeyAlreadyExistsException(secret.key());
        }
        var created = secretCrudService.create(secret, secretValueEncryptor.encrypt(secret, value.plaintext()));
        audit(SecretAuditEvent.SECRET_CREATED, null, created, created.createdAt().atZone(TimeProvider.clock().getZone()), auditInfo);
        return created;
    }

    public Secret update(Secret current, SecretValue value, AuditInfo auditInfo) {
        var updated = secretCrudService.update(
            current.withUpdatedAt(TimeProvider.instantNow()),
            secretValueEncryptor.encrypt(current, value.plaintext())
        );
        audit(SecretAuditEvent.SECRET_UPDATED, current, updated, updated.updatedAt().atZone(TimeProvider.clock().getZone()), auditInfo);
        return updated;
    }

    public SecretSnapshot snapshot(Secret secret) {
        return new SecretSnapshot(secret, secretCrudService.getEncryptedValue(secret));
    }

    /** Writes the stored value back unchanged: the encryption is bound to the secret id, not to its update date. */
    public Secret restore(SecretSnapshot snapshot, AuditInfo auditInfo) {
        var restored = secretCrudService.update(snapshot.secret().withUpdatedAt(TimeProvider.instantNow()), snapshot.encryptedValue());
        audit(
            SecretAuditEvent.SECRET_UPDATED,
            snapshot.secret(),
            restored,
            restored.updatedAt().atZone(TimeProvider.clock().getZone()),
            auditInfo
        );
        return restored;
    }

    public void delete(Secret secret, AuditInfo auditInfo) {
        secretCrudService.delete(secret);
        audit(SecretAuditEvent.SECRET_DELETED, secret, null, TimeProvider.now(), auditInfo);
    }

    private void audit(SecretAuditEvent event, Secret before, Secret after, ZonedDateTime at, AuditInfo auditInfo) {
        auditService.createEnvironmentAuditLog(
            EnvironmentAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .event(event)
                .actor(auditInfo.actor())
                .oldValue(before)
                .newValue(after)
                .createdAt(at)
                .properties(Map.of(AuditProperties.SECRET, (after == null ? before : after).id()))
                .build()
        );
    }
}
