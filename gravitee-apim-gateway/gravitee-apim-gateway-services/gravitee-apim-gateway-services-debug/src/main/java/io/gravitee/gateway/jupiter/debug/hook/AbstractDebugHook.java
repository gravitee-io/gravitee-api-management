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
package io.gravitee.gateway.jupiter.debug.hook;

import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.gravitee.gateway.jupiter.api.context.HttpExecutionContext;
import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.gravitee.gateway.jupiter.api.hook.Hook;
import io.gravitee.gateway.jupiter.debug.reactor.context.DebugRequestExecutionContext;
import io.reactivex.Completable;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractDebugHook implements Hook {

    @Override
    public Completable pre(final String id, final HttpExecutionContext ctx, final ExecutionPhase executionPhase) {
        DebugRequestExecutionContext debugCtx = getExecutionContext(ctx);
        if (debugCtx != null) {
            return pre(id, debugCtx, executionPhase);
        }
        return errorOnWrongContext();
    }

    protected abstract Completable pre(final String id, final DebugRequestExecutionContext ctx, final ExecutionPhase executionPhase);

    @Override
    public Completable post(final String id, final HttpExecutionContext ctx, final ExecutionPhase executionPhase) {
        DebugRequestExecutionContext debugCtx = getExecutionContext(ctx);
        if (debugCtx != null) {
            return post(id, debugCtx, executionPhase);
        }
        return errorOnWrongContext();
    }

    protected abstract Completable post(final String id, final DebugRequestExecutionContext ctx, final ExecutionPhase executionPhase);

    @Override
    public Completable error(
        final String id,
        final HttpExecutionContext ctx,
        final ExecutionPhase executionPhase,
        final Throwable throwable
    ) {
        DebugRequestExecutionContext debugCtx = getExecutionContext(ctx);
        if (debugCtx != null) {
            return error(id, debugCtx, executionPhase, throwable);
        }
        return errorOnWrongContext();
    }

    protected abstract Completable error(
        final String id,
        final DebugRequestExecutionContext ctx,
        final ExecutionPhase executionPhase,
        final Throwable throwable
    );

    @Override
    public Completable interrupt(final String id, final HttpExecutionContext ctx, final ExecutionPhase executionPhase) {
        DebugRequestExecutionContext debugCtx = getExecutionContext(ctx);
        if (debugCtx != null) {
            return interrupt(id, debugCtx, executionPhase);
        }
        return errorOnWrongContext();
    }

    protected abstract Completable interrupt(final String id, final DebugRequestExecutionContext ctx, final ExecutionPhase executionPhase);

    @Override
    public Completable interruptWith(
        final String id,
        final HttpExecutionContext ctx,
        final ExecutionPhase executionPhase,
        final ExecutionFailure failure
    ) {
        return Completable.defer(
            () -> {
                DebugRequestExecutionContext debugCtx = getExecutionContext(ctx);
                if (debugCtx != null) {
                    return interruptWith(id, debugCtx, executionPhase, failure);
                }
                return errorOnWrongContext();
            }
        );
    }

    protected abstract Completable interruptWith(
        final String id,
        final DebugRequestExecutionContext ctx,
        final ExecutionPhase executionPhase,
        final ExecutionFailure failure
    );

    private DebugRequestExecutionContext getExecutionContext(final HttpExecutionContext ctx) {
        if (ctx instanceof DebugRequestExecutionContext) {
            return (DebugRequestExecutionContext) ctx;
        }
        return null;
    }

    private Completable errorOnWrongContext() {
        return Completable.error(new IllegalArgumentException("Given context is not a DebugRequestExecutionContext"));
    }
}
