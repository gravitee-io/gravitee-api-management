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
import io.gravitee.gateway.core.condition.ExpressionLanguageStringConditionEvaluator;
import io.gravitee.gateway.policy.PolicyException;
import io.gravitee.policy.api.PolicyChain;
import java.lang.reflect.Method;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author GraviteeSource Team
 */
public class ConditionalExecutablePolicy extends ExecutablePolicy {

    public static final Logger LOGGER = LoggerFactory.getLogger(ConditionalExecutablePolicy.class);

    protected Function<ExecutionContext, Boolean> evaluationFunction;
    private final Object policy;
    private Method headMethod;
    private Method streamMethod;
    protected String condition;
    protected ConditionEvaluator<String> conditionEvaluator;

    public ConditionalExecutablePolicy(
        String id,
        Object policy,
        Method headMethod,
        Method streamMethod,
        String condition,
        ConditionEvaluator<String> conditionEvaluator
    ) {
        super(id, policy, headMethod, streamMethod);
        this.policy = policy;
        this.headMethod = headMethod;
        this.streamMethod = streamMethod;
        this.condition = condition;
        this.conditionEvaluator = conditionEvaluator;
        this.evaluationFunction = context -> conditionEvaluator.evaluate(context, condition);
    }

    protected ConditionalExecutablePolicy(ConditionalExecutablePolicy delegate) {
        this(delegate.id(), delegate.policy, delegate.headMethod, delegate.streamMethod, delegate.condition, delegate.conditionEvaluator);
    }

    @Override
    public void execute(PolicyChain chain, ExecutionContext context) throws PolicyException {
        boolean isConditionTruthy = evaluateCondition(context);

        if (isConditionTruthy) {
            super.execute(chain, context);
        } else {
            chain.doNext(context.request(), context.response());
        }
    }

    @Override
    public ReadWriteStream<Buffer> stream(PolicyChain chain, ExecutionContext context) throws PolicyException {
        ReadWriteStream<Buffer> stream = super.stream(chain, context);

        if (stream == null) {
            return null;
        }
        return new ConditionalReadWriteStream(stream, chain, context, this.id(), evaluationFunction);
    }

    protected boolean evaluateCondition(ExecutionContext context) throws PolicyException {
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
