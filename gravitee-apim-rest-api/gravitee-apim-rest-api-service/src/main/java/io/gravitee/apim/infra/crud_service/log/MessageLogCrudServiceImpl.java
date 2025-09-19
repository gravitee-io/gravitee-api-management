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

import io.gravitee.apim.core.log.crud_service.MessageLogCrudService;
import io.gravitee.apim.core.log.model.AggregatedMessageLog;
import io.gravitee.apim.infra.adapter.MessageLogAdapter;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.log.v4.api.LogRepository;
import io.gravitee.repository.log.v4.model.LogResponse;
import io.gravitee.repository.log.v4.model.message.MessageLogQuery;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.v4.log.SearchLogsResponse;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
@Slf4j
class MessageLogCrudServiceImpl implements MessageLogCrudService {

    private final LogRepository logRepository;

    public MessageLogCrudServiceImpl(@Lazy LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @Override
    public SearchLogsResponse<AggregatedMessageLog> searchApiMessageLog(
        ExecutionContext executionContext,
        String apiId,
        String requestId,
        Pageable pageable
    ) {
        try {
            var response = logRepository.searchAggregatedMessageLog(
                executionContext.getQueryContext(),
                MessageLogQuery.builder()
                    .filter(MessageLogQuery.Filter.builder().apiId(apiId).requestId(requestId).build())
                    .page(pageable.getPageNumber())
                    .size(pageable.getPageSize())
                    .build()
            );
            return mapToMessageResponse(response);
        } catch (AnalyticsException e) {
            log.error("An error occurs while trying to search message of api [apiId={}, requestId={}]", apiId, requestId, e);
            throw new TechnicalManagementException("Error while searching message logs of api " + apiId + " request " + requestId, e);
        }
    }

    private SearchLogsResponse<AggregatedMessageLog> mapToMessageResponse(
        LogResponse<io.gravitee.repository.log.v4.model.message.AggregatedMessageLog> logs
    ) {
        var total = logs.total();
        var data = MessageLogAdapter.INSTANCE.toEntities(logs.data());

        return new SearchLogsResponse<>(total, data);
    }
}
