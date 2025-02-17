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

import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.*;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.search.Indexable;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.ReferenceContext;
import io.gravitee.rest.api.service.impl.search.SearchResult;
import io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer;
import jakarta.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiDocumentSearcher extends AbstractDocumentSearcher {

    public static final String FIELD_API_TYPE_VALUE = "api";

    private static final Map<String, Float> API_FIELD_BOOST = Map.of(
        FIELD_NAME,
        20.0f,
        FIELD_NAME_LOWERCASE,
        20.0f,
        FIELD_NAME_SPLIT,
        18.0f,
        FIELD_PATHS,
        10.0f,
        FIELD_HOSTS,
        10.0f,
        FIELD_LABELS,
        8.0f,
        FIELD_DESCRIPTION,
        5.0f,
        FIELD_METADATA,
        4.0f,
        FIELD_TAGS,
        1.0f
    );

    private static final String[] API_FIELD_SEARCH = new String[] {
        ApiDocumentTransformer.FIELD_ID,
        FIELD_NAME,
        FIELD_NAME_SORTED,
        FIELD_NAME_LOWERCASE,
        FIELD_NAME_SPLIT,
        FIELD_DESCRIPTION,
        FIELD_DESCRIPTION_LOWERCASE,
        FIELD_DESCRIPTION_SPLIT,
        FIELD_OWNER,
        FIELD_LABELS,
        FIELD_LABELS_SPLIT,
        FIELD_TAGS,
        FIELD_TAGS_SPLIT,
        FIELD_CATEGORIES,
        FIELD_CATEGORIES_SPLIT,
        FIELD_PATHS,
        FIELD_PATHS_SPLIT,
        FIELD_HOSTS,
        FIELD_HOSTS_SPLIT,
        FIELD_METADATA,
        FIELD_METADATA_SPLIT,
    };

    private static final String[] AUTHORIZED_EXPLICIT_FILTER = new String[] {
        FIELD_NAME,
        FIELD_OWNER,
        FIELD_LABELS,
        FIELD_CATEGORIES,
        FIELD_DESCRIPTION,
        FIELD_PATHS,
        FIELD_TAGS,
        FIELD_ORIGIN,
        FIELD_HAS_HEALTH_CHECK,
        FIELD_DEFINITION_VERSION,
    };

    public ApiDocumentSearcher(IndexWriter indexWriter) {
        super(indexWriter);
    }

    private BooleanQuery.Builder buildApiQuery(ExecutionContext executionContext, Optional<Query> filterQuery) {
        BooleanQuery.Builder apiQuery = new BooleanQuery.Builder()
            .add(new TermQuery(new Term(FIELD_TYPE, FIELD_API_TYPE_VALUE)), BooleanClause.Occur.FILTER);

        if (executionContext.hasEnvironmentId()) {
            apiQuery.add(buildEnvCriteria(executionContext), BooleanClause.Occur.FILTER);
        }

        filterQuery.ifPresent(q -> apiQuery.add(q, BooleanClause.Occur.FILTER));

        return apiQuery;
    }

    private Optional<BooleanQuery> buildIdsQuery(
        ExecutionContext executionContext,
        io.gravitee.rest.api.service.search.query.Query<?> query
    ) {
        if (!isBlank(query.getQuery()) && query.getIds() != null && !query.getIds().isEmpty()) {
            BooleanQuery.Builder mainQuery = new BooleanQuery.Builder();
            query
                .getIds()
                .forEach(id -> {
                    BooleanQuery.Builder idQuery = new BooleanQuery.Builder();
                    // Explicitly add a filter on the environment id to be sure that the query will not return APIs from another environment based on the ids
                    if (executionContext.hasEnvironmentId()) {
                        idQuery.add(buildEnvCriteria(executionContext), BooleanClause.Occur.FILTER);
                    }
                    idQuery.add(new TermQuery(new Term(FIELD_ID, id)), BooleanClause.Occur.FILTER);

                    mainQuery.add(idQuery.build(), BooleanClause.Occur.SHOULD);
                });

            return Optional.of(mainQuery.build());
        }
        return Optional.empty();
    }

    private Optional<BooleanQuery> buildExactMatchQuery(
        ExecutionContext executionContext,
        io.gravitee.rest.api.service.search.query.Query<?> query,
        Optional<Query> filterQuery
    ) throws ParseException {
        if (!isBlank(query.getQuery())) {
            BooleanQuery.Builder apiQuery = buildApiQuery(executionContext, filterQuery);
            MultiFieldQueryParser apiParser = new MultiFieldQueryParser(API_FIELD_SEARCH, new KeywordAnalyzer(), API_FIELD_BOOST);
            String queryEscaped = QueryParserBase.escape(query.getQuery());
            Query queryParsed = apiParser.parse(queryEscaped);
            apiQuery.add(queryParsed, BooleanClause.Occur.MUST);
            return Optional.of(apiQuery.build());
        }
        return Optional.empty();
    }

    private Optional<BooleanQuery> buildWildcardQuery(
        ExecutionContext executionContext,
        io.gravitee.rest.api.service.search.query.Query<?> query,
        Optional<Query> baseFilterQuery
    ) throws ParseException {
        if (!isBlank(query.getQuery())) {
            BooleanQuery.Builder mainQuery = buildApiQuery(executionContext, baseFilterQuery);

            MultiFieldQueryParser apiParser = new MultiFieldQueryParser(API_FIELD_SEARCH, new KeywordAnalyzer(), API_FIELD_BOOST);
            apiParser.setAllowLeadingWildcard(true);
            apiParser.setFuzzyMinSim(0.6f);
            String queryEscaped = QueryParserBase.escape(query.getQuery());
            Query queryParsed = apiParser.parse(queryEscaped);
            mainQuery.add(queryParsed, BooleanClause.Occur.SHOULD);
            mainQuery.add(buildApiFields(query.getQuery()), BooleanClause.Occur.MUST);
            return Optional.of(mainQuery.build());
        }
        return Optional.empty();
    }

    @Override
    public SearchResult search(ExecutionContext executionContext, io.gravitee.rest.api.service.search.query.Query<?> query)
        throws TechnicalException {
        BooleanQuery.Builder apiQuery = new BooleanQuery.Builder();

        try {
            final Optional<Query> baseFilterQuery = buildFilterQuery(query.getFilters(), Map.of(FIELD_API_TYPE_VALUE, FIELD_ID));
            buildExcludedFilters(query.getExcludedFilters()).ifPresent(q -> apiQuery.add(q, BooleanClause.Occur.MUST_NOT));
            buildExplicitQuery(executionContext, query, baseFilterQuery).ifPresent(q -> apiQuery.add(q, BooleanClause.Occur.MUST));
            buildExactMatchQuery(executionContext, query, baseFilterQuery)
                .ifPresent(q -> apiQuery.add(new BoostQuery(q, 4.0f), BooleanClause.Occur.SHOULD));
            buildWildcardQuery(executionContext, query, baseFilterQuery).ifPresent(q -> apiQuery.add(q, BooleanClause.Occur.SHOULD));
            buildIdsQuery(executionContext, query).ifPresent(q -> apiQuery.add(q, BooleanClause.Occur.SHOULD));
        } catch (ParseException pe) {
            logger.error("Invalid query to search for API documents", pe);
            throw new TechnicalException("Invalid query to search for API documents", pe);
        }

        BooleanQuery finalQuery = apiQuery.build();

        try {
            return search(finalQuery, query.getSort());
        } catch (IndexSearcher.TooManyClauses tooManyClauses) {
            int maxClauseCount = getClauseCount(finalQuery);
            increaseMaxClauseCountIfNecessary(maxClauseCount);
            return search(finalQuery, query.getSort());
        }
    }

    private int getClauseCount(Query query) {
        int result = 0;
        if (query instanceof BooleanQuery) {
            List<BooleanClause> clauses = ((BooleanQuery) query).clauses();
            result = clauses.size();
            for (BooleanClause clause : clauses) {
                result += getClauseCount(clause.getQuery());
            }
        } else if (query instanceof BoostQuery) {
            result += getClauseCount(((BoostQuery) query).getQuery());
        }

        return result;
    }

    private Optional<BooleanQuery> buildExcludedFilters(Map<String, Collection<String>> excludedFilters) {
        if (excludedFilters != null && !excludedFilters.isEmpty()) {
            BooleanQuery.Builder excludedFiltersQuery = new BooleanQuery.Builder();
            List<Query> excludedQuery = excludedFilters
                .keySet()
                .stream()
                .filter(key -> !excludedFilters.get(key).isEmpty())
                .map(key -> {
                    Collection<?> values = excludedFilters.get(key);
                    BooleanQuery.Builder booleanQuery = new BooleanQuery.Builder();
                    values.forEach(value -> booleanQuery.add(new TermQuery(new Term(key, (String) value)), BooleanClause.Occur.SHOULD));
                    return booleanQuery.build();
                })
                .collect(toList());

            excludedQuery.forEach(query -> excludedFiltersQuery.add(query, BooleanClause.Occur.SHOULD));
            return Optional.of(excludedFiltersQuery.build());
        }
        return Optional.empty();
    }

    private Optional<BooleanQuery> buildExplicitQuery(
        ExecutionContext executionContext,
        io.gravitee.rest.api.service.search.query.Query<?> query,
        Optional<Query> baseFilterQuery
    ) {
        BooleanQuery.Builder filtersQuery = buildApiQuery(executionContext, baseFilterQuery);
        String rest = completeQueryWithFilters(query, filtersQuery);
        if (!rest.equals(query.getQuery())) {
            query.setQuery(rest);
            return Optional.of(filtersQuery.build());
        }
        return Optional.empty();
    }

    protected String completeQueryWithFilters(io.gravitee.rest.api.service.search.query.Query<?> query, BooleanQuery.Builder mainQuery) {
        if (isBlank(query.getQuery())) {
            return "";
        }
        try {
            BooleanQuery.Builder restQuery = new BooleanQuery.Builder();
            Set<String> rest = appendExplicitFilters(query.getQuery(), mainQuery, restQuery);
            BooleanQuery build = restQuery.build();
            if (!build.clauses().isEmpty()) {
                mainQuery.add(build, build.clauses().getFirst().getOccur());
            }
            if (!CollectionUtils.isEmpty(rest)) {
                return String.join(" ", rest);
            } else {
                return "";
            }
        } catch (ParseException e) {
            logger.error("Unable to parse query", e);
            return query.getQuery();
        }
    }

    protected Set<String> appendExplicitFilters(String query, BooleanQuery.Builder mainQuery, BooleanQuery.Builder restQuery)
        throws ParseException {
        QueryParser parser = new QueryParser("", new KeywordAnalyzer());
        parser.setAllowLeadingWildcard(true);
        // Escape [ and ] because they can be used in API names
        String escapedQuery = query.replace("[", "\\[").replace("]", "\\]");
        if (escapedQuery.startsWith("/")) { // escape if we are looking for a path
            escapedQuery = QueryParserBase.escape(query);
        }
        org.apache.lucene.search.Query parse = parser.parse(escapedQuery);
        if (hasExplicitFilter(query)) {
            return appendExplicitFilters(parse, mainQuery, restQuery, null);
        }
        return Collections.singleton(query);
    }

    private @Nullable Set<String> appendExplicitFilters(
        org.apache.lucene.search.Query query,
        BooleanQuery.Builder mainQuery,
        BooleanQuery.Builder restQuery,
        BooleanClause clause
    ) {
        Set<String> rest = new HashSet<>();
        BooleanClause.Occur currentOccur = clause != null ? clause.getOccur() : BooleanClause.Occur.FILTER;
        if (query instanceof TermQuery) {
            appendTermFilters((TermQuery) query, mainQuery, restQuery, clause, rest, currentOccur);
        } else if (query instanceof BooleanQuery) {
            if (appendBoolFilters((BooleanQuery) query, mainQuery, restQuery, rest)) {
                return null;
            }
        } else if (query instanceof WildcardQuery) {
            appendWildcardFilters((WildcardQuery) query, mainQuery, rest, currentOccur);
        }
        return rest;
    }

    private void appendWildcardFilters(
        WildcardQuery query,
        BooleanQuery.Builder mainQuery,
        Set<String> rest,
        BooleanClause.Occur currentOccur
    ) {
        Term term = query.getTerm();
        if (Arrays.stream(AUTHORIZED_EXPLICIT_FILTER).anyMatch(field -> field.equals(term.field()))) {
            mainQuery.add(query, currentOccur);
        } else {
            rest.add(term.text());
        }
    }

    private boolean appendBoolFilters(
        BooleanQuery query,
        BooleanQuery.Builder mainQuery,
        BooleanQuery.Builder restQuery,
        Set<String> rest
    ) {
        List<BooleanClause> clauses = query.clauses();
        if (!clauses.isEmpty()) {
            BooleanQuery.Builder subQuery = new BooleanQuery.Builder();
            for (BooleanClause _clause : clauses) {
                Query innerQuery = _clause.getQuery();
                Set<String> innerRest = appendExplicitFilters(innerQuery, subQuery, restQuery, _clause);
                if (innerRest == null) {
                    return true;
                } else {
                    rest.addAll(innerRest);
                }
            }
            mainQuery.add(subQuery.build(), BooleanClause.Occur.FILTER);
        }
        return false;
    }

    private void appendTermFilters(
        TermQuery query,
        BooleanQuery.Builder mainQuery,
        BooleanQuery.Builder restQuery,
        BooleanClause clause,
        Set<String> rest,
        BooleanClause.Occur currentOccur
    ) {
        Term term = query.getTerm();
        if (Arrays.stream(AUTHORIZED_EXPLICIT_FILTER).anyMatch(field -> field.equals(term.field()))) {
            mainQuery.add(buildQueryFilter(term), currentOccur);
        } else if (clause != null) {
            restQuery.add(buildApiFields(term.text()), clause.getOccur());
        } else {
            rest.add(term.text());
        }
    }

    private Query buildQueryFilter(Term term) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        String field = term.field();
        String text = term.text();
        if (FIELD_CATEGORIES.equals(term.field())) {
            text = formatCategoryField(term.text());
        } else if (
            !FIELD_PATHS.equals(term.field()) &&
            !FIELD_TAGS.equals(term.field()) &&
            !FIELD_ORIGIN.equals(term.field()) &&
            !FIELD_HAS_HEALTH_CHECK.equals(term.field()) &&
            !FIELD_DEFINITION_VERSION.equals(term.field())
        ) {
            text = text.toLowerCase();
            field = field.concat("_lowercase");
        }
        return builder
            .add(new PhraseQuery(field, text), BooleanClause.Occur.SHOULD)
            .add(new WildcardQuery(new Term(field, text)), BooleanClause.Occur.SHOULD)
            .build();
    }

    public static String formatCategoryField(String category) {
        // Actually we index hrid categories...
        return category.toLowerCase().replace(" ", "-");
    }

    private Query toWildcard(String field, String query) {
        return new WildcardQuery(new Term(field, '*' + query + '*'));
    }

    private BooleanQuery buildApiFields(String query, Query... queries) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        if (queries != null) {
            for (Query q : queries) {
                builder.add(q, BooleanClause.Occur.SHOULD);
            }
        }

        String[] tokens = query.split(" ");

        // Add boost on exact match on name
        builder
            .add(new BoostQuery(toWildcard(FIELD_NAME, query), 20.0f), BooleanClause.Occur.SHOULD)
            .add(new BoostQuery(toWildcard(FIELD_NAME_LOWERCASE, query.toLowerCase()), 18.0f), BooleanClause.Occur.SHOULD)
            .add(new BoostQuery(toWildcard(FIELD_NAME_SORTED, query.toLowerCase()), 15.0f), BooleanClause.Occur.SHOULD);

        // Add boost for partial match
        for (String token : tokens) {
            builder
                .add(new BoostQuery(toWildcard(FIELD_NAME, token), 12.0f), BooleanClause.Occur.SHOULD)
                .add(new BoostQuery(toWildcard(FIELD_NAME_LOWERCASE, token.toLowerCase()), 10.0f), BooleanClause.Occur.SHOULD)
                .add(new BoostQuery(toWildcard(FIELD_PATHS, token), 8.0f), BooleanClause.Occur.SHOULD)
                .add(toWildcard(FIELD_DESCRIPTION, token), BooleanClause.Occur.SHOULD)
                .add(toWildcard(FIELD_DESCRIPTION_LOWERCASE, token.toLowerCase()), BooleanClause.Occur.SHOULD)
                .add(toWildcard(FIELD_HOSTS, token), BooleanClause.Occur.SHOULD)
                .add(toWildcard(FIELD_LABELS, token), BooleanClause.Occur.SHOULD)
                .add(toWildcard(FIELD_CATEGORIES, token), BooleanClause.Occur.SHOULD)
                .add(toWildcard(FIELD_TAGS, token), BooleanClause.Occur.SHOULD)
                .add(toWildcard(FIELD_METADATA, token), BooleanClause.Occur.SHOULD);
        }
        return builder.build();
    }

    private BooleanQuery buildEnvCriteria(ExecutionContext executionContext) {
        return new BooleanQuery.Builder()
            .add(new TermQuery(new Term(FIELD_REFERENCE_TYPE, ReferenceContext.Type.ENVIRONMENT.name())), BooleanClause.Occur.FILTER)
            .add(new TermQuery(new Term(FIELD_REFERENCE_ID, executionContext.getEnvironmentId())), BooleanClause.Occur.FILTER)
            .build();
    }

    private boolean hasExplicitFilter(String query) {
        if (query != null) {
            return Arrays.stream(AUTHORIZED_EXPLICIT_FILTER).anyMatch(field -> query.contains(field + ":"));
        }
        return false;
    }

    @Override
    public boolean handle(Class<? extends Indexable> source) {
        return source.isAssignableFrom(ApiEntity.class) || source.isAssignableFrom(io.gravitee.rest.api.model.v4.api.ApiEntity.class);
    }
}
