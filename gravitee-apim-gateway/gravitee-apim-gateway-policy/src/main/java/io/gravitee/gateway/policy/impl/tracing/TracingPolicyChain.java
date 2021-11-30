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
package io.gravitee.gateway.policy.impl.tracing;

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.PolicyResult;
import io.gravitee.tracing.api.Span;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TracingPolicyChain implements PolicyChain {

    private final PolicyChain chain;
    private final Span span;

    TracingPolicyChain(final PolicyChain chain, final Span span) {
        this.chain = chain;
        this.span = span;
    }

    @Override
    public void doNext(Request request, Response response) {
        span.end();
        chain.doNext(request, response);
    }

    @Override
    public void failWith(PolicyResult policyResult) {
        span.reportError(policyResult.message()).end();
        chain.failWith(policyResult);
    }

    @Override
    public void streamFailWith(PolicyResult policyResult) {
        span.reportError(policyResult.message()).end();
        chain.failWith(policyResult);
    }
}
