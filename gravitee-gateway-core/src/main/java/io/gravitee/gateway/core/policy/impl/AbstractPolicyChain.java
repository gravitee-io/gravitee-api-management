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
package io.gravitee.gateway.core.policy.impl;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.gateway.api.handler.Handler;
import io.gravitee.gateway.api.policy.PolicyChain;
import io.gravitee.gateway.api.policy.PolicyResult;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public abstract class AbstractPolicyChain implements PolicyChain {

    protected static final PolicyResult SUCCESS_POLICY_CHAIN = new SuccessPolicyResult();

    protected Handler<PolicyResult> resultHandler;

    @Override
    public void failWith(PolicyResult policyResult) {
        resultHandler.handle(policyResult);
    }

    protected void failWith(Throwable throwable) {
        failWith(new PolicyResult() {
            @Override
            public boolean isFailure() {
                return true;
            }

            @Override
            public int httpStatusCode() {
                return HttpStatusCode.INTERNAL_SERVER_ERROR_500;
            }

            @Override
            public String message() {
                return throwable.getMessage();
            }
        });
    }

    public void setResultHandler(Handler<PolicyResult> resultHandler) {
        this.resultHandler = resultHandler;
    }
}
