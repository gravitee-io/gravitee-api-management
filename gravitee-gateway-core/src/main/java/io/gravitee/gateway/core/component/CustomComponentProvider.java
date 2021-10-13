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
package io.gravitee.gateway.core.component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CustomComponentProvider implements ComponentProvider {

    private final Map<Class<?>, Object> components = new ConcurrentHashMap<>();

    public <T> void add(Class<T> clazz, T component) {
        components.put(clazz, component);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getComponent(Class<T> clazz) {
        return (T) components.get(clazz);
    }
}
