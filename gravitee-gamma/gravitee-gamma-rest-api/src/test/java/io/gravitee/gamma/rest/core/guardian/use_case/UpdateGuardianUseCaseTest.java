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
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aDefinitionToSubmit;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardian;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.anUpdateGuardianCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.gravitee.apim.core.audit.domain_service.AuditDomainService;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.EnvironmentAuditLogEntity;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.guardian.Guardian.Phase;
import io.gravitee.gamma.rest.core.guardian.domain_service.ValidateGuardianDomainService;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianNotFoundException;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardianRepository;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.core.guardian.model.GuardianAuditEvent;
import io.gravitee.gamma.rest.core.guardian.model.UpdateGuardianCommand;
import jakarta.validation.ConstraintViolationException;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UpdateGuardianUseCaseTest {

    private static final Date DEPLOYED_AT = Date.from(java.time.Instant.parse("2026-03-01T10:00:00Z"));

    private final InMemoryGuardianRepository guardianRepository = new InMemoryGuardianRepository();
    private final AuditDomainService auditDomainService = mock(AuditDomainService.class);

    private UpdateGuardianUseCase useCase;

    @BeforeEach
    void setUp() {
        guardianRepository.reset();
        guardianRepository.givenGuardian(aGuardian().deployedAt(DEPLOYED_AT).build());
        useCase = new UpdateGuardianUseCase(guardianRepository, new ValidateGuardianDomainService(guardianRepository), auditDomainService);
    }

    private UpdateGuardianUseCase.Output update(String environmentId, UpdateGuardianCommand command) {
        return useCase.execute(
            UpdateGuardianUseCase.Input.builder()
                .auditInfo(auditInfo(environmentId))
                .guardianId(GUARDIAN_ID)
                .guardianToUpdate(command)
                .build()
        );
    }

    @Test
    void should_replace_the_whole_definition_and_the_description() {
        var output = update(
            ENVIRONMENT_ID,
            anUpdateGuardianCommand()
                .definition(aDefinitionToSubmit().name("Renamed").phase(Phase.RESPONSE).policies(List.of()).build())
                .description("A new description")
                .build()
        );

        assertThat(output.guardian())
            .extracting(Guardian::getName, Guardian::getPhase, Guardian::getPolicies, Guardian::getDescription)
            .containsExactly("Renamed", Phase.RESPONSE, List.of(), "A new description");
    }

    @Test
    void should_clear_the_description_when_the_payload_omits_it() {
        // a PUT replaces: leaving the description out is asking for it to be gone
        var output = update(ENVIRONMENT_ID, anUpdateGuardianCommand().description(null).build());

        assertThat(output.guardian().getDescription()).isNull();
    }

    @Test
    void should_keep_the_identity_and_the_deployment_stamp_whatever_the_payload_says() {
        var output = update(
            ENVIRONMENT_ID,
            anUpdateGuardianCommand()
                .definition(aDefinitionToSubmit().id("0b0c2a6e-3f1d-4a7b-9c8e-5d4f3a2b1c0d").environmentId("other-env").build())
                .build()
        );

        assertThat(output.guardian())
            .extracting(Guardian::getId, Guardian::getEnvironmentId, Guardian::getDeployedAt)
            .containsExactly(GUARDIAN_ID, ENVIRONMENT_ID, DEPLOYED_AT);
    }

    @Test
    void should_touch_the_update_date() {
        var before = aGuardian().build().getUpdatedAt();

        var output = update(ENVIRONMENT_ID, anUpdateGuardianCommand().build());

        assertThat(output.guardian().getUpdatedAt()).isAfter(before);
    }

    @Test
    void should_record_the_update_in_the_audit_trail() {
        var output = update(ENVIRONMENT_ID, anUpdateGuardianCommand().build());

        var log = recordedAuditLog();
        assertThat(log.event()).isEqualTo(GuardianAuditEvent.GUARDIAN_UPDATED);
        assertThat(((Guardian) log.oldValue()).getName()).isEqualTo("Credential leakage");
        assertThat(log.newValue()).isEqualTo(output.guardian());
    }

    @Test
    void should_refuse_to_move_a_guardian_onto_an_unsupported_api_type() {
        assertThatThrownBy(() ->
            update(
                ENVIRONMENT_ID,
                anUpdateGuardianCommand().definition(aDefinitionToSubmit().apiTypes(Set.of(ApiType.MCP_PROXY)).build()).build()
            )
        )
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("llm-proxy");

        verifyNoInteractions(auditDomainService);
    }

    @Test
    void should_refuse_to_update_a_guardian_of_another_environment() {
        assertThatThrownBy(() -> update("other-env", anUpdateGuardianCommand().build())).isInstanceOf(GuardianNotFoundException.class);

        verifyNoInteractions(auditDomainService);
    }

    /** As on creation, the command's own constraints hold for any caller of the use case. */
    @Nested
    class Structure {

        @Test
        void should_refuse_a_missing_definition() {
            assertThatThrownBy(() -> update(ENVIRONMENT_ID, anUpdateGuardianCommand().definition(null).build())).isInstanceOf(
                ConstraintViolationException.class
            );

            verifyNoInteractions(auditDomainService);
        }

        /**
         * Raised by the automated reviewer on round 6: the command only declares @NotNull on the
         * definition, so nothing re-checks the definition's own constraints once the id and the
         * environment have been merged in. A blank name would be stored.
         */
        @Test
        void should_refuse_a_blank_name_on_the_merged_definition() {
            assertThatThrownBy(() ->
                update(ENVIRONMENT_ID, anUpdateGuardianCommand().definition(aDefinitionToSubmit().name("   ").build()).build())
            ).isInstanceOf(ConstraintViolationException.class);

            verifyNoInteractions(auditDomainService);
        }

        @Test
        void should_refuse_a_missing_phase_on_the_merged_definition() {
            assertThatThrownBy(() ->
                update(ENVIRONMENT_ID, anUpdateGuardianCommand().definition(aDefinitionToSubmit().phase(null).build()).build())
            ).isInstanceOf(ConstraintViolationException.class);
        }

        @Test
        void should_refuse_a_description_longer_than_its_column() {
            assertThatThrownBy(() -> update(ENVIRONMENT_ID, anUpdateGuardianCommand().description("d".repeat(1025)).build())).isInstanceOf(
                ConstraintViolationException.class
            );
        }
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
