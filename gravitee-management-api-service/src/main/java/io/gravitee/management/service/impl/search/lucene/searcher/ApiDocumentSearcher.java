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

import io.gravitee.management.model.api.ApiEntity;
import io.gravitee.management.model.search.Indexable;
import io.gravitee.repository.exceptions.TechnicalException;
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
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class ApiDocumentSearcher extends AbstractDocumentSearcher {

    private final static String FIELD_API_TYPE_VALUE = "api";
    private final static String FIELD_PAGE_TYPE_VALUE = "page";

    private final static Map<String, Float> API_FIELD_BOOST = new HashMap<String, Float>() {
        {
            put("name", 12.0f);
            put("name_split", 10.0f);
            put("description", 8.0f);
        }
    };

    private final static Map<String, Float> PAGE_FIELD_BOOST = new HashMap<String, Float>() {
        {
            put("name", 10.0f);
            put("content", 5.0f);
        }
    };

    @Override
    public List<String> search(io.gravitee.management.service.search.query.Query query) throws TechnicalException {
        MultiFieldQueryParser apiParser = new MultiFieldQueryParser(new String[]{
                "name",
                "name_split",
                "description",
                "ownerName",
                "ownerMail",
                "labels",
                "tags",
                "views",
                "path",
                "path_split"
        }, analyzer, API_FIELD_BOOST);
        apiParser.setFuzzyMinSim(0.6f);
        apiParser.setAllowLeadingWildcard(true);

        QueryParser pageParser = new MultiFieldQueryParser(new String[]{
                "name",
                "content"
        }, analyzer, PAGE_FIELD_BOOST);
        pageParser.setFuzzyMinSim(0.6f);
        pageParser.setAllowLeadingWildcard(true);

        try {
            String inputQuery = QueryParserBase.escape(query.getQuery());
            Query parse = apiParser.parse(inputQuery);
            Query parsePage = pageParser.parse(inputQuery);

            Query apisFilter = getApisFilter(FIELD_ID, query.getFilters());

            // Search in API fields
            BooleanQuery.Builder apiQuery = new BooleanQuery.Builder();
            BooleanQuery.Builder apiFieldsQuery = new BooleanQuery.Builder();

            final String lowerCasedQuery = query.getQuery().toLowerCase();
            apiFieldsQuery.add(parse, BooleanClause.Occur.SHOULD);
            apiFieldsQuery.add(new WildcardQuery(new Term("name", '*' + lowerCasedQuery + '*')), BooleanClause.Occur.SHOULD);
            apiFieldsQuery.add(new WildcardQuery(new Term("path", '*' + lowerCasedQuery + '*')), BooleanClause.Occur.SHOULD);

            apiQuery.add(apiFieldsQuery.build(), BooleanClause.Occur.MUST);
            apiQuery.add(new TermQuery(new Term(FIELD_TYPE, FIELD_API_TYPE_VALUE)), BooleanClause.Occur.MUST);
            if (apisFilter != null) {
                apiQuery.add(apisFilter, BooleanClause.Occur.MUST);
            }

            // Search in page fields
            BooleanQuery.Builder pageQuery = new BooleanQuery.Builder();
            pageQuery.add(parsePage, BooleanClause.Occur.MUST);
            pageQuery.add(new TermQuery(new Term(FIELD_TYPE, FIELD_PAGE_TYPE_VALUE)), BooleanClause.Occur.MUST);

            apisFilter = getApisFilter(FIELD_API_TYPE_VALUE, query.getFilters());
            if (apisFilter != null) {
                pageQuery.add(apisFilter, BooleanClause.Occur.MUST);
            } else {
                pageQuery.add(new DocValuesFieldExistsQuery(FIELD_API_TYPE_VALUE), BooleanClause.Occur.MUST);
            }

            BooleanQuery.Builder mainQuery = new BooleanQuery.Builder();
            mainQuery.add(new BoostQuery(apiQuery.build(), 2.0f), BooleanClause.Occur.SHOULD);
            //mainQuery.add(new BoostQuery(pathQuery.build(), 4.0f), BooleanClause.Occur.SHOULD);
            mainQuery.add(pageQuery.build(), BooleanClause.Occur.SHOULD);

            // Manage filters
            if (query.getFilters() != null) {
                BooleanQuery.Builder filtersQuery = new BooleanQuery.Builder();
                final boolean[] hasClause = {false};
                query.getFilters().forEach(new BiConsumer<String, Object>() {
                    @Override
                    public void accept(String field, Object value) {
                        if (Collection.class.isAssignableFrom(value.getClass())) {
                        } else {
                            filtersQuery.add(new TermQuery(new Term(field, QueryParserBase.escape((String) value))), BooleanClause.Occur.MUST);
                            hasClause[0] = true;
                        }
                    }
                });

                if (hasClause[0]) {
                    mainQuery.add(filtersQuery.build(), BooleanClause.Occur.MUST);
                }

            }
            return search(mainQuery.build());
        } catch (ParseException pe) {
            logger.error("Invalid query to search for API documents", pe);
            throw new TechnicalException("Invalid query to search for API documents", pe);
        }
    }

    private Query getApisFilter(String field, Map<String, Object> filters) {
        Object filter = filters.get(FIELD_API_TYPE_VALUE);
        if (filter != null) {
            BooleanQuery.Builder filterApisQuery = new BooleanQuery.Builder();

            ((Collection)filter)
                    .stream()
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
