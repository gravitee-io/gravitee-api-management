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
package io.gravitee.gateway.reactive.policy.adapter.policy;

import static org.mockito.Mockito.*;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.RequestExecutionContext;
import io.gravitee.policy.api.PolicyResult;
import io.reactivex.CompletableEmitter;
import org.junit.jupiter.api.Test;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
class PolicyChainAdapterTest {

    @Test
    public void shouldCompleteWhenDoNext() {
        final RequestExecutionContext ctx = mock(RequestExecutionContext.class);
        final CompletableEmitter emitter = mock(CompletableEmitter.class);

        final PolicyChainAdapter cut = new PolicyChainAdapter(ctx, emitter);

        cut.doNext(mock(Request.class), mock(Response.class));

        verify(emitter).onComplete();
    }

    @Test
    public void shouldInterruptAndCompleteWhenFailWith() {
        final RequestExecutionContext ctx = mock(RequestExecutionContext.class);
        final io.gravitee.gateway.reactive.api.context.Response response = mock(io.gravitee.gateway.reactive.api.context.Response.class);
        final CompletableEmitter emitter = mock(CompletableEmitter.class);
        final PolicyResult policyResult = mock(PolicyResult.class);

        when(ctx.response()).thenReturn(response);
        when(policyResult.statusCode()).thenReturn(HttpStatusCode.SERVICE_UNAVAILABLE_503);

        final PolicyChainAdapter cut = new PolicyChainAdapter(ctx, emitter);

        cut.failWith(policyResult);

        // TODO: This is subject to change when ExecutionFailure will be implemented.
        verify(response).status(HttpStatusCode.SERVICE_UNAVAILABLE_503);
        verify(emitter).onComplete();
        verify(ctx).interrupt();
    }

    @Test
    public void shouldInterruptAndCompleteWhenStreamFailWith() {
        final RequestExecutionContext ctx = mock(RequestExecutionContext.class);
        final io.gravitee.gateway.reactive.api.context.Response response = mock(io.gravitee.gateway.reactive.api.context.Response.class);
        final CompletableEmitter emitter = mock(CompletableEmitter.class);
        final PolicyResult policyResult = mock(PolicyResult.class);

        when(ctx.response()).thenReturn(response);
        when(policyResult.statusCode()).thenReturn(HttpStatusCode.SERVICE_UNAVAILABLE_503);

        final PolicyChainAdapter cut = new PolicyChainAdapter(ctx, emitter);

        cut.streamFailWith(policyResult);

        // TODO: This is subject to change when ExecutionFailure will be implemented.
        verify(response).status(HttpStatusCode.SERVICE_UNAVAILABLE_503);
        verify(emitter).onComplete();
        verify(ctx).interrupt();
    }
}
