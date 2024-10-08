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
package io.gravitee.gateway.reactive.policy.adapter.policy;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.http.HttpMessageExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpPlainExecutionContext;
import io.gravitee.gateway.reactive.api.policy.http.HttpPolicy;
import io.gravitee.gateway.reactive.policy.adapter.context.ExecutionContextAdapter;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Policy adapter allows to adapt the behavior of a v3 policy to make it compatible with the reactive execution.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PolicyAdapter implements HttpPolicy {

    private final io.gravitee.gateway.policy.Policy policy;

    public PolicyAdapter(io.gravitee.gateway.policy.Policy policy) {
        this.policy = policy;
    }

    @Override
    public String id() {
        return policy.id();
    }

    @Override
    public Completable onRequest(HttpPlainExecutionContext ctx) {
        return execute(ctx, ExecutionPhase.REQUEST);
    }

    @Override
    public Completable onResponse(HttpPlainExecutionContext ctx) {
        return execute(ctx, ExecutionPhase.RESPONSE);
    }

    @Override
    public Completable onMessageRequest(final HttpMessageExecutionContext ctx) {
        return Completable.error(new RuntimeException("Cannot adapt v3 policy for message execution"));
    }

    @Override
    public Completable onMessageResponse(final HttpMessageExecutionContext ctx) {
        return Completable.error(new RuntimeException("Cannot adapt v3 policy for message execution"));
    }

    /**
     * The objective is to chain the execution of <code>policy.execute(...)</code> and <code>policy.stream(...)</code> functions.
     * This way, the legacy policy behave like a new one where there is no more distinction between head and body phases.
     *
     * <b>WARN</b>: there is no reason to invoke this policy adapter for an ASYNC_REQUEST or ASYNC_RESPONSE phase because these execution phases require new policy implementations.
     *
     * @param ctx the execution context to use.
     * @param phase the current execution phase (REQUEST or RESPONSE).
     *
     * @return a {@link Completable} indicating the execution is completed.
     */
    private Completable execute(final HttpPlainExecutionContext ctx, final ExecutionPhase phase) {
        final ExecutionContextAdapter adaptedCtx = ExecutionContextAdapter.create(ctx);

        Completable completable;

        if (policy.isRunnable()) {
            // Execute the policy.execute(...) function.
            completable = this.policyExecute(adaptedCtx);
        } else {
            completable = Completable.complete();
        }

        if (policy.isStreamable()) {
            // Chain execution with the execution of the policy.stream(...) function.
            completable = completable.andThen(this.policyStream(adaptedCtx, phase));
        }

        return completable.doFinally(adaptedCtx::restore);
    }

    private Completable policyExecute(ExecutionContextAdapter adaptedCtx) {
        return Completable.create(emitter -> {
            try {
                policy.execute(new PolicyChainAdapter(adaptedCtx.getDelegate(), emitter), adaptedCtx);
            } catch (Throwable t) {
                emitter.tryOnError(new Exception("An error occurred while trying to execute policy " + policy.id(), t));
            }
        });
    }

    private Completable policyStream(ExecutionContextAdapter adaptedCtx, ExecutionPhase phase) {
        return Completable.create(emitter -> {
            try {
                // Invoke the policy to get the appropriate read/write stream.
                final HttpPlainExecutionContext ctx = adaptedCtx.getDelegate();
                final ReadWriteStream<Buffer> stream = policy.stream(new PolicyChainAdapter(ctx, emitter), adaptedCtx);

                if (stream == null) {
                    // Null stream means that the policy does nothing.
                    emitter.onComplete();
                } else {
                    final AtomicBoolean hasBody = new AtomicBoolean(false);
                    final Buffer newBuffer = Buffer.buffer();
                    // Add a body handler to capture all the chunks eventually written by the policy.
                    stream.bodyHandler(buffer -> {
                        newBuffer.appendBuffer(buffer);
                        hasBody.set(true);
                    });

                    // Add an end handler to capture end of the legacy stream and continue the reactive chain.
                    stream.endHandler(result -> {
                        if (hasBody.get()) {
                            // Replace the chunks of the request or response if not interrupted.
                            setBody(ctx, phase, newBuffer);
                        }
                        emitter.onComplete();
                    });

                    getBody(ctx, phase)
                        .doOnSuccess(stream::write)
                        .doFinally(stream::end)
                        .doOnError(emitter::tryOnError)
                        .onErrorResumeWith(s -> {})
                        .subscribe();
                }
            } catch (Throwable t) {
                emitter.tryOnError(new Exception("An error occurred while trying to execute policy " + policy.id(), t));
            }
        });
    }

    private Maybe<Buffer> getBody(HttpPlainExecutionContext ctx, ExecutionPhase phase) {
        if (phase == ExecutionPhase.REQUEST) {
            return ctx.request().body();
        } else {
            return ctx.response().body();
        }
    }

    private void setBody(HttpPlainExecutionContext ctx, ExecutionPhase phase, Buffer newBuffer) {
        if (phase == ExecutionPhase.REQUEST) {
            ctx.request().body(newBuffer);
        } else {
            ctx.response().body(newBuffer);
        }
    }
}
