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

import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.ENVIRONMENT_ID;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.GUARDRAIL_ID;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.ORGANIZATION_ID;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aGuardedApi;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aGuardrail;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aGuardrailDefinition;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aGuardrailUsage;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aGuardrailUsageRunning;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.infra.json.jackson.JacksonJsonDiffProcessor;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.definition.model.guardrail.GuardrailSteps;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.gamma.rest.core.guardrail.GuardrailAuditFixture;
import io.gravitee.gamma.rest.core.guardrail.GuardrailPublicationFixture;
import io.gravitee.gamma.rest.core.guardrail.domain_service.GuardrailUsageDomainService;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailInUseException;
import io.gravitee.gamma.rest.core.guardrail.inmemory.InMemoryGuardedApiPort;
import io.gravitee.gamma.rest.core.guardrail.inmemory.InMemoryGuardrailRepository;
import io.gravitee.gamma.rest.core.guardrail.model.Guardrail;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailAuditEvent;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailDeployment;
import io.gravitee.gamma.rest.core.guardrail.model.GuardrailLifecycleState;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UndeployGuardrailUseCaseTest {

    private static final Instant DEPLOYED_AT = Instant.parse("2026-11-09T10:00:00Z");
    private static final Instant WITHDRAWN_AT = DEPLOYED_AT.plusSeconds(60);
    private final InMemoryGuardrailRepository repository = new InMemoryGuardrailRepository();
    private final GuardrailPublicationFixture publications = new GuardrailPublicationFixture();
    private final GuardrailAuditFixture audits = new GuardrailAuditFixture();
    private final AuditInfo actor = AuditInfo.builder()
        .organizationId(ORGANIZATION_ID)
        .environmentId(ENVIRONMENT_ID)
        .actor(AuditActor.builder().userId("original-user").build())
        .build();

    @BeforeEach
    void given_a_deployed_guardrail() {
        TimeProvider.overrideClock(Clock.fixed(WITHDRAWN_AT, ZoneOffset.UTC));
        repository.givenGuardrail(aGuardrail().lifecycleState(GuardrailLifecycleState.DEPLOYED).deployedAt(DEPLOYED_AT).build());
        publications.givenPublication(aGuardrailDefinition().build(), DEPLOYED_AT);
    }

    @AfterEach
    void restore_clock() {
        TimeProvider.overrideClock(Clock.systemDefaultZone());
    }

    private UndeployGuardrailUseCase useCase(InMemoryGuardrailRepository store, GuardrailPublicationFixture events) {
        return new UndeployGuardrailUseCase(store, events.service(store), audits.service, aGuardrailUsage());
    }

    @Test
    void should_refuse_to_undeploy_a_guardrail_an_api_runs() {
        var useCase = new UndeployGuardrailUseCase(
            repository,
            publications.service(repository),
            audits.service,
            aGuardrailUsageRunning(GUARDRAIL_ID)
        );

        assertThatThrownBy(() -> useCase.execute(new UndeployGuardrailUseCase.Input(actor, GUARDRAIL_ID)))
            .as("undeploying a guardrail in use")
            .isInstanceOf(GuardrailInUseException.class)
            .hasMessageContaining("1 API runs it");
        assertThat(publications.find(ENVIRONMENT_ID, GUARDRAIL_ID))
            .as("publication")
            .get()
            .isInstanceOf(GuardrailDeployment.Published.class);
    }

    /**
     * An API turning the Guardrail on between the usage check and the withdrawal: the API side checks the Guardrail after
     * its own write, but it may have checked before this withdrawal landed, so this side has to look again too.
     */
    @Test
    void should_keep_the_guardrail_deployed_when_an_api_turned_it_on_while_it_was_withdrawn() {
        var apis = new InMemoryGuardedApiPort();
        var useCase = new UndeployGuardrailUseCase(
            repository,
            publications.service(repository),
            audits.service,
            new GuardrailUsageDomainService(apis)
        );
        publications.beforeWithdrawal = () ->
            apis.givenApi(
                aGuardedApi()
                    .internalFlow(Flow.builder().request(List.of(GuardrailSteps.of(GUARDRAIL_ID, "Credential leakage"))).build())
                    .build()
            );

        assertThatThrownBy(() -> useCase.execute(new UndeployGuardrailUseCase.Input(actor, GUARDRAIL_ID)))
            .as("undeploying a guardrail an API turned on meanwhile")
            .isInstanceOf(GuardrailInUseException.class);
        assertThat(publications.find(ENVIRONMENT_ID, GUARDRAIL_ID))
            .as("deployment the gateways run once the withdrawal is undone")
            .get()
            .isInstanceOfSatisfying(GuardrailDeployment.Published.class, published ->
                assertThat(published.definition()).as("definition published again").isEqualTo(aGuardrailDefinition().build())
            );
    }

    /**
     * A withdrawal takes the publication down without writing the draft, so it does not count as a write:
     * the version an editor read before it is still the one to state after.
     */
    @Test
    void should_leave_the_version_of_the_draft_alone() {
        repository.reset();
        repository.givenGuardrail(aGuardrail().lifecycleState(GuardrailLifecycleState.DEPLOYED).deployedAt(DEPLOYED_AT).version(3).build());

        var withdrawn = useCase(repository, publications).execute(new UndeployGuardrailUseCase.Input(actor, GUARDRAIL_ID)).guardrail();

        assertSoftly(softly -> {
            softly.assertThat(withdrawn.getVersion()).as("answered").isEqualTo(3);
            softly.assertThat(repository.storedGuardrails()).as("stored").singleElement().extracting(Guardrail::getVersion).isEqualTo(3);
        });
    }

    @Test
    void should_preserve_a_draft_saved_while_the_withdrawal_is_published() {
        var newer = aGuardrail(d -> d.name("Concurrent draft"))
            .description("Concurrent description")
            .build();
        publications.beforeWithdrawal = () -> repository.update(newer);

        var result = useCase(repository, publications).execute(new UndeployGuardrailUseCase.Input(actor, GUARDRAIL_ID)).guardrail();

        assertSoftly(softly -> {
            softly
                .assertThat(repository.findByEnvironmentId(ENVIRONMENT_ID, GUARDRAIL_ID).map(Guardrail::getDefinition))
                .as("concurrent draft remains stored")
                .contains(newer.getDefinition());
            softly.assertThat(result.getDefinition()).as("response reads current draft").isEqualTo(newer.getDefinition());
            softly.assertThat(result.getDescription()).as("response preserves current description").isEqualTo(newer.getDescription());
            softly.assertThat(result.getLifecycleState()).as("withdrawal state").isEqualTo(GuardrailLifecycleState.UNDEPLOYED);
        });
    }

    @Test
    void should_not_replace_the_guardrail_when_undeploying() {
        var readOnly = new InMemoryGuardrailRepository() {
            @Override
            public Guardrail update(Guardrail guardrail) {
                throw new TechnicalDomainException("Guardrail writes unavailable");
            }
        };
        readOnly.givenGuardrail(repository.findByEnvironmentId(ENVIRONMENT_ID, GUARDRAIL_ID).orElseThrow());

        var result = useCase(readOnly, publications).execute(new UndeployGuardrailUseCase.Input(actor, GUARDRAIL_ID)).guardrail();

        assertSoftly(softly -> {
            softly
                .assertThat(result.getLifecycleState())
                .as("withdrawal does not depend on a draft write")
                .isEqualTo(GuardrailLifecycleState.UNDEPLOYED);
            softly.assertThat(result.getDeployedAt()).as("last deployment retained").isEqualTo(DEPLOYED_AT);
            softly.assertThat(result.getUpdatedAt()).as("withdrawal date derived from event").isEqualTo(WITHDRAWN_AT);
            softly.assertThat(audits.storage()).as("withdrawal audited").hasSize(1);
        });
    }

    @Test
    void should_record_the_withdrawal_in_the_audit_trail() {
        var deployed = publications.service(repository).state(repository.findByEnvironmentId(ENVIRONMENT_ID, GUARDRAIL_ID).orElseThrow());

        var result = useCase(repository, publications).execute(new UndeployGuardrailUseCase.Input(actor, GUARDRAIL_ID)).guardrail();

        assertThat(audits.storage())
            .as("one withdrawal audit")
            .singleElement()
            .satisfies(audit ->
                assertSoftly(softly -> {
                    softly.assertThat(audit.getEvent()).as("withdrawal event").isEqualTo(GuardrailAuditEvent.GUARDRAIL_UNDEPLOYED.name());
                    softly.assertThat(audit.getUser()).as("actor").isEqualTo(actor.actor().userId());
                    softly.assertThat(audit.getOrganizationId()).as("organization").isEqualTo(ORGANIZATION_ID);
                    softly.assertThat(audit.getEnvironmentId()).as("environment").isEqualTo(ENVIRONMENT_ID);
                    softly.assertThat(audit.getReferenceId()).as("environment reference").isEqualTo(ENVIRONMENT_ID);
                    softly.assertThat(audit.getProperties()).as("guardrail reference").containsEntry("GUARDRAIL", GUARDRAIL_ID);
                    softly.assertThat(audit.getCreatedAt().toInstant()).as("withdrawal date").isEqualTo(WITHDRAWN_AT);
                    softly
                        .assertThat(audit.getPatch())
                        .as("deployed to withdrawn state patch")
                        .isEqualTo(new JacksonJsonDiffProcessor().diff(deployed, result));
                })
            );
    }

    @Test
    void should_audit_only_the_call_that_withdraws_the_guardrail() {
        useCase(repository, publications).execute(new UndeployGuardrailUseCase.Input(actor, GUARDRAIL_ID));
        publications.failWithdrawal = true;

        useCase(repository, publications).execute(new UndeployGuardrailUseCase.Input(actor, GUARDRAIL_ID));

        assertThat(audits.storage()).as("repeated undeployment is not audited again").hasSize(1);
    }

    @Test
    void should_withdraw_the_guardrail_even_when_its_audit_cannot_be_recorded() {
        audits.failRecording = true;

        var result = useCase(repository, publications).execute(new UndeployGuardrailUseCase.Input(actor, GUARDRAIL_ID)).guardrail();

        assertSoftly(softly -> {
            softly.assertThat(result.getLifecycleState()).as("withdrawn state").isEqualTo(GuardrailLifecycleState.UNDEPLOYED);
            softly.assertThat(publications.withdrawn(ENVIRONMENT_ID, GUARDRAIL_ID)).as("withdrawal published").isTrue();
            softly
                .assertThat(repository.findByEnvironmentId(ENVIRONMENT_ID, GUARDRAIL_ID).orElseThrow().getUpdatedAt())
                .as("withdrawal completed")
                .isEqualTo(WITHDRAWN_AT);
            softly.assertThat(audits.storage()).as("best-effort audit").isEmpty();
        });
    }

    @Test
    void should_recover_the_sorting_timestamp_after_a_metadata_write_failure_without_duplicating_audit() {
        var failing = new InMemoryGuardrailRepository() {
            boolean fail = true;

            @Override
            public void updateUpdatedAt(String environmentId, String guardrailId, Instant date) {
                if (fail) {
                    throw new TechnicalDomainException("Metadata unavailable");
                }
                super.updateUpdatedAt(environmentId, guardrailId, date);
            }
        };
        failing.givenGuardrail(repository.findByEnvironmentId(ENVIRONMENT_ID, GUARDRAIL_ID).orElseThrow());
        assertThatThrownBy(() ->
            useCase(failing, publications).execute(new UndeployGuardrailUseCase.Input(actor, GUARDRAIL_ID))
        ).isInstanceOf(TechnicalDomainException.class);
        failing.fail = false;
        publications.failWithdrawal = true;

        useCase(failing, publications).execute(new UndeployGuardrailUseCase.Input(actor, GUARDRAIL_ID));

        assertSoftly(softly -> {
            softly.assertThat(audits.storage()).as("one audit despite retrying metadata").hasSize(1);
            softly
                .assertThat(failing.findByEnvironmentId(ENVIRONMENT_ID, GUARDRAIL_ID).orElseThrow().getUpdatedAt())
                .as("withdrawal timestamp persisted for sorting")
                .isEqualTo(WITHDRAWN_AT);
        });
    }
}
