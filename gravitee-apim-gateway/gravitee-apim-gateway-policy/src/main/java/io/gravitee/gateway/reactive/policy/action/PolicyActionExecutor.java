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
package io.gravitee.gateway.reactive.policy.action;

import io.gravitee.gateway.reactive.api.ComponentType;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.hook.PolicyHook;
import io.gravitee.gateway.reactive.api.policy.base.BasePolicy;
import io.gravitee.gateway.reactive.core.context.ComponentScope;
import io.gravitee.gateway.reactive.core.hook.HookHelper;
import io.gravitee.gateway.reactive.policy.tracing.TracingPolicyActionHook;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Executes the actions policies registered on the execution context for a given {@link PolicyActionType}. Each action
 * runs in the scope of the policy that registered it, so that logs and metrics are attributed to it, and through the
 * policy action hooks: with tracing enabled, each action gets its own span.
 */
public class PolicyActionExecutor {

    private final PolicyActionType type;
    private final List<PolicyHook> hooks;

    public PolicyActionExecutor(final PolicyActionType type, final boolean tracingEnabled) {
        this.type = type;
        this.hooks = tracingEnabled ? List.of(new TracingPolicyActionHook(type)) : List.of();
    }

    /**
     * Executes the action registered by the policy.
     */
    public Completable execute(
        final HttpExecutionContext ctx,
        final BasePolicy policy,
        final Function<HttpExecutionContext, Completable> action
    ) {
        return Completable.defer(() -> {
            ComponentScope.push(ctx, ComponentType.POLICY, policy.id());
            return HookHelper.hook(() -> action.apply(ctx), policy.id(), hooks, ctx, type.phase()).doFinally(() ->
                ComponentScope.remove(ctx, ComponentType.POLICY, policy.id())
            );
        });
    }

    /**
     * Executes the actions one after the other, in the order they are given. A failing action stops the run and its
     * error is propagated, as a failing policy's would.
     */
    public Completable executeAll(
        final HttpExecutionContext ctx,
        final Map<BasePolicy, Function<HttpExecutionContext, Completable>> actions
    ) {
        if (actions.isEmpty()) {
            return Completable.complete();
        }
        return Flowable.fromIterable(actions.entrySet()).concatMapCompletable(entry -> execute(ctx, entry.getKey(), entry.getValue()));
    }
}
