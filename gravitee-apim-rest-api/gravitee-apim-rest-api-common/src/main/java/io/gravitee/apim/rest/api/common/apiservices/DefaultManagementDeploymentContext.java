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
package io.gravitee.apim.rest.api.common.apiservices;

import io.gravitee.definition.model.v4.Api;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import org.springframework.context.ApplicationContext;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultManagementDeploymentContext implements ManagementDeploymentContext {

    private final Map<Class<?>, Object> components = new ConcurrentHashMap<>();
    private final ApplicationContext applicationContext;

    public DefaultManagementDeploymentContext(@Nonnull Api apiDefinitionV4, @Nonnull ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        components.put(Api.class, apiDefinitionV4);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getComponent(Class<T> clazz) {
        final T component = (T) components.get(clazz);
        if (component != null) {
            return component;
        }
        return applicationContext.getBean(clazz);
    }
}
