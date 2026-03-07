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
package io.gravitee.repository.elasticsearch.v4.log.adapter.connection;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchConnectionLogErrorKeysResponseAdapterTest {

    @Test
    void should_return_empty_list_when_response_is_null() {
        assertThat(SearchConnectionLogErrorKeysResponseAdapter.adapt(null)).isEmpty();
    }

    @Test
    void should_return_empty_list_when_aggregations_are_missing() {
        SearchResponse response = new SearchResponse();
        assertThat(SearchConnectionLogErrorKeysResponseAdapter.adapt(response)).isEmpty();
    }

    @Test
    void should_return_empty_list_when_error_keys_aggregation_is_missing() {
        SearchResponse response = new SearchResponse();
        response.setAggregations(Map.of("other_agg", new Aggregation()));
        assertThat(SearchConnectionLogErrorKeysResponseAdapter.adapt(response)).isEmpty();
    }

    @Test
    void should_adapt_search_response_with_buckets() {
        // Given
        SearchResponse response = new SearchResponse();
        Aggregation aggregation = new Aggregation();

        ObjectNode bucket1 = JsonNodeFactory.instance.objectNode();
        bucket1.put("key", "GATEWAY_CLIENT_CONNECTION_ERROR");
        bucket1.put("doc_count", 10);

        ObjectNode bucket2 = JsonNodeFactory.instance.objectNode();
        bucket2.put("key", "NO_ENDPOINT_FOUND");
        bucket2.put("doc_count", 5);

        aggregation.setBuckets(List.of(bucket1, bucket2));
        response.setAggregations(Map.of("error_keys", aggregation));

        // When
        List<String> result = SearchConnectionLogErrorKeysResponseAdapter.adapt(response);

        // Then
        assertThat(result).containsExactly("GATEWAY_CLIENT_CONNECTION_ERROR", "NO_ENDPOINT_FOUND");
    }

    @Test
    void should_ignore_null_or_blank_keys() {
        // Given
        SearchResponse response = new SearchResponse();
        Aggregation aggregation = new Aggregation();

        ObjectNode bucket1 = JsonNodeFactory.instance.objectNode();
        bucket1.set("key", JsonNodeFactory.instance.nullNode());

        ObjectNode bucket2 = JsonNodeFactory.instance.objectNode();
        bucket2.put("key", "");

        ObjectNode bucket3 = JsonNodeFactory.instance.objectNode();
        bucket3.put("key", "VALID_KEY");

        aggregation.setBuckets(List.of(bucket1, bucket2, bucket3));
        response.setAggregations(Map.of("error_keys", aggregation));

        // When
        List<String> result = SearchConnectionLogErrorKeysResponseAdapter.adapt(response);

        // Then
        assertThat(result).containsExactly("VALID_KEY");
    }
}
