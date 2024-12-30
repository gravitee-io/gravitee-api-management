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
package io.gravitee.gateway.reactive.policy;

import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.base.BaseExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.hook.Hookable;
import io.gravitee.gateway.reactive.api.hook.HttpHook;
import io.gravitee.gateway.reactive.api.hook.PolicyHook;
import io.gravitee.gateway.reactive.api.hook.PolicyMessageHook;
import io.gravitee.gateway.reactive.api.policy.http.HttpPolicy;
import io.gravitee.gateway.reactive.core.hook.HookHelper;
import io.reactivex.rxjava3.core.Completable;
import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * PolicyChain is responsible for executing a given list of policies respecting the original order.
 * Policy execution must occur in an ordered sequence, one by one.
 * It is the responsibility of the chain to handle any policy execution error and stop the entire execution of the policy chain.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
public class HttpPolicyChain extends AbstractPolicyChain<HttpPolicy> implements Hookable<HttpHook> {

    private List<PolicyHook> policyHooks;
    private List<PolicyMessageHook> policyMessageHooks;

    /**
     * Creates a policy chain with the given list of policies.
     *
     * @param id an arbitrary id that helps to identify the policy chain at execution time.
     * @param policies the list of the policies to be part of the execution chain.
     * @param phase the execution phase that will be used to determine the method of the policies to execute.
     */
    public HttpPolicyChain(@Nonnull String id, @Nonnull List<HttpPolicy> policies, @Nonnull ExecutionPhase phase) {
        super(id, policies, phase);
    }

    @Override
    public void addHooks(final List<HttpHook> hooks) {
        if (this.policyHooks == null) {
            this.policyHooks = new ArrayList<>();
        }
        if (this.policyMessageHooks == null) {
            this.policyMessageHooks = new ArrayList<>();
        }
        hooks.forEach(hook -> {
            if (hook instanceof PolicyHook) {
                this.policyHooks.add((PolicyHook) hook);
            } else if (hook instanceof PolicyMessageHook) {
                this.policyMessageHooks.add((PolicyMessageHook) hook);
            }
        });
    }

    @Override
    protected Completable executePolicy(final BaseExecutionContext baseCtx, final HttpPolicy policy) {
        log.debug("Executing policy {} on phase {} in policy chain {}", policy.id(), phase, id);

        HttpExecutionContext ctx = (HttpExecutionContext) baseCtx;
        return switch (phase) {
            case REQUEST -> HookHelper.hook(() -> policy.onRequest(ctx), policy.id(), policyHooks, ctx, phase);
            case RESPONSE -> HookHelper.hook(() -> policy.onResponse(ctx), policy.id(), policyHooks, ctx, phase);
            case MESSAGE_REQUEST -> HookHelper.hook(() -> policy.onMessageRequest(ctx), policy.id(), policyMessageHooks, ctx, phase);
            case MESSAGE_RESPONSE -> HookHelper.hook(() -> policy.onMessageResponse(ctx), policy.id(), policyMessageHooks, ctx, phase);
            default -> Completable.error(new IllegalArgumentException("Execution phase unknown"));
        };
    }
}
