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
package io.gravitee.rest.api.service.impl.search.lucene.searcher;

import io.gravitee.node.logging.NodeLoggerFactory;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.service.impl.search.SearchResult;
import io.gravitee.rest.api.service.impl.search.lucene.DocumentSearcher;
import io.gravitee.rest.api.service.impl.search.lucene.analyzer.CustomWhitespaceAnalyzer;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollectorManager;
import org.slf4j.Logger;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractDocumentSearcher implements DocumentSearcher {

    protected final Logger log = NodeLoggerFactory.getLogger(this.getClass());

    protected static final String FIELD_ID = "id";
    protected static final String FIELD_TYPE = "type";
    protected static final String FIELD_API_TYPE_VALUE = "api";

    protected static void increaseMaxClauseCountIfNecessary(int size) {
        if (size > IndexSearcher.getMaxClauseCount()) {
            IndexSearcher.setMaxClauseCount(Math.min(size + 100, Integer.MAX_VALUE));
        }
    }

    protected Analyzer analyzer = new CustomWhitespaceAnalyzer();

    protected IndexWriter indexWriter;

    protected AbstractDocumentSearcher(IndexWriter indexWriter) {
        this.indexWriter = indexWriter;
    }

    protected SearchResult search(Query query) throws TechnicalException {
        return search(query, null, null, FIELD_ID);
    }

    protected SearchResult search(Query query, Sortable sortable) throws TechnicalException {
        return search(query, sortable, null, FIELD_ID);
    }

    public SearchResult searchReference(Query query) throws TechnicalException {
        return search(query, null, null, FIELD_REFERENCE_ID);
    }

    protected SearchResult search(Query query, Sortable sort, Pageable pageable) throws TechnicalException {
        return this.search(query, sort, pageable, FIELD_ID);
    }

    protected SearchResult search(Query query, Sortable sort, Pageable pageable, String fieldReference) throws TechnicalException {
        log.debug("Searching for: {}", query.toString());

        try {
            IndexSearcher searcher = getIndexSearcher();
            TopDocs topDocs;

            if (pageable != null) {
                //TODO: How to find the accurate numhits ?
                var collectorManager = new TopScoreDocCollectorManager(1000, null, 1000);
                searcher.search(query, collectorManager);

                topDocs = collectorManager
                    .newCollector()
                    .topDocs((pageable.getPageNumber() - 1) * pageable.getPageSize(), pageable.getPageSize());
            } else if (sort != null) {
                topDocs = searcher.search(query, Integer.MAX_VALUE, convert(sort));
            } else {
                topDocs = searcher.search(query, Integer.MAX_VALUE);
            }

            final Set<String> results = new LinkedHashSet<>();

            log.debug("Found {} total matching documents", topDocs.totalHits);
            for (ScoreDoc doc : topDocs.scoreDocs) {
                String reference = searcher.storedFields().document(doc.doc).get(fieldReference);
                results.add(reference);
            }

            return new SearchResult(results, topDocs.totalHits.value());
        } catch (IOException ioe) {
            log.error("An error occurs while getting documents from search result", ioe);
            throw new TechnicalException("An error occurs while getting documents from search result", ioe);
        }
    }

    protected Sort convert(Sortable sort) {
        if (sort != null) {
            SortField sortField = new SortField(sort.getField() + "_sorted", SortField.Type.STRING, !sort.isAscOrder());
            if (sort.isAscOrder()) {
                sortField.setMissingValue(SortField.STRING_LAST);
            } else {
                sortField.setMissingValue(SortField.STRING_FIRST);
            }
            return new Sort(sortField);
        }
        return null;
    }

    protected IndexSearcher getIndexSearcher() throws IOException {
        return new IndexSearcher(DirectoryReader.open(indexWriter));
    }

    protected Optional<Query> buildFilterQuery(Map<String, Object> filters, Map<String, String> remapFields) {
        if (filters == null || filters.isEmpty()) {
            return Optional.empty();
        }

        var queries = filters
            .entrySet()
            .stream()
            .flatMap(e -> prepareQuery(e.getKey(), e.getValue(), remapFields))
            .toList();

        return switch (queries.size()) {
            case 0 -> Optional.empty();
            case 1 -> Optional.of(queries.getFirst());
            default -> {
                BooleanQuery.Builder filtersQuery = new BooleanQuery.Builder();
                queries.forEach(v -> filtersQuery.add(new BooleanClause(v, BooleanClause.Occur.MUST)));
                filtersQuery.add(filtersQuery.build(), BooleanClause.Occur.MUST);
                yield Optional.of(filtersQuery.build());
            }
        };
    }

    private Stream<Query> prepareQuery(String field, Object values, Map<String, String> remapFields) {
        if (Collection.class.isAssignableFrom(values.getClass())) {
            Collection<?> valuesAsCollection = (Collection<?>) values;
            increaseMaxClauseCountIfNecessary(valuesAsCollection.size());

            BooleanQuery.Builder filterApisQuery = new BooleanQuery.Builder();
            valuesAsCollection.forEach(value ->
                filterApisQuery.add(
                    new TermQuery(new Term(remapFields.getOrDefault(field, field), (String) value)),
                    BooleanClause.Occur.SHOULD
                )
            );
            if (!valuesAsCollection.isEmpty()) {
                return Stream.of(filterApisQuery.build());
            }
        } else if (values instanceof String value) {
            return Stream.of(new TermQuery(new Term(remapFields.getOrDefault(field, field), QueryParserBase.escape(value))));
        } else if (values instanceof Boolean bool) {
            return Stream.of(new TermQuery(new Term(remapFields.getOrDefault(field, field), Boolean.toString(bool))));
        }
        return Stream.empty();
    }
}
