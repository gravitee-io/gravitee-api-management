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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Deterministic mock embedding service that derives vectors from a SHA-256 hash of the input text.
 * Useful for testing and development without an external LLM.
 * <p>
 * Vectors are deterministic: the same input always produces the same vector,
 * so semantically similar but differently worded texts will NOT be close in vector space.
 */
public class MockEmbeddingService implements EmbeddingService {

    private final int dims;

    public MockEmbeddingService(int dimensions) {
        this.dims = dimensions;
    }

    @Override
    public float[] embedText(String text) {
        byte[] hash = sha256(text);
        var vector = new float[dims];
        for (int i = 0; i < dims; i++) {
            // cycle through hash bytes to fill all dimensions, normalize to [-1, 1]
            vector[i] = (hash[i % hash.length] & 0xFF) / 127.5f - 1.0f;
        }
        return normalize(vector);
    }

    @Override
    public int dimensions() {
        return dims;
    }

    private static byte[] sha256(String text) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static float[] normalize(float[] v) {
        double norm = 0;
        for (float f : v) {
            norm += f * f;
        }
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < v.length; i++) {
                v[i] /= (float) norm;
            }
        }
        return v;
    }
}
