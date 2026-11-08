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
package io.gravitee.apim.core.secret.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.EnvironmentAuditLogEntity;
import io.gravitee.apim.core.secret.crud_service.SecretCrudService;
import io.gravitee.apim.core.secret.exception.SecretKeyAlreadyExistsException;
import io.gravitee.apim.core.secret.model.Secret;
import io.gravitee.apim.core.secret.model.SecretAuditEvent;
import io.gravitee.apim.core.secret.model.SecretValue;
import io.gravitee.apim.core.secret.service_provider.SecretValueEncryptor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.service.common.UuidString;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateSecretUseCase {

    private final SecretCrudService secretCrudService;
    private final SecretValueEncryptor secretValueEncryptor;
    private final AuditDomainService auditService;

    public record Input(String key, SecretValue value, AuditInfo auditInfo) {}

    public record Output(Secret secret) {}

    public Output execute(Input input) {
        var auditInfo = input.auditInfo();
        var secret = Secret.create(
            UuidString.generateRandom(),
            auditInfo.organizationId(),
            auditInfo.environmentId(),
            input.key(),
            TimeProvider.instantNow()
        );
        if (secretCrudService.findByKey(secret.organizationId(), secret.environmentId(), secret.key()).isPresent()) {
            throw new SecretKeyAlreadyExistsException(secret.key());
        }
        var created = secretCrudService.create(secret, secretValueEncryptor.encrypt(secret, input.value().plaintext()));
        createAuditLog(created, auditInfo);
        return new Output(created);
    }

    private void createAuditLog(Secret created, AuditInfo auditInfo) {
        auditService.createEnvironmentAuditLog(
            EnvironmentAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .event(SecretAuditEvent.SECRET_CREATED)
                .actor(auditInfo.actor())
                .newValue(created)
                .createdAt(created.createdAt().atZone(TimeProvider.clock().getZone()))
                .properties(Map.of(AuditProperties.SECRET, created.id()))
                .build()
        );
    }
}
