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
package io.gravitee.management.service.impl.search.lucene;

import io.gravitee.repository.exceptions.TechnicalException;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class SearchEngineIndexer {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(SearchEngineIndexer.class);

    private final static String ID_FIELD = "id";
    private final static String TYPE_FIELD = "type";

    @Autowired
    private IndexWriter writer;

    public long index(Document document) throws TechnicalException {
        logger.debug("Updating a document into the Lucene index");
        String id = document.get(ID_FIELD);
        try {
            long seq = writer.updateDocument(new Term(ID_FIELD, id), document);
            writer.commit();
            return seq;
        } catch (IOException ioe) {
            logger.error("Fail to index document with ID: {}", id, ioe);
            throw new TechnicalException("Fail to index document with ID: " + id, ioe);
        }
    }

    public void remove(Document document) throws TechnicalException {
        String type = document.get(TYPE_FIELD);
        String id = document.get(ID_FIELD);

        logger.debug("Removing document type[{}] ID[{}]", type, id);

        BooleanQuery.Builder bq = new BooleanQuery.Builder();
        bq.add(new TermQuery(new Term(ID_FIELD, id)), BooleanClause.Occur.MUST);
        bq.add(new TermQuery(new Term(TYPE_FIELD, type)), BooleanClause.Occur.MUST);

        try {
            writer.deleteDocuments(bq.build());
        } catch (IOException ioe) {
            logger.error("Fail to index document with ID: {}", id, ioe);
            throw new TechnicalException("Fail to index document with ID: " + id, ioe);
        }
    }
}
