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

import com.google.common.base.Throwables;
import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.stream.BufferedReadWriteStream;
import io.gravitee.gateway.core.processor.ProcessorFailure;
import io.gravitee.gateway.core.processor.StreamableProcessor;
import io.gravitee.gateway.policy.Policy;
import io.gravitee.gateway.policy.PolicyChainException;
import io.gravitee.gateway.policy.impl.processor.PolicyChainProcessorFailure;
import io.gravitee.policy.api.PolicyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class PolicyChain extends BufferedReadWriteStream
        implements io.gravitee.policy.api.PolicyChain, StreamableProcessor<ExecutionContext, Buffer> {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected Handler<ExecutionContext> resultHandler;
    protected Handler<ProcessorFailure> errorHandler;
    private Handler<ProcessorFailure> streamErrorHandler;
    protected final List<Policy> policies;
    private final Iterator<Policy> policyIterator;
    protected final ExecutionContext executionContext;

    protected PolicyChain(List<Policy> policies, final ExecutionContext executionContext) {
        Objects.requireNonNull(policies, "Policies must not be null");
        Objects.requireNonNull(executionContext, "ExecutionContext must not be null");

        this.policies = policies;
        this.executionContext = executionContext;

        policyIterator = iterator();
    }

    @Override
    public void doNext(final Request request, final Response response) {
        if (policyIterator.hasNext()) {
            Policy policy = policyIterator.next();
            try {
                if (policy.isRunnable()) {
                    execute(
                            policy,
                            this,
                            executionContext.request(),
                            executionContext.response(),
                            executionContext);
                } else {
                    doNext(executionContext.request(), executionContext.response());
                }
            } catch (Exception ex) {
                request.metrics().setMessage("An error occurs in policy[" + policy.id()+"] error["+Throwables.getStackTraceAsString(ex)+"]");
                if (errorHandler != null) {
                    errorHandler.handle(new PolicyChainProcessorFailure(PolicyResult.failure(ex.getMessage())));
                }
            }
        } else {
            resultHandler.handle(executionContext);
        }
    }

    @Override
    public void failWith(PolicyResult policyResult) {
        errorHandler.handle(new PolicyChainProcessorFailure(policyResult));
    }

    @Override
    public void streamFailWith(PolicyResult policyResult) {
        streamErrorHandler.handle(new PolicyChainProcessorFailure(policyResult));
    }

    @Override
    public void handle(ExecutionContext context) {
        doNext(context.request(), context.response());
    }

    @Override
    public StreamableProcessor<ExecutionContext, Buffer> handler(Handler<ExecutionContext> handler) {
        this.resultHandler = handler;
        return this;
    }

    @Override
    public StreamableProcessor<ExecutionContext, Buffer> errorHandler(Handler<ProcessorFailure> handler) {
        this.errorHandler = handler;
        return this;
    }

    @Override
    public StreamableProcessor<ExecutionContext, Buffer> streamErrorHandler(Handler<ProcessorFailure> handler) {
        this.streamErrorHandler = handler;
        return this;
    }

    @Override
    public StreamableProcessor<ExecutionContext, Buffer> exitHandler(Handler<Void> handler) {
        // Nothing to do here, we are never exiting from policy chain without a processor failure.
        return this;
    }

    protected abstract void execute(Policy policy, Object ... args) throws PolicyChainException;
    protected abstract Iterator<Policy> iterator();
}
