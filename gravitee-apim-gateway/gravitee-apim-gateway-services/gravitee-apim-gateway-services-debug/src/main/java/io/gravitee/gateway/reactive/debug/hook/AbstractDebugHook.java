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
package io.gravitee.gateway.reactive.debug.hook;

import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.hook.HttpHook;
import io.gravitee.gateway.reactive.debug.reactor.context.DebugExecutionContext;
import io.reactivex.rxjava3.core.Completable;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractDebugHook implements HttpHook {

    @Override
    public Completable pre(final String id, final HttpExecutionContext ctx, final ExecutionPhase executionPhase) {
        DebugExecutionContext debugCtx = getExecutionContext(ctx);
        if (debugCtx != null) {
            return pre(id, debugCtx, executionPhase);
        }
        return errorOnWrongContext();
    }

    protected abstract Completable pre(final String id, final DebugExecutionContext ctx, final ExecutionPhase executionPhase);

    @Override
    public Completable post(final String id, final HttpExecutionContext ctx, final ExecutionPhase executionPhase) {
        DebugExecutionContext debugCtx = getExecutionContext(ctx);
        if (debugCtx != null) {
            return post(id, debugCtx, executionPhase);
        }
        return errorOnWrongContext();
    }

    protected abstract Completable post(final String id, final DebugExecutionContext ctx, final ExecutionPhase executionPhase);

    @Override
    public Completable error(
        final String id,
        final HttpExecutionContext ctx,
        final ExecutionPhase executionPhase,
        final Throwable throwable
    ) {
        DebugExecutionContext debugCtx = getExecutionContext(ctx);
        if (debugCtx != null) {
            return error(id, debugCtx, executionPhase, throwable);
        }
        return errorOnWrongContext();
    }

    protected abstract Completable error(
        final String id,
        final DebugExecutionContext ctx,
        final ExecutionPhase executionPhase,
        final Throwable throwable
    );

    @Override
    public Completable interrupt(final String id, final HttpExecutionContext ctx, final ExecutionPhase executionPhase) {
        DebugExecutionContext debugCtx = getExecutionContext(ctx);
        if (debugCtx != null) {
            return interrupt(id, debugCtx, executionPhase);
        }
        return errorOnWrongContext();
    }

    protected abstract Completable interrupt(final String id, final DebugExecutionContext ctx, final ExecutionPhase executionPhase);

    @Override
    public Completable interruptWith(
        final String id,
        final HttpExecutionContext ctx,
        final ExecutionPhase executionPhase,
        final ExecutionFailure failure
    ) {
        return Completable.defer(() -> {
            DebugExecutionContext debugCtx = getExecutionContext(ctx);
            if (debugCtx != null) {
                return interruptWith(id, debugCtx, executionPhase, failure);
            }
            return errorOnWrongContext();
        });
    }

    protected abstract Completable interruptWith(
        final String id,
        final DebugExecutionContext ctx,
        final ExecutionPhase executionPhase,
        final ExecutionFailure failure
    );

    private DebugExecutionContext getExecutionContext(final HttpExecutionContext ctx) {
        if (ctx instanceof DebugExecutionContext) {
            return (DebugExecutionContext) ctx;
        }
        return null;
    }

    private Completable errorOnWrongContext() {
        return Completable.error(new IllegalArgumentException("Given context is not a DebugExecutionContext"));
    }
}
