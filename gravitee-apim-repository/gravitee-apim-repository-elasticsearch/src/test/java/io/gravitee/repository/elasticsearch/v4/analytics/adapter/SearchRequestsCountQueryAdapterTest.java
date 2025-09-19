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

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.log.v4.model.analytics.RequestsCountQuery;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SearchRequestsCountQueryAdapterTest {

    public static final String QUERY_WITHOUT_FILTER = """
        {
            "size": 0,
            "aggs": {
                "entrypoints": {
                    "terms": {"field":"entrypoint-id"}
                },
                "all_apis_status_ranges": {
                     "range": {"field": "status","ranges": [{"from": 100.0,"to": 600.0}]}
                }
            }
        }
         """;

    @Test
    void should_build_query_without_filter() {
        var result = SearchRequestsCountQueryAdapter.adapt(null, true);

        assertThatJson(result).isEqualTo(QUERY_WITHOUT_FILTER);
    }

    @Test
    void should_build_query_with_empty_filter() {
        var result = SearchRequestsCountQueryAdapter.adapt(RequestsCountQuery.builder().build(), true);

        assertThatJson(result).isEqualTo(QUERY_WITHOUT_FILTER);
    }

    @Test
    void should_build_query_with_api_filter() {
        var result = SearchRequestsCountQueryAdapter.adapt(RequestsCountQuery.builder().apiId("api-id").build(), true);

        assertThatJson(result).isEqualTo(
            """
               {
                   "size": 0,
                   "query":{
                       "bool": {
                           "must": [
                               {
                                   "term": {"api-id":"api-id"}
                               }
                           ]
                       }
                   },
                   "aggs": {
                       "entrypoints": {
                               "terms": {"field":"entrypoint-id"}
                       },
                       "all_apis_status_ranges": {
                           "range": {"field": "status","ranges": [{"from": 100.0,"to": 600.0}]}
                     }
                   }
               }
            """
        );
    }

    @Test
    void should_adapt_the_query_according_when_entrypoint_id_not_keyword() {
        var result = SearchRequestsCountQueryAdapter.adapt(RequestsCountQuery.builder().apiId("api-id").build(), false);

        assertThatJson(result).isEqualTo(
            """
               {
                   "size": 0,
                   "query":{
                       "bool": {
                           "must": [
                               {
                                   "term": {"api-id":"api-id"}
                               }
                           ]
                       }
                   },
                   "aggs": {
                       "entrypoints": {
                               "terms": {"field":"entrypoint-id.keyword"}
                       },
                       "all_apis_status_ranges": {
                             "range": {"field": "status","ranges": [{"from": 100.0,"to": 600.0}]}
                       }
                   }
               }
            """
        );
    }
}
