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
package io.gravitee.gamma.rest.core.guardian.use_case;

import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.ENVIRONMENT_ID;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.GUARDIAN_ID;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.ORGANIZATION_ID;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardian;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.EnvironmentAuditLogEntity;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianNotFoundException;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardianRepository;
import io.gravitee.gamma.rest.core.guardian.model.GuardianAuditEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class DeleteGuardianUseCaseTest {

    private final InMemoryGuardianRepository guardianRepository = new InMemoryGuardianRepository();
    private final AuditDomainService auditDomainService = mock(AuditDomainService.class);

    private DeleteGuardianUseCase useCase;

    @BeforeEach
    void setUp() {
        guardianRepository.reset();
        guardianRepository.givenGuardian(aGuardian().build());
        useCase = new DeleteGuardianUseCase(guardianRepository, auditDomainService);
    }

    private void delete(String environmentId, String guardianId) {
        useCase.execute(DeleteGuardianUseCase.Input.builder().auditInfo(auditInfo(environmentId)).guardianId(guardianId).build());
    }

    @Test
    void should_delete_the_guardian() {
        delete(ENVIRONMENT_ID, GUARDIAN_ID);

        assertThat(guardianRepository.storedGuardians()).isEmpty();
    }

    @Test
    void should_record_the_deletion_in_the_audit_trail() {
        delete(ENVIRONMENT_ID, GUARDIAN_ID);

        var log = recordedAuditLog();
        assertThat(log.event()).isEqualTo(GuardianAuditEvent.GUARDIAN_DELETED);
        assertThat(((io.gravitee.gamma.rest.core.guardian.model.Guardian) log.oldValue()).getId()).isEqualTo(GUARDIAN_ID);
    }

    @Test
    void should_refuse_to_delete_a_guardian_of_another_environment() {
        assertThatThrownBy(() -> delete("other-env", GUARDIAN_ID)).isInstanceOf(GuardianNotFoundException.class);

        assertThat(guardianRepository.storedGuardians()).hasSize(1);
        verifyNoInteractions(auditDomainService);
    }

    private static AuditInfo auditInfo(String environmentId) {
        return AuditInfo.builder()
            .organizationId(ORGANIZATION_ID)
            .environmentId(environmentId)
            .actor(AuditActor.builder().userId("someone").build())
            .build();
    }

    private EnvironmentAuditLogEntity recordedAuditLog() {
        var captor = ArgumentCaptor.forClass(EnvironmentAuditLogEntity.class);
        verify(auditDomainService).createEnvironmentAuditLog(captor.capture());
        return captor.getValue();
    }
}
