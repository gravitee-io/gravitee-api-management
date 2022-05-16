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
import io.gravitee.gateway.reactive.reactor.handler.context.DefaultRequestExecutionContext;
import io.gravitee.gateway.reactive.reactor.handler.context.interruption.InterruptionFailureException;
import io.gravitee.policy.api.PolicyResult;
import io.reactivex.Completable;
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
        final RequestExecutionContext ctx = new DefaultRequestExecutionContext(null, null, null, null);
        final PolicyResult policyResult = PolicyResult.failure("key", 500, "error");

        Completable
            .create(
                emitter -> {
                    final PolicyChainAdapter policyChainAdapter = new PolicyChainAdapter(ctx, emitter);
                    policyChainAdapter.failWith(policyResult);
                }
            )
            .test()
            .assertFailure(InterruptionFailureException.class);
    }

    @Test
    public void shouldInterruptAndCompleteWhenStreamFailWith() {
        final RequestExecutionContext ctx = new DefaultRequestExecutionContext(null, null, null, null);
        final PolicyResult policyResult = PolicyResult.failure("key", 500, "error");

        Completable
            .create(
                emitter -> {
                    final PolicyChainAdapter policyChainAdapter = new PolicyChainAdapter(ctx, emitter);
                    policyChainAdapter.streamFailWith(policyResult);
                }
            )
            .test()
            .assertFailure(InterruptionFailureException.class);
    }
}
