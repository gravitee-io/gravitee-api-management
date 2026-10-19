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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.analytics.engine.api.query.TrafficScope;
import io.gravitee.repository.elasticsearch.v4.shared.EntrypointScopeClause;
import io.gravitee.repository.log.v4.model.analytics.FilterValuesQuery;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FilterValuesQueryAdapterTest {

    private final FilterValuesQueryAdapter adapter = new FilterValuesQueryAdapter();

    @Test
    void should_build_composite_query_without_time_range() {
        var query = FilterValuesQuery.builder().esFieldName("api-id").size(10).build();

        var result = new JsonObject(adapter.adapt(query));

        assertThat(result.getInteger("size")).isEqualTo(0);
        assertThat(result.getJsonObject("query")).isNull();

        var composite = result.getJsonObject("aggs").getJsonObject("filter_values").getJsonObject("composite");
        assertThat(composite.getInteger("size")).isEqualTo(10);

        var source = composite.getJsonArray("sources").getJsonObject(0).getJsonObject("value").getJsonObject("terms");
        assertThat(source.getString("field")).isEqualTo("api-id");
    }

    @Test
    void should_build_composite_query_with_time_range() {
        var query = FilterValuesQuery.builder().esFieldName("gateway").from(1704067200000L).to(1735689599000L).size(10).build();

        var result = new JsonObject(adapter.adapt(query));

        var boolFilter = result.getJsonObject("query").getJsonObject("bool").getJsonArray("filter");
        assertThat(boolFilter).hasSize(1);

        var range = boolFilter.getJsonObject(0).getJsonObject("range").getJsonObject("@timestamp");
        assertThat(range.getLong("gte")).isEqualTo(1704067200000L);
        assertThat(range.getLong("lte")).isEqualTo(1735689599000L);
    }

    @Test
    void should_include_after_key_for_pagination() {
        var query = FilterValuesQuery.builder().esFieldName("api-id").size(10).afterKey(Map.of("value", "last-api-id")).build();

        var result = new JsonObject(adapter.adapt(query));
        var composite = result.getJsonObject("aggs").getJsonObject("filter_values").getJsonObject("composite");

        assertThat(composite.getJsonObject("after").getString("value")).isEqualTo("last-api-id");
    }

    @Test
    void should_omit_after_key_when_null() {
        var query = FilterValuesQuery.builder().esFieldName("api-id").size(10).build();

        var result = new JsonObject(adapter.adapt(query));
        var composite = result.getJsonObject("aggs").getJsonObject("filter_values").getJsonObject("composite");

        assertThat(composite.getJsonObject("after")).isNull();
    }

    @Test
    void should_include_cardinality_aggregation() {
        var query = FilterValuesQuery.builder().esFieldName("api-id").size(10).build();

        var result = new JsonObject(adapter.adapt(query));

        var cardinality = result.getJsonObject("aggs").getJsonObject("total_count").getJsonObject("cardinality");
        assertThat(cardinality.getString("field")).isEqualTo("api-id");
    }

    @Test
    void should_include_term_query_for_search_pattern() {
        var query = FilterValuesQuery.builder().esFieldName("gateway").size(10).searchPattern("gw-1").build();

        var result = new JsonObject(adapter.adapt(query));
        var boolFilter = result.getJsonObject("query").getJsonObject("bool").getJsonArray("filter");
        assertThat(boolFilter).hasSize(1);

        var term = boolFilter.getJsonObject(0).getJsonObject("term").getJsonObject("gateway");
        assertThat(term.getString("value")).isEqualTo("gw-1");
        assertThat(term.getBoolean("case_insensitive")).isTrue();
    }

    @Test
    void should_combine_time_range_and_search_pattern() {
        var query = FilterValuesQuery.builder()
            .esFieldName("gateway")
            .from(1704067200000L)
            .to(1735689599000L)
            .size(10)
            .searchPattern("prod")
            .build();

        var result = new JsonObject(adapter.adapt(query));
        var boolFilter = result.getJsonObject("query").getJsonObject("bool").getJsonArray("filter");
        assertThat(boolFilter).hasSize(2);

        var range = boolFilter.getJsonObject(0).getJsonObject("range").getJsonObject("@timestamp");
        assertThat(range.getLong("gte")).isEqualTo(1704067200000L);

        var term = boolFilter.getJsonObject(1).getJsonObject("term").getJsonObject("gateway");
        assertThat(term.getString("value")).isEqualTo("prod");
        assertThat(term.getBoolean("case_insensitive")).isTrue();
    }

    @Test
    void should_add_terms_filter_on_api_id_when_api_ids_provided() {
        var query = FilterValuesQuery.builder().esFieldName("gateway").size(10).apiIds(Set.of("a1", "a2")).build();

        var result = new JsonObject(adapter.adapt(query));
        var boolFilter = result.getJsonObject("query").getJsonObject("bool").getJsonArray("filter");
        assertThat(boolFilter).hasSize(1);

        var terms = boolFilter.getJsonObject(0).getJsonObject("terms").getJsonArray("api-id");
        assertThat(terms).containsExactlyInAnyOrder("a1", "a2");
    }

    /** The values a module scoped by API kind offers come from the calls it shows, nothing else. */
    @Test
    void should_offer_only_the_values_of_the_calls_in_the_traffic_scope() {
        var query = FilterValuesQuery.builder()
            .esFieldName("application-id")
            .size(10)
            .apiIds(Set.of("studio", "orders"))
            .trafficScope(new TrafficScope(Set.of("studio"), Set.of("mcp")))
            .build();

        var boolFilter = new JsonObject(adapter.adapt(query)).getJsonObject("query").getJsonObject("bool").getJsonArray("filter");

        assertThat(boolFilter).hasSize(2);
        assertThat(boolFilter.getJsonObject(1)).isEqualTo(
            EntrypointScopeClause.apisOrEntrypoints(
                JsonObject.of("terms", JsonObject.of("api-id", new JsonArray(List.of("studio")))),
                List.of("mcp")
            )
        );
    }

    @Test
    void should_offer_only_the_requested_values() {
        var query = FilterValuesQuery.builder()
            .esFieldName("application-id")
            .size(2)
            .apiIds(Set.of("api-1"))
            .values(Set.of("app-1", "app-2"))
            .build();

        var boolFilter = new JsonObject(adapter.adapt(query)).getJsonObject("query").getJsonObject("bool").getJsonArray("filter");

        assertThat(boolFilter).hasSize(2);
        assertThat(boolFilter.getJsonObject(1).getJsonObject("terms").getJsonArray("application-id")).containsExactlyInAnyOrder(
            "app-1",
            "app-2"
        );
    }

    @Test
    void should_offer_only_the_values_of_the_calls_made_through_the_api_product() {
        var query = FilterValuesQuery.builder()
            .esFieldName("application-id")
            .size(10)
            .apiIds(Set.of("api-1"))
            .apiProductId("product-1")
            .build();

        var boolFilter = new JsonObject(adapter.adapt(query)).getJsonObject("query").getJsonObject("bool").getJsonArray("filter");

        assertThat(boolFilter).hasSize(2);
        assertThat(boolFilter.getJsonObject(1).getJsonObject("term").getString("api-product-id")).isEqualTo("product-1");
    }

    @Test
    void should_keep_the_api_product_next_to_the_traffic_scope() {
        var query = FilterValuesQuery.builder()
            .esFieldName("application-id")
            .size(10)
            .apiIds(Set.of("studio", "orders"))
            .trafficScope(new TrafficScope(Set.of("studio"), Set.of("mcp")))
            .apiProductId("product-1")
            .build();

        var boolFilter = new JsonObject(adapter.adapt(query)).getJsonObject("query").getJsonObject("bool").getJsonArray("filter");

        assertThat(boolFilter).hasSize(3);
        assertThat(boolFilter.getJsonObject(1).getJsonObject("term").getString("api-product-id")).isEqualTo("product-1");
        assertThat(boolFilter.getJsonObject(2)).isEqualTo(
            EntrypointScopeClause.apisOrEntrypoints(
                JsonObject.of("terms", JsonObject.of("api-id", new JsonArray(List.of("studio")))),
                List.of("mcp")
            )
        );
    }

    @Test
    void should_add_match_none_when_no_value_is_requested() {
        var query = FilterValuesQuery.builder().esFieldName("application-id").size(10).values(Set.of()).build();

        var boolFilter = new JsonObject(adapter.adapt(query)).getJsonObject("query").getJsonObject("bool").getJsonArray("filter");

        assertThat(boolFilter).hasSize(1);
        assertThat(boolFilter.getJsonObject(0).getJsonObject("match_none")).isNotNull();
    }

    @Test
    void should_add_match_none_when_api_ids_empty_set() {
        var query = FilterValuesQuery.builder().esFieldName("api-id").size(10).apiIds(Set.of()).build();

        var result = new JsonObject(adapter.adapt(query));
        var boolFilter = result.getJsonObject("query").getJsonObject("bool").getJsonArray("filter");
        assertThat(boolFilter).hasSize(1);
        assertThat(boolFilter.getJsonObject(0).getJsonObject("match_none")).isNotNull();
    }

    @Test
    void should_combine_time_range_search_pattern_and_api_ids() {
        var query = FilterValuesQuery.builder()
            .esFieldName("gateway")
            .from(1704067200000L)
            .to(1735689599000L)
            .size(10)
            .searchPattern("prod")
            .apiIds(Set.of("api-1"))
            .build();

        var result = new JsonObject(adapter.adapt(query));
        var boolFilter = result.getJsonObject("query").getJsonObject("bool").getJsonArray("filter");
        assertThat(boolFilter).hasSize(3);

        assertThat(boolFilter.getJsonObject(0).getJsonObject("range")).isNotNull();
        assertThat(boolFilter.getJsonObject(1).getJsonObject("term")).isNotNull();
        assertThat(boolFilter.getJsonObject(2).getJsonObject("terms").getJsonArray("api-id")).containsExactly("api-1");
    }
}
