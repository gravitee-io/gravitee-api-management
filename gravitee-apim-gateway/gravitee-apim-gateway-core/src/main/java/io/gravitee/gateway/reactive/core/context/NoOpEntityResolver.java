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
import lombok.CustomLog;

/**
 * No-op {@link EntityResolver} used as default when no resolver has been explicitly set on the execution context.
 * All {@link #resolve} calls return the name unchanged; {@link #register} calls are ignored.
 */
@CustomLog
public class NoOpEntityResolver implements EntityResolver {

    @Override
    public void register(String kind, String name, String entityId) {
        log.debug("Entity registration ignored (no resolver configured): kind={}, name={}, entityId={}", kind, name, entityId);
    }

    @Override
    public String resolve(String kind, String name) {
        log.debug("Entity resolution passthrough (no resolver configured): kind={}, name={} -> {}", kind, name, name);
        return name;
    }
}
