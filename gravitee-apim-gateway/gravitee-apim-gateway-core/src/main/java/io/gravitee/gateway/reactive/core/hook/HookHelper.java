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
import io.gravitee.gateway.reactive.api.hook.MessageHook;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionHelper;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HookHelper {

    private static final Logger log = LoggerFactory.getLogger(HookHelper.class);

    private HookHelper() {}

    public static <T extends HttpHook> Completable hook(
        final Supplier<Completable> completableSupplier,
        final String componentId,
        final List<T> hooks,
        final HttpExecutionContext ctx,
        final ExecutionPhase executionPhase
    ) {
        if (hooks != null && !hooks.isEmpty()) {
            return executeHooks(componentId, hooks, HookPhase.PRE, ctx, executionPhase, null, null)
                .andThen(Completable.defer(completableSupplier::get))
                .andThen(executeHooks(componentId, hooks, HookPhase.POST, ctx, executionPhase, null, null))
                .onErrorResumeNext(throwable ->
                    executeHookOnError(componentId, hooks, ctx, executionPhase, throwable).andThen(Completable.error(throwable))
                );
        } else {
            return completableSupplier.get();
        }
    }

    public static <T> Maybe<T> hookMaybe(
        final Supplier<Maybe<T>> maybeSupplier,
        final String componentId,
        final List<MessageHook> hooks,
        final HttpExecutionContext ctx,
        final ExecutionPhase executionPhase
    ) {
        if (hooks != null && !hooks.isEmpty()) {
            return executeHooks(componentId, hooks, HookPhase.PRE, ctx, executionPhase, null, null)
                .andThen(Maybe.defer(maybeSupplier::get))
                .switchIfEmpty(executeHooks(componentId, hooks, HookPhase.POST, ctx, executionPhase, null, null).toMaybe())
                .flatMap(t -> executeHooks(componentId, hooks, HookPhase.POST, ctx, executionPhase, null, null).andThen(Maybe.just(t)))
                .onErrorResumeNext(throwable -> {
                    return executeHookOnError(componentId, hooks, ctx, executionPhase, throwable).andThen(Maybe.error(throwable));
                });
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
            .concatMapCompletable(hook -> {
                switch (phase) {
                    case PRE:
                        return hook.pre(componentId, ctx, executionPhase);
                    case POST:
                        return hook.post(componentId, ctx, executionPhase);
                    case INTERRUPT:
                        return hook.interrupt(componentId, ctx, executionPhase);
                    case INTERRUPT_WITH:
                        return hook.interruptWith(componentId, ctx, executionPhase, executionFailure);
                    case ERROR:
                        return hook.error(componentId, ctx, executionPhase, throwable);
                    default:
                        return Completable.error(new RuntimeException(String.format("Unknown hook phase %s while executing hook", phase)));
                }
            })
            .doOnError(error -> log.warn("Unable to execute '{}' hook on flow '{}'", phase.name(), componentId, error))
            .onErrorComplete();
    }
}
