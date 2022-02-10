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
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.core.condition.ConditionEvaluator;
import io.gravitee.gateway.policy.Policy;
import io.gravitee.gateway.policy.PolicyException;
import io.gravitee.policy.api.PolicyChain;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author GraviteeSource Team
 */
public class ConditionalExecutablePolicy implements Policy {

    public static final Logger LOGGER = LoggerFactory.getLogger(ConditionalExecutablePolicy.class);

    protected final Policy policy;
    protected final Function<ExecutionContext, Boolean> evaluationFunction;
    protected final String condition;

    public ConditionalExecutablePolicy(Policy policy, String condition, ConditionEvaluator<String> conditionEvaluator) {
        this.policy = policy;
        this.condition = condition;
        this.evaluationFunction = context -> conditionEvaluator.evaluate(context, condition);
    }

    protected ConditionalExecutablePolicy(ConditionalExecutablePolicy delegate, BiConsumer<String, Boolean> conditionResultConsumer) {
        this.policy = delegate.policy;
        this.condition = delegate.condition;
        this.evaluationFunction =
            context -> {
                Boolean isConditionTruthy = null;
                try {
                    isConditionTruthy = delegate.evaluationFunction.apply(context);
                } finally {
                    conditionResultConsumer.accept(delegate.condition, isConditionTruthy);
                }
                return isConditionTruthy;
            };
    }

    @Override
    public String id() {
        return policy.id();
    }

    @Override
    public void execute(PolicyChain chain, ExecutionContext context) throws PolicyException {
        boolean isConditionTruthy = evaluateCondition(context);

        if (isConditionTruthy) {
            policy.execute(chain, context);
        } else {
            chain.doNext(context.request(), context.response());
        }
    }

    @Override
    public ReadWriteStream<Buffer> stream(PolicyChain chain, ExecutionContext context) throws PolicyException {
        ReadWriteStream<Buffer> stream = policy.stream(chain, context);

        if (stream == null) {
            return null;
        }
        return new ConditionalReadWriteStream(stream, chain, context, this.id(), evaluationFunction);
    }

    @Override
    public boolean isRunnable() {
        return policy.isRunnable();
    }

    @Override
    public boolean isStreamable() {
        return policy.isStreamable();
    }

    private boolean evaluateCondition(ExecutionContext context) throws PolicyException {
        boolean isConditionTruthy;
        try {
            isConditionTruthy = evaluationFunction.apply(context);
        } catch (RuntimeException e) {
            // Catching all RuntimeException to catch those thrown by spring-expression without adding dependency to it
            LOGGER.error("Condition evaluation fails for policy {}", this.id(), e);
            throw new PolicyException("Request failed unintentionally", e);
        }
        return isConditionTruthy;
    }
}
