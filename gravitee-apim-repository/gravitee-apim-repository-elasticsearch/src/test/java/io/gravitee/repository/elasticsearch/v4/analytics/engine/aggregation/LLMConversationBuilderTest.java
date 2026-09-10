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
class LLMConversationBuilderTest {

    private static final String CONVERSATION = "additional-metrics.keyword_llm-proxy_conversation-id";
    private static final String TURN = "additional-metrics.keyword_llm-proxy_turn-id";

    private final LLMConversationBuilder builder = new LLMConversationBuilder();

    private JsonObject ratioAggs(java.util.Map<String, JsonObject> built, String aggName) {
        return built.get("_" + aggName).getJsonObject("aggs");
    }

    @Test
    void should_count_the_distinct_conversations() {
        var agg = builder.buildConversations("A").get("A");

        assertThat(agg.getJsonObject("cardinality").getString("field")).isEqualTo(CONVERSATION);
    }

    @Test
    void should_count_the_distinct_turns() {
        var agg = builder.buildTurns("A").get("A");

        assertThat(agg.getJsonObject("cardinality").getString("field")).isEqualTo(TURN);
    }

    /** Turns over conversations: distinct turns divided by the conversations they were spread across. */
    @Test
    void should_divide_turns_by_conversations() {
        var aggs = ratioAggs(builder.buildTurnsPerConversation("A"), "A");

        assertThat(aggs.getJsonObject("_total").getJsonObject("cardinality").getString("field")).isEqualTo(TURN);
        assertThat(aggs.getJsonObject("_divisor").getJsonObject("cardinality").getString("field")).isEqualTo(CONVERSATION);
    }

    /**
     * Calls over turns, with the numerator counted over the turn field rather than every document: a side
     * call carries no turn, so counting it here would divide traffic nobody asked for by turns somebody did.
     */
    @Test
    void should_divide_calls_by_turns_counting_only_traffic_that_has_a_turn() {
        var aggs = ratioAggs(builder.buildCallsPerTurn("A"), "A");

        assertThat(aggs.getJsonObject("_total").getJsonObject("value_count").getString("field")).isEqualTo(TURN);
        assertThat(aggs.getJsonObject("_divisor").getJsonObject("cardinality").getString("field")).isEqualTo(TURN);
    }

    /** Cost over conversations, and unlike the turn metrics it counts every request the conversation made. */
    @Test
    void should_divide_the_summed_cost_by_conversations() {
        var aggs = ratioAggs(builder.buildCostPerConversation("A"), "A");

        var source = aggs.getJsonObject("_total").getJsonObject("sum").getJsonObject("script").getString("source");
        assertThat(source).contains("double_llm-proxy_sent-cost").contains("double_llm-proxy_received-cost");
        assertThat(aggs.getJsonObject("_divisor").getJsonObject("cardinality").getString("field")).isEqualTo(CONVERSATION);
    }

    /** The same script the standalone cost aggregation uses — one definition, two callers. */
    @Test
    void should_reuse_the_cost_script_rather_than_restate_it() {
        var nested = ratioAggs(builder.buildCostPerConversation("A"), "A").getJsonObject("_total");

        assertThat(nested).isEqualTo(new LLMTotalCostBuilder().sum());
    }
}
