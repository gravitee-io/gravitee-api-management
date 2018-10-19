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
package io.gravitee.management.service.impl.search.lucene.searcher;

import io.gravitee.management.model.common.Pageable;
import io.gravitee.management.service.impl.search.SearchResult;
import io.gravitee.management.service.impl.search.lucene.DocumentSearcher;
import io.gravitee.management.service.impl.search.lucene.analyzer.CustomWhitespaceAnalyzer;
import io.gravitee.repository.exceptions.TechnicalException;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class AbstractDocumentSearcher implements DocumentSearcher {

    /**
     * Logger.
     */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected final static String FIELD_ID = "id";
    protected final static String FIELD_TYPE = "type";

    protected Analyzer analyzer = new CustomWhitespaceAnalyzer();

    @Autowired
    protected IndexWriter indexWriter;

    protected SearchResult search(Query query) throws TechnicalException {
        return search(query, null);
    }

    protected SearchResult search(Query query, Pageable pageable) throws TechnicalException {
        logger.debug("Searching for: {}", query.toString());

        try {
            IndexSearcher searcher = getIndexSearcher();
            TopDocs topDocs;

            if (pageable != null) {
                //TODO: How to find the accurate numhits ?
                TopScoreDocCollector collector = TopScoreDocCollector.create(1000);
                searcher.search(query, collector);

                topDocs = collector.topDocs((pageable.getPageNumber() - 1) * pageable.getPageSize(), pageable.getPageSize());
            } else {
                topDocs = searcher.search(query, Integer.MAX_VALUE);
            }

            final ScoreDoc[] hits = topDocs.scoreDocs;
            final List<String> results = new ArrayList<>();

            logger.debug("Found {} total matching documents", topDocs.totalHits);

            if (hits.length > 0) {
                // Iterate over found results
                for (ScoreDoc hit : hits) {
                    results.add(getReference(searcher.doc(hit.doc)));
                }
            }

            return new SearchResult(results.stream().distinct().collect(Collectors.toList()), topDocs.totalHits);
        } catch (IOException ioe) {
            logger.error("An error occurs while getting documents from search result", ioe);
            throw new TechnicalException("An error occurs while getting documents from search result", ioe);
        }
    }

    protected String getReference(Document document) {
        return document.get(FIELD_ID);
    }

    private IndexSearcher getIndexSearcher() throws IOException {
        return new IndexSearcher(DirectoryReader.open(indexWriter));
    }
}
