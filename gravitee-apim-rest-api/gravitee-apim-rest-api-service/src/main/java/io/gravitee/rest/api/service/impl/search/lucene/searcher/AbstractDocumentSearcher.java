/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl.search.lucene.searcher;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.Sortable;
import io.gravitee.rest.api.service.impl.search.SearchResult;
import io.gravitee.rest.api.service.impl.search.lucene.DocumentSearcher;
import io.gravitee.rest.api.service.impl.search.lucene.analyzer.CustomWhitespaceAnalyzer;
import java.io.IOException;
import java.util.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractDocumentSearcher implements DocumentSearcher {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected static final String FIELD_ID = "id";
    protected static final String FIELD_TYPE = "type";
    protected static final String FIELD_API_TYPE_VALUE = "api";

    protected Analyzer analyzer = new CustomWhitespaceAnalyzer();

    @Autowired
    protected IndexWriter indexWriter;

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
        logger.debug("Searching for: {}", query.toString());

        try {
            IndexSearcher searcher = getIndexSearcher();
            TopDocs topDocs;

            if (pageable != null) {
                //TODO: How to find the accurate numhits ?
                TopScoreDocCollector collector = TopScoreDocCollector.create(1000);
                searcher.search(query, collector);

                topDocs = collector.topDocs((pageable.getPageNumber() - 1) * pageable.getPageSize(), pageable.getPageSize());
            } else if (sort != null) {
                topDocs = searcher.search(query, Integer.MAX_VALUE, convert(sort));
            } else {
                topDocs = searcher.search(query, Integer.MAX_VALUE);
            }

            final Set<String> results = new LinkedHashSet<>();

            logger.debug("Found {} total matching documents", topDocs.totalHits);
            for (ScoreDoc doc : topDocs.scoreDocs) {
                String reference = searcher.doc(doc.doc).get(fieldReference);
                results.add(reference);
            }

            return new SearchResult(results, results.size());
        } catch (IOException ioe) {
            logger.error("An error occurs while getting documents from search result", ioe);
            throw new TechnicalException("An error occurs while getting documents from search result", ioe);
        }
    }

    protected Sort convert(Sortable sort) {
        if (sort != null) {
            return new Sort(new SortField(sort.getField() + "_sorted", SortField.Type.STRING, !sort.isAscOrder()));
        }
        return null;
    }

    private IndexSearcher getIndexSearcher() throws IOException {
        return new IndexSearcher(DirectoryReader.open(indexWriter));
    }

    protected Optional<Query> buildFilterQuery(String apiReferenceField, Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return Optional.empty();
        }
        BooleanQuery.Builder filtersQuery = new BooleanQuery.Builder();
        if (filters.containsKey(FIELD_API_TYPE_VALUE)) {
            Object values = filters.get(FIELD_API_TYPE_VALUE);
            if (Collection.class.isAssignableFrom(values.getClass())) {
                Collection valuesAsCollection = (Collection) values;
                if (valuesAsCollection.size() > BooleanQuery.getMaxClauseCount()) {
                    BooleanQuery.setMaxClauseCount(valuesAsCollection.size());
                }
                BooleanQuery.Builder filterApisQuery = new BooleanQuery.Builder();
                ((Collection<?>) values).forEach(
                        value -> filterApisQuery.add(new TermQuery(new Term(apiReferenceField, (String) value)), BooleanClause.Occur.SHOULD)
                    );
                if (valuesAsCollection.size() > 0) {
                    filtersQuery.add(filterApisQuery.build(), BooleanClause.Occur.SHOULD);
                }
            }
        }

        final boolean[] hasClause = { false };
        filters.forEach(
            (field, value) -> {
                if (!Collection.class.isAssignableFrom(value.getClass())) {
                    filtersQuery.add(new TermQuery(new Term(field, QueryParserBase.escape((String) value))), BooleanClause.Occur.MUST);
                    hasClause[0] = true;
                }
            }
        );

        if (hasClause[0]) {
            filtersQuery.add(filtersQuery.build(), BooleanClause.Occur.MUST);
        }

        return Optional.of(filtersQuery.build());
    }
}
