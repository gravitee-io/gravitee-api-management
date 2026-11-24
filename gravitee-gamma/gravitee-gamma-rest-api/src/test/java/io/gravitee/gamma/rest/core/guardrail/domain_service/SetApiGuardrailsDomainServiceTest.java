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
package io.gravitee.gamma.rest.core.guardrail.domain_service;

import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.API_ID;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.ENVIRONMENT_ID;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.GUARDRAIL_ID;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.ORGANIZATION_ID;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aGuardedApi;
import static io.gravitee.gamma.rest.core.guardrail.GuardrailFixtures.aGuardrail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import io.gravitee.apim.core.api.exception.ApiNotFoundException;
import io.gravitee.apim.core.audit.model.AuditActor;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.audit.model.event.ApiAuditEvent;
import io.gravitee.definition.model.guardrail.GuardrailSteps;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.guardrail.Guardrail.GuardrailBuilder;
import io.gravitee.definition.model.v4.guardrail.Guardrail.Phase;
import io.gravitee.gamma.rest.core.guardrail.GuardrailAuditFixture;
import io.gravitee.gamma.rest.core.guardrail.GuardrailPublicationFixture;
import io.gravitee.gamma.rest.core.guardrail.exception.ApiCannotRunGuardrailsException;
import io.gravitee.gamma.rest.core.guardrail.exception.ApiGuardrailSequenceRefusedException;
import io.gravitee.gamma.rest.core.guardrail.exception.ApiInternalFlowChangedException;
import io.gravitee.gamma.rest.core.guardrail.exception.DuplicateApiGuardrailException;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailIncompatibleApiTypeException;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailNotDeployedException;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailNotFoundException;
import io.gravitee.gamma.rest.core.guardrail.exception.GuardrailPhaseMismatchException;
import io.gravitee.gamma.rest.core.guardrail.exception.TooManyApiGuardrailsException;
import io.gravitee.gamma.rest.core.guardrail.inmemory.InMemoryGuardedApiPort;
import io.gravitee.gamma.rest.core.guardrail.inmemory.InMemoryGuardrailRepository;
import io.gravitee.gamma.rest.core.guardrail.model.ApiGuardrail;
import io.gravitee.gamma.rest.core.guardrail.model.Guardrail;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

class SetApiGuardrailsDomainServiceTest {

    private static final Instant DEPLOYED_AT = Instant.parse("2026-11-09T10:00:00Z");

    private final InMemoryGuardrailRepository guardrails = new InMemoryGuardrailRepository();
    private final GuardrailPublicationFixture publications = new GuardrailPublicationFixture();
    private final GuardrailAuditFixture audits = new GuardrailAuditFixture();
    private final InMemoryGuardedApiPort apis = new InMemoryGuardedApiPort();
    private final AuditInfo actor = AuditInfo.builder()
        .organizationId(ORGANIZATION_ID)
        .environmentId(ENVIRONMENT_ID)
        .actor(AuditActor.builder().userId("publisher").build())
        .build();
    private final SetApiGuardrailsDomainService service = serviceOn(apis);

    @Test
    void should_run_the_guardrails_of_both_phases_in_the_order_given() {
        givenDeployedGuardrail(d -> d.id("guardrail-0").name("Prompt injection").phase(Phase.REQUEST));
        givenDeployedGuardrail(d -> d.name("Credential leakage").phase(Phase.REQUEST));
        givenDeployedGuardrail(d -> d.id("guardrail-2").name("Toxicity").phase(Phase.RESPONSE));
        apis.givenApi(aGuardedApi().build());

        var enabled = service.set(API_ID, List.of(GUARDRAIL_ID, "guardrail-0"), List.of("guardrail-2"), actor);

        assertSoftly(softly -> {
            softly
                .assertThat(enabled)
                .as("guardrails enabled on the API")
                .containsExactly(
                    new ApiGuardrail(GUARDRAIL_ID, "Credential leakage", Phase.REQUEST, 1, List.of()),
                    new ApiGuardrail("guardrail-0", "Prompt injection", Phase.REQUEST, 2, List.of()),
                    new ApiGuardrail("guardrail-2", "Toxicity", Phase.RESPONSE, 1, List.of())
                );
            var stored = apis.findById(ENVIRONMENT_ID, API_ID).orElseThrow().internalFlow();
            softly
                .assertThat(stored.getRequest())
                .as("stored request steps")
                .containsExactly(
                    GuardrailSteps.of(GUARDRAIL_ID, "Credential leakage"),
                    GuardrailSteps.of("guardrail-0", "Prompt injection")
                );
            softly
                .assertThat(stored.getResponse())
                .as("stored response steps")
                .containsExactly(GuardrailSteps.of("guardrail-2", "Toxicity"));
        });
    }

    @Test
    void should_replace_the_guardrails_the_api_ran() {
        givenDeployedGuardrail(d -> d);
        apis.givenApi(
            aGuardedApi().internalFlow(Flow.builder().request(List.of(GuardrailSteps.of("guardrail-9", "Gone"))).build()).build()
        );

        var enabled = service.set(API_ID, List.of(GUARDRAIL_ID), List.of(), actor);

        assertThat(enabled).as("guardrails enabled on the API").extracting(ApiGuardrail::guardrailId).containsExactly(GUARDRAIL_ID);
    }

    @Test
    void should_run_no_guardrail_at_all_on_two_empty_lists() {
        apis.givenApi(
            aGuardedApi()
                .internalFlow(Flow.builder().request(List.of(GuardrailSteps.of(GUARDRAIL_ID, "Credential leakage"))).build())
                .build()
        );

        var enabled = service.set(API_ID, List.of(), List.of(), actor);

        assertSoftly(softly -> {
            softly.assertThat(enabled).as("guardrails enabled on the API").isEmpty();
            softly
                .assertThat(apis.findById(ENVIRONMENT_ID, API_ID).orElseThrow().internalFlow().getRequest())
                .as("stored request steps")
                .isEmpty();
        });
    }

    @Test
    void should_read_a_missing_list_as_no_guardrail_on_that_phase() {
        apis.givenApi(
            aGuardedApi()
                .internalFlow(Flow.builder().request(List.of(GuardrailSteps.of(GUARDRAIL_ID, "Credential leakage"))).build())
                .build()
        );

        var enabled = service.set(API_ID, null, null, actor);

        assertSoftly(softly -> {
            softly.assertThat(enabled).as("guardrails enabled on the API").isEmpty();
            softly
                .assertThat(apis.findById(ENVIRONMENT_ID, API_ID).orElseThrow().internalFlow().getRequest())
                .as("stored request steps")
                .isEmpty();
        });
    }

    @Test
    void should_run_the_guardrail_in_the_phase_of_its_deployed_revision() {
        givenDeployedGuardrail(d -> d.name("Credential leakage").phase(Phase.RESPONSE));
        guardrails.update(aGuardrail(d -> d.name("Renamed draft").phase(Phase.REQUEST)).build());
        apis.givenApi(aGuardedApi().build());

        var enabled = service.set(API_ID, List.of(), List.of(GUARDRAIL_ID), actor);

        assertSoftly(softly -> {
            softly
                .assertThat(enabled)
                .as("guardrails enabled on the API, named as the revision the gateways run names them")
                .containsExactly(new ApiGuardrail(GUARDRAIL_ID, "Credential leakage", Phase.RESPONSE, 1, List.of()));
            softly
                .assertThat(apis.findById(ENVIRONMENT_ID, API_ID).orElseThrow().internalFlow().getResponse())
                .as("stored response steps, named as the deployed revision is")
                .containsExactly(GuardrailSteps.of(GUARDRAIL_ID, "Credential leakage"));
        });
    }

    @Test
    void should_record_the_change_in_the_audit_trail_of_the_api() {
        givenDeployedGuardrail(d -> d);
        apis.givenApi(aGuardedApi().build());

        service.set(API_ID, List.of(GUARDRAIL_ID), List.of(), actor);

        assertThat(audits.storage())
            .as("audits")
            .singleElement()
            .satisfies(audit ->
                assertSoftly(softly -> {
                    softly.assertThat(audit.getEvent()).as("event").isEqualTo(ApiAuditEvent.API_UPDATED.name());
                    softly.assertThat(audit.getReferenceId()).as("API reference").isEqualTo(API_ID);
                    softly.assertThat(audit.getUser()).as("actor").isEqualTo("publisher");
                })
            );
    }

    @Test
    void should_refuse_a_guardrail_that_is_not_deployed() {
        guardrails.givenGuardrail(aGuardrail().build());
        apis.givenApi(aGuardedApi().build());

        assertThatThrownBy(() -> service.set(API_ID, List.of(GUARDRAIL_ID), List.of(), actor))
            .as("undeployed guardrail")
            .isInstanceOf(GuardrailNotDeployedException.class);
    }

    @Test
    void should_refuse_a_guardrail_of_another_environment() {
        givenDeployedGuardrail(d -> d.environmentId("other-env"));
        apis.givenApi(aGuardedApi().build());

        assertThatThrownBy(() -> service.set(API_ID, List.of(GUARDRAIL_ID), List.of(), actor))
            .as("guardrail of another environment")
            .isInstanceOf(GuardrailNotFoundException.class);
    }

    @Test
    void should_refuse_a_guardrail_that_does_not_support_the_api_type() {
        givenDeployedGuardrail(d -> d.apiTypes(Set.of(ApiType.PROXY)));
        apis.givenApi(aGuardedApi().build());

        assertThatThrownBy(() -> service.set(API_ID, List.of(GUARDRAIL_ID), List.of(), actor))
            .as("guardrail not supporting llm-proxy")
            .isInstanceOf(GuardrailIncompatibleApiTypeException.class);
    }

    @Test
    void should_refuse_a_guardrail_listed_on_a_phase_that_is_not_the_one_it_runs_in() {
        givenDeployedGuardrail(d -> d.phase(Phase.REQUEST));
        apis.givenApi(aGuardedApi().build());

        assertThatThrownBy(() -> service.set(API_ID, List.of(), List.of(GUARDRAIL_ID), actor))
            .as("request guardrail listed on the response phase")
            .isInstanceOf(GuardrailPhaseMismatchException.class);
    }

    /**
     * The length of the list comes from a client payload, and both the validation and the store read it whole: the
     * refusal happens before either does.
     */
    @Test
    void should_refuse_more_guardrails_on_one_phase_than_an_api_may_run() {
        apis.givenApi(aGuardedApi().build());
        var tooMany = IntStream.rangeClosed(1, 21)
            .mapToObj(i -> "guardrail-" + i)
            .toList();

        assertSoftly(softly -> {
            softly
                .assertThatThrownBy(() -> service.set(API_ID, tooMany, List.of(), actor))
                .as("a sequence longer than a phase may run")
                .isInstanceOf(TooManyApiGuardrailsException.class)
                .hasMessageContaining("at most 20 per phase");
            softly.assertThat(apis.findById(ENVIRONMENT_ID, API_ID).orElseThrow().internalFlow()).as("the API, left as it was").isNull();
        });
    }

    @Test
    void should_refuse_a_guardrail_listed_twice() {
        givenDeployedGuardrail(d -> d);
        apis.givenApi(aGuardedApi().build());

        assertThatThrownBy(() -> service.set(API_ID, List.of(GUARDRAIL_ID, GUARDRAIL_ID), List.of(), actor))
            .as("guardrail listed twice")
            .isInstanceOf(DuplicateApiGuardrailException.class);
    }

    @Test
    void should_refuse_an_api_that_cannot_run_guardrails() {
        givenDeployedGuardrail(d -> d);
        apis.givenApi(aGuardedApi().supportsGuardrails(false).build());

        assertThatThrownBy(() -> service.set(API_ID, List.of(GUARDRAIL_ID), List.of(), actor))
            .as("API without internal flow")
            .isInstanceOf(ApiCannotRunGuardrailsException.class);
    }

    @Test
    void should_refuse_an_api_of_another_environment() {
        givenDeployedGuardrail(d -> d);
        apis.givenApi(aGuardedApi().environmentId("other-env").build());

        assertThatThrownBy(() -> service.set(API_ID, List.of(GUARDRAIL_ID), List.of(), actor))
            .as("API of another environment")
            .isInstanceOf(ApiNotFoundException.class);
    }

    @Test
    void should_leave_the_api_untouched_when_one_guardrail_of_the_list_is_refused() {
        givenDeployedGuardrail(d -> d);
        apis.givenApi(
            aGuardedApi()
                .internalFlow(Flow.builder().request(List.of(GuardrailSteps.of(GUARDRAIL_ID, "Credential leakage"))).build())
                .build()
        );

        assertThatThrownBy(() -> service.set(API_ID, List.of(GUARDRAIL_ID, "unknown-guardrail"), List.of(), actor))
            .as("list holding an unknown guardrail")
            .isInstanceOf(GuardrailNotFoundException.class);

        assertSoftly(softly -> {
            softly
                .assertThat(apis.findById(ENVIRONMENT_ID, API_ID).orElseThrow().internalFlow().getRequest())
                .as("stored request steps")
                .containsExactly(GuardrailSteps.of(GUARDRAIL_ID, "Credential leakage"));
            softly.assertThat(audits.storage()).as("audits").isEmpty();
        });
    }

    /**
     * A sequence is validated whole, so a publisher who got three ids wrong learns all three at once instead of
     * fixing one and sending the request again. Every id the environment does not hold is one refusal.
     */
    @Test
    void should_name_every_guardrail_the_environment_does_not_hold_in_one_refusal() {
        apis.givenApi(aGuardedApi().build());

        assertThatThrownBy(() -> service.set(API_ID, List.of("ghost-1", "ghost-2", "ghost-3"), List.of(), actor))
            .as("three ids the environment does not hold")
            .isInstanceOf(GuardrailNotFoundException.class)
            .hasMessageContainingAll("ghost-1", "ghost-2", "ghost-3");
    }

    /**
     * The hard case: the two refusals do not mean the same thing to the caller, so one answer has to carry both
     * without collapsing them. The kinds are told apart by the parameters, not by the sentence.
     */
    @Test
    void should_tell_apart_an_unknown_guardrail_from_one_the_api_cannot_run_in_one_refusal() {
        givenDeployedGuardrail(d -> d.id("wrong-phase").phase(Phase.RESPONSE));
        guardrails.givenGuardrail(aGuardrail(d -> d.id("never-deployed")).build());
        apis.givenApi(aGuardedApi().build());

        assertThatThrownBy(() -> service.set(API_ID, List.of("ghost", "wrong-phase", "never-deployed"), List.of(), actor))
            .as("an unknown id, a Guardrail on the other phase and an undeployed one")
            .isInstanceOf(ApiGuardrailSequenceRefusedException.class)
            .asInstanceOf(InstanceOfAssertFactories.throwable(ApiGuardrailSequenceRefusedException.class))
            .extracting(ApiGuardrailSequenceRefusedException::getParameters, InstanceOfAssertFactories.map(String.class, String.class))
            .as("what each refusal is")
            .containsEntry("notFoundGuardrailIds", "ghost")
            .containsEntry("phaseMismatchGuardrailIds", "wrong-phase")
            .containsEntry("notDeployedGuardrailIds", "never-deployed");
    }

    /** The whole request is one sequence: a fault on each phase is still one answer. */
    @Test
    void should_report_the_faults_of_both_phases_in_one_refusal() {
        apis.givenApi(aGuardedApi().build());

        assertThatThrownBy(() -> service.set(API_ID, List.of("ghost-request"), List.of("ghost-response"), actor))
            .as("one unknown id on each phase")
            .isInstanceOf(GuardrailNotFoundException.class)
            .hasMessageContainingAll("ghost-request", "ghost-response");
    }

    /**
     * A single fault keeps reading exactly as it did: its own type, its own sentence. The aggregate is for the case
     * that used to cost a round trip per fault, and it does not change what one fault answers.
     */
    @Test
    void should_keep_a_single_fault_as_its_own_refusal() {
        givenDeployedGuardrail(d -> d.phase(Phase.REQUEST));
        apis.givenApi(aGuardedApi().build());

        assertThatThrownBy(() -> service.set(API_ID, List.of(), List.of(GUARDRAIL_ID), actor))
            .as("one Guardrail listed on the phase it does not run in")
            .isInstanceOf(GuardrailPhaseMismatchException.class)
            .isNotInstanceOf(ApiGuardrailSequenceRefusedException.class);
    }

    /**
     * The write carries the flow the call read, so an API that moved meanwhile answers a conflict instead of losing
     * the other call's Guardrails.
     */
    @Test
    void should_write_only_while_the_api_still_holds_the_flow_the_call_read() {
        givenDeployedGuardrail(d -> d.phase(Phase.REQUEST));
        var ran = Flow.builder().request(List.of(GuardrailSteps.of("guardrail-0", "Prompt injection"))).build();
        var recorder = new InMemoryGuardedApiPort() {
            private Flow expected;

            @Override
            public io.gravitee.gamma.rest.core.guardrail.model.GuardedApi updateInternalFlow(
                AuditInfo auditInfo,
                String apiId,
                Flow expectedInternalFlow,
                Flow internalFlow
            ) {
                expected = expectedInternalFlow;
                return super.updateInternalFlow(auditInfo, apiId, expectedInternalFlow, internalFlow);
            }
        };
        recorder.givenApi(aGuardedApi().internalFlow(ran).build());

        serviceOn(recorder).set(API_ID, List.of(GUARDRAIL_ID), List.of(), actor);

        assertThat(recorder.expected).as("flow the write is guarded by").isEqualTo(ran);
    }

    @Test
    void should_leave_the_api_untouched_when_it_changed_since_the_call_read_it() {
        givenDeployedGuardrail(d -> d.phase(Phase.REQUEST));
        var movedMeanwhile = new InMemoryGuardedApiPort() {
            @Override
            public io.gravitee.gamma.rest.core.guardrail.model.GuardedApi updateInternalFlow(
                AuditInfo auditInfo,
                String apiId,
                Flow expectedInternalFlow,
                Flow internalFlow
            ) {
                throw new ApiInternalFlowChangedException(apiId);
            }
        };
        movedMeanwhile.givenApi(aGuardedApi().build());

        assertSoftly(softly -> {
            softly
                .assertThatThrownBy(() -> serviceOn(movedMeanwhile).set(API_ID, List.of(GUARDRAIL_ID), List.of(), actor))
                .as("conflict")
                .isInstanceOf(ApiInternalFlowChangedException.class);
            softly.assertThat(audits.storage()).as("nothing audited").isEmpty();
        });
    }

    /**
     * An undeployment landing between the validation and the write: the undeployment checks the APIs after its own
     * write, but it may have checked before this write landed, so this side has to look again too.
     */
    @Test
    void should_turn_off_again_a_guardrail_undeployed_while_it_was_turned_on() {
        givenDeployedGuardrail(d -> d);
        apis.givenApi(aGuardedApi().build());
        apis.beforeNextUpdate = () ->
            publications.givenWithdrawal(aGuardrail().build().getDefinition(), DEPLOYED_AT, DEPLOYED_AT.plusSeconds(60));

        assertThatThrownBy(() -> service.set(API_ID, List.of(GUARDRAIL_ID), List.of(), actor))
            .as("turning on a guardrail undeployed meanwhile")
            .isInstanceOf(GuardrailNotDeployedException.class);
        assertThat(apis.findById(ENVIRONMENT_ID, API_ID).orElseThrow().guardrails().list())
            .as("guardrails the API runs once the change is undone")
            .isEmpty();
    }

    private SetApiGuardrailsDomainService serviceOn(InMemoryGuardedApiPort port) {
        return new SetApiGuardrailsDomainService(
            port,
            new ValidateApiGuardrailsDomainService(guardrails, publications.service(guardrails)),
            new DescribeApiGuardrailsDomainService(publications.service(guardrails), port),
            audits.service
        );
    }

    private void givenDeployedGuardrail(UnaryOperator<GuardrailBuilder> definition) {
        Guardrail guardrail = aGuardrail(definition).build();
        guardrails.givenGuardrail(guardrail);
        publications.givenPublication(guardrail.getDefinition(), DEPLOYED_AT);
    }
}
