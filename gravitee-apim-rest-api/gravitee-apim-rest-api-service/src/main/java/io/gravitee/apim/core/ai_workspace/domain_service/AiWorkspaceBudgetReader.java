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

/**
 * Reads the allocated budget from a plan flow's {@code cost-ratelimit} policy.
 * The limit is stored in micro-dollars and the window in minutes, which is how the Gamma AI Workspace writes it.
 */
public final class AiWorkspaceBudgetReader {

    static final String COST_RATELIMIT = "cost-ratelimit";
    private static final BigDecimal MICRO_DOLLARS = BigDecimal.valueOf(1_000_000L);
    private static final Map<Integer, String> PERIOD_BY_MINUTES = Map.of(60, "HOUR", 1_440, "DAY", 10_080, "WEEK", 43_200, "MONTH");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AiWorkspaceBudgetReader() {}

    public static Optional<AiWorkspaceBudget> read(List<Flow> flows) {
        if (flows == null) {
            return Optional.empty();
        }
        for (Flow flow : flows) {
            if (flow.getRequest() == null) {
                continue;
            }
            for (Step step : flow.getRequest()) {
                if (step != null && COST_RATELIMIT.equals(step.getPolicy())) {
                    Optional<AiWorkspaceBudget> budget = readConfiguration(step.getConfiguration());
                    if (budget.isPresent()) {
                        return budget;
                    }
                }
            }
        }
        return Optional.empty();
    }

    static Optional<AiWorkspaceBudget> readConfiguration(String configuration) {
        if (configuration == null || configuration.isBlank()) {
            return Optional.empty();
        }
        JsonNode rate;
        try {
            rate = MAPPER.readTree(configuration).path("rate");
        } catch (Exception e) {
            return Optional.empty();
        }
        if (!rate.path("limit").isNumber()) {
            return Optional.empty();
        }
        BigDecimal amount = BigDecimal.valueOf(rate.path("limit").asLong()).divide(MICRO_DOLLARS, 2, RoundingMode.HALF_UP);
        String period = rate.path("periodTime").isNumber() ? PERIOD_BY_MINUTES.get(rate.path("periodTime").asInt()) : null;
        return Optional.of(new AiWorkspaceBudget(amount, period));
    }
}
