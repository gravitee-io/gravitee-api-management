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
package io.gravitee.repository.elasticsearch.v4.shared;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.analytics.engine.api.query.ObservabilityEntrypoints;
import io.vertx.core.json.JsonObject;
import java.util.List;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class EntrypointScopeClauseTest {

    @Test
    void should_exclude_the_ids_on_the_field_and_on_its_keyword_sub_field() {
        var clause = EntrypointScopeClause.excluding(List.of("sse", "native-kafka"));

        assertThat(clause).isEqualTo(
            new JsonObject(
                """
                {"bool":{"must_not":[{"terms":{"entrypoint-id":["sse","native-kafka"]}},{"terms":{"entrypoint-id.keyword":["sse","native-kafka"]}}]}}
                """
            )
        );
    }

    @Test
    void should_require_an_entrypoint_when_the_exclusion_names_the_synthetic_value() {
        var clause = EntrypointScopeClause.excluding(List.of("sse", "(none)"));

        assertThat(clause.getJsonObject("bool").getJsonArray("must_not")).hasSize(3);
        assertThat(clause.encode()).contains("\"exists\":{\"field\":\"entrypoint-id\"}");
    }

    /**
     * Both factories are public in a module other repositories build on, so an empty list has to answer rather
     * than fail: the complement of nothing is everything, which is also what this scope does whenever the
     * decision is missing.
     */
    @Test
    void should_keep_every_document_when_the_exclusion_is_empty() {
        assertThat(EntrypointScopeClause.excluding(List.of())).isEqualTo(JsonObject.of("match_all", JsonObject.of()));
    }

    @Test
    void should_build_the_analytics_default_from_the_registry() {
        var clause = EntrypointScopeClause.analyticsDefault();

        assertThat(
            clause.getJsonObject("bool").getJsonArray("must_not").getJsonObject(0).getJsonObject("terms").getJsonArray("entrypoint-id")
        ).containsExactlyElementsOf(ObservabilityEntrypoints.ANALYTICS_EXCLUDED_IDS);
    }

    @Test
    void should_select_the_given_ids_on_the_field_and_on_its_keyword_sub_field() {
        var clause = EntrypointScopeClause.exactly(List.of("mcp", "mcp-studio"));

        assertThat(clause).isEqualTo(
            new JsonObject(
                """
                {"bool":{"should":[{"terms":{"entrypoint-id":["mcp","mcp-studio"]}},{"terms":{"entrypoint-id.keyword":["mcp","mcp-studio"]}}],"minimum_should_match":1}}
                """
            )
        );
    }

    @Test
    void should_select_documents_without_an_entrypoint_for_the_synthetic_value_alone() {
        var clause = EntrypointScopeClause.exactly(List.of("(none)"));

        assertThat(clause).isEqualTo(new JsonObject("{\"bool\":{\"must_not\":{\"exists\":{\"field\":\"entrypoint-id\"}}}}"));
    }

    @Test
    void should_combine_ids_and_the_synthetic_value_as_alternatives() {
        var clause = EntrypointScopeClause.exactly(List.of("mcp", "(none)"));

        var alternatives = clause.getJsonObject("bool").getJsonArray("should");
        assertThat(alternatives).hasSize(3);
        assertThat(clause.getJsonObject("bool").getInteger("minimum_should_match")).isEqualTo(1);
        assertThat(clause.encode()).contains("\"exists\":{\"field\":\"entrypoint-id\"}");
    }

    @Test
    void should_select_nothing_when_the_filter_names_no_value() {
        // An empty IN selects nothing everywhere else in the query builders; failing here would turn a caller's
        // empty list into a 500 from inside the repository.
        assertThat(EntrypointScopeClause.exactly(List.of())).isEqualTo(new JsonObject("{\"match_none\":{}}"));
    }

    @Test
    void should_select_the_calls_of_the_apis_or_made_through_the_entrypoints() {
        var apis = JsonObject.of("terms", JsonObject.of("api-id", List.of("studio")));

        var clause = EntrypointScopeClause.apisOrEntrypoints(apis, List.of("mcp"));

        assertThat(clause).isEqualTo(
            new JsonObject(
                """
                {"bool":{"should":[{"terms":{"api-id":["studio"]}},{"bool":{"should":[{"terms":{"entrypoint-id":["mcp"]}},{"terms":{"entrypoint-id.keyword":["mcp"]}}],"minimum_should_match":1}}],"minimum_should_match":1}}
                """
            )
        );
    }

    @Test
    void should_select_only_the_entrypoint_calls_when_no_api_is_in_scope_by_type() {
        var clause = EntrypointScopeClause.apisOrEntrypoints(null, List.of("mcp"));

        assertThat(clause).isEqualTo(EntrypointScopeClause.exactly(List.of("mcp")));
    }

    @Test
    void should_select_only_the_api_calls_when_no_entrypoint_adds_a_kind() {
        var apis = JsonObject.of("terms", JsonObject.of("api-id", List.of("studio")));

        assertThat(EntrypointScopeClause.apisOrEntrypoints(apis, List.of())).isEqualTo(apis);
    }

    @Test
    void should_select_nothing_when_neither_arm_names_anything() {
        assertThat(EntrypointScopeClause.apisOrEntrypoints(null, List.of())).isEqualTo(JsonObject.of("match_none", JsonObject.of()));
    }
}
