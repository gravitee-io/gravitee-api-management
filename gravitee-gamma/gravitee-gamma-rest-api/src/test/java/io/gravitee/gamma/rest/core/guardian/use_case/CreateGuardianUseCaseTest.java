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
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aCreateGuardian;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardianValidator;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aPolicy;
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
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardianRepository;
import io.gravitee.gamma.rest.core.guardian.model.CreateGuardian;
import io.gravitee.gamma.rest.core.guardian.model.GuardianAuditEvent;
import io.gravitee.gamma.rest.core.guardian.model.GuardianLifecycleState;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreateGuardianUseCaseTest {

    private final InMemoryGuardianRepository guardianRepository = new InMemoryGuardianRepository();
    private final AuditDomainService auditDomainService = mock(AuditDomainService.class);

    private CreateGuardianUseCase useCase;

    @BeforeEach
    void setUp() {
        guardianRepository.reset();
        useCase = new CreateGuardianUseCase(guardianRepository, aGuardianValidator(guardianRepository), auditDomainService);
    }

    private CreateGuardianUseCase.Output create(CreateGuardian command) {
        return useCase.execute(
            CreateGuardianUseCase.Input.builder().auditInfo(auditInfo(ENVIRONMENT_ID)).guardianToCreate(command).build()
        );
    }

    @Test
    void should_create_an_undeployed_guardian_in_the_context_environment() {
        var output = create(aCreateGuardian().build());

        assertThat(output.guardian())
            .extracting(g -> g.getEnvironmentId(), g -> g.getLifecycleState(), g -> g.getDeployedAt())
            .containsExactly(ENVIRONMENT_ID, GuardianLifecycleState.UNDEPLOYED, null);
    }

    @Test
    void should_store_the_whole_definition_and_the_description() {
        var output = create(aCreateGuardian().build());

        assertThat(output.guardian())
            .extracting(g -> g.getName(), g -> g.getPhase(), g -> g.getApiTypes(), g -> g.getPolicies(), g -> g.getDescription())
            .containsExactly(
                "Prompt injection shield",
                Phase.REQUEST,
                Set.of(ApiType.LLM_PROXY),
                List.of(aPolicy()),
                "Flags a prompt that tries to override the system instructions."
            );
    }

    @Test
    void should_keep_the_description_out_of_the_definition_the_gateway_reads() {
        // the gateway has no use for it, so it is a column of the control plane rather than part of
        // the serialized definition
        var output = create(aCreateGuardian().build());

        assertThat(output.guardian().getDefinition().toString()).doesNotContain("Flags a prompt");
    }

    @Test
    void should_generate_an_id_when_the_payload_carries_none() {
        var output = create(aCreateGuardian().build());

        assertThat(output.guardian().getId()).isNotBlank();
        assertThat(guardianRepository.storedGuardians()).hasSize(1);
    }

    @Test
    void should_keep_the_id_the_payload_carries() {
        var output = create(aCreateGuardian().id(GUARDIAN_ID).build());

        assertThat(output.guardian().getId()).isEqualTo(GUARDIAN_ID);
    }

    @Test
    void should_generate_an_id_when_the_caller_sends_a_blank_one() {
        // Optional.ofNullable("") is present, so without the blank filter an empty id would be
        // stored and no /guardians/{guardianId} route could reach the Guardian again
        var output = create(aCreateGuardian().id("   ").build());

        assertThat(output.guardian().getId()).isNotBlank().doesNotContain(" ");
    }

    @Test
    void should_record_the_creation_in_the_audit_trail() {
        var output = create(aCreateGuardian().build());

        var log = recordedAuditLog();
        assertThat(log.event()).isEqualTo(GuardianAuditEvent.GUARDIAN_CREATED);
        assertThat(log.organizationId()).isEqualTo(ORGANIZATION_ID);
        assertThat(log.newValue()).isEqualTo(output.guardian());
    }

    @Test
    void should_refuse_an_incompatible_guardrail_before_creating_a_guardian_or_an_audit() {
        var policy = aPolicy();
        policy.setConfiguration("{\"scope\":{\"protocol\":\"MCP\"}}");

        assertThatThrownBy(() -> create(aCreateGuardian().policies(List.of(policy)).build()))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("Detect credentials")
            .hasMessageContaining("llm-proxy");

        assertThat(guardianRepository.storedGuardians()).as("a rejected guardrail leaves no Guardian draft").isEmpty();
        verifyNoInteractions(auditDomainService);
    }

    @Test
    void should_refuse_an_api_type_other_than_llm_proxy() {
        assertThatThrownBy(() -> create(aCreateGuardian().apiTypes(Set.of(ApiType.MCP_PROXY)).build()))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("llm-proxy");

        assertThat(guardianRepository.storedGuardians()).isEmpty();
    }

    /**
     * The constraints sit on {@link CreateGuardian}, so they hold for every caller of the use case
     * and not only for one arriving through JAX-RS — which is the whole point of putting them there.
     * Each case would be a 500 if the use case trusted its input.
     */
    @Nested
    class Structure {

        @Test
        void should_refuse_a_blank_name() {
            assertThatThrownBy(() -> create(aCreateGuardian().name("   ").build())).isInstanceOf(ConstraintViolationException.class);

            assertThat(guardianRepository.storedGuardians()).isEmpty();
            verifyNoInteractions(auditDomainService);
        }

        @Test
        void should_refuse_a_name_longer_than_its_column() {
            assertThatThrownBy(() -> create(aCreateGuardian().name("n".repeat(513)).build())).isInstanceOf(
                ConstraintViolationException.class
            );
        }

        @Test
        void should_refuse_a_description_longer_than_its_column() {
            assertThatThrownBy(() -> create(aCreateGuardian().description("d".repeat(1025)).build())).isInstanceOf(
                ConstraintViolationException.class
            );
        }

        @Test
        void should_refuse_a_missing_phase() {
            assertThatThrownBy(() -> create(aCreateGuardian().phase(null).build())).isInstanceOf(ConstraintViolationException.class);
        }

        @Test
        void should_refuse_an_empty_api_type_set() {
            // an empty set means "any API" to the definition model, which 4.14 cannot honour
            assertThatThrownBy(() -> create(aCreateGuardian().apiTypes(Set.of()).build())).isInstanceOf(ConstraintViolationException.class);
        }

        @Test
        void should_refuse_an_id_that_is_not_a_uuid() {
            // the id lands in the Location header of the creation and in every later path, and the
            // server generates UUIDs itself
            assertThatThrownBy(() -> create(aCreateGuardian().id("chosen-id").build())).isInstanceOf(ConstraintViolationException.class);
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
