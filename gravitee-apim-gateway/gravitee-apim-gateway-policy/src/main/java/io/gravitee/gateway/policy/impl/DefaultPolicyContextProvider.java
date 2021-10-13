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
package io.gravitee.gateway.policy.impl;

import io.gravitee.gateway.core.component.ComponentProvider;
import io.gravitee.policy.api.PolicyContextProvider;

/**
 * @author David BRASSELY (david at graviteesource.com)
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DefaultPolicyContextProvider implements PolicyContextProvider {

    private final ComponentProvider componentProvider;

    public DefaultPolicyContextProvider(ComponentProvider componentProvider) {
        this.componentProvider = componentProvider;
    }

    @Override
    public <T> T getNativeProvider() {
        return (T) componentProvider;
    }

    @Override
    public <T> T getComponent(Class<T> componentClass) {
        return componentProvider.getComponent(componentClass);
    }
}
