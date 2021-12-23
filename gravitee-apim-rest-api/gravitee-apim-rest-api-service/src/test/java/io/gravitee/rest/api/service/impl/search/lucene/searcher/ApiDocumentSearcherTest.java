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

import static org.junit.Assert.assertEquals;

import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.search.query.Query;
import io.gravitee.rest.api.service.search.query.QueryBuilder;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanQuery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiDocumentSearcherTest {

    ApiDocumentSearcher searcher = new ApiDocumentSearcher();

    @Test
    public void shouldCompleteQueryWithFilters() {
        Query query = QueryBuilder.create(ApiEntity.class).setQuery("name: Foobar AND name: \"Foo Foo\"").build();
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        assertEquals("", searcher.completeQueryWithFilters(query, builder));
        assertEquals(
            "#(+(name_lowercase:\"foobar\" name_lowercase:foobar) +(name_lowercase:\"foo foo\" name_lowercase:foo foo))",
            builder.build().toString()
        );
    }

    @Test
    public void shouldCompleteQueryWithFiltersAndWildcard() {
        Query query = QueryBuilder.create(ApiEntity.class).setQuery("name: *").build();
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        assertEquals("", searcher.completeQueryWithFilters(query, builder));
        assertEquals("#name:*", builder.build().toString());
    }

    @Test
    public void shouldCompleteQueryWithFiltersAndText() throws ParseException {
        Query query = QueryBuilder.create(ApiEntity.class).setQuery("categories:Sports AND Cycling").build();
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        assertEquals("", searcher.completeQueryWithFilters(query, builder));
        assertEquals(
            "#(+(categories:\"sports\" categories:sports)) +(+((name:*Cycling*)^12.0 (name_lowercase:*cycling*)^10.0 (paths:*Cycling*)^8.0 description:*Cycling* description_lowercase:*cycling* hosts:*Cycling* labels:*Cycling* categories:*Cycling* tags:*Cycling* metadata:*Cycling*))",
            builder.build().toString()
        );
    }
}
