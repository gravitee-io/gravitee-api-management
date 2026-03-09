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

import static io.gravitee.rest.api.service.impl.search.lucene.transformer.IndexableApiProductDocumentTransformer.*;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.gravitee.apim.core.search.model.IndexableApiProduct;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.search.Indexable;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.ReferenceContext;
import io.gravitee.rest.api.service.impl.search.SearchResult;
import java.util.Map;
import java.util.Optional;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.springframework.stereotype.Component;

@Component
public class ApiProductDocumentSearcher extends AbstractDocumentSearcher {

    private static final Map<String, Float> API_PRODUCT_FIELD_BOOST = Map.ofEntries(
        Map.entry(FIELD_NAME, 20.0f),
        Map.entry(FIELD_NAME_LOWERCASE, 20.0f),
        Map.entry(FIELD_NAME_SPLIT, 18.0f),
        Map.entry(FIELD_DESCRIPTION, 5.0f),
        Map.entry(FIELD_DESCRIPTION_LOWERCASE, 5.0f),
        Map.entry(FIELD_DESCRIPTION_SPLIT, 4.0f),
        Map.entry(FIELD_OWNER, 3.0f),
        Map.entry(FIELD_OWNER_LOWERCASE, 3.0f)
    );

    private static final String[] API_PRODUCT_FIELD_SEARCH = new String[] {
        FIELD_ID,
        FIELD_NAME,
        FIELD_NAME_SORTED,
        FIELD_NAME_LOWERCASE,
        FIELD_NAME_SPLIT,
        FIELD_DESCRIPTION,
        FIELD_DESCRIPTION_LOWERCASE,
        FIELD_DESCRIPTION_SPLIT,
        FIELD_OWNER,
        FIELD_OWNER_LOWERCASE,
    };

    public ApiProductDocumentSearcher(IndexWriter indexWriter) {
        super(indexWriter);
    }

    private BooleanQuery.Builder buildApiProductQuery(ExecutionContext executionContext, Optional<Query> filterQuery) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder().add(
            new TermQuery(new Term(FIELD_TYPE, FIELD_TYPE_VALUE)),
            BooleanClause.Occur.FILTER
        );

        if (executionContext.hasEnvironmentId()) {
            builder.add(buildEnvCriteria(executionContext), BooleanClause.Occur.FILTER);
        }

        filterQuery.ifPresent(q -> builder.add(q, BooleanClause.Occur.FILTER));

        return builder;
    }

    private BooleanQuery buildEnvCriteria(ExecutionContext executionContext) {
        return new BooleanQuery.Builder()
            .add(new TermQuery(new Term(FIELD_REFERENCE_TYPE, ReferenceContext.Type.ENVIRONMENT.name())), BooleanClause.Occur.FILTER)
            .add(new TermQuery(new Term(FIELD_REFERENCE_ID, executionContext.getEnvironmentId())), BooleanClause.Occur.FILTER)
            .build();
    }

    private Optional<BooleanQuery> buildTextQuery(
        ExecutionContext executionContext,
        io.gravitee.rest.api.service.search.query.Query<?> query,
        Optional<Query> filterQuery
    ) throws ParseException {
        if (isBlank(query.getQuery())) {
            return Optional.empty();
        }
        BooleanQuery.Builder baseQuery = buildApiProductQuery(executionContext, filterQuery);
        MultiFieldQueryParser parser = new MultiFieldQueryParser(API_PRODUCT_FIELD_SEARCH, new KeywordAnalyzer(), API_PRODUCT_FIELD_BOOST);
        String escaped = QueryParserBase.escape(query.getQuery());
        Query parsed = parser.parse(escaped);
        baseQuery.add(parsed, BooleanClause.Occur.MUST);
        return Optional.of(baseQuery.build());
    }

    private Optional<BooleanQuery> buildWildcardQuery(
        ExecutionContext executionContext,
        io.gravitee.rest.api.service.search.query.Query<?> query,
        Optional<Query> filterQuery
    ) {
        if (isBlank(query.getQuery())) {
            return Optional.empty();
        }
        BooleanQuery.Builder baseQuery = buildApiProductQuery(executionContext, filterQuery);
        baseQuery.add(buildNameWildcardQuery(query.getQuery()), BooleanClause.Occur.MUST);
        return Optional.of(baseQuery.build());
    }

    private BooleanQuery buildNameWildcardQuery(String query) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        String lower = query.toLowerCase();
        builder.add(new WildcardQuery(new Term(FIELD_NAME, '*' + query + '*')), BooleanClause.Occur.SHOULD);
        builder.add(new WildcardQuery(new Term(FIELD_NAME_LOWERCASE, '*' + lower + '*')), BooleanClause.Occur.SHOULD);
        for (String token : query.split("\\s+")) {
            if (!token.isEmpty()) {
                builder.add(new WildcardQuery(new Term(FIELD_NAME, '*' + token + '*')), BooleanClause.Occur.SHOULD);
                builder.add(new WildcardQuery(new Term(FIELD_NAME_LOWERCASE, '*' + token.toLowerCase() + '*')), BooleanClause.Occur.SHOULD);
            }
        }
        return builder.build();
    }

    @Override
    public SearchResult search(ExecutionContext executionContext, io.gravitee.rest.api.service.search.query.Query<?> query)
        throws TechnicalException {
        BooleanQuery.Builder mainQuery = new BooleanQuery.Builder();

        try {
            final Optional<Query> baseFilterQuery = buildFilterQuery(query.getFilters(), Map.of(FIELD_TYPE_VALUE, FIELD_ID));
            buildTextQuery(executionContext, query, baseFilterQuery).ifPresent(q ->
                mainQuery.add(new BoostQuery(q, 4.0f), BooleanClause.Occur.SHOULD)
            );
            buildWildcardQuery(executionContext, query, baseFilterQuery).ifPresent(q -> mainQuery.add(q, BooleanClause.Occur.SHOULD));

            if (!mainQuery.build().clauses().iterator().hasNext()) {
                mainQuery.add(buildApiProductQuery(executionContext, baseFilterQuery).build(), BooleanClause.Occur.MUST);
            }
        } catch (ParseException pe) {
            log.error("Invalid query to search for API Product documents", pe);
            throw new TechnicalException("Invalid query to search for API Product documents", pe);
        }

        BooleanQuery finalQuery = mainQuery.build();

        try {
            return search(finalQuery, query.getSort());
        } catch (IndexSearcher.TooManyClauses e) {
            int maxClauseCount = getClauseCount(finalQuery);
            increaseMaxClauseCountIfNecessary(maxClauseCount);
            return search(finalQuery, query.getSort());
        }
    }

    private int getClauseCount(Query query) {
        int result = 0;
        if (query instanceof BooleanQuery bq) {
            result = bq.clauses().size();
            for (BooleanClause clause : bq.clauses()) {
                result += getClauseCount(clause.query());
            }
        } else if (query instanceof BoostQuery bq) {
            result += getClauseCount(bq.getQuery());
        }
        return result;
    }

    @Override
    public boolean handle(Class<? extends Indexable> source) {
        return IndexableApiProduct.class.isAssignableFrom(source);
    }
}
