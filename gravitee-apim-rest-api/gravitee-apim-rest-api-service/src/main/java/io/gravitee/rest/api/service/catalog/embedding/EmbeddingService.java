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

/**
 * Abstraction over text-to-vector embedding providers.
 * <p>
 * Swap implementations to target different backends (Ollama, OpenAI, mock, etc.).
 * The vector dimension returned by {@link #embedText(String)} must always equal {@link #dimensions()}.
 */
public interface EmbeddingService {
    /**
     * Convert a text string into a dense floating-point vector.
     *
     * @param text the input text to embed
     * @return a float array whose length equals {@link #dimensions()}
     */
    float[] embedText(String text);

    /**
     * @return the number of dimensions produced by this embedding model
     */
    int dimensions();
}
