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
 * Strategy interface for retrieving RAG (Retrieval-Augmented Generation)
 * context
 * for a specific resource type.
 *
 * <p>
 * Each implementation retrieves deterministic context (existing resources,
 * available
 * plugins, etc.) that gets injected into the LLM prompt to improve generation
 * quality.
 *
 * <p>
 * Implementations must be safe to call concurrently and must never throw —
 * graceful
 * degradation (returning partial or empty context) is preferred over
 * propagating errors.
 *
 * @author Derek Burger
 */
public interface RagContextStrategy {
    /**
     * The resource type this strategy handles.
     *
     * @return the resource type, or {@code null} for fallback/no-op strategies
     */
    ZeeResourceType resourceType();

    /**
     * Retrieve context relevant to the given environment and request data.
     *
     * @param envId       the environment identifier (tenant scoping)
     * @param orgId       the organization identifier (tenant scoping)
     * @param contextData free-form map of request-specific parameters
     *                    (e.g. {@code apiId} for flow context)
     * @return formatted markdown string with context sections, or empty string if
     *         no context is available
     */
    String retrieveContext(String envId, String orgId, Map<String, Object> contextData);
}
