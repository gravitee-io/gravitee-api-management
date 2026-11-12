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
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardianDefinition;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardianValidator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.gamma.rest.core.guardian.GuardianAuditFixture;
import io.gravitee.gamma.rest.core.guardian.GuardianPublicationFixture;
import io.gravitee.gamma.rest.core.guardian.domain_service.GuardianDeploymentDomainService;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianNotFoundException;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardianRepository;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.core.guardian.model.GuardianAuditEvent;
import io.gravitee.gamma.rest.core.guardian.model.GuardianLifecycleState;
import io.gravitee.gamma.rest.core.guardian.model.UpdateGuardianCommand;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GuardianDeploymentUseCasesTest {

    private final InMemoryGuardianRepository repository = new InMemoryGuardianRepository();
    private final GuardianPublicationFixture deployments = new GuardianPublicationFixture();
    private final GuardianAuditFixture audits = new GuardianAuditFixture();
    private final GuardianDeploymentDomainService deploymentService = deployments.service(repository);
    private final UndeployGuardianUseCase undeploy = new UndeployGuardianUseCase(repository, deploymentService, audits.service);
    private final DeployGuardianUseCase deploy = new DeployGuardianUseCase(repository, deploymentService, audits.service);
    private final UpdateGuardianUseCase update = new UpdateGuardianUseCase(
        repository,
        aGuardianValidator(),
        audits.service,
        deploymentService
    );
    private final AuditInfo actor = AuditInfo.builder()
        .organizationId(ORGANIZATION_ID)
        .environmentId(ENVIRONMENT_ID)
        .actor(AuditActor.builder().userId("deployer").build())
        .build();

    @BeforeEach
    void given_an_undeployed_guardian() {
        TimeProvider.overrideClock(Clock.fixed(Instant.parse("2026-11-09T10:00:00Z"), ZoneOffset.UTC));
        repository.givenGuardian(aGuardian().build());
    }

    @AfterEach
    void restore_clock() {
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    @Test
    void should_publish_the_definition_and_record_its_revision_and_audit() {
        var result = deploy.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, "Reviewed"));
        var publication = deployments.published(ENVIRONMENT_ID, GUARDIAN_ID);

        assertSoftly(softly -> {
            softly.assertThat(publication.definition()).as("gateway definition").isEqualTo(result.guardian().getDefinition());
            softly.assertThat(result.guardian().getLifecycleState()).as("published draft").isEqualTo(GuardianLifecycleState.DEPLOYED);
            softly.assertThat(result.guardian().getDeployedAt()).as("date of the actual publication").isEqualTo(publication.at());
            softly.assertThat(deployments.lastLabel()).as("optional deployment label").isEqualTo("Reviewed");
            softly
                .assertThat(audits.storage())
                .as("deployment audit")
                .singleElement()
                .extracting(AuditEntity::getEvent)
                .isEqualTo(GuardianAuditEvent.GUARDIAN_DEPLOYED.name());
        });
    }

    @Test
    void should_answer_a_deployment_with_a_draft_saved_while_it_is_published() {
        var savedMeanwhile = aGuardian(definition -> definition.name("Saved meanwhile")).build();
        deployments.beforePublication = () -> repository.update(savedMeanwhile);

        var result = deploy.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, null)).guardian();

        assertSoftly(softly -> {
            softly.assertThat(result.getName()).as("current draft").isEqualTo("Saved meanwhile");
            softly.assertThat(result.getLifecycleState()).as("differs from the publication").isEqualTo(GuardianLifecycleState.PENDING);
        });
    }

    @Test
    void should_audit_a_deployment_even_when_its_update_date_cannot_be_stored() {
        var failing = new InMemoryGuardianRepository() {
            @Override
            public void updateUpdatedAt(String environmentId, String guardianId, Instant updatedAt) {
                throw new TechnicalDomainException("Storage unavailable");
            }
        };
        failing.givenGuardian(aGuardian().build());
        var failingDeploy = new DeployGuardianUseCase(failing, deployments.service(failing), audits.service);

        assertThatThrownBy(() -> failingDeploy.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, null))).isInstanceOf(
            TechnicalDomainException.class
        );

        assertSoftly(softly -> {
            softly.assertThat(deployments.publications()).as("published").isEqualTo(1);
            softly
                .assertThat(audits.storage())
                .as("the published revision is audited")
                .singleElement()
                .extracting(AuditEntity::getEvent)
                .isEqualTo(GuardianAuditEvent.GUARDIAN_DEPLOYED.name());
        });
    }

    @Test
    void should_publish_a_persisted_draft_without_revalidating_its_policies() {
        repository.reset();
        repository.givenGuardian(
            aGuardian(definition ->
                definition.policies(
                    List.of(
                        Step.builder()
                            .name("MCP guardrail")
                            .policy("data-logging-masking")
                            .configuration("{\"scope\":{\"protocol\":\"MCP\"}}")
                            .build()
                    )
                )
            ).build()
        );

        var result = deploy.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, "Reviewed"));

        assertSoftly(softly -> {
            softly.assertThat(deployments.publications()).as("persisted draft is published").isEqualTo(1);
            softly.assertThat(result.guardian().getLifecycleState()).as("published draft state").isEqualTo(GuardianLifecycleState.DEPLOYED);
        });
    }

    @Test
    void should_mark_a_changed_draft_pending_without_publishing_it() {
        deploy.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, null));
        var result = save(aGuardianDefinition().name("Changed").build(), "description");

        assertSoftly(softly -> {
            softly.assertThat(result.getLifecycleState()).as("changed draft").isEqualTo(GuardianLifecycleState.PENDING);
            softly.assertThat(deployments.publications()).as("a save does not deploy").isEqualTo(1);
            softly.assertThat(deployments.published(ENVIRONMENT_ID, GUARDIAN_ID).definition().getName()).isEqualTo("Credential leakage");
        });
    }

    @Test
    void should_restore_in_sync_when_the_saved_definition_matches_the_publication_again() {
        deploy.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, null));
        save(aGuardianDefinition().name("Changed").build(), "description");
        var result = save(aGuardianDefinition().build(), "another description");

        assertThat(result.getLifecycleState()).as("returning to the published definition").isEqualTo(GuardianLifecycleState.DEPLOYED);
    }

    @Test
    void should_keep_an_undeployed_guardian_undeployed() {
        var result = save(aGuardianDefinition().name("Changed").build(), "description");
        assertThat(result.getLifecycleState()).isEqualTo(GuardianLifecycleState.UNDEPLOYED);
        assertThat(deployments.publications()).isZero();
    }

    @Test
    void should_ignore_json_object_key_order_when_comparing_deployments() {
        repository.reset();
        repository.givenGuardian(
            aGuardian(d ->
                d.policies(List.of(Step.builder().name("Detect").policy("data-logging-masking").configuration("{\"a\":1,\"b\":2}").build()))
            ).build()
        );
        deploy.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, null));
        var result = save(
            aGuardianDefinition()
                .policies(List.of(Step.builder().name("Detect").policy("data-logging-masking").configuration("{\"b\":2,\"a\":1}").build()))
                .build(),
            "description"
        );

        assertThat(result.getLifecycleState())
            .as("JSON object field order is not a runtime change")
            .isEqualTo(GuardianLifecycleState.DEPLOYED);
    }

    @Test
    void should_refuse_unknown_or_foreign_guardians_without_publication_or_audit() {
        var foreign = AuditInfo.builder().organizationId(ORGANIZATION_ID).environmentId("another-environment").actor(actor.actor()).build();
        assertThatThrownBy(() -> deploy.execute(new DeployGuardianUseCase.Input(foreign, GUARDIAN_ID, null))).isInstanceOf(
            GuardianNotFoundException.class
        );
        assertThatThrownBy(() -> deploy.execute(new DeployGuardianUseCase.Input(actor, "unknown", null))).isInstanceOf(
            GuardianNotFoundException.class
        );
        assertThat(deployments.publications()).isZero();
        assertThat(audits.storage()).as("no audit recorded").isEmpty();
    }

    @Test
    void should_read_the_publication_even_when_the_guardian_stamp_is_stale() {
        deployments.givenPublication(aGuardianDefinition().build(), Instant.parse("2026-11-09T10:00:00Z"));

        var result = new GetGuardianUseCase(repository, deploymentService).execute(
            new GetGuardianUseCase.Input(ENVIRONMENT_ID, GUARDIAN_ID)
        );

        assertThat(result.guardian().getLifecycleState())
            .as("publication is authoritative after a partial failure")
            .isEqualTo(GuardianLifecycleState.DEPLOYED);
        assertThat(result.guardian().getDeployedAt()).isEqualTo(Instant.parse("2026-11-09T10:00:00Z"));
    }

    @Test
    void should_reconcile_the_publication_state_of_a_list_page() {
        deployments.givenPublication(aGuardianDefinition().name("Older revision").build(), Instant.parse("2026-11-09T10:00:00Z"));

        var result = new SearchGuardianUseCase(repository, deploymentService).execute(
            SearchGuardianUseCase.Input.builder().environmentId(ENVIRONMENT_ID).pageNumber(1).pageSize(10).build()
        );

        assertThat(result.guardians().getContent())
            .singleElement()
            .extracting(Guardian::getLifecycleState)
            .isEqualTo(GuardianLifecycleState.PENDING);
    }

    @Test
    void should_withdraw_a_pending_guardian_preserving_its_draft_and_last_deployment() {
        var deployed = deploy.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, null)).guardian();
        var draft = save(aGuardianDefinition().name("Changed").build(), "Changed description");
        audits.reset();
        TimeProvider.overrideClock(Clock.fixed(Instant.parse("2026-11-10T10:00:00Z"), ZoneOffset.UTC));

        var result = undeploy.execute(new UndeployGuardianUseCase.Input(actor, GUARDIAN_ID)).guardian();
        var recorded = audits.storage().getFirst();

        assertSoftly(softly -> {
            softly.assertThat(result.getLifecycleState()).as("withdrawn draft state").isEqualTo(GuardianLifecycleState.UNDEPLOYED);
            softly.assertThat(result.getDefinition()).as("draft definition preserved").isEqualTo(draft.getDefinition());
            softly.assertThat(result.getDescription()).as("draft description preserved").isEqualTo(draft.getDescription());
            softly.assertThat(result.getDeployedAt()).as("last deployment retained").isEqualTo(deployed.getDeployedAt());
            softly.assertThat(result.getUpdatedAt()).as("withdrawal date").isEqualTo(TimeProvider.instantNow());
            softly.assertThat(deployments.withdrawn(ENVIRONMENT_ID, GUARDIAN_ID)).as("withdrawal published").isTrue();
            softly.assertThat(audits.storage()).as("withdrawal audit").hasSize(1);
            softly.assertThat(recorded.getUser()).as("audit actor").isEqualTo(actor.actor().userId());
            softly.assertThat(recorded.getEnvironmentId()).as("audit environment").isEqualTo(ENVIRONMENT_ID);
            softly.assertThat(recorded.getOrganizationId()).as("audit organization").isEqualTo(ORGANIZATION_ID);
            softly
                .assertThat(recorded.getPatch())
                .as("previous and withdrawn state patch")
                .isEqualTo(new JacksonJsonDiffProcessor().diff(draft, result));
            softly.assertThat(recorded.getCreatedAt()).as("audit withdrawal date").isEqualTo(TimeProvider.now());
        });
    }

    @Test
    void should_read_the_last_deployment_of_a_guardian_withdrawn_without_edit() {
        var deployed = deploy.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, null)).guardian();
        TimeProvider.overrideClock(Clock.fixed(Instant.parse("2026-11-10T10:00:00Z"), ZoneOffset.UTC));
        undeploy.execute(new UndeployGuardianUseCase.Input(actor, GUARDIAN_ID));

        var result = new GetGuardianUseCase(repository, deploymentService).execute(
            new GetGuardianUseCase.Input(ENVIRONMENT_ID, GUARDIAN_ID)
        );

        assertSoftly(softly -> {
            softly.assertThat(result.guardian().getLifecycleState()).as("withdrawn").isEqualTo(GuardianLifecycleState.UNDEPLOYED);
            softly.assertThat(result.guardian().getDeployedAt()).as("last deployment").isEqualTo(deployed.getDeployedAt());
        });
    }

    @Test
    void should_not_derive_the_last_deployment_from_the_stored_guardian() {
        repository.reset();
        repository.givenGuardian(aGuardian().deployedAt(Instant.parse("2026-01-01T00:00:00Z")).build());
        deployments.givenWithdrawal(aGuardianDefinition().build(), null, Instant.parse("2026-11-10T10:00:00Z"));

        var result = new GetGuardianUseCase(repository, deploymentService).execute(
            new GetGuardianUseCase.Input(ENVIRONMENT_ID, GUARDIAN_ID)
        );

        assertThat(result.guardian().getDeployedAt()).as("unknown when the withdrawal does not carry it").isNull();
    }

    @Test
    void should_not_repeat_a_withdrawal_or_its_audit() {
        deploy.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, null));
        var first = undeploy.execute(new UndeployGuardianUseCase.Input(actor, GUARDIAN_ID)).guardian();
        var publication = deployments.find(ENVIRONMENT_ID, GUARDIAN_ID);
        audits.reset();
        deployments.failWithdrawal = true;

        var second = undeploy.execute(new UndeployGuardianUseCase.Input(actor, GUARDIAN_ID)).guardian();

        assertSoftly(softly -> {
            softly.assertThat(second).as("idempotent response").isEqualTo(first);
            softly.assertThat(deployments.find(ENVIRONMENT_ID, GUARDIAN_ID)).as("unchanged withdrawal").isEqualTo(publication);
        });
        assertThat(audits.storage()).as("no audit recorded").isEmpty();
    }

    @Test
    void should_not_withdraw_a_never_deployed_guardian() {
        var before = deploymentService.state(repository.findByEnvironmentId(ENVIRONMENT_ID, GUARDIAN_ID).orElseThrow());
        deployments.failWithdrawal = true;

        var result = undeploy.execute(new UndeployGuardianUseCase.Input(actor, GUARDIAN_ID)).guardian();

        assertThat(result).as("unchanged unpublished draft").isEqualTo(before);
        assertThat(deployments.find(ENVIRONMENT_ID, GUARDIAN_ID)).as("no event written").isEmpty();
        assertThat(audits.storage()).as("no audit recorded").isEmpty();
    }

    @Test
    void should_preserve_the_guardian_when_withdrawal_fails() {
        deploy.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, null));
        var before = repository.findByEnvironmentId(ENVIRONMENT_ID, GUARDIAN_ID).orElseThrow();
        audits.reset();
        deployments.failWithdrawal = true;

        assertThatThrownBy(() -> undeploy.execute(new UndeployGuardianUseCase.Input(actor, GUARDIAN_ID))).isInstanceOf(
            TechnicalDomainException.class
        );

        assertThat(repository.findByEnvironmentId(ENVIRONMENT_ID, GUARDIAN_ID)).as("unchanged guardian").contains(before);
        assertThat(deploymentService.state(before).getLifecycleState()).as("still published").isEqualTo(GuardianLifecycleState.DEPLOYED);
        assertThat(audits.storage()).as("no audit recorded").isEmpty();
    }

    @Test
    void should_redeploy_the_current_draft_after_undeployment() {
        deploy.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, null));
        undeploy.execute(new UndeployGuardianUseCase.Input(actor, GUARDIAN_ID));
        var draft = save(aGuardianDefinition().name("Changed").build(), "Changed description");

        var result = deploy.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, null)).guardian();

        assertSoftly(softly -> {
            softly.assertThat(result.getLifecycleState()).as("republished state").isEqualTo(GuardianLifecycleState.DEPLOYED);
            softly
                .assertThat(deployments.published(ENVIRONMENT_ID, GUARDIAN_ID).definition())
                .as("current draft published")
                .isEqualTo(draft.getDefinition());
        });
    }

    @Test
    void should_refuse_undeployment_of_unknown_or_foreign_guardians() {
        var foreign = AuditInfo.builder().organizationId(ORGANIZATION_ID).environmentId("another-environment").actor(actor.actor()).build();
        assertThatThrownBy(() -> undeploy.execute(new UndeployGuardianUseCase.Input(foreign, GUARDIAN_ID))).isInstanceOf(
            GuardianNotFoundException.class
        );
        assertThatThrownBy(() -> undeploy.execute(new UndeployGuardianUseCase.Input(actor, "unknown"))).isInstanceOf(
            GuardianNotFoundException.class
        );
        assertThat(deployments.find(ENVIRONMENT_ID, GUARDIAN_ID)).as("no withdrawal written").isEmpty();
        assertThat(audits.storage()).as("no audit recorded").isEmpty();
    }

    private Guardian save(io.gravitee.definition.model.v4.guardian.Guardian definition, String description) {
        return update
            .execute(
                new UpdateGuardianUseCase.Input(
                    actor,
                    GUARDIAN_ID,
                    UpdateGuardianCommand.builder().definition(definition).description(description).build()
                )
            )
            .guardian();
    }
}
