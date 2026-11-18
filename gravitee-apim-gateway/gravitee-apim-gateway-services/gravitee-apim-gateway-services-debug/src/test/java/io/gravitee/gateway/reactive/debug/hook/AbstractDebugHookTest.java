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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.core.context.WrappedExecutionContext;
import io.gravitee.gateway.reactive.debug.reactor.context.DebugExecutionContext;
import io.reactivex.rxjava3.core.Completable;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
class AbstractDebugHookTest {

    @Test
    public void shouldThrowExceptionWithWrongCtxOnPre() {
        DummyDebugHook dummyDebugHook = new DummyDebugHook();
        dummyDebugHook.pre("id", mock(ExecutionContext.class), ExecutionPhase.REQUEST).test().assertFailure(IllegalArgumentException.class);
    }

    @Test
    public void shouldThrowExceptionWithWrongCtxOnPost() {
        DummyDebugHook dummyDebugHook = new DummyDebugHook();
        dummyDebugHook
            .post("id", mock(ExecutionContext.class), ExecutionPhase.REQUEST)
            .test()
            .assertFailure(IllegalArgumentException.class);
    }

    @Test
    public void shouldThrowExceptionWithWrongCtxOnError() {
        DummyDebugHook dummyDebugHook = new DummyDebugHook();
        dummyDebugHook
            .error("id", mock(ExecutionContext.class), ExecutionPhase.REQUEST, new RuntimeException())
            .test()
            .assertFailure(IllegalArgumentException.class);
    }

    @Test
    public void shouldThrowExceptionWithWrongCtxOnInterruptFailure() {
        DummyDebugHook dummyDebugHook = new DummyDebugHook();
        dummyDebugHook
            .interruptWith("id", mock(ExecutionContext.class), ExecutionPhase.REQUEST, new ExecutionFailure(500))
            .test()
            .assertFailure(IllegalArgumentException.class);
    }

    @Test
    public void shouldThrowExceptionWithWrongCtxOnInterrupt() {
        DummyDebugHook dummyDebugHook = new DummyDebugHook();
        dummyDebugHook
            .interrupt("id", mock(ExecutionContext.class), ExecutionPhase.REQUEST)
            .test()
            .assertFailure(IllegalArgumentException.class);
    }

    @Test
    void should_hand_the_wrapped_debug_context_to_the_hook_when_the_context_is_decorated() {
        DebugExecutionContext debugCtx = mock(DebugExecutionContext.class);
        HttpExecutionContext decorated = decorating(decorating(debugCtx));
        AtomicReference<DebugExecutionContext> seen = new AtomicReference<>();
        AbstractDebugHook hook = new DummyDebugHook() {
            @Override
            protected Completable pre(String id, DebugExecutionContext ctx, ExecutionPhase executionPhase) {
                seen.set(ctx);
                return Completable.complete();
            }
        };

        hook.pre("id", decorated, ExecutionPhase.REQUEST).test().assertResult();

        assertThat(seen.get()).isSameAs(debugCtx);
    }

    @Test
    void should_fail_when_the_decorated_context_does_not_wrap_a_debug_context() {
        HttpExecutionContext decorated = decorating(mock(ExecutionContext.class));

        new DummyDebugHook().pre("id", decorated, ExecutionPhase.REQUEST).test().assertFailure(IllegalArgumentException.class);
    }

    private static HttpExecutionContext decorating(HttpExecutionContext wrapped) {
        HttpExecutionContext decorator = mock(HttpExecutionContext.class, withSettings().extraInterfaces(WrappedExecutionContext.class));
        when(((WrappedExecutionContext) decorator).unwrap()).thenReturn(wrapped);
        return decorator;
    }

    public class DummyDebugHook extends AbstractDebugHook {

        @Override
        protected Completable pre(final String id, final DebugExecutionContext ctx, final ExecutionPhase executionPhase) {
            return Completable.complete();
        }

        @Override
        protected Completable post(final String id, final DebugExecutionContext ctx, final ExecutionPhase executionPhase) {
            return Completable.complete();
        }

        @Override
        protected Completable error(
            final String id,
            final DebugExecutionContext ctx,
            final ExecutionPhase executionPhase,
            final Throwable throwable
        ) {
            return Completable.complete();
        }

        @Override
        protected Completable interrupt(final String id, final DebugExecutionContext ctx, final ExecutionPhase executionPhase) {
            return Completable.complete();
        }

        @Override
        protected Completable interruptWith(
            final String id,
            final DebugExecutionContext ctx,
            final ExecutionPhase executionPhase,
            final ExecutionFailure failure
        ) {
            return Completable.complete();
        }

        @Override
        public String id() {
            return "hook-dummy";
        }
    }
}
