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

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.guardian.GuardianPolicyConfiguration;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.guardian.Guardian;
import io.gravitee.gateway.handlers.guardian.reactor.GuardianReactor;
import io.gravitee.gateway.handlers.guardian.registry.GuardianRegistry;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.context.InternalContextAttributes;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.policy.http.HttpPolicy;
import io.gravitee.gateway.reactor.ReactableApi;
import io.reactivex.rxjava3.core.Completable;

/**
 * Internal policy placed in API flows to execute a deployed Guardian. The chain comes from the Guardian reactor
 * through the {@link GuardianRegistry}; when the Guardian is not deployed, the request is interrupted.
 *
 * <p>An interruption gives the client a generic message only; its detail goes through the cause to the diagnostic
 * reported in the metrics, where the API publisher sees it in the Console.</p>
 *
 * @author GraviteeSource Team
 */
public class GuardianPolicy implements HttpPolicy {

    public static final String POLICY_ID = "guardian-policy";
    public static final String NOT_DEPLOYED_KEY = "GUARDIAN_NOT_DEPLOYED";
    public static final String INCOMPATIBLE_API_TYPE_KEY = "GUARDIAN_INCOMPATIBLE_API_TYPE";
    public static final String INCOMPATIBLE_PHASE_KEY = "GUARDIAN_INCOMPATIBLE_PHASE";

    private final String id;
    private final GuardianPolicyConfiguration configuration;

    public GuardianPolicy() {
        this(POLICY_ID, null);
    }

    public GuardianPolicy(GuardianPolicyConfiguration configuration) {
        this(POLICY_ID, configuration);
    }

    public GuardianPolicy(String id, GuardianPolicyConfiguration configuration) {
        this.id = id;
        this.configuration = configuration;
    }

    @Override
    public String id() {
        return id;
    }

    /**
     * Runs the Guardian right before the endpoint is invoked, so it sees the request as the API flows left it.
     */
    @Override
    public Completable onRequest(HttpPlainExecutionContext ctx) {
        return Completable.fromRunnable(() ->
            ctx.addActionOnEndpointRequest(this, actionCtx -> Completable.defer(() -> execute(actionCtx, Guardian.Phase.REQUEST)))
        );
    }

    /**
     * Runs the Guardian right before the entrypoint sends the response, so it sees the response as the API flows left it.
     */
    @Override
    public Completable onResponse(HttpPlainExecutionContext ctx) {
        return Completable.fromRunnable(() ->
            ctx.addActionOnEntrypointResponse(this, actionCtx -> Completable.defer(() -> execute(actionCtx, Guardian.Phase.RESPONSE)))
        );
    }

    private Completable execute(HttpPlainExecutionContext ctx, Guardian.Phase phase) {
        GuardianReactor reactor = ctx.getComponent(GuardianRegistry.class).get(configuration.getGuardianId(), callingEnvironmentId(ctx));
        if (reactor == null) {
            // Letting the request through would leave it unguarded until the Guardian is synchronized: fail closed.
            return ctx.interruptWith(
                new ExecutionFailure(HttpStatusCode.SERVICE_UNAVAILABLE_503)
                    .key(NOT_DEPLOYED_KEY)
                    .message("Service Unavailable")
                    .cause(new GuardianFailureException(String.format("Guardian '%s' is not deployed", configuration.getGuardianId())))
            );
        }
        Guardian guardian = reactor.reactableGuardian().getDefinition();
        if (guardian.getPhase() != phase) {
            // A Guardian is built for its own phase: run on the other one, a request Guardian would only see a request
            // already sent. Fail closed so the misplaced step is visible.
            return ctx.interruptWith(
                new ExecutionFailure(HttpStatusCode.INTERNAL_SERVER_ERROR_500)
                    .key(INCOMPATIBLE_PHASE_KEY)
                    .message("Internal Server Error")
                    .cause(
                        new GuardianFailureException(
                            String.format(
                                "Guardian '%s' runs on the %s phase, not on the %s phase",
                                guardian.getId(),
                                guardian.getPhase(),
                                phase
                            )
                        )
                    )
            );
        }
        ApiType apiType = callingApiType(ctx);
        if (!guardian.isCompatibleWith(apiType)) {
            // A declared restriction that the calling API violates never heals by itself: fail closed so the
            // misconfiguration is visible instead of silently unguarded.
            return ctx.interruptWith(
                new ExecutionFailure(HttpStatusCode.INTERNAL_SERVER_ERROR_500)
                    .key(INCOMPATIBLE_API_TYPE_KEY)
                    .message("Internal Server Error")
                    .cause(
                        new GuardianFailureException(
                            String.format(
                                "Guardian '%s' supports API types %s, not %s",
                                guardian.getId(),
                                guardian.getApiTypes().stream().map(ApiType::getLabel).sorted().toList(),
                                apiType == null ? "this API" : apiType.getLabel()
                            )
                        )
                    )
            );
        }
        return reactor.execute(ctx, this);
    }

    /**
     * Reads the environment from the deployed API rather than from the environment attribute, which any API flow can
     * overwrite to run the Guardian of another environment, with its resources and secrets.
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
