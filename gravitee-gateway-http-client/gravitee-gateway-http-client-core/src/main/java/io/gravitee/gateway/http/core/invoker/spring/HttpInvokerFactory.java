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

import io.gravitee.definition.model.Api;
import io.gravitee.gateway.api.Invoker;
import io.gravitee.gateway.http.core.common.spring.AbstractAutowiringFactoryBean;
import io.gravitee.gateway.http.core.invoker.DefaultHttpInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 * @author GraviteeSource Team
 */
public class HttpInvokerFactory extends AbstractAutowiringFactoryBean<Invoker> {

    private final Logger LOGGER = LoggerFactory.getLogger(HttpInvokerFactory.class);

    @Autowired
    private Api api;

    @Override
    public Class<?> getObjectType() {
        return Invoker.class;
    }

    @Override
    protected Invoker doCreateInstance() throws Exception {
        return new DefaultHttpInvoker();
    }
}
