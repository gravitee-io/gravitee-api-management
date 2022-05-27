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
package io.gravitee.gateway.reactive.core.hook;

import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.RequestExecutionContext;
import io.gravitee.gateway.reactive.api.hook.Hook;
import io.gravitee.gateway.reactive.api.hook.MessageHook;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionHelper;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HookHelper {

    private static final Logger log = LoggerFactory.getLogger(HookHelper.class);

    public static <T extends Hook> Completable hook(
        final Completable completable,
        final String componentId,
        final List<T> hooks,
        final RequestExecutionContext ctx,
        final ExecutionPhase executionPhase
    ) {
        return completable
            .doOnSubscribe(disposable -> executeHooks(componentId, hooks, HookPhase.PRE, ctx, executionPhase, null, null))
            .doOnComplete(() -> executeHooks(componentId, hooks, HookPhase.POST, ctx, executionPhase, null, null))
            .doOnError(
                throwable -> {
                    if (InterruptionHelper.isInterruption(throwable)) {
                        executeHooks(componentId, hooks, HookPhase.INTERRUPT, ctx, executionPhase, throwable, null);
                    } else if (InterruptionHelper.isInterruptionWithFailure(throwable)) {
                        executeHooks(
                            componentId,
                            hooks,
                            HookPhase.INTERRUPT_WITH,
                            ctx,
                            executionPhase,
                            throwable,
                            InterruptionHelper.extractExecutionFailure(throwable)
                        );
                    } else {
                        executeHooks(componentId, hooks, HookPhase.ERROR, ctx, executionPhase, throwable, null);
                    }
                }
            );
    }

    public static <T> Maybe<T> hook(
        final Maybe<T> maybe,
        final String componentId,
        final List<MessageHook> hooks,
        final RequestExecutionContext ctx,
        final ExecutionPhase executionPhase
    ) {
        return maybe
            .doOnSubscribe(disposable -> executeHooks(componentId, hooks, HookPhase.PRE, ctx, executionPhase, null, null))
            .doOnEvent(
                (event, throwable) -> {
                    if (throwable != null) {
                        if (InterruptionHelper.isInterruption(throwable)) {
                            executeHooks(componentId, hooks, HookPhase.INTERRUPT, ctx, executionPhase, throwable, null);
                        } else if (InterruptionHelper.isInterruptionWithFailure(throwable)) {
                            executeHooks(
                                componentId,
                                hooks,
                                HookPhase.INTERRUPT_WITH,
                                ctx,
                                executionPhase,
                                throwable,
                                InterruptionHelper.extractExecutionFailure(throwable)
                            );
                        } else {
                            executeHooks(componentId, hooks, HookPhase.INTERRUPT_WITH, ctx, executionPhase, throwable, null);
                        }
                    } else {
                        executeHooks(componentId, hooks, HookPhase.POST, ctx, executionPhase, null, null);
                    }
                }
            );
    }

    private static <T extends Hook> void executeHooks(
        final String componentId,
        final List<T> hooks,
        final HookPhase phase,
        final RequestExecutionContext ctx,
        final ExecutionPhase executionPhase,
        final Throwable throwable,
        final ExecutionFailure executionFailure
    ) {
        if (hooks != null) {
            hooks.forEach(
                hook -> {
                    try {
                        switch (phase) {
                            case PRE:
                                hook.pre(componentId, ctx, executionPhase);
                                break;
                            case POST:
                                hook.post(componentId, ctx, executionPhase);
                                break;
                            case INTERRUPT:
                                hook.interrupt(componentId, ctx, executionPhase);
                                break;
                            case INTERRUPT_WITH:
                                hook.interruptWith(componentId, ctx, executionPhase, executionFailure);
                                break;
                            case ERROR:
                                hook.error(componentId, ctx, executionPhase, throwable);
                                break;
                        }
                    } catch (Exception e) {
                        log.warn(String.format("Unable to execute '%s' hook '%s' on flow '%s'", phase.name(), hook.id(), componentId), e);
                    }
                }
            );
        }
    }
}
