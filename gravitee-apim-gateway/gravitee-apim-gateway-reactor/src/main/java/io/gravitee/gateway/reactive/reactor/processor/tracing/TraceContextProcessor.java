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
package io.gravitee.gateway.reactive.reactor.processor.tracing;

import io.gravitee.common.utils.UUID;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.processor.Processor;
import io.gravitee.gateway.reactor.processor.transaction.TraceparentHelper;
import io.reactivex.rxjava3.core.Completable;

/**
 * A {@link Request} processor used to set check the traceparent header as described
 * in the https://www.w3.org/TR/trace-context specification.
 *
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TraceContextProcessor implements Processor {

    static final String HEADER_TRACE_STATE = "tracestate";
    static final String HEADER_TRACE_PARENT = "traceparent";

    @Override
    public String getId() {
        return "processor-trace-context";
    }

    @Override
    public Completable execute(final HttpExecutionContextInternal ctx) {
        return Completable.fromRunnable(() -> {
            String traceparent = ctx.request().headers().get(HEADER_TRACE_PARENT);
            if (traceparent == null) {
                traceparent = TraceparentHelper.buildTraceparentFrom(UUID.random());
                ctx.request().headers().set(HEADER_TRACE_PARENT, traceparent);
                // traceparent was missing, remove tracestate
                ctx.request().headers().remove(HEADER_TRACE_STATE);
            } else if (!TraceparentHelper.isValid(traceparent)) {
                // check format to know if we have to create a new one
                // see : https://www.w3.org/TR/trace-context/#a-traceparent-is-received
                traceparent = TraceparentHelper.buildTraceparentFrom(UUID.random());
                ctx.request().headers().set(HEADER_TRACE_PARENT, traceparent);
                // traceparent was invalid, remove tracestate
                ctx.request().headers().remove(HEADER_TRACE_STATE);
            }
            ctx.response().headers().set(HEADER_TRACE_PARENT, traceparent);
        });
    }
}
