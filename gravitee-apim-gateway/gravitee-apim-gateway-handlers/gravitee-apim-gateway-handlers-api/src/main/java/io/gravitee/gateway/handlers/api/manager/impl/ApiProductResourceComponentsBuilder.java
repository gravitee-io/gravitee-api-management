/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.handlers.api.manager.impl;

import io.gravitee.gateway.handlers.api.ReactableApiProduct;
import io.gravitee.gateway.reactive.handlers.common.AbstractReactorFactory;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.handler.ReactorHandler;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import java.util.List;
import org.springframework.context.ApplicationContext;

class ApiProductResourceComponentsBuilder extends AbstractReactorFactory<ReactableApiProduct> {

    ApiProductResourceComponentsBuilder(ApplicationContext applicationContext) {
        super(applicationContext);
    }

    ResourceLifecycleManager createResourceManager(ReactableApiProduct product) {
        String environmentId = product.getEnvironmentId() == null ? "" : product.getEnvironmentId();
        ReactableComponents components = createComponents(
            product,
            ReactableApiProduct.class,
            environmentTemplateVariableProviders(environmentId, List.of())
        );
        return components.resourceLifecycleManager();
    }

    @Override
    public boolean support(Class<? extends Reactable> clazz) {
        return ReactableApiProduct.class.isAssignableFrom(clazz);
    }

    @Override
    public boolean canCreate(ReactableApiProduct apiProduct) {
        return true;
    }

    @Override
    public ReactorHandler create(ReactableApiProduct apiProduct) {
        throw new UnsupportedOperationException();
    }
}
