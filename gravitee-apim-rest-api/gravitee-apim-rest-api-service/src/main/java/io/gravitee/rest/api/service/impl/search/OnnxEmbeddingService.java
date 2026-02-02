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
package io.gravitee.rest.api.service.impl.search;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.rest.api.service.search.EmbeddingService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * ONNX Runtime based embedding service for generating text embeddings.
 * Uses the all-MiniLM-L6-v2 model for generating 384-dimensional embeddings.
 *
 * @author GraviteeSource Team
 */
@Slf4j
@Component
public class OnnxEmbeddingService implements EmbeddingService {

    private static final String MODEL_RESOURCE_PATH = "/models/all-MiniLM-L6-v2.onnx";
    private static final String TOKENIZER_RESOURCE_PATH = "/models/tokenizer.json";
    private static final int MAX_SEQUENCE_LENGTH = 128;

    @Value("${services.semantic-search.enabled:true}")
    private boolean enabled;

    private OrtEnvironment environment;
    private OrtSession session;
    private WordPieceTokenizer tokenizer;
    private boolean available = false;
    private boolean usingRealModel = false;

    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info("Semantic search is disabled");
            return;
        }

        try {
            log.info("Initializing ONNX embedding service...");

            // Initialize ONNX Runtime
            environment = OrtEnvironment.getEnvironment();

            // Load model from resources or external path
            Path modelPath = loadModel();
            if (modelPath != null) {
                session = environment.createSession(modelPath.toString(), new OrtSession.SessionOptions());
                log.info("ONNX model loaded successfully from: {}", modelPath);

                // Load tokenizer vocabulary
                tokenizer = loadTokenizer();
                if (tokenizer != null) {
                    usingRealModel = true;
                    log.info("WordPiece tokenizer loaded with {} vocabulary entries", tokenizer.getVocabSize());
                } else {
                    log.warn("Tokenizer not found. Using fallback tokenizer.");
                    tokenizer = new WordPieceTokenizer();
                }
            } else {
                log.warn("ONNX model not found. Semantic search will use fallback mode.");
                initializeFallbackMode();
                return;
            }

            available = true;
            log.info("ONNX embedding service initialized successfully (real model: {})", usingRealModel);
        } catch (Exception e) {
            log.warn("Failed to initialize ONNX embedding service: {}. Using fallback mode.", e.getMessage());
            initializeFallbackMode();
        }
    }

    private WordPieceTokenizer loadTokenizer() {
        try {
            InputStream tokenizerStream = getClass().getResourceAsStream(TOKENIZER_RESOURCE_PATH);
            if (tokenizerStream != null) {
                String json = new String(tokenizerStream.readAllBytes(), StandardCharsets.UTF_8);
                return WordPieceTokenizer.fromJson(json);
            }
            return null;
        } catch (IOException e) {
            log.error("Error loading tokenizer", e);
            return null;
        }
    }

    private void initializeFallbackMode() {
        // In fallback mode, we use simple hash-based embeddings
        // This allows the system to work without the ML model
        tokenizer = new WordPieceTokenizer();
        available = true;
        log.info("Embedding service running in fallback mode (hash-based embeddings)");
    }

    private Path loadModel() {
        try {
            // First, try to load from classpath resources
            InputStream modelStream = getClass().getResourceAsStream(MODEL_RESOURCE_PATH);
            if (modelStream != null) {
                Path tempFile = Files.createTempFile("embedding-model", ".onnx");
                Files.copy(modelStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
                tempFile.toFile().deleteOnExit();
                return tempFile;
            }

            // Try external path from configuration
            String externalPath = System.getProperty("gravitee.home");
            if (externalPath != null) {
                Path modelFile = Path.of(externalPath, "models", "all-MiniLM-L6-v2.onnx");
                if (Files.exists(modelFile)) {
                    return modelFile;
                }
            }

            return null;
        } catch (IOException e) {
            log.error("Error loading ONNX model", e);
            return null;
        }
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (session != null) {
                session.close();
            }
            if (environment != null) {
                environment.close();
            }
        } catch (OrtException e) {
            log.error("Error closing ONNX session", e);
        }
    }

    @Override
    public float[] embed(String text) {
        if (!available) {
            return new float[EMBEDDING_DIMENSION];
        }

        if (text == null || text.isBlank()) {
            return new float[EMBEDDING_DIMENSION];
        }

        // Use ONNX model if available, otherwise use fallback
        if (session != null && usingRealModel) {
            return embedWithOnnx(text);
        } else {
            return embedWithFallback(text);
        }
    }

    /**
     * Checks if the service is using the real ONNX model or fallback mode.
     */
    public boolean isUsingRealModel() {
        return usingRealModel;
    }

    private float[] embedWithOnnx(String text) {
        try {
            // Tokenize the text
            long[][] tokenIds = tokenizer.tokenize(text, MAX_SEQUENCE_LENGTH);
            long[][] attentionMask = createAttentionMask(tokenIds);
            long[][] tokenTypeIds = createTokenTypeIds(tokenIds);

            // Create input tensors
            Map<String, OnnxTensor> inputs = new HashMap<>();
            inputs.put("input_ids", OnnxTensor.createTensor(environment, tokenIds));
            inputs.put("attention_mask", OnnxTensor.createTensor(environment, attentionMask));
            inputs.put("token_type_ids", OnnxTensor.createTensor(environment, tokenTypeIds));

            // Run inference
            try (OrtSession.Result results = session.run(inputs)) {
                // Get the output (last_hidden_state)
                float[][][] output = (float[][][]) results.get(0).getValue();

                // Mean pooling over sequence length
                float[] embedding = meanPooling(output[0], attentionMask[0]);

                // L2 normalize
                return normalize(embedding);
            }
        } catch (OrtException e) {
            log.error("Error generating embedding with ONNX", e);
            return embedWithFallback(text);
        }
    }

    private float[] embedWithFallback(String text) {
        // Simple hash-based embedding for fallback mode
        // This provides consistent but less semantic embeddings
        float[] embedding = new float[EMBEDDING_DIMENSION];

        String normalizedText = text.toLowerCase().trim();
        String[] words = normalizedText.split("\\s+");

        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            int hash = word.hashCode();

            // Distribute hash across embedding dimensions
            for (int j = 0; j < EMBEDDING_DIMENSION; j++) {
                int mixedHash = hash ^ (j * 31 + i * 17);
                embedding[j] += (float) (Math.sin(mixedHash * 0.0001) * 0.1);
            }
        }

        return normalize(embedding);
    }

    private long[][] createAttentionMask(long[][] tokenIds) {
        long[][] mask = new long[1][tokenIds[0].length];
        for (int i = 0; i < tokenIds[0].length; i++) {
            mask[0][i] = tokenIds[0][i] != 0 ? 1L : 0L;
        }
        return mask;
    }

    private long[][] createTokenTypeIds(long[][] tokenIds) {
        return new long[1][tokenIds[0].length]; // All zeros for single sentence
    }

    private float[] meanPooling(float[][] lastHiddenState, long[] attentionMask) {
        float[] pooled = new float[EMBEDDING_DIMENSION];
        int validTokens = 0;

        for (int i = 0; i < lastHiddenState.length; i++) {
            if (attentionMask[i] == 1) {
                for (int j = 0; j < EMBEDDING_DIMENSION; j++) {
                    pooled[j] += lastHiddenState[i][j];
                }
                validTokens++;
            }
        }

        if (validTokens > 0) {
            for (int j = 0; j < EMBEDDING_DIMENSION; j++) {
                pooled[j] /= validTokens;
            }
        }

        return pooled;
    }

    private float[] normalize(float[] vector) {
        float norm = 0;
        for (float v : vector) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);

        if (norm > 0) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= norm;
            }
        }

        return vector;
    }

    @Override
    public boolean isAvailable() {
        return available && enabled;
    }

    /**
     * WordPiece tokenizer for BERT-style models.
     * Loads vocabulary from tokenizer.json file.
     */
    private static class WordPieceTokenizer {

        private static final long CLS_TOKEN = 101;
        private static final long SEP_TOKEN = 102;
        private static final long PAD_TOKEN = 0;
        private static final long UNK_TOKEN = 100;
        private static final String WORDPIECE_PREFIX = "##";

        private Map<String, Long> vocab = new HashMap<>();
        private boolean hasVocab = false;

        public WordPieceTokenizer() {
            // Default constructor for fallback mode
        }

        public static WordPieceTokenizer fromJson(String json) {
            WordPieceTokenizer tokenizer = new WordPieceTokenizer();
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(json);

                // Parse vocabulary from model.vocab in tokenizer.json
                JsonNode model = root.get("model");
                if (model != null && model.has("vocab")) {
                    JsonNode vocabNode = model.get("vocab");
                    vocabNode.fields().forEachRemaining(entry -> {
                        tokenizer.vocab.put(entry.getKey(), entry.getValue().asLong());
                    });
                    tokenizer.hasVocab = true;
                }
            } catch (Exception e) {
                // Log silently, will use fallback
            }
            return tokenizer;
        }

        public int getVocabSize() {
            return vocab.size();
        }

        public long[][] tokenize(String text, int maxLength) {
            if (!hasVocab) {
                return tokenizeFallback(text, maxLength);
            }

            // Normalize text: lowercase and clean
            String normalized = text.toLowerCase().trim();
            // Basic pre-tokenization: split by whitespace and punctuation
            List<String> words = preTokenize(normalized);

            List<Long> tokens = new ArrayList<>();
            tokens.add(CLS_TOKEN);

            for (String word : words) {
                if (tokens.size() >= maxLength - 1) break;

                // WordPiece tokenization
                List<Long> wordTokens = wordPieceTokenize(word);
                for (Long token : wordTokens) {
                    if (tokens.size() >= maxLength - 1) break;
                    tokens.add(token);
                }
            }

            tokens.add(SEP_TOKEN);

            // Pad to maxLength
            long[] result = new long[maxLength];
            for (int i = 0; i < tokens.size() && i < maxLength; i++) {
                result[i] = tokens.get(i);
            }
            // Rest are PAD_TOKEN (0)

            return new long[][] { result };
        }

        private List<String> preTokenize(String text) {
            List<String> tokens = new ArrayList<>();
            StringBuilder current = new StringBuilder();

            for (char c : text.toCharArray()) {
                if (Character.isWhitespace(c)) {
                    if (current.length() > 0) {
                        tokens.add(current.toString());
                        current = new StringBuilder();
                    }
                } else if (isPunctuation(c)) {
                    if (current.length() > 0) {
                        tokens.add(current.toString());
                        current = new StringBuilder();
                    }
                    tokens.add(String.valueOf(c));
                } else {
                    current.append(c);
                }
            }
            if (current.length() > 0) {
                tokens.add(current.toString());
            }
            return tokens;
        }

        private boolean isPunctuation(char c) {
            return c == '.' || c == ',' || c == '!' || c == '?' || c == ';' || c == ':' ||
                   c == '-' || c == '_' || c == '(' || c == ')' || c == '[' || c == ']' ||
                   c == '{' || c == '}' || c == '"' || c == '\'' || c == '/' || c == '\\';
        }

        private List<Long> wordPieceTokenize(String word) {
            List<Long> tokens = new ArrayList<>();

            if (vocab.containsKey(word)) {
                tokens.add(vocab.get(word));
                return tokens;
            }

            // Try to break into subwords
            int start = 0;
            while (start < word.length()) {
                int end = word.length();
                Long foundToken = null;
                String foundSubword = null;

                while (start < end) {
                    String subword = word.substring(start, end);
                    if (start > 0) {
                        subword = WORDPIECE_PREFIX + subword;
                    }

                    if (vocab.containsKey(subword)) {
                        foundToken = vocab.get(subword);
                        foundSubword = subword;
                        break;
                    }
                    end--;
                }

                if (foundToken != null) {
                    tokens.add(foundToken);
                    start = end;
                } else {
                    // Unknown token for this character
                    tokens.add(UNK_TOKEN);
                    start++;
                }
            }

            return tokens;
        }

        private long[][] tokenizeFallback(String text, int maxLength) {
            String[] words = text.toLowerCase().split("\\s+");
            long[] tokens = new long[maxLength];

            tokens[0] = CLS_TOKEN;

            int pos = 1;
            for (String word : words) {
                if (pos >= maxLength - 1) break;
                // Hash-based fallback
                int hash = Math.abs(word.hashCode()) % 30000;
                tokens[pos++] = hash + 1000;
            }

            if (pos < maxLength) {
                tokens[pos] = SEP_TOKEN;
            }

            return new long[][] { tokens };
        }
    }
}
