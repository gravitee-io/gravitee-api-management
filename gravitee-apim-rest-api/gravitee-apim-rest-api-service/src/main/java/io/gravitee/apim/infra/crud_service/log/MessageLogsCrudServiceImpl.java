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

import io.gravitee.apim.core.log.crud_service.MessageLogsCrudService;
import io.gravitee.apim.core.log.model.MessageLog;
import io.gravitee.apim.infra.adapter.MessageLogAdapter;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.log.v4.api.MetricsRepository;
import io.gravitee.repository.log.v4.model.LogResponse;
import io.gravitee.repository.log.v4.model.message.MessageMetricsQuery;
import io.gravitee.rest.api.model.analytics.SearchMessageLogsFilters;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.v4.log.SearchLogsResponse;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@Slf4j
public class MessageLogsCrudServiceImpl implements MessageLogsCrudService {

    private final MetricsRepository metricsRepository;

    public MessageLogsCrudServiceImpl(@Lazy MetricsRepository metricsRepository) {
        this.metricsRepository = metricsRepository;
    }

    @Override
    public SearchLogsResponse<MessageLog> searchApiMessageLogs(
        ExecutionContext executionContext,
        String apiId,
        SearchMessageLogsFilters filters,
        Pageable pageable
    ) {
        try {
            var response = metricsRepository.searchMessageMetrics(
                executionContext.getQueryContext(),
                MessageMetricsQuery.builder()
                    .filter(
                        MessageMetricsQuery.Filter.builder()
                            .apiId(apiId)
                            .requestId(filters.requestId())
                            .connectorId(filters.connectorId())
                            .connectorType(filters.connectorType())
                            .operation(filters.operation())
                            .from(filters.from())
                            .to(filters.to())
                            .build()
                    )
                    .page(pageable.getPageNumber())
                    .size(pageable.getPageSize())
                    .build()
            );
            return mapToMessageResponse(response);
        } catch (AnalyticsException e) {
            throw new TechnicalManagementException("An error occurs while searching message metrics for api [" + apiId + "]", e);
        }
    }

    private SearchLogsResponse<MessageLog> mapToMessageResponse(
        LogResponse<io.gravitee.repository.log.v4.model.message.MessageMetrics> logs
    ) {
        var total = logs.total();
        var data = MessageLogAdapter.INSTANCE.mapToMessageMetrics(logs.data());

        return new SearchLogsResponse<>(total, data);
    }
}
