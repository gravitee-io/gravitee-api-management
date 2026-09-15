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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.elasticsearch.model.SearchHit;
import io.gravitee.elasticsearch.model.SearchHits;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.elasticsearch.model.TotalHits;
import io.gravitee.repository.log.v4.model.analytics.AgentActivityQuery;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class AgentActivityQueryAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    class AdaptHops {

        @Test
        void should_build_query_with_api_id() {
            var query = new AgentActivityQuery("api-a2a", Collections.<String>emptyList(), "actor-1", 0L, 0L, 0, 25);
            var result = AgentActivityQueryAdapter.adaptHops(query);
            assertThatJson(result).isEqualTo(
                "{" +
                    "\"size\": 25," +
                    "\"from\": 0," +
                    "\"query\": {" +
                    "  \"bool\": {" +
                    "    \"must\": [{" +
                    "      \"bool\": {" +
                    "        \"should\": [" +
                    "          { \"term\": { \"api-id\": \"api-a2a\" } }" +
                    "        ]," +
                    "        \"minimum_should_match\": 1" +
                    "      }" +
                    "    }]" +
                    "  }" +
                    "}," +
                    "\"sort\": [" +
                    "  { \"@timestamp\": { \"order\": \"desc\" } }" +
                    "]" +
                    "}"
            );
        }

        @Test
        void should_build_query_with_application_ids() {
            var query = new AgentActivityQuery(null, java.util.Arrays.asList("app-1", "app-2"), "actor-1", 0L, 0L, 0, 25);
            var result = AgentActivityQueryAdapter.adaptHops(query);
            assertThatJson(result)
                .inPath("$.query.bool.must[0].bool.should")
                .isEqualTo("[ { \"term\": { \"application-id\": \"app-1\" } }, { \"term\": { \"application-id\": \"app-2\" } } ]");
        }

        @Test
        void should_build_query_with_both_api_id_and_application_ids() {
            var query = new AgentActivityQuery("api-a2a", List.of("app-1"), "actor-1", 0L, 0L, 0, 25);
            var result = AgentActivityQueryAdapter.adaptHops(query);
            assertThatJson(result)
                .inPath("$.query.bool.must[0].bool.should")
                .isEqualTo("[ { \"term\": { \"api-id\": \"api-a2a\" } }, { \"term\": { \"application-id\": \"app-1\" } } ]");
            assertThatJson(result).inPath("$.query.bool.must[0].bool.minimum_should_match").isEqualTo(1);
        }

        @Test
        void should_include_time_range_when_provided() {
            var query = new AgentActivityQuery("api-a2a", Collections.<String>emptyList(), "actor-1", 1000L, 2000L, 0, 25);
            var result = AgentActivityQueryAdapter.adaptHops(query);
            assertThatJson(result)
                .inPath("$.query.bool.must[0]")
                .isEqualTo("{ \"range\": { \"@timestamp\": { \"gte\": 1000, \"lte\": 2000 } } }");
        }

        @Test
        void should_include_only_from_when_to_is_zero() {
            var query = new AgentActivityQuery("api-a2a", Collections.<String>emptyList(), "actor-1", 1000L, 0L, 0, 25);
            var result = AgentActivityQueryAdapter.adaptHops(query);
            assertThatJson(result).inPath("$.query.bool.must[0].range.@timestamp").isEqualTo("{ \"gte\": 1000 }");
        }

        @Test
        void should_include_only_to_when_from_is_zero() {
            var query = new AgentActivityQuery("api-a2a", Collections.<String>emptyList(), "actor-1", 0L, 2000L, 0, 25);
            var result = AgentActivityQueryAdapter.adaptHops(query);
            assertThatJson(result).inPath("$.query.bool.must[0].range.@timestamp").isEqualTo("{ \"lte\": 2000 }");
        }

        @Test
        void should_match_none_when_both_api_id_and_app_ids_are_missing() {
            var query = new AgentActivityQuery(null, Collections.<String>emptyList(), "actor-1", 0L, 0L, 0, 25);
            var result = AgentActivityQueryAdapter.adaptHops(query);
            assertThatJson(result).isEqualTo(
                "{" +
                    "\"size\": 25," +
                    "\"from\": 0," +
                    "\"query\": {" +
                    "  \"bool\": {" +
                    "    \"must\": [{ \"match_none\": {} }]" +
                    "  }" +
                    "}," +
                    "\"sort\": [" +
                    "  { \"@timestamp\": { \"order\": \"desc\" } }" +
                    "]" +
                    "}"
            );
        }

        @Test
        void should_match_none_when_time_range_is_set_but_agent_ids_are_missing() {
            var query = new AgentActivityQuery(null, Collections.<String>emptyList(), "actor-1", 1000L, 2000L, 0, 25);
            var result = AgentActivityQueryAdapter.adaptHops(query);
            assertThatJson(result)
                .inPath("$.query.bool.must[0]")
                .isEqualTo("{ \"range\": { \"@timestamp\": { \"gte\": 1000, \"lte\": 2000 } } }");
            assertThatJson(result).inPath("$.query.bool.must[1]").isEqualTo("{ \"match_none\": {} }");
        }

        @Test
        void should_paginate_correctly() {
            var query = new AgentActivityQuery("api-a2a", Collections.<String>emptyList(), "actor-1", 0L, 0L, 2, 25);
            var result = AgentActivityQueryAdapter.adaptHops(query);
            assertThatJson(result).inPath("$.from").isEqualTo(50);
            assertThatJson(result).inPath("$.size").isEqualTo(25);
        }
    }

    @Nested
    class AdaptDecisions {

        @Test
        void should_build_terms_query_on_request_ids() {
            var query = new AgentActivityQuery("api-a2a", Collections.<String>emptyList(), "actor-1", 0L, 0L, 0, 25);
            var result = AgentActivityQueryAdapter.adaptDecisions(query, Set.of("req-1", "req-2"));
            assertThatJson(result).inPath("$.query.bool.must").isArray().hasSize(1);
            assertThatJson(result).inPath("$.query.bool.must[0].terms['request-id']").isArray().contains("req-1", "req-2");
        }

        @Test
        void should_join_by_request_id_only_even_when_actor_is_set() {
            var query = new AgentActivityQuery("api-a2a", Collections.<String>emptyList(), "actor-1", 0L, 0L, 0, 25);
            var result = AgentActivityQueryAdapter.adaptDecisions(query, Set.of("req-1"));
            assertThat(result).doesNotContain("actor-id", "actorId");
            assertThatJson(result).isEqualTo(
                "{" +
                    "\"size\": 5," +
                    "\"query\": {" +
                    "  \"bool\": {" +
                    "    \"must\": [" +
                    "      { \"terms\": { \"request-id\": [\"req-1\"] } }" +
                    "    ]" +
                    "  }" +
                    "}" +
                    "}"
            );
        }

        @Test
        void should_omit_actor_id_term_when_null() {
            var query = new AgentActivityQuery("api-a2a", Collections.<String>emptyList(), null, 0L, 0L, 0, 25);
            var result = AgentActivityQueryAdapter.adaptDecisions(query, Set.of("req-1"));
            assertThatJson(result).inPath("$.query.bool.must").isArray().hasSize(1);
        }
    }

    @Nested
    class ExtractRequestIds {

        @Test
        @SneakyThrows
        void should_extract_request_ids_from_v4_metrics() {
            var response = hitsResponse("{ \"request-id\": \"req-1\" }", "{ \"request-id\": \"req-2\" }");
            var ids = AgentActivityQueryAdapter.extractRequestIds(response);
            assertThat(ids).containsExactlyInAnyOrder("req-1", "req-2");
        }

        @Test
        @SneakyThrows
        void should_fallback_to_id_field_in_v2_format() {
            var response = hitsResponse("{ \"id\": \"req-v2\" }");
            var ids = AgentActivityQueryAdapter.extractRequestIds(response);
            assertThat(ids).containsExactly("req-v2");
        }

        @Test
        void should_return_empty_set_when_response_is_null() {
            assertThat(AgentActivityQueryAdapter.extractRequestIds(null)).isEmpty();
        }

        @Test
        void should_return_empty_set_when_hits_are_null() {
            var response = new SearchResponse();
            response.setSearchHits(null);
            assertThat(AgentActivityQueryAdapter.extractRequestIds(response)).isEmpty();
        }

        @Test
        @SneakyThrows
        void should_return_empty_set_when_source_is_null() {
            var hit = new SearchHit();
            hit.setSource(null);
            var hits = new SearchHits();
            hits.setHits(List.of(hit));
            var response = new SearchResponse();
            response.setSearchHits(hits);
            assertThat(AgentActivityQueryAdapter.extractRequestIds(response)).isEmpty();
        }
    }

    @SafeVarargs
    @SneakyThrows
    private SearchResponse hitsResponse(String... sources) {
        var response = new SearchResponse();
        var hits = new SearchHits();
        var hitList = new SearchHit[sources.length];
        for (int i = 0; i < sources.length; i++) {
            var hit = new SearchHit();
            hit.setSource(objectMapper.readTree(sources[i]));
            hitList[i] = hit;
        }
        hits.setHits(List.of(hitList));
        hits.setTotal(new TotalHits(sources.length));
        response.setSearchHits(hits);
        return response;
    }
}
