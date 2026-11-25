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

import io.gravitee.apim.core.log.crud_service.GuardrailDecisionLogsCrudService;
import io.gravitee.apim.core.log.model.GuardrailDecisionLog;
import io.gravitee.apim.core.log.model.GuardrailDecisionLogFilters;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.api.MetricsRepository;
import io.gravitee.repository.log.v4.model.guardrail.GuardrailDecisionLogQuery;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.v4.log.SearchLogsResponse;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author GraviteeSource Team
 */
@Component
@CustomLog
class GuardrailDecisionLogsCrudServiceImpl implements GuardrailDecisionLogsCrudService {

    private final MetricsRepository metricsRepository;

    public GuardrailDecisionLogsCrudServiceImpl(@Lazy MetricsRepository metricsRepository) {
        this.metricsRepository = metricsRepository;
    }

    @Override
    public SearchLogsResponse<GuardrailDecisionLog> searchDecisionLogs(
        ExecutionContext executionContext,
        GuardrailDecisionLogFilters filters,
        Pageable pageable
    ) {
        var apiIds = filters.apiIds();
        if (apiIds == null || apiIds.isEmpty()) {
            return new SearchLogsResponse<>(0, List.of());
        }

        try {
            var response = metricsRepository.searchGuardrailDecisionLogs(
                new QueryContext(executionContext.getOrganizationId(), executionContext.getEnvironmentId()),
                GuardrailDecisionLogQuery.builder()
                    .apiIds(apiIds)
                    .from(filters.from())
                    .to(filters.to())
                    .guardrailIds(filters.guardrailIds())
                    .outcomes(filters.outcomes())
                    .text(filters.text())
                    .textApiIds(filters.textApiIds())
                    .page(pageable.getPageNumber())
                    .size(pageable.getPageSize())
                    .build()
            );
            return new SearchLogsResponse<>(
                response.total(),
                response.data().stream().map(GuardrailDecisionLogsCrudServiceImpl::toDomain).toList()
            );
        } catch (AnalyticsException e) {
            log.error("An error occurs while trying to search Guardrail decision logs [apiIds={}]", apiIds, e);
            throw new TechnicalManagementException("Unable to search Guardrail decision logs", e);
        }
    }

    private static GuardrailDecisionLog toDomain(io.gravitee.repository.log.v4.model.guardrail.GuardrailDecisionLog decision) {
        return GuardrailDecisionLog.builder()
            .eventId(decision.eventId())
            .timestamp(decision.timestamp())
            .apiId(decision.apiId())
            .requestId(decision.requestId())
            .guardrailId(decision.guardrailId())
            .revision(decision.revision())
            .checkpoint(decision.checkpoint())
            .outcome(decision.outcome())
            .enforced(decision.enforced())
            .transformationType(decision.transformationType())
            .verdict(decision.verdict())
            .reasons(decision.reasons())
            .durationNanos(decision.durationNanos())
            .findings(toDomain(decision.findings()))
            .build();
    }

    private static List<GuardrailDecisionLog.Finding> toDomain(
        List<io.gravitee.repository.log.v4.model.guardrail.GuardrailDecisionLog.Finding> findings
    ) {
        if (findings == null) {
            return List.of();
        }
        return findings
            .stream()
            .map(finding ->
                new GuardrailDecisionLog.Finding(finding.guardrailPolicy(), finding.rule(), finding.location(), finding.effect())
            )
            .toList();
    }
}
