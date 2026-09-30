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
package io.gravitee.apim.core.ai_workspace.domain_service;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AiWorkspaceBudgetReaderTest {

    @ParameterizedTest
    @CsvSource({ "60,HOUR", "1440,DAY", "10080,WEEK", "43200,MONTH" })
    void converts_micro_dollars_and_minutes(int minutes, String period) {
        var budget = AiWorkspaceBudgetReader.read(List.of(flow(config(5_000_000, minutes)))).orElseThrow();

        assertThat(budget.amount()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(budget.period()).isEqualTo(period);
    }

    @Test
    void missing_policy_is_empty() {
        Step other = new Step();
        other.setPolicy("rate-limit");
        other.setConfiguration("{\"rate\":{\"limit\":1}}");
        Flow flow = new Flow();
        flow.setRequest(List.of(other));

        assertThat(AiWorkspaceBudgetReader.read(List.of(flow))).isEmpty();
        assertThat(AiWorkspaceBudgetReader.read(List.of(flow("{\"rate\":{}}")))).isEmpty();
        assertThat(AiWorkspaceBudgetReader.read(List.of())).isEmpty();
        assertThat(AiWorkspaceBudgetReader.read(null)).isEmpty();
    }

    @Test
    void malformed_configuration_is_empty() {
        assertThat(AiWorkspaceBudgetReader.read(List.of(flow("not-json")))).isEmpty();
        assertThat(AiWorkspaceBudgetReader.read(List.of(flow("not-json")), "plan-1")).isEmpty();
    }

    @Test
    void unknown_window_keeps_the_amount() {
        var budget = AiWorkspaceBudgetReader.read(List.of(flow(config(2_500_000, 90)))).orElseThrow();

        assertThat(budget.amount()).isEqualByComparingTo(new BigDecimal("2.50"));
        assertThat(budget.period()).isNull();
    }

    @Test
    void converts_period_time_unit_before_the_lookup() {
        var week = AiWorkspaceBudgetReader.read(
            List.of(flow("{\"rate\":{\"limit\":50000000,\"periodTime\":10080,\"periodTimeUnit\":\"MINUTES\"}}"))
        ).orElseThrow();
        var hour = AiWorkspaceBudgetReader.read(
            List.of(flow("{\"rate\":{\"limit\":1000000,\"periodTime\":1,\"periodTimeUnit\":\"HOURS\"}}"))
        ).orElseThrow();
        var seconds = AiWorkspaceBudgetReader.read(
            List.of(flow("{\"rate\":{\"limit\":1000000,\"periodTime\":60,\"periodTimeUnit\":\"SECONDS\"}}"))
        ).orElseThrow();

        assertThat(week.period()).isEqualTo("WEEK");
        assertThat(week.amount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(hour.period()).isEqualTo("HOUR");
        assertThat(seconds.amount()).isEqualByComparingTo(new BigDecimal("1.00"));
        assertThat(seconds.period()).isNull();

        var partialMinute = AiWorkspaceBudgetReader.read(
            List.of(flow("{\"rate\":{\"limit\":1000000,\"periodTime\":3630,\"periodTimeUnit\":\"SECONDS\"}}"))
        ).orElseThrow();
        var exactHour = AiWorkspaceBudgetReader.read(
            List.of(flow("{\"rate\":{\"limit\":1000000,\"periodTime\":3600,\"periodTimeUnit\":\"SECONDS\"}}")),
            "plan-1"
        ).orElseThrow();

        assertThat(partialMinute.amount()).isEqualByComparingTo(new BigDecimal("1.00"));
        assertThat(partialMinute.period()).isNull();
        assertThat(exactHour.period()).isEqualTo("HOUR");
    }

    @Test
    void skips_a_disabled_cost_ratelimit_step() {
        Step disabled = new Step();
        disabled.setPolicy(AiWorkspaceBudgetReader.COST_RATELIMIT);
        disabled.setEnabled(false);
        disabled.setConfiguration(config(5_000_000, 60));
        Flow flow = new Flow();
        flow.setRequest(List.of(disabled));

        assertThat(AiWorkspaceBudgetReader.read(List.of(flow))).isEmpty();

        Flow disabledFlow = flow(config(5_000_000, 60));
        disabledFlow.setEnabled(false);
        assertThat(AiWorkspaceBudgetReader.read(List.of(disabledFlow))).isEmpty();
    }

    private static Flow flow(String configuration) {
        Step step = new Step();
        step.setPolicy(AiWorkspaceBudgetReader.COST_RATELIMIT);
        step.setConfiguration(configuration);
        Flow flow = new Flow();
        flow.setRequest(List.of(step));
        return flow;
    }

    private static String config(long microDollars, int minutes) {
        return "{\"rate\":{\"limit\":" + microDollars + ",\"periodTime\":" + minutes + ",\"periodTimeUnit\":\"MINUTES\"}}";
    }
}
