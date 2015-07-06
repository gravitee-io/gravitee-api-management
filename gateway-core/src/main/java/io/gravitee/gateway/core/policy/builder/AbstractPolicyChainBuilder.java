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
package io.gravitee.gateway.core.policy.builder;

import io.gravitee.gateway.api.PolicyChain;
import io.gravitee.gateway.api.Request;
import io.gravitee.gateway.core.policy.PolicyChainBuilder;
import io.gravitee.gateway.core.policy.PolicyRegistry;
import io.gravitee.model.Api;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public abstract class AbstractPolicyChainBuilder<T extends PolicyChain> implements PolicyChainBuilder<T, Request> {

    @Autowired
    private Api api;

    @Autowired
    private PolicyRegistry registry;

    public Api getApi() {
        return api;
    }

    public void setApi(Api api) {
        this.api = api;
    }

    public PolicyRegistry getRegistry() {
        return registry;
    }

    public void setRegistry(PolicyRegistry registry) {
        this.registry = registry;
    }
}
