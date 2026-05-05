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

import io.gravitee.apim.core.log.crud_service.NativeApiLogCrudService;
import io.gravitee.apim.core.log.model.NativeApiLog;
import io.gravitee.apim.core.log.model.NativeConnectionStatus;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.log.v4.api.MetricsRepository;
import io.gravitee.repository.log.v4.model.connection.NativeApiMetricKeys;
import io.gravitee.repository.log.v4.model.connection.NativeApiMetrics;
import io.gravitee.repository.log.v4.model.connection.NativeApiMetricsQuery;
import io.gravitee.rest.api.model.v4.log.SearchLogsResponse;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.CustomLog;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@CustomLog
@Service
class NativeApiLogCrudServiceImpl implements NativeApiLogCrudService {

    private final MetricsRepository metricsRepository;

    public NativeApiLogCrudServiceImpl(@Lazy MetricsRepository metricsRepository) {
        this.metricsRepository = metricsRepository;
    }

    @Override
    public Optional<NativeApiLog> findLog(ExecutionContext executionContext, String apiId, String requestId, Long from, Long to) {
        try {
            return metricsRepository
                .findNativeApiMetrics(executionContext.getQueryContext(), apiId, requestId, from, to)
                .map(NativeApiLogCrudServiceImpl::mapFull);
        } catch (AnalyticsException e) {
            log.error("An error occurs while trying to find native connection log [apiId={},requestId={}]", apiId, requestId, e);
            throw new TechnicalManagementException("Error while finding native connection log " + apiId + "/" + requestId, e);
        }
    }

    @Override
    public SearchLogsResponse<NativeApiLog> searchLogs(
        ExecutionContext executionContext,
        String apiId,
        NativeApiLogCrudService.Filter filter,
        int page,
        int size
    ) {
        try {
            var query = NativeApiMetricsQuery.builder()
                .apiId(apiId)
                .from(filter.from())
                .to(filter.to())
                .applicationIds(filter.applicationIds())
                .planIds(filter.planIds())
                .connectionStatuses(toStrings(filter.connectionStatuses()))
                .page(page)
                .size(size)
                .build();
            var response = metricsRepository.searchNativeApiMetrics(executionContext.getQueryContext(), query);
            return new SearchLogsResponse<>(response.total(), response.data().stream().map(NativeApiLogCrudServiceImpl::map).toList());
        } catch (AnalyticsException e) {
            log.error("An error occurs while trying to search native connection logs [apiId={}]", apiId, e);
            throw new TechnicalManagementException("Error while searching native connection logs " + apiId, e);
        }
    }

    private static NativeApiLog mapFull(NativeApiMetrics metrics) {
        var additional = metrics.getAdditionalMetrics();
        return NativeApiLog.builder()
            .apiId(metrics.getApiId())
            .requestId(metrics.getRequestId())
            .transactionId(metrics.getTransactionId())
            .timestamp(metrics.getTimestamp())
            .applicationId(metrics.getApplicationId())
            .planId(metrics.getPlanId())
            .clientIdentifier(metrics.getClientIdentifier())
            .subscriptionId(metrics.getSubscriptionId())
            .entrypointId(metrics.getEntrypointId())
            .gateway(metrics.getGateway())
            .remoteAddress(metrics.getRemoteAddress())
            .localAddress(metrics.getLocalAddress())
            .host(metrics.getHost())
            .errorKey(metrics.getErrorKey())
            .message(metrics.getMessage())
            .connectionStatus(parseConnectionStatus(additional))
            .clientId(asString(additional, NativeApiMetricKeys.CLIENT_ID))
            .brokerId(asString(additional, NativeApiMetricKeys.BROKER_ID))
            .connectionDurationMs(asLong(additional, NativeApiMetricKeys.CONNECTION_DURATION_MS))
            .build();
    }

    private static NativeApiLog map(NativeApiMetrics metrics) {
        var additional = metrics.getAdditionalMetrics();
        return NativeApiLog.builder()
            .apiId(metrics.getApiId())
            .requestId(metrics.getRequestId())
            .transactionId(metrics.getTransactionId())
            .timestamp(metrics.getTimestamp())
            .applicationId(metrics.getApplicationId())
            .planId(metrics.getPlanId())
            .clientIdentifier(metrics.getClientIdentifier())
            .entrypointId(metrics.getEntrypointId())
            .connectionStatus(parseConnectionStatus(additional))
            .connectionDurationMs(asLong(additional, NativeApiMetricKeys.CONNECTION_DURATION_MS))
            .build();
    }

    private static Set<String> toStrings(Set<NativeConnectionStatus> statuses) {
        return statuses == null ? null : statuses.stream().map(Enum::name).collect(Collectors.toSet());
    }

    private static NativeConnectionStatus parseConnectionStatus(Map<String, Object> source) {
        var raw = asString(source, NativeApiMetricKeys.CONNECTION_STATUS);
        var parsed = NativeConnectionStatus.fromString(raw);
        if (parsed.isEmpty() && raw != null) {
            log.warn("Unknown native connection status '{}'; treating as null", raw);
        }
        return parsed.orElse(null);
    }

    private static String asString(Map<String, Object> source, String key) {
        if (source == null) {
            return null;
        }
        var value = source.get(key);
        return value == null ? null : value.toString();
    }

    private static Long asLong(Map<String, Object> source, String key) {
        if (source == null) {
            return null;
        }
        var value = source.get(key);
        if (value instanceof Number n) {
            return n.longValue();
        }
        return null;
    }
}
