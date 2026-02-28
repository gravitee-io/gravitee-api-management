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

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

/**
 * Domain service for LLM-based structured resource generation.
 *
 * <p>
 * Generates schema-valid Gravitee resource definitions from natural language
 * prompts using the {@code json-schema-llm} roundtrip pipeline. The
 * implementation
 * handles provider communication, schema conversion, and rehydration.
 *
 * @author Derek Burger
 */
public interface LlmEngineService {
    /**
     * Generate a structured resource using LLM structured output.
     *
     * @param prompt        the enriched prompt (user + RAG + file context)
     * @param componentName the SDK component name (e.g., "Flow", "Plan",
     *                      "v2/Proxy")
     * @return the generation result with rehydrated data
     * @throws IllegalArgumentException if componentName is not registered
     */
    LlmGenerationResult generate(String prompt, String componentName);

    /**
     * Result of an LLM structured output generation.
     *
     * @param data             rehydrated JSON in the original Gravitee schema shape
     * @param valid            whether the rehydrated data passes schema validation
     * @param tokensUsed       approximate token usage, or {@code -1} if unavailable
     * @param warnings         non-fatal warnings from the rehydration pipeline
     * @param validationErrors schema validation errors (empty when {@code valid} is
     *                         true)
     */
    record LlmGenerationResult(JsonNode data, boolean valid, int tokensUsed, List<String> warnings, List<String> validationErrors) {}
}
