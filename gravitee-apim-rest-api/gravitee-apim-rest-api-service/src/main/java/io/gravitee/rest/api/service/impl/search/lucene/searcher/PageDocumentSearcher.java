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

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.search.Indexable;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.impl.search.SearchResult;
import java.util.Optional;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.*;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class PageDocumentSearcher extends AbstractDocumentSearcher {

    protected static final String FIELD_TYPE_VALUE = "page";

    @Override
    public SearchResult searchReference(ExecutionContext executionContext, io.gravitee.rest.api.service.search.query.Query query)
        throws TechnicalException {
        try {
            BooleanQuery.Builder pageQuery = this.buildQuery(query);
            // Note: Page search does not seem to be used so for now we don't implement any filtering for organization or environment.
            return searchReference(pageQuery.build());
        } catch (ParseException pe) {
            logger.error("Invalid query to search for page documents", pe);
            throw new TechnicalException("Invalid query to search for page documents", pe);
        }
    }

    @Override
    public SearchResult search(ExecutionContext executionContext, io.gravitee.rest.api.service.search.query.Query query)
        throws TechnicalException {
        try {
            BooleanQuery.Builder pageQuery = this.buildQuery(query);
            // Note: Page search does not seem to be used so for now we don't implement any filtering for organization or environment.
            return search(pageQuery.build());
        } catch (ParseException pe) {
            logger.error("Invalid query to search for page documents", pe);
            throw new TechnicalException("Invalid query to search for page documents", pe);
        }
    }

    private BooleanQuery.Builder buildQuery(io.gravitee.rest.api.service.search.query.Query query) throws ParseException {
        QueryParser parser = new MultiFieldQueryParser(new String[] { "name", "name_lowercase", "content" }, analyzer);
        parser.setFuzzyMinSim(0.6f);

        BooleanQuery.Builder pageQuery = new BooleanQuery.Builder();
        BooleanQuery.Builder pageFieldsQuery = new BooleanQuery.Builder();

        if (!isBlank(query.getQuery())) {
            final Query parse = parser.parse(QueryParserBase.escape(query.getQuery()));
            pageFieldsQuery.add(parse, BooleanClause.Occur.SHOULD);
            pageFieldsQuery.add(new WildcardQuery(new Term("name", '*' + query.getQuery() + '*')), BooleanClause.Occur.SHOULD);
            pageFieldsQuery.add(
                new WildcardQuery(new Term("name_lowercase", '*' + query.getQuery().toLowerCase() + '*')),
                BooleanClause.Occur.SHOULD
            );
            pageFieldsQuery.add(new WildcardQuery(new Term("content", '*' + query.getQuery() + '*')), BooleanClause.Occur.SHOULD);
        }

        pageQuery.add(pageFieldsQuery.build(), BooleanClause.Occur.MUST);
        pageQuery.add(new TermQuery(new Term(FIELD_TYPE, FIELD_TYPE_VALUE)), BooleanClause.Occur.MUST);
        Optional<Query> baseFilterQuery = this.buildFilterQuery(FIELD_REFERENCE_ID, query.getFilters());
        baseFilterQuery.ifPresent(q -> pageQuery.add(q, BooleanClause.Occur.FILTER));
        return pageQuery;
    }

    @Override
    public boolean handle(Class<? extends Indexable> source) {
        return source.isAssignableFrom(PageEntity.class);
    }
}
