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
package io.gravitee.gateway.reactive.policy;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.MessageExecutionContext;
import io.gravitee.gateway.reactive.api.context.RequestExecutionContext;
import io.gravitee.gateway.reactive.api.policy.Policy;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import java.util.List;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PolicyChain is responsible for executing a given list of policies respecting the original order.
 * Policy execution must occur in an ordered sequence, one by one.
 * It is the responsibility of the chain to handle any policy execution error and stop the entire execution of the policy chain.
 * The PolicyChain should also check the current execution context against the {@link ExecutionContext#isInterrupted()} flag and abort the chain execution accordingly.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PolicyChain {

    private final Logger log = LoggerFactory.getLogger(PolicyChain.class);

    private final String id;
    private final Flowable<Policy> policies;
    private final ExecutionPhase phase;

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

    /**
     * Executes all the policies composing the chain.
     *
     * @param ctx the current context that will be passed to each policy to be executed.
     *
     * @return a {@link Completable} that completes when all the policies of the chain have been executed or the chain has been interrupted.
     * The {@link Completable} may complete in error in case of any error occurred while executing the policies.
     */
    public Completable execute(RequestExecutionContext ctx) {
        log.debug("Executing chain {}", id);

        return policies
            .flatMapCompletable(policy -> executePolicy(ctx, policy), false, 1)
            .onErrorResumeNext(throwable -> interruptOnError(ctx, throwable));
    }

    private Completable interruptOnError(RequestExecutionContext ctx, Throwable throwable) {
        try {
            // FIXME: ExecutionFailure
            ctx.interrupt();
            ctx.response().status(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
            log.error("An unexpected error occurred during policy chain execution. Interrupt the chain.", throwable);

            return Completable.complete();
        } catch (Throwable t) {
            return Completable.error(t);
        }
    }

    private Completable executePolicy(RequestExecutionContext ctx, Policy policy) {
        if (ctx.isInterrupted()) {
            return Completable.complete();
        }

        log.debug("Executing policy {} on phase {}", policy.getId(), phase);

        if (ExecutionPhase.REQUEST == phase || ExecutionPhase.RESPONSE == phase) {
            // Ensure right context is given
            if (!(ctx instanceof RequestExecutionContext)) {
                return Completable.error(
                    new IllegalArgumentException(
                        String.format("Context '%s' is compatible with the given phase '%s'", ctx.getClass().getSimpleName(), phase)
                    )
                );
            }
            RequestExecutionContext RequestExecutionContext = (RequestExecutionContext) ctx;
            if (ExecutionPhase.REQUEST == phase) {
                return policy.onRequest(RequestExecutionContext);
            } else {
                return policy.onResponse(RequestExecutionContext);
            }
        } else if (ExecutionPhase.ASYNC_REQUEST == phase || ExecutionPhase.ASYNC_RESPONSE == phase) {
            // Ensure right context is given
            if (!(ctx instanceof MessageExecutionContext)) {
                return Completable.error(
                    new IllegalArgumentException(
                        String.format("Context '%s' is compatible with the given phase '%s'", ctx.getClass().getSimpleName(), phase)
                    )
                );
            }
            MessageExecutionContext messageExecutionContext = (MessageExecutionContext) ctx;
            if (ExecutionPhase.ASYNC_REQUEST == phase) {
                return policy
                    .onRequest(messageExecutionContext)
                    .andThen(
                        messageExecutionContext
                            .incomingMessageFlow()
                            .onMessage(
                                upstream ->
                                    policy
                                        .onMessageFlow(messageExecutionContext, upstream)
                                        .flatMapMaybe(message -> policy.onMessage(messageExecutionContext, message))
                            )
                    );
            } else {
                return policy
                    .onResponse(messageExecutionContext)
                    .andThen(
                        messageExecutionContext
                            .outgoingMessageFlow()
                            .onMessage(
                                upstream ->
                                    policy
                                        .onMessageFlow(messageExecutionContext, upstream)
                                        .flatMapMaybe(message -> policy.onMessage(messageExecutionContext, message))
                            )
                    );
            }
        }
        return Completable.error(new RuntimeException("Invalid execution phase"));
    }
}
