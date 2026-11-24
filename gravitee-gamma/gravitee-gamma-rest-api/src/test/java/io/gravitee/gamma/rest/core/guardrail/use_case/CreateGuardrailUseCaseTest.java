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
package io.gravitee.gamma.rest.core.guardrail.use_case;

import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.A_STORE_SECRET_REFERENCE;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.ENVIRONMENT_ID;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.GUARDRAIL_ID;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.ORGANIZATION_ID;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aCreateGuardrail;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aGuardrailValidator;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aPolicy;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aSecretReferenceValidatorWithoutPermissions;
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
import io.gravitee.apim.core.secret.exception.SecretReferenceNotAllowedException;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.guardrail.Guardrail.Phase;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailNameAlreadyExistsException;
import io.gravitee.gamma.rest.core.guardrail.inmemory.InMemoryGuardrailRepository;
import io.gravitee.gamma.rest.core.guardrail.model.CreateGuardrail;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailAuditEvent;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailLifecycleState;
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
class CreateGuardrailUseCaseTest {

    private final InMemoryGuardrailRepository guardrailRepository = new InMemoryGuardrailRepository();
    private final AuditDomainService auditDomainService = mock(AuditDomainService.class);

    private CreateGuardrailUseCase useCase;

    @BeforeEach
    void setUp() {
        guardrailRepository.reset();
        useCase = new CreateGuardrailUseCase(
            guardrailRepository,
            aGuardrailValidator(guardrailRepository),
            auditDomainService,
            aSecretReferenceValidatorWithoutPermissions()
        );
    }

    private CreateGuardrailUseCase.Output create(CreateGuardrail command) {
        return useCase.execute(
            CreateGuardrailUseCase.Input.builder().auditInfo(auditInfo(ENVIRONMENT_ID)).guardrailToCreate(command).build()
        );
    }

    /** The resources a Guardrail holds are judged before it is stored, as its steps are. */
    @Test
    void should_refuse_a_guardrail_holding_a_resource_the_node_cannot_run_and_store_nothing() {
        var command = aCreateGuardrail().resources(List.of(new Resource("classifier", "absent-from-the-node", "{}", true))).build();

        assertThatThrownBy(() -> create(command))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessage("Resource [classifier] references resource type [absent-from-the-node], which is not installed.");

        assertThat(guardrailRepository.storedGuardrails()).isEmpty();
    }

    /** The first write of the draft: what the first update has to state. */
    @Test
    void should_create_a_guardrail_at_version_1() {
        assertThat(create(aCreateGuardrail().build()).guardrail().getVersion()).isEqualTo(1);
    }

    @Test
    void should_refuse_a_guardrail_referencing_a_store_secret_without_the_secret_read_permission_and_store_nothing() {
        var command = aCreateGuardrail().name(A_STORE_SECRET_REFERENCE).build();

        assertThatThrownBy(() -> create(command)).isInstanceOf(SecretReferenceNotAllowedException.class);

        assertThat(guardrailRepository.storedGuardrails()).isEmpty();
    }

    @Test
    void should_create_an_undeployed_guardrail_in_the_context_environment() {
        var output = create(aCreateGuardrail().build());

        assertThat(output.guardrail())
            .extracting(g -> g.getEnvironmentId(), g -> g.getLifecycleState(), g -> g.getDeployedAt())
            .containsExactly(ENVIRONMENT_ID, GuardrailLifecycleState.UNDEPLOYED, null);
    }

    @Test
    void should_store_the_name_without_the_whitespace_around_it() {
        var output = create(aCreateGuardrail().name("  Credential leakage  ").build());

        assertThat(output.guardrail().getName()).isEqualTo("Credential leakage");
    }

    /** Padding is not a different name, so the one already taken is still taken. */
    @Test
    void should_refuse_a_name_a_padded_variant_of_which_is_taken() {
        create(aCreateGuardrail().name("Credential leakage").build());

        assertThatThrownBy(() -> create(aCreateGuardrail().id(null).name(" Credential leakage ").build())).isInstanceOf(
            GuardrailNameAlreadyExistsException.class
        );
    }

    @Test
    void should_store_the_whole_definition_and_the_description() {
        var output = create(aCreateGuardrail().build());

        assertThat(output.guardrail())
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
        var output = create(aCreateGuardrail().build());

        assertThat(output.guardrail().getDefinition().toString()).doesNotContain("Flags a prompt");
    }

    @Test
    void should_generate_an_id_when_the_payload_carries_none() {
        var output = create(aCreateGuardrail().build());

        assertThat(output.guardrail().getId()).isNotBlank();
        assertThat(guardrailRepository.storedGuardrails()).hasSize(1);
    }

    @Test
    void should_keep_the_id_the_payload_carries() {
        var output = create(aCreateGuardrail().id(GUARDRAIL_ID).build());

        assertThat(output.guardrail().getId()).isEqualTo(GUARDRAIL_ID);
    }

    @Test
    void should_generate_an_id_when_the_caller_sends_a_blank_one() {
        // Optional.ofNullable("") is present, so without the blank filter an empty id would be
        // stored and no /guardrails/{guardrailId} route could reach the Guardrail again
        var output = create(aCreateGuardrail().id("   ").build());

        assertThat(output.guardrail().getId()).isNotBlank().doesNotContain(" ");
    }

    @Test
    void should_record_the_creation_in_the_audit_trail() {
        var output = create(aCreateGuardrail().build());

        var log = recordedAuditLog();
        assertThat(log.event()).isEqualTo(GuardrailAuditEvent.GUARDRAIL_CREATED);
        assertThat(log.organizationId()).isEqualTo(ORGANIZATION_ID);
        assertThat(log.newValue()).isEqualTo(output.guardrail());
    }

    @Test
    void should_refuse_an_incompatible_guardrail_policy_before_creating_a_guardrail_or_an_audit() {
        var policy = aPolicy();
        policy.setConfiguration("{\"scope\":{\"protocol\":\"MCP\"}}");

        assertThatThrownBy(() -> create(aCreateGuardrail().policies(List.of(policy)).build()))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("Detect credentials")
            .hasMessageContaining("llm-proxy");

        assertThat(guardrailRepository.storedGuardrails()).as("a rejected guardrail policy leaves no Guardrail draft").isEmpty();
        verifyNoInteractions(auditDomainService);
    }

    @Test
    void should_refuse_an_api_type_other_than_llm_proxy() {
        assertThatThrownBy(() -> create(aCreateGuardrail().apiTypes(Set.of(ApiType.MCP_PROXY)).build()))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("llm-proxy");

        assertThat(guardrailRepository.storedGuardrails()).isEmpty();
    }

    /**
     * The constraints sit on {@link CreateGuardrail}, so they hold for every caller of the use case
     * and not only for one arriving through JAX-RS — which is the whole point of putting them there.
     * Each case would be a 500 if the use case trusted its input.
     */
    @Nested
    class Structure {

        @Test
        void should_refuse_a_blank_name() {
            assertThatThrownBy(() -> create(aCreateGuardrail().name("   ").build())).isInstanceOf(ConstraintViolationException.class);

            assertThat(guardrailRepository.storedGuardrails()).isEmpty();
            verifyNoInteractions(auditDomainService);
        }

        @Test
        void should_refuse_a_name_longer_than_its_column() {
            assertThatThrownBy(() -> create(aCreateGuardrail().name("n".repeat(513)).build())).isInstanceOf(
                ConstraintViolationException.class
            );
        }

        @Test
        void should_refuse_a_description_longer_than_its_column() {
            assertThatThrownBy(() -> create(aCreateGuardrail().description("d".repeat(1025)).build())).isInstanceOf(
                ConstraintViolationException.class
            );
        }

        @Test
        void should_refuse_a_missing_phase() {
            assertThatThrownBy(() -> create(aCreateGuardrail().phase(null).build())).isInstanceOf(ConstraintViolationException.class);
        }

        @Test
        void should_refuse_an_empty_api_type_set() {
            // an empty set means "any API" to the definition model, which 4.14 cannot honour
            assertThatThrownBy(() -> create(aCreateGuardrail().apiTypes(Set.of()).build())).isInstanceOf(
                ConstraintViolationException.class
            );
        }

        @Test
        void should_refuse_an_id_that_is_not_a_uuid() {
            // the id lands in the Location header of the creation and in every later path, and the
            // server generates UUIDs itself
            assertThatThrownBy(() -> create(aCreateGuardrail().id("chosen-id").build())).isInstanceOf(ConstraintViolationException.class);
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
