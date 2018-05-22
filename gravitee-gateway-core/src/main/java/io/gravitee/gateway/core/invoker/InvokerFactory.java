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
package io.gravitee.gateway.core.invoker;

import io.gravitee.common.spring.factory.AbstractAutowiringFactoryBean;
import io.gravitee.definition.model.Api;
import io.gravitee.gateway.api.Invoker;
import io.gravitee.gateway.core.failover.FailoverInvoker;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class InvokerFactory extends AbstractAutowiringFactoryBean<Invoker> {

    @Autowired
    private Api api;

    @Override
    public Class<?> getObjectType() {
        return Invoker.class;
    }

    @Override
    protected Invoker doCreateInstance() throws Exception {
        if (api.getProxy().failoverEnabled()) {
            return new FailoverInvoker();
        }

        return new DefaultInvoker();
    }
}
