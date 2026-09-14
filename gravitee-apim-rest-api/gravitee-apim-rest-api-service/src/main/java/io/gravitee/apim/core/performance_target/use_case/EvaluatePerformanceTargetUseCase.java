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
import io.gravitee.apim.core.performance_target.crud_service.PerformanceTargetCrudService;
import io.gravitee.apim.core.performance_target.crud_service.PerformanceTargetEvaluationCrudService;
import io.gravitee.apim.core.performance_target.domain_service.PerformanceTargetScheduleStateDomainService;
import io.gravitee.apim.core.performance_target.domain_service.PerformanceTargetTransitionDomainService;
import io.gravitee.apim.core.performance_target.exception.PerformanceTargetEvaluatedTooRecentlyException;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetEvaluation;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetNotificationPolicy;
import io.gravitee.apim.core.performance_target.model.PerformanceTargetRuleTransition;
import io.gravitee.apim.core.performance_target.query_service.PerformanceTargetEvaluationQueryService;
import io.gravitee.apim.core.performance_target.service_provider.PerformanceTargetEvaluator;
import io.gravitee.apim.core.performance_target.service_provider.PerformanceTargetTransitionPublisher;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.common.UuidString;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * Evaluates a target on demand and stores the result as its latest evaluation, through the same evaluator as the
 * scheduler, then prunes the history beyond {@link PerformanceTargetEvaluation#HISTORY_RETENTION}. A target is
 * evaluated at most once per {@link #MIN_DELAY_BETWEEN_EVALUATIONS}, whoever triggered the previous run, so a UI
 * refresh cannot turn into a query storm. The rules whose verdict changed with this evaluation are published for
 * the owners to be told, as the scheduler does.
 */
@RequiredArgsConstructor
@UseCase
public class EvaluatePerformanceTargetUseCase {

    public static final Duration MIN_DELAY_BETWEEN_EVALUATIONS = Duration.ofSeconds(30);

    private final PerformanceTargetCrudService performanceTargetCrudService;
    private final PerformanceTargetEvaluationQueryService performanceTargetEvaluationQueryService;
    private final PerformanceTargetEvaluationCrudService performanceTargetEvaluationCrudService;
    private final PerformanceTargetEvaluator performanceTargetEvaluator;
    private final PerformanceTargetScheduleStateDomainService scheduleState;
    private final PerformanceTargetTransitionDomainService transitions;
    private final PerformanceTargetTransitionPublisher transitionPublisher;
    private final PerformanceTargetNotificationPolicy notificationPolicy;

    public Output execute(Input input) {
        var target = performanceTargetCrudService.get(input.environmentId(), input.targetId());

        var now = TimeProvider.instantNow();
        // As deep as a transition needs to look back: the latest evaluation, which also guards the rate, comes first.
        var history = performanceTargetEvaluationQueryService
            .findByTargetId(target.id(), new PageableImpl(1, notificationPolicy.notEvaluableAfter()))
            .getContent();
        history
            .stream()
            .findFirst()
            .map(latest -> Duration.between(now, latest.evaluatedAt().plus(MIN_DELAY_BETWEEN_EVALUATIONS)))
            .filter(Duration::isPositive)
            .ifPresent(retryAfter -> {
                throw new PerformanceTargetEvaluatedTooRecentlyException(target.id(), retryAfter);
            });

        var evaluation = performanceTargetEvaluator.evaluate(target, now).toBuilder().id(UuidString.generateRandom()).latest(true).build();
        var stored = performanceTargetEvaluationCrudService.create(evaluation);
        performanceTargetEvaluationCrudService.pruneHistory(target.id(), PerformanceTargetEvaluation.HISTORY_RETENTION);
        // The scheduler judges a backoff on the latest evaluations it knows of; this one counts as much as its own.
        scheduleState.record(target.id(), stored);
        var changed = transitions.detect(target, stored, history, notificationPolicy);
        if (!changed.isEmpty()) {
            transitionPublisher.publish(changed);
        }
        return new Output(stored, changed);
    }

    public record Input(String environmentId, String targetId) {}

    /**
     * @param transitions the rules whose verdict changed with this evaluation
     */
    public record Output(PerformanceTargetEvaluation evaluation, List<PerformanceTargetRuleTransition> transitions) {}
}
