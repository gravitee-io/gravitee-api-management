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

import io.gravitee.gateway.reactive.api.context.EntityResolver;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Default implementation of {@link EntityResolver} backed by a concurrent map.
 * Populated at reactor startup from the catalog.
 */
public class DefaultEntityResolver implements EntityResolver {

    private record Key(String kind, String name) {}

    private final ConcurrentMap<Key, String> registry = new ConcurrentHashMap<>();

    @Override
    public void register(String kind, String name, String entityId) {
        registry.put(new Key(kind, name), entityId);
    }

    @Override
    public String resolve(String kind, String name) {
        return registry.getOrDefault(new Key(kind, name), name);
    }
}
