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
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.search.Indexable;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.impl.search.SearchResult;
import io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer;
import io.gravitee.rest.api.service.impl.search.lucene.transformer.PageDocumentTransformer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.apache.lucene.search.*;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static io.gravitee.rest.api.service.impl.search.lucene.transformer.ApiDocumentTransformer.*;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiDocumentSearcher extends AbstractDocumentSearcher {

    private static final String FIELD_API_TYPE_VALUE = "api";
    private static final String FIELD_PAGE_TYPE_VALUE = "page";

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
        FIELD_OWNER,
        FIELD_OWNER_MAIL,
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
    };

    @Override
    public SearchResult search(io.gravitee.rest.api.service.search.query.Query query) throws TechnicalException {
        MultiFieldQueryParser apiParser = new MultiFieldQueryParser(API_FIELD_SEARCH, analyzer, API_FIELD_BOOST);
        apiParser.setFuzzyMinSim(0.6f);
        apiParser.setAllowLeadingWildcard(true);

        QueryParser pageParser = new MultiFieldQueryParser(PAGE_FIELD_SEARCH, analyzer, PAGE_FIELD_BOOST);
        pageParser.setFuzzyMinSim(0.6f);
        pageParser.setAllowLeadingWildcard(true);

        try {
            String inputQuery = QueryParserBase.escape(query.getQuery());
            Query parsePage = pageParser.parse(inputQuery);

            BooleanQuery envCriteria = buildEnvCriteria();

            // Search in API fields
            BooleanQuery.Builder apiQuery = new BooleanQuery.Builder()
                .add(buildApiFields(query, apiParser.parse(inputQuery)), BooleanClause.Occur.MUST)
                .add(new TermQuery(new Term(FIELD_TYPE, FIELD_API_TYPE_VALUE)), BooleanClause.Occur.MUST)
                .add(envCriteria, BooleanClause.Occur.FILTER);

            Query apisFilter = getApisFilter(FIELD_ID, query.getFilters());
            if (apisFilter != null) {
                apiQuery.add(apisFilter, BooleanClause.Occur.MUST);
            }

            // Search in page fields
            BooleanQuery.Builder pageFieldsQuery = new BooleanQuery.Builder()
                .add(parsePage, BooleanClause.Occur.SHOULD)
                .add(toWildcard(PageDocumentTransformer.FIELD_NAME, query.getQuery()), BooleanClause.Occur.SHOULD)
                .add(toWildcard(PageDocumentTransformer.FIELD_NAME_LOWERCASE, query.getQuery().toLowerCase()), BooleanClause.Occur.SHOULD)
                .add(toWildcard(PageDocumentTransformer.FIELD_CONTENT, query.getQuery()), BooleanClause.Occur.SHOULD);

            BooleanQuery.Builder pageQuery = new BooleanQuery.Builder()
                .add(pageFieldsQuery.build(), BooleanClause.Occur.MUST)
                .add(new TermQuery(new Term(FIELD_TYPE, FIELD_PAGE_TYPE_VALUE)), BooleanClause.Occur.MUST)
                .add(envCriteria, BooleanClause.Occur.FILTER);

            Query pageApisFilter = getApisFilter(FIELD_API_TYPE_VALUE, query.getFilters());
            if (pageApisFilter != null) {
                pageQuery.add(pageApisFilter, BooleanClause.Occur.MUST);
            } else {
                pageQuery.add(new DocValuesFieldExistsQuery(FIELD_API_TYPE_VALUE), BooleanClause.Occur.MUST);
            }

            BooleanQuery.Builder mainQuery = new BooleanQuery.Builder()
                .add(new BoostQuery(apiQuery.build(), 2.0f), BooleanClause.Occur.SHOULD)
                .add(pageQuery.build(), BooleanClause.Occur.SHOULD);

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

    private Query toWildcard(String field, String query) {
        return new WildcardQuery(new Term(field, '*' + query + '*'));
    }

    private BooleanQuery buildApiFields(io.gravitee.rest.api.service.search.query.Query query, Query queryParsed) {
        String _query = query.getQuery();
        return new BooleanQuery.Builder()
            .add(queryParsed, BooleanClause.Occur.SHOULD)
            .add(toWildcard(FIELD_NAME, _query), BooleanClause.Occur.SHOULD)
            .add(toWildcard(FIELD_NAME_LOWERCASE, _query.toLowerCase()), BooleanClause.Occur.SHOULD)
            .add(toWildcard(FIELD_PATHS, _query), BooleanClause.Occur.SHOULD)
            .add(toWildcard(FIELD_HOSTS, _query), BooleanClause.Occur.SHOULD)
            .add(toWildcard(FIELD_LABELS, _query), BooleanClause.Occur.SHOULD)
            .add(toWildcard(FIELD_CATEGORIES, _query), BooleanClause.Occur.SHOULD)
            .add(toWildcard(FIELD_TAGS, _query), BooleanClause.Occur.SHOULD)
            .add(toWildcard(FIELD_METADATA, _query), BooleanClause.Occur.SHOULD)
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
            return document.get(FIELD_API_TYPE_VALUE);
        }

        return null;
    }
}
