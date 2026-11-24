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
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.GUARDRAIL_POLICY;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.ORGANIZATION_ID;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aDeployGuardrailUseCase;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aGuardedApi;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aGuardrail;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aGuardrailDefinition;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aGuardrailUsage;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aGuardrailValidator;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aPolicyPluginProvider;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aSecretReferenceValidator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.AuditProperties;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.permission.domain_service.PermissionDomainService;
import io.gravitee.apim.core.secret.exception.SecretReferenceNotAllowedException;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.guardrail.GuardrailSteps;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardrail.Guardrail.Phase;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.gamma.rest.core.gravitee_plugin.inmemory.InMemoryPolicyValidation;
import io.gravitee.gamma.rest.core.guardrail.GuardrailAuditFixture;
import io.gravitee.gamma.rest.core.guardrail.GuardrailPublicationFixture;
import io.gravitee.gamma.rest.core.guardrail.domain_service.GuardrailDeploymentDomainService;
import io.gravitee.gamma.rest.core.guardrail.domain_service.GuardrailDraftDomainService;
import io.gravitee.gamma.rest.core.guardrail.domain_service.GuardrailUsageDomainService;
import io.gravitee.gamma.rest.core.guardrail.domain_service.ValidateGuardrailDomainService;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailDeploymentNotFoundException;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailInUseException;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailNameAlreadyExistsException;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailNotFoundException;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailOverwriteOvertakenException;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailVersionConflictException;
import io.gravitee.gamma.rest.core.guardrail.inmemory.InMemoryGuardedApiPort;
import io.gravitee.gamma.rest.core.guardrail.inmemory.InMemoryGuardrailRepository;
import io.gravitee.gamma.rest.core.guardrail.model.CreateGuardrail;
import io.gravitee.gamma.rest.core.guardrail.model.DraftPrecondition;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailAuditEvent;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailLifecycleState;
import io.gravitee.gamma.rest.core.guardrail.model.UpdateGuardrailCommand;
import io.gravitee.rest.api.model.EventType;
import jakarta.validation.ConstraintViolationException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RollbackGuardrailUseCaseTest {

    private static final String OTHER_GUARDRAIL_ID = "8c0e4d6b-2f71-4a9e-b5d3-1e6f9a2c7b85";

    private final InMemoryGuardrailRepository repository = new InMemoryGuardrailRepository();
    private final GuardrailPublicationFixture publications = new GuardrailPublicationFixture();
    private final GuardrailAuditFixture audits = new GuardrailAuditFixture();
    private final GuardrailDeploymentDomainService deploymentService = publications.service(repository);
    /** The schema answers, which a test installs to make a stored revision non-conforming. */
    private final InMemoryPolicyValidation policyValidation = new InMemoryPolicyValidation();

    private final ValidateGuardrailDomainService validator = aGuardrailValidator(repository, aPolicyPluginProvider(), policyValidation);
    private final GuardrailUsageDomainService usage = aGuardrailUsage();
    private final PermissionDomainService permissions = mock(PermissionDomainService.class);
    private final GuardrailDraftDomainService drafts = new GuardrailDraftDomainService(
        repository,
        validator,
        audits.service,
        deploymentService,
        usage,
        aSecretReferenceValidator(permissions)
    );
    private final DeployGuardrailUseCase deploy = aDeployGuardrailUseCase(repository, deploymentService, audits.service);
    private final UndeployGuardrailUseCase undeploy = new UndeployGuardrailUseCase(repository, deploymentService, audits.service, usage);
    private final UpdateGuardrailUseCase update = new UpdateGuardrailUseCase(repository, validator, drafts);
    private final RollbackGuardrailUseCase rollback = new RollbackGuardrailUseCase(repository, validator, publications.store(), drafts);
    private final AuditInfo actor = AuditInfo.builder()
        .organizationId(ORGANIZATION_ID)
        .environmentId(ENVIRONMENT_ID)
        .actor(AuditActor.builder().userId("deployer").build())
        .build();

    @BeforeEach
    void given_an_undeployed_guardrail() {
        TimeProvider.overrideClock(Clock.fixed(Instant.parse("2026-11-09T10:00:00Z"), ZoneOffset.UTC));
        repository.givenGuardrail(aGuardrail(definition -> definition.name("First name")).build());
    }

    @AfterEach
    void restore_clock() {
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    /** A rollback replaces the draft, so an editor who read it before has to be told it moved on. */
    @Test
    void should_count_a_rollback_as_a_write_of_the_draft() {
        repository.reset();
        repository.givenGuardrail(
            aGuardrail(definition -> definition.name("First name"))
                .version(3)
                .build()
        );
        var first = deployRevision();
        save("Second name", "Changed description");

        var result = rollback.execute(new RollbackGuardrailUseCase.Input(actor, GUARDRAIL_ID, first)).guardrail();

        assertThat(result.getVersion()).as("3, saved to 4, rolled back to 5").isEqualTo(5);
    }

    /**
     * A rollback states no version, so losing every attempt is a plain conflict and not a failed
     * precondition: the draft stays as the other writers stored it, and the rollback can be tried again.
     */
    @Test
    void should_answer_a_conflict_when_other_writers_overtake_the_rollback_every_time() {
        repository.reset();
        repository.givenGuardrail(
            aGuardrail(definition -> definition.name("First name"))
                .version(3)
                .build()
        );
        var first = deployRevision();
        save("Second name", "Changed description");
        repository.givenAnotherWriterAlwaysGetsInFirst(stored ->
            stored.toBuilder().description("Saved meanwhile").version(stored.getVersion() + 1).build()
        );

        assertThatThrownBy(() -> rollback.execute(new RollbackGuardrailUseCase.Input(actor, GUARDRAIL_ID, first)))
            .isInstanceOf(GuardrailOverwriteOvertakenException.class)
            .isNotInstanceOf(GuardrailVersionConflictException.class);

        assertThat(repository.findByEnvironmentId(ENVIRONMENT_ID, GUARDRAIL_ID)).hasValueSatisfying(stored -> {
            assertThat(stored.getName()).as("the draft, not rolled back").isEqualTo("Second name");
            assertThat(stored.getDescription()).as("as the other writers left it").isEqualTo("Saved meanwhile");
        });
    }

    /**
     * A rollback restores a definition and keeps the description, which is not part of one. A description
     * saved by someone else while the rollback was being stored is the one to keep, not the one it read first.
     */
    @Test
    void should_keep_a_description_saved_while_the_rollback_was_being_stored() {
        repository.reset();
        repository.givenGuardrail(
            aGuardrail(definition -> definition.name("First name"))
                .version(3)
                .build()
        );
        var first = deployRevision();
        save("Second name", "Changed description");
        repository.givenAnotherWriterGetsInFirst(stored ->
            stored.toBuilder().description("Saved meanwhile").version(stored.getVersion() + 1).build()
        );

        var result = rollback.execute(new RollbackGuardrailUseCase.Input(actor, GUARDRAIL_ID, first)).guardrail();

        assertSoftly(softly -> {
            softly.assertThat(result.getName()).as("restored definition").isEqualTo("First name");
            softly.assertThat(result.getDescription()).as("description kept as it is stored now").isEqualTo("Saved meanwhile");
        });
    }

    /** A node older than the counter writes a draft without version: nothing is left to compare, the description still is. */
    @Test
    void should_keep_a_description_saved_by_a_writer_that_left_no_version() {
        repository.reset();
        repository.givenGuardrail(
            aGuardrail(definition -> definition.name("First name"))
                .version(3)
                .build()
        );
        var first = deployRevision();
        save("Second name", "Changed description");
        repository.givenAnotherWriterGetsInFirst(stored -> stored.toBuilder().description("Saved meanwhile").version(null).build());

        var result = rollback.execute(new RollbackGuardrailUseCase.Input(actor, GUARDRAIL_ID, first)).guardrail();

        assertSoftly(softly -> {
            softly.assertThat(result.getName()).as("restored definition").isEqualTo("First name");
            softly.assertThat(result.getDescription()).as("description kept as it is stored now").isEqualTo("Saved meanwhile");
        });
    }

    @Test
    void should_restore_the_revision_as_a_pending_draft_without_deploying_it() {
        var first = deployRevision();
        save("Second name", "Changed description");
        var second = deployRevision();

        var result = rollback.execute(new RollbackGuardrailUseCase.Input(actor, GUARDRAIL_ID, first)).guardrail();

        assertSoftly(softly -> {
            softly.assertThat(result.getName()).as("restored definition").isEqualTo("First name");
            softly.assertThat(result.getDescription()).as("description kept").isEqualTo("Changed description");
            softly.assertThat(result.getLifecycleState()).as("differs from what runs").isEqualTo(GuardrailLifecycleState.PENDING);
            softly.assertThat(result.getDeployedRevisionId()).as("revision still running").isEqualTo(second);
            softly.assertThat(publications.publications()).as("nothing published").isEqualTo(2);
            softly
                .assertThat(repository.findByEnvironmentId(ENVIRONMENT_ID, GUARDRAIL_ID))
                .as("stored draft")
                .hasValueSatisfying(stored -> assertThat(stored.getName()).isEqualTo("First name"));
        });
    }

    /**
     * A rollback replays the revision through the update path, so the name it restores is normalised like
     * any other write — deliberately: a revision deployed before the rule is not a licence to put a padded
     * name back in the store. The same reasoning as the structure check this use case already runs, for a
     * constraint that may have tightened since.
     */
    @Test
    void should_restore_the_revision_name_without_the_whitespace_around_it() {
        // written straight to the history rather than through the update path, which strips: a revision
        // published before the rule is the only thing this can restore, and the only thing worth pinning
        publications.givenPublication(aGuardrailDefinition().name("  Padded name  ").build(), Instant.parse("2026-11-08T10:00:00Z"));
        var padded = publications.history.storage().getLast().getId();

        var result = rollback.execute(new RollbackGuardrailUseCase.Input(actor, GUARDRAIL_ID, padded)).guardrail();

        assertThat(result.getName()).isEqualTo("Padded name");
    }

    @Test
    void should_record_a_new_revision_when_the_rolled_back_draft_is_deployed() {
        var first = deployRevision();
        save("Second name", "description");
        var second = deployRevision();
        rollback.execute(new RollbackGuardrailUseCase.Input(actor, GUARDRAIL_ID, first));

        var redeployed = deploy.execute(new DeployGuardrailUseCase.Input(actor, GUARDRAIL_ID, null)).guardrail();

        assertSoftly(softly -> {
            softly.assertThat(redeployed.getDeployedRevisionId()).as("new revision").isNotIn(first, second);
            softly.assertThat(redeployed.getName()).as("rolled back definition deployed").isEqualTo("First name");
        });
    }

    @Test
    void should_be_deployed_again_when_rolling_back_to_the_running_revision() {
        var running = deployRevision();
        save("Second name", "description");

        var result = rollback.execute(new RollbackGuardrailUseCase.Input(actor, GUARDRAIL_ID, running)).guardrail();

        assertThat(result.getLifecycleState()).as("draft matches what runs").isEqualTo(GuardrailLifecycleState.DEPLOYED);
    }

    @Test
    void should_audit_the_rollback() {
        var first = deployRevision();
        save("Second name", "description");
        audits.reset();

        rollback.execute(new RollbackGuardrailUseCase.Input(actor, GUARDRAIL_ID, first));

        assertThat(audits.storage())
            .as("rollback audit")
            .singleElement()
            .satisfies(audit ->
                assertSoftly(softly -> {
                    softly.assertThat(audit.getEvent()).as("event").isEqualTo(GuardrailAuditEvent.GUARDRAIL_ROLLED_BACK.name());
                    softly
                        .assertThat(audit.getProperties())
                        .as("restored revision")
                        .containsEntry(AuditProperties.GUARDRAIL.name(), GUARDRAIL_ID)
                        .containsEntry(AuditProperties.GUARDRAIL_DEPLOYMENT.name(), first);
                })
            );
    }

    @Test
    void should_refuse_a_rollback_whose_draft_an_update_would_refuse() {
        var first = deployRevision();
        repository.reset();
        repository.givenGuardrail(
            aGuardrail(definition -> definition.name("First name"))
                .description("x".repeat(CreateGuardrail.DESCRIPTION_MAX_LENGTH + 1))
                .build()
        );
        audits.reset();

        assertThatThrownBy(() -> rollback.execute(new RollbackGuardrailUseCase.Input(actor, GUARDRAIL_ID, first))).isInstanceOf(
            ConstraintViolationException.class
        );
        assertThat(audits.storage()).as("no audit recorded").isEmpty();
    }

    /**
     * The revision goes through the update path, so it is refused as an update would be: what decides where a
     * Guardrail runs cannot change under the APIs running it.
     */
    @Test
    void should_refuse_a_revision_of_another_phase_while_an_api_runs_the_guardrail() {
        repository.update(aGuardrail(definition -> definition.name("First name").phase(Phase.REQUEST)).build());
        var first = deployRevision();
        repository.update(aGuardrail(definition -> definition.name("First name").phase(Phase.RESPONSE)).build());
        deployRevision();
        var apis = new InMemoryGuardedApiPort();
        apis.givenApi(
            aGuardedApi().internalFlow(Flow.builder().response(List.of(GuardrailSteps.of(GUARDRAIL_ID, "First name"))).build()).build()
        );
        var rollbackWhereApisRun = new RollbackGuardrailUseCase(
            repository,
            validator,
            publications.store(),
            new GuardrailDraftDomainService(
                repository,
                validator,
                audits.service,
                deploymentService,
                new GuardrailUsageDomainService(apis),
                aSecretReferenceValidator(permissions)
            )
        );

        assertThatThrownBy(() -> rollbackWhereApisRun.execute(new RollbackGuardrailUseCase.Input(actor, GUARDRAIL_ID, first)))
            .as("rolling back to a revision of another phase a guardrail an API runs")
            .isInstanceOf(GuardrailInUseException.class);
        assertThat(repository.findByEnvironmentId(ENVIRONMENT_ID, GUARDRAIL_ID))
            .as("draft unchanged")
            .hasValueSatisfying(stored -> assertThat(stored.getDefinition().getPhase()).isEqualTo(Phase.RESPONSE));
    }

    @Test
    void should_refuse_a_name_another_guardrail_took_since_and_leave_the_draft_unchanged() {
        var first = deployRevision();
        save("Second name", "description");
        repository.givenGuardrail(aGuardrail(definition -> definition.id(OTHER_GUARDRAIL_ID).name("First name")).build());
        audits.reset();

        assertThatThrownBy(() -> rollback.execute(new RollbackGuardrailUseCase.Input(actor, GUARDRAIL_ID, first))).isInstanceOf(
            GuardrailNameAlreadyExistsException.class
        );
        assertSoftly(softly -> {
            softly
                .assertThat(repository.findByEnvironmentId(ENVIRONMENT_ID, GUARDRAIL_ID))
                .as("draft unchanged")
                .hasValueSatisfying(stored -> assertThat(stored.getName()).isEqualTo("Second name"));
            softly.assertThat(audits.storage()).as("no audit recorded").isEmpty();
        });
    }

    @Test
    void should_refuse_a_revision_whose_guardrail_policy_is_no_longer_installed() {
        publications.givenPublication(
            aGuardrailDefinition()
                .name("First name")
                .policies(List.of(Step.builder().name("Removed").policy("uninstalled-policy").enabled(true).build()))
                .build(),
            Instant.parse("2026-11-08T10:00:00Z")
        );
        var revision = publications.history.storage().getLast().getId();

        assertThatThrownBy(() -> rollback.execute(new RollbackGuardrailUseCase.Input(actor, GUARDRAIL_ID, revision))).isInstanceOf(
            ValidationDomainException.class
        );
    }

    /** A revision deployed before resources were judged, or whose resource type was uninstalled since. */
    @Test
    void should_refuse_a_revision_holding_a_resource_the_node_cannot_run_and_leave_the_draft_unchanged() {
        publications.givenPublication(
            aGuardrailDefinition()
                .name("First name")
                .resources(List.of(new Resource("classifier", "absent-from-the-node", "{}", true)))
                .build(),
            Instant.parse("2026-11-08T10:00:00Z")
        );
        var revision = publications.history.storage().getLast().getId();
        var draft = repository.storedGuardrails();

        assertThatThrownBy(() -> rollback.execute(new RollbackGuardrailUseCase.Input(actor, GUARDRAIL_ID, revision)))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessage("Resource [classifier] references resource type [absent-from-the-node], which is not installed.");

        assertThat(repository.storedGuardrails()).isEqualTo(draft);
    }

    @Test
    void should_refuse_a_revision_whose_step_configuration_the_policy_schema_no_longer_accepts() {
        var revision = deployRevision();
        save("Second name", "description");
        policyValidation.givenRefuses(GUARDRAIL_POLICY, "$.rules: is missing but it is required");

        assertSoftly(softly -> {
            softly
                .assertThatThrownBy(() -> rollback.execute(new RollbackGuardrailUseCase.Input(actor, GUARDRAIL_ID, revision)))
                .as(
                    "a policy upgraded to a stricter schema closes the way back, on purpose: restoring a draft the gateways " +
                        "could not start would only move the failure to the next deployment"
                )
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContainingAll("Detect credentials", "$.rules");
            softly
                .assertThat(repository.findByEnvironmentId(ENVIRONMENT_ID, GUARDRAIL_ID))
                .as("draft unchanged")
                .hasValueSatisfying(stored -> assertThat(stored.getName()).isEqualTo("Second name"));
        });
    }

    @Test
    void should_refuse_a_revision_its_policy_would_only_accept_corrected() {
        var revision = deployRevision();
        save("Second name", "description");
        // the policy gained the default of a required property since the revision was published
        policyValidation.givenAccepts("{}", "{\"mode\":\"BLOCK\"}");

        assertSoftly(softly -> {
            softly
                .assertThatThrownBy(() -> rollback.execute(new RollbackGuardrailUseCase.Input(actor, GUARDRAIL_ID, revision)))
                .as(
                    "a revision is restored as it was published: storing the corrected one would store a definition no gateway " +
                        "ever ran, and leave the Guardrail PENDING straight after the rollback"
                )
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContainingAll("Detect credentials", "edit and save the Guardrail");
            softly
                .assertThat(repository.findByEnvironmentId(ENVIRONMENT_ID, GUARDRAIL_ID))
                .as("draft unchanged")
                .hasValueSatisfying(stored -> assertThat(stored.getName()).isEqualTo("Second name"));
        });
    }

    @Test
    void should_refuse_an_unknown_revision_a_withdrawal_or_a_revision_of_another_guardrail() {
        repository.givenGuardrail(aGuardrail(definition -> definition.id(OTHER_GUARDRAIL_ID).name("Other")).build());
        deploy.execute(new DeployGuardrailUseCase.Input(actor, OTHER_GUARDRAIL_ID, null));
        var other = publications.history.storage().getLast().getId();
        deployRevision();
        undeploy.execute(new UndeployGuardrailUseCase.Input(actor, GUARDRAIL_ID));
        var withdrawal = publications.history
            .storage()
            .stream()
            .filter(event -> event.getType() == EventType.UNPUBLISH_GENERIC_EVENT)
            .map(Event::getId)
            .findFirst()
            .orElseThrow();

        assertSoftly(softly -> {
            for (var revision : List.of("unknown", withdrawal, other)) {
                softly
                    .assertThatThrownBy(() -> rollback.execute(new RollbackGuardrailUseCase.Input(actor, GUARDRAIL_ID, revision)))
                    .as("revision %s", revision)
                    .isInstanceOf(GuardrailDeploymentNotFoundException.class);
            }
        });
    }

    @Test
    void should_refuse_a_guardrail_unknown_in_the_environment() {
        var revision = deployRevision();
        var foreign = AuditInfo.builder().organizationId(ORGANIZATION_ID).environmentId("another-environment").actor(actor.actor()).build();

        assertThatThrownBy(() -> rollback.execute(new RollbackGuardrailUseCase.Input(foreign, GUARDRAIL_ID, revision))).isInstanceOf(
            GuardrailNotFoundException.class
        );
    }

    @Test
    void should_refuse_a_rollback_bringing_back_a_store_secret_reference_without_the_secret_read_permission() {
        when(permissions.hasPermission(any(), any(), any(), any(), any())).thenReturn(true);
        save(A_STORE_SECRET_REFERENCE, "description");
        var withReference = deployRevision();
        save("Plain name", "description");
        when(permissions.hasPermission(any(), any(), any(), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> rollback.execute(new RollbackGuardrailUseCase.Input(actor, GUARDRAIL_ID, withReference))).isInstanceOf(
            SecretReferenceNotAllowedException.class
        );

        assertThat(repository.findByEnvironmentId(ENVIRONMENT_ID, GUARDRAIL_ID)).hasValueSatisfying(stored ->
            assertThat(stored.getName()).isEqualTo("Plain name")
        );
    }

    /** Deploys the current draft, then answers the id of the revision the deployment recorded. */
    private String deployRevision() {
        deploy.execute(new DeployGuardrailUseCase.Input(actor, GUARDRAIL_ID, null));
        return publications.history.storage().getLast().getId();
    }

    private void save(String name, String description) {
        update.execute(
            new UpdateGuardrailUseCase.Input(
                actor,
                GUARDRAIL_ID,
                UpdateGuardrailCommand.builder().definition(aGuardrailDefinition().name(name).build()).description(description).build(),
                DraftPrecondition.anyVersion()
            )
        );
    }
}
