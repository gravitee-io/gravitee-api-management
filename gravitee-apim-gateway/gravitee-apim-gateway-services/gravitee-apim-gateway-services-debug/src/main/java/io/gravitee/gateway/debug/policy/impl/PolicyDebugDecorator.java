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

import io.gravitee.common.utils.UUID;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.stream.ReadWriteStream;
import io.gravitee.gateway.core.condition.ConditionEvaluator;
import io.gravitee.gateway.debug.reactor.handler.context.DebugExecutionContext;
import io.gravitee.gateway.debug.reactor.handler.context.steps.DebugStep;
import io.gravitee.gateway.debug.reactor.handler.context.steps.DebugStepFactory;
import io.gravitee.gateway.policy.Policy;
import io.gravitee.gateway.policy.PolicyException;
import io.gravitee.gateway.policy.StreamType;
import io.gravitee.gateway.policy.impl.ConditionalExecutablePolicy;
import io.gravitee.policy.api.PolicyChain;

import java.lang.reflect.Method;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class PolicyDebugDecorator implements Policy {

    private final StreamType streamType;
    private final Policy policy;
    private final String uuid;

    public PolicyDebugDecorator(StreamType streamType, Policy policy) {
        this.streamType = streamType;
        this.policy = policy;
        this.uuid = new UUID().randomString();
    }

    @Override
    public String id() {
        return policy.id();
    }

    @Override
    public void execute(PolicyChain chain, ExecutionContext context) throws PolicyException {
        DebugExecutionContext debugContext = (DebugExecutionContext) context;

        DebugStep<?> debugStep = DebugStepFactory.createExecuteDebugStep(policy.id(), streamType, uuid);
        final DebugPolicyChain debugPolicyChain = new DebugPolicyChain(chain, debugStep, debugContext);
        debugContext.beforePolicyExecution(debugStep);

        final Policy policy = this.policy instanceof ConditionalExecutablePolicy ? new DebugConditionalExecutablePolicy((ConditionalExecutablePolicy) this.policy, debugStep) : this.policy;

        try {
            policy.execute(debugPolicyChain, context);
        } catch (Throwable ex) {
            debugStep.error(ex);
            throw ex;
        }
    }

    @Override
    public ReadWriteStream<Buffer> stream(PolicyChain chain, ExecutionContext context) throws PolicyException {
        DebugExecutionContext debugContext = (DebugExecutionContext) context;
        DebugStep<?> debugStep = DebugStepFactory.createStreamDebugStep(policy.id(), streamType, uuid);
        final DebugPolicyChain debugPolicyChain = new DebugStreamablePolicyChain(chain, debugStep, debugContext);

        final Policy policy = this.policy instanceof ConditionalExecutablePolicy ? new DebugConditionalExecutablePolicy((ConditionalExecutablePolicy) this.policy, debugStep) : this.policy;

        try {
            final ReadWriteStream<Buffer> stream = policy.stream(debugPolicyChain, context);

            if (stream == null) {
                return null;
            }

            return new DebugReadWriteStream(debugContext, stream, debugStep);
        } catch (Throwable ex) {
            debugStep.error(ex);
            throw ex;
        }
    }

    @Override
    public boolean isStreamable() {
        return policy.isStreamable();
    }

    @Override
    public boolean isRunnable() {
        return policy.isRunnable();
    }

    class DebugConditionalExecutablePolicy extends ConditionalExecutablePolicy {

        public DebugConditionalExecutablePolicy(ConditionalExecutablePolicy conditionalExecutablePolicy, DebugStep<?> debugStep) {
            super(conditionalExecutablePolicy);
            evaluationFunction = context -> {
                final boolean isConditionTruthy = conditionEvaluator.evaluate(context, condition);
                if (!isConditionTruthy) {
                    debugStep.skipped(condition);
                }
                return isConditionTruthy;
            };
        }

        @Override
        protected boolean evaluateCondition(ExecutionContext context) throws PolicyException {
            final boolean isConditionTruthy = super.evaluateCondition(context);

            return isConditionTruthy;
        }
    }

}
