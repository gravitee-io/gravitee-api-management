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
package io.gravitee.gateway.reactive.debug.invoker;

import io.gravitee.gateway.debug.core.invoker.InvokerResponse;
import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.hook.InvokerHook;
import io.gravitee.gateway.reactive.debug.hook.AbstractDebugHook;
import io.gravitee.gateway.reactive.debug.reactor.context.DebugExecutionContext;
import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.core.Completable;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DebugInvokerHook extends AbstractDebugHook implements InvokerHook {

    @Override
    public String id() {
        return "hook-debug-invoker";
    }

    @Override
    protected Completable pre(final String id, final DebugExecutionContext ctx, final ExecutionPhase executionPhase) {
        return Completable.complete();
    }

    @Override
    protected Completable post(final String id, final DebugExecutionContext debugCtx, @Nullable final ExecutionPhase executionPhase) {
        return debugCtx
            .response()
            .bodyOrEmpty()
            .doOnSuccess(outputBody -> {
                InvokerResponse invokerResponse = debugCtx.getInvokerResponse();
                invokerResponse.setHeaders(debugCtx.response().headers());
                invokerResponse.setStatus(debugCtx.response().status());
                invokerResponse.getBuffer().appendBuffer(outputBody);
            })
            .ignoreElement();
    }

    @Override
    protected Completable error(
        final String id,
        final DebugExecutionContext debugCtx,
        final ExecutionPhase executionPhase,
        final Throwable throwable
    ) {
        // FIXME Shouldn't we do something with the throwable?
        return post(id, debugCtx, executionPhase);
    }

    @Override
    protected Completable interrupt(final String id, final DebugExecutionContext debugCtx, final ExecutionPhase executionPhase) {
        return Completable.complete();
    }

    @Override
    protected Completable interruptWith(
        final String id,
        final DebugExecutionContext debugCtx,
        final ExecutionPhase executionPhase,
        final ExecutionFailure failure
    ) {
        return Completable.complete();
    }
}
