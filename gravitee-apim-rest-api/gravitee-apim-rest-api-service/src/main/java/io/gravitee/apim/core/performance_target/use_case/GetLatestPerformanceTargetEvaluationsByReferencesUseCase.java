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
package io.gravitee.apim.core.performance_target.use_case;

import static java.util.stream.Collectors.groupingBy;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation;
import io.gravitee.apim.core.performance_target.query_service.PerformanceTargetEvaluationQueryService;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * The status to show next to each reference of a list: its worst latest evaluation, so that a reference with a
 * target that cannot be evaluated never reads as a pass.
 */
@RequiredArgsConstructor
@UseCase
public class GetLatestPerformanceTargetEvaluationsByReferencesUseCase {

    private static final Comparator<PerformanceTargetEvaluation> WORST_LAST = Comparator.comparing(
        (PerformanceTargetEvaluation evaluation) -> severity(evaluation.status())
    ).thenComparing(PerformanceTargetEvaluation::evaluatedAt);

    private final PerformanceTargetEvaluationQueryService performanceTargetEvaluationQueryService;

    public Output execute(Input input) {
        var latestByReference = performanceTargetEvaluationQueryService
            .findLatestByReferences(input.environmentId(), input.references())
            .stream()
            .collect(groupingBy(PerformanceTargetEvaluation::reference));

        var result = new LinkedHashMap<String, PerformanceTargetEvaluation>();
        for (var reference : input.references()) {
            result.put(reference, latestByReference.getOrDefault(reference, List.of()).stream().max(WORST_LAST).orElse(null));
        }
        return new Output(result);
    }

    private static int severity(PerformanceTargetEvaluation.Status status) {
        return switch (status) {
            case BREACH -> 2;
            case NOT_EVALUABLE -> 1;
            case PASS -> 0;
        };
    }

    public record Input(String environmentId, Collection<String> references) {}

    /**
     * @param latestByReference one entry per requested reference, in request order; {@code null} when the reference has
     *                          no evaluation
     */
    public record Output(Map<String, PerformanceTargetEvaluation> latestByReference) {}
}
