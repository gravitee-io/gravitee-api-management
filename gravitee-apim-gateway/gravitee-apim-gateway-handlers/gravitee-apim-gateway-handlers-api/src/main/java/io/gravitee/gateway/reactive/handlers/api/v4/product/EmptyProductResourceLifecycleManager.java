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
package io.gravitee.gateway.reactive.handlers.api.v4.product;

import io.gravitee.common.component.AbstractLifecycleComponent;
import io.gravitee.gateway.resource.ResourceLifecycleManager;
import io.gravitee.resource.api.Resource;
import io.gravitee.resource.api.ResourceManager;
import java.util.Collection;
import java.util.List;

/**
 * A product runtime with no configured resources still scopes the product plan flow away from the
 * member API {@link ResourceManager}: this manager resolves no resource for any name or type.
 */
final class EmptyProductResourceLifecycleManager extends AbstractLifecycleComponent<ResourceManager> implements ResourceLifecycleManager {

    static final EmptyProductResourceLifecycleManager INSTANCE = new EmptyProductResourceLifecycleManager();

    private EmptyProductResourceLifecycleManager() {}

    @Override
    protected void doStart() {}

    @Override
    protected void doStop() {}

    @Override
    public Object getResource(String name) {
        return null;
    }

    @Override
    public <T> T getResource(Class<T> requiredType) {
        return null;
    }

    @Override
    public <T> T getResource(String name, Class<T> requiredType) {
        return null;
    }

    @Override
    public Class<?> getType(String name) {
        return null;
    }

    @Override
    public boolean containsResource(String name) {
        return false;
    }

    @Override
    public Collection<? extends Resource> getResources() {
        return List.of();
    }
}
