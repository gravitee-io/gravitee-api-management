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
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.GUARDRAIL_POLICY;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.ORGANIZATION_ID;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aDeployGuardianUseCase;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aDisabledPolicy;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardedApi;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardian;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardianDefinition;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardianUsage;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardianUsageRunning;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardianValidator;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aGuardianValidatorAgainst;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aPolicy;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.aSecretReferenceValidatorWithoutPermissions;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.flows;
import static io.gravitee.gamma.rest.core.guardian.GuardianFixtures.theInstalledPolicies;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.guardian.GuardianSteps;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.guardian.Guardian.GuardianBuilder;
import io.gravitee.definition.model.v4.guardian.Guardian.Phase;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.gamma.rest.core.gravitee_plugin.inmemory.InMemoryPolicyValidation;
import io.gravitee.gamma.rest.core.gravitee_plugin.model.PolicyCapabilities;
import io.gravitee.gamma.rest.core.guardian.GuardianAuditFixture;
import io.gravitee.gamma.rest.core.guardian.GuardianPublicationFixture;
import io.gravitee.gamma.rest.core.guardian.domain_service.GuardianDeploymentDomainService;
import io.gravitee.gamma.rest.core.guardian.domain_service.GuardianDraftDomainService;
import io.gravitee.gamma.rest.core.guardian.domain_service.GuardianUsageDomainService;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianInUseException;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianNotFoundException;
import io.gravitee.gamma.rest.core.guardian.exception.GuardianWithoutEnabledGuardrailException;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardedApiPort;
import io.gravitee.gamma.rest.core.guardian.inmemory.InMemoryGuardianRepository;
import io.gravitee.gamma.rest.core.guardian.model.DraftPrecondition;
import io.gravitee.gamma.rest.core.guardian.model.Guardian;
import io.gravitee.gamma.rest.core.guardian.model.GuardianAuditEvent;
import io.gravitee.gamma.rest.core.guardian.model.GuardianLifecycleState;
import io.gravitee.gamma.rest.core.guardian.model.UpdateGuardianCommand;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.assertj.core.api.Condition;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.assertj.core.condition.VerboseCondition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class GuardianDeploymentUseCasesTest {

    private final InMemoryGuardianRepository repository = new InMemoryGuardianRepository();
    private final GuardianPublicationFixture deployments = new GuardianPublicationFixture();
    private final GuardianAuditFixture audits = new GuardianAuditFixture();
    private final GuardianDeploymentDomainService deploymentService = deployments.service(repository);
    private final UndeployGuardianUseCase undeploy = new UndeployGuardianUseCase(
        repository,
        deploymentService,
        audits.service,
        aGuardianUsage()
    );

    /** The schema answers, which a test installs to make a stored draft non-conforming. */
    private final InMemoryPolicyValidation policyValidation = new InMemoryPolicyValidation();

    private final DeployGuardianUseCase deploy = aDeployGuardianUseCase(repository, deploymentService, audits.service, policyValidation);
    private final UpdateGuardianUseCase update = new UpdateGuardianUseCase(
        repository,
        aGuardianValidator(),
        new GuardianDraftDomainService(
            repository,
            aGuardianValidator(),
            audits.service,
            deploymentService,
            aGuardianUsage(),
            aSecretReferenceValidatorWithoutPermissions()
        )
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
    void should_refuse_to_publish_elsewhere_a_guardian_an_api_runs() {
        repository.update(aGuardian(definition -> definition.phase(Phase.REQUEST)).build());
        deploy.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, null));
        var running = deployments.published(ENVIRONMENT_ID, GUARDIAN_ID).definition();
        repository.update(aGuardian(definition -> definition.phase(Phase.RESPONSE)).build());
        var deployWhereAnApiRunsIt = aDeployGuardianUseCase(
            repository,
            deploymentService,
            audits.service,
            aGuardianUsageRunning(GUARDIAN_ID)
        );

        assertThatThrownBy(() -> deployWhereAnApiRunsIt.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, null)))
            .as("publishing in another phase a guardian an API runs")
            .isInstanceOf(GuardianInUseException.class);
        assertThat(deployments.published(ENVIRONMENT_ID, GUARDIAN_ID).definition()).as("revision the gateways run").isEqualTo(running);
    }

    /**
     * An API turning the Guardian on between the usage check and the publication of a draft that moved to another
     * phase: the API side checks the Guardian after its own write, but it may have checked before this publication
     * landed, so this side has to look again too.
     */
    @Test
    void should_keep_the_revision_an_api_turned_on_while_a_draft_moved_elsewhere_was_published() {
        repository.update(aGuardian(definition -> definition.phase(Phase.REQUEST)).build());
        deploy.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, null));
        var running = deployments.published(ENVIRONMENT_ID, GUARDIAN_ID).definition();
        repository.update(aGuardian(definition -> definition.phase(Phase.RESPONSE)).build());
        var apis = new InMemoryGuardedApiPort();
        var deployWhereApisRun = aDeployGuardianUseCase(
            repository,
            deploymentService,
            audits.service,
            new GuardianUsageDomainService(apis)
        );
        deployments.beforePublication = () ->
            apis.givenApi(
                aGuardedApi()
                    .internalFlow(Flow.builder().request(List.of(GuardianSteps.of(GUARDIAN_ID, "Credential leakage"))).build())
                    .build()
            );

        assertThatThrownBy(() -> deployWhereApisRun.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, null)))
            .as("publishing elsewhere a guardian an API turned on meanwhile")
            .isInstanceOf(GuardianInUseException.class);
        assertThat(deployments.published(ENVIRONMENT_ID, GUARDIAN_ID).definition())
            .as("revision the gateways run once the publication is undone")
            .isEqualTo(running);
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
        var failingDeploy = aDeployGuardianUseCase(failing, deployments.service(failing), audits.service);

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
    void should_refuse_to_deploy_a_persisted_draft_the_installed_policies_no_longer_accept() {
        repository.reset();
        repository.givenGuardian(
            aGuardian(definition ->
                definition.policies(
                    List.of(
                        Step.builder()
                            .name("MCP guardrail")
                            .policy("data-logging-masking")
                            .enabled(true)
                            .configuration("{\"scope\":{\"protocol\":\"MCP\"}}")
                            .build()
                    )
                )
            ).build()
        );

        assertSoftly(softly -> {
            softly
                .assertThatThrownBy(() -> deploy.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, "Reviewed")))
                .as("a draft stored before the check, or saved against policies that have changed since")
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContainingAll("MCP guardrail", "MCP", "llm-proxy");
            softly.assertThat(deployments.publications()).as("nothing published").isZero();
            softly.assertThat(audits.storage()).as("no deployment audit").isEmpty();
        });
    }

    /**
     * A draft stored before its resources were judged at all reaches the gateways through here: the deployment
     * is what refuses it, publishing nothing and leaving the draft as it is stored.
     */
    @Test
    void should_refuse_to_deploy_a_stored_draft_holding_a_resource_the_node_cannot_run() {
        repository.reset();
        var stored = aGuardian(definition ->
            definition.resources(List.of(new Resource("classifier", "absent-from-the-node", "{}", true)))
        ).build();
        repository.givenGuardian(stored);

        assertSoftly(softly -> {
            softly
                .assertThatThrownBy(() -> deploy.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, "Reviewed")))
                .isInstanceOf(ValidationDomainException.class)
                .hasMessage("Resource [classifier] references resource type [absent-from-the-node], which is not installed.");
            softly.assertThat(deployments.publications()).as("nothing published").isZero();
            softly.assertThat(repository.storedGuardians()).as("the draft, untouched").containsExactly(stored);
            softly.assertThat(audits.storage()).as("no deployment audit").isEmpty();
        });
    }

    @Test
    void should_refuse_to_deploy_a_draft_whose_step_configuration_breaks_the_policy_schema() {
        policyValidation.givenRefuses(GUARDRAIL_POLICY, "$.rules: is missing but it is required");

        assertSoftly(softly -> {
            softly
                .assertThatThrownBy(() -> deploy.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, "Reviewed")))
                .as("the revision the gateways would refuse to start is refused here instead")
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContainingAll("Detect credentials", "$.rules: is missing but it is required");
            softly.assertThat(deployments.publications()).as("nothing published").isZero();
            softly.assertThat(audits.storage()).as("no deployment audit").isEmpty();
        });
    }

    /**
     * A draft its policy would only accept corrected — stored before the check, or against a policy that
     * has gained a default since — is refused rather than published. A deployment publishes what is
     * stored and cannot rewrite it: sanitising here would publish a revision the draft no longer matches,
     * and leave the Guardian PENDING from its own deployment; publishing the stored one would publish a
     * configuration the policy was never validated with. Saving the Guardian again stores the corrected
     * configuration, and that deploys.
     */
    @Test
    void should_refuse_to_deploy_a_draft_its_policy_would_only_accept_corrected() {
        repository.reset();
        var policy = aPolicy();
        policy.setConfiguration("{\"rules\":[{\"policy\":\"ban-topics\"}]}");
        repository.givenGuardian(aGuardian(definition -> definition.policies(List.of(policy))).build());
        policyValidation.givenAccepts(
            "{\"rules\":[{\"policy\":\"ban-topics\"}]}",
            "{\"rules\":[{\"policy\":\"ban-topics\"}],\"mode\":\"BLOCK\"}"
        );

        assertSoftly(softly -> {
            softly
                .assertThatThrownBy(() -> deploy.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, "Reviewed")))
                .as("refusal naming the step and what to do")
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContainingAll("Detect credentials", "edit and save the Guardian");
            softly.assertThat(deployments.publications()).as("nothing published").isZero();
            softly
                .assertThat(policy.getConfiguration())
                .as("the stored draft is left alone")
                .isEqualTo("{\"rules\":[{\"policy\":\"ban-topics\"}]}");
        });
    }

    /**
     * A deployment publishes the draft without writing it, so it does not count as a write: the version an
     * editor read before it is still the one to state after.
     */
    @Test
    void should_leave_the_version_of_the_draft_alone() {
        repository.reset();
        repository.givenGuardian(aGuardian().version(3).build());

        var deployed = deploy.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, "Reviewed")).guardian();

        assertSoftly(softly -> {
            softly.assertThat(deployed.getVersion()).as("answered").isEqualTo(3);
            softly.assertThat(repository.storedGuardians()).as("stored").singleElement().extracting(Guardian::getVersion).isEqualTo(3);
        });
    }

    @Test
    void should_deploy_a_draft_the_installed_policies_accept_without_rewriting_it() {
        repository.reset();
        var policy = aPolicy();
        policy.setConfiguration("{\"rules\":[{\"policy\":\"ban-topics\"}],\"mode\":\"BLOCK\"}");
        repository.givenGuardian(aGuardian(definition -> definition.policies(List.of(policy))).build());

        var result = deploy.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, "Reviewed"));

        assertSoftly(softly -> {
            softly.assertThat(deployments.publications()).as("persisted draft is published").isEqualTo(1);
            softly.assertThat(result.guardian().getLifecycleState()).as("published draft state").isEqualTo(GuardianLifecycleState.DEPLOYED);
            softly
                .assertThat(policy.getConfiguration())
                .as("the stored draft is published as it is stored")
                .isEqualTo("{\"rules\":[{\"policy\":\"ban-topics\"}],\"mode\":\"BLOCK\"}");
        });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void should_refuse_to_deploy_a_guardian_without_an_enabled_guardrail(UnaryOperator<GuardianBuilder> definition) {
        repository.reset();
        repository.givenGuardian(aGuardian(definition).build());

        assertRefused(() -> deploy.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, "Reviewed")), aGuardrailRefusal());
    }

    static Stream<Arguments> should_refuse_to_deploy_a_guardian_without_an_enabled_guardrail() {
        return Stream.of(
            arguments(sequence("no policies", definition -> definition.policies(null))),
            arguments(sequence("an empty sequence", definition -> definition.policies(List.of()))),
            arguments(sequence("only a disabled guardrail", definition -> definition.policies(List.of(aDisabledPolicy())))),
            arguments(sequence("only a null step", definition -> definition.policies(Collections.singletonList(null))))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void should_refuse_to_deploy_a_guardian_whose_enabled_step_is_no_guardrail(Map<String, PolicyCapabilities> installed) {
        var deployAgainstRegistry = new DeployGuardianUseCase(
            repository,
            deploymentService,
            audits.service,
            aGuardianValidatorAgainst(installed),
            aGuardianUsage()
        );

        assertRefused(
            () -> deployAgainstRegistry.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, null)),
            aRefusalNaming("Detect credentials", GUARDRAIL_POLICY)
        );
    }

    static Stream<Arguments> should_refuse_to_deploy_a_guardian_whose_enabled_step_is_no_guardrail() {
        return Stream.of(
            arguments(registry("a policy that is not a guardrail", capabilities -> capabilities.put(GUARDRAIL_POLICY, notAGuardrail()))),
            arguments(registry("a policy no longer installed", capabilities -> capabilities.remove(GUARDRAIL_POLICY)))
        );
    }

    @Test
    void should_keep_the_deployed_revision_when_the_draft_lost_its_guardrails() {
        deploy.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, "Reviewed"));
        var pending = save(aGuardianDefinition().policies(List.of()).build(), "description");

        assertSoftly(softly -> {
            softly
                .assertThatThrownBy(() -> deploy.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, null)))
                .as("refusal")
                .is(aGuardrailRefusal());
            softly.assertThat(deployments.publications()).as("no revision added").isEqualTo(1);
            softly
                .assertThat(deployments.published(ENVIRONMENT_ID, GUARDIAN_ID).definition())
                .as("the revision the gateways still run")
                .is(enforcingAGuardrail());
            softly
                .assertThat(audits.storage())
                .as("no second deployment audit")
                .filteredOn(audit -> GuardianAuditEvent.GUARDIAN_DEPLOYED.name().equals(audit.getEvent()))
                .hasSize(1);
            var read = new GetGuardianUseCase(repository, deploymentService)
                .execute(new GetGuardianUseCase.Input(ENVIRONMENT_ID, GUARDIAN_ID))
                .guardian();
            softly
                .assertThat(read.getLifecycleState())
                .as("state left as the refused deployment found it")
                .isEqualTo(pending.getLifecycleState());
            softly.assertThat(read.getDeployedRevisionId()).as("revision left in place").isEqualTo(pending.getDeployedRevisionId());
        });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource
    void should_deploy_a_guardian_with_an_enabled_guardrail(UnaryOperator<GuardianBuilder> definition) {
        repository.reset();
        repository.givenGuardian(aGuardian(definition).build());

        var result = deploy.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, null));

        assertSoftly(softly -> {
            softly.assertThat(result.guardian().getLifecycleState()).as("published draft").isEqualTo(GuardianLifecycleState.DEPLOYED);
            softly.assertThat(deployments.publications()).as("published").isEqualTo(1);
            softly
                .assertThat(deployments.published(ENVIRONMENT_ID, GUARDIAN_ID).definition())
                .as("gateway definition")
                .is(enforcingAGuardrail());
        });
    }

    static Stream<Arguments> should_deploy_a_guardian_with_an_enabled_guardrail() {
        return Stream.of(
            arguments(sequence("a single enabled guardrail", definition -> definition.policies(List.of(aPolicy())))),
            arguments(
                sequence("a disabled step beside an enabled guardrail", definition ->
                    definition.policies(List.of(aDisabledPolicy(), aPolicy()))
                )
            )
        );
    }

    /**
     * The guardrail rule reads past a hole in the sequence — a null step is nothing the gateways would
     * run — but the revalidation of the draft does not: a definition carrying one is not a definition a
     * gateway can read, and publishing it is the failure AIAM-1750 is about.
     */
    @Test
    void should_refuse_to_deploy_a_draft_whose_sequence_carries_a_null_step() {
        repository.reset();
        repository.givenGuardian(aGuardian(definition -> definition.policies(Arrays.asList(null, aPolicy()))).build());

        assertSoftly(softly -> {
            softly
                .assertThatThrownBy(() -> deploy.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, "Reviewed")))
                .as("refusal")
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("must not contain null steps");
            softly.assertThat(deployments.publications()).as("nothing published").isZero();
            softly.assertThat(audits.storage()).as("no deployment audit").isEmpty();
        });
    }

    /**
     * A deployment validates the draft again, so the state of the plugins at that moment decides: a guardrail whose
     * plugin no longer declares the Guardian phase is refused here, where the gateways would refuse to start it.
     */
    @Test
    void should_refuse_to_deploy_a_guardian_whose_guardrail_no_longer_declares_the_guardian_phase() {
        var downgraded = aGuardianValidatorAgainst(Map.of(GUARDRAIL_POLICY, new PolicyCapabilities(true, flows())));
        var deployAgainstRegistry = new DeployGuardianUseCase(repository, deploymentService, audits.service, downgraded, aGuardianUsage());

        assertSoftly(softly -> {
            softly
                .assertThatThrownBy(() -> deployAgainstRegistry.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, null)))
                .as("refusal")
                .isInstanceOf(ValidationDomainException.class)
                .hasMessageContaining("does not support Guardians");
            softly.assertThat(deployments.publications()).as("nothing published").isZero();
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
    void should_report_the_revision_the_gateways_run() {
        deploy.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, null));
        var first = deployments.lastPublicationId();
        var deployed = deploy.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, null)).guardian();
        var second = deployments.lastPublicationId();

        var read = new GetGuardianUseCase(repository, deploymentService).execute(new GetGuardianUseCase.Input(ENVIRONMENT_ID, GUARDIAN_ID));

        assertSoftly(softly -> {
            softly.assertThat(second).as("a new revision per deployment").isNotEqualTo(first);
            softly.assertThat(deployed.getDeployedRevisionId()).as("revision answered by the deployment").isEqualTo(second);
            softly.assertThat(read.guardian().getDeployedRevisionId()).as("revision read back").isEqualTo(second);
        });
    }

    @Test
    void should_keep_reporting_the_deployed_revision_of_a_changed_draft() {
        deploy.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, null));

        var result = save(aGuardianDefinition().name("Changed").build(), "description");

        assertSoftly(softly -> {
            softly.assertThat(result.getLifecycleState()).as("changed draft").isEqualTo(GuardianLifecycleState.PENDING);
            softly.assertThat(result.getDeployedRevisionId()).as("revision still running").isEqualTo(deployments.lastPublicationId());
        });
    }

    @Test
    void should_report_no_deployed_revision_once_withdrawn_or_never_deployed() {
        var neverDeployed = save(aGuardianDefinition().name("Changed").build(), "description");
        deploy.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, null));

        var withdrawn = undeploy.execute(new UndeployGuardianUseCase.Input(actor, GUARDIAN_ID)).guardian();

        assertSoftly(softly -> {
            softly.assertThat(neverDeployed.getDeployedRevisionId()).as("never deployed").isNull();
            softly.assertThat(withdrawn.getDeployedRevisionId()).as("withdrawn").isNull();
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
                    UpdateGuardianCommand.builder().definition(definition).description(description).build(),
                    DraftPrecondition.anyVersion()
                )
            )
            .guardian();
    }

    /** A sequence a data row describes, named for the test report. */
    private static Named<UnaryOperator<GuardianBuilder>> sequence(String name, UnaryOperator<GuardianBuilder> definition) {
        return Named.of(name, definition);
    }

    /** The installed policies as a data row changes them, starting from the registry the fixtures install. */
    private static Named<Map<String, PolicyCapabilities>> registry(String name, Consumer<Map<String, PolicyCapabilities>> change) {
        var installed = theInstalledPolicies();
        change.accept(installed);
        return Named.of(name, installed);
    }

    /** The guardrail of the fixtures as the registry would describe a policy that is not one: same flows, not a guardrail. */
    private static PolicyCapabilities notAGuardrail() {
        return new PolicyCapabilities(false, theInstalledPolicies().get(GUARDRAIL_POLICY).flows());
    }

    private void assertRefused(ThrowingCallable deployment, Condition<Throwable> refusal) {
        assertSoftly(softly -> {
            softly.assertThatThrownBy(deployment).as("refusal").is(refusal);
            softly.assertThat(deployments.publications()).as("nothing published").isZero();
            softly.assertThat(audits.storage()).as("nothing audited").isEmpty();
        });
    }

    /** The refusal a sequence with nothing enabled answers: there is no step to point at. */
    private static Condition<Throwable> aGuardrailRefusal() {
        return refusal("a refusal asking for an enabled guardrail", "at least one enabled guardrail");
    }

    /** The refusal an enabled step that is no installed guardrail answers: it says which step to fix. */
    private static Condition<Throwable> aRefusalNaming(String step, String policy) {
        return refusal(
            "a refusal naming step [%s] and policy [%s]".formatted(step, policy),
            "step [%s]".formatted(step),
            "policy [%s]".formatted(policy)
        );
    }

    /**
     * A draft is free to move to another phase while no API runs the Guardian, and publishing it is what would move
     * the step under the APIs the gateways run it for: their definitions name the phase the deployed revision
     * declared, so those APIs would turn into APIs the gateways refuse.
     */
    @Test
    void should_refuse_to_publish_a_draft_that_moved_where_its_apis_do_not_expect_it() {
        var runningIt = new InMemoryGuardedApiPort();
        var deployOnRunningApi = aDeployGuardianUseCase(
            repository,
            deploymentService,
            audits.service,
            new GuardianUsageDomainService(runningIt),
            policyValidation
        );
        deployOnRunningApi.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, null));
        runningIt.givenApi(
            aGuardedApi()
                .name("Support assistant")
                .internalFlow(Flow.builder().request(List.of(GuardianSteps.of(GUARDIAN_ID, "Credential leakage"))).build())
                .build()
        );
        repository.update(aGuardian(d -> d.phase(Phase.RESPONSE)).build());

        assertSoftly(softly -> {
            softly
                .assertThatThrownBy(() -> deployOnRunningApi.execute(new DeployGuardianUseCase.Input(actor, GUARDIAN_ID, null)))
                .as("publishing a draft that left the phase its APIs run it on")
                .isInstanceOf(GuardianInUseException.class)
                .hasMessageContaining("1 API runs it");
            softly
                .assertThat(deployments.published(ENVIRONMENT_ID, GUARDIAN_ID).definition().getPhase())
                .as("phase of the revision the APIs still run")
                .isEqualTo(Phase.REQUEST);
        });
    }

    private static Condition<Throwable> refusal(String description, String... expectedInMessage) {
        return VerboseCondition.verboseCondition(
            error ->
                error instanceof GuardianWithoutEnabledGuardrailException &&
                Stream.of(expectedInMessage).allMatch(error.getMessage()::contains),
            description,
            // soft assertions carry on with a null throwable when nothing was thrown, and that case is
            // exactly the regression this condition guards: it has to read as a message, not as an NPE
            error ->
                error == null
                    ? " but nothing was thrown"
                    : " but was %s: %s".formatted(error.getClass().getSimpleName(), error.getMessage())
        );
    }

    /** A definition that enforces something: one of its enabled steps runs the guardrail of the fixtures. */
    private static Condition<io.gravitee.definition.model.v4.guardian.Guardian> enforcingAGuardrail() {
        return new Condition<>(
            definition ->
                definition.getPolicies() != null &&
                definition
                    .getPolicies()
                    .stream()
                    .filter(Objects::nonNull)
                    .filter(Step::isEnabled)
                    .anyMatch(step -> GUARDRAIL_POLICY.equals(step.getPolicy())),
            "enforcing an enabled guardrail"
        );
    }
}
