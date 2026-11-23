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

import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.A_STORE_SECRET_REFERENCE;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.ENVIRONMENT_ID;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.GUARDIAN_ID;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.GUARDRAIL_POLICY;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.ORGANIZATION_ID;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aDeployGuardianUseCase;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardedApi;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardian;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardianDefinition;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardianUsage;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardianValidator;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aPolicyPluginProvider;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aSecretReferenceValidator;
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
import io.gravitee.definition.model.guardian.GuardianSteps;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardian.Guardian.Phase;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.gamma.rest.core.gravitee_plugin.inmemory.InMemoryPolicyValidation;
import io.gravitee.gamma.rest.core.guardian.GuardianAuditFixture;
import io.gravitee.gamma.rest.core.guardian.GuardianPublicationFixture;
import io.gravitee.gamma.rest.core.guardian.domain_service.GuardianDeploymentDomainService;
import io.gravitee.gamma.rest.core.guardian.domain_service.GuardianDraftDomainService;
import io.gravitee.gamma.rest.core.guardian.domain_service.GuardianUsageDomainService;
import io.gravitee.gamma.rest.core.guardian.domain_service.ValidateGuardianDomainService;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianDeploymentNotFoundException;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianInUseException;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianNameAlreadyExistsException;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianNotFoundException;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianOverwriteOvertakenException;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianVersionConflictException;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardedApiPort;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardianRepository;
import io.gravitee.gamma.rest.core.guardian.model.CreateGuardian;
import io.gravitee.gamma.rest.core.guardian.model.DraftPrecondition;
import io.gravitee.gamma.rest.core.guardian.model.GuardianAuditEvent;
import io.gravitee.gamma.rest.core.guardian.model.GuardianLifecycleState;
import io.gravitee.gamma.rest.core.guardian.model.UpdateGuardianCommand;
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
class RollbackGuardianUseCaseTest {

    private static final String OTHER_GUARDIAN_ID = "8c0e4d6b-2f71-4a9e-b5d3-1e6f9a2c7b85";

    private final InMemoryGuardianRepository repository = new InMemoryGuardianRepository();
    private final GuardianPublicationFixture publications = new GuardianPublicationFixture();
    private final GuardianAuditFixture audits = new GuardianAuditFixture();
    private final GuardianDeploymentDomainService deploymentService = publications.service(repository);
    /** The schema answers, which a test installs to make a stored revision non-conforming. */
    private final InMemoryPolicyValidation policyValidation = new InMemoryPolicyValidation();

    private final ValidateGuardianDomainService validator = aGuardianValidator(repository, aPolicyPluginProvider(), policyValidation);
    private final GuardianUsageDomainService usage = aGuardianUsage();
    private final PermissionDomainService permissions = mock(PermissionDomainService.class);
    private final GuardianDraftDomainService drafts = new GuardianDraftDomainService(
        repository,
        validator,
        audits.service,
        deploymentService,
        usage,
        aSecretReferenceValidator(permissions)
    );
    private final DeployGuardianUseCase deploy = aDeployGuardianUseCase(repository, deploymentService, audits.service);
    private final UndeployGuardianUseCase undeploy = new UndeployGuardianUseCase(repository, deploymentService, audits.service, usage);
    private final UpdateGuardianUseCase update = new UpdateGuardianUseCase(repository, validator, drafts);
    private final RollbackGuardianUseCase rollback = new RollbackGuardianUseCase(repository, validator, publications.store(), drafts);
    private final AuditInfo actor = AuditInfo.builder()
        .organizationId(ORGANIZATION_ID)
        .environmentId(ENVIRONMENT_ID)
        .actor(AuditActor.builder().userId("deployer").build())
        .build();

    @BeforeEach
    void given_an_undeployed_guardian() {
        TimeProvider.overrideClock(Clock.fixed(Instant.parse("2026-11-09T10:00:00Z"), ZoneOffset.UTC));
        repository.givenGuardian(aGuardian(definition -> definition.name("First name")).build());
    }

    @AfterEach
    void restore_clock() {
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    /** A rollback replaces the draft, so an editor who read it before has to be told it moved on. */
    @Test
    void should_count_a_rollback_as_a_write_of_the_draft() {
        repository.reset();
        repository.givenGuardian(
            aGuardian(definition -> definition.name("First name"))
                .version(3)
                .build()
        );
        var first = deployRevision();
        save("Second name", "Changed description");

        var result = rollback.execute(new RollbackGuardianUseCase.Input(actor, GUARDIAN_ID, first)).guardian();

        assertThat(result.getVersion()).as("3, saved to 4, rolled back to 5").isEqualTo(5);
    }

    /**
     * A rollback states no version, so losing every attempt is a plain conflict and not a failed
     * precondition: the draft stays as the other writers stored it, and the rollback can be tried again.
     */
    @Test
    void should_answer_a_conflict_when_other_writers_overtake_the_rollback_every_time() {
        repository.reset();
        repository.givenGuardian(
            aGuardian(definition -> definition.name("First name"))
                .version(3)
                .build()
        );
        var first = deployRevision();
        save("Second name", "Changed description");
        repository.givenAnotherWriterAlwaysGetsInFirst(stored ->
            stored.toBuilder().description("Saved meanwhile").version(stored.getVersion() + 1).build()
        );

        assertThatThrownBy(() -> rollback.execute(new RollbackGuardianUseCase.Input(actor, GUARDIAN_ID, first)))
            .isInstanceOf(GuardianOverwriteOvertakenException.class)
            .isNotInstanceOf(GuardianVersionConflictException.class);

        assertThat(repository.findByEnvironmentId(ENVIRONMENT_ID, GUARDIAN_ID)).hasValueSatisfying(stored -> {
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
        repository.givenGuardian(
            aGuardian(definition -> definition.name("First name"))
                .version(3)
                .build()
        );
        var first = deployRevision();
        save("Second name", "Changed description");
        repository.givenAnotherWriterGetsInFirst(stored ->
            stored.toBuilder().description("Saved meanwhile").version(stored.getVersion() + 1).build()
        );

        var result = rollback.execute(new RollbackGuardianUseCase.Input(actor, GUARDIAN_ID, first)).guardian();

        assertSoftly(softly -> {
            softly.assertThat(result.getName()).as("restored definition").isEqualTo("First name");
            softly.assertThat(result.getDescription()).as("description kept as it is stored now").isEqualTo("Saved meanwhile");
        });
    }

    /** A node older than the counter writes a draft without version: nothing is left to compare, the description still is. */
    @Test
    void should_keep_a_description_saved_by_a_writer_that_left_no_version() {
        repository.reset();
        repository.givenGuardian(
            aGuardian(definition -> definition.name("First name"))
                .version(3)
                .build()
        );
        var first = deployRevision();
        save("Second name", "Changed description");
        repository.givenAnotherWriterGetsInFirst(stored -> stored.toBuilder().description("Saved meanwhile").version(null).build());

        var result = rollback.execute(new RollbackGuardianUseCase.Input(actor, GUARDIAN_ID, first)).guardian();

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

        var result = rollback.execute(new RollbackGuardianUseCase.Input(actor, GUARDIAN_ID, first)).guardian();

        assertSoftly(softly -> {
            softly.assertThat(result.getName()).as("restored definition").isEqualTo("First name");
            softly.assertThat(result.getDescription()).as("description kept").isEqualTo("Changed description");
            softly.assertThat(result.getLifecycleState()).as("differs from what runs").isEqualTo(GuardianLifecycleState.PENDING);
            softly.assertThat(result.getDeployedRevisionId()).as("revision still running").isEqualTo(second);
            softly.assertThat(publications.publications()).as("nothing published").isEqualTo(2);
            softly
                .assertThat(repository.findByEnvironmentId(ENVIRONMENT_ID, GUARDIAN_ID))
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
        publications.givenPublication(aGuardianDefinition().name("  Padded name  ").build(), Instant.parse("2026-11-08T10:00:00Z"));
        var padded = publications.history.storage().getLast().getId();

        var result = rollback.execute(new RollbackGuardianUseCase.Input(actor, GUARDIAN_ID, padded)).guardian();

        assertThat(result.getName()).isEqualTo("Padded name");
    }

    @Test
    void should_record_a_new_revision_when_the_rolled_back_draft_is_deployed() {
        var first = deployRevision();
        save("Second name", "description");
        var second = deployRevision();
        rollback.execute(new RollbackGuardianUseCase.Input(actor, GUARDIAN_ID, first));

        var redeployed = deploy.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, null)).guardian();

        assertSoftly(softly -> {
            softly.assertThat(redeployed.getDeployedRevisionId()).as("new revision").isNotIn(first, second);
            softly.assertThat(redeployed.getName()).as("rolled back definition deployed").isEqualTo("First name");
        });
    }

    @Test
    void should_be_deployed_again_when_rolling_back_to_the_running_revision() {
        var running = deployRevision();
        save("Second name", "description");

        var result = rollback.execute(new RollbackGuardianUseCase.Input(actor, GUARDIAN_ID, running)).guardian();

        assertThat(result.getLifecycleState()).as("draft matches what runs").isEqualTo(GuardianLifecycleState.DEPLOYED);
    }

    @Test
    void should_audit_the_rollback() {
        var first = deployRevision();
        save("Second name", "description");
        audits.reset();

        rollback.execute(new RollbackGuardianUseCase.Input(actor, GUARDIAN_ID, first));

        assertThat(audits.storage())
            .as("rollback audit")
            .singleElement()
            .satisfies(audit ->
                assertSoftly(softly -> {
                    softly.assertThat(audit.getEvent()).as("event").isEqualTo(GuardianAuditEvent.GUARDIAN_ROLLED_BACK.name());
                    softly
                        .assertThat(audit.getProperties())
                        .as("restored revision")
                        .containsEntry(AuditProperties.GUARDIAN.name(), GUARDIAN_ID)
                        .containsEntry(AuditProperties.GUARDIAN_DEPLOYMENT.name(), first);
                })
            );
    }

    @Test
    void should_refuse_a_rollback_whose_draft_an_update_would_refuse() {
        var first = deployRevision();
        repository.reset();
        repository.givenGuardian(
            aGuardian(definition -> definition.name("First name"))
                .description("x".repeat(CreateGuardian.DESCRIPTION_MAX_LENGTH + 1))
                .build()
        );
        audits.reset();

        assertThatThrownBy(() -> rollback.execute(new RollbackGuardianUseCase.Input(actor, GUARDIAN_ID, first))).isInstanceOf(
            ConstraintViolationException.class
        );
        assertThat(audits.storage()).as("no audit recorded").isEmpty();
    }

    /**
     * The revision goes through the update path, so it is refused as an update would be: what decides where a
     * Guardian runs cannot change under the APIs running it.
     */
    @Test
    void should_refuse_a_revision_of_another_phase_while_an_api_runs_the_guardian() {
        repository.update(aGuardian(definition -> definition.name("First name").phase(Phase.REQUEST)).build());
        var first = deployRevision();
        repository.update(aGuardian(definition -> definition.name("First name").phase(Phase.RESPONSE)).build());
        deployRevision();
        var apis = new InMemoryGuardedApiPort();
        apis.givenApi(
            aGuardedApi().internalFlow(Flow.builder().response(List.of(GuardianSteps.of(GUARDIAN_ID, "First name"))).build()).build()
        );
        var rollbackWhereApisRun = new RollbackGuardianUseCase(
            repository,
            validator,
            publications.store(),
            new GuardianDraftDomainService(
                repository,
                validator,
                audits.service,
                deploymentService,
                new GuardianUsageDomainService(apis),
                aSecretReferenceValidator(permissions)
            )
        );

        assertThatThrownBy(() -> rollbackWhereApisRun.execute(new RollbackGuardianUseCase.Input(actor, GUARDIAN_ID, first)))
            .as("rolling back to a revision of another phase a guardian an API runs")
            .isInstanceOf(GuardianInUseException.class);
        assertThat(repository.findByEnvironmentId(ENVIRONMENT_ID, GUARDIAN_ID))
            .as("draft unchanged")
            .hasValueSatisfying(stored -> assertThat(stored.getDefinition().getPhase()).isEqualTo(Phase.RESPONSE));
    }

    @Test
    void should_refuse_a_name_another_guardian_took_since_and_leave_the_draft_unchanged() {
        var first = deployRevision();
        save("Second name", "description");
        repository.givenGuardian(aGuardian(definition -> definition.id(OTHER_GUARDIAN_ID).name("First name")).build());
        audits.reset();

        assertThatThrownBy(() -> rollback.execute(new RollbackGuardianUseCase.Input(actor, GUARDIAN_ID, first))).isInstanceOf(
            GuardianNameAlreadyExistsException.class
        );
        assertSoftly(softly -> {
            softly
                .assertThat(repository.findByEnvironmentId(ENVIRONMENT_ID, GUARDIAN_ID))
                .as("draft unchanged")
                .hasValueSatisfying(stored -> assertThat(stored.getName()).isEqualTo("Second name"));
            softly.assertThat(audits.storage()).as("no audit recorded").isEmpty();
        });
    }

    @Test
    void should_refuse_a_revision_whose_guardrail_is_no_longer_installed() {
        publications.givenPublication(
            aGuardianDefinition()
                .name("First name")
                .policies(List.of(Step.builder().name("Removed").policy("uninstalled-policy").enabled(true).build()))
                .build(),
            Instant.parse("2026-11-08T10:00:00Z")
        );
        var revision = publications.history.storage().getLast().getId();

        assertThatThrownBy(() -> rollback.execute(new RollbackGuardianUseCase.Input(actor, GUARDIAN_ID, revision))).isInstanceOf(
            ValidationDomainException.class
        );
    }

    /** A revision deployed before resources were judged, or whose resource type was uninstalled since. */
    @Test
    void should_refuse_a_revision_holding_a_resource_the_node_cannot_run_and_leave_the_draft_unchanged() {
        publications.givenPublication(
            aGuardianDefinition()
                .name("First name")
                .resources(List.of(new Resource("classifier", "absent-from-the-node", "{}", true)))
                .build(),
            Instant.parse("2026-11-08T10:00:00Z")
        );
        var revision = publications.history.storage().getLast().getId();
        var draft = repository.storedGuardians();

        assertThatThrownBy(() -> rollback.execute(new RollbackGuardianUseCase.Input(actor, GUARDIAN_ID, revision)))
            .isInstanceOf(ValidationDomainException.class)
            .hasMessage("Resource [classifier] references resource type [absent-from-the-node], which is not installed.");

        assertThat(repository.storedGuardians()).isEqualTo(draft);
    }

    @Test
    void should_refuse_a_revision_whose_step_configuration_the_policy_schema_no_longer_accepts() {
        var revision = deployRevision();
        save("Second name", "description");
        policyValidation.givenRefuses(GUARDRAIL_POLICY, "$.rules: is missing but it is required");

        assertSoftly(softly -> {
            softly
                .assertThatThrownBy(() -> rollback.execute(new RollbackGuardianUseCase.Input(actor, GUARDIAN_ID, revision)))
                .as(
                    "a policy upgraded to a stricter schema closes the way back, on purpose: restoring a draft the gateways " +
                        "could not start would only move the failure to the next deployment"
                )
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContainingAll("Detect credentials", "$.rules");
            softly
                .assertThat(repository.findByEnvironmentId(ENVIRONMENT_ID, GUARDIAN_ID))
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
                .assertThatThrownBy(() -> rollback.execute(new RollbackGuardianUseCase.Input(actor, GUARDIAN_ID, revision)))
                .as(
                    "a revision is restored as it was published: storing the corrected one would store a definition no gateway " +
                        "ever ran, and leave the Guardian PENDING straight after the rollback"
                )
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContainingAll("Detect credentials", "edit and save the Guardian");
            softly
                .assertThat(repository.findByEnvironmentId(ENVIRONMENT_ID, GUARDIAN_ID))
                .as("draft unchanged")
                .hasValueSatisfying(stored -> assertThat(stored.getName()).isEqualTo("Second name"));
        });
    }

    @Test
    void should_refuse_an_unknown_revision_a_withdrawal_or_a_revision_of_another_guardian() {
        repository.givenGuardian(aGuardian(definition -> definition.id(OTHER_GUARDIAN_ID).name("Other")).build());
        deploy.execute(new DeployGuardianUseCase.Input(actor, OTHER_GUARDIAN_ID, null));
        var other = publications.history.storage().getLast().getId();
        deployRevision();
        undeploy.execute(new UndeployGuardianUseCase.Input(actor, GUARDIAN_ID));
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
                    .assertThatThrownBy(() -> rollback.execute(new RollbackGuardianUseCase.Input(actor, GUARDIAN_ID, revision)))
                    .as("revision %s", revision)
                    .isInstanceOf(GuardianDeploymentNotFoundException.class);
            }
        });
    }

    @Test
    void should_refuse_a_guardian_unknown_in_the_environment() {
        var revision = deployRevision();
        var foreign = AuditInfo.builder().organizationId(ORGANIZATION_ID).environmentId("another-environment").actor(actor.actor()).build();

        assertThatThrownBy(() -> rollback.execute(new RollbackGuardianUseCase.Input(foreign, GUARDIAN_ID, revision))).isInstanceOf(
            GuardianNotFoundException.class
        );
    }

    /** A revision that held a store secret reference, put there by a user who could read secrets, is not restored by one who cannot. */
    @Test
    void should_refuse_a_rollback_bringing_back_a_store_secret_reference_without_the_secret_read_permission() {
        when(permissions.hasPermission(any(), any(), any(), any(), any())).thenReturn(true);
        save(A_STORE_SECRET_REFERENCE, "description");
        var withReference = deployRevision();
        save("Plain name", "description");
        when(permissions.hasPermission(any(), any(), any(), any(), any())).thenReturn(false);

        assertThatThrownBy(() -> rollback.execute(new RollbackGuardianUseCase.Input(actor, GUARDIAN_ID, withReference))).isInstanceOf(
            SecretReferenceNotAllowedException.class
        );

        assertThat(repository.findByEnvironmentId(ENVIRONMENT_ID, GUARDIAN_ID)).hasValueSatisfying(stored ->
            assertThat(stored.getName()).isEqualTo("Plain name")
        );
    }

    /** Deploys the current draft, then answers the id of the revision the deployment recorded. */
    private String deployRevision() {
        deploy.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, null));
        return publications.history.storage().getLast().getId();
    }

    private void save(String name, String description) {
        update.execute(
            new UpdateGuardianUseCase.Input(
                actor,
                GUARDIAN_ID,
                UpdateGuardianCommand.builder().definition(aGuardianDefinition().name(name).build()).description(description).build(),
                DraftPrecondition.anyVersion()
            )
        );
    }
}
