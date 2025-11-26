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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.search.query.Query;
import io.gravitee.rest.api.service.search.query.QueryBuilder;
import org.apache.lucene.search.BooleanQuery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiDocumentSearcherTest {

    ApiDocumentSearcher searcher = new ApiDocumentSearcher(null);

    @Test
    public void shouldCompleteQueryWithFilters() {
        Query<ApiEntity> query = QueryBuilder.create(ApiEntity.class).setQuery("name: Foobar AND name: \"Foo Foo\"").build();
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        assertEquals("", searcher.completeQueryWithFilters(query, builder));
        assertEquals(
            "#(+(name_lowercase:\"foobar\" name_lowercase:foobar) +(name_lowercase:\"foo foo\" name_lowercase:foo foo))",
            builder.build().toString()
        );
    }

    @Test
    public void shouldCompleteQueryWithFiltersAndWildcard() {
        Query<ApiEntity> query = QueryBuilder.create(ApiEntity.class).setQuery("name: *").build();
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        assertEquals("", searcher.completeQueryWithFilters(query, builder));
        assertEquals("#name:*", builder.build().toString());
    }

    @Test
    public void shouldCompleteQueryWithFiltersAndText() {
        Query<ApiEntity> query = QueryBuilder.create(ApiEntity.class).setQuery("categories:Sports AND Cycling").build();
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        assertEquals("", searcher.completeQueryWithFilters(query, builder));
        assertEquals(
            "#(+(categories:\"sports\" categories:sports)) +(+((name:*Cycling*)^20.0 (name_lowercase:*cycling*)^18.0 (name_sorted:*cycling*)^15.0 (name:*Cycling*)^12.0 (name_lowercase:*cycling*)^10.0 (paths:*Cycling*)^8.0 (paths_lowercase:*cycling*)^7.0 description:*Cycling* description_lowercase:*cycling* hosts:*Cycling* hosts_lowercase:*cycling* ownerName:*Cycling* ownerName_lowercase:*cycling* labels:*Cycling* labels_lowercase:*cycling* categories:*Cycling* tags:*Cycling* metadata:*Cycling*))",
            builder.build().toString()
        );
    }

    @Test
    public void shouldCompleteQueryWithOrigin() {
        Query query = QueryBuilder.create(ApiEntity.class).setQuery("origin:kubernetes").build();
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        assertEquals("", searcher.completeQueryWithFilters(query, builder));
        assertEquals("#(origin:\"kubernetes\" origin:kubernetes)", builder.build().toString());
    }

    @Test
    public void shouldCompleteQueryWithHasHealthCheck() {
        Query query = QueryBuilder.create(ApiEntity.class).setQuery("has_health_check:true").build();
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        assertEquals("", searcher.completeQueryWithFilters(query, builder));
        assertEquals("#(has_health_check:\"true\" has_health_check:true)", builder.build().toString());
    }

    @Test
    public void should_convert_description_to_lowercase() {
        var query = QueryBuilder.create(ApiEntity.class).setQuery("description:TestValue").build();
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        assertThat(searcher.completeQueryWithFilters(query, builder)).isEmpty();
        assertThat(builder.build()).hasToString("#(description_lowercase:\"testvalue\" description_lowercase:testvalue)");
    }

    @Test
    public void should_convert_owner_to_lowercase() {
        var query = QueryBuilder.create(ApiEntity.class).setQuery("ownerName:JohnDoe").build();
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        assertThat(searcher.completeQueryWithFilters(query, builder)).isEmpty();
        assertThat(builder.build()).hasToString("#(ownerName_lowercase:\"johndoe\" ownerName_lowercase:johndoe)");
    }

    @Test
    public void should_convert_labels_to_lowercase() {
        var query = QueryBuilder.create(ApiEntity.class).setQuery("labels:MyLabel").build();
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        assertThat(searcher.completeQueryWithFilters(query, builder)).isEmpty();
        assertThat(builder.build()).hasToString("#(labels_lowercase:\"mylabel\" labels_lowercase:mylabel)");
    }

    @Test
    public void should_include_paths_lowercase_in_text_search() {
        var query = QueryBuilder.create(ApiEntity.class).setQuery("TestPath").build();
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        String remaining = searcher.completeQueryWithFilters(query, builder);
        assertThat(remaining).isEqualTo("TestPath");
    }
}
