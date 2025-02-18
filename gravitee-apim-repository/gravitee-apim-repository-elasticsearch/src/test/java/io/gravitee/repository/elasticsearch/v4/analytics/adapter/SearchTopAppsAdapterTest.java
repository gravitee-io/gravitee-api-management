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

import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchTopHitsAdapter.API_TOP_HITS_COUNT;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchTopHitsAdapter.HITS_COUNT;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.analytics.TopHitsQueryCriteria;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SearchTopAppsAdapterTest {

    private static final long FROM = 1728992401566L;
    private static final long TO = 1729078801566L;
    private static final List<String> API_IDS = List.of("api-id-1", "api-id-2");

    @Nested
    class Query {

        @Test
        void should_build_top_apps_query_searching_for_top_ids_and_time_ranges() {
            var queryParams = new TopHitsQueryCriteria(API_IDS, FROM, TO);

            var result = SearchTopAppsAdapter.adaptQuery(queryParams);

            assertThatJson(result).isEqualTo(TOP_APPS_QUERY_WITH_API_IDS_AND_TIME_RANGES);
        }

        @Test
        void should_build_top_apps_query_with_empty_id_filter_array_for_null_query_params() {
            TopHitsQueryCriteria queryParams = null;

            var result = SearchTopAppsAdapter.adaptQuery(queryParams);

            assertThatJson(result).isEqualTo(TOP_APPS_QUERY_WITH_NULL_TIME_RANGE_PARAMS);
        }

        @Test
        void should_build_top_apps_query_with_empty_id_filter_array_for_empty_api_ids_query_params() {
            var queryParams = new TopHitsQueryCriteria(List.of(), FROM, TO);
            var result = SearchTopAppsAdapter.adaptQuery(queryParams);

            assertThatJson(result).isEqualTo(TOP_APPS_QUERY_WITH_EMPTY_APP_ID_LIST);
        }

        private static final String TOP_APPS_QUERY_WITH_API_IDS_AND_TIME_RANGES =
            """
                 {
                     "size": 0,
                     "query": {
                         "bool": {
                             "filter": [
                                 {
                                     "terms": {
                                         "api-id": [
                                             "api-id-1",
                                             "api-id-2"
                                         ]
                                     }
                                 },
                                 {
                                     "range": {
                                         "@timestamp": {
                                             "gte": 1728992401566,
                                             "lte" : 1729078801566
                                         }
                                     }
                                 }
                             ]
                         }
                     },
                     "aggs": {
                         "app_top_hits_count": {
                             "terms": {
                                 "field": "application-id"
                             },
                             "aggs": {
                                 "hits_count": {
                                     "value_count": {
                                         "field": "application-id"
                                     }
                                 }
                             }
                         }
                     }
                 }\s
                \s""";

        private static final String TOP_APPS_QUERY_WITH_NULL_TIME_RANGE_PARAMS =
            """
                 {
                     "size": 0,
                     "query": {
                         "bool": {
                             "filter": [
                                 {
                                     "terms": {
                                         "api-id": []
                                     }
                                 }
                             ]
                         }
                     },
                     "aggs": {
                         "app_top_hits_count": {
                             "terms": {
                                 "field": "application-id"
                             },
                             "aggs": {
                                 "hits_count": {
                                     "value_count": {
                                         "field": "application-id"
                                     }
                                 }
                             }
                         }
                     }
                 }\s
                \s""";

        private static final String TOP_APPS_QUERY_WITH_EMPTY_APP_ID_LIST =
            """
                 {
                     "size": 0,
                     "query": {
                         "bool": {
                             "filter": [
                                 {
                                     "terms": {
                                         "api-id": []
                                     }
                                 },
                                 {
                                     "range": {
                                         "@timestamp": {
                                             "gte": 1728992401566,
                                             "lte" : 1729078801566
                                         }
                                     }
                                 }
                             ]
                         }
                     },
                     "aggs": {
                         "app_top_hits_count": {
                             "terms": {
                                 "field": "application-id"
                             },
                             "aggs": {
                                 "hits_count": {
                                     "value_count": {
                                         "field": "application-id"
                                     }
                                 }
                             }
                         }
                     }
                 }\s
                \s""";
    }

    @Nested
    class Response {

        private final ObjectMapper objectMapper = new ObjectMapper();
        private final SearchResponse searchResponse = new SearchResponse();

        @Test
        void should_return_empty_result_if_aggregations_is_null() {
            assertThat(SearchTopAppsAdapter.adaptResponse(searchResponse)).isEmpty();
        }

        @Test
        void should_return_empty_result_if_no_aggregation_exist() {
            searchResponse.setAggregations(Map.of());

            assertThat(SearchTopAppsAdapter.adaptResponse(searchResponse)).isEmpty();
        }

        @Test
        void should_return_empty_result_if_no_top_apps_count_aggregation_exist() {
            searchResponse.setAggregations(Map.of("fake_aggregation", new Aggregation()));

            assertThat(SearchTopAppsAdapter.adaptResponse(searchResponse)).isEmpty();
        }

        @Test
        void should_return_top_apps_aggregate() {
            final var APP_ID_1 = "app-id-1";
            final var APP_ID_1_COUNT = 35L;
            final var APP_ID_2 = "app-id-2";
            final var APP_ID_2_COUNT = 47L;
            final Aggregation topHitsCountAggregation = new Aggregation();

            searchResponse.setAggregations(Map.of(API_TOP_HITS_COUNT, topHitsCountAggregation));

            topHitsCountAggregation.setBuckets(
                List.of(createTopHitsBucket(APP_ID_1, APP_ID_1_COUNT), createTopHitsBucket(APP_ID_2, APP_ID_2_COUNT))
            );

            var result = SearchTopHitsAdapter.adaptResponse(searchResponse);

            assertThat(result)
                .hasValueSatisfying(topHits ->
                    assertThat(topHits.getTopHitsCounts()).containsEntry(APP_ID_1, APP_ID_1_COUNT).containsEntry(APP_ID_2, APP_ID_2_COUNT)
                );
        }

        JsonNode createTopHitsBucket(String apiId, long topHitsCount) {
            var node = objectMapper.createObjectNode();
            node.put("key", apiId);
            node.put("doc_count", topHitsCount);
            node.putObject(HITS_COUNT).put("value", topHitsCount);
            return node;
        }
    }
}
