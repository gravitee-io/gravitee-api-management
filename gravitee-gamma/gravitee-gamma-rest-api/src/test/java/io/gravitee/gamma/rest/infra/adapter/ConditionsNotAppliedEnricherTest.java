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
package io.gravitee.gamma.rest.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ConditionsNotAppliedEnricherTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void should_append_after_what_the_engine_reported_each_name_once() throws Exception {
        var response = json(
            """
            { "metrics": [
              { "name": "HTTP_REQUESTS", "ignoredFilters": ["NATIVE_CLIENT_ID"] },
              { "name": "HTTP_ERRORS" }
            ] }
            """
        );

        ConditionsNotAppliedEnricher.enrichMeasures(response, Map.of("HTTP_REQUESTS", List.of("RECORD_TYPE", "NATIVE_CLIENT_ID")));

        assertThat(response.at("/metrics/0/ignoredFilters")).isEqualTo(json("[\"NATIVE_CLIENT_ID\", \"RECORD_TYPE\"]"));
        assertThat(response.at("/metrics/1").has("ignoredFilters")).isFalse();
    }

    @Test
    void should_find_a_facet_metric_under_its_own_key() throws Exception {
        var response = json("{ \"metrics\": [ { \"metric\": \"HTTP_REQUESTS\", \"buckets\": [] } ] }");

        ConditionsNotAppliedEnricher.enrichFacets(response, Map.of("HTTP_REQUESTS", List.of("PLAN")));

        assertThat(response.at("/metrics/0/ignoredFilters")).isEqualTo(json("[\"PLAN\"]"));
    }

    @Test
    void should_leave_an_empty_response_alone() throws Exception {
        var response = json("{}");

        ConditionsNotAppliedEnricher.enrichTimeSeries(response, Map.of("HTTP_REQUESTS", List.of("PLAN")));

        assertThat(response).isEqualTo(json("{}"));
    }

    private static JsonNode json(String text) throws Exception {
        return JSON.readTree(text);
    }
}
