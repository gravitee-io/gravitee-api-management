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

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.performance_target.crud_service.PerformanceTargetCrudService;
import io.gravitee.apim.core.performance_target.crud_service.PerformanceTargetEvaluationCrudService;
import io.gravitee.apim.core.performance_target.exception.PerformanceTargetEvaluatedTooRecentlyException;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation;
import io.gravitee.apim.core.performance_target.query_service.PerformanceTargetEvaluationQueryService;
import io.gravitee.apim.core.performance_target.service_provider.PerformanceTargetEvaluator;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * Evaluates a target on demand and stores the result as its latest evaluation, through the same evaluator as the
 * scheduler. A target is evaluated at most once per {@link #MIN_DELAY_BETWEEN_EVALUATIONS}, whoever triggered the
 * previous run, so a UI refresh cannot turn into a query storm.
 */
@RequiredArgsConstructor
@UseCase
public class EvaluatePerformanceTargetUseCase {

    public static final Duration MIN_DELAY_BETWEEN_EVALUATIONS = Duration.ofSeconds(30);

    private final PerformanceTargetCrudService performanceTargetCrudService;
    private final PerformanceTargetEvaluationQueryService performanceTargetEvaluationQueryService;
    private final PerformanceTargetEvaluationCrudService performanceTargetEvaluationCrudService;
    // Optional until the evaluation engine lands: the REST contract ships first, the engine follows.
    private final Optional<PerformanceTargetEvaluator> performanceTargetEvaluator;

    public Output execute(Input input) {
        var target = performanceTargetCrudService.get(input.environmentId(), input.targetId());

        var now = TimeProvider.instantNow();
        performanceTargetEvaluationQueryService
            .findByTargetId(target.id(), new PageableImpl(1, 1))
            .getContent()
            .stream()
            .findFirst()
            .map(latest -> Duration.between(now, latest.evaluatedAt().plus(MIN_DELAY_BETWEEN_EVALUATIONS)))
            .filter(Duration::isPositive)
            .ifPresent(retryAfter -> {
                throw new PerformanceTargetEvaluatedTooRecentlyException(target.id(), retryAfter);
            });

        var evaluator = performanceTargetEvaluator.orElseThrow(() ->
            new TechnicalDomainException("Performance target evaluation is not available")
        );
        var evaluation = evaluator.evaluate(target, now).toBuilder().id(UuidString.generateRandom()).latest(true).build();
        return new Output(performanceTargetEvaluationCrudService.create(evaluation));
    }

    public record Input(String environmentId, String targetId) {}

    public record Output(PerformanceTargetEvaluation evaluation) {}
}
