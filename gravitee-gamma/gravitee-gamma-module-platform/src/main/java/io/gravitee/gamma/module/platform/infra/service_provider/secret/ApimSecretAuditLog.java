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
package io.gravitee.gamma.module.platform.infra.service_provider.secret;

import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.EnvironmentAuditLogEntity;
import io.gravitee.apim.core.audit.model.event.AuditEvent;
import io.gravitee.gamma.module.platform.core.secret.model.Secret;
import io.gravitee.gamma.module.platform.core.secret.model.SecretActor;
import io.gravitee.gamma.module.platform.core.secret.model.SecretAuditEvent;
import io.gravitee.gamma.module.platform.core.secret.port.service_provider.SecretAuditLog;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ApimSecretAuditLog implements SecretAuditLog {

    private final AuditDomainService auditDomainService;

    @Override
    public void record(SecretAuditEvent event, Secret before, Secret after, SecretActor actor, Instant at) {
        var scope = after != null ? after : before;
        AuditEvent auditEvent = event::name;
        auditDomainService.createEnvironmentAuditLog(
            EnvironmentAuditLogEntity.builder()
                .organizationId(scope.organizationId())
                .environmentId(scope.environmentId())
                .actor(new AuditActor(actor.userId(), actor.source(), actor.sourceId()))
                .properties(Map.of())
                .event(auditEvent)
                .createdAt(at.atZone(ZoneOffset.UTC))
                .oldValue(before)
                .newValue(after)
                .build()
        );
    }
}
