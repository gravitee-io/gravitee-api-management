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
package io.gravitee.gateway.reactive.policy.tracing;

import io.gravitee.gateway.reactive.api.ExecutionPhase;
import io.gravitee.gateway.reactive.api.context.http.HttpExecutionContext;
import io.gravitee.gateway.reactive.api.hook.MessageHook;
import io.gravitee.tracing.api.Span;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TracingMessageHook extends AbstractTracingPolicyHook implements MessageHook {

    private static final String SPAN_MESSAGE_ATTR = "message";

    @Override
    public String id() {
        return "hook-tracing-message-policy";
    }

    @Override
    protected void withAttributes(final String id, final HttpExecutionContext ctx, final ExecutionPhase executionPhase, final Span span) {
        super.withAttributes(id, ctx, executionPhase, span);
        if (ExecutionPhase.MESSAGE_REQUEST == executionPhase) {
            span.withAttribute(SPAN_MESSAGE_ATTR, "incoming");
        } else if (ExecutionPhase.MESSAGE_RESPONSE == executionPhase) {
            span.withAttribute(SPAN_MESSAGE_ATTR, "outgoing");
        }
    }
}
