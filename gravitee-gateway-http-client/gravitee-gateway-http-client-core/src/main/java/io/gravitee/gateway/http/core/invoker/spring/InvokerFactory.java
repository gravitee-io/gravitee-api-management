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
package io.gravitee.gateway.http.core.invoker.spring;

import io.gravitee.common.spring.factory.AbstractAutowiringFactoryBean;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.LoadBalancer;
import io.gravitee.gateway.api.Invoker;
import io.gravitee.gateway.api.endpoint.EndpointManager;
import io.gravitee.gateway.http.core.invoker.DefaultInvoker;
import io.gravitee.gateway.http.core.invoker.failover.FailoverInvoker;
import io.gravitee.gateway.http.core.invoker.loadbalancer.RandomLoadBalancer;
import io.gravitee.gateway.http.core.invoker.loadbalancer.RoundRobinLoadBalancer;
import io.gravitee.gateway.http.core.invoker.loadbalancer.WeightedRandomLoadBalancer;
import io.gravitee.gateway.http.core.invoker.loadbalancer.WeightedRoundRobinLoadBalancer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class InvokerFactory extends AbstractAutowiringFactoryBean<Invoker> implements ApplicationContextAware {

    @Autowired
    private Api api;

    @Autowired
    private EndpointManager endpointManager;

    private ApplicationContext applicationContext;

    @Override
    public Class<?> getObjectType() {
        return Invoker.class;
    }

    @Override
    protected Invoker doCreateInstance() throws Exception {
        LoadBalancer lb = api.getProxy().getLoadBalancer();
        DefaultInvoker invoker;

        if (lb != null) {
            switch (lb.getType()) {
                case RANDOM:
                    invoker = new RandomLoadBalancer(endpointManager.endpoints());
                    break;
                case WEIGHTED_RANDOM:
                    invoker = new WeightedRandomLoadBalancer(endpointManager.endpoints());
                    break;
                case WEIGHTED_ROUND_ROBIN:
                    invoker = new WeightedRoundRobinLoadBalancer(endpointManager.endpoints());
                    break;
                default:
                    invoker = new RoundRobinLoadBalancer(endpointManager.endpoints());
                    break;
            }
        } else {
            invoker = new RoundRobinLoadBalancer(endpointManager.endpoints());
        }

        if (api.getProxy().failoverEnabled()) {
            applicationContext
                    .getAutowireCapableBeanFactory()
                    .autowireBean(invoker);

            invoker = new FailoverInvoker(invoker);
        }

        return invoker;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        super.setApplicationContext(applicationContext);

        this.applicationContext = applicationContext;
    }
}
