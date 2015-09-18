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
package io.gravitee.gateway.core.reactor.handler;

import io.gravitee.gateway.core.policy.PolicyChainBuilder;
import io.gravitee.gateway.core.policy.PolicyResolver;
import io.gravitee.gateway.core.policy.impl.RequestPolicyChain;
import io.gravitee.gateway.core.policy.impl.ResponsePolicyChain;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public abstract class ContextReactorHandler extends AbstractReactorHandler implements ReactorHandler {

    @Autowired
    private PolicyChainBuilder<RequestPolicyChain> requestPolicyChainBuilder;

    @Autowired
    private PolicyChainBuilder<ResponsePolicyChain> responsePolicyChainBuilder;

    @Autowired
    private PolicyResolver policyResolver;

    public PolicyResolver getPolicyResolver() {
        return policyResolver;
    }

    public PolicyChainBuilder<RequestPolicyChain> getRequestPolicyChainBuilder() {
        return requestPolicyChainBuilder;
    }

    public PolicyChainBuilder<ResponsePolicyChain> getResponsePolicyChainBuilder() {
        return responsePolicyChainBuilder;
    }

    public boolean hasVirtualHost() {
        return (getVirtualHost() != null && !getVirtualHost().isEmpty());
    }

    public abstract String getContextPath();

    public abstract String getVirtualHost();
}
