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

import io.gravitee.apim.core.log.crud_service.ConnectionLogsCrudService;
import io.gravitee.apim.infra.adapter.ConnectionLogAdapter;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.api.LogRepository;
import io.gravitee.repository.log.v4.api.MetricsRepository;
import io.gravitee.repository.log.v4.model.LogResponse;
import io.gravitee.repository.log.v4.model.connection.ConnectionLogDetailQuery;
import io.gravitee.repository.log.v4.model.connection.Metrics;
import io.gravitee.repository.log.v4.model.connection.MetricsQuery;
import io.gravitee.rest.api.model.analytics.SearchLogsFilters;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.model.v4.log.SearchLogsResponse;
import io.gravitee.rest.api.model.v4.log.connection.BaseConnectionLog;
import io.gravitee.rest.api.model.v4.log.connection.ConnectionLogDetail;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@CustomLog
class ConnectionLogsCrudServiceImpl implements ConnectionLogsCrudService {

    private final LogRepository logRepository;
    private final MetricsRepository metricsRepository;

    public ConnectionLogsCrudServiceImpl(@Lazy LogRepository logRepository, @Lazy MetricsRepository metricsRepository) {
        this.logRepository = logRepository;
        this.metricsRepository = metricsRepository;
    }

    @Override
    public SearchLogsResponse<BaseConnectionLog> searchApiConnectionLogs(
        ExecutionContext executionContext,
        SearchLogsFilters logsFilters,
        Pageable pageable,
        List<DefinitionVersion> definitionVersions
    ) {
        return searchApiConnectionLogs(executionContext, logsFilters.apiIds(), logsFilters, pageable, definitionVersions);
    }

    @Override
    public SearchLogsResponse<BaseConnectionLog> searchApiConnectionLogs(
        ExecutionContext executionContext,
        String apiId,
        SearchLogsFilters logsFilters,
        Pageable pageable,
        List<DefinitionVersion> definitionVersions
    ) {
        return searchApiConnectionLogs(executionContext, Set.of(apiId), logsFilters, pageable, definitionVersions);
    }

    @Override
    public SearchLogsResponse<BaseConnectionLog> searchApiConnectionLogs(
        ExecutionContext executionContext,
        Set<String> apiIds,
        SearchLogsFilters logsFilters,
        Pageable pageable,
        List<DefinitionVersion> definitionVersions
    ) {
        var connectionLogsFilterBuilder = mapToConnectionLogQueryFilterBuilder(logsFilters).apiIds(apiIds);
        var detailFilterBuilder = mapToConnectionLogDetailQueryFilterBuilder(logsFilters).apiIds(apiIds);

        try {
            return searchConnectionLogsWithOptionalBodyText(
                executionContext,
                logsFilters,
                pageable,
                definitionVersions,
                connectionLogsFilterBuilder,
                detailFilterBuilder
            );
        } catch (AnalyticsException e) {
            String joinedApiIds = String.join(",", apiIds);
            log.error("An error occurs while trying to search connection logs of api [apiIds={}]", joinedApiIds, e);
            throw new TechnicalManagementException("Error while searching connection logs of api " + joinedApiIds, e);
        }
    }

    @Override
    public SearchLogsResponse<BaseConnectionLog> searchApplicationConnectionLogs(
        ExecutionContext executionContext,
        String applicationId,
        SearchLogsFilters logsFilters,
        Pageable pageable
    ) {
        var connectionLogsFilterBuilder = mapToConnectionLogQueryFilterBuilder(logsFilters).applicationIds(Set.of(applicationId));
        var detailFilterBuilder = mapToConnectionLogDetailQueryFilterBuilder(logsFilters);

        try {
            return searchConnectionLogsWithOptionalBodyText(
                executionContext,
                logsFilters,
                pageable,
                List.of(DefinitionVersion.V2, DefinitionVersion.V4),
                connectionLogsFilterBuilder,
                detailFilterBuilder
            );
        } catch (AnalyticsException e) {
            log.error("An error occurs while trying to search application connection logs [applicationId={}]", applicationId, e);
            throw new TechnicalManagementException("Error while searching application connection logs " + applicationId, e);
        }
    }

    @Override
    public Optional<ConnectionLogDetail> searchApiConnectionLog(ExecutionContext executionContext, String apiId, String requestId) {
        try {
            var response = logRepository.searchConnectionLogDetail(
                new QueryContext(executionContext.getOrganizationId(), executionContext.getEnvironmentId()),
                ConnectionLogDetailQuery.builder()
                    .filter(ConnectionLogDetailQuery.Filter.builder().apiIds(Set.of(apiId)).requestIds(Set.of(requestId)).build())
                    .build()
            );
            return response.map(this::mapToConnectionLogDetail);
        } catch (AnalyticsException e) {
            log.error("An error occurs while trying to search connection log of api [apiId={}, requestId={}]", apiId, requestId, e);
            throw new TechnicalManagementException("Error while searching connection log of api " + apiId + " requestId " + requestId, e);
        }
    }

    private SearchLogsResponse<BaseConnectionLog> searchConnectionLogsWithOptionalBodyText(
        ExecutionContext executionContext,
        SearchLogsFilters logsFilters,
        Pageable pageable,
        List<DefinitionVersion> definitionVersions,
        MetricsQuery.Filter.FilterBuilder metricsFilterBuilder,
        ConnectionLogDetailQuery.Filter.FilterBuilder detailFilterBuilder
    ) throws AnalyticsException {
        var hasBodyTextFilter = logsFilters.bodyText() != null && !logsFilters.bodyText().isBlank();
        long bodySearchTotal = 0L;

        if (hasBodyTextFilter) {
            var bodySearchResponse = searchConnectionLogDetails(
                executionContext,
                ConnectionLogDetailQuery.builder()
                    .projectionFields(List.of("_id", "request-id"))
                    .filter(detailFilterBuilder.build())
                    .page(pageable.getPageNumber())
                    .size(pageable.getPageSize())
                    .build()
            );

            if (bodySearchResponse.total() == 0L) {
                return new SearchLogsResponse<>(0, new ArrayList<>());
            }

            bodySearchTotal = bodySearchResponse.total();

            var matchedRequestIds = bodySearchResponse
                .data()
                .stream()
                .map(io.gravitee.repository.log.v4.model.connection.ConnectionLogDetail::getRequestId)
                .collect(Collectors.toSet());
            metricsFilterBuilder.requestIds(matchedRequestIds);
        }

        var metricsPageable = hasBodyTextFilter ? new PageableImpl(1, pageable.getPageSize()) : pageable;
        var response = getConnectionLogsResponse(executionContext, metricsFilterBuilder.build(), metricsPageable, definitionVersions);

        if (hasBodyTextFilter && response.total() == pageable.getPageSize()) {
            return mapToConnectionResponse(new LogResponse<>(bodySearchTotal, response.data()));
        }

        return mapToConnectionResponse(response);
    }

    private SearchLogsResponse<BaseConnectionLog> mapToConnectionResponse(LogResponse<Metrics> logs) {
        var total = logs != null ? logs.total() : 0L;
        var data = ConnectionLogAdapter.INSTANCE.toEntitiesList(logs != null ? logs.data() : new ArrayList<>());

        return new SearchLogsResponse<>(total, data);
    }

    private ConnectionLogDetail mapToConnectionLogDetail(
        io.gravitee.repository.log.v4.model.connection.ConnectionLogDetail connectionLogDetail
    ) {
        return ConnectionLogAdapter.INSTANCE.toEntity(connectionLogDetail);
    }

    private static MetricsQuery.Filter.FilterBuilder mapToConnectionLogQueryFilterBuilder(SearchLogsFilters searchLogsFilters) {
        return MetricsQuery.Filter.builder()
            .from(searchLogsFilters.from())
            .to(searchLogsFilters.to())
            .applicationIds(searchLogsFilters.applicationIds())
            .apiIds(searchLogsFilters.apiIds())
            .apiProductIds(searchLogsFilters.apiProductIds())
            .planIds(searchLogsFilters.planIds())
            .methods(searchLogsFilters.methods())
            .mcpMethods(searchLogsFilters.mcpMethods())
            .statuses(searchLogsFilters.statuses())
            .statusRanges(ConnectionLogAdapter.INSTANCE.convertStatusRanges(searchLogsFilters.statusRanges()))
            .statusCodeGroups(searchLogsFilters.statusCodeGroups())
            .entrypointIds(searchLogsFilters.entrypointIds())
            .requestIds(searchLogsFilters.requestIds())
            .transactionIds(searchLogsFilters.transactionIds())
            .uri(searchLogsFilters.uri())
            .responseTimeRanges(ConnectionLogAdapter.INSTANCE.convert(searchLogsFilters.responseTimeRanges()))
            .errorKeys(searchLogsFilters.errorKeys())
            .llmProxyModels(searchLogsFilters.llmProxyModels())
            .llmProxyProviders(searchLogsFilters.llmProxyProviders())
            .mcpProxyTools(searchLogsFilters.mcpProxyTools())
            .mcpProxyResources(searchLogsFilters.mcpProxyResources())
            .mcpProxyPrompts(searchLogsFilters.mcpProxyPrompts());
    }

    /**
     * Builds a filter for the v4-log index (body text search phase). Only includes fields that
     * actually exist in v4-log documents: time range, apiIds, requestIds, and bodyText.
     * Metric-specific fields (statuses, methods, uri, etc.) must NOT be included here — they
     * only exist in v4-metrics and are applied in the second phase of a two-phase search.
     */
    private static ConnectionLogDetailQuery.Filter.FilterBuilder mapToConnectionLogDetailQueryFilterBuilder(
        SearchLogsFilters searchLogsFilters
    ) {
        return ConnectionLogDetailQuery.Filter.builder()
            .from(searchLogsFilters.from())
            .to(searchLogsFilters.to())
            .apiIds(searchLogsFilters.apiIds())
            .requestIds(searchLogsFilters.requestIds())
            .bodyText(searchLogsFilters.bodyText());
    }

    private @NotNull LogResponse<Metrics> getConnectionLogsResponse(
        ExecutionContext executionContext,
        MetricsQuery.Filter connectionLogQueryFilter,
        Pageable pageable,
        List<DefinitionVersion> definitionVersions
    ) throws AnalyticsException {
        return metricsRepository.searchMetrics(
            new QueryContext(executionContext.getOrganizationId(), executionContext.getEnvironmentId()),
            MetricsQuery.builder().filter(connectionLogQueryFilter).page(pageable.getPageNumber()).size(pageable.getPageSize()).build(),
            definitionVersions
        );
    }

    private LogResponse<io.gravitee.repository.log.v4.model.connection.ConnectionLogDetail> searchConnectionLogDetails(
        ExecutionContext executionContext,
        ConnectionLogDetailQuery connectionLogDetailQuery
    ) throws AnalyticsException {
        return logRepository.searchConnectionLogDetails(
            new QueryContext(executionContext.getOrganizationId(), executionContext.getEnvironmentId()),
            connectionLogDetailQuery
        );
    }

    @Override
    public List<String> searchApiConnectionLogErrorKeys(ExecutionContext executionContext, String apiId, Long from, Long to) {
        return searchConnectionLogErrorKeys(executionContext, apiId, from, to);
    }

    @Override
    public List<String> searchEnvironmentConnectionLogErrorKeys(ExecutionContext executionContext, Long from, Long to) {
        return searchConnectionLogErrorKeys(executionContext, null, from, to);
    }

    private List<String> searchConnectionLogErrorKeys(ExecutionContext executionContext, String apiId, Long from, Long to) {
        try {
            return metricsRepository.searchConnectionLogErrorKeys(
                new QueryContext(executionContext.getOrganizationId(), executionContext.getEnvironmentId()),
                apiId,
                from,
                to
            );
        } catch (AnalyticsException e) {
            var context = apiId != null ? "api " + apiId : "environment " + executionContext.getEnvironmentId();
            log.error("An error occurs while trying to search error keys for {}", context, e);
            throw new TechnicalManagementException("Error while searching error keys for " + context, e);
        }
    }
}
