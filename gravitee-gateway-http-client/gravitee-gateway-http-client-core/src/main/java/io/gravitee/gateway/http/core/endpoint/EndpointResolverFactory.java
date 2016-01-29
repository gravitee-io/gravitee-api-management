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
package io.gravitee.gateway.http.core.endpoint;

import io.gravitee.definition.model.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class EndpointResolverFactory extends AbstractFactoryBean<EndpointResolver> {

    @Autowired
    private Api api;

    @Override
    public Class<?> getObjectType() {
        return EndpointResolver.class;
    }

    @Override
    protected EndpointResolver createInstance() throws Exception {
        // Endpoint resolver can be of three different types :
        // 1_ A simple (and default) single endpoint without expression
        // 2_ An expressed endpoint (using SpEL)
        // 3_ A multiple endpoint definition to apply round-robin

        if (api.getProxy().getEndpoints().size() == 1) {
            return new SingleEndpointResolver(api.getProxy().getEndpoints().iterator().next().getTarget());
        } else {
            return new RoundRobinEndpointResolver(api.getProxy().getEndpoints());
        }
    }
}
