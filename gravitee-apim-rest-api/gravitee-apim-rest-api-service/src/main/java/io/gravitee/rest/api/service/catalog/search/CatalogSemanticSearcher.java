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
package io.gravitee.rest.api.service.catalog.search;

import static io.gravitee.rest.api.service.catalog.search.CatalogSemanticIndexer.*;

import io.gravitee.rest.api.service.catalog.embedding.EmbeddingService;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.CustomLog;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.KnnFloatVectorQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;

/**
 * Searches the dedicated catalog semantic index.
 * <p>
 * Two search strategies are available:
 * <ul>
 *   <li>{@link #semanticSearch} — pure KNN vector search using cosine similarity</li>
 *   <li>{@link #hybridSearch} — combines KNN with BM25 keyword matching on title + description</li>
 * </ul>
 * <p>
 * Completely isolated from the existing {@code SearchEngineServiceImpl} infrastructure.
 */
@CustomLog
public class CatalogSemanticSearcher implements Closeable {

    private final Directory directory;
    private final EmbeddingService embeddingService;

    public CatalogSemanticSearcher(Directory directory, EmbeddingService embeddingService) {
        this.directory = directory;
        this.embeddingService = embeddingService;
    }

    /**
     * Pure semantic (vector) search: embed the intent and find the k nearest neighbors.
     */
    public List<ScoredCatalogItem> semanticSearch(String intent, int k) throws IOException {
        float[] queryVector = embeddingService.embedText(intent);
        var knnQuery = new KnnFloatVectorQuery(FIELD_EMBEDDING, queryVector, k);

        try (var reader = DirectoryReader.open(directory)) {
            var searcher = new IndexSearcher(reader);
            TopDocs topDocs = searcher.search(knnQuery, k);
            return toScoredItems(searcher, topDocs);
        }
    }

    /**
     * Hybrid search: combines KNN vector similarity with BM25 keyword matching.
     * <p>
     * Both the KNN query and the BM25 query are added as SHOULD clauses in a
     * {@link BooleanQuery}. Lucene merges their scores, so documents that match
     * on both vectors AND keywords rank higher than those matching on only one signal.
     */
    public List<ScoredCatalogItem> hybridSearch(String intent, int k) throws IOException {
        float[] queryVector = embeddingService.embedText(intent);

        // Vector similarity component
        var knnQuery = new KnnFloatVectorQuery(FIELD_EMBEDDING, queryVector, k);

        // BM25 keyword component across title and description
        var keywordQuery = buildKeywordQuery(intent);

        // Combine both signals — SHOULD means either can match, scores are additive
        var hybridQuery = new BooleanQuery.Builder()
            .add(knnQuery, BooleanClause.Occur.SHOULD)
            .add(keywordQuery, BooleanClause.Occur.SHOULD)
            .build();

        try (var reader = DirectoryReader.open(directory)) {
            var searcher = new IndexSearcher(reader);
            TopDocs topDocs = searcher.search(hybridQuery, k);
            return toScoredItems(searcher, topDocs);
        }
    }

    @Override
    public void close() throws IOException {
        directory.close();
    }

    /**
     * Build a BM25 keyword query that searches the intent terms across title and description fields.
     */
    private BooleanQuery buildKeywordQuery(String intent) {
        var builder = new BooleanQuery.Builder();

        // Tokenize the intent into words and search across both text fields
        String[] terms = intent.toLowerCase().split("\\s+");
        for (String term : terms) {
            var termQuery = new BooleanQuery.Builder()
                .add(
                    new org.apache.lucene.search.TermQuery(new org.apache.lucene.index.Term(FIELD_TITLE, term)),
                    BooleanClause.Occur.SHOULD
                )
                .add(
                    new org.apache.lucene.search.TermQuery(new org.apache.lucene.index.Term(FIELD_DESCRIPTION, term)),
                    BooleanClause.Occur.SHOULD
                )
                .build();
            builder.add(termQuery, BooleanClause.Occur.SHOULD);
        }

        return builder.build();
    }

    private List<ScoredCatalogItem> toScoredItems(IndexSearcher searcher, TopDocs topDocs) throws IOException {
        var results = new ArrayList<ScoredCatalogItem>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            Document doc = searcher.storedFields().document(scoreDoc.doc);
            var item = CatalogItem.builder()
                .id(doc.get(FIELD_ID))
                .title(doc.get(FIELD_TITLE))
                .description(doc.get(FIELD_DESCRIPTION))
                .type(doc.get(FIELD_TYPE))
                .owner(doc.get(FIELD_OWNER))
                .tags(doc.get(FIELD_TAGS) != null ? Arrays.asList(doc.get(FIELD_TAGS).split(",")) : List.of())
                .build();
            results.add(new ScoredCatalogItem(item, scoreDoc.score));
        }
        return results;
    }

    /**
     * A catalog item together with its search relevance score.
     */
    public record ScoredCatalogItem(CatalogItem item, float score) {}
}
