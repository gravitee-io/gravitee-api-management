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

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class NativeApiMetricsFindQueryAdapterTest {

    private static final String API_ID = "kafka-api-001";
    private static final String REQUEST_ID = "conn-connected-001";
    private static final long FROM = 1_700_000_000_000L;
    private static final long TO = 1_700_003_600_000L;

    @Test
    void builds_size_one_search_filtered_by_api_id_request_id_and_inclusive_timestamp_range() {
        var json = NativeApiMetricsFindQueryAdapter.adapt(API_ID, REQUEST_ID, FROM, TO);

        assertThatJson(json).isEqualTo(
            """
            {
              "size": 1,
              "query": {
                "bool": {
                  "must": [
                    { "term": { "api-id": "kafka-api-001" } },
                    { "term": { "request-id": "conn-connected-001" } },
                    { "range": { "@timestamp": { "gte": 1700000000000, "lte": 1700003600000 } } }
                  ]
                }
              }
            }
            """
        );
    }
}
