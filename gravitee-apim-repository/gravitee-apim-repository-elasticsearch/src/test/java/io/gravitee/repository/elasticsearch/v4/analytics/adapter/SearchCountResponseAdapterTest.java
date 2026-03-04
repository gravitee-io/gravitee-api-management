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
package io.gravitee.repository.elasticsearch.v4.analytics.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.elasticsearch.model.SearchHits;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.elasticsearch.model.TotalHits;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchCountResponseAdapterTest {

    @Test
    void should_return_empty_when_search_hits_is_null() {
        var response = new SearchResponse();
        assertThat(SearchCountResponseAdapter.adapt(response)).isEmpty();
    }

    @Test
    void should_return_empty_when_total_is_null() {
        var response = new SearchResponse();
        var hits = new SearchHits();
        response.setSearchHits(hits);
        assertThat(SearchCountResponseAdapter.adapt(response)).isEmpty();
    }

    @Test
    void should_return_zero_count_when_no_documents_match() {
        var response = buildResponseWithTotal(0L);
        assertThat(SearchCountResponseAdapter.adapt(response)).hasValue(0L);
    }

    @Test
    void should_return_count_from_total_hits() {
        var response = buildResponseWithTotal(12345L);
        assertThat(SearchCountResponseAdapter.adapt(response)).hasValue(12345L);
    }

    @Test
    void should_return_large_count() {
        var response = buildResponseWithTotal(999_999_999L);
        assertThat(SearchCountResponseAdapter.adapt(response)).hasValue(999_999_999L);
    }

    private static SearchResponse buildResponseWithTotal(long total) {
        var response = new SearchResponse();
        var hits = new SearchHits();
        hits.setTotal(new TotalHits(total));
        response.setSearchHits(hits);
        return response;
    }
}
