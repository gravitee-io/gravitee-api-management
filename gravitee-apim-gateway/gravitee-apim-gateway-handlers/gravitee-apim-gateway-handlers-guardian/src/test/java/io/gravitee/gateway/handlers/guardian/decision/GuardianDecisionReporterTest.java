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
package io.gravitee.gateway.handlers.guardian.decision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.guardian.Guardian;
import io.gravitee.gateway.handlers.guardian.ReactableGuardian;
import io.gravitee.gateway.handlers.guardian.policy.GuardianPolicy;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainRequest;
import io.gravitee.gateway.reactive.api.policy.guardrail.GuardrailVerdict;
import io.gravitee.gateway.reactive.api.policy.guardrail.GuardrailVerdict.Finding;
import io.gravitee.gateway.reactive.api.policy.guardrail.GuardrailVerdict.Transformation;
import io.gravitee.gateway.reactive.api.tracing.Tracer;
import io.gravitee.gateway.reactor.ReactableApi;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.node.api.Node;
import io.gravitee.reporter.api.v4.report.DecisionReport;
import io.gravitee.reporter.api.v4.report.DecisionReport.Enforced;
import io.gravitee.reporter.api.v4.report.DecisionReport.IndeterminateCause;
import io.gravitee.reporter.api.v4.report.DecisionReport.MatchedRule;
import io.gravitee.reporter.api.v4.report.DecisionReport.Outcome;
import io.gravitee.reporter.api.v4.report.DecisionReport.Phase;
import io.gravitee.reporter.api.v4.report.DecisionReport.Status;
import io.gravitee.reporter.api.v4.report.DecisionReport.TransformationType;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GuardianDecisionReporterTest {

    private static final String STEP = "guard the prompt";
    private static final String GUARDIAN_ID = "guardian-id";
    private static final String REVISION = "1730000000000";
    private static final Finding FINDING = new Finding("aws-key", "messages[1].user");

    @Mock
    private HttpPlainExecutionContext ctx;

    @Mock
    private HttpPlainRequest request;

    @Mock
    private ReporterService reporterService;

    @Mock
    private Node node;

    @Mock
    private ReactableApi<?> api;

    private final GuardianVerdicts verdicts = new GuardianVerdicts();
    private GuardianDecisionReporter cut;

    @BeforeEach
    void setUp() {
        lenient().when(ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_REACTABLE_API)).thenReturn(api);
        lenient().when(ctx.request()).thenReturn(request);
        lenient().when(ctx.getComponent(Api.class)).thenReturn(callingApi(ApiType.LLM_PROXY));
        lenient().when(request.id()).thenReturn("request-id");
        lenient().when(node.id()).thenReturn("gateway-id");
        lenient().when(api.getOrganizationId()).thenReturn("organization-id");
        lenient().when(api.getEnvironmentId()).thenReturn("environment-id");
        lenient().when(api.getId()).thenReturn("api-id");
        cut = new GuardianDecisionReporter(reporterService, node);
    }

    @Nested
    class Decided {

        @Test
        void should_report_an_allow_when_every_guardrail_let_the_call_through() {
            verdicts.record("first", GuardrailVerdict.allow("CONTINUE", List.of("first did not match"), List.of()));
            verdicts.record("second", GuardrailVerdict.allow("CONTINUE", List.of("second matched"), List.of(FINDING)));

            cut.decided(ctx, STEP, guardian(), Guardian.Phase.REQUEST, verdicts, 42L);

            DecisionReport decision = reported();
            assertThat(decision.getOutcome()).isEqualTo(Outcome.ALLOW);
            assertThat(decision.getEnforced()).isEqualTo(Enforced.ALLOW);
            assertThat(decision.getStatus()).isEqualTo(Status.SUCCESS);
            assertThat(decision.getVerdict()).isNull();
            assertThat(decision.getTransformed()).isNull();
            assertThat(decision.getDurationNanos()).isEqualTo(42L);
            assertThat(decision.getReasons()).containsExactly("first: CONTINUE — first did not match", "second: CONTINUE — second matched");
            assertThat(decision.getMatchedRules()).containsExactly(
                new MatchedRule("aws-key", "aws-key", null, "ALLOW", Map.of("guardrail", "second", "location", "messages[1].user"))
            );
        }

        @Test
        void should_report_a_transform_with_the_most_destructive_transformation() {
            verdicts.record("masking", GuardrailVerdict.transform(Transformation.MASK, "MASK", List.of("masked"), List.of(FINDING)));
            verdicts.record("redacting", GuardrailVerdict.transform(Transformation.REDACT, "REDACT", List.of("redacted"), List.of()));

            cut.decided(ctx, STEP, guardian(), Guardian.Phase.REQUEST, verdicts, 42L);

            DecisionReport decision = reported();
            assertThat(decision.getOutcome()).isEqualTo(Outcome.TRANSFORM);
            assertThat(decision.getEnforced()).isEqualTo(Enforced.TRANSFORM);
            assertThat(decision.getStatus()).isEqualTo(Status.SUCCESS);
            assertThat(decision.getTransformed()).isTrue();
            assertThat(decision.getTransformationType()).isEqualTo(TransformationType.REDACT);
            assertThat(decision.getReasons()).containsExactly("masking: MASK — masked", "redacting: REDACT — redacted");
            assertThat(decision.getMatchedRules()).extracting(MatchedRule::effect).containsExactly("TRANSFORM");
        }

        @Test
        void should_report_not_applicable_without_a_verdict() {
            cut.decided(ctx, STEP, guardian(), Guardian.Phase.REQUEST, verdicts, 42L);

            DecisionReport decision = reported();
            assertThat(decision.getOutcome()).isEqualTo(Outcome.INDETERMINATE);
            assertThat(decision.getIndeterminateCause()).isEqualTo(IndeterminateCause.NOT_APPLICABLE);
            assertThat(decision.getEnforced()).isEqualTo(Enforced.ALLOW);
            assertThat(decision.getStatus()).isEqualTo(Status.SUCCESS);
        }
    }

    @Nested
    class Denied {

        private final ExecutionFailure failure = new ExecutionFailure(403).key("RULE_MATCHED").message("Blocked");

        @Test
        void should_report_a_deny_when_a_guardrail_blocked() {
            verdicts.record("before", GuardrailVerdict.allow("CONTINUE", List.of("did not match"), List.of()));
            verdicts.record("blocking", GuardrailVerdict.deny("BLOCK", List.of("matched"), List.of(FINDING)));

            cut.denied(ctx, STEP, guardian(), Guardian.Phase.REQUEST, verdicts, failure, 42L);

            DecisionReport decision = reported();
            assertThat(decision.getOutcome()).isEqualTo(Outcome.DENY);
            assertThat(decision.getEnforced()).isEqualTo(Enforced.DENY);
            assertThat(decision.getStatus()).isEqualTo(Status.SUCCESS);
            assertThat(decision.getVerdict()).isEqualTo("RULE_MATCHED");
            assertThat(decision.getReasons()).containsExactly("Blocked", "before: CONTINUE — did not match", "blocking: BLOCK — matched");
            assertThat(decision.getMatchedRules()).containsExactly(
                new MatchedRule("aws-key", "aws-key", null, "DENY", Map.of("guardrail", "blocking", "location", "messages[1].user"))
            );
        }

        @Test
        void should_keep_the_failure_message_out_when_no_guardrail_denied() {
            verdicts.record("before", GuardrailVerdict.allow("CONTINUE", List.of("did not match"), List.of()));

            cut.denied(ctx, STEP, guardian(), Guardian.Phase.REQUEST, verdicts, failure, 42L);

            DecisionReport decision = reported();
            assertThat(decision.getOutcome()).isEqualTo(Outcome.DENY);
            assertThat(decision.getVerdict()).isEqualTo("RULE_MATCHED");
            assertThat(decision.getReasons()).containsExactly("before: CONTINUE — did not match");
        }

        @Test
        void should_report_not_ready_when_the_guardian_was_undeployed_during_the_run() {
            cut.denied(
                ctx,
                STEP,
                guardian(),
                Guardian.Phase.REQUEST,
                verdicts,
                new ExecutionFailure(503).key(GuardianPolicy.NOT_DEPLOYED_KEY),
                42L
            );

            assertUndecided(reported(), IndeterminateCause.NOT_READY, "GUARDIAN_NOT_DEPLOYED");
        }

        @Test
        void should_report_an_error_when_a_guardrail_has_no_typed_view() {
            ExecutionFailure unsupported = new ExecutionFailure(500).key(GuardianPolicy.GUARDRAIL_UNSUPPORTED_API_TYPE_KEY);

            cut.denied(ctx, STEP, guardian(), Guardian.Phase.REQUEST, verdicts, unsupported, 42L);

            assertUndecided(reported(), IndeterminateCause.ERROR, "GUARDIAN_GUARDRAIL_UNSUPPORTED_API_TYPE");
        }
    }

    @Test
    void should_report_an_error_with_the_exception_name_when_the_chain_failed() {
        verdicts.record("before", GuardrailVerdict.allow("CONTINUE", List.of("did not match"), List.of()));

        cut.failed(ctx, STEP, guardian(), Guardian.Phase.REQUEST, verdicts, new IllegalStateException("down"), 42L);

        DecisionReport decision = reported();
        assertUndecided(decision, IndeterminateCause.ERROR, "IllegalStateException");
        assertThat(decision.getReasons()).containsExactly("before: CONTINUE — did not match");
    }

    @Test
    void should_report_not_ready_when_the_guardian_is_not_deployed() {
        cut.notReady(ctx, STEP, GUARDIAN_ID, Guardian.Phase.REQUEST);

        DecisionReport decision = reported();
        assertUndecided(decision, IndeterminateCause.NOT_READY, "GUARDIAN_NOT_DEPLOYED");
        assertThat(decision.getDecisionPointId()).isEqualTo(GUARDIAN_ID);
        assertThat(decision.getDecisionPointVersion()).isNull();
    }

    @Test
    void should_report_an_error_when_the_api_type_is_incompatible() {
        cut.incompatible(ctx, STEP, guardian(), Guardian.Phase.REQUEST, GuardianPolicy.INCOMPATIBLE_API_TYPE_KEY);

        assertUndecided(reported(), IndeterminateCause.ERROR, "GUARDIAN_INCOMPATIBLE_API_TYPE");
    }

    @Test
    void should_report_an_error_when_the_phase_is_incompatible() {
        cut.incompatible(ctx, STEP, guardian(), Guardian.Phase.RESPONSE, GuardianPolicy.INCOMPATIBLE_PHASE_KEY);

        assertUndecided(reported(), IndeterminateCause.ERROR, "GUARDIAN_INCOMPATIBLE_PHASE");
    }

    @Nested
    class CommonFields {

        @Test
        void should_describe_the_run() {
            Tracer tracer = mock(Tracer.class);
            when(tracer.traceId()).thenReturn("trace-id");
            when(ctx.getTracer()).thenReturn(tracer);

            cut.decided(ctx, STEP, guardian(), Guardian.Phase.REQUEST, verdicts, 42L);

            DecisionReport decision = reported();
            assertThat(decision.getTimestamp()).isPositive();
            assertThat(decision.getGatewayId()).isEqualTo("gateway-id");
            assertThat(decision.getOrganizationId()).isEqualTo("organization-id");
            assertThat(decision.getEnvironmentId()).isEqualTo("environment-id");
            assertThat(decision.getApiId()).isEqualTo("api-id");
            assertThat(decision.getEventId()).isNotBlank();
            assertThat(decision.getPhase()).isEqualTo(Phase.RESOLVED);
            assertThat(decision.getDecisionPointType()).isEqualTo(DecisionReport.DECISION_POINT_GUARDIAN);
            assertThat(decision.getDecisionPointId()).isEqualTo(GUARDIAN_ID);
            assertThat(decision.getCaller()).isEqualTo(STEP);
            assertThat(decision.getRequestId()).isEqualTo("request-id");
            assertThat(decision.getTraceId()).isEqualTo("trace-id");
        }

        @Test
        void should_read_the_decision_point_version_from_the_revision() {
            cut.decided(ctx, STEP, guardian(), Guardian.Phase.REQUEST, verdicts, 42L);

            assertThat(reported().getDecisionPointVersion()).isEqualTo(REVISION);
        }

        @Test
        void should_have_no_decision_point_version_without_a_revision() {
            ReactableGuardian guardian = guardian();
            guardian.setRevision(null);

            cut.decided(ctx, STEP, guardian, Guardian.Phase.REQUEST, verdicts, 42L);

            assertThat(reported().getDecisionPointVersion()).isNull();
        }

        @Test
        void should_build_the_checkpoint_from_the_phase_and_the_api_type_label() {
            cut.decided(ctx, STEP, guardian(), Guardian.Phase.RESPONSE, verdicts, 42L);

            assertThat(reported().getCheckpoint()).isEqualTo("response.llm-proxy");
        }

        @Test
        void should_set_the_application_as_subject() {
            when(ctx.getAttribute(ContextAttributes.ATTR_APPLICATION)).thenReturn("application-id");
            when(ctx.getAttribute(ContextAttributes.ATTR_PLAN)).thenReturn("plan-id");

            cut.decided(ctx, STEP, guardian(), Guardian.Phase.REQUEST, verdicts, 42L);

            DecisionReport decision = reported();
            assertThat(decision.getSubjectType()).isEqualTo("application");
            assertThat(decision.getSubjectId()).isEqualTo("application-id");
            assertThat(decision.getApplicationId()).isEqualTo("application-id");
            assertThat(decision.getPlanId()).isEqualTo("plan-id");
        }

        @Test
        void should_have_no_subject_without_application() {
            cut.decided(ctx, STEP, guardian(), Guardian.Phase.REQUEST, verdicts, 42L);

            assertNoSubject(reported());
        }

        @Test
        void should_read_the_anonymous_id_as_no_application_and_no_plan() {
            when(ctx.getAttribute(ContextAttributes.ATTR_APPLICATION)).thenReturn("1");
            when(ctx.getAttribute(ContextAttributes.ATTR_PLAN)).thenReturn("1");

            cut.decided(ctx, STEP, guardian(), Guardian.Phase.REQUEST, verdicts, 42L);

            assertNoSubject(reported());
        }

        @Test
        void should_read_a_blank_id_as_no_application_and_no_plan() {
            when(ctx.getAttribute(ContextAttributes.ATTR_APPLICATION)).thenReturn(" ");
            when(ctx.getAttribute(ContextAttributes.ATTR_PLAN)).thenReturn("");

            cut.decided(ctx, STEP, guardian(), Guardian.Phase.REQUEST, verdicts, 42L);

            assertNoSubject(reported());
        }

        @Test
        void should_report_no_additional_metric() {
            cut.decided(ctx, STEP, guardian(), Guardian.Phase.REQUEST, verdicts, 42L);

            assertThat(reported().getAdditionalMetrics()).isNullOrEmpty();
        }

        private void assertNoSubject(DecisionReport decision) {
            assertThat(decision.getSubjectType()).isNull();
            assertThat(decision.getSubjectId()).isNull();
            assertThat(decision.getApplicationId()).isNull();
            assertThat(decision.getPlanId()).isNull();
        }
    }

    @Test
    void should_not_propagate_a_reporter_failure() {
        Logger logger = mock(Logger.class);
        when(ctx.withLogger(any())).thenReturn(logger);
        doThrow(new IllegalStateException("reporter down")).when(reporterService).report(any());

        assertThatCode(() -> cut.decided(ctx, STEP, guardian(), Guardian.Phase.REQUEST, verdicts, 42L)).doesNotThrowAnyException();
    }

    private static void assertUndecided(DecisionReport decision, IndeterminateCause cause, String errorType) {
        assertThat(decision.getOutcome()).isEqualTo(Outcome.INDETERMINATE);
        assertThat(decision.getIndeterminateCause()).isEqualTo(cause);
        assertThat(decision.getEnforced()).isEqualTo(Enforced.DENY);
        assertThat(decision.getStatus()).isEqualTo(Status.ERROR);
        assertThat(decision.getErrorType()).isEqualTo(errorType);
    }

    private DecisionReport reported() {
        ArgumentCaptor<DecisionReport> decision = ArgumentCaptor.forClass(DecisionReport.class);
        verify(reporterService).report(decision.capture());
        return decision.getValue();
    }

    private static ReactableGuardian guardian() {
        ReactableGuardian guardian = new ReactableGuardian(Guardian.builder().id(GUARDIAN_ID).name("My Guardian").build());
        guardian.setRevision(REVISION);
        return guardian;
    }

    private static Api callingApi(ApiType apiType) {
        Api api = new Api();
        api.setType(apiType);
        return api;
    }
}
