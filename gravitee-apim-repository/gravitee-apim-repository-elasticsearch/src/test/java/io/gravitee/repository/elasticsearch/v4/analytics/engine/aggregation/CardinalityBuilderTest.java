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

import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
class CardinalityBuilderTest {

    private static final String FIELD = "a-keyword-field";

    private final CardinalityBuilder builder = new CardinalityBuilder(FIELD);

    /**
     * Counting requests would say how many calls were made, not how many conversations they belonged to.
     * Every request of a conversation repeats the same id, so the distinct count is the whole point.
     */
    @Test
    void should_count_distinct_values_of_its_field() {
        var result = builder.build("LLM_CONVERSATIONS#COUNT");

        assertThat(result).containsKey("LLM_CONVERSATIONS#COUNT");

        var agg = result.get("LLM_CONVERSATIONS#COUNT");
        assertThat(agg.containsKey("cardinality")).isTrue();
        assertThat(agg.getJsonObject("cardinality").getString("field")).isEqualTo(FIELD);
    }

    @Test
    void should_count_whichever_field_it_was_given() {
        var turns = new CardinalityBuilder("another-keyword-field");

        var agg = turns.build("LLM_TURNS#COUNT").get("LLM_TURNS#COUNT");

        assertThat(agg.getJsonObject("cardinality").getString("field")).isEqualTo("another-keyword-field");
    }
}
