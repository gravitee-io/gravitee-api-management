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
package fixtures.core.model;

import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.performance_target.model.PerformanceTarget;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Supplier;

public class PerformanceTargetFixtures {

    private PerformanceTargetFixtures() {}

    public static final String ENVIRONMENT_ID = "environment-id";
    public static final String A2A_API_ID = "a2a-api";

    public static final Supplier<PerformanceTarget.PerformanceTargetBuilder> BASE = () ->
        PerformanceTarget.builder()
            .id("target-id")
            .environmentId(ENVIRONMENT_ID)
            .subject(new PerformanceTarget.Subject(List.of(A2A_API_ID), A2A_API_ID))
            .window(Duration.ofMinutes(15))
            .interval(Duration.ofMinutes(5))
            .minSampleSize(20)
            .rules(List.of(aLatencyRule()))
            .createdAt(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneId.systemDefault()))
            .updatedAt(Instant.parse("2020-02-03T20:22:02.00Z").atZone(ZoneId.systemDefault()));

    public static PerformanceTarget aTarget() {
        return BASE.get().build();
    }

    public static PerformanceTarget aTarget(String id) {
        return BASE.get().id(id).build();
    }

    public static PerformanceTarget.Rule aLatencyRule() {
        return PerformanceTarget.Rule.builder()
            .metric(MetricSpec.Name.HTTP_GATEWAY_RESPONSE_TIME)
            .measure(MetricSpec.Measure.P95)
            .operator(PerformanceTarget.Operator.LTE)
            .threshold(2000)
            .build();
    }

    public static PerformanceTargetEvaluation anEvaluation(String id, String targetId, PerformanceTargetEvaluation.Status status) {
        return anEvaluation(id, targetId, status, Instant.parse("2020-02-03T20:37:02.00Z"));
    }

    public static PerformanceTargetEvaluation anEvaluation(
        String id,
        String targetId,
        PerformanceTargetEvaluation.Status status,
        Instant evaluatedAt
    ) {
        var breached = status == PerformanceTargetEvaluation.Status.BREACH;
        return PerformanceTargetEvaluation.builder()
            .id(id)
            .targetId(targetId)
            .environmentId(ENVIRONMENT_ID)
            .reference(A2A_API_ID)
            .status(status)
            .rules(
                List.of(
                    new PerformanceTargetEvaluation.RuleResult(
                        MetricSpec.Name.HTTP_GATEWAY_RESPONSE_TIME,
                        MetricSpec.Measure.P95,
                        PerformanceTarget.Operator.LTE,
                        2000,
                        status == PerformanceTargetEvaluation.Status.NOT_EVALUABLE ? null : breached ? 4810.0 : 1500.0,
                        status == PerformanceTargetEvaluation.Status.NOT_EVALUABLE
                            ? null
                            : new PerformanceTargetEvaluation.Deviation(breached ? 2810 : -500, breached ? 1.405 : -0.25),
                        status == PerformanceTargetEvaluation.Status.NOT_EVALUABLE ? 0 : 63,
                        status
                    )
                )
            )
            .windowFrom(evaluatedAt.minus(Duration.ofMinutes(15)))
            .windowTo(evaluatedAt)
            .coveredApiIds(List.of(A2A_API_ID))
            .evaluatedAt(evaluatedAt)
            .latest(true)
            .build();
    }
}
