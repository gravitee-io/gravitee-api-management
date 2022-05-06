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

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.RequestExecutionContext;
import io.gravitee.policy.api.PolicyResult;
import io.reactivex.CompletableEmitter;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PolicyChainAdapter implements io.gravitee.policy.api.PolicyChain {

    private final RequestExecutionContext ctx;
    private final CompletableEmitter emitter;

    public PolicyChainAdapter(RequestExecutionContext ctx, CompletableEmitter emitter) {
        this.ctx = ctx;
        this.emitter = emitter;
    }

    @Override
    public void doNext(Request request, Response response) {
        emitter.onComplete();
    }

    @Override
    public void streamFailWith(PolicyResult policyResult) {
        failWith(policyResult);
    }

    @Override
    public void failWith(PolicyResult policyResult) {
        // TODO: ExecutionFailure.
        ctx.response().status(policyResult.statusCode());

        //ctx.response().content(Buffer.buffer(policyResult.message()));
        ctx.interrupt();
        emitter.onComplete();
    }
}
