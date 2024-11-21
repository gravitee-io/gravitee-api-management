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
package io.gravitee.gateway.reactive.handlers.api.security;

import io.gravitee.definition.model.Api;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.hook.Hookable;
import io.gravitee.gateway.reactive.api.hook.SecurityPlanHook;
import io.gravitee.gateway.reactive.core.hook.HookHelper;
import io.gravitee.gateway.reactive.handlers.api.security.plan.HttpSecurityPlan;
import io.gravitee.gateway.reactive.handlers.api.security.plan.HttpSecurityPlanFactory;
import io.gravitee.gateway.reactive.policy.PolicyManager;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * {@link HttpSecurityChain} is a special chain dedicated to execute policy associated with plans.
 * The security chain is responsible to create {@link HttpSecurityPlan} for each plan of the api and executed them in order.
 * Only the first {@link HttpSecurityPlan} that can handle the current request is executed.
 * The result of the security chain execution depends on this {@link HttpSecurityPlan} execution.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpSecurityChain
    extends AbstractSecurityChain<HttpSecurityPlan, HttpPlainExecutionContext>
    implements Hookable<SecurityPlanHook> {

    private final ExecutionPhase executionPhase;

    private List<SecurityPlanHook> securityPlanHooks;

    public HttpSecurityChain(Api api, PolicyManager policyManager, ExecutionPhase executionPhase) {
        super(
            Flowable.fromIterable(
                api
                    .getPlans()
                    .stream()
                    .map(plan -> HttpSecurityPlanFactory.forPlan(plan, policyManager))
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparingInt(HttpSecurityPlan::order))
                    .collect(Collectors.toList())
            )
        );
        this.executionPhase = executionPhase;
    }

    public HttpSecurityChain(Flowable<HttpSecurityPlan> securityPlans, ExecutionPhase executionPhase) {
        super(securityPlans);
        this.executionPhase = executionPhase;
    }

    @Override
    protected Completable sendError(HttpPlainExecutionContext ctx, ExecutionFailure failure) {
        return ctx.interruptWith(failure);
    }

    @Override
    protected Single<Boolean> executePlan(HttpSecurityPlan httpSecurityPlan, HttpPlainExecutionContext ctx) {
        return HookHelper
            .hook(
                () -> httpSecurityPlan.execute(ctx, executionPhase),
                httpSecurityPlan.id(),
                securityPlanHooks,
                (HttpExecutionContext) ctx,
                executionPhase
            )
            .andThen(TRUE);
    }

    @Override
    public void addHooks(final List<SecurityPlanHook> hooks) {
        if (this.securityPlanHooks == null) {
            this.securityPlanHooks = new ArrayList<>();
        }
        this.securityPlanHooks.addAll(hooks);
    }
}
