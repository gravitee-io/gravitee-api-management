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
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.EnvironmentAuditLogEntity;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.gamma.module.platform.core.credential.model.Credential;
import io.gravitee.gamma.module.platform.core.credential.model.CredentialActor;
import io.gravitee.gamma.module.platform.core.credential.model.CredentialAuditEvent;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@DomainService
@RequiredArgsConstructor
public class CredentialAuditDomainService {

    private final AuditDomainService auditDomainService;

    /** A credential holds no secret value, so the audit patch carries none. */
    public void record(CredentialActor actor, CredentialAuditEvent event, Credential oldValue, Credential newValue) {
        var credentialId = newValue != null ? newValue.id() : oldValue.id();
        auditDomainService.createEnvironmentAuditLog(
            EnvironmentAuditLogEntity.builder()
                .organizationId(actor.organizationId())
                .environmentId(actor.environmentId())
                .actor(actor.auditInfo().actor())
                .event(event)
                .properties(Map.of(AuditProperties.CREDENTIAL, credentialId))
                .createdAt(TimeProvider.now())
                .oldValue(oldValue)
                .newValue(newValue)
                .build()
        );
    }
}
