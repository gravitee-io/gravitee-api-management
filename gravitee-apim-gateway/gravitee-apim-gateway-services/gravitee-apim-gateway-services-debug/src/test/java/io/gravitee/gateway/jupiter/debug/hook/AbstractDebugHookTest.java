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

import static org.mockito.Mockito.mock;

import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.gravitee.gateway.jupiter.debug.reactor.context.DebugRequestExecutionContext;
import io.reactivex.Completable;
import org.junit.jupiter.api.Test;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
class AbstractDebugHookTest {

    @Test
    public void shouldThrowExceptionWithWrongCtxOnPre() {
        DummyDebugHook dummyDebugHook = new DummyDebugHook();
        dummyDebugHook
            .pre("id", mock(RequestExecutionContext.class), ExecutionPhase.REQUEST)
            .test()
            .assertFailure(IllegalArgumentException.class);
    }

    @Test
    public void shouldThrowExceptionWithWrongCtxOnPost() {
        DummyDebugHook dummyDebugHook = new DummyDebugHook();
        dummyDebugHook
            .post("id", mock(RequestExecutionContext.class), ExecutionPhase.REQUEST)
            .test()
            .assertFailure(IllegalArgumentException.class);
    }

    @Test
    public void shouldThrowExceptionWithWrongCtxOnError() {
        DummyDebugHook dummyDebugHook = new DummyDebugHook();
        dummyDebugHook
            .error("id", mock(RequestExecutionContext.class), ExecutionPhase.REQUEST, new RuntimeException())
            .test()
            .assertFailure(IllegalArgumentException.class);
    }

    @Test
    public void shouldThrowExceptionWithWrongCtxOnInterruptFailure() {
        DummyDebugHook dummyDebugHook = new DummyDebugHook();
        dummyDebugHook
            .interruptWith("id", mock(RequestExecutionContext.class), ExecutionPhase.REQUEST, new ExecutionFailure(500))
            .test()
            .assertFailure(IllegalArgumentException.class);
    }

    @Test
    public void shouldThrowExceptionWithWrongCtxOnInterrupt() {
        DummyDebugHook dummyDebugHook = new DummyDebugHook();
        dummyDebugHook
            .interrupt("id", mock(RequestExecutionContext.class), ExecutionPhase.REQUEST)
            .test()
            .assertFailure(IllegalArgumentException.class);
    }

    public class DummyDebugHook extends AbstractDebugHook {

        @Override
        protected Completable pre(final String id, final DebugRequestExecutionContext ctx, final ExecutionPhase executionPhase) {
            return Completable.complete();
        }

        @Override
        protected Completable post(final String id, final DebugRequestExecutionContext ctx, final ExecutionPhase executionPhase) {
            return Completable.complete();
        }

        @Override
        protected Completable error(
            final String id,
            final DebugRequestExecutionContext ctx,
            final ExecutionPhase executionPhase,
            final Throwable throwable
        ) {
            return Completable.complete();
        }

        @Override
        protected Completable interrupt(final String id, final DebugRequestExecutionContext ctx, final ExecutionPhase executionPhase) {
            return Completable.complete();
        }

        @Override
        protected Completable interruptWith(
            final String id,
            final DebugRequestExecutionContext ctx,
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
