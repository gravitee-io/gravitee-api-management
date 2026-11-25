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
package io.gravitee.apim.infra.query_service.log;

import io.gravitee.apim.core.log.model.GuardianDecision;
import io.gravitee.apim.core.log.model.GuardianDecisionFilters;
import io.gravitee.apim.core.log.query_service.GuardianDecisionQueryService;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.api.MetricsRepository;
import io.gravitee.repository.log.v4.model.guardian.GuardianDecisionLog;
import io.gravitee.repository.log.v4.model.guardian.GuardianDecisionLogQuery;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.v4.log.SearchLogsResponse;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import java.util.Optional;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * @author GraviteeSource Team
 */
@Service
@CustomLog
public class GuardianDecisionQueryServiceImpl implements GuardianDecisionQueryService {

    private final MetricsRepository metricsRepository;

    public GuardianDecisionQueryServiceImpl(@Lazy MetricsRepository metricsRepository) {
        this.metricsRepository = metricsRepository;
    }

    @Override
    public SearchLogsResponse<GuardianDecision> search(
        ExecutionContext executionContext,
        GuardianDecisionFilters filters,
        Pageable pageable
    ) {
        if (filters.apiIds() != null && filters.apiIds().isEmpty()) {
            return new SearchLogsResponse<>(0, List.of());
        }
        try {
            var response = metricsRepository.searchGuardianDecisionLogs(
                queryContext(executionContext),
                GuardianDecisionLogQuery.builder()
                    .apiIds(filters.apiIds())
                    .guardianIds(filters.guardianIds())
                    .outcomes(filters.outcomes())
                    .from(filters.from())
                    .to(filters.to())
                    .requestIds(filters.requestIds())
                    .requestIdContains(filters.requestIdContains())
                    .page(pageable.getPageNumber())
                    .size(pageable.getPageSize())
                    .build()
            );
            return new SearchLogsResponse<>(
                response.total(),
                response.data().stream().map(GuardianDecisionQueryServiceImpl::toDomain).toList()
            );
        } catch (AnalyticsException e) {
            log.error(
                "An error occurs while trying to search guardian decisions [environmentId={}, apiIds={}]",
                executionContext.getEnvironmentId(),
                filters.apiIds(),
                e
            );
            throw new TechnicalManagementException("Unable to search guardian decisions", e);
        }
    }

    @Override
    public Optional<GuardianDecision> findById(ExecutionContext executionContext, String decisionId) {
        try {
            return metricsRepository
                .findGuardianDecisionLog(queryContext(executionContext), decisionId)
                .map(GuardianDecisionQueryServiceImpl::toDomain);
        } catch (AnalyticsException e) {
            log.error(
                "An error occurs while trying to find guardian decision [environmentId={}, decisionId={}]",
                executionContext.getEnvironmentId(),
                decisionId,
                e
            );
            throw new TechnicalManagementException("Unable to find guardian decision " + decisionId, e);
        }
    }

    private static QueryContext queryContext(ExecutionContext executionContext) {
        return new QueryContext(executionContext.getOrganizationId(), executionContext.getEnvironmentId());
    }

    private static GuardianDecision toDomain(GuardianDecisionLog decision) {
        return GuardianDecision.builder()
            .id(decision.eventId())
            .timestamp(decision.timestamp())
            .organizationId(decision.organizationId())
            .environmentId(decision.environmentId())
            .gatewayId(decision.gatewayId())
            .apiId(decision.apiId())
            .planId(decision.planId())
            .applicationId(decision.applicationId())
            .guardianId(decision.guardianId())
            .guardianRevision(decision.guardianRevision())
            .checkpoint(decision.checkpoint())
            .caller(decision.caller())
            .requestId(decision.requestId())
            .traceId(decision.traceId())
            .outcome(decision.outcome())
            .enforced(decision.enforced())
            .verdict(decision.verdict())
            .indeterminateCause(decision.indeterminateCause())
            .errorType(decision.errorType())
            .status(decision.status())
            .reasons(decision.reasons())
            .matchedRules(toDomain(decision.matchedRules()))
            .transformed(decision.transformed())
            .transformationType(decision.transformationType())
            .durationNanos(decision.durationNanos())
            .subjectType(decision.subjectType())
            .subjectId(decision.subjectId())
            .actorType(decision.actorType())
            .actorId(decision.actorId())
            .additionalMetrics(decision.additionalMetrics())
            .build();
    }

    private static List<GuardianDecision.MatchedRule> toDomain(List<GuardianDecisionLog.MatchedRule> matchedRules) {
        if (matchedRules == null) {
            return List.of();
        }
        return matchedRules
            .stream()
            .map(rule -> new GuardianDecision.MatchedRule(rule.id(), rule.name(), rule.version(), rule.effect(), rule.annotations()))
            .toList();
    }
}
