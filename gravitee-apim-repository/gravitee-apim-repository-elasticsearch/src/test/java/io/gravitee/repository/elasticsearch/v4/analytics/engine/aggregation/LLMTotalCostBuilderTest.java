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
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
class LLMTotalCostBuilderTest {

    private final LLMTotalCostBuilder builder = new LLMTotalCostBuilder();

    @Test
    void should_build_sum_aggregation() {
        var result = builder.buildSum("LLM_PROMPT_TOKEN_TOTAL_COST#COUNT");

        assertThat(result).containsKey("LLM_PROMPT_TOKEN_TOTAL_COST#COUNT");

        var agg = result.get("LLM_PROMPT_TOKEN_TOTAL_COST#COUNT");
        assertThat(agg.containsKey("sum")).isTrue();

        var sumAgg = agg.getJsonObject("sum");
        assertThat(sumAgg.containsKey("script")).isTrue();

        var script = sumAgg.getJsonObject("script");
        assertThat(script.getString("source")).contains("additional-metrics.double_llm-proxy_sent-cost");
        assertThat(script.getString("source")).contains("additional-metrics.double_llm-proxy_received-cost");
        assertThat(script.getString("source")).contains("+");
    }

    @Test
    void should_build_avg_aggregation() {
        var result = builder.buildAvg("LLM_PROMPT_TOKEN_TOTAL_COST#AVG");

        assertThat(result).containsKey("LLM_PROMPT_TOKEN_TOTAL_COST#AVG");

        var agg = result.get("LLM_PROMPT_TOKEN_TOTAL_COST#AVG");
        assertThat(agg.containsKey("avg")).isTrue();

        var avgAgg = agg.getJsonObject("avg");
        assertThat(avgAgg.containsKey("script")).isTrue();

        var script = avgAgg.getJsonObject("script");
        assertThat(script.getString("source")).contains("additional-metrics.double_llm-proxy_sent-cost");
        assertThat(script.getString("source")).contains("additional-metrics.double_llm-proxy_received-cost");
        assertThat(script.getString("source")).contains("+");
    }
}
