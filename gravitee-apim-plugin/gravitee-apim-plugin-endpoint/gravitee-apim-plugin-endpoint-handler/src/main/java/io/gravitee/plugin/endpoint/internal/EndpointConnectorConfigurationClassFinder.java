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
package io.gravitee.plugin.endpoint.internal;

import io.gravitee.gateway.reactive.api.connector.endpoint.EndpointConnectorConfiguration;
import io.gravitee.plugin.core.api.AbstractSingleSubTypesFinder;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;

/**
 * @author GraviteeSource Team
 */
@Slf4j
public class EndpointConnectorConfigurationClassFinder extends AbstractSingleSubTypesFinder<EndpointConnectorConfiguration> {

    public EndpointConnectorConfigurationClassFinder() {
        super(EndpointConnectorConfiguration.class);
    }

    @Override
    public Collection<Class<? extends EndpointConnectorConfiguration>> lookup(Class<?> clazz, ClassLoader classLoader) {
        log.debug("Looking for a configuration class for endpoint {} in package {}", clazz.getName(), clazz.getPackage().getName());
        Collection<Class<? extends EndpointConnectorConfiguration>> configurations = super.lookup(clazz, classLoader);

        if (configurations.isEmpty()) {
            log.info("No endpoint configuration class defined for endpoint {}", clazz.getName());
        }

        return configurations;
    }
}
