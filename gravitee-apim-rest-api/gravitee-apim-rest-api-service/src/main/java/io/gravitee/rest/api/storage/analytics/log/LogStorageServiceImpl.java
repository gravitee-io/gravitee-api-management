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
package io.gravitee.rest.api.storage.analytics.log;

import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.log.v4.api.LogRepository;
import io.gravitee.repository.log.v4.model.ConnectionLog;
import io.gravitee.repository.log.v4.model.ConnectionLogQuery;
import io.gravitee.repository.log.v4.model.LogResponse;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.v4.log.BaseConnectionLog;
import io.gravitee.rest.api.model.v4.log.SearchLogResponse;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Slf4j
class LogStorageServiceImpl implements LogStorageService {

    private final LogRepository logRepository;

    public LogStorageServiceImpl(@Lazy LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    public SearchLogResponse<BaseConnectionLog> searchApiConnectionLog(String apiId, Pageable pageable) {
        try {
            var response = logRepository.searchConnectionLog(
                ConnectionLogQuery
                    .builder()
                    .filter(ConnectionLogQuery.Filter.builder().appId(apiId).build())
                    .page(pageable.getPageNumber())
                    .size(pageable.getPageSize())
                    .build()
            );
            return mapToResponse(response);
        } catch (AnalyticsException e) {
            log.error("An error occurs while trying to search connection of api [apiId={}]", apiId, e);
            throw new TechnicalManagementException("Error while searching connection logs of api " + apiId, e);
        }
    }

    private SearchLogResponse<BaseConnectionLog> mapToResponse(LogResponse<ConnectionLog> logs) {
        var total = logs.total();
        var data = logs.data().stream().map(this::mapToModel).toList();

        return new SearchLogResponse<>(total, data);
    }

    private BaseConnectionLog mapToModel(ConnectionLog connectionLog) {
        return BaseConnectionLog
            .builder()
            .apiId(connectionLog.getApiId())
            .requestId(connectionLog.getRequestId())
            .timestamp(connectionLog.getTimestamp())
            .applicationId(connectionLog.getApplicationId())
            .clientIdentifier(connectionLog.getClientIdentifier())
            .method(connectionLog.getMethod())
            .planId(connectionLog.getPlanId())
            .requestEnded(connectionLog.isRequestEnded())
            .transactionId(connectionLog.getTransactionId())
            .status(connectionLog.getStatus())
            .build();
    }
}
