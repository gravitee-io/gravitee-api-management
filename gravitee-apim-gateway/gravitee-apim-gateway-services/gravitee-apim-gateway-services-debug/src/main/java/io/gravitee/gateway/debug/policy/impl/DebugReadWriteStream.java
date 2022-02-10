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
package io.gravitee.gateway.debug.policy.impl;

import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.stream.ReadStream;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.api.stream.WriteStream;
import io.gravitee.gateway.debug.reactor.handler.context.DebugExecutionContext;
import io.gravitee.gateway.debug.reactor.handler.context.steps.DebugStep;

/**
 * A {@link ReadWriteStream} used to write debug content of the policy execution in DebugExecutionContext.
 *
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DebugReadWriteStream implements ReadWriteStream<Buffer> {

    private final ReadWriteStream<Buffer> delegate;
    private final DebugStep<?> debugStep;
    private final DebugExecutionContext context;
    private Buffer outputBuffer;
    private Buffer inputBuffer;

    public DebugReadWriteStream(final DebugExecutionContext context, final ReadWriteStream<Buffer> delegate, DebugStep<?> debugStep) {
        this.context = context;
        this.delegate = delegate;
        this.debugStep = debugStep;
    }

    @Override
    public ReadStream<Buffer> bodyHandler(Handler<Buffer> bodyHandler) {
        delegate.bodyHandler(
            buf -> {
                if (outputBuffer == null) {
                    outputBuffer = Buffer.buffer();
                }
                outputBuffer.appendBuffer(buf);
                // If there is a bodyHandler, then the step is ended here.
                context.afterPolicyExecution(debugStep, inputBuffer, outputBuffer);
                bodyHandler.handle(buf);
            }
        );
        return this;
    }

    @Override
    public ReadStream<Buffer> endHandler(Handler<Void> endHandler) {
        delegate.endHandler(
            avoid -> {
                // If there is no bodyHandler, we end the step here, in the endHandler.
                context.afterPolicyExecution(debugStep, inputBuffer, outputBuffer);
                endHandler.handle(null);
            }
        );
        return this;
    }

    @Override
    public ReadStream<Buffer> pause() {
        delegate.pause();
        return this;
    }

    @Override
    public ReadStream<Buffer> resume() {
        delegate.resume();
        return this;
    }

    @Override
    public WriteStream<Buffer> write(Buffer chunk) {
        if (inputBuffer == null) {
            inputBuffer = Buffer.buffer();
        }
        context.beforePolicyExecution(debugStep);
        inputBuffer.appendBuffer(chunk);
        delegate.write(chunk);
        return this;
    }

    @Override
    public void end() {
        context.beforePolicyExecution(debugStep);
        delegate.end();
    }

    @Override
    public void end(Buffer chunk) {
        delegate.end(chunk);
    }

    @Override
    public WriteStream<Buffer> drainHandler(Handler<Void> drainHandler) {
        delegate.drainHandler(drainHandler);
        return this;
    }

    @Override
    public boolean writeQueueFull() {
        return delegate.writeQueueFull();
    }
}
