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
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.search.Indexable;
import io.gravitee.rest.api.service.impl.search.SearchResult;
import io.gravitee.rest.api.service.search.query.QueryBuilder;
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
public class UserDocumentSearcher extends AbstractDocumentSearcher {

    protected static final String FIELD_TYPE_VALUE = "user";

    @Override
    public SearchResult search(io.gravitee.rest.api.service.search.query.Query query) throws TechnicalException {
        QueryParser parser = new MultiFieldQueryParser(
            new String[] { "firstname", "lastname", "displayname", "displayname_split", "email", "reference" },
            analyzer
        );
        parser.setFuzzyMinSim(0.6f);
        parser.setAllowLeadingWildcard(true);

        try {
            Query parse = parser.parse(QueryParserBase.escape(query.getQuery()));

            BooleanQuery.Builder userQuery = new BooleanQuery.Builder();
            BooleanQuery.Builder userFieldsQuery = new BooleanQuery.Builder();

            userFieldsQuery.add(parse, BooleanClause.Occur.SHOULD);
            userFieldsQuery.add(new WildcardQuery(new Term("firstname", '*' + query.getQuery() + '*')), BooleanClause.Occur.SHOULD);
            userFieldsQuery.add(new WildcardQuery(new Term("lastname", '*' + query.getQuery() + '*')), BooleanClause.Occur.SHOULD);
            userFieldsQuery.add(new WildcardQuery(new Term("displayname", '*' + query.getQuery() + '*')), BooleanClause.Occur.SHOULD);
            userFieldsQuery.add(new WildcardQuery(new Term("email", '*' + query.getQuery() + '*')), BooleanClause.Occur.SHOULD);
            userFieldsQuery.add(new WildcardQuery(new Term("reference", '*' + query.getQuery() + '*')), BooleanClause.Occur.SHOULD);
            userQuery.add(userFieldsQuery.build(), BooleanClause.Occur.MUST);
            userQuery.add(new TermQuery(new Term(FIELD_TYPE, FIELD_TYPE_VALUE)), BooleanClause.Occur.MUST);

            return search(userQuery.build(), query.getPage());
        } catch (ParseException pe) {
            logger.error("Invalid query to search for user documents", pe);
            throw new TechnicalException("Invalid query to search for user documents", pe);
        }
    }

    @Override
    public boolean handle(Class<? extends Indexable> source) {
        return source.isAssignableFrom(UserEntity.class);
    }
}
