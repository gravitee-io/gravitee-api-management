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
package io.gravitee.gamma.authorization.audit;

import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.EnvironmentAuditLogEntity;
import io.gravitee.apim.core.audit.model.event.AuditEvent;
import io.gravitee.gamma.authorization.api.AuthzAuditEntry;
import io.gravitee.gamma.authorization.api.AuthzAuditPort;
import io.gravitee.gamma.authorization.api.AuthzAuditReferenceKind;
import io.gravitee.gamma.authorization.api.AuthzCallerContext;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import lombok.CustomLog;

@CustomLog
public final class ApimAuthzAuditAdapter implements AuthzAuditPort {

    private final AuditDomainService auditDomainService;

    public ApimAuthzAuditAdapter(AuditDomainService auditDomainService) {
        this.auditDomainService = auditDomainService;
    }

    @Override
    public void record(AuthzAuditEntry entry) {
        try {
            AuthzCallerContext caller = entry.caller();
            String eventName = entry.event().name();
            EnvironmentAuditLogEntity audit = EnvironmentAuditLogEntity.builder()
                .organizationId(caller.organizationId())
                .environmentId(caller.environmentId())
                .actor(AuditActor.builder().userId(caller.userId()).build())
                .event(() -> eventName)
                .properties(Map.of(toDddProperty(entry.referenceKind()), entry.referenceId()))
                .createdAt(ZonedDateTime.now(ZoneOffset.UTC))
                .oldValue(entry.oldSnapshot())
                .newValue(entry.newSnapshot())
                .build();
            auditDomainService.createEnvironmentAuditLog(audit);
        } catch (RuntimeException e) {
            log.warn(
                "authz-audit emit failed (event={} ref={} id={})",
                entry.event().name(),
                entry.referenceKind().name(),
                entry.referenceId(),
                e
            );
        }
    }

    private static AuditProperties toDddProperty(AuthzAuditReferenceKind kind) {
        return switch (kind) {
            case POLICY -> AuditProperties.AUTHORIZATION_POLICY;
            case ENTITY -> AuditProperties.AUTHORIZATION_ENTITY;
        };
    }
}
