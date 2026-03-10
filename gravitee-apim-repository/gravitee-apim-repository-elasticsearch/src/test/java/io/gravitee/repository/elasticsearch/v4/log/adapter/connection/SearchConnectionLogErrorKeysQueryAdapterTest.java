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

import io.gravitee.repository.log.v4.model.connection.SearchConnectionLogErrorKeysQuery;
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
        String apiId = "my-api-id";
        SearchConnectionLogErrorKeysQuery query = SearchConnectionLogErrorKeysQuery.of(apiId, 1000L, 2000L);

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
                          { "term": { "api": "my-api-id" } },
                          { "term": { "api-id": "my-api-id" } }
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
        String apiId = "my-api-id";
        SearchConnectionLogErrorKeysQuery query = new SearchConnectionLogErrorKeysQuery(apiId, null, null, 50);

        // When
        String result = SearchConnectionLogErrorKeysQueryAdapter.adapt(query);

        // Then
        assertThatJson(result).node("aggs.error_keys.terms.size").isEqualTo(50);
    }

    @Test
    void should_build_elasticsearch_query_without_api_id() {
        SearchConnectionLogErrorKeysQuery query = new SearchConnectionLogErrorKeysQuery(null, 1000L, 2000L, 20);
        String result = SearchConnectionLogErrorKeysQueryAdapter.adapt(query);

        assertThatJson(result).node("query.bool.must").isArray().hasSize(1);
        assertThatJson(result).node("query.bool.must[0].range.@timestamp.gte").isEqualTo(1000L);
        assertThatJson(result).node("aggs.error_keys.terms.field").isEqualTo("error-key");
    }
}
