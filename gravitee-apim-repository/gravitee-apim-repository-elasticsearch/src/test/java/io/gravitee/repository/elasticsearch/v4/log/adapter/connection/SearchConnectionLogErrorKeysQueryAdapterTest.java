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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.repository.log.v4.model.connection.SearchConnectionLogErrorKeysQuery;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchConnectionLogErrorKeysQueryAdapterTest {

    @Test
    void should_build_elasticsearch_query() {
        // Given
        SearchConnectionLogErrorKeysQuery query = SearchConnectionLogErrorKeysQuery.of(List.of("api-1", "api-2"), 1000L, 2000L);

        // When
        String result = SearchConnectionLogErrorKeysQueryAdapter.adapt(query);

        // Then
        assertThatJson(result).isEqualTo(
            """
            {
              "size": 0,
              "query": {
                "bool": {
                  "must": [
                    {
                      "bool": {
                        "should": [
                          { "terms": { "api": ["api-1", "api-2"] } },
                          { "terms": { "api-id": ["api-1", "api-2"] } }
                        ]
                      }
                    },
                    {
                      "range": {
                        "@timestamp": {
                          "gte": 1000,
                          "lte": 2000
                        }
                      }
                    }
                  ]
                }
              },
              "aggs": {
                "error_keys": {
                  "terms": {
                    "field": "error-key",
                    "size": 100
                  }
                }
              }
            }
            """
        );
    }

    @Test
    void should_build_query_with_custom_max_buckets() {
        // Given
        SearchConnectionLogErrorKeysQuery query = new SearchConnectionLogErrorKeysQuery(List.of("my-api-id"), null, null, 50);

        // When
        String result = SearchConnectionLogErrorKeysQueryAdapter.adapt(query);

        // Then
        assertThatJson(result).node("aggs.error_keys.terms.size").isEqualTo(50);
    }

    @Test
    void should_match_no_document_when_no_api_id_is_given() {
        SearchConnectionLogErrorKeysQuery query = new SearchConnectionLogErrorKeysQuery(List.of(), 1000L, 2000L, 20);
        String result = SearchConnectionLogErrorKeysQueryAdapter.adapt(query);

        assertThatJson(result).node("query.bool.must").isArray().hasSize(2);
        assertThatJson(result).node("query.bool.must[0].bool.should").isEqualTo("[{\"terms\":{\"api\":[]}},{\"terms\":{\"api-id\":[]}}]");
    }

    @Test
    void should_reject_a_query_without_api_ids() {
        assertThatThrownBy(() -> SearchConnectionLogErrorKeysQuery.of(null, 1000L, 2000L)).isInstanceOf(NullPointerException.class);
    }
}
