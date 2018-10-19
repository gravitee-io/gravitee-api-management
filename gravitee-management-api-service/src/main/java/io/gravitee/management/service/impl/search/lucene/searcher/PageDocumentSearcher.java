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

import io.gravitee.management.model.PageEntity;
import io.gravitee.management.model.search.Indexable;
import io.gravitee.management.service.impl.search.SearchResult;
import io.gravitee.repository.exceptions.TechnicalException;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class PageDocumentSearcher extends AbstractDocumentSearcher {

    protected final static String FIELD_TYPE_VALUE = "page";

    @Override
    public SearchResult search(io.gravitee.management.service.search.query.Query query) throws TechnicalException {
        QueryParser parser = new MultiFieldQueryParser(new String[]{
                "name",
                "content"
        }, analyzer);
        parser.setFuzzyMinSim(0.6f);

        try {
            final Query parse = parser.parse(QueryParserBase.escape(query.getQuery()));

            BooleanQuery.Builder bq = new BooleanQuery.Builder();
            bq.add(parse, BooleanClause.Occur.MUST);
            bq.add(new TermQuery(new Term(FIELD_TYPE, FIELD_TYPE_VALUE)), BooleanClause.Occur.MUST);

            return search(bq.build());
        } catch (ParseException pe) {
            logger.error("Invalid query to search for page documents", pe);
            throw new TechnicalException("Invalid query to search for page documents", pe);
        }
    }

    @Override
    public boolean handle(Class<? extends Indexable> source) {
        return source.isAssignableFrom(PageEntity.class);
    }
}
