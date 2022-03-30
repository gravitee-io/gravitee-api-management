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
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.impl.search.SearchResult;
import io.gravitee.rest.api.service.search.query.Query;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class UserDocumentSearcher extends AbstractDocumentSearcher {

    protected static final String FIELD_TYPE_VALUE = "user";

    @Override
    public SearchResult search(ExecutionContext executionContext, Query query) throws TechnicalException {
        QueryParser parser = new MultiFieldQueryParser(
            new String[] { "displayname", "displayname_reverted", "email", "reference" },
            analyzer
        );
        parser.setFuzzyMinSim(0.6f);
        parser.setAllowLeadingWildcard(true);

        BooleanQuery.Builder userQuery = new BooleanQuery.Builder();
        BooleanQuery.Builder userFieldsQuery = new BooleanQuery.Builder();

        try {
            parser.parse(QueryParserBase.escape(query.getQuery()));
            final String normalizedQuery = StringUtils.stripAccents(query.getQuery().toLowerCase());

            if (isUserIdFormat(query)) {
                userFieldsQuery.add(new WildcardQuery(new Term("id", normalizedQuery)), BooleanClause.Occur.MUST);
            } else {
                userFieldsQuery.add(new WildcardQuery(new Term("displayname", '*' + normalizedQuery + '*')), BooleanClause.Occur.SHOULD);
                userFieldsQuery.add(
                    new WildcardQuery(new Term("displayname_reverted", '*' + normalizedQuery + '*')),
                    BooleanClause.Occur.SHOULD
                );

                userFieldsQuery.add(new WildcardQuery(new Term("email", '*' + normalizedQuery + '*')), BooleanClause.Occur.SHOULD);
                userFieldsQuery.add(new WildcardQuery(new Term("reference", '*' + normalizedQuery + '*')), BooleanClause.Occur.SHOULD);
            }

            userQuery.add(userFieldsQuery.build(), BooleanClause.Occur.MUST);
            userQuery.add(new TermQuery(new Term(FIELD_TYPE, FIELD_TYPE_VALUE)), BooleanClause.Occur.MUST);

            BooleanQuery.Builder orgCriteria = new BooleanQuery.Builder();
            orgCriteria.add(
                new TermQuery(new Term(FIELD_REFERENCE_TYPE, GraviteeContext.ReferenceContextType.ORGANIZATION.name())),
                BooleanClause.Occur.MUST
            );
            orgCriteria.add(new TermQuery(new Term(FIELD_REFERENCE_ID, executionContext.getOrganizationId())), BooleanClause.Occur.MUST);

            userQuery.add(orgCriteria.build(), BooleanClause.Occur.FILTER);

            return search(userQuery.build(), null, query.getPage());
        } catch (ParseException pe) {
            logger.error("Invalid query to search for user documents", pe);
            throw new TechnicalException("Invalid query to search for user documents", pe);
        }
    }

    private boolean isUserIdFormat(io.gravitee.rest.api.service.search.query.Query query) {
        try {
            UUID.fromString(query.getQuery());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public boolean handle(Class<? extends Indexable> source) {
        return source.isAssignableFrom(UserEntity.class);
    }
}
