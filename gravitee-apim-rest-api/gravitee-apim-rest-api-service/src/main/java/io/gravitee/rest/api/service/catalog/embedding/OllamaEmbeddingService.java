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
package io.gravitee.rest.api.service.catalog.embedding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import lombok.CustomLog;

/**
 * Embedding service backed by a local Ollama instance.
 * Calls {@code POST /api/embed} with the configured model (e.g. {@code nomic-embed-text}, 768 dimensions).
 */
@CustomLog
public class OllamaEmbeddingService implements EmbeddingService {

    private final String baseUrl;
    private final String model;
    private final int dims;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OllamaEmbeddingService(String baseUrl, String model, int dimensions) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.model = model;
        this.dims = dimensions;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public float[] embedText(String text) {
        try {
            var body = objectMapper.writeValueAsString(Map.of("model", model, "input", text));

            var request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/embed"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(30))
                .build();

            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IllegalStateException(
                    "Ollama embed request failed (HTTP " +
                        response.statusCode() +
                        "): " +
                        response.body().substring(0, Math.min(500, response.body().length()))
                );
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode embeddings = root.get("embeddings");
            if (embeddings == null || !embeddings.isArray() || embeddings.isEmpty()) {
                throw new IllegalStateException("Ollama response missing 'embeddings' array");
            }

            JsonNode firstEmbedding = embeddings.get(0);
            var vector = new float[firstEmbedding.size()];
            for (int i = 0; i < firstEmbedding.size(); i++) {
                vector[i] = (float) firstEmbedding.get(i).asDouble();
            }

            if (vector.length != dims) {
                log.warn("Ollama returned {} dimensions but {} were configured. Using actual size.", vector.length, dims);
            }

            return vector;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to obtain embedding from Ollama at " + baseUrl, e);
        }
    }

    @Override
    public int dimensions() {
        return dims;
    }
}
