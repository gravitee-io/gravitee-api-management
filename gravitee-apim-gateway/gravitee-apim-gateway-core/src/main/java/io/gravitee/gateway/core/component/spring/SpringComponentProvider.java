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
package io.gravitee.gateway.core.component.spring;

import io.gravitee.gateway.core.component.ComponentProvider;
import org.springframework.context.ApplicationContext;

/**
 * A {@link ComponentProvider} based on Spring which allows to gain access to all the beans managed by the Spring
 * {@link ApplicationContext}.
 *
 * TODO: How we could we limit the scope of components that could be retrieved by calling the <code>getComponent</code>
 * method.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SpringComponentProvider implements ComponentProvider {

    private final ApplicationContext applicationContext;

    public SpringComponentProvider(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public <T> T getComponent(Class<T> clazz) {
        return applicationContext.getBean(clazz);
    }
}
