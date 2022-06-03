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
package io.gravitee.gateway.jupiter.debug.invoker;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.debug.core.invoker.InvokerResponse;
import io.gravitee.gateway.jupiter.api.ExecutionFailure;
import io.gravitee.gateway.jupiter.api.ExecutionPhase;
import io.gravitee.gateway.jupiter.core.context.MutableResponse;
import io.gravitee.gateway.jupiter.debug.reactor.context.DebugRequestExecutionContext;
import io.reactivex.Completable;
import io.reactivex.Single;
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
class DebugInvokerHookTest {

    private DebugInvokerHook debugInvokerHook;

    @Mock
    private DebugRequestExecutionContext debugCtx;

    @BeforeEach
    public void beforeEach() {
        debugInvokerHook = new DebugInvokerHook();
    }

    @Test
    public void shouldHaveAnId() {
        assertThat(debugInvokerHook.id()).isEqualTo("hook-debug-invoker");
    }

    @Test
    public void shouldNotDoAnythingWhenCallingPre() {
        debugInvokerHook.pre("id", debugCtx, ExecutionPhase.REQUEST).test().assertResult();
        verifyNoInteractions(debugCtx);
    }

    @Test
    public void shouldNotDoAnythingWhenCallingInterrupt() {
        debugInvokerHook.interrupt("id", debugCtx, ExecutionPhase.REQUEST).test().assertResult();
        verifyNoInteractions(debugCtx);
    }

    @Test
    public void shouldNotDoAnythingWhenCallingInterruptWith() {
        debugInvokerHook.interruptWith("id", debugCtx, ExecutionPhase.REQUEST, new ExecutionFailure(200)).test().assertResult();
        verifyNoInteractions(debugCtx);
    }

    @Test
    public void shouldStoreInvokerDataOnPost() {
        MutableResponse mutableResponse = mock(MutableResponse.class);
        when(mutableResponse.headers()).thenReturn(HttpHeaders.create());
        when(mutableResponse.status()).thenReturn(200);
        when(mutableResponse.bodyOrEmpty()).thenReturn(Single.just(Buffer.buffer("body")));
        when(debugCtx.response()).thenReturn(mutableResponse);
        InvokerResponse invokerResponse = new InvokerResponse();
        when(debugCtx.getInvokerResponse()).thenReturn(invokerResponse);
        debugInvokerHook.post("id", debugCtx, ExecutionPhase.REQUEST).test().assertResult();
        assertThat(invokerResponse.getStatus()).isEqualTo(200);
        assertThat(invokerResponse.getHeaders()).isNotNull();
        assertThat(invokerResponse.getBuffer().toString()).isEqualTo("body");
    }

    @Test
    public void shouldStoreInvokerDataOnError() {
        MutableResponse mutableResponse = mock(MutableResponse.class);
        when(mutableResponse.headers()).thenReturn(HttpHeaders.create());
        when(mutableResponse.status()).thenReturn(200);
        when(mutableResponse.bodyOrEmpty()).thenReturn(Single.just(Buffer.buffer("body")));
        when(debugCtx.response()).thenReturn(mutableResponse);
        InvokerResponse invokerResponse = new InvokerResponse();
        when(debugCtx.getInvokerResponse()).thenReturn(invokerResponse);
        debugInvokerHook.error("id", debugCtx, ExecutionPhase.REQUEST, new RuntimeException()).test().assertResult();
        assertThat(invokerResponse.getStatus()).isEqualTo(200);
        assertThat(invokerResponse.getHeaders()).isNotNull();
        assertThat(invokerResponse.getBuffer().toString()).isEqualTo("body");
    }
}
