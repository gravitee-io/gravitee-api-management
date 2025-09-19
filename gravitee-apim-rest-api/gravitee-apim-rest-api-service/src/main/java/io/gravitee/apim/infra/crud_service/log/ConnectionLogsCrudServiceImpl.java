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
import io.gravitee.repository.log.v4.model.LogResponse;
import io.gravitee.repository.log.v4.model.connection.ConnectionLog;
import io.gravitee.repository.log.v4.model.connection.ConnectionLogDetailQuery;
import io.gravitee.repository.log.v4.model.connection.ConnectionLogQuery;
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
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Slf4j
class ConnectionLogsCrudServiceImpl implements ConnectionLogsCrudService {

    private final LogRepository logRepository;

    public ConnectionLogsCrudServiceImpl(@Lazy LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @Override
    public SearchLogsResponse<BaseConnectionLog> searchApiConnectionLogs(
        ExecutionContext executionContext,
        String apiId,
        SearchLogsFilters logsFilters,
        Pageable pageable,
        List<DefinitionVersion> definitionVersions
    ) {
        try {
            var response = getConnectionLogsResponse(
                executionContext,
                mapToConnectionLogQueryFilterBuilder(logsFilters).apiIds(Set.of(apiId)).build(),
                pageable,
                definitionVersions
            );
            return mapToConnectionResponse(response);
        } catch (AnalyticsException e) {
            log.error("An error occurs while trying to search connection logs of api [apiId={}]", apiId, e);
            throw new TechnicalManagementException("Error while searching connection logs of api " + apiId, e);
        }
    }

    @Override
    public SearchLogsResponse<BaseConnectionLog> searchApplicationConnectionLogs(
        ExecutionContext executionContext,
        String applicationId,
        SearchLogsFilters logsFilters,
        Pageable pageable
    ) {
        var executeConnectionLogDetailsSearch = logsFilters.bodyText() != null && !logsFilters.bodyText().isBlank();
        var connectionLogDetailsResponseTotal = 0L;

        var connectionLogsFilterBuilder = mapToConnectionLogQueryFilterBuilder(logsFilters).applicationIds(Set.of(applicationId));

        try {
            if (executeConnectionLogDetailsSearch) {
                var connectionLogDetailsResponse = searchConnectionLogDetails(
                    executionContext,
                    ConnectionLogDetailQuery.builder()
                        .projectionFields(List.of("_id", "request-id"))
                        .filter(mapToConnectionLogDetailQueryFilterBuilder(logsFilters).build())
                        .page(pageable.getPageNumber())
                        .size(pageable.getPageSize())
                        .build()
                );

                if (connectionLogDetailsResponse.total() == 0L) {
                    return new SearchLogsResponse<>(0, new ArrayList<>());
                }

                connectionLogDetailsResponseTotal = connectionLogDetailsResponse.total();

                var requestIds = connectionLogDetailsResponse
                    .data()
                    .stream()
                    .map(io.gravitee.repository.log.v4.model.connection.ConnectionLogDetail::getRequestId)
                    .collect(Collectors.toSet());
                connectionLogsFilterBuilder.requestIds(requestIds);
            }

            var connectionLogsResponsePageable = executeConnectionLogDetailsSearch ? new PageableImpl(1, pageable.getPageSize()) : pageable;
            var connectionLogsResponse = getConnectionLogsResponse(
                executionContext,
                connectionLogsFilterBuilder.build(),
                connectionLogsResponsePageable,
                List.of(DefinitionVersion.V2, DefinitionVersion.V4)
            );

            // If a previous search has been done and a full page is being returned in the second search, use the first search total
            if (executeConnectionLogDetailsSearch && connectionLogsResponse.total() == pageable.getPageSize()) {
                return mapToConnectionResponse(new LogResponse<>(connectionLogDetailsResponseTotal, connectionLogsResponse.data()));
            }

            return mapToConnectionResponse(connectionLogsResponse);
        } catch (AnalyticsException e) {
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

    private SearchLogsResponse<BaseConnectionLog> mapToConnectionResponse(LogResponse<ConnectionLog> logs) {
        var total = logs != null ? logs.total() : 0L;
        var data = ConnectionLogAdapter.INSTANCE.toEntitiesList(logs != null ? logs.data() : new ArrayList<>());

        return new SearchLogsResponse<>(total, data);
    }

    private ConnectionLogDetail mapToConnectionLogDetail(
        io.gravitee.repository.log.v4.model.connection.ConnectionLogDetail connectionLogDetail
    ) {
        return ConnectionLogAdapter.INSTANCE.toEntity(connectionLogDetail);
    }

    private static ConnectionLogQuery.Filter.FilterBuilder mapToConnectionLogQueryFilterBuilder(SearchLogsFilters searchLogsFilters) {
        return ConnectionLogQuery.Filter.builder()
            .from(searchLogsFilters.from())
            .to(searchLogsFilters.to())
            .applicationIds(searchLogsFilters.applicationIds())
            .apiIds(searchLogsFilters.apiIds())
            .planIds(searchLogsFilters.planIds())
            .methods(searchLogsFilters.methods())
            .statuses(searchLogsFilters.statuses())
            .entrypointIds(searchLogsFilters.entrypointIds())
            .requestIds(searchLogsFilters.requestIds())
            .transactionIds(searchLogsFilters.transactionIds())
            .uri(searchLogsFilters.uri())
            .responseTimeRanges(ConnectionLogAdapter.INSTANCE.convert(searchLogsFilters.responseTimeRanges()));
    }

    private static ConnectionLogDetailQuery.Filter.FilterBuilder mapToConnectionLogDetailQueryFilterBuilder(
        SearchLogsFilters searchLogsFilters
    ) {
        return ConnectionLogDetailQuery.Filter.builder()
            .from(searchLogsFilters.from())
            .to(searchLogsFilters.to())
            .apiIds(searchLogsFilters.apiIds())
            .methods(searchLogsFilters.methods())
            .statuses(searchLogsFilters.statuses())
            .requestIds(searchLogsFilters.requestIds())
            .uri(searchLogsFilters.uri())
            .bodyText(searchLogsFilters.bodyText());
    }

    private @NotNull LogResponse<ConnectionLog> getConnectionLogsResponse(
        ExecutionContext executionContext,
        ConnectionLogQuery.Filter connectionLogQueryFilter,
        Pageable pageable,
        List<DefinitionVersion> definitionVersions
    ) throws AnalyticsException {
        return logRepository.searchConnectionLogs(
            new QueryContext(executionContext.getOrganizationId(), executionContext.getEnvironmentId()),
            ConnectionLogQuery.builder()
                .filter(connectionLogQueryFilter)
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .build(),
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
}
