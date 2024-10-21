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
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.stream.ReadStream;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.api.stream.WriteStream;
import io.gravitee.gateway.policy.Policy;
import io.gravitee.gateway.reactive.api.tracing.Tracer;
import io.gravitee.node.api.opentelemetry.Span;
import io.gravitee.node.api.opentelemetry.internal.InternalRequest;
import java.util.Map;

/**
 * A traced {@link ReadWriteStream} used to trace beginning and ending of the policy execution.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class TracingReadWriteStream implements ReadWriteStream<Buffer> {

    private final Tracer tracer;
    private final ReadWriteStream<Buffer> stream;
    private final Policy policy;
    private Span span;

    public TracingReadWriteStream(final ExecutionContext context, final ReadWriteStream<Buffer> stream, final Policy policy) {
        this.policy = policy;
        this.tracer = context.getTracer();
        this.stream = stream;
    }

    @Override
    public ReadStream<Buffer> bodyHandler(Handler<Buffer> bodyHandler) {
        span = tracer.startSpanFrom(new InternalRequest(this.policy.id(), Map.of(TracingPolicy.SPAN_ATTRIBUTE, this.policy.id())));
        stream.bodyHandler(bodyHandler);

        return this;
    }

    @Override
    public ReadStream<Buffer> endHandler(Handler<Void> endHandler) {
        stream.endHandler(endHandler);

        return this;
    }

    @Override
    public ReadStream<Buffer> pause() {
        stream.pause();
        return this;
    }

    @Override
    public ReadStream<Buffer> resume() {
        stream.resume();
        return this;
    }

    @Override
    public WriteStream<Buffer> write(Buffer chunk) {
        stream.write(chunk);
        return this;
    }

    @Override
    public void end() {
        stream.end();
        tracer.end(span);
    }

    @Override
    public void end(Buffer chunk) {
        stream.end(chunk);
        tracer.end(span);
    }

    @Override
    public WriteStream<Buffer> drainHandler(Handler<Void> drainHandler) {
        stream.drainHandler(drainHandler);
        return this;
    }

    @Override
    public boolean writeQueueFull() {
        return stream.writeQueueFull();
    }
}
