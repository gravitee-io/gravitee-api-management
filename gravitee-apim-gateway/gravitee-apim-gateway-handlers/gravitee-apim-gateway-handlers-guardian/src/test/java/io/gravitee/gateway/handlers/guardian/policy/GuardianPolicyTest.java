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
package io.gravitee.gateway.handlers.guardian.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.guardian.GuardianPolicyConfiguration;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.guardian.Guardian;
import io.gravitee.gateway.handlers.guardian.ReactableGuardian;
import io.gravitee.gateway.handlers.guardian.decision.GuardianDecisionReporter;
import io.gravitee.gateway.handlers.guardian.decision.GuardianVerdicts;
import io.gravitee.gateway.handlers.guardian.reactor.GuardianReactor;
import io.gravitee.gateway.handlers.guardian.registry.GuardianRegistry;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.ContextAttributes;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionException;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionFailureException;
import io.gravitee.gateway.reactor.ReactableApi;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.observers.TestObserver;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class GuardianPolicyTest {

    private static final String GUARDIAN_ID = "guardian-id";
    private static final String ENVIRONMENT_ID = "env-id";

    @Mock
    private HttpExecutionContext executionContext;

    @Mock
    private GuardianRegistry guardianRegistry;

    @Mock
    private GuardianReactor guardianReactor;

    @Mock
    private ReactableApi<?> callingApi;

    @Mock
    private GuardianDecisionReporter reporter;

    private GuardianPolicyConfiguration configuration;
    private GuardianPolicy cut;

    @BeforeEach
    void setUp() {
        lenient().when(executionContext.getComponent(GuardianRegistry.class)).thenReturn(guardianRegistry);
        lenient().when(executionContext.getComponent(GuardianDecisionReporter.class)).thenReturn(reporter);
        lenient().when(callingApi.getEnvironmentId()).thenReturn(ENVIRONMENT_ID);
        lenient().when(executionContext.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_REACTABLE_API)).thenReturn(callingApi);
        lenient().when(guardianReactor.reactableGuardian()).thenReturn(guardianFor(Guardian.Phase.REQUEST, null));

        configuration = new GuardianPolicyConfiguration();
        configuration.setGuardianId(GUARDIAN_ID);
        cut = new GuardianPolicy("my-guardian-step", configuration);
    }

    @Test
    void should_expose_the_step_name_as_id() {
        assertThat(cut.id()).isEqualTo("my-guardian-step");
    }

    @Test
    void should_execute_guardian_right_before_the_endpoint_request() {
        when(guardianRegistry.get(GUARDIAN_ID, ENVIRONMENT_ID)).thenReturn(guardianReactor);
        when(guardianReactor.execute(eq(executionContext), eq(cut), any())).thenReturn(Completable.complete());

        cut.onRequest(executionContext).test().assertComplete();
        verify(guardianReactor, never()).execute(any(), any(), any());

        endpointRequestAction().apply(executionContext).test().assertComplete();

        verify(guardianReactor).execute(eq(executionContext), eq(cut), any());
        verify(executionContext, never()).addActionOnEntrypointResponse(any(), any());
    }

    @Test
    void should_execute_guardian_right_before_the_entrypoint_response() {
        when(guardianRegistry.get(GUARDIAN_ID, ENVIRONMENT_ID)).thenReturn(guardianReactor);
        when(guardianReactor.reactableGuardian()).thenReturn(guardianFor(Guardian.Phase.RESPONSE, null));
        when(guardianReactor.execute(eq(executionContext), eq(cut), any())).thenReturn(Completable.complete());

        cut.onResponse(executionContext).test().assertComplete();
        verify(guardianReactor, never()).execute(any(), any(), any());

        entrypointResponseAction().apply(executionContext).test().assertComplete();

        verify(guardianReactor).execute(eq(executionContext), eq(cut), any());
        verify(executionContext, never()).addActionOnEndpointRequest(any(), any());
    }

    @Test
    void should_resolve_the_guardian_of_the_api_environment_whatever_the_environment_attribute() {
        lenient().when(executionContext.getAttribute(ContextAttributes.ATTR_ENVIRONMENT)).thenReturn("another-env");
        when(guardianRegistry.get(GUARDIAN_ID, ENVIRONMENT_ID)).thenReturn(guardianReactor);
        when(guardianReactor.execute(eq(executionContext), eq(cut), any())).thenReturn(Completable.complete());

        runOnEndpointRequest();

        verify(guardianReactor).execute(eq(executionContext), eq(cut), any());
        verify(guardianRegistry, never()).get(GUARDIAN_ID, "another-env");
    }

    @Test
    void should_register_nothing_until_the_request_phase_is_subscribed() {
        cut.onRequest(executionContext);

        verify(executionContext, never()).addActionOnEndpointRequest(any(), any());
    }

    @Test
    void should_register_nothing_until_the_response_phase_is_subscribed() {
        cut.onResponse(executionContext);

        verify(executionContext, never()).addActionOnEntrypointResponse(any(), any());
    }

    @Test
    void should_resolve_the_guardian_only_when_the_action_is_subscribed() {
        cut.onRequest(executionContext).test().assertComplete();

        endpointRequestAction().apply(executionContext);

        verify(guardianRegistry, never()).get(any(), any());
    }

    @Test
    void should_interrupt_when_guardian_is_not_deployed_on_request() {
        when(guardianRegistry.get(GUARDIAN_ID, ENVIRONMENT_ID)).thenReturn(null);
        when(executionContext.interruptWith(any(ExecutionFailure.class))).thenReturn(Completable.complete());

        cut.onRequest(executionContext).test().assertComplete();
        endpointRequestAction().apply(executionContext).test().assertComplete();

        verify(guardianReactor, never()).execute(any(), any(), any());
        assertNotDeployedInterruption();
    }

    @Test
    void should_interrupt_when_guardian_is_not_deployed_on_response() {
        when(guardianRegistry.get(GUARDIAN_ID, ENVIRONMENT_ID)).thenReturn(null);
        when(executionContext.interruptWith(any(ExecutionFailure.class))).thenReturn(Completable.complete());

        cut.onResponse(executionContext).test().assertComplete();
        entrypointResponseAction().apply(executionContext).test().assertComplete();

        verify(guardianReactor, never()).execute(any(), any(), any());
        assertNotDeployedInterruption();
    }

    @Nested
    class Decision {

        private final ReactableGuardian guardian = guardianFor(Guardian.Phase.REQUEST, null);

        @BeforeEach
        void setUp() {
            when(guardianRegistry.get(GUARDIAN_ID, ENVIRONMENT_ID)).thenReturn(guardianReactor);
            when(guardianReactor.reactableGuardian()).thenReturn(guardian);
        }

        @Test
        void should_report_the_collector_the_reactor_received_when_the_run_completes() {
            when(guardianReactor.execute(eq(executionContext), eq(cut), any())).thenReturn(Completable.complete());

            runOnEndpointRequest();

            ArgumentCaptor<GuardianVerdicts> verdicts = ArgumentCaptor.forClass(GuardianVerdicts.class);
            ArgumentCaptor<Long> duration = ArgumentCaptor.forClass(Long.class);
            verify(guardianReactor).execute(eq(executionContext), eq(cut), verdicts.capture());
            verify(reporter).decided(
                eq(executionContext),
                eq("my-guardian-step"),
                eq(guardian),
                eq(Guardian.Phase.REQUEST),
                same(verdicts.getValue()),
                duration.capture()
            );
            assertThat(duration.getValue()).isGreaterThanOrEqualTo(0L);
        }

        @Test
        void should_report_the_response_phase() {
            ReactableGuardian responseGuardian = guardianFor(Guardian.Phase.RESPONSE, null);
            when(guardianReactor.reactableGuardian()).thenReturn(responseGuardian);
            when(guardianReactor.execute(eq(executionContext), eq(cut), any())).thenReturn(Completable.complete());

            cut.onResponse(executionContext).test().assertComplete();
            entrypointResponseAction().apply(executionContext).test().assertComplete();

            verify(reporter).decided(any(), any(), eq(responseGuardian), eq(Guardian.Phase.RESPONSE), any(), anyLong());
        }

        @Test
        void should_report_a_denial_and_keep_the_interruption_going() {
            ExecutionFailure failure = new ExecutionFailure(403).key("RULE_MATCHED");
            InterruptionFailureException interruption = new InterruptionFailureException(failure);
            when(guardianReactor.execute(eq(executionContext), eq(cut), any())).thenReturn(Completable.error(interruption));

            cut.onRequest(executionContext).test().assertComplete();
            endpointRequestAction().apply(executionContext).test().assertError(interruption);

            ArgumentCaptor<GuardianVerdicts> verdicts = ArgumentCaptor.forClass(GuardianVerdicts.class);
            verify(guardianReactor).execute(eq(executionContext), eq(cut), verdicts.capture());
            verify(reporter).denied(
                any(),
                any(),
                eq(guardian),
                eq(Guardian.Phase.REQUEST),
                same(verdicts.getValue()),
                same(failure),
                anyLong()
            );
            verify(reporter, never()).decided(any(), any(), any(), any(), any(), anyLong());
        }

        @Test
        void should_report_an_interruption_without_failure_and_keep_it_going() {
            InterruptionException interruption = new InterruptionException();
            when(guardianReactor.execute(eq(executionContext), eq(cut), any())).thenReturn(Completable.error(interruption));

            cut.onRequest(executionContext).test().assertComplete();
            endpointRequestAction().apply(executionContext).test().assertError(interruption);

            ArgumentCaptor<GuardianVerdicts> verdicts = ArgumentCaptor.forClass(GuardianVerdicts.class);
            verify(guardianReactor).execute(eq(executionContext), eq(cut), verdicts.capture());
            verify(reporter).interrupted(
                eq(executionContext),
                eq("my-guardian-step"),
                eq(guardian),
                eq(Guardian.Phase.REQUEST),
                same(verdicts.getValue()),
                anyLong()
            );
            verify(reporter, never()).decided(any(), any(), any(), any(), any(), anyLong());
            verify(reporter, never()).failed(any(), any(), any(), any(), any(), any(), anyLong());
        }

        @Test
        void should_report_a_failure_and_keep_the_error_going() {
            IllegalStateException error = new IllegalStateException("resource down");
            when(guardianReactor.execute(eq(executionContext), eq(cut), any())).thenReturn(Completable.error(error));

            cut.onRequest(executionContext).test().assertComplete();
            endpointRequestAction().apply(executionContext).test().assertError(error);

            ArgumentCaptor<GuardianVerdicts> verdicts = ArgumentCaptor.forClass(GuardianVerdicts.class);
            verify(guardianReactor).execute(eq(executionContext), eq(cut), verdicts.capture());
            verify(reporter).failed(
                any(),
                any(),
                eq(guardian),
                eq(Guardian.Phase.REQUEST),
                same(verdicts.getValue()),
                same(error),
                anyLong()
            );
        }

        @Test
        void should_report_a_cancelled_run() {
            when(guardianReactor.execute(eq(executionContext), eq(cut), any())).thenReturn(Completable.never());

            cut.onRequest(executionContext).test().assertComplete();
            endpointRequestAction().apply(executionContext).test().dispose();

            ArgumentCaptor<GuardianVerdicts> verdicts = ArgumentCaptor.forClass(GuardianVerdicts.class);
            verify(guardianReactor).execute(eq(executionContext), eq(cut), verdicts.capture());
            verify(reporter).cancelled(
                eq(executionContext),
                eq("my-guardian-step"),
                eq(guardian),
                eq(Guardian.Phase.REQUEST),
                same(verdicts.getValue()),
                anyLong()
            );
        }

        @Test
        void should_report_a_run_cancelled_by_a_timeout() {
            TestScheduler scheduler = new TestScheduler();
            when(guardianReactor.execute(eq(executionContext), eq(cut), any())).thenReturn(Completable.never());

            cut.onRequest(executionContext).test().assertComplete();
            TestObserver<Void> run = endpointRequestAction().apply(executionContext).timeout(1, TimeUnit.SECONDS, scheduler).test();
            scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

            run.assertError(TimeoutException.class);
            verify(reporter).cancelled(any(), any(), eq(guardian), eq(Guardian.Phase.REQUEST), any(), anyLong());
        }

        @Test
        void should_report_once_a_run_disposed_after_it_completed() {
            when(guardianReactor.execute(eq(executionContext), eq(cut), any())).thenReturn(Completable.complete());

            cut.onRequest(executionContext).test().assertComplete();
            TestObserver<Void> run = endpointRequestAction().apply(executionContext).test();
            run.assertComplete();
            run.dispose();

            verify(reporter).decided(any(), any(), any(), any(), any(), anyLong());
            verify(reporter, never()).cancelled(any(), any(), any(), any(), any(), anyLong());
        }

        @Test
        void should_report_once_a_run_disposed_after_it_failed() {
            IllegalStateException error = new IllegalStateException("resource down");
            when(guardianReactor.execute(eq(executionContext), eq(cut), any())).thenReturn(Completable.error(error));

            cut.onRequest(executionContext).test().assertComplete();
            TestObserver<Void> run = endpointRequestAction().apply(executionContext).test();
            run.assertError(error);
            run.dispose();

            verify(reporter).failed(any(), any(), any(), any(), any(), any(), anyLong());
            verify(reporter, never()).cancelled(any(), any(), any(), any(), any(), anyLong());
        }
    }

    @Nested
    class Caller {

        @BeforeEach
        void setUp() {
            when(guardianRegistry.get(GUARDIAN_ID, ENVIRONMENT_ID)).thenReturn(null);
            when(executionContext.interruptWith(any(ExecutionFailure.class))).thenReturn(Completable.complete());
        }

        @Test
        void should_name_the_step_as_the_caller_of_a_decision() {
            cut = new GuardianPolicy(GuardianPolicy.POLICY_ID, "my step", configuration);

            runOnEndpointRequest();

            verify(reporter).notReady(executionContext, "my step", GUARDIAN_ID, Guardian.Phase.REQUEST);
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = { "", " " })
        void should_fall_back_on_the_id_for_a_step_without_a_name(String stepName) {
            cut = new GuardianPolicy(GuardianPolicy.POLICY_ID, stepName, configuration);

            runOnEndpointRequest();

            verify(reporter).notReady(executionContext, GuardianPolicy.POLICY_ID, GUARDIAN_ID, Guardian.Phase.REQUEST);
        }
    }

    @Test
    void should_report_not_ready_before_interrupting_when_the_guardian_is_not_deployed() {
        when(guardianRegistry.get(GUARDIAN_ID, ENVIRONMENT_ID)).thenReturn(null);
        when(executionContext.interruptWith(any(ExecutionFailure.class))).thenReturn(Completable.complete());

        runOnEndpointRequest();

        InOrder order = inOrder(reporter, executionContext);
        order.verify(reporter).notReady(executionContext, "my-guardian-step", GUARDIAN_ID, Guardian.Phase.REQUEST);
        order.verify(executionContext).interruptWith(any(ExecutionFailure.class));
    }

    private void assertNotDeployedInterruption() {
        ArgumentCaptor<ExecutionFailure> failure = ArgumentCaptor.forClass(ExecutionFailure.class);
        verify(executionContext).interruptWith(failure.capture());
        assertThat(failure.getValue().statusCode()).isEqualTo(503);
        assertThat(failure.getValue().key()).isEqualTo("GUARDIAN_NOT_DEPLOYED");
        assertThat(failure.getValue().message()).isEqualTo("Service Unavailable");
        assertThat(failure.getValue().cause()).hasMessage("Guardian 'guardian-id' is not deployed");
    }

    @Nested
    class PhaseCompatibility {

        @BeforeEach
        void setUp() {
            when(guardianRegistry.get(GUARDIAN_ID, ENVIRONMENT_ID)).thenReturn(guardianReactor);
            when(executionContext.interruptWith(any(ExecutionFailure.class))).thenReturn(Completable.complete());
        }

        @Test
        void should_interrupt_when_a_request_guardian_runs_on_the_response() {
            cut.onResponse(executionContext).test().assertComplete();
            entrypointResponseAction().apply(executionContext).test().assertComplete();

            verify(guardianReactor, never()).execute(any(), any(), any());
            assertIncompatiblePhaseInterruption("Guardian 'guardian-id' runs on the REQUEST phase, not on the RESPONSE phase");
        }

        @Test
        void should_interrupt_when_a_response_guardian_runs_on_the_request() {
            when(guardianReactor.reactableGuardian()).thenReturn(guardianFor(Guardian.Phase.RESPONSE, null));

            runOnEndpointRequest();

            verify(guardianReactor, never()).execute(any(), any(), any());
            assertIncompatiblePhaseInterruption("Guardian 'guardian-id' runs on the RESPONSE phase, not on the REQUEST phase");
        }

        @Test
        void should_report_the_incompatible_phase_before_interrupting() {
            runOnResponse();

            InOrder order = inOrder(reporter, executionContext);
            order
                .verify(reporter)
                .incompatible(
                    eq(executionContext),
                    eq("my-guardian-step"),
                    any(),
                    eq(Guardian.Phase.RESPONSE),
                    eq("GUARDIAN_INCOMPATIBLE_PHASE")
                );
            order.verify(executionContext).interruptWith(any(ExecutionFailure.class));
        }

        private void runOnResponse() {
            cut.onResponse(executionContext).test().assertComplete();
            entrypointResponseAction().apply(executionContext).test().assertComplete();
        }

        private void assertIncompatiblePhaseInterruption(String detail) {
            ArgumentCaptor<ExecutionFailure> failure = ArgumentCaptor.forClass(ExecutionFailure.class);
            verify(executionContext).interruptWith(failure.capture());
            assertThat(failure.getValue().statusCode()).isEqualTo(500);
            assertThat(failure.getValue().key()).isEqualTo("GUARDIAN_INCOMPATIBLE_PHASE");
            assertThat(failure.getValue().message()).isEqualTo("Internal Server Error");
            assertThat(failure.getValue().cause()).hasMessage(detail);
        }
    }

    @Nested
    class ApiTypeCompatibility {

        @BeforeEach
        void setUp() {
            when(guardianRegistry.get(GUARDIAN_ID, ENVIRONMENT_ID)).thenReturn(guardianReactor);
            lenient().when(guardianReactor.execute(eq(executionContext), eq(cut), any())).thenReturn(Completable.complete());
            lenient().when(executionContext.interruptWith(any(ExecutionFailure.class))).thenReturn(Completable.complete());
        }

        @Test
        void should_execute_guardian_declared_for_the_calling_api_type() {
            when(guardianReactor.reactableGuardian()).thenReturn(
                guardianFor(Guardian.Phase.REQUEST, Set.of(ApiType.LLM_PROXY, ApiType.MCP_PROXY))
            );
            when(executionContext.getComponent(Api.class)).thenReturn(callingApi(ApiType.LLM_PROXY));

            runOnEndpointRequest();

            verify(guardianReactor).execute(eq(executionContext), eq(cut), any());
            verify(executionContext, never()).interruptWith(any(ExecutionFailure.class));
        }

        @Test
        void should_execute_guardian_without_declared_api_types_whatever_the_calling_api() {
            when(executionContext.getComponent(Api.class)).thenReturn(callingApi(ApiType.PROXY));

            runOnEndpointRequest();

            verify(guardianReactor).execute(eq(executionContext), eq(cut), any());
        }

        @Test
        void should_interrupt_when_the_calling_api_type_is_not_declared() {
            when(guardianReactor.reactableGuardian()).thenReturn(guardianFor(Guardian.Phase.REQUEST, Set.of(ApiType.LLM_PROXY)));
            when(executionContext.getComponent(Api.class)).thenReturn(callingApi(ApiType.MCP_PROXY));

            runOnEndpointRequest();

            verify(guardianReactor, never()).execute(any(), any(), any());
            ArgumentCaptor<ExecutionFailure> failure = ArgumentCaptor.forClass(ExecutionFailure.class);
            verify(executionContext).interruptWith(failure.capture());
            assertThat(failure.getValue().statusCode()).isEqualTo(500);
            assertThat(failure.getValue().key()).isEqualTo("GUARDIAN_INCOMPATIBLE_API_TYPE");
            assertThat(failure.getValue().message()).isEqualTo("Internal Server Error");
            assertThat(failure.getValue().cause()).hasMessage("Guardian 'guardian-id' supports API types [llm-proxy], not mcp-proxy");
        }

        @Test
        void should_report_the_incompatible_api_type_before_interrupting() {
            when(guardianReactor.reactableGuardian()).thenReturn(guardianFor(Guardian.Phase.REQUEST, Set.of(ApiType.LLM_PROXY)));
            when(executionContext.getComponent(Api.class)).thenReturn(callingApi(ApiType.MCP_PROXY));

            runOnEndpointRequest();

            InOrder order = inOrder(reporter, executionContext);
            order
                .verify(reporter)
                .incompatible(
                    eq(executionContext),
                    eq("my-guardian-step"),
                    any(),
                    eq(Guardian.Phase.REQUEST),
                    eq("GUARDIAN_INCOMPATIBLE_API_TYPE")
                );
            order.verify(executionContext).interruptWith(any(ExecutionFailure.class));
        }

        @Test
        void should_interrupt_when_the_calling_api_type_is_unknown_and_the_guardian_is_restricted() {
            when(guardianReactor.reactableGuardian()).thenReturn(guardianFor(Guardian.Phase.RESPONSE, Set.of(ApiType.A2A_PROXY)));
            when(executionContext.getComponent(Api.class)).thenReturn(null);

            cut.onResponse(executionContext).test().assertComplete();
            entrypointResponseAction().apply(executionContext).test().assertComplete();

            verify(guardianReactor, never()).execute(any(), any(), any());
            ArgumentCaptor<ExecutionFailure> failure = ArgumentCaptor.forClass(ExecutionFailure.class);
            verify(executionContext).interruptWith(failure.capture());
            assertThat(failure.getValue().message()).isEqualTo("Internal Server Error");
            assertThat(failure.getValue().cause()).hasMessage("Guardian 'guardian-id' supports API types [a2a-proxy], not this API");
        }
    }

    private void runOnEndpointRequest() {
        cut.onRequest(executionContext).test().assertComplete();
        endpointRequestAction().apply(executionContext).test().assertComplete();
    }

    @SuppressWarnings("unchecked")
    private Function<HttpExecutionContext, Completable> endpointRequestAction() {
        ArgumentCaptor<Function<HttpExecutionContext, Completable>> action = ArgumentCaptor.forClass(Function.class);
        verify(executionContext).addActionOnEndpointRequest(eq(cut), action.capture());
        return action.getValue();
    }

    @SuppressWarnings("unchecked")
    private Function<HttpExecutionContext, Completable> entrypointResponseAction() {
        ArgumentCaptor<Function<HttpExecutionContext, Completable>> action = ArgumentCaptor.forClass(Function.class);
        verify(executionContext).addActionOnEntrypointResponse(eq(cut), action.capture());
        return action.getValue();
    }

    private static ReactableGuardian guardianFor(Guardian.Phase phase, Set<ApiType> apiTypes) {
        return new ReactableGuardian(
            Guardian.builder().id(GUARDIAN_ID).environmentId(ENVIRONMENT_ID).phase(phase).apiTypes(apiTypes).build()
        );
    }

    private static Api callingApi(ApiType apiType) {
        Api api = new Api();
        api.setType(apiType);
        return api;
    }
}
