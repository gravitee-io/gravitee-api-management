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
package io.gravitee.gateway.handlers.guardrail.policy;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.guardrail.GuardrailRunPolicyConfiguration;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.guardrail.Guardrail;
import io.gravitee.gateway.handlers.guardrail.ReactableGuardrail;
import io.gravitee.gateway.handlers.guardrail.decision.GuardrailDecisionReporter;
import io.gravitee.gateway.handlers.guardrail.decision.GuardrailVerdicts;
import io.gravitee.gateway.handlers.guardrail.reactor.GuardrailReactor;
import io.gravitee.gateway.handlers.guardrail.registry.GuardrailRegistry;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.policy.http.HttpPolicy;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionException;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionFailureException;
import io.gravitee.gateway.reactor.ReactableApi;
import io.reactivex.rxjava3.core.Completable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Internal policy placed in API flows to execute a deployed Guardrail. The chain comes from the Guardrail reactor
 * through the {@link GuardrailRegistry}; when the Guardrail is not deployed, the request is interrupted. Every run is
 * reported as one decision through the {@link GuardrailDecisionReporter}.
 *
 * <p>An interruption gives the client a generic message only; its detail goes through the cause to the diagnostic
 * reported in the metrics, where the API publisher sees it in the Console.</p>
 *
 * @author GraviteeSource Team
 */
public class GuardrailRunPolicy implements HttpPolicy {

    public static final String POLICY_ID = "guardrail-run";
    public static final String NOT_DEPLOYED_KEY = "GUARDRAIL_NOT_DEPLOYED";
    public static final String INCOMPATIBLE_API_TYPE_KEY = "GUARDRAIL_INCOMPATIBLE_API_TYPE";
    public static final String INCOMPATIBLE_PHASE_KEY = "GUARDRAIL_INCOMPATIBLE_PHASE";
    public static final String GUARDRAIL_POLICY_UNSUPPORTED_API_TYPE_KEY = "GUARDRAIL_GUARDRAIL_POLICY_UNSUPPORTED_API_TYPE";

    private final String id;
    private final String stepName;
    private final GuardrailRunPolicyConfiguration configuration;

    public GuardrailRunPolicy() {
        this(POLICY_ID, null);
    }

    public GuardrailRunPolicy(GuardrailRunPolicyConfiguration configuration) {
        this(POLICY_ID, configuration);
    }

    public GuardrailRunPolicy(String id, GuardrailRunPolicyConfiguration configuration) {
        this(id, null, configuration);
    }

    /**
     * @param id the id of the policy, the plugin id for a step of a flow.
     * @param stepName the name of the step in the API flow, {@code null} or blank when it has none.
     * @param configuration the configuration of the step.
     */
    public GuardrailRunPolicy(String id, String stepName, GuardrailRunPolicyConfiguration configuration) {
        this.id = id;
        this.stepName = stepName;
        this.configuration = configuration;
    }

    @Override
    public String id() {
        return id;
    }

    /**
     * Runs the Guardrail right before the endpoint is invoked, so it sees the request as the API flows left it.
     */
    @Override
    public Completable onRequest(HttpPlainExecutionContext ctx) {
        return Completable.fromRunnable(() ->
            ctx.addActionOnEndpointRequest(this, actionCtx -> Completable.defer(() -> execute(actionCtx, Guardrail.Phase.REQUEST)))
        );
    }

    /**
     * Runs the Guardrail right before the entrypoint sends the response, so it sees the response as the API flows left it.
     */
    @Override
    public Completable onResponse(HttpPlainExecutionContext ctx) {
        return Completable.fromRunnable(() ->
            ctx.addActionOnEntrypointResponse(this, actionCtx -> Completable.defer(() -> execute(actionCtx, Guardrail.Phase.RESPONSE)))
        );
    }

    private Completable execute(HttpPlainExecutionContext ctx, Guardrail.Phase phase) {
        GuardrailDecisionReporter reporter = ctx.getComponent(GuardrailDecisionReporter.class);
        GuardrailReactor reactor = ctx.getComponent(GuardrailRegistry.class).get(configuration.getGuardrailId(), callingEnvironmentId(ctx));
        if (reactor == null) {
            // Letting the request through would leave it unguarded until the Guardrail is synchronized: fail closed.
            reporter.notReady(ctx, caller(), configuration.getGuardrailId(), phase);
            return ctx.interruptWith(
                new ExecutionFailure(HttpStatusCode.SERVICE_UNAVAILABLE_503)
                    .key(NOT_DEPLOYED_KEY)
                    .message("Service Unavailable")
                    .cause(new GuardrailFailureException(String.format("Guardrail '%s' is not deployed", configuration.getGuardrailId())))
            );
        }
        ReactableGuardrail reactableGuardrail = reactor.reactableGuardrail();
        Guardrail guardrail = reactableGuardrail.getDefinition();
        if (guardrail.getPhase() != phase) {
            // A Guardrail is built for its own phase: run on the other one, a request Guardrail would only see a request
            // already sent. Fail closed so the misplaced step is visible.
            reporter.incompatible(ctx, caller(), reactableGuardrail, phase, INCOMPATIBLE_PHASE_KEY);
            return ctx.interruptWith(
                new ExecutionFailure(HttpStatusCode.INTERNAL_SERVER_ERROR_500)
                    .key(INCOMPATIBLE_PHASE_KEY)
                    .message("Internal Server Error")
                    .cause(
                        new GuardrailFailureException(
                            String.format(
                                "Guardrail '%s' runs on the %s phase, not on the %s phase",
                                guardrail.getId(),
                                guardrail.getPhase(),
                                phase
                            )
                        )
                    )
            );
        }
        ApiType apiType = callingApiType(ctx);
        if (!guardrail.isCompatibleWith(apiType)) {
            // A declared restriction that the calling API violates never heals by itself: fail closed so the
            // misconfiguration is visible instead of silently unguarded.
            reporter.incompatible(ctx, caller(), reactableGuardrail, phase, INCOMPATIBLE_API_TYPE_KEY);
            return ctx.interruptWith(
                new ExecutionFailure(HttpStatusCode.INTERNAL_SERVER_ERROR_500)
                    .key(INCOMPATIBLE_API_TYPE_KEY)
                    .message("Internal Server Error")
                    .cause(
                        new GuardrailFailureException(
                            String.format(
                                "Guardrail '%s' supports API types %s, not %s",
                                guardrail.getId(),
                                guardrail.getApiTypes().stream().map(ApiType::getLabel).sorted().toList(),
                                apiType == null ? "this API" : apiType.getLabel()
                            )
                        )
                    )
            );
        }
        GuardrailVerdicts verdicts = new GuardrailVerdicts();
        // A dispose can follow a terminal signal: only the first outcome of the run is its decision.
        AtomicBoolean reported = new AtomicBoolean();
        long start = System.nanoTime();
        return reactor
            .execute(ctx, this, verdicts)
            .doOnComplete(() -> once(reported, () -> reporter.decided(ctx, caller(), reactableGuardrail, phase, verdicts, elapsed(start))))
            .doOnError(error ->
                once(reported, () -> {
                    if (error instanceof InterruptionFailureException interruption) {
                        reporter.denied(
                            ctx,
                            caller(),
                            reactableGuardrail,
                            phase,
                            verdicts,
                            interruption.getExecutionFailure(),
                            elapsed(start)
                        );
                    } else if (error instanceof InterruptionException) {
                        reporter.interrupted(ctx, caller(), reactableGuardrail, phase, verdicts, elapsed(start));
                    } else {
                        reporter.failed(ctx, caller(), reactableGuardrail, phase, verdicts, error, elapsed(start));
                    }
                })
            )
            .doOnDispose(() ->
                once(reported, () -> reporter.cancelled(ctx, caller(), reactableGuardrail, phase, verdicts, elapsed(start)))
            );
    }

    private static void once(AtomicBoolean reported, Runnable report) {
        if (reported.compareAndSet(false, true)) {
            report.run();
        }
    }

    /**
     * What a decision names as its caller: two steps of a flow share the id of the plugin, not their names.
     */
    private String caller() {
        return stepName == null || stepName.isBlank() ? id : stepName;
    }

    private static long elapsed(long start) {
        return System.nanoTime() - start;
    }

    /**
     * Reads the environment from the deployed API rather than from the environment attribute, which any API flow can
     * overwrite to run the Guardrail of another environment, with its resources and secrets.
     */
    private static String callingEnvironmentId(HttpPlainExecutionContext ctx) {
        ReactableApi<?> api = ctx.getInternalAttribute(InternalContextAttributes.ATTR_INTERNAL_REACTABLE_API);
        return api == null ? null : api.getEnvironmentId();
    }

    private static ApiType callingApiType(HttpPlainExecutionContext ctx) {
        Api api = ctx.getComponent(Api.class);
        return api == null ? null : api.getType();
    }
}
