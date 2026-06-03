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
package io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter;

import static io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.FilterAdapter.*;
import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
class FilterAdapterTest {

    private final FilterAdapter filterAdapter = new FilterAdapter(new HTTPFieldResolver());

    @Test
    void should_include_all_http_entrypoint_ids_in_http_filter() {
        var httpFilter = filterAdapter.httpFilter();

        var termsFilter = httpFilter.getJsonObject("bool").getJsonArray("should").getJsonObject(0);

        var entrypointIds = termsFilter.getJsonObject("terms").getJsonArray(ENTRYPOINT_FIELD);

        assertThat(entrypointIds.getList()).containsExactly(
            HTTP_GET_ENTRYPOINT_ID,
            HTTP_POST_ENTRYPOINT_ID,
            HTTP_PROXY_ENTRYPOINT_ID,
            LLM_PROXY_ENTRYPOINT_ID,
            MCP_PROXY_ENTRYPOINT_ID
        );
    }

    @Test
    void should_include_field_missing_clause_in_http_filter() {
        var httpFilter = filterAdapter.httpFilter();

        var shouldClauses = httpFilter.getJsonObject("bool").getJsonArray("should");
        assertThat(shouldClauses).hasSize(2);

        var fieldMissingClause = shouldClauses.getJsonObject(1);
        var mustNot = fieldMissingClause.getJsonObject("bool").getJsonObject("must_not");
        var existsField = mustNot.getJsonObject("exists").getString("field");

        assertThat(existsField).isEqualTo(ENTRYPOINT_FIELD);
    }
}
