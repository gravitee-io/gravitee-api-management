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
package io.gravitee.apim.core.zee.domain_service;

import io.gravitee.apim.core.zee.model.ZeeResourceType;
import java.util.Map;

/**
 * Fallback {@link RagContextStrategy} that returns empty context.
 *
 * <p>
 * Used when no specific strategy is registered for the requested resource type.
 * Always returns an empty string from {@link #retrieveContext} without any I/O.
 *
 * @author Derek Burger
 */
public final class NoOpRagStrategy implements RagContextStrategy {

    /** Shared singleton — stateless and thread-safe. */
    public static final NoOpRagStrategy INSTANCE = new NoOpRagStrategy();

    private NoOpRagStrategy() {
    }

    /**
     * Not registered for any specific resource type.
     *
     * @return {@code null}
     */
    @Override
    public ZeeResourceType resourceType() {
        return null;
    }

    /**
     * Returns an empty string — no context retrieval is performed.
     *
     * @return empty string
     */
    @Override
    public String retrieveContext(String envId, String orgId, Map<String, Object> contextData) {
        return "";
    }
}
