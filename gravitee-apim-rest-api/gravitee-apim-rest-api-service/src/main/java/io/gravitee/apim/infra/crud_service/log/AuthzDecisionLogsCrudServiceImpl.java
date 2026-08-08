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

import io.gravitee.apim.core.log.crud_service.AuthzDecisionLogsCrudService;
import io.gravitee.apim.core.log.model.AuthzDecisionLog;
import io.gravitee.apim.core.log.model.AuthzDecisionLogFilters;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.api.MetricsRepository;
import io.gravitee.repository.log.v4.model.authz.AuthzDecisionLogQuery;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.v4.log.SearchLogsResponse;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import java.util.Set;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component
@CustomLog
class AuthzDecisionLogsCrudServiceImpl implements AuthzDecisionLogsCrudService {

    private final MetricsRepository metricsRepository;

    public AuthzDecisionLogsCrudServiceImpl(@Lazy MetricsRepository metricsRepository) {
        this.metricsRepository = metricsRepository;
    }

    @Override
    public SearchLogsResponse<AuthzDecisionLog> searchDecisionLogs(
        ExecutionContext executionContext,
        AuthzDecisionLogFilters filters,
        Pageable pageable
    ) {
        var apiIds = filters.apiIds();
        if (apiIds == null || apiIds.isEmpty()) {
            return new SearchLogsResponse<>(0, List.of());
        }
        try {
            var response = metricsRepository.searchAuthzDecisionLogs(
                new QueryContext(executionContext.getOrganizationId(), executionContext.getEnvironmentId()),
                AuthzDecisionLogQuery.builder()
                    .apiIds(apiIds)
                    .from(filters.from())
                    .to(filters.to())
                    .decisions(filters.decisions())
                    .page(pageable.getPageNumber())
                    .size(pageable.getPageSize())
                    .build()
            );
            return new SearchLogsResponse<>(
                response.total(),
                response.data().stream().map(AuthzDecisionLogsCrudServiceImpl::toDomain).toList()
            );
        } catch (AnalyticsException e) {
            log.error("An error occurs while trying to search authz decision logs [apiIds={}]", apiIds, e);
            throw new TechnicalManagementException("Unable to search authz decision logs", e);
        }
    }

    private static AuthzDecisionLog toDomain(io.gravitee.repository.log.v4.model.authz.AuthzDecisionLog decision) {
        return AuthzDecisionLog.builder()
            .eventId(decision.eventId())
            .timestamp(decision.timestamp())
            .apiId(decision.apiId())
            .organizationId(decision.organizationId())
            .environmentId(decision.environmentId())
            .gatewayId(decision.gatewayId())
            .requestId(decision.requestId())
            .operation(decision.operation())
            .status(decision.status())
            .caller(decision.caller())
            .targetPdpId(decision.targetPdpId())
            .policyGeneration(decision.policyGeneration())
            .decision(decision.decision())
            .matchedPolicyNames(decision.matchedPolicyNames())
            .reasons(decision.reasons())
            .subjectType(decision.subjectType())
            .subjectId(decision.subjectId())
            .action(decision.action())
            .resourceType(decision.resourceType())
            .resourceId(decision.resourceId())
            .batchId(decision.batchId())
            .batchIndex(decision.batchIndex())
            .batchSize(decision.batchSize())
            .searchType(decision.searchType())
            .resultCount(decision.resultCount())
            .durationNanos(decision.durationNanos())
            .build();
    }
}
