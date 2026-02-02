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
package io.gravitee.rest.api.service.search;

/**
 * Service for generating text embeddings using a machine learning model.
 * Used for semantic search capabilities.
 *
 * @author GraviteeSource Team
 */
public interface EmbeddingService {
    /**
     * The dimension of the embedding vectors produced by the model.
     * all-MiniLM-L6-v2 produces 384-dimensional vectors.
     */
    int EMBEDDING_DIMENSION = 384;

    /**
     * Generates an embedding vector for the given text.
     *
     * @param text The text to vectorize
     * @return A float array of 384 dimensions representing the semantic meaning of the text
     */
    float[] embed(String text);

    /**
     * Checks if the embedding service is available and properly initialized.
     *
     * @return true if the service can generate embeddings, false otherwise
     */
    boolean isAvailable();

    /**
     * Checks if the service is using the real ONNX model or a fallback mode.
     * When using fallback mode (hash-based embeddings), semantic search won't work correctly.
     *
     * @return true if using the real ML model, false if using fallback mode
     */
    boolean isUsingRealModel();
}
