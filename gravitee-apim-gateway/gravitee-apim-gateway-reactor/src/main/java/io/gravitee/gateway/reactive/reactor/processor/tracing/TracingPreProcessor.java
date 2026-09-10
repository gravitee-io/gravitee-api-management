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

import io.gravitee.gateway.reactive.api.tracing.Tracer;
import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.processor.Processor;
import io.gravitee.gateway.reactive.core.tracing.AbstractTracingHook;
import io.reactivex.rxjava3.core.Completable;

/**
 * Enriches the root OTel span with Gravitee request identifiers once they are available.
 *
 * <p>The attributes are deferred on the request-scoped {@link Tracer} and stamped when the root span
 * closes, so this processor never needs a reference to the span itself. It runs immediately after
 * {@link io.gravitee.gateway.reactive.reactor.processor.transaction.TransactionPreProcessor}, so
 * {@code gravitee.transaction.id} is already assigned by the time it reads it.
 *
 * <p>This processor is only registered when OTel traces are enabled.
 *
 * @author GraviteeSource Team
 */
public class TracingPreProcessor implements Processor {

    @Override
    public String getId() {
        return "processor-tracing";
    }

    @Override
    public Completable execute(final HttpExecutionContextInternal ctx) {
        return Completable.fromRunnable(() -> {
            if (ctx.request() != null) {
                final Tracer tracer = ctx.getTracer();
                tracer.deferRootSpanAttribute(AbstractTracingHook.SPAN_REQUEST_ID_ATTR, ctx.request().id());
                tracer.deferRootSpanAttribute(AbstractTracingHook.SPAN_TRANSACTION_ID_ATTR, ctx.request().transactionId());
            }
        });
    }
}
