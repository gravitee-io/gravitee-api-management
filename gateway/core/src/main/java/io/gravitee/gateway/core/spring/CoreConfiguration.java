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
package io.gravitee.gateway.core.spring;

import io.gravitee.gateway.api.Registry;
import io.gravitee.gateway.core.Reactor;
import io.gravitee.gateway.core.http.client.HttpClientFactory;
import io.gravitee.gateway.core.http.client.jetty.JettyHttpClientFactory;
import io.gravitee.gateway.core.reactor.GraviteeReactor;
import io.gravitee.gateway.core.reactor.RouteMatcher;
import io.gravitee.gateway.core.policy.builder.RequestPolicyChainBuilder;
import io.gravitee.gateway.core.policy.builder.ResponsePolicyChainBuilder;
import io.gravitee.gateway.core.registry.FileRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Configuration
public class CoreConfiguration {

    @Bean
    public Reactor reactor() {
        return new GraviteeReactor();
    }

    @Bean
    public Registry registry() {
        return new FileRegistry();
    }

    @Bean
    public RequestPolicyChainBuilder requestPolicyChainBuilder() {
        return new RequestPolicyChainBuilder();
    }

    @Bean
    public ResponsePolicyChainBuilder responsePolicyChainBuilder() {
        return new ResponsePolicyChainBuilder();
    }

    @Bean
    public HttpClientFactory httpClientFactory() {
        return new JettyHttpClientFactory();
    }

    @Bean
    public RouteMatcher routeMatcher(Registry registry) {
        return new RouteMatcher(registry);
    }
}
