/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.elasticsearch.v4.shared;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

class ConnectionPhaseClauseTest {

    private static final JsonObject OPENING = JsonObject.of("term", JsonObject.of("connection-phase", "open"));
    private static final JsonObject END = JsonObject.of("term", JsonObject.of("connection-phase", "end"));

    @Test
    void should_keep_the_query_and_leave_openings_out() {
        var search = JsonObject.of("size", 0, "query", JsonObject.of("term", JsonObject.of("api-id", "api")), "aggs", JsonObject.of());

        var result = new JsonObject(ConnectionPhaseClause.excludeOpeningsFrom(search.encode()));

        assertThat(result.getJsonObject("query")).isEqualTo(
            JsonObject.of("bool", JsonObject.of("must_not", JsonArray.of(OPENING), "must", JsonArray.of(search.getJsonObject("query"))))
        );
        assertThat(result.getInteger("size")).isZero();
        assertThat(result.getJsonObject("aggs")).isEqualTo(JsonObject.of());
    }

    @Test
    void should_leave_ends_out_of_a_search_without_query() {
        var result = new JsonObject(ConnectionPhaseClause.excludeEndsFrom(JsonObject.of("size", 0).encode()));

        assertThat(result.getJsonObject("query")).isEqualTo(JsonObject.of("bool", JsonObject.of("must_not", JsonArray.of(END))));
    }

    @Test
    void should_filter_an_aggregation_on_documents_that_are_not_an_end() {
        assertThat(ConnectionPhaseClause.notEnd()).isEqualTo(JsonObject.of("bool", JsonObject.of("must_not", JsonArray.of(END))));
    }

    @Test
    void should_filter_an_aggregation_on_documents_that_are_not_an_opening() {
        assertThat(ConnectionPhaseClause.notOpening()).isEqualTo(JsonObject.of("bool", JsonObject.of("must_not", JsonArray.of(OPENING))));
    }
}
