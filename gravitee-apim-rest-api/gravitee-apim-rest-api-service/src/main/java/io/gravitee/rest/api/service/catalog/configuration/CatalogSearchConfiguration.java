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
package io.gravitee.rest.api.service.catalog.configuration;

import io.gravitee.rest.api.service.catalog.embedding.EmbeddingService;
import io.gravitee.rest.api.service.catalog.embedding.MockEmbeddingService;
import io.gravitee.rest.api.service.catalog.embedding.OllamaEmbeddingService;
import io.gravitee.rest.api.service.catalog.search.CatalogSemanticIndexer;
import io.gravitee.rest.api.service.catalog.search.CatalogSemanticSearcher;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for the isolated catalog semantic search pipeline.
 * All beans declared here are independent of the existing {@code SearchEngineConfiguration}.
 */
@Configuration
public class CatalogSearchConfiguration {

    @Value("${catalog.search.data:${gravitee.home:#{systemProperties['java.io.tmpdir']}}/data/catalog_semantic_index}")
    private String catalogIndexPath;

    @Value("${catalog.search.embedding.provider:ollama}")
    private String embeddingProvider;

    /** Default 768 dimensions for Ollama nomic-embed-text. Set to 1536 for OpenAI text-embedding-ada-002. */
    @Value("${catalog.search.embedding.dimensions:768}")
    private int embeddingDimensions;

    @Value("${catalog.search.ollama.url:http://localhost:11434}")
    private String ollamaUrl;

    @Value("${catalog.search.ollama.model:nomic-embed-text}")
    private String ollamaModel;

    @Bean
    public EmbeddingService catalogEmbeddingService() {
        return switch (embeddingProvider.toLowerCase()) {
            case "ollama" -> new OllamaEmbeddingService(ollamaUrl, ollamaModel, embeddingDimensions);
            case "mock" -> new MockEmbeddingService(embeddingDimensions);
            default -> throw new IllegalArgumentException(
                "Unknown catalog embedding provider: " + embeddingProvider + ". Supported: ollama, mock"
            );
        };
    }

    @Bean
    public Directory catalogIndexDirectory() throws IOException {
        Path path = Paths.get(catalogIndexPath);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        return FSDirectory.open(path);
    }

    @Bean
    public CatalogSemanticIndexer catalogSemanticIndexer(
        @Qualifier("catalogIndexDirectory") Directory catalogIndexDirectory,
        @Qualifier("catalogEmbeddingService") EmbeddingService catalogEmbeddingService
    ) throws IOException {
        return new CatalogSemanticIndexer(catalogIndexDirectory, catalogEmbeddingService);
    }

    @Bean
    public CatalogSemanticSearcher catalogSemanticSearcher(
        @Qualifier("catalogIndexDirectory") Directory catalogIndexDirectory,
        @Qualifier("catalogEmbeddingService") EmbeddingService catalogEmbeddingService
    ) {
        return new CatalogSemanticSearcher(catalogIndexDirectory, catalogEmbeddingService);
    }
}
