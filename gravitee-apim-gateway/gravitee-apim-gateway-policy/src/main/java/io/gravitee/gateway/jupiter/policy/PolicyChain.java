/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.jupiter.policy;

import static io.reactivex.Completable.defer;

import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.gravitee.gateway.jupiter.api.context.MessageExecutionContext;
import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.gravitee.gateway.jupiter.api.hook.Hook;
import io.gravitee.gateway.jupiter.api.hook.Hookable;
import io.gravitee.gateway.jupiter.api.hook.MessageHook;
import io.gravitee.gateway.jupiter.api.hook.PolicyHook;
import io.gravitee.gateway.jupiter.api.policy.Policy;
import io.gravitee.gateway.jupiter.core.hook.HookHelper;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
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
public class PolicyChain implements Hookable<Hook> {

    private final Logger log = LoggerFactory.getLogger(PolicyChain.class);

    private final String id;
    private final Flowable<Policy> policies;
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
    public PolicyChain(@Nonnull String id, @Nonnull List<Policy> policies, @Nonnull ExecutionPhase phase) {
        this.id = id;
        this.phase = phase;
        this.policies = Flowable.fromIterable(policies);
    }

    public String getId() {
        return id;
    }

    @Override
    public void addHooks(final List<Hook> hooks) {
        if (this.policyHooks == null) {
            this.policyHooks = new ArrayList<>();
        }
        if (this.messageHooks == null) {
            this.messageHooks = new ArrayList<>();
        }
        hooks.forEach(
            hook -> {
                if (hook instanceof PolicyHook) {
                    this.policyHooks.add((PolicyHook) hook);
                } else if (hook instanceof MessageHook) {
                    this.messageHooks.add((MessageHook) hook);
                }
            }
        );
    }

    /**
     * Executes all the policies composing the chain.
     *
     * @param ctx the current context that will be passed to each policy to be executed.
     *
     * @return a {@link Completable} that completes when all the policies of the chain have been executed or the chain has been interrupted.
     * The {@link Completable} may complete in error in case of any error occurred while executing the policies.
     */
    public Completable execute(RequestExecutionContext ctx) {
        return policies
            .doOnSubscribe(subscription -> log.debug("Executing chain {}", id))
            .flatMapCompletable(policy -> executePolicy(ctx, policy), false, 1);
    }

    private Completable executePolicy(final RequestExecutionContext ctx, final Policy policy) {
        log.debug("Executing policy {} on phase {}", policy.id(), phase);

        // Ensure right context is given
        if (
            (ExecutionPhase.ASYNC_REQUEST == phase || ExecutionPhase.ASYNC_RESPONSE == phase) && (!(ctx instanceof MessageExecutionContext))
        ) {
            return Completable.error(
                new IllegalArgumentException(
                    String.format("Context '%s' is compatible with the given phase '%s'", ctx.getClass().getSimpleName(), phase)
                )
            );
        }

        if (ExecutionPhase.REQUEST == phase || ExecutionPhase.ASYNC_REQUEST == phase) {
            Completable requestExecution = HookHelper.hook(() -> policy.onRequest(ctx), policy.id(), policyHooks, ctx, phase);
            if (ExecutionPhase.ASYNC_REQUEST == phase) {
                MessageExecutionContext messageExecutionContext = (MessageExecutionContext) ctx;
                return requestExecution.andThen(
                    messageExecutionContext
                        .incomingMessageFlow()
                        .onMessage(
                            upstream ->
                                policy
                                    .onMessageFlow(messageExecutionContext, upstream)
                                    .flatMapMaybe(
                                        message ->
                                            HookHelper.hookMaybe(
                                                () -> policy.onMessage(messageExecutionContext, message),
                                                policy.id(),
                                                messageHooks,
                                                ctx,
                                                phase
                                            )
                                    )
                        )
                );
            }
            return requestExecution;
        } else {
            Completable responseExecution = HookHelper.hook(() -> policy.onResponse(ctx), policy.id(), policyHooks, ctx, phase);
            if (ExecutionPhase.ASYNC_RESPONSE == phase) {
                MessageExecutionContext messageExecutionContext = (MessageExecutionContext) ctx;
                return responseExecution.andThen(
                    messageExecutionContext
                        .outgoingMessageFlow()
                        .onMessage(
                            upstream ->
                                policy
                                    .onMessageFlow(messageExecutionContext, upstream)
                                    .flatMapMaybe(
                                        message ->
                                            HookHelper.hookMaybe(
                                                () -> policy.onMessage(messageExecutionContext, message),
                                                policy.id(),
                                                messageHooks,
                                                ctx,
                                                phase
                                            )
                                    )
                        )
                );
            }
            return responseExecution;
        }
    }
}
