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
package io.gravitee.apim.infra.crud_service.log;

import io.gravitee.apim.core.log.crud_service.DecisionLogsCrudService;
import io.gravitee.apim.core.log.model.DecisionLog;
import io.gravitee.apim.core.log.model.DecisionLogFilters;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.api.MetricsRepository;
import io.gravitee.repository.log.v4.model.decision.DecisionLogQuery;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.v4.log.SearchLogsResponse;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import java.util.Optional;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component
@CustomLog
class DecisionLogsCrudServiceImpl implements DecisionLogsCrudService {

    private final MetricsRepository metricsRepository;

    public DecisionLogsCrudServiceImpl(@Lazy MetricsRepository metricsRepository) {
        this.metricsRepository = metricsRepository;
    }

    @Override
    public SearchLogsResponse<DecisionLog> searchDecisionLogs(
        ExecutionContext executionContext,
        DecisionLogFilters filters,
        Pageable pageable
    ) {
        var decisionPointType = filters.decisionPointType();
        if (decisionPointType == null || decisionPointType.isBlank()) {
            // A caller that forgot the family would otherwise read every kind of decision as one list.
            // That is a programming error, not an empty result, so it is refused rather than served.
            throw new IllegalArgumentException("decisionPointType is required to search decision logs");
        }
        var apiIds = filters.apiIds();
        if (apiIds != null && apiIds.isEmpty()) {
            // An empty set is a permission filter that matched nothing, and must not widen into a read of
            // the whole environment the way a null (no api restriction) legitimately does.
            return new SearchLogsResponse<>(0, List.of());
        }
        try {
            var response = metricsRepository.searchDecisionLogs(
                new QueryContext(executionContext.getOrganizationId(), executionContext.getEnvironmentId()),
                DecisionLogQuery.builder()
                    .decisionPointType(decisionPointType)
                    .apiIds(apiIds)
                    // Exclusions travel as they are: an empty one rules nothing out, so unlike apiIds
                    // above it has nothing to short-circuit.
                    .excludedApiIds(filters.excludedApiIds())
                    .applicationIds(filters.applicationIds())
                    .excludedApplicationIds(filters.excludedApplicationIds())
                    .planIds(filters.planIds())
                    .from(filters.from())
                    .to(filters.to())
                    .decisionPointIds(filters.decisionPointIds())
                    .excludedDecisionPointIds(filters.excludedDecisionPointIds())
                    .checkpoints(filters.checkpoints())
                    .callers(filters.callers())
                    .outcomes(filters.outcomes())
                    .excludedOutcomes(filters.excludedOutcomes())
                    .enforcements(filters.enforcements())
                    .verdicts(filters.verdicts())
                    .statuses(filters.statuses())
                    .subjectIds(filters.subjectIds())
                    .actorIds(filters.actorIds())
                    .actions(filters.actions())
                    .resourceIds(filters.resourceIds())
                    .caseIds(filters.caseIds())
                    .requestIds(filters.requestIds())
                    .traceIds(filters.traceIds())
                    .reasonContains(filters.reasonContains())
                    .page(pageable.getPageNumber())
                    .size(pageable.getPageSize())
                    .build()
            );
            return new SearchLogsResponse<>(response.total(), response.data().stream().map(DecisionLogsCrudServiceImpl::toDomain).toList());
        } catch (AnalyticsException e) {
            log.error("An error occurs while trying to search decision logs [decisionPointType={}]", decisionPointType, e);
            throw new TechnicalManagementException("Unable to search decision logs", e);
        }
    }

    @Override
    public Optional<DecisionLog> findDecisionLog(ExecutionContext executionContext, String apiId, String eventId) {
        try {
            return metricsRepository
                .findDecisionLog(
                    new QueryContext(executionContext.getOrganizationId(), executionContext.getEnvironmentId()),
                    apiId,
                    eventId
                )
                .map(DecisionLogsCrudServiceImpl::toDomain);
        } catch (AnalyticsException e) {
            log.error("An error occurs while trying to find decision [apiId={}, eventId={}]", apiId, eventId, e);
            throw new TechnicalManagementException("Unable to find decision " + eventId, e);
        }
    }

    private static DecisionLog toDomain(io.gravitee.repository.log.v4.model.decision.DecisionLog decision) {
        return DecisionLog.builder()
            .timestamp(decision.timestamp())
            .gatewayId(decision.gatewayId())
            .organizationId(decision.organizationId())
            .environmentId(decision.environmentId())
            .apiId(decision.apiId())
            .planId(decision.planId())
            .applicationId(decision.applicationId())
            .eventId(decision.eventId())
            .caseId(decision.caseId())
            .batchId(decision.batchId())
            .phase(decision.phase())
            .decisionPointType(decision.decisionPointType())
            .decisionPointId(decision.decisionPointId())
            .decisionPointVersion(decision.decisionPointVersion())
            .checkpoint(decision.checkpoint())
            .caller(decision.caller())
            .subjectType(decision.subjectType())
            .subjectId(decision.subjectId())
            .actorType(decision.actorType())
            .actorId(decision.actorId())
            .action(decision.action())
            .resourceType(decision.resourceType())
            .resourceId(decision.resourceId())
            .argsHash(decision.argsHash())
            .outcome(decision.outcome())
            .enforced(decision.enforced())
            .verdict(decision.verdict())
            .indeterminateCause(decision.indeterminateCause())
            .confidence(decision.confidence())
            .reasons(decision.reasons())
            .matchedRules(toDomain(decision.matchedRules()))
            .transformed(decision.transformed())
            .transformationType(decision.transformationType())
            .requiredApprover(decision.requiredApprover())
            .deciderType(decision.deciderType())
            .deciderId(decision.deciderId())
            .channel(decision.channel())
            .requestId(decision.requestId())
            .traceId(decision.traceId())
            .conversationId(decision.conversationId())
            .missionId(decision.missionId())
            .status(decision.status())
            .errorType(decision.errorType())
            .durationNanos(decision.durationNanos())
            .waitedNanos(decision.waitedNanos())
            .additionalMetrics(decision.additionalMetrics())
            .build();
    }

    private static List<DecisionLog.MatchedRule> toDomain(
        List<io.gravitee.repository.log.v4.model.decision.DecisionLog.MatchedRule> rules
    ) {
        if (rules == null) {
            return List.of();
        }
        return rules
            .stream()
            .map(rule -> new DecisionLog.MatchedRule(rule.id(), rule.name(), rule.effect()))
            .toList();
    }
}
