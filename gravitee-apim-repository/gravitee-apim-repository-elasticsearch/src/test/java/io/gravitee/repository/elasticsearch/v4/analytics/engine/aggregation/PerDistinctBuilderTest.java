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
package io.gravitee.repository.elasticsearch.v4.analytics.engine.aggregation;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
class PerDistinctBuilderTest {

    private static final String FIELD = "a-keyword-field";
    private static final String OTHER_FIELD = "another-keyword-field";

    private static final String AGG = "LLM_TURNS_PER_CONVERSATION#AVG";

    private final PerDistinctBuilder builder = new PerDistinctBuilder(FIELD);

    private JsonObject turnCount() {
        return new JsonObject().put("cardinality", new JsonObject().put("field", OTHER_FIELD));
    }

    private JsonObject requestCount() {
        return new JsonObject().put("value_count", new JsonObject().put("field", "@timestamp"));
    }

    /**
     * A {@code bucket_script} only runs inside a bucket, and a measures query has none — hence the single
     * all-encompassing date histogram, and the underscore-prefixed name the response side unwraps.
     */
    @Test
    void should_wrap_the_ratio_in_a_single_bucket_the_response_side_can_unwrap() {
        var result = builder.build(AGG, turnCount());

        assertThat(result).containsKey("_" + AGG);

        var wrapper = result.get("_" + AGG);
        assertThat(wrapper.containsKey("date_histogram")).isTrue();
        assertThat(wrapper.getJsonObject("date_histogram").getString("fixed_interval")).isEqualTo("999999h");
    }

    @Test
    void should_divide_what_it_was_given_by_the_field_it_was_built_for() {
        var aggs = builder.build(AGG, turnCount()).get("_" + AGG).getJsonObject("aggs");

        assertThat(aggs.getJsonObject("_total")).isEqualTo(turnCount());
        assertThat(aggs.getJsonObject("_divisor").getJsonObject("cardinality").getString("field")).isEqualTo(FIELD);

        var script = aggs.getJsonObject(AGG).getJsonObject("bucket_script");
        assertThat(script.getJsonObject("buckets_path").getString("total")).isEqualTo("_total");
        assertThat(script.getJsonObject("buckets_path").getString("divisor")).isEqualTo("_divisor");
        assertThat(script.getJsonObject("script").getString("source")).contains("params.total / params.divisor");
    }

    /** Calls per turn divides by turns, not conversations — the same builder, a different denominator. */
    @Test
    void should_divide_by_turns_when_built_for_turns() {
        var perTurn = new PerDistinctBuilder(OTHER_FIELD);

        var aggs = perTurn.build("LLM_CALLS_PER_TURN#AVG", requestCount()).get("_LLM_CALLS_PER_TURN#AVG").getJsonObject("aggs");

        assertThat(aggs.getJsonObject("_total")).isEqualTo(requestCount());
        assertThat(aggs.getJsonObject("_divisor").getJsonObject("cardinality").getString("field")).isEqualTo(OTHER_FIELD);
    }

    /** A window with requests but nothing correlated would otherwise divide by zero. */
    @Test
    void should_guard_against_a_window_with_no_divisor() {
        var aggs = builder.build(AGG, turnCount()).get("_" + AGG).getJsonObject("aggs");

        var source = aggs.getJsonObject(AGG).getJsonObject("bucket_script").getJsonObject("script").getString("source");

        assertThat(source).startsWith("params.divisor > 0 ?").endsWith(": 0");
    }
}
