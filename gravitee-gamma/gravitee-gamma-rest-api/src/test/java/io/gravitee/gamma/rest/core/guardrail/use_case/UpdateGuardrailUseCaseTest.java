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
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aDefinitionToSubmit;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aGuardrail;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aGuardrailUsage;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aGuardrailUsageRunning;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aGuardrailValidator;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aSecretReferenceValidatorWithoutPermissions;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.anUpdateGuardrailCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import io.gravitee.gamma.rest.core.guardrail.GuardrailPublicationFixture;
import io.gravitee.gamma.rest.core.guardrail.domain_service.GuardrailDraftDomainService;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailInUseException;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailNameAlreadyExistsException;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailNotFoundException;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailOverwriteOvertakenException;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailVersionConflictException;
import io.gravitee.gamma.rest.core.guardrail.inmemory.InMemoryGuardrailRepository;
import io.gravitee.gamma.rest.core.guardrail.model.DraftPrecondition;
import io.gravitee.gamma.rest.core.guardrail.model.Guardrail;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailAuditEvent;
import io.gravitee.gamma.rest.core.guardrail.model.UpdateGuardrailCommand;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UpdateGuardrailUseCaseTest {

    private static final Instant DEPLOYED_AT = Instant.parse("2026-03-01T10:00:00Z");

    private final InMemoryGuardrailRepository guardrailRepository = new InMemoryGuardrailRepository();
    private final AuditDomainService auditDomainService = mock(AuditDomainService.class);

    private UpdateGuardrailUseCase useCase;
    private UpdateGuardrailUseCase runByAnApi;

    @BeforeEach
    void setUp() {
        guardrailRepository.reset();
        guardrailRepository.givenGuardrail(aGuardrail().deployedAt(DEPLOYED_AT).build());
        var publications = new GuardrailPublicationFixture();
        publications.givenPublication(aGuardrail().build().getDefinition(), DEPLOYED_AT);
        var deployments = publications.service(guardrailRepository);
        var validator = aGuardrailValidator(guardrailRepository);
        useCase = new UpdateGuardrailUseCase(
            guardrailRepository,
            validator,
            new GuardrailDraftDomainService(
                guardrailRepository,
                validator,
                auditDomainService,
                deployments,
                aGuardrailUsage(),
                aSecretReferenceValidatorWithoutPermissions()
            )
        );
        runByAnApi = new UpdateGuardrailUseCase(
            guardrailRepository,
            validator,
            new GuardrailDraftDomainService(
                guardrailRepository,
                validator,
                auditDomainService,
                deployments,
                aGuardrailUsageRunning(GUARDRAIL_ID),
                aSecretReferenceValidatorWithoutPermissions()
            )
        );
    }

    static Stream<DraftPrecondition> everyPrecondition() {
        return Stream.of(DraftPrecondition.version(3), DraftPrecondition.anyVersion());
    }

    /** An update that overwrites whatever is stored, for the tests that are not about the version. */
    private UpdateGuardrailUseCase.Output update(String environmentId, UpdateGuardrailCommand command) {
        return update(useCase, environmentId, command, DraftPrecondition.anyVersion());
    }

    private UpdateGuardrailUseCase.Output update(String environmentId, UpdateGuardrailCommand command, DraftPrecondition precondition) {
        return update(useCase, environmentId, command, precondition);
    }

    private UpdateGuardrailUseCase.Output update(UpdateGuardrailUseCase target, String environmentId, UpdateGuardrailCommand command) {
        return update(target, environmentId, command, DraftPrecondition.anyVersion());
    }

    private UpdateGuardrailUseCase.Output update(
        UpdateGuardrailUseCase target,
        String environmentId,
        UpdateGuardrailCommand command,
        DraftPrecondition precondition
    ) {
        return target.execute(
            UpdateGuardrailUseCase.Input.builder()
                .auditInfo(auditInfo(environmentId))
                .guardrailId(GUARDRAIL_ID)
                .guardrailToUpdate(command)
                .precondition(precondition)
                .build()
        );
    }

    /**
     * A writer states the version of the draft it read. The write goes through only if the draft is still
     * at that version, so two people — or two tabs — editing one Guardrail cannot silently undo each other.
     */
    @Nested
    class The_version_an_update_is_built_on {

        private void givenStoredAt(Integer version) {
            guardrailRepository.reset();
            guardrailRepository.givenGuardrail(aGuardrail().version(version).build());
        }

        private UpdateGuardrailCommand aRename(String name) {
            return anUpdateGuardrailCommand().definition(aDefinitionToSubmit().name(name).build()).build();
        }

        @Test
        void should_store_an_update_built_on_the_stored_version_and_count_it() {
            givenStoredAt(3);

            var output = update(ENVIRONMENT_ID, aRename("Renamed"), DraftPrecondition.version(3));

            assertThat(output.guardrail()).extracting(Guardrail::getName, Guardrail::getVersion).containsExactly("Renamed", 4);
            assertThat(guardrailRepository.storedGuardrails()).singleElement().extracting(Guardrail::getVersion).isEqualTo(4);
        }

        @Test
        void should_refuse_an_update_built_on_an_older_version_and_store_nothing() {
            givenStoredAt(4);

            assertThatThrownBy(() -> update(ENVIRONMENT_ID, aRename("Renamed"), DraftPrecondition.version(3)))
                .isInstanceOf(GuardrailVersionConflictException.class)
                .hasMessageContaining("the current version is 4");

            assertThat(guardrailRepository.storedGuardrails())
                .singleElement()
                .extracting(Guardrail::getName, Guardrail::getVersion)
                .containsExactly("Credential leakage", 4);
            verifyNoInteractions(auditDomainService);
        }

        /** What a client reloads from, without a second request that would race with the next writer. */
        @Test
        void should_hand_the_refused_caller_the_guardrail_as_it_is_now() {
            givenStoredAt(4);

            assertThatThrownBy(() -> update(ENVIRONMENT_ID, aRename("Renamed"), DraftPrecondition.version(3)))
                .asInstanceOf(InstanceOfAssertFactories.type(GuardrailVersionConflictException.class))
                .extracting(GuardrailVersionConflictException::getCurrent)
                .extracting(Guardrail::getId, Guardrail::getVersion)
                .containsExactly(GUARDRAIL_ID, 4);
        }

        /** A stale payload is refused for being stale, not for what validating it would happen to find. */
        @Test
        void should_refuse_a_stale_update_before_judging_its_content() {
            givenStoredAt(4);
            var unsupported = anUpdateGuardrailCommand()
                .definition(aDefinitionToSubmit().apiTypes(Set.of(ApiType.MCP_PROXY)).build())
                .build();

            assertThatThrownBy(() -> update(ENVIRONMENT_ID, unsupported, DraftPrecondition.version(3))).isInstanceOf(
                GuardrailVersionConflictException.class
            );
        }

        @Test
        void should_accept_any_of_the_versions_stated() {
            givenStoredAt(4);

            var output = update(ENVIRONMENT_ID, aRename("Renamed"), DraftPrecondition.oneOf(Set.of(2, 4)));

            assertThat(output.guardrail().getVersion()).isEqualTo(5);
        }

        @Test
        void should_overwrite_whatever_is_stored_when_asked_to_and_still_count_it() {
            givenStoredAt(4);

            var output = update(ENVIRONMENT_ID, aRename("Renamed"), DraftPrecondition.anyVersion());

            assertThat(output.guardrail()).extracting(Guardrail::getName, Guardrail::getVersion).containsExactly("Renamed", 5);
        }

        /** Another writer got in between the read and the write: the store, not the check before it, has the last word. */
        @Test
        void should_refuse_an_update_another_writer_overtook_after_it_was_checked() {
            givenStoredAt(3);
            guardrailRepository.givenAnotherWriterGetsInFirst(stored -> stored.toBuilder().version(4).build());

            assertThatThrownBy(() -> update(ENVIRONMENT_ID, aRename("Renamed"), DraftPrecondition.version(3)))
                .isInstanceOf(GuardrailVersionConflictException.class)
                .hasMessageContaining("the current version is 4");

            assertThat(guardrailRepository.storedGuardrails())
                .singleElement()
                .extracting(Guardrail::getName)
                .isEqualTo("Credential leakage");
        }

        /**
         * An overwrite that crossed another writer must not reuse the number that writer took: two drafts
         * under one version would let a third writer pass a precondition against a draft it never read.
         */
        @Test
        void should_give_an_overwrite_that_crossed_another_writer_a_version_of_its_own() {
            givenStoredAt(3);
            guardrailRepository.givenAnotherWriterGetsInFirst(stored -> stored.toBuilder().version(4).build());

            var output = update(ENVIRONMENT_ID, aRename("Renamed"), DraftPrecondition.anyVersion());

            assertThat(output.guardrail()).extracting(Guardrail::getName, Guardrail::getVersion).containsExactly("Renamed", 5);
        }

        /** An overwrite does not give up at the first lost race: it counts again from what it finds, each time. */
        @Test
        void should_store_an_overwrite_that_lost_twice_and_won_the_third_attempt() {
            givenStoredAt(3);
            guardrailRepository.givenAnotherWriterGetsInFirst(2, stored -> stored.toBuilder().version(stored.getVersion() + 1).build());

            var output = update(ENVIRONMENT_ID, aRename("Renamed"), DraftPrecondition.anyVersion());

            assertThat(output.guardrail()).extracting(Guardrail::getName, Guardrail::getVersion).containsExactly("Renamed", 6);
        }

        /**
         * Answered as a plain conflict rather than settled by a blind write, which would reuse a number just
         * taken — and not as a failed precondition, since an overwrite states none.
         */
        @Test
        void should_refuse_an_overwrite_that_another_writer_overtakes_every_time() {
            givenStoredAt(3);
            guardrailRepository.givenAnotherWriterAlwaysGetsInFirst(stored -> stored.toBuilder().version(stored.getVersion() + 1).build());

            assertThatThrownBy(() -> update(ENVIRONMENT_ID, aRename("Renamed"), DraftPrecondition.anyVersion()))
                .isInstanceOf(GuardrailOverwriteOvertakenException.class)
                .isNotInstanceOf(GuardrailVersionConflictException.class)
                .hasMessageContaining("try again");

            assertThat(guardrailRepository.storedGuardrails())
                .singleElement()
                .extracting(Guardrail::getName)
                .isEqualTo("Credential leakage");
            verifyNoInteractions(auditDomainService);
        }

        /** The audit shows the draft this write took the place of, which is no longer the one read before the race. */
        @Test
        void should_audit_the_draft_an_overwrite_actually_replaced() {
            givenStoredAt(3);
            guardrailRepository.givenAnotherWriterGetsInFirst(stored ->
                stored.toBuilder().description("Written by someone else").version(4).build()
            );

            update(ENVIRONMENT_ID, aRename("Renamed"), DraftPrecondition.anyVersion());

            var audit = ArgumentCaptor.forClass(EnvironmentAuditLogEntity.class);
            verify(auditDomainService).createEnvironmentAuditLog(audit.capture());
            assertThat(audit.getValue().oldValue())
                .asInstanceOf(InstanceOfAssertFactories.type(Guardrail.class))
                .extracting(Guardrail::getDescription, Guardrail::getVersion)
                .containsExactly("Written by someone else", 4);
        }

        @ParameterizedTest
        @MethodSource("io.gravitee.gamma.rest.core.guardrail.use_case.UpdateGuardrailUseCaseTest#everyPrecondition")
        void should_answer_not_found_when_another_writer_deleted_the_guardrail_meanwhile(DraftPrecondition precondition) {
            givenStoredAt(3);
            guardrailRepository.givenAnotherWriterGetsInFirst(stored -> null);

            assertThatThrownBy(() -> update(ENVIRONMENT_ID, aRename("Renamed"), precondition)).isInstanceOf(
                GuardrailNotFoundException.class
            );
        }

        @Nested
        class A_guardrail_stored_before_the_counter_existed {

            /** No version is no version: it matches none a caller could state. */
            @Test
            void should_satisfy_no_stated_version() {
                givenStoredAt(null);

                assertThatThrownBy(() -> update(ENVIRONMENT_ID, aRename("Renamed"), DraftPrecondition.version(1))).isInstanceOf(
                    GuardrailVersionConflictException.class
                );
            }

            @Test
            void should_get_its_first_version_from_an_overwrite() {
                givenStoredAt(null);

                var output = update(ENVIRONMENT_ID, aRename("Renamed"), DraftPrecondition.anyVersion());

                assertThat(output.guardrail().getVersion()).isEqualTo(1);
            }
        }

        @Test
        void should_refuse_an_update_that_states_nothing() {
            assertThatThrownBy(() ->
                UpdateGuardrailUseCase.Input.builder()
                    .auditInfo(auditInfo(ENVIRONMENT_ID))
                    .guardrailId(GUARDRAIL_ID)
                    .guardrailToUpdate(aRename("Renamed"))
                    .build()
            ).isInstanceOf(NullPointerException.class);
        }
    }

    @Test
    void should_refuse_an_update_adding_a_store_secret_reference_without_the_secret_read_permission_and_store_nothing() {
        assertThatThrownBy(() ->
            update(
                ENVIRONMENT_ID,
                anUpdateGuardrailCommand().definition(aDefinitionToSubmit().name(A_STORE_SECRET_REFERENCE).build()).build()
            )
        ).isInstanceOf(SecretReferenceNotAllowedException.class);

        assertThat(guardrailRepository.findByEnvironmentId(ENVIRONMENT_ID, GUARDRAIL_ID)).hasValueSatisfying(stored ->
            assertThat(stored.getName()).isEqualTo(aGuardrail().build().getName())
        );
    }

    @Test
    void should_store_the_name_without_the_whitespace_around_it() {
        var output = update(
            ENVIRONMENT_ID,
            anUpdateGuardrailCommand().definition(aDefinitionToSubmit().name("  Renamed  ").build()).build()
        );

        assertThat(output.guardrail().getName()).isEqualTo("Renamed");
    }

    /** Padding is not a different name, so a name another Guardrail of the environment holds is still refused. */
    @Test
    void should_refuse_a_rename_onto_a_padded_variant_of_a_taken_name() {
        guardrailRepository.givenGuardrail(aGuardrail(definition -> definition.id("other-id").name("Taken")).build());

        assertThatThrownBy(() ->
            update(ENVIRONMENT_ID, anUpdateGuardrailCommand().definition(aDefinitionToSubmit().name(" Taken ").build()).build())
        ).isInstanceOf(GuardrailNameAlreadyExistsException.class);
    }

    /** An update replaces the resources with the rest of the definition, and judges them the same way. */
    @Test
    void should_refuse_an_update_holding_a_resource_the_node_cannot_run_and_leave_the_draft_unchanged() {
        var command = anUpdateGuardrailCommand()
            .definition(aDefinitionToSubmit().resources(List.of(new Resource("classifier", "absent-from-the-node", "{}", true))).build())
            .build();

        assertThatThrownBy(() -> update(ENVIRONMENT_ID, command))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessage("Resource [classifier] references resource type [absent-from-the-node], which is not installed.");

        assertThat(guardrailRepository.storedGuardrails()).singleElement().extracting(Guardrail::getResources).isEqualTo(List.of());
        verifyNoInteractions(auditDomainService);
    }

    @Test
    void should_replace_the_whole_definition_and_the_description() {
        var output = update(
            ENVIRONMENT_ID,
            anUpdateGuardrailCommand()
                .definition(aDefinitionToSubmit().name("Renamed").phase(Phase.RESPONSE).policies(List.of()).build())
                .description("A new description")
                .build()
        );

        assertThat(output.guardrail())
            .extracting(Guardrail::getName, Guardrail::getPhase, Guardrail::getPolicies, Guardrail::getDescription)
            .containsExactly("Renamed", Phase.RESPONSE, List.of(), "A new description");
    }

    @Test
    void should_clear_the_description_when_the_payload_omits_it() {
        // a PUT replaces: leaving the description out is asking for it to be gone
        var output = update(ENVIRONMENT_ID, anUpdateGuardrailCommand().description(null).build());

        assertThat(output.guardrail().getDescription()).isNull();
    }

    @Test
    void should_keep_the_identity_and_the_deployment_stamp_whatever_the_payload_says() {
        var output = update(
            ENVIRONMENT_ID,
            anUpdateGuardrailCommand()
                .definition(aDefinitionToSubmit().id("0b0c2a6e-3f1d-4a7b-9c8e-5d4f3a2b1c0d").environmentId("other-env").build())
                .build()
        );

        assertThat(output.guardrail())
            .extracting(Guardrail::getId, Guardrail::getEnvironmentId, Guardrail::getDeployedAt)
            .containsExactly(GUARDRAIL_ID, ENVIRONMENT_ID, DEPLOYED_AT);
    }

    @Test
    void should_touch_the_update_date() {
        var before = aGuardrail().build().getUpdatedAt();

        var output = update(ENVIRONMENT_ID, anUpdateGuardrailCommand().build());

        assertThat(output.guardrail().getUpdatedAt()).isAfter(before);
    }

    @Test
    void should_record_the_update_in_the_audit_trail() {
        var output = update(ENVIRONMENT_ID, anUpdateGuardrailCommand().build());

        var log = recordedAuditLog();
        assertThat(log.event()).isEqualTo(GuardrailAuditEvent.GUARDRAIL_UPDATED);
        assertThat(((Guardrail) log.oldValue()).getName()).isEqualTo("Credential leakage");
        assertThat(log.newValue()).isEqualTo(output.guardrail());
    }

    @Test
    void should_refuse_to_move_a_guardrail_onto_an_unsupported_api_type() {
        assertThatThrownBy(() ->
            update(
                ENVIRONMENT_ID,
                anUpdateGuardrailCommand().definition(aDefinitionToSubmit().apiTypes(Set.of(ApiType.MCP_PROXY)).build()).build()
            )
        )
            .isInstanceOf(ValidationDomainException.class)
            .hasMessageContaining("llm-proxy");

        verifyNoInteractions(auditDomainService);
    }

    @Test
    void should_refuse_to_change_the_phase_of_a_guardrail_an_api_runs() {
        var command = anUpdateGuardrailCommand().definition(aDefinitionToSubmit().phase(Phase.RESPONSE).build()).build();

        assertThatThrownBy(() -> update(runByAnApi, ENVIRONMENT_ID, command))
            .as("moving a guardrail in use to another phase")
            .isInstanceOf(GuardrailInUseException.class);
    }

    @Test
    void should_update_the_rest_of_the_definition_of_a_guardrail_an_api_runs() {
        var command = anUpdateGuardrailCommand().definition(aDefinitionToSubmit().name("Renamed").build()).build();

        var output = update(runByAnApi, ENVIRONMENT_ID, command);

        assertThat(output.guardrail().getName()).as("name").isEqualTo("Renamed");
    }

    @Test
    void should_refuse_to_update_a_guardrail_of_another_environment() {
        assertThatThrownBy(() -> update("other-env", anUpdateGuardrailCommand().build())).isInstanceOf(GuardrailNotFoundException.class);

        verifyNoInteractions(auditDomainService);
    }

    /** As on creation, the command's own constraints hold for any caller of the use case. */
    @Nested
    class Structure {

        @Test
        void should_refuse_a_missing_definition() {
            assertThatThrownBy(() -> update(ENVIRONMENT_ID, anUpdateGuardrailCommand().definition(null).build())).isInstanceOf(
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
                update(ENVIRONMENT_ID, anUpdateGuardrailCommand().definition(aDefinitionToSubmit().name("   ").build()).build())
            ).isInstanceOf(ConstraintViolationException.class);

            verifyNoInteractions(auditDomainService);
        }

        @Test
        void should_refuse_a_missing_phase_on_the_merged_definition() {
            assertThatThrownBy(() ->
                update(ENVIRONMENT_ID, anUpdateGuardrailCommand().definition(aDefinitionToSubmit().phase(null).build()).build())
            ).isInstanceOf(ConstraintViolationException.class);
        }

        @Test
        void should_refuse_a_description_longer_than_its_column() {
            assertThatThrownBy(() -> update(ENVIRONMENT_ID, anUpdateGuardrailCommand().description("d".repeat(1025)).build())).isInstanceOf(
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
