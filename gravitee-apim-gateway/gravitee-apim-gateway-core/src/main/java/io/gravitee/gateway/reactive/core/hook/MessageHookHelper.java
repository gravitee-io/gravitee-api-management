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
import io.gravitee.gateway.reactive.api.message.Message;
import io.gravitee.gateway.reactive.core.context.OnMessagesInterceptor;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionHelper;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.FlowableTransformer;
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
public class MessageHookHelper {

    public static <T extends HttpHook> Completable hook(
        final Supplier<Completable> completableSupplier,
        final String componentId,
        final List<T> hooks,
        final HttpExecutionContext ctx,
        final ExecutionPhase executionPhase
    ) {
        if (hooks != null && !hooks.isEmpty()) {
            OnMessagesInterceptor<Message> onMessagesInterceptor = extractInterceptor(ctx, executionPhase);
            if (onMessagesInterceptor != null) {
                List<MessageHook> messageHooks = hooks
                    .stream()
                    .filter(hook -> hook instanceof MessageHook)
                    .map(hook -> ((MessageHook) hook))
                    .toList();
                if (!messageHooks.isEmpty()) {
                    String interceptorId = "interceptor-message-hook-%s".formatted(componentId);
                    return completableSupplier
                        .get()
                        .doOnTerminate(() -> onMessagesInterceptor.unregisterMessagesInterceptor(interceptorId))
                        .doOnSubscribe(disposable ->
                            onMessagesInterceptor.registerMessagesInterceptor(
                                OnMessagesInterceptor.MessagesInterceptor
                                    .<Message>builder()
                                    .id(interceptorId)
                                    .transformersFunction(interceptedTransformer ->
                                        transformerInterceptor(interceptedTransformer, componentId, messageHooks, ctx, executionPhase)
                                    )
                                    .build()
                            )
                        );
                }
            }
        }
        return completableSupplier.get();
    }

    private static OnMessagesInterceptor<Message> extractInterceptor(final HttpExecutionContext ctx, final ExecutionPhase executionPhase) {
        OnMessagesInterceptor<Message> onMessagesInterceptor;
        if (executionPhase == null || executionPhase == ExecutionPhase.MESSAGE_REQUEST) {
            onMessagesInterceptor = (OnMessagesInterceptor<Message>) ctx.request();
        } else if (executionPhase == ExecutionPhase.MESSAGE_RESPONSE) {
            onMessagesInterceptor = (OnMessagesInterceptor<Message>) ctx.response();
        } else {
            onMessagesInterceptor = null;
        }
        return onMessagesInterceptor;
    }

    public static <T extends MessageHook> FlowableTransformer<Message, Message> transformerInterceptor(
        final FlowableTransformer<Message, Message> interceptedTransformer,
        final String componentId,
        final List<T> hooks,
        final HttpExecutionContext ctx,
        final ExecutionPhase executionPhase
    ) {
        return upstream ->
            upstream.concatMap(originalMessage ->
                executeHooks(componentId, hooks, HookPhase.PRE, ctx, executionPhase, originalMessage, null, null)
                    .andThen(interceptedTransformer.apply(Flowable.just(originalMessage)))
                    .switchIfEmpty(
                        executeHooks(componentId, hooks, HookPhase.POST, ctx, executionPhase, originalMessage, null, null)
                            .andThen(Flowable.empty())
                    )
                    .concatMap(transformMessage ->
                        executeHooks(componentId, hooks, HookPhase.POST, ctx, executionPhase, transformMessage, null, null)
                            .andThen(Flowable.just(transformMessage))
                    )
                    .onErrorResumeNext(throwable ->
                        executeHookOnError(componentId, hooks, ctx, executionPhase, originalMessage, throwable)
                            .andThen(Flowable.error(throwable))
                    )
                    .doOnCancel(() ->
                        executeHooks(componentId, hooks, HookPhase.POST, ctx, executionPhase, originalMessage, null, null).subscribe()
                    )
            );
    }

    private static <T extends MessageHook> Completable executeHookOnError(
        final String componentId,
        final List<T> hooks,
        final HttpExecutionContext ctx,
        final ExecutionPhase executionPhase,
        final Message message,
        final Throwable throwable
    ) {
        if (InterruptionHelper.isInterruption(throwable)) {
            return executeHooks(componentId, hooks, HookPhase.INTERRUPT, ctx, executionPhase, message, throwable, null);
        } else if (InterruptionHelper.isInterruptionWithFailure(throwable)) {
            return executeHooks(
                componentId,
                hooks,
                HookPhase.INTERRUPT_WITH,
                ctx,
                executionPhase,
                message,
                throwable,
                InterruptionHelper.extractExecutionFailure(throwable)
            );
        } else {
            return executeHooks(componentId, hooks, HookPhase.ERROR, ctx, executionPhase, message, throwable, null);
        }
    }

    private static <T extends MessageHook> Completable executeHooks(
        final String componentId,
        final List<T> hooks,
        final HookPhase phase,
        final HttpExecutionContext ctx,
        final ExecutionPhase executionPhase,
        final Message message,
        final Throwable throwable,
        final ExecutionFailure executionFailure
    ) {
        return Flowable
            .fromIterable(hooks)
            .concatMapCompletable(hook ->
                switch (phase) {
                    case PRE -> hook.preMessage(componentId, ctx, executionPhase, message);
                    case POST -> hook.postMessage(componentId, ctx, executionPhase, message);
                    case INTERRUPT -> hook.interruptMessage(componentId, ctx, executionPhase, message);
                    case INTERRUPT_WITH -> hook.interruptMessageWith(componentId, ctx, executionPhase, message, executionFailure);
                    case ERROR -> hook.errorMessage(componentId, ctx, executionPhase, message, throwable);
                    case CANCEL -> {
                        hook.cancelMessage(componentId, ctx, executionPhase, message);
                        yield Completable.complete();
                    }
                }
            )
            .doOnError(error -> log.warn("Unable to execute '{}' message hook on '{}'", phase.name(), componentId, error))
            .onErrorComplete();
    }
}
