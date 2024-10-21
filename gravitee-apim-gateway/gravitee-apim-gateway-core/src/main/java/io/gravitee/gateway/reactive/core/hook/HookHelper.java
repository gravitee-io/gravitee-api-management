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
package io.gravitee.gateway.reactive.core.hook;

import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.hook.HttpHook;
import io.gravitee.gateway.reactive.api.hook.PolicyMessageHook;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionHelper;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.List;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HookHelper {

    public static <T extends HttpHook> Completable hook(
        final Supplier<Completable> completableSupplier,
        final String componentId,
        final List<T> hooks,
        final HttpExecutionContext ctx,
        final ExecutionPhase executionPhase
    ) {
        if (hooks != null && !hooks.isEmpty()) {
            return executeHooks(componentId, hooks, HookPhase.PRE, ctx, executionPhase, null, null)
                .andThen(
                    Completable.defer(() -> {
                        if (executionPhase == ExecutionPhase.MESSAGE_REQUEST || executionPhase == ExecutionPhase.MESSAGE_RESPONSE) {
                            return MessageHookHelper.hook(completableSupplier, componentId, hooks, ctx, executionPhase);
                        }
                        return completableSupplier.get();
                    })
                )
                .andThen(executeHooks(componentId, hooks, HookPhase.POST, ctx, executionPhase, null, null))
                .onErrorResumeNext(throwable ->
                    executeHookOnError(componentId, hooks, ctx, executionPhase, throwable).andThen(Completable.error(throwable))
                )
                .doOnDispose(() ->
                    executeHooks(componentId, hooks, HookPhase.CANCEL, ctx, executionPhase, null, null)
                        // In case of cancel phase, there is nothing to subscribe to.
                        .subscribe()
                );
        } else {
            return completableSupplier.get();
        }
    }

    public static <T> Maybe<T> hookMaybe(
        final Supplier<Maybe<T>> maybeSupplier,
        final String componentId,
        final List<PolicyMessageHook> hooks,
        final HttpExecutionContext ctx,
        final ExecutionPhase executionPhase
    ) {
        if (hooks != null && !hooks.isEmpty()) {
            return executeHooks(componentId, hooks, HookPhase.PRE, ctx, executionPhase, null, null)
                .andThen(Maybe.defer(maybeSupplier::get))
                .switchIfEmpty(executeHooks(componentId, hooks, HookPhase.POST, ctx, executionPhase, null, null).toMaybe())
                .concatMap(t -> executeHooks(componentId, hooks, HookPhase.POST, ctx, executionPhase, null, null).andThen(Maybe.just(t)))
                .onErrorResumeNext(throwable ->
                    executeHookOnError(componentId, hooks, ctx, executionPhase, throwable).andThen(Maybe.error(throwable))
                )
                .doOnDispose(() -> executeHooks(componentId, hooks, HookPhase.CANCEL, ctx, executionPhase, null, null).subscribe());
        } else {
            return maybeSupplier.get();
        }
    }

    private static <T extends HttpHook> Completable executeHookOnError(
        final String componentId,
        final List<T> hooks,
        final HttpExecutionContext ctx,
        final ExecutionPhase executionPhase,
        final Throwable throwable
    ) {
        if (InterruptionHelper.isInterruption(throwable)) {
            return executeHooks(componentId, hooks, HookPhase.INTERRUPT, ctx, executionPhase, throwable, null);
        } else if (InterruptionHelper.isInterruptionWithFailure(throwable)) {
            return executeHooks(
                componentId,
                hooks,
                HookPhase.INTERRUPT_WITH,
                ctx,
                executionPhase,
                throwable,
                InterruptionHelper.extractExecutionFailure(throwable)
            );
        } else {
            return executeHooks(componentId, hooks, HookPhase.ERROR, ctx, executionPhase, throwable, null);
        }
    }

    private static <T extends HttpHook> Completable executeHooks(
        final String componentId,
        final List<T> hooks,
        final HookPhase phase,
        final HttpExecutionContext ctx,
        final ExecutionPhase executionPhase,
        final Throwable throwable,
        final ExecutionFailure executionFailure
    ) {
        return Flowable
            .fromIterable(hooks)
            .concatMapCompletable(hook ->
                switch (phase) {
                    case PRE -> hook.pre(componentId, ctx, executionPhase);
                    case POST -> hook.post(componentId, ctx, executionPhase);
                    case INTERRUPT -> hook.interrupt(componentId, ctx, executionPhase);
                    case INTERRUPT_WITH -> hook.interruptWith(componentId, ctx, executionPhase, executionFailure);
                    case ERROR -> hook.error(componentId, ctx, executionPhase, throwable);
                    case CANCEL -> {
                        hook.cancel(componentId, ctx, executionPhase);
                        yield Completable.complete();
                    }
                }
            )
            .doOnError(error -> log.warn("Unable to execute '{}' hook on '{}'", phase.name(), componentId, error))
            .onErrorComplete();
    }
}
