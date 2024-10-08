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
package io.gravitee.gateway.reactive.core.context;

import static io.gravitee.gateway.reactive.api.context.ContextAttributes.ATTR_PREFIX;

import java.io.Serial;
import java.util.HashMap;

/**
 * Special {@link java.util.Map} implementation allowing to retrieve attributes prefixed with #ATTR_PREFIX without having to specify it explicitly.
 */
class ContextAttributeMap extends HashMap<String, Object> {

    @Serial
    private static final long serialVersionUID = -8914016743809221307L;

    /**
     * In the most general case, the context will not store more than 12 elements in the Map.
     * Then, the initial capacity must be set to limit size in memory.
     */
    ContextAttributeMap() {
        super(12, 1.0f);
    }

    @Override
    public Object get(Object key) {
        Object value = super.get(key);
        return (value != null) ? value : super.get(ATTR_PREFIX + key);
    }

    @Override
    public boolean containsKey(Object key) {
        return super.containsKey(key) || super.containsKey(ATTR_PREFIX + key);
    }
}
