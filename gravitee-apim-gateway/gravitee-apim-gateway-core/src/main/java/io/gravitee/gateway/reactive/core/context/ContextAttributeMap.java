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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * Special {@link java.util.Map} implementation allowing to retrieve attributes prefixed with #ATTR_PREFIX without having to specify it explicitly.
 */
public class ContextAttributeMap extends HashMap<String, Object> {

    @Serial
    private static final long serialVersionUID = -8914016743809221307L;

    private boolean enableGraviteePrefix = true;

    private Map<String, Object> fallbackContextAttributeMap;

    /**
     * In the most general case, the context will not store more than 12 elements in the Map.
     * Then, the initial capacity must be set to limit size in memory.
     */
    public ContextAttributeMap() {
        super(12, 1.0f);
    }

    public ContextAttributeMap(boolean enableGraviteePrefix) {
        this();
        this.enableGraviteePrefix = enableGraviteePrefix;
    }

    public ContextAttributeMap(Map<String, Object> fallbackContextAttributeMap) {
        this();
        this.fallbackContextAttributeMap = fallbackContextAttributeMap;
    }

    public ContextAttributeMap(Map<String, Object> fallbackContextAttributeMap, boolean enableGraviteePrefix) {
        this(enableGraviteePrefix);
        this.fallbackContextAttributeMap = fallbackContextAttributeMap;
    }

    @Override
    public Object get(Object key) {
        Object value = super.get(key);
        if (value != null) {
            return value;
        }
        if (enableGraviteePrefix) {
            value = super.get(ATTR_PREFIX + key);
            if (value != null) {
                return value;
            }
        }
        return (fallbackContextAttributeMap != null) ? fallbackContextAttributeMap.get(key) : null;
    }

    @Override
    public boolean containsKey(Object key) {
        return (
            super.containsKey(key) ||
            (enableGraviteePrefix && super.containsKey(ATTR_PREFIX + key)) ||
            (fallbackContextAttributeMap != null && fallbackContextAttributeMap.containsKey(key))
        );
    }

    public Map<String, Object> getFallbackContextAttributeMap() {
        return fallbackContextAttributeMap;
    }
}
