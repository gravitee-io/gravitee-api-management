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

import static io.gravitee.gateway.reactive.api.context.InternalContextAttributes.ATTR_INTERNAL_TRACING_ROOT_SPAN;

import io.gravitee.gateway.reactive.core.context.HttpExecutionContextInternal;
import io.gravitee.gateway.reactive.core.processor.Processor;
import io.gravitee.gateway.reactive.core.tracing.AbstractTracingHook;
import io.gravitee.node.api.opentelemetry.Span;
import io.reactivex.rxjava3.core.Completable;

/**
 * Enriches the root OTel span with Gravitee request identifiers once they are available.
 *
 * <p>The root span is created in {@code DefaultHttpRequestDispatcher.doOnSubscribe()} and stored
 * under {@code ATTR_INTERNAL_TRACING_ROOT_SPAN} before the pre-processor chain runs. At that point
 * {@code transactionId} has not yet been assigned by
 * {@link io.gravitee.gateway.reactive.reactor.processor.transaction.TransactionPreProcessor}.
 * This processor runs immediately after {@code TransactionPreProcessor}, so both
 * {@code gravitee.request.id} and {@code gravitee.transaction.id} are guaranteed to be set.
 *
 * <p>This processor is only registered when OTel traces are enabled. When tracing is disabled the
 * root span is never stored in context, so this processor would be a no-op regardless.
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
            Span rootSpan = ctx.getInternalAttribute(ATTR_INTERNAL_TRACING_ROOT_SPAN);
            if (rootSpan != null && ctx.request() != null) {
                rootSpan.withAttribute(AbstractTracingHook.SPAN_REQUEST_ID_ATTR, ctx.request().id());
                if (ctx.request().transactionId() != null) {
                    rootSpan.withAttribute(AbstractTracingHook.SPAN_TRANSACTION_ID_ATTR, ctx.request().transactionId());
                }
            }
        });
    }
}
