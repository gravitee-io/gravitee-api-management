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
package io.gravitee.gateway.reactor.handler;

import io.gravitee.common.spring.factory.SpringFactoriesLoader;
import io.gravitee.gateway.reactor.Reactable;
import org.springframework.beans.factory.InitializingBean;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ReactorHandlerFactoryManager extends SpringFactoriesLoader<ReactorHandlerFactory> implements InitializingBean {

    private ReactorHandlerFactory<Reactable> reactorHandlerFactory;

    public ReactorHandler create(Reactable reactable) {
        return reactorHandlerFactory.create(reactable);
    }

    @Override
    protected Class<ReactorHandlerFactory> getObjectType() {
        return ReactorHandlerFactory.class;
    }

    @Override
    public void afterPropertiesSet() {
        // For now, there is only a single reactorHandlerFactory.
        // This must be updated as soon as we are looking to manage more reactable type
        reactorHandlerFactory = getFactoriesInstances().iterator().next();
    }
}
