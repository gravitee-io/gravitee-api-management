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
import io.gravitee.rest.api.model.v4.log.SearchLogsResponse;
import io.gravitee.rest.api.model.v4.log.connection.BaseConnectionLog;
import io.gravitee.rest.api.model.v4.log.connection.ConnectionLogDetail;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
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
            var response = logRepository.searchConnectionLogs(
                new QueryContext(executionContext.getOrganizationId(), executionContext.getEnvironmentId()),
                ConnectionLogQuery
                    .builder()
                    .filter(
                        ConnectionLogQuery.Filter
                            .builder()
                            .apiIds(Set.of(apiId))
                            .from(logsFilters.from())
                            .to(logsFilters.to())
                            .applicationIds(logsFilters.applicationIds())
                            .planIds(logsFilters.planIds())
                            .methods(logsFilters.methods())
                            .statuses(logsFilters.statuses())
                            .entrypointIds(logsFilters.entrypointIds())
                            .build()
                    )
                    .page(pageable.getPageNumber())
                    .size(pageable.getPageSize())
                    .build(),
                definitionVersions
            );
            return mapToConnectionResponse(response);
        } catch (AnalyticsException e) {
            log.error("An error occurs while trying to search connection logs of api [apiId={}]", apiId, e);
            throw new TechnicalManagementException("Error while searching connection logs of api " + apiId, e);
        }
    }

    @Override
    public Optional<ConnectionLogDetail> searchApiConnectionLog(ExecutionContext executionContext, String apiId, String requestId) {
        try {
            var response = logRepository.searchConnectionLogDetail(
                new QueryContext(executionContext.getOrganizationId(), executionContext.getEnvironmentId()),
                ConnectionLogDetailQuery
                    .builder()
                    .filter(ConnectionLogDetailQuery.Filter.builder().apiId(apiId).requestId(requestId).build())
                    .build()
            );
            return response.map(this::mapToConnectionLogDetail);
        } catch (AnalyticsException e) {
            log.error("An error occurs while trying to search connection log of api [apiId={}, requestId={}]", apiId, requestId, e);
            throw new TechnicalManagementException("Error while searching connection log of api " + apiId + " requestId " + requestId, e);
        }
    }

    private SearchLogsResponse<BaseConnectionLog> mapToConnectionResponse(LogResponse<ConnectionLog> logs) {
        var total = logs.total();
        var data = ConnectionLogAdapter.INSTANCE.toEntitiesList(logs.data());

        return new SearchLogsResponse<>(total, data);
    }

    private ConnectionLogDetail mapToConnectionLogDetail(
        io.gravitee.repository.log.v4.model.connection.ConnectionLogDetail connectionLogDetail
    ) {
        return ConnectionLogAdapter.INSTANCE.toEntity(connectionLogDetail);
    }
}
