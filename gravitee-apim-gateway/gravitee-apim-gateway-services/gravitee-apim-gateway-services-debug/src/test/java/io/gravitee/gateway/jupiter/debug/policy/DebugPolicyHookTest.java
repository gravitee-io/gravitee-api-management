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
package io.gravitee.gateway.jupiter.debug.policy;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.gravitee.gateway.jupiter.api.context.RequestExecutionContext;
import io.gravitee.gateway.jupiter.debug.reactor.context.DebugRequestExecutionContext;
import io.reactivex.Completable;
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
class DebugPolicyHookTest {

    private DebugPolicyHook debugPolicyHook;

    @Mock
    private DebugRequestExecutionContext debugCtx;

    @BeforeEach
    public void beforeEach() {
        debugPolicyHook = new DebugPolicyHook();
    }

    @Test
    public void shouldHaveAnId() {
        assertThat(debugPolicyHook.id()).isEqualTo("hook-debug-policy");
    }

    @Test
    public void shouldCallPreOnCtx() {
        when(debugCtx.prePolicyExecution(eq("id"), eq(ExecutionPhase.REQUEST))).thenReturn(Completable.complete());
        debugPolicyHook.pre("id", debugCtx, ExecutionPhase.REQUEST).test().assertResult();
        verify(debugCtx).prePolicyExecution(eq("id"), eq(ExecutionPhase.REQUEST));
    }

    @Test
    public void shouldCallPostOnCtx() {
        when(debugCtx.postPolicyExecution()).thenReturn(Completable.complete());
        debugPolicyHook.post("id", debugCtx, ExecutionPhase.REQUEST).test().assertResult();
        verify(debugCtx).postPolicyExecution();
    }

    @Test
    public void shouldCallPostOnExceptionOnCtx() {
        when(debugCtx.postPolicyExecution(any(RuntimeException.class))).thenReturn(Completable.complete());
        debugPolicyHook.error("id", debugCtx, ExecutionPhase.REQUEST, new RuntimeException()).test().assertResult();
        verify(debugCtx).postPolicyExecution(any(RuntimeException.class));
    }

    @Test
    public void shouldCallPostOnInterruptWithFailureOnCtx() {
        when(debugCtx.postPolicyExecution(any(ExecutionFailure.class))).thenReturn(Completable.complete());
        debugPolicyHook.interruptWith("id", debugCtx, ExecutionPhase.REQUEST, new ExecutionFailure(400)).test().assertResult();
        verify(debugCtx).postPolicyExecution(any(ExecutionFailure.class));
    }

    @Test
    public void shouldCallPostOnInterruptOnCtx() {
        when(debugCtx.postPolicyExecution()).thenReturn(Completable.complete());
        debugPolicyHook.interrupt("id", debugCtx, ExecutionPhase.REQUEST).test().assertResult();
        verify(debugCtx).postPolicyExecution();
    }
}
