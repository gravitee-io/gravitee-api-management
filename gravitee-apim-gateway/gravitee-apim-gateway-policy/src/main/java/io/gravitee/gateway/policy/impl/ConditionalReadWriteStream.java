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
package io.gravitee.gateway.policy.impl;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.stream.ReadStream;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.api.stream.SimpleReadWriteStream;
import io.gravitee.gateway.api.stream.WriteStream;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.PolicyResult;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConditionalReadWriteStream extends SimpleReadWriteStream<Buffer> {

    private static final String GATEWAY_POLICY_INTERNAL_ERROR_KEY = "GATEWAY_POLICY_INTERNAL_ERROR";
    public static final Logger LOGGER = LoggerFactory.getLogger(ConditionalReadWriteStream.class);

    private final String policyId;
    private final ReadWriteStream<Buffer> delegate;
    private final ExecutionContext context;
    private final PolicyChain chain;
    private final Function<ExecutionContext, Boolean> evaluationFunction;

    private Boolean isConditionTruthy;

    public ConditionalReadWriteStream(
        ReadWriteStream<Buffer> delegate,
        PolicyChain chain,
        ExecutionContext context,
        String policyId,
        Function<ExecutionContext, Boolean> evaluationFunction
    ) {
        this.policyId = policyId;
        this.delegate = delegate;
        this.context = context;
        this.chain = chain;
        this.evaluationFunction = evaluationFunction;
    }

    /**
     * Set body handler for both delegate and this instance
     * @param handler will handle the buffer
     * @return this instance with the good body handler
     */
    @Override
    public SimpleReadWriteStream<Buffer> bodyHandler(Handler<Buffer> handler) {
        delegate.bodyHandler(handler);
        return super.bodyHandler(handler);
    }

    /**
     * Set end handler for both delegate and this instance
     * @param handler that will be called at the end
     * @return this instance with the good end handler
     */
    @Override
    public SimpleReadWriteStream<Buffer> endHandler(Handler<Void> handler) {
        delegate.endHandler(handler);
        return super.endHandler(handler);
    }

    /**
     * Depending on condition evaluation, pause delegate or this instance in case of full queue
     * @return this instance of ReadStream<Buffer>
     */
    @Override
    public ReadStream<Buffer> pause() {
        return evaluateCondition() ? delegate.pause() : super.pause();
    }

    /**
     * Depending on condition evaluation, resume delegate or this instance in case of full queue
     * @return this instance of ReadStream<Buffer>
     */
    @Override
    public ReadStream<Buffer> resume() {
        return evaluateCondition() ? delegate.resume() : super.resume();
    }

    /**
     * When condition evaluate to true, then we write on the delegate (targeted policy), else, on this instance
     * @param buffer is the one to write
     * @return this instance to be able to continue the chaining
     */
    @Override
    public SimpleReadWriteStream<Buffer> write(Buffer buffer) {
        if (evaluateCondition()) {
            delegate.write(buffer);
        } else {
            super.write(buffer);
        }
        return this;
    }

    /**
     * When condition evaluate to true, we end the delegate (targeted policy), else, we just end the current instance
     */
    @Override
    public void end() {
        if (evaluateCondition()) {
            delegate.end();
        } else {
            super.end();
        }
    }

    /**
     * When condition evaluate to true, we end the delegate (targeted policy), else, we just end the current instance
     */
    @Override
    public void end(Buffer buffer) {
        if (evaluateCondition()) {
            delegate.end(buffer);
        } else {
            super.end(buffer);
        }
    }

    /**
     * Drain handler for both delegate (targeted policy) and this instance.
     * @param drainHandler to release the pressure on the stream
     * @return this instance as a WriteStream<Buffer>
     */
    @Override
    public WriteStream<Buffer> drainHandler(Handler<Void> drainHandler) {
        delegate.drainHandler(drainHandler);
        return super.drainHandler(drainHandler);
    }

    /**
     * Depending on condition evaluation, get writeQueueFull()
     * @return true if write queue is full
     */
    @Override
    public boolean writeQueueFull() {
        return evaluateCondition() ? delegate.writeQueueFull() : super.writeQueueFull();
    }

    /**
     * Evaluate condition once and keep its value for this instance. This avoid evaluating multiple times the condition.
     * In case of Exception during evaluation, policy chain will fail and exception rethrown
     * @return the evaluation result of the condition.
     */
    private boolean evaluateCondition() {
        if (isConditionTruthy == null) {
            try {
                isConditionTruthy = evaluationFunction.apply(context);
            } catch (RuntimeException e) {
                LOGGER.error("Condition evaluation fails for policy {}", policyId, e);
                chain.streamFailWith(PolicyResult.failure(GATEWAY_POLICY_INTERNAL_ERROR_KEY, "Request failed unintentionally"));
                isConditionTruthy = false;
            }
        }
        return isConditionTruthy;
    }
}
