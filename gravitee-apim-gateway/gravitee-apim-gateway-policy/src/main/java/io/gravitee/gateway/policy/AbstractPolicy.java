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
package io.gravitee.gateway.policy;

import io.gravitee.gateway.api.ExecutionContext;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.api.Response;
import io.gravitee.policy.api.PolicyChain;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractPolicy implements Policy {

    public void onRequest(Object... args) throws PolicyException {
        ExecutionContext executionContext = getParameterAssignableTo(ExecutionContext.class, args);
        PolicyChain policyChain = getParameterAssignableTo(PolicyChain.class, args);
        Request request = getParameterAssignableTo(Request.class, args);
        Response response = getParameterAssignableTo(Response.class, args);

        this.onRequest(request, response, policyChain, executionContext);
    }

    protected void onRequest(Request request, Response response, PolicyChain policyChain,
                             ExecutionContext executionContext) throws PolicyException {

    }

    public void onResponse(Object... args) throws PolicyException {
        ExecutionContext executionContext = getParameterAssignableTo(ExecutionContext.class, args);
        PolicyChain policyChain = getParameterAssignableTo(PolicyChain.class, args);
        Request request = getParameterAssignableTo(Request.class, args);
        Response response = getParameterAssignableTo(Response.class, args);

        this.onResponse(request, response, policyChain, executionContext);
    }

    protected void onResponse(Request request, Response response, PolicyChain policyChain,
                              ExecutionContext executionContext) throws PolicyException {

    }

    protected <T> T getParameterAssignableTo(Class<T> paramType, Object ... args) {
        for(Object arg: args) {
            if (paramType.isAssignableFrom(arg.getClass())) {
                return (T) arg;
            }
        }

        return null;
    }
}
