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
import io.gravitee.gateway.debug.reactor.handler.context.DebugExecutionContext;
import io.gravitee.gateway.debug.reactor.handler.context.steps.DebugStep;
import io.gravitee.gateway.debug.reactor.handler.context.steps.DebugStepFactory;
import io.gravitee.gateway.policy.Policy;
import io.gravitee.gateway.policy.PolicyException;
import io.gravitee.gateway.policy.StreamType;
import io.gravitee.policy.api.PolicyChain;

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
        debugContext.beforePolicyExecution(debugStep);
        policy.execute(new DebugPolicyChain(chain, debugStep, debugContext), context);
    }

    @Override
    public ReadWriteStream<Buffer> stream(PolicyChain chain, ExecutionContext context) throws PolicyException {
        DebugExecutionContext debugContext = (DebugExecutionContext) context;

        final ReadWriteStream<Buffer> stream = policy.stream(chain, context);

        if (stream == null) {
            return null;
        }

        DebugStep<?> debugStep = DebugStepFactory.createStreamDebugStep(policy.id(), streamType, uuid);
        return new DebugReadWriteStream(debugContext, stream, debugStep);
    }

    @Override
    public boolean isStreamable() {
        return policy.isStreamable();
    }

    @Override
    public boolean isRunnable() {
        return policy.isRunnable();
    }
}
