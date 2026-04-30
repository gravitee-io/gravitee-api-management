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
package io.gravitee.rest.api.service.catalog.quality;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.search.ApiCriteria;
import io.gravitee.repository.management.api.search.ApiFieldFilter;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiLifecycleState;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
@CustomLog
public class CatalogQualityService {

    private static final String SYSTEM_PROMPT = """
        You are an API catalog quality expert. Your job is to improve API titles and descriptions \
        so they are optimally discoverable by AI agents performing semantic search.

        The catalog uses embedding-based vector search (cosine similarity) and BM25 keyword search. \
        Good titles and descriptions should:
        - Use natural language with spaces (not kebab-case or snake_case)
        - Be specific about what the API does, its domain, and its use cases
        - Include action verbs describing capabilities (e.g. "manages", "retrieves", "authenticates")
        - Title: 10-80 characters, human-readable, specific
        - Description: 100-500 characters, focused on business value and capabilities

        Respond ONLY with valid JSON in this exact format (no markdown, no extra text):
        {
          "suggestedTitle": "...",
          "suggestedDescription": "...",
          "reasoning": "..."
        }
        """;

    private final ApiRepository apiRepository;
    private final TitleDescriptionScoringService scoringService;
    private final CatalogQualityChatClient chatClient;
    private final ObjectMapper objectMapper;

    public CatalogQualityService(
        @Lazy ApiRepository apiRepository,
        TitleDescriptionScoringService scoringService,
        CatalogQualityChatClient chatClient,
        ObjectMapper objectMapper
    ) {
        this.apiRepository = apiRepository;
        this.scoringService = scoringService;
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
    }

    public record CatalogQualityScoreRow(
        String apiId,
        String name,
        String description,
        String definitionVersion,
        String apiType,
        int titleScore,
        int descriptionScore,
        int totalScore,
        List<String> titleIssues,
        List<String> descriptionIssues
    ) {}

    public record CatalogQualitySuggestionRow(String apiId, String suggestedTitle, String suggestedDescription, String reasoning) {}

    public record ScoreResult(
        int titleScore,
        int descriptionScore,
        int totalScore,
        List<String> titleIssues,
        List<String> descriptionIssues
    ) {}

    public ScoreResult score(String title, String description) {
        var safeTitle = title != null ? title : "";
        var safeDesc = description != null ? description : "";
        var titleResult = scoringService.scoreTitle(safeTitle);
        var descResult = scoringService.scoreDescription(safeDesc, safeTitle);
        return new ScoreResult(
            titleResult.score(),
            descResult.score(),
            titleResult.score() + descResult.score(),
            titleResult.issues(),
            descResult.issues()
        );
    }

    public List<CatalogQualityScoreRow> listScores(String environmentId) {
        var criteria = new ApiCriteria.Builder().lifecycleStates(List.of(ApiLifecycleState.PUBLISHED)).environmentId(environmentId).build();

        var scores = new ArrayList<CatalogQualityScoreRow>();

        try (var apiStream = apiRepository.search(criteria, null, ApiFieldFilter.defaultFields())) {
            apiStream.forEach(api -> scores.add(toScoreRow(api)));
        }

        return scores;
    }

    public CatalogQualitySuggestionRow generateSuggestions(String apiId, String environmentId) {
        var apiOpt = tryFindApi(apiId);
        if (apiOpt.isEmpty()) {
            throw new IllegalArgumentException("API not found: " + apiId);
        }
        var api = apiOpt.get();
        if (!environmentId.equals(api.getEnvironmentId())) {
            throw new IllegalArgumentException("API not found in this environment: " + apiId);
        }

        var name = api.getName() != null ? api.getName() : "";
        var description = api.getDescription() != null ? api.getDescription() : "";

        var userPrompt = """
            Analyze this API and suggest improved title and description for catalog discovery:

            Current title: "%s"
            Current description: "%s"
            """.formatted(name, truncate(description, 2000));

        var messages = List.of(
            new CatalogQualityChatClient.ChatMessage("system", SYSTEM_PROMPT),
            new CatalogQualityChatClient.ChatMessage("user", userPrompt)
        );

        var response = chatClient.chat(messages);
        return parseSuggestion(apiId, response);
    }

    private Optional<Api> tryFindApi(String apiId) {
        try {
            return apiRepository.findById(apiId);
        } catch (TechnicalException e) {
            throw new IllegalStateException("Failed to load API " + apiId, e);
        }
    }

    private CatalogQualityScoreRow toScoreRow(Api api) {
        var name = api.getName() != null ? api.getName() : "";
        var description = api.getDescription() != null ? api.getDescription() : "";

        var titleResult = scoringService.scoreTitle(name);
        var descResult = scoringService.scoreDescription(description, name);

        return new CatalogQualityScoreRow(
            api.getId(),
            name,
            description,
            api.getDefinitionVersion() != null ? api.getDefinitionVersion().name() : null,
            api.getType() != null ? api.getType().name() : null,
            titleResult.score(),
            descResult.score(),
            titleResult.score() + descResult.score(),
            titleResult.issues(),
            descResult.issues()
        );
    }

    private CatalogQualitySuggestionRow parseSuggestion(String apiId, String response) {
        try {
            var cleaned = response.trim();
            int jsonStart = cleaned.indexOf('{');
            int jsonEnd = cleaned.lastIndexOf('}');
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                cleaned = cleaned.substring(jsonStart, jsonEnd + 1);
            }

            JsonNode root = objectMapper.readTree(cleaned);

            return new CatalogQualitySuggestionRow(
                apiId,
                getTextOrDefault(root, "suggestedTitle", ""),
                getTextOrDefault(root, "suggestedDescription", ""),
                getTextOrDefault(root, "reasoning", "")
            );
        } catch (Exception e) {
            log.warn("Failed to parse LLM response as JSON for API '{}', returning raw response as reasoning: {}", apiId, e.getMessage());
            return new CatalogQualitySuggestionRow(apiId, "", "", response);
        }
    }

    private String getTextOrDefault(JsonNode root, String field, String defaultValue) {
        var node = root.get(field);
        return (node != null && !node.isNull()) ? node.asText() : defaultValue;
    }

    private String truncate(String text, int maxLen) {
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }
}
