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
package io.gravitee.apim.core.zee.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Inbound request to generate a Gravitee resource via Zee.
 *
 * @param resourceType the type of resource to generate (required)
 * @param prompt       natural-language description of the desired resource
 *                     (required, non-blank)
 * @param files        optional uploaded file contents for prompt enrichment
 * @param contextData  optional context (apiId, envId, orgId, etc.)
 */
public record ZeeRequest(ZeeResourceType resourceType, String prompt, List<FileContent> files,
        Map<String, Object> contextData) {
    public ZeeRequest {
        Objects.requireNonNull(resourceType, "resourceType is required");
        Objects.requireNonNull(prompt, "prompt is required");
        if (prompt.isBlank()) {
            throw new IllegalArgumentException("prompt cannot be blank");
        }
        files = files != null ? List.copyOf(files) : List.of();
        contextData = contextData != null ? Map.copyOf(contextData) : Map.of();
    }
}
