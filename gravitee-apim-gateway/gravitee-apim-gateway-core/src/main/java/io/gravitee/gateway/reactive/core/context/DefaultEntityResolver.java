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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.CustomLog;

/**
 * Default implementation of {@link EntityResolver} backed by a concurrent map.
 * Registered entities are keyed by "{kind}:{name}" and resolved back to their entity IDs.
 * When no mapping is found, {@link #resolve(String, String)} returns the name unchanged (passthrough).
 */
@CustomLog
public class DefaultEntityResolver implements EntityResolver {

    private final Map<String, String> registry = new ConcurrentHashMap<>();

    @Override
    public void register(String kind, String name, String entityId) {
        log.debug("Registering entity: kind={}, name={}, entityId={}", kind, name, entityId);
        registry.put(key(kind, name), entityId);
    }

    @Override
    public String resolve(String kind, String name) {
        String resolved = registry.getOrDefault(key(kind, name), name);
        log.debug("Resolving entity: kind={}, name={} -> {}", kind, name, resolved);
        return resolved;
    }

    private static String key(String kind, String name) {
        return kind + ":" + name;
    }
}
