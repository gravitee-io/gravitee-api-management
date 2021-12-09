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
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
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
            put(FIELD_NAME, 12.0f);
            put(FIELD_NAME_LOWERCASE, 12.0f);
            put(FIELD_NAME_SPLIT, 10.0f);
            put(FIELD_PATHS, 10.0f);
            put(FIELD_HOSTS, 10.0f);
            put(FIELD_LABELS, 8.0f);
            put(FIELD_DESCRIPTION, 6.0f);
            put(FIELD_METADATA, 4.0f);
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
    };

    @Override
    public SearchResult search(io.gravitee.rest.api.service.search.query.Query query) throws TechnicalException {
        BooleanQuery.Builder mainQuery = new BooleanQuery.Builder();
        try {
            String rest = completeQueryWithFilters(query, mainQuery);

            MultiFieldQueryParser apiParser = new MultiFieldQueryParser(API_FIELD_SEARCH, analyzer, API_FIELD_BOOST);
            apiParser.setFuzzyMinSim(0.6f);
            apiParser.setAllowLeadingWildcard(true);

            QueryParser pageParser = new MultiFieldQueryParser(PAGE_FIELD_SEARCH, analyzer, PAGE_FIELD_BOOST);
            pageParser.setFuzzyMinSim(0.6f);
            pageParser.setAllowLeadingWildcard(true);

            BooleanQuery.Builder pageFieldsQuery = new BooleanQuery.Builder();
            BooleanQuery.Builder apiQuery = new BooleanQuery.Builder();
            String inputQuery = QueryParserBase.escape(rest);
            if (!inputQuery.isEmpty()) {
                Query parsePage = pageParser.parse(inputQuery);
                pageFieldsQuery.add(parsePage, BooleanClause.Occur.SHOULD);
                Query parseApi = apiParser.parse(inputQuery);
                apiQuery.add(buildApiFields(rest, parseApi, BooleanClause.Occur.SHOULD), BooleanClause.Occur.MUST);
            }

            BooleanQuery envCriteria = buildEnvCriteria();

            // Search in API fields
            apiQuery
                .add(new TermQuery(new Term(FIELD_TYPE, FIELD_API_TYPE_VALUE)), BooleanClause.Occur.MUST)
                .add(envCriteria, BooleanClause.Occur.FILTER);

            Query apisFilter = getApisFilter(FIELD_ID, query.getFilters());
            if (apisFilter != null) {
                apiQuery.add(apisFilter, BooleanClause.Occur.MUST);
            }

            // Search in page fields
            pageFieldsQuery
                .add(toWildcard(PageDocumentTransformer.FIELD_NAME, rest), BooleanClause.Occur.SHOULD)
                .add(toWildcard(PageDocumentTransformer.FIELD_NAME_LOWERCASE, rest.toLowerCase()), BooleanClause.Occur.SHOULD)
                .add(toWildcard(PageDocumentTransformer.FIELD_CONTENT, rest), BooleanClause.Occur.SHOULD);

            BooleanQuery.Builder pageQuery = new BooleanQuery.Builder()
                .add(pageFieldsQuery.build(), BooleanClause.Occur.MUST)
                .add(new TermQuery(new Term(FIELD_TYPE, PageDocumentTransformer.FIELD_TYPE_VALUE)), BooleanClause.Occur.MUST)
                .add(new TermQuery(new Term(FIELD_REFERENCE_TYPE, FIELD_API_TYPE_VALUE)), BooleanClause.Occur.MUST);

            Query pageApisFilter = getApisFilter(FIELD_REFERENCE_ID, query.getFilters());
            if (pageApisFilter != null) {
                pageQuery.add(pageApisFilter, BooleanClause.Occur.MUST);
            }

            BooleanQuery.Builder pageOrApiQuery = new BooleanQuery.Builder();
            pageOrApiQuery
                .add(new BoostQuery(apiQuery.build(), 2.0f), BooleanClause.Occur.SHOULD)
                .add(pageQuery.build(), BooleanClause.Occur.SHOULD);

            mainQuery.add(pageOrApiQuery.build(), BooleanClause.Occur.MUST);

            // Manage filters
            if (query.getFilters() != null) {
                BooleanQuery.Builder filtersQuery = new BooleanQuery.Builder();
                final boolean[] hasClause = { false };
                query
                    .getFilters()
                    .forEach(
                        (BiConsumer<String, Object>) (field, value) -> {
                            if (!Collection.class.isAssignableFrom(value.getClass())) {
                                filtersQuery.add(
                                    new TermQuery(new Term(field, QueryParserBase.escape((String) value))),
                                    BooleanClause.Occur.MUST
                                );
                                hasClause[0] = true;
                            }
                        }
                    );

                if (hasClause[0]) {
                    filtersQuery.add(envCriteria, BooleanClause.Occur.FILTER);
                    mainQuery.add(filtersQuery.build(), BooleanClause.Occur.MUST);
                }
            }
            return search(mainQuery.build());
        } catch (ParseException pe) {
            logger.error("Invalid query to search for API documents", pe);
            throw new TechnicalException("Invalid query to search for API documents", pe);
        }
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
                mainQuery.add(new TermQuery(buildTermFilter(term)), currentOccur);
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

    private Term buildTermFilter(Term term) {
        if (FIELD_CATEGORIES.equals(term.field())) {
            return new Term(term.field(), formatCategoryField(term.text()));
        } else {
            return new Term(term.field() + "_lowercase", term.text().toLowerCase());
        }
    }

    public static String formatCategoryField(String category) {
        // Actually we index hrid categories...
        return category.toLowerCase().replaceAll(" ", "-");
    }

    private Query toWildcard(String field, String query) {
        return new WildcardQuery(new Term(field, '*' + query + '*'));
    }

    private BooleanQuery buildApiFields(String query, BooleanClause.Occur occur) {
        return buildApiFields(query, null, occur);
    }

    private BooleanQuery buildApiFields(String query, Query queryParsed, BooleanClause.Occur occur) {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        if (queryParsed != null) {
            builder.add(queryParsed, occur);
        }

        return builder
            .add(toWildcard(FIELD_NAME, query), occur)
            .add(toWildcard(FIELD_NAME_LOWERCASE, query.toLowerCase()), occur)
            .add(toWildcard(FIELD_PATHS, query), occur)
            .add(toWildcard(FIELD_DESCRIPTION, query), occur)
            .add(toWildcard(FIELD_DESCRIPTION_LOWERCASE, query.toLowerCase()), occur)
            .add(toWildcard(FIELD_HOSTS, query), occur)
            .add(toWildcard(FIELD_LABELS, query), occur)
            .add(toWildcard(FIELD_CATEGORIES, query), occur)
            .add(toWildcard(FIELD_TAGS, query), occur)
            .add(toWildcard(FIELD_METADATA, query), occur)
            .build();
    }

    private BooleanQuery buildEnvCriteria() {
        return new BooleanQuery.Builder()
            .add(
                new TermQuery(new Term(FIELD_REFERENCE_TYPE, GraviteeContext.ReferenceContextType.ENVIRONMENT.name())),
                BooleanClause.Occur.MUST
            )
            .add(new TermQuery(new Term(FIELD_REFERENCE_ID, GraviteeContext.getCurrentEnvironmentOrDefault())), BooleanClause.Occur.MUST)
            .build();
    }

    private Query getApisFilter(String field, Map<String, Object> filters) {
        Object filter = filters.get(FIELD_API_TYPE_VALUE);
        if (filter != null) {
            BooleanQuery.Builder filterApisQuery = new BooleanQuery.Builder();
            ((Collection) filter).stream()
                .forEach(value1 -> filterApisQuery.add(new TermQuery(new Term(field, (String) value1)), BooleanClause.Occur.SHOULD));
            return filterApisQuery.build();
        }
        return null;
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

    @Override
    protected String getReference(Document document) {
        String type = document.get(FIELD_TYPE);
        if (FIELD_API_TYPE_VALUE.equals(type)) {
            return document.get(FIELD_ID);
        } else if (FIELD_PAGE_TYPE_VALUE.equals(type)) {
            return document.get(FIELD_REFERENCE_ID);
        }

        return null;
    }
}
