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
package io.gravitee.apim.core.zee.usecase;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.zee.domain_service.LlmEngineService;
import io.gravitee.apim.core.zee.domain_service.NoOpRagStrategy;
import io.gravitee.apim.core.zee.domain_service.RagContextStrategy;
import io.gravitee.apim.core.zee.model.FileContent;
import io.gravitee.apim.core.zee.model.ZeeMetadata;
import io.gravitee.apim.core.zee.model.ZeeRequest;
import io.gravitee.apim.core.zee.model.ZeeResourceType;
import io.gravitee.apim.core.zee.model.ZeeResult;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Orchestrates the full Zee generation pipeline:
 * <ol>
 * <li>Resolve component name from resource type</li>
 * <li>Gather RAG context (tenant-scoped, with graceful degradation)</li>
 * <li>Build enriched prompt with RAG context, uploaded files, and user
 * prompt</li>
 * <li>Invoke LLM structured output generation</li>
 * <li>Return domain result with generation metadata</li>
 * </ol>
 *
 * <p>
 * This class lives in the core layer and has zero infrastructure/Spring
 * dependencies.
 * All collaborators are injected via constructor.
 *
 * @author Derek Burger
 */
@Slf4j
@UseCase
public class GenerateResourceUseCase {

    private static final String MODEL_IDENTIFIER = "gpt-4o-mini";

    private final LlmEngineService llmEngineService;

    /**
     * Map from resource type → responsible RAG strategy.
     * NoOpRagStrategy (resourceType == null) is excluded — it is used as the
     * fallback.
     */
    private final Map<ZeeResourceType, RagContextStrategy> ragStrategies;

    /**
     * @param llmEngineService the LLM engine (core domain service interface)
     * @param strategies       all {@link RagContextStrategy} beans; NoOpRagStrategy
     *                         (resourceType == null) is automatically excluded from
     *                         the map
     */
    public GenerateResourceUseCase(LlmEngineService llmEngineService, List<RagContextStrategy> strategies) {
        this.llmEngineService = llmEngineService;
        this.ragStrategies = strategies
                .stream()
                .filter(s -> s.resourceType() != null)
                .collect(
                        Collectors.toUnmodifiableMap(RagContextStrategy::resourceType, Function.identity(),
                                (first, second) -> {
                                    log.warn(
                                            "Duplicate RagContextStrategy for resourceType={}; keeping first ({})",
                                            first.resourceType(),
                                            first.getClass().getSimpleName());
                                    return first;
                                }));
    }

    /**
     * Execute the Zee generation pipeline.
     *
     * @param request the inbound generation request
     * @param envId   the environment identifier for tenant-scoped RAG
     * @param orgId   the organisation identifier for tenant-scoped RAG
     * @return the generation result including the rehydrated resource and metadata
     */
    public ZeeResult execute(ZeeRequest request, String envId, String orgId) {
        var componentName = request.resourceType().componentName();

        log.info(
                "Zee generation request received: resourceType={}, component={}, promptLength={}, files={}, hasCurrentState={}",
                request.resourceType(), componentName, request.prompt().length(),
                request.files().size(), request.contextData().containsKey("currentState"));
        log.debug("Zee user prompt: {}", request.prompt());

        var ragContext = retrieveRagContext(request, envId, orgId);
        var prompt = buildPrompt(request.prompt(), ragContext, request.files(), request.contextData());

        log.info("Zee enriched prompt built: {} chars, hasRag={}", prompt.length(), !ragContext.isBlank());
        log.debug("Zee enriched prompt:\n{}", prompt);

        var llmResult = llmEngineService.generate(prompt, componentName);

        log.info("Zee LLM response received: tokensUsed={}, errors={}, warnings={}",
                llmResult.tokensUsed(), llmResult.errors().size(), llmResult.warnings().size());
        log.debug("Zee LLM result data: {}", llmResult.data());

        var result = new ZeeResult(
                request.resourceType(),
                llmResult.data(),
                new ZeeMetadata(MODEL_IDENTIFIER, llmResult.tokensUsed(), !ragContext.isBlank()));

        log.info("Zee generation complete: resourceType={}, tokensUsed={}, hasRag={}",
                result.resourceType(), result.metadata().tokensUsed(), result.metadata().usedRag());

        return result;
    }

    // ── private helpers ──────────────────────────────────────────────────────

    private String retrieveRagContext(ZeeRequest request, String envId, String orgId) {
        var strategy = ragStrategies.getOrDefault(request.resourceType(), NoOpRagStrategy.INSTANCE);
        try {
            var ctx = strategy.retrieveContext(envId, orgId, request.contextData());
            return ctx != null ? ctx : "";
        } catch (Exception e) {
            log.warn(
                    "RAG context retrieval failed for resourceType={} envId={}; continuing without RAG context",
                    request.resourceType(),
                    envId,
                    e);
            return "";
        }
    }

    /**
     * Assembles the enriched prompt in sections: system preamble → current state
     * (if updating) → RAG context (if non-empty) → uploaded files (if any) →
     * user request.
     */
    private String buildPrompt(String userPrompt, String ragContext, List<FileContent> files,
            Map<String, Object> contextData) {
        var sb = new StringBuilder();
        sb.append("You are Zee, an expert Gravitee APIM assistant. ");

        var currentState = contextData.get("currentState");
        if (currentState != null) {
            // Update mode — LLM should preserve existing fields
            sb.append("You are updating an existing Gravitee resource. ");
            sb.append("Only alter data required to fulfill the user's request. ");
            sb.append("Preserve all existing field values unless the user explicitly asks to change them.\n\n");
            sb.append("NOTE: Users may use informal names for fields. Common aliases:\n");
            sb.append("- 'labels' and 'tags' refer to the same field (use 'tags' in output)\n");
            sb.append("- 'version' refers to 'apiVersion'\n\n");
            sb.append("## Current Resource State\n");
            try {
                var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                sb.append("```json\n").append(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(currentState))
                        .append("\n```\n\n");
            } catch (Exception e) {
                sb.append("```json\n").append(currentState).append("\n```\n\n");
            }
        } else {
            // Create mode — LLM generates from scratch
            sb.append("Generate Gravitee API definition resources based on the user's request. ");
            sb.append("Be precise and follow Gravitee conventions.\n\n");
        }

        if (!ragContext.isBlank()) {
            sb.append("## Existing Configuration Context\n").append(ragContext).append("\n\n");
        }

        if (files != null && !files.isEmpty()) {
            sb.append("## Uploaded Files\n");
            for (var file : files) {
                sb.append("### ").append(file.filename()).append("\n");
                sb.append(file.content()).append("\n\n");
            }
        }

        sb.append("## User Request\n").append(userPrompt);
        return sb.toString();
    }
}
