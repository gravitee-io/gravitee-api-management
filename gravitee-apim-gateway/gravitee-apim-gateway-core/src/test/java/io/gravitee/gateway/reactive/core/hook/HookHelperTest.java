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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.gateway.reactive.api.ExecutionFailure;
import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.hook.PolicyMessageHook;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionException;
import io.gravitee.gateway.reactive.core.context.interruption.InterruptionFailureException;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Maybe;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class HookHelperTest {

    @Mock
    private PolicyMessageHook mockHook;

    @Mock
    private PolicyMessageHook mockHook2;

    @Mock
    private ExecutionContext mockCtx;

    @BeforeEach
    public void beforeEach() {
        lenient().when(mockHook.pre(any(), any(), any())).thenReturn(Completable.complete());
        lenient().when(mockHook.post(any(), any(), any())).thenReturn(Completable.complete());
        lenient().when(mockHook.error(any(), any(), any(), any())).thenReturn(Completable.complete());
        lenient().when(mockHook.interrupt(any(), any(), any())).thenReturn(Completable.complete());
        lenient().when(mockHook.interruptWith(any(), any(), any(), any())).thenReturn(Completable.complete());
        lenient().when(mockHook2.pre(any(), any(), any())).thenReturn(Completable.complete());
        lenient().when(mockHook2.post(any(), any(), any())).thenReturn(Completable.complete());
        lenient().when(mockHook2.error(any(), any(), any(), any())).thenReturn(Completable.complete());
        lenient().when(mockHook2.interrupt(any(), any(), any())).thenReturn(Completable.complete());
        lenient().when(mockHook2.interruptWith(any(), any(), any(), any())).thenReturn(Completable.complete());
    }

    @Test
    public void shouldCompleteWithoutHookingCompleteCompletable() {
        // Just use call a mock to be sure the completable has been subscribed
        Completable completable = Completable.fromRunnable(() -> mockCtx.getAttributes());
        HookHelper.hook(() -> completable, "componentId", List.of(), mockCtx, ExecutionPhase.REQUEST).test().assertResult();
        verifyNoMoreInteractions(mockHook);
        verify(mockCtx).getAttributes();
        verifyNoMoreInteractions(mockHook2);
    }

    @Test
    public void shouldCompleteWhenHookingCompleteCompletable() {
        // Just use call a mock to be sure the completable has been subscribed
        Completable completable = Completable.fromRunnable(() -> mockCtx.getAttributes());
        HookHelper
            .hook(() -> completable, "componentId", List.of(mockHook, mockHook2), mockCtx, ExecutionPhase.REQUEST)
            .test()
            .assertResult();
        verify(mockHook).pre(eq("componentId"), any(), any());
        verify(mockHook).post(eq("componentId"), any(), any());
        verify(mockHook, times(0)).error(any(), any(), any(), any());
        verify(mockHook, times(0)).interrupt(any(), any(), any());
        verify(mockHook, times(0)).interruptWith(any(), any(), any(), any());
        verifyNoMoreInteractions(mockHook);

        verify(mockCtx).getAttributes();

        verify(mockHook2).pre(eq("componentId"), any(), any());
        verify(mockHook2).post(eq("componentId"), any(), any());
        verify(mockHook2, times(0)).error(any(), any(), any(), any());
        verify(mockHook2, times(0)).interrupt(any(), any(), any());
        verify(mockHook2, times(0)).interruptWith(any(), any(), any(), any());
        verifyNoMoreInteractions(mockHook2);
    }

    @Test
    public void shouldCompleteOnErrorWhenHookingErrorCompletable() {
        Completable completable = Completable.error(new RuntimeException());
        HookHelper
            .hook(() -> completable, "componentId", List.of(mockHook, mockHook2), mockCtx, ExecutionPhase.REQUEST)
            .test()
            .assertFailure(RuntimeException.class);
        verify(mockHook).pre(eq("componentId"), any(), any());
        verify(mockHook).error(eq("componentId"), any(), any(), any(RuntimeException.class));
        verify(mockHook, times(0)).post(any(), any(), any());
        verify(mockHook, times(0)).interrupt(any(), any(), any());
        verify(mockHook, times(0)).interruptWith(any(), any(), any(), any());
        verifyNoMoreInteractions(mockHook);
        verify(mockHook2).pre(eq("componentId"), any(), any());
        verify(mockHook2).error(eq("componentId"), any(), any(), any(RuntimeException.class));
        verify(mockHook2, times(0)).post(any(), any(), any());
        verify(mockHook2, times(0)).interrupt(any(), any(), any());
        verify(mockHook2, times(0)).interruptWith(any(), any(), any(), any());
        verifyNoMoreInteractions(mockHook2);
    }

    @Test
    public void shouldCompleteOnErrorWhenHookingInterruptionCompletable() {
        Completable completable = Completable.error(new InterruptionException());
        HookHelper
            .hook(() -> completable, "componentId", List.of(mockHook, mockHook2), mockCtx, ExecutionPhase.REQUEST)
            .test()
            .assertFailure(RuntimeException.class);
        verify(mockHook).pre(eq("componentId"), any(), any());
        verify(mockHook).interrupt(eq("componentId"), any(), any());
        verify(mockHook, times(0)).post(any(), any(), any());
        verify(mockHook, times(0)).error(any(), any(), any(), any());
        verify(mockHook, times(0)).interruptWith(any(), any(), any(), any());
        verifyNoMoreInteractions(mockHook);
        verify(mockHook2).pre(eq("componentId"), any(), any());
        verify(mockHook2).interrupt(eq("componentId"), any(), any());
        verify(mockHook2, times(0)).post(any(), any(), any());
        verify(mockHook2, times(0)).error(any(), any(), any(), any());
        verify(mockHook2, times(0)).interruptWith(any(), any(), any(), any());
        verifyNoMoreInteractions(mockHook2);
    }

    @Test
    public void shouldCompleteOnErrorWhenHookingInterruptionWithFailureCompletable() {
        Completable completable = Completable.error(new InterruptionFailureException(new ExecutionFailure(404)));
        HookHelper
            .hook(() -> completable, "componentId", List.of(mockHook, mockHook2), mockCtx, ExecutionPhase.REQUEST)
            .test()
            .assertFailure(RuntimeException.class);
        verify(mockHook).pre(eq("componentId"), any(), any());
        verify(mockHook).interruptWith(eq("componentId"), any(), any(), any());
        verify(mockHook, times(0)).post(any(), any(), any());
        verify(mockHook, times(0)).error(any(), any(), any(), any());
        verify(mockHook, times(0)).interrupt(any(), any(), any());
        verifyNoMoreInteractions(mockHook);
        verify(mockHook2).pre(eq("componentId"), any(), any());
        verify(mockHook2).interruptWith(eq("componentId"), any(), any(), any());
        verify(mockHook2, times(0)).post(any(), any(), any());
        verify(mockHook2, times(0)).error(any(), any(), any(), any());
        verify(mockHook2, times(0)).interrupt(any(), any(), any());
        verifyNoMoreInteractions(mockHook2);
    }

    @Test
    public void shouldReturnMaybeWithoutHookingMaybe() {
        Maybe<String> maybe = Maybe.fromCallable(() -> {
            mockCtx.getAttributes();
            return "string";
        });
        HookHelper.hookMaybe(() -> maybe, "componentId", List.of(), mockCtx, ExecutionPhase.REQUEST).test().assertResult("string");
        verifyNoMoreInteractions(mockHook);
        verify(mockCtx).getAttributes();
        verifyNoMoreInteractions(mockHook2);
    }

    @Test
    public void shouldReturnMaybeWhenHookingMaybe() {
        // Just use call a mock to be sure the completable has been subscribed
        Maybe<String> maybe = Maybe.fromCallable(() -> {
            mockCtx.getAttributes();
            return "string";
        });
        HookHelper
            .hookMaybe(() -> maybe, "componentId", List.of(mockHook, mockHook2), mockCtx, ExecutionPhase.REQUEST)
            .test()
            .assertResult("string");
        verify(mockHook).pre(eq("componentId"), any(), any());
        verify(mockHook).post(eq("componentId"), any(), any());
        verify(mockHook, times(0)).error(any(), any(), any(), any());
        verify(mockHook, times(0)).interrupt(any(), any(), any());
        verify(mockHook, times(0)).interruptWith(any(), any(), any(), any());
        verifyNoMoreInteractions(mockHook);

        verify(mockCtx).getAttributes();

        verify(mockHook2).pre(eq("componentId"), any(), any());
        verify(mockHook2).post(eq("componentId"), any(), any());
        verify(mockHook2, times(0)).error(any(), any(), any(), any());
        verify(mockHook2, times(0)).interrupt(any(), any(), any());
        verify(mockHook2, times(0)).interruptWith(any(), any(), any(), any());
        verifyNoMoreInteractions(mockHook2);
    }

    @Test
    public void shouldReturnEmptyMaybeWhenHookingEmptyMaybe() {
        // Just use call a mock to be sure the completable has been subscribed
        Maybe<String> maybe = Maybe.fromCallable(() -> {
            mockCtx.getAttributes();
            return null;
        });
        HookHelper
            .hookMaybe(() -> maybe, "componentId", List.of(mockHook, mockHook2), mockCtx, ExecutionPhase.REQUEST)
            .test()
            .assertResult();
        verify(mockHook).pre(eq("componentId"), any(), any());
        verify(mockHook).post(eq("componentId"), any(), any());
        verify(mockHook, times(0)).error(any(), any(), any(), any());
        verify(mockHook, times(0)).interrupt(any(), any(), any());
        verify(mockHook, times(0)).interruptWith(any(), any(), any(), any());
        verifyNoMoreInteractions(mockHook);

        verify(mockCtx).getAttributes();

        verify(mockHook2).pre(eq("componentId"), any(), any());
        verify(mockHook2).post(eq("componentId"), any(), any());
        verify(mockHook2, times(0)).error(any(), any(), any(), any());
        verify(mockHook2, times(0)).interrupt(any(), any(), any());
        verify(mockHook2, times(0)).interruptWith(any(), any(), any(), any());
        verifyNoMoreInteractions(mockHook2);
    }

    @Test
    public void shouldReturnMaybeOnErrorWhenHookingErrorMaybe() {
        Maybe<?> maybe = Maybe.error(new RuntimeException());
        HookHelper
            .hookMaybe(() -> maybe, "componentId", List.of(mockHook, mockHook2), mockCtx, ExecutionPhase.REQUEST)
            .test()
            .assertFailure(RuntimeException.class);
        verify(mockHook).pre(eq("componentId"), any(), any());
        verify(mockHook).error(eq("componentId"), any(), any(), any(RuntimeException.class));
        verify(mockHook, times(0)).post(any(), any(), any());
        verify(mockHook, times(0)).interrupt(any(), any(), any());
        verify(mockHook, times(0)).interruptWith(any(), any(), any(), any());
        verifyNoMoreInteractions(mockHook);
        verify(mockHook2).pre(eq("componentId"), any(), any());
        verify(mockHook2).error(eq("componentId"), any(), any(), any(RuntimeException.class));
        verify(mockHook2, times(0)).post(any(), any(), any());
        verify(mockHook2, times(0)).interrupt(any(), any(), any());
        verify(mockHook2, times(0)).interruptWith(any(), any(), any(), any());
        verifyNoMoreInteractions(mockHook2);
    }

    @Test
    public void shouldReturnMaybeOnErrorWhenHookingInterruptionCompletable() {
        Maybe<?> maybe = Maybe.error(new InterruptionException());
        HookHelper
            .hookMaybe(() -> maybe, "componentId", List.of(mockHook, mockHook2), mockCtx, ExecutionPhase.REQUEST)
            .test()
            .assertFailure(RuntimeException.class);
        verify(mockHook).pre(eq("componentId"), any(), any());
        verify(mockHook).interrupt(eq("componentId"), any(), any());
        verify(mockHook, times(0)).post(any(), any(), any());
        verify(mockHook, times(0)).error(any(), any(), any(), any());
        verify(mockHook, times(0)).interruptWith(any(), any(), any(), any());
        verifyNoMoreInteractions(mockHook);
        verify(mockHook2).pre(eq("componentId"), any(), any());
        verify(mockHook2).interrupt(eq("componentId"), any(), any());
        verify(mockHook2, times(0)).post(any(), any(), any());
        verify(mockHook2, times(0)).error(any(), any(), any(), any());
        verify(mockHook2, times(0)).interruptWith(any(), any(), any(), any());
        verifyNoMoreInteractions(mockHook2);
    }

    @Test
    public void shouldReturnMaybeOnErrorWhenHookingInterruptionWithFailureCompletable() {
        Maybe<?> maybe = Maybe.error(new InterruptionFailureException(new ExecutionFailure(404)));
        HookHelper
            .hookMaybe(() -> maybe, "componentId", List.of(mockHook, mockHook2), mockCtx, ExecutionPhase.REQUEST)
            .test()
            .assertFailure(RuntimeException.class);
        verify(mockHook).pre(eq("componentId"), any(), any());
        verify(mockHook).interruptWith(eq("componentId"), any(), any(), any());
        verify(mockHook, times(0)).post(any(), any(), any());
        verify(mockHook, times(0)).error(any(), any(), any(), any());
        verify(mockHook, times(0)).interrupt(any(), any(), any());
        verifyNoMoreInteractions(mockHook);
        verify(mockHook2).pre(eq("componentId"), any(), any());
        verify(mockHook2).interruptWith(eq("componentId"), any(), any(), any());
        verify(mockHook2, times(0)).post(any(), any(), any());
        verify(mockHook2, times(0)).error(any(), any(), any(), any());
        verify(mockHook2, times(0)).interrupt(any(), any(), any());
        verifyNoMoreInteractions(mockHook2);
    }
}
