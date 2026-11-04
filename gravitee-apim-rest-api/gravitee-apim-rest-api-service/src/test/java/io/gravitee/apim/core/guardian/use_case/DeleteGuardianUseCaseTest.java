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
package io.gravitee.apim.core.guardian.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fixtures.core.model.GuardianFixtures;
import inmemory.AuditCrudServiceInMemory;
import inmemory.GuardianCrudServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.guardian.exception.GuardianNotFoundException;
import io.gravitee.apim.core.guardian.model.GuardianAuditEvent;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DeleteGuardianUseCaseTest {

    private static final AuditInfo AUDIT_INFO = AuditInfo.builder()
        .organizationId("org-id")
        .environmentId("env-id")
        .actor(AuditActor.builder().userId("user-id").build())
        .build();

    private final GuardianCrudServiceInMemory guardianCrudService = new GuardianCrudServiceInMemory();
    private final UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();
    private final AuditCrudServiceInMemory auditCrudService = new AuditCrudServiceInMemory();

    private DeleteGuardianUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteGuardianUseCase(
            guardianCrudService,
            new AuditDomainService(auditCrudService, userCrudService, new JacksonJsonDiffProcessor())
        );
        guardianCrudService.initWith(List.of(GuardianFixtures.aGuardian().build()));
    }

    @AfterEach
    void tearDown() {
        guardianCrudService.reset();
        auditCrudService.reset();
    }

    @Test
    void should_delete_the_guardian() {
        useCase.execute(new DeleteGuardianUseCase.Input("guardian-id", AUDIT_INFO));

        assertThat(guardianCrudService.storage()).isEmpty();
    }

    @Test
    void should_write_an_audit_log() {
        useCase.execute(new DeleteGuardianUseCase.Input("guardian-id", AUDIT_INFO));

        assertThat(auditCrudService.storage())
            .hasSize(1)
            .first()
            .satisfies(audit -> assertThat(audit.getEvent()).isEqualTo(GuardianAuditEvent.GUARDIAN_DELETED.name()));
    }

    @Test
    void should_throw_when_the_guardian_does_not_exist() {
        assertThatThrownBy(() -> useCase.execute(new DeleteGuardianUseCase.Input("unknown", AUDIT_INFO))).isInstanceOf(
            GuardianNotFoundException.class
        );
        assertThat(guardianCrudService.storage()).hasSize(1);
    }
}
