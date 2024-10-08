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
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.hook.Hookable;
import io.gravitee.gateway.reactive.api.hook.HttpHook;
import io.gravitee.gateway.reactive.api.hook.MessageHook;
import io.gravitee.gateway.reactive.api.hook.PolicyHook;
import io.gravitee.gateway.reactive.api.policy.http.HttpPolicy;
import io.gravitee.gateway.reactive.core.hook.HookHelper;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PolicyChain is responsible for executing a given list of policies respecting the original order.
 * Policy execution must occur in an ordered sequence, one by one.
 * It is the responsibility of the chain to handle any policy execution error and stop the entire execution of the policy chain.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpPolicyChain implements Hookable<HttpHook>, PolicyChain<HttpExecutionContext> {

    private final Logger log = LoggerFactory.getLogger(HttpPolicyChain.class);

    private final String id;
    private final Flowable<HttpPolicy> policies;
    private final ExecutionPhase phase;
    private List<PolicyHook> policyHooks;
    private List<MessageHook> messageHooks;

    /**
     * Creates a policy chain with the given list of policies.
     *
     * @param id an arbitrary id that helps to identify the policy chain at execution time.
     * @param policies the list of the policies to be part of the execution chain.
     * @param phase the execution phase that will be used to determine the method of the policies to execute.
     */
    public HttpPolicyChain(@Nonnull String id, @Nonnull List<HttpPolicy> policies, @Nonnull ExecutionPhase phase) {
        this.id = id;
        this.phase = phase;
        this.policies = Flowable.fromIterable(policies);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void addHooks(final List<HttpHook> hooks) {
        if (this.policyHooks == null) {
            this.policyHooks = new ArrayList<>();
        }
        if (this.messageHooks == null) {
            this.messageHooks = new ArrayList<>();
        }
        hooks.forEach(hook -> {
            if (hook instanceof PolicyHook) {
                this.policyHooks.add((PolicyHook) hook);
            } else if (hook instanceof MessageHook) {
                this.messageHooks.add((MessageHook) hook);
            }
        });
    }

    /**
     * Executes all the policies composing the chain.
     *
     * @param ctx the current context that will be passed to each policy to be executed.
     *
     * @return a {@link Completable} that completes when all the policies of the chain have been executed or the chain has been interrupted.
     * The {@link Completable} may complete in error in case of any error occurred while executing the policies.
     */
    @Override
    public Completable execute(HttpExecutionContext ctx) {
        return policies.concatMapCompletable(policy -> executePolicy(ctx, policy));
    }

    private Completable executePolicy(final HttpExecutionContext ctx, final HttpPolicy policy) {
        log.debug("Executing policy {} on phase {} in policy chain {}", policy.id(), phase, id);

        switch (phase) {
            case REQUEST:
                return HookHelper.hook(() -> policy.onRequest(ctx), policy.id(), policyHooks, ctx, phase);
            case RESPONSE:
                return HookHelper.hook(() -> policy.onResponse(ctx), policy.id(), policyHooks, ctx, phase);
            case MESSAGE_REQUEST:
                return HookHelper.hook(() -> policy.onMessageRequest(ctx), policy.id(), messageHooks, ctx, phase);
            case MESSAGE_RESPONSE:
                return HookHelper.hook(() -> policy.onMessageResponse(ctx), policy.id(), messageHooks, ctx, phase);
            default:
                return Completable.error(new IllegalArgumentException("Execution phase unknown"));
        }
    }
}
