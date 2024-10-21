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
package io.gravitee.gateway.policy.impl.tracing;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.policy.Policy;
import io.gravitee.gateway.policy.PolicyException;
import io.gravitee.node.api.opentelemetry.Span;
import io.gravitee.node.api.opentelemetry.internal.InternalRequest;
import io.gravitee.policy.api.PolicyChain;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TracingPolicy implements Policy {

    static final String SPAN_ATTRIBUTE = "policy";

    private final Policy policy;

    public TracingPolicy(final Policy policy) {
        this.policy = policy;
    }

    @Override
    public String id() {
        return policy.id();
    }

    @Override
    public void execute(PolicyChain chain, ExecutionContext context) throws PolicyException {
        Span span = context.getTracer().startSpanFrom(new InternalRequest(this.policy.id(), Map.of(SPAN_ATTRIBUTE, this.policy.id())));

        try {
            // The policy is ending once doNext or failWith are called from the chain
            this.policy.execute(new TracingPolicyChain(chain, context, span), context);
        } catch (Exception ex) {
            context.getTracer().endOnError(span, ex);
            // Propagate the exception
            throw ex;
        }
    }

    @Override
    public ReadWriteStream<Buffer> stream(PolicyChain chain, ExecutionContext context) throws PolicyException {
        ReadWriteStream<Buffer> stream = policy.stream(chain, context);
        return (stream == null) ? null : new TracingReadWriteStream(context, stream, policy);
    }

    @Override
    public boolean isStreamable() {
        return policy.isStreamable();
    }

    @Override
    public boolean isRunnable() {
        return policy.isRunnable();
    }
}
