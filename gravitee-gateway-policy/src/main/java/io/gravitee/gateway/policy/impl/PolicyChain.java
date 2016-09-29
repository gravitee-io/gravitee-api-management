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
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.stream.BufferedReadWriteStream;
import io.gravitee.gateway.policy.Policy;
import io.gravitee.policy.api.PolicyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class PolicyChain extends BufferedReadWriteStream implements io.gravitee.policy.api.PolicyChain {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    protected static final PolicyResult SUCCESS_POLICY_CHAIN = new SuccessPolicyResult();

    protected Handler<PolicyResult> resultHandler;
    protected final List<Policy> policies;
    protected final Iterator<Policy> iterator;
    protected final ExecutionContext executionContext;

    protected PolicyChain(List<Policy> policies, final ExecutionContext executionContext) {
        Objects.requireNonNull(policies, "Policies must not be null");
        Objects.requireNonNull(executionContext, "ExecutionContext must not be null");

        this.policies = policies;
        this.executionContext = executionContext;

        iterator = iterator();
    }

    @Override
    public void doNext(final Request request, final Response response) {
        if (iterator.hasNext()) {
            Policy policy = iterator.next();
            try {
                if (policy.isRunnable()) {
                    execute(policy, request, response, this, executionContext);
                } else {
                    doNext(request, response);
                }
            } catch (Exception ex) {
                LOGGER.error("Unexpected error while running policy {}", policy, ex);
                failWith(ex);
            }
        } else {
            resultHandler.handle(SUCCESS_POLICY_CHAIN);
        }
    }

    @Override
    public void failWith(PolicyResult policyResult) {
        resultHandler.handle(policyResult);
    }

    protected void failWith(Throwable throwable) {
        failWith(PolicyResult.failure(convertStackTrace(throwable)));
    }

    public void setResultHandler(Handler<PolicyResult> resultHandler) {
        this.resultHandler = resultHandler;
    }

    private String convertStackTrace(Throwable throwable) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }

    protected abstract void execute(Policy policy, Object ... args) throws Exception;
    protected abstract Iterator<Policy> iterator();
}
