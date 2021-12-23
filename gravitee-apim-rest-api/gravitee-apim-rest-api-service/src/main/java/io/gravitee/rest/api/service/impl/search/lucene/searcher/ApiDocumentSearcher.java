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

import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.*;
import static org.apache.commons.lang3.StringUtils.isBlank;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.search.Indexable;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.impl.search.SearchResult;
import io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer;
import io.gravitee.rest.api.service.impl.search.lucene.transformer.PageDocumentTransformer;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
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
public class ApiDocumentSearcher extends AbstractDocumentSearcher {

    public static final String FIELD_API_TYPE_VALUE = "api";
    public static final String FIELD_PAGE_TYPE_VALUE = "page";

    private static final Map<String, Float> API_FIELD_BOOST = new HashMap<>() {
        {
            put(FIELD_NAME, 20.0f);
            put(FIELD_NAME_LOWERCASE, 20.0f);
            put(FIELD_NAME_SPLIT, 18.0f);
            put(FIELD_PATHS, 10.0f);
            put(FIELD_HOSTS, 10.0f);
            put(FIELD_LABELS, 8.0f);
            put(FIELD_DESCRIPTION, 5.0f);
            put(FIELD_METADATA, 4.0f);
            put(FIELD_TAGS, 1.0f);
        }
    };

    private static final Map<String, Float> PAGE_FIELD_BOOST = new HashMap<>() {
        {
            put(PageDocumentTransformer.FIELD_NAME, 1.0f);
            put(PageDocumentTransformer.FIELD_NAME_LOWERCASE, 1.0f);
            put(PageDocumentTransformer.FIELD_NAME_SPLIT, 1.0f);
            put(PageDocumentTransformer.FIELD_CONTENT, 1.0f);
        }
    };

    private static final String[] API_FIELD_SEARCH = new String[] {
        ApiDocumentTransformer.FIELD_ID,
        FIELD_NAME,
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
    private static final String[] PAGE_FIELD_SEARCH = new String[] {
        PageDocumentTransformer.FIELD_NAME,
        PageDocumentTransformer.FIELD_NAME_LOWERCASE,
        PageDocumentTransformer.FIELD_NAME_SPLIT,
        PageDocumentTransformer.FIELD_CONTENT,
        PageDocumentTransformer.FIELD_REFERENCE_ID,
        PageDocumentTransformer.FIELD_REFERENCE_TYPE,
    };
    private static final String[] AUTHORIZED_EXPLICIT_FILTER = new String[] {
        FIELD_NAME,
        FIELD_OWNER,
        FIELD_LABELS,
        FIELD_CATEGORIES,
        FIELD_DESCRIPTION,
        FIELD_PATHS,
        FIELD_TAGS,
    };

    private BooleanQuery.Builder buildApiQuery(io.gravitee.rest.api.service.search.query.Query query) {
        BooleanQuery.Builder apiQuery = new BooleanQuery.Builder();
        BooleanQuery envCriteria = buildEnvCriteria();

        // Search in API fields
        apiQuery
            .add(new TermQuery(new Term(FIELD_TYPE, FIELD_API_TYPE_VALUE)), BooleanClause.Occur.FILTER)
            .add(envCriteria, BooleanClause.Occur.FILTER);

        Query apisFilter = this.buildFilterQuery(FIELD_ID, query.getFilters());
        if (apisFilter != null) {
            apiQuery.add(apisFilter, BooleanClause.Occur.FILTER);
        }
        return apiQuery;
    }

    private Optional<BooleanQuery> buildIdsQuery(io.gravitee.rest.api.service.search.query.Query query) throws ParseException {
        if (!isBlank(query.getQuery()) && query.getIds() != null && !query.getIds().isEmpty()) {
            BooleanQuery.Builder mainQuery = new BooleanQuery.Builder();
            query.getIds().forEach(id -> mainQuery.add(new TermQuery(new Term(FIELD_ID, (String) id)), BooleanClause.Occur.SHOULD));
            return Optional.of(mainQuery.build());
        }
        return Optional.empty();
    }

    private Optional<BooleanQuery> buildExactMatchQuery(io.gravitee.rest.api.service.search.query.Query query) throws ParseException {
        if (!isBlank(query.getQuery())) {
            BooleanQuery.Builder mainQuery = buildApiQuery(query);
            MultiFieldQueryParser apiParser = new MultiFieldQueryParser(API_FIELD_SEARCH, new KeywordAnalyzer(), API_FIELD_BOOST);
            String queryEscaped = QueryParserBase.escape(query.getQuery());
            Query queryParsed = apiParser.parse(queryEscaped);
            mainQuery.add(queryParsed, BooleanClause.Occur.MUST);
            return Optional.of(mainQuery.build());
        }
        return Optional.empty();
    }

    private Optional<BooleanQuery> buildWildcardQuery(io.gravitee.rest.api.service.search.query.Query query) throws ParseException {
        if (!isBlank(query.getQuery())) {
            BooleanQuery.Builder mainQuery = buildApiQuery(query);

            MultiFieldQueryParser apiParser = new MultiFieldQueryParser(API_FIELD_SEARCH, new KeywordAnalyzer(), API_FIELD_BOOST);
            apiParser.setAllowLeadingWildcard(true);
            apiParser.setFuzzyMinSim(0.6f);
            String queryEscaped = QueryParserBase.escape(query.getQuery());
            Query queryParsed = apiParser.parse(queryEscaped);
            mainQuery.add(queryParsed, BooleanClause.Occur.SHOULD);
            mainQuery.add(buildApiFields(query.getQuery(), BooleanClause.Occur.SHOULD), BooleanClause.Occur.MUST);
            return Optional.of(mainQuery.build());
        }
        return Optional.empty();
    }

    @Override
    public SearchResult search(io.gravitee.rest.api.service.search.query.Query query) throws TechnicalException {
        try {
            Optional<BooleanQuery> filtersQuery = this.buildExplicitQuery(query);
            Optional<BooleanQuery> exactMatchQuery = this.buildExactMatchQuery(query);
            Optional<BooleanQuery> wildcardQuery = this.buildWildcardQuery(query);
            Optional<BooleanQuery> idsQuery = this.buildIdsQuery(query);

            BooleanQuery.Builder apiQuery = new BooleanQuery.Builder();

            filtersQuery.ifPresent(query1 -> apiQuery.add(query1, BooleanClause.Occur.MUST));
            exactMatchQuery.ifPresent(query1 -> apiQuery.add(new BoostQuery(query1, 4.0f), BooleanClause.Occur.SHOULD));
            wildcardQuery.ifPresent(query1 -> apiQuery.add(query1, BooleanClause.Occur.SHOULD));
            idsQuery.ifPresent(query1 -> apiQuery.add(query1, BooleanClause.Occur.SHOULD));

            return this.search(apiQuery.build(), query.getSort());
        } catch (ParseException pe) {
            logger.error("Invalid query to search for API documents", pe);
            throw new TechnicalException("Invalid query to search for API documents", pe);
        }
    }

    private Optional<BooleanQuery> buildExplicitQuery(io.gravitee.rest.api.service.search.query.Query query) {
        BooleanQuery.Builder filtersQuery = buildApiQuery(query);
        String rest = completeQueryWithFilters(query, filtersQuery);
        if (!rest.equals(query.getQuery())) {
            query.setQuery(rest);
            return Optional.of(filtersQuery.build());
        }
        return Optional.empty();
    }

    protected String completeQueryWithFilters(io.gravitee.rest.api.service.search.query.Query query, BooleanQuery.Builder mainQuery) {
        if (isBlank(query.getQuery())) {
            return "";
        }
        try {
            BooleanQuery.Builder restQuery = new BooleanQuery.Builder();
            Set<String> rest = appendExplicitFilters(query.getQuery(), mainQuery, restQuery);
            BooleanQuery build = restQuery.build();
            if (build.clauses().size() > 0) {
                mainQuery.add(build, build.clauses().get(0).getOccur());
            }
            if (rest != null) {
                return rest.stream().collect(Collectors.joining(" "));
            } else {
                return "";
            }
        } catch (ParseException e) {
            e.printStackTrace();
            return query.getQuery();
        }
    }

    protected Set<String> appendExplicitFilters(String query, BooleanQuery.Builder mainQuery, BooleanQuery.Builder restQuery)
        throws ParseException {
        QueryParser parser = new QueryParser("", new KeywordAnalyzer());
        parser.setAllowLeadingWildcard(true);
        org.apache.lucene.search.Query parse = parser.parse(query);
        if (hasExplicitFilter(query)) {
            return appendExplicitFilters(parse, mainQuery, restQuery, null);
        }
        return Collections.singleton(query);
    }

    private Set<String> appendExplicitFilters(
        org.apache.lucene.search.Query query,
        BooleanQuery.Builder mainQuery,
        BooleanQuery.Builder restQuery,
        BooleanClause clause
    ) {
        Set<String> rest = new HashSet<>();
        BooleanClause.Occur currentOccur = clause != null ? clause.getOccur() : BooleanClause.Occur.FILTER;
        if (query instanceof TermQuery) {
            TermQuery tQuery = (TermQuery) query;
            Term term = tQuery.getTerm();
            if (Arrays.stream(AUTHORIZED_EXPLICIT_FILTER).anyMatch(field -> field.equals(term.field()))) {
                mainQuery.add(buildQueryFilter(term), currentOccur);
            } else if (clause != null) {
                restQuery.add(buildApiFields(term.text(), BooleanClause.Occur.SHOULD), clause.getOccur());
            } else {
                rest.add(term.text());
            }
        } else if (query instanceof BooleanQuery) {
            BooleanQuery bQuery = (BooleanQuery) query;
            List<BooleanClause> clauses = bQuery.clauses();
            if (!clauses.isEmpty()) {
                BooleanQuery.Builder subQuery = new BooleanQuery.Builder();
                for (BooleanClause _clause : clauses) {
                    Query innerQuery = _clause.getQuery();
                    Set<String> innerRest = appendExplicitFilters(innerQuery, subQuery, restQuery, _clause);
                    if (innerRest == null) {
                        return null;
                    } else {
                        rest.addAll(innerRest);
                    }
                }
                mainQuery.add(subQuery.build(), BooleanClause.Occur.FILTER);
            }
        } else if (query instanceof WildcardQuery) {
            WildcardQuery wQuery = (WildcardQuery) query;
            Term term = wQuery.getTerm();
            if (Arrays.stream(AUTHORIZED_EXPLICIT_FILTER).anyMatch(field -> field.equals(term.field()))) {
                mainQuery.add(wQuery, currentOccur);
            } else {
                rest.add(term.text());
            }
        } else { //TODO support more lucene query types
            return null;
        }
        return rest;
    }

    private Query buildQueryFilter(Term term) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        String field = term.field();
        String text = term.text();
        if (FIELD_CATEGORIES.equals(term.field())) {
            text = formatCategoryField(term.text());
        } else if (!FIELD_PATHS.equals(term.field()) && !FIELD_TAGS.equals(term.field())) {
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
        return category.toLowerCase().replaceAll(" ", "-");
    }

    private Query toWildcard(String field, String query) {
        return new WildcardQuery(new Term(field, '*' + query + '*'));
    }

    private BooleanQuery buildApiFields(String query, BooleanClause.Occur occur, Query... queries) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        if (queries != null) {
            for (Query q : queries) {
                builder.add(q, occur);
            }
        }

        String[] tokens = query.split(" ");
        for (String token : tokens) {
            builder
                .add(new BoostQuery(toWildcard(FIELD_NAME, token), 12.0f), occur)
                .add(new BoostQuery(toWildcard(FIELD_NAME_LOWERCASE, token.toLowerCase()), 10.0f), occur)
                .add(new BoostQuery(toWildcard(FIELD_PATHS, token), 8.0f), occur)
                .add(toWildcard(FIELD_DESCRIPTION, token), occur)
                .add(toWildcard(FIELD_DESCRIPTION_LOWERCASE, token.toLowerCase()), occur)
                .add(toWildcard(FIELD_HOSTS, token), occur)
                .add(toWildcard(FIELD_LABELS, token), occur)
                .add(toWildcard(FIELD_CATEGORIES, token), occur)
                .add(toWildcard(FIELD_TAGS, token), occur)
                .add(toWildcard(FIELD_METADATA, token), occur);
        }
        return builder.build();
    }

    private BooleanQuery buildEnvCriteria() {
        return new BooleanQuery.Builder()
            .add(
                new TermQuery(new Term(FIELD_REFERENCE_TYPE, GraviteeContext.ReferenceContextType.ENVIRONMENT.name())),
                BooleanClause.Occur.FILTER
            )
            .add(new TermQuery(new Term(FIELD_REFERENCE_ID, GraviteeContext.getCurrentEnvironmentOrDefault())), BooleanClause.Occur.FILTER)
            .build();
    }

    private boolean hasExplicitFilter(String query) {
        if (query != null) {
            return Arrays.asList(AUTHORIZED_EXPLICIT_FILTER).stream().anyMatch(field -> query.contains(field + ":"));
        }
        return false;
    }

    @Override
    public boolean handle(Class<? extends Indexable> source) {
        return source.isAssignableFrom(ApiEntity.class);
    }
}
