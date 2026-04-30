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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import lombok.CustomLog;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.KnnFloatVectorQuery;
import org.apache.lucene.search.Query;
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

    private static final int RRF_K = 60;

    private static final String[] KEYWORD_FIELDS = {
        FIELD_TITLE,
        FIELD_DESCRIPTION,
        FIELD_PATHS,
        FIELD_ENTRYPOINT_TYPES,
        FIELD_ENDPOINT_TYPES,
        FIELD_CATEGORIES,
        FIELD_LISTENER_TYPES,
    };

    private static final Map<String, Float> KEYWORD_BOOSTS = Map.of(
        FIELD_TITLE,
        3.0f,
        FIELD_DESCRIPTION,
        1.0f,
        FIELD_PATHS,
        2.0f,
        FIELD_ENTRYPOINT_TYPES,
        1.5f,
        FIELD_ENDPOINT_TYPES,
        1.5f,
        FIELD_CATEGORIES,
        1.0f,
        FIELD_LISTENER_TYPES,
        1.0f
    );

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
     * Hybrid search using Reciprocal Rank Fusion (RRF).
     * <p>
     * Runs the KNN vector query and a BM25 keyword query independently, then
     * merges their rankings with RRF so that score-scale differences between
     * cosine similarity and BM25 do not distort the final ordering.
     * <p>
     * {@code RRF_score(doc) = 1/(K + rank_knn) + 1/(K + rank_bm25)}
     */
    public List<ScoredCatalogItem> hybridSearch(String intent, int k) throws IOException {
        int candidateSize = Math.max(k * 2, 10);
        float[] queryVector = embeddingService.embedText(intent);

        try (var reader = DirectoryReader.open(directory)) {
            var searcher = new IndexSearcher(reader);

            var knnDocs = searcher.search(new KnnFloatVectorQuery(FIELD_EMBEDDING, queryVector, candidateSize), candidateSize);
            var bm25Docs = searcher.search(buildKeywordQuery(intent), candidateSize);

            var knnRanks = buildRankMap(knnDocs);
            var bm25Ranks = buildRankMap(bm25Docs);

            var allDocIds = new HashSet<>(knnRanks.keySet());
            allDocIds.addAll(bm25Ranks.keySet());

            int missingRank = candidateSize + 1;
            var scored = allDocIds
                .stream()
                .map(docId -> {
                    double rrfScore =
                        1.0 / (RRF_K + knnRanks.getOrDefault(docId, missingRank)) +
                        1.0 / (RRF_K + bm25Ranks.getOrDefault(docId, missingRank));
                    return Map.entry(docId, rrfScore);
                })
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(k)
                .toList();

            return toScoredItemsFromEntries(searcher, scored);
        }
    }

    @Override
    public void close() throws IOException {
        directory.close();
    }

    /**
     * Build a BM25 keyword query using {@link MultiFieldQueryParser} with {@link StandardAnalyzer}
     * to ensure query terms are analyzed identically to the indexed text, with per-field boosts.
     */
    private Query buildKeywordQuery(String intent) throws IOException {
        var analyzer = new StandardAnalyzer();
        var parser = new MultiFieldQueryParser(KEYWORD_FIELDS, analyzer, KEYWORD_BOOSTS);
        parser.setDefaultOperator(QueryParser.Operator.OR);
        try {
            return parser.parse(QueryParser.escape(intent));
        } catch (ParseException e) {
            throw new IOException("Failed to parse keyword query for intent: " + intent, e);
        }
    }

    private Map<Integer, Integer> buildRankMap(TopDocs topDocs) {
        var ranks = new HashMap<Integer, Integer>();
        int rank = 1;
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            ranks.put(scoreDoc.doc, rank++);
        }
        return ranks;
    }

    private List<ScoredCatalogItem> toScoredItems(IndexSearcher searcher, TopDocs topDocs) throws IOException {
        var results = new ArrayList<ScoredCatalogItem>();
        for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
            results.add(new ScoredCatalogItem(toItem(searcher, scoreDoc.doc), scoreDoc.score));
        }
        return results;
    }

    private List<ScoredCatalogItem> toScoredItemsFromEntries(IndexSearcher searcher, List<Map.Entry<Integer, Double>> entries)
        throws IOException {
        var results = new ArrayList<ScoredCatalogItem>();
        for (var entry : entries) {
            results.add(new ScoredCatalogItem(toItem(searcher, entry.getKey()), entry.getValue().floatValue()));
        }
        return results;
    }

    private CatalogItem toItem(IndexSearcher searcher, int docId) throws IOException {
        Document doc = searcher.storedFields().document(docId);
        return CatalogItem.builder()
            .id(doc.get(FIELD_ID))
            .title(doc.get(FIELD_TITLE))
            .description(doc.get(FIELD_DESCRIPTION))
            .type(doc.get(FIELD_TYPE))
            .owner(doc.get(FIELD_OWNER))
            .tags(doc.get(FIELD_TAGS) != null ? Arrays.asList(doc.get(FIELD_TAGS).split(",")) : List.of())
            .paths(splitWhitespaceTokens(doc.get(FIELD_PATHS)))
            .entrypointTypes(splitWhitespaceTokens(doc.get(FIELD_ENTRYPOINT_TYPES)))
            .endpointTypes(splitWhitespaceTokens(doc.get(FIELD_ENDPOINT_TYPES)))
            .categories(resolveCategoriesFromDoc(doc))
            .listenerTypes(splitWhitespaceTokens(doc.get(FIELD_LISTENER_TYPES)))
            .build();
    }

    private static List<String> resolveCategoriesFromDoc(Document doc) {
        var stored = doc.get(FIELD_CATEGORIES_STORED);
        if (stored != null && !stored.isEmpty()) {
            return Arrays.stream(stored.split(String.valueOf(CATEGORY_NAME_DELIMITER), -1))
                .filter(s -> !s.isEmpty())
                .toList();
        }
        return splitWhitespaceTokens(doc.get(FIELD_CATEGORIES));
    }

    private static List<String> splitWhitespaceTokens(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.trim().split("\\s+"))
            .filter(s -> !s.isEmpty())
            .toList();
    }

    /**
     * A catalog item together with its search relevance score.
     */
    public record ScoredCatalogItem(CatalogItem item, float score) {}
}
