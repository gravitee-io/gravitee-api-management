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

import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.debug.reactor.handler.context.DebugExecutionContext;
import io.gravitee.gateway.debug.reactor.handler.context.steps.DebugStep;
import io.gravitee.policy.api.PolicyChain;
import io.gravitee.policy.api.PolicyResult;

public class DebugPolicyChain implements PolicyChain {

    private final PolicyChain chain;
    private final DebugStep<?> debugStep;
    private final DebugExecutionContext debugContext;

    public DebugPolicyChain(PolicyChain chain, DebugStep<?> debugStep, DebugExecutionContext debugContext) {
        this.chain = chain;
        this.debugStep = debugStep;
        this.debugContext = debugContext;
    }

    @Override
    public void doNext(Request request, Response response) {
        debugContext.afterPolicyExecution(debugStep);
        chain.doNext(request, response);
    }

    // TODO: error management will be done in another ticket
    @Override
    public void failWith(PolicyResult policyResult) {
        chain.failWith(policyResult);
    }

    // TODO: error management will be done in another ticket (don't forget to instantiate it in PolicyDebugDecorator.stream()
    @Override
    public void streamFailWith(PolicyResult policyResult) {
        chain.failWith(policyResult);
    }
}
