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
import io.gravitee.apim.core.secret.model.Secret;
import io.gravitee.apim.core.secret.model.SecretAuditEvent;
import io.gravitee.common.utils.TimeProvider;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class DeleteSecretUseCase {

    private final SecretCrudService secretCrudService;
    private final AuditDomainService auditService;

    /** {@code credential} is set only by the credential API, which owns the secrets that hold credential values. */
    public record Input(String secretId, AuditInfo auditInfo, boolean credential) {
        /** The input of a user, who may not change the secrets that hold credential values. */
        public Input(String secretId, AuditInfo auditInfo) {
            this(secretId, auditInfo, false);
        }
    }

    public record Output() {}

    public Output execute(Input input) {
        var auditInfo = input.auditInfo();
        var secret = secretCrudService.get(auditInfo.organizationId(), auditInfo.environmentId(), input.secretId());
        secret.requireChangeableBy(input.credential());
        secretCrudService.delete(secret);
        createAuditLog(secret, auditInfo);
        return new Output();
    }

    private void createAuditLog(Secret deleted, AuditInfo auditInfo) {
        auditService.createEnvironmentAuditLog(
            EnvironmentAuditLogEntity.builder()
                .organizationId(auditInfo.organizationId())
                .environmentId(auditInfo.environmentId())
                .event(SecretAuditEvent.SECRET_DELETED)
                .actor(auditInfo.actor())
                .oldValue(deleted)
                .createdAt(TimeProvider.now())
                .properties(Map.of(AuditProperties.SECRET, deleted.id()))
                .build()
        );
    }
}
