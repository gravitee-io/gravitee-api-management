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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.audit.model.EnvironmentAuditLogEntity;
import io.gravitee.gamma.module.platform.core.secret.model.Secret;
import io.gravitee.gamma.module.platform.core.secret.model.SecretActor;
import io.gravitee.gamma.module.platform.core.secret.model.SecretAuditEvent;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ApimSecretAuditLogTest {

    private static final Instant AT = Instant.parse("2026-11-06T10:00:00Z");
    private static final Secret SECRET = new Secret("secret-1", "org-1", "env-1", "anthropic-key", AT, AT);
    private static final SecretActor ACTOR = new SecretActor("user-1", "memory", "user-1-source");

    @Mock
    private AuditDomainService auditDomainService;

    @InjectMocks
    private ApimSecretAuditLog auditLog;

    @Test
    void should_write_an_environment_audit_log_of_the_secret_metadata() {
        auditLog.record(SecretAuditEvent.SECRET_CREATED, null, SECRET, ACTOR, AT);

        var captor = ArgumentCaptor.forClass(EnvironmentAuditLogEntity.class);
        verify(auditDomainService).createEnvironmentAuditLog(captor.capture());
        assertThat(captor.getValue()).satisfies(entry -> {
            assertThat(entry.organizationId()).isEqualTo("org-1");
            assertThat(entry.environmentId()).isEqualTo("env-1");
            assertThat(entry.actor()).isEqualTo(new AuditActor("user-1", "memory", "user-1-source"));
            assertThat(entry.event().name()).isEqualTo("SECRET_CREATED");
            assertThat(entry.createdAt()).isEqualTo(AT.atZone(ZoneOffset.UTC));
            assertThat(entry.oldValue()).isNull();
            assertThat(entry.newValue()).isEqualTo(SECRET);
            assertThat(entry.properties()).isEqualTo(Map.of(AuditProperties.SECRET, "secret-1"));
        });
    }

    @Test
    void should_take_the_scope_from_the_deleted_secret() {
        auditLog.record(SecretAuditEvent.SECRET_DELETED, SECRET, null, ACTOR, AT);

        var captor = ArgumentCaptor.forClass(EnvironmentAuditLogEntity.class);
        verify(auditDomainService).createEnvironmentAuditLog(captor.capture());
        assertThat(captor.getValue().organizationId()).isEqualTo("org-1");
        assertThat(captor.getValue().environmentId()).isEqualTo("env-1");
        assertThat(captor.getValue().oldValue()).isEqualTo(SECRET);
        assertThat(captor.getValue().newValue()).isNull();
        assertThat(captor.getValue().properties()).isEqualTo(Map.of(AuditProperties.SECRET, "secret-1"));
    }

    @Test
    void should_name_the_rotated_secret() {
        var rotated = SECRET.rotatedAt(AT.plusSeconds(60));

        auditLog.record(SecretAuditEvent.SECRET_UPDATED, SECRET, rotated, ACTOR, AT.plusSeconds(60));

        var captor = ArgumentCaptor.forClass(EnvironmentAuditLogEntity.class);
        verify(auditDomainService).createEnvironmentAuditLog(captor.capture());
        assertThat(captor.getValue().properties()).isEqualTo(Map.of(AuditProperties.SECRET, "secret-1"));
    }
}
