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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.ai_workspace.model.AiWorkspaceBudget;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.step.Step;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.CustomLog;

/**
 * Reads the allocated budget from a plan flow's {@code cost-ratelimit} policy.
 * The limit is stored in micro-dollars. {@code periodTime} and {@code periodTimeUnit} are converted to minutes
 * before the window lookup (60 HOUR, 1440 DAY, 10080 WEEK, 43200 MONTH). Gamma writes the unit explicitly,
 * for example {@code periodTime: 10080, periodTimeUnit: MINUTES}. A missing unit is treated as minutes.
 * Any other window still returns the amount, with no period.
 */
@CustomLog
public final class AiWorkspaceBudgetReader {

    static final String COST_RATELIMIT = "cost-ratelimit";
    private static final BigDecimal MICRO_DOLLARS = BigDecimal.valueOf(1_000_000L);
    private static final Map<Integer, String> PERIOD_BY_MINUTES = Map.of(60, "HOUR", 1_440, "DAY", 10_080, "WEEK", 43_200, "MONTH");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AiWorkspaceBudgetReader() {}

    public static Optional<AiWorkspaceBudget> read(List<Flow> flows) {
        return read(flows, null);
    }

    public static Optional<AiWorkspaceBudget> read(List<Flow> flows, String planId) {
        if (flows == null) {
            return Optional.empty();
        }
        for (Flow flow : flows) {
            if (flow == null || !flow.isEnabled() || flow.getRequest() == null) {
                continue;
            }
            for (Step step : flow.getRequest()) {
                if (step != null && step.isEnabled() && COST_RATELIMIT.equals(step.getPolicy())) {
                    Optional<AiWorkspaceBudget> budget = readConfiguration(step.getConfiguration(), planId);
                    if (budget.isPresent()) {
                        return budget;
                    }
                }
            }
        }
        return Optional.empty();
    }

    static Optional<AiWorkspaceBudget> readConfiguration(String configuration, String planId) {
        if (configuration == null || configuration.isBlank()) {
            return Optional.empty();
        }
        JsonNode rate;
        try {
            rate = MAPPER.readTree(configuration).path("rate");
        } catch (Exception exception) {
            log.warn("Could not read cost-ratelimit configuration for plan {}", planId, exception);
            return Optional.empty();
        }
        if (!rate.path("limit").isNumber()) {
            return Optional.empty();
        }
        BigDecimal amount = BigDecimal.valueOf(rate.path("limit").asLong()).divide(MICRO_DOLLARS, 2, RoundingMode.HALF_UP);
        String period = period(rate);
        return Optional.of(new AiWorkspaceBudget(amount, period));
    }

    private static String period(JsonNode rate) {
        Integer minutes = toMinutes(rate);
        if (minutes == null) {
            return null;
        }
        String period = PERIOD_BY_MINUTES.get(minutes);
        if (period == null && rate.path("periodTime").isNumber()) {
            log.debug(
                "cost-ratelimit window periodTime {} {} is not HOUR, DAY, WEEK or MONTH",
                rate.path("periodTime").asInt(),
                rate.path("periodTimeUnit").asText("")
            );
        }
        return period;
    }

    /**
     * @return minutes, or null when the unit is present but not one this reader understands
     */
    private static Integer toMinutes(JsonNode rate) {
        if (!rate.path("periodTime").isNumber()) {
            return null;
        }
        int periodTime = rate.path("periodTime").asInt();
        if (!rate.hasNonNull("periodTimeUnit") || rate.path("periodTimeUnit").asText().isBlank()) {
            return periodTime;
        }
        try {
            return switch (rate.path("periodTimeUnit").asText()) {
                case "SECONDS" -> periodTime % 60 == 0 ? periodTime / 60 : null;
                case "MINUTES" -> periodTime;
                case "HOURS" -> Math.multiplyExact(periodTime, 60);
                case "DAYS" -> Math.multiplyExact(periodTime, 1_440);
                case "WEEKS" -> Math.multiplyExact(periodTime, 10_080);
                case "MONTHS" -> Math.multiplyExact(periodTime, 43_200);
                default -> null;
            };
        } catch (ArithmeticException overflow) {
            log.debug("cost-ratelimit periodTime {} {} overflows", periodTime, rate.path("periodTimeUnit").asText());
            return null;
        }
    }
}
