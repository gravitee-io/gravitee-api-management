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
package inmemory;

import io.gravitee.apim.core.log.crud_service.ConnectionLogsCrudService;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.analytics.SearchLogsFilters;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.v4.log.SearchLogsResponse;
import io.gravitee.rest.api.model.v4.log.connection.BaseConnectionLog;
import io.gravitee.rest.api.model.v4.log.connection.ConnectionLogDetail;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.jspecify.annotations.NonNull;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

public class ConnectionLogsCrudServiceInMemory implements ConnectionLogsCrudService, InMemoryAlternative<Object> {

    private final InMemoryConnectionLogs connectionLogs = new InMemoryConnectionLogs();
    private final InMemoryConnectionLogDetails connectionLogDetails = new InMemoryConnectionLogDetails();

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
        var predicate = getBaseConnectionLogPredicate(logsFilters.toBuilder().apiIds(apiIds).build());

        var pageNumber = pageable.getPageNumber();
        var pageSize = pageable.getPageSize();

        var matches = connectionLogs
            .storage()
            .stream()
            .filter(predicate)
            .sorted(Comparator.comparing(BaseConnectionLog::getTimestamp).reversed())
            .toList();

        var page = matches.size() <= pageSize ? matches : matches.subList((pageNumber - 1) * pageSize, pageNumber * pageSize);

        return new SearchLogsResponse<>(matches.size(), page);
    }

    @Override
    public SearchLogsResponse<BaseConnectionLog> searchApplicationConnectionLogs(
        ExecutionContext executionContext,
        String applicationId,
        SearchLogsFilters logsFilters,
        Pageable pageable
    ) {
        var pageNumber = pageable.getPageNumber();
        var pageSize = pageable.getPageSize();

        var connectionLogFilterBuilder = logsFilters.toBuilder().applicationIds(Set.of(applicationId));

        var connectionLogDetailTotal = 0;
        var executeConnectionLogDetail = logsFilters.bodyText() != null && !logsFilters.bodyText().isBlank();

        if (executeConnectionLogDetail) {
            var connectionLogDetailPredicate = getConnectionLogDetailsPredicate(logsFilters);
            var connectionLogDetailResults = connectionLogDetails
                .storage()
                .stream()
                .filter(connectionLogDetailPredicate)
                .sorted(Comparator.comparing(ConnectionLogDetail::getTimestamp).reversed())
                .toList();

            if (connectionLogDetailResults.isEmpty()) {
                return new SearchLogsResponse<>(0, new ArrayList<>());
            }

            connectionLogDetailTotal = connectionLogDetailResults.size();

            var connectionLogDetailResultPage = connectionLogDetailResults.size() <= pageSize
                ? connectionLogDetailResults
                : connectionLogDetailResults.subList((pageNumber - 1) * pageSize, pageNumber * pageSize);

            connectionLogFilterBuilder.requestIds(
                connectionLogDetailResultPage.stream().map(ConnectionLogDetail::getRequestId).collect(Collectors.toSet())
            );
        }

        var connectionLogPredicate = getBaseConnectionLogPredicate(connectionLogFilterBuilder.build());

        var matches = connectionLogs
            .storage()
            .stream()
            .filter(connectionLogPredicate)
            .sorted(Comparator.comparing(BaseConnectionLog::getTimestamp).reversed())
            .toList();

        if (executeConnectionLogDetail) {
            var total = matches.size() == pageSize ? connectionLogDetailTotal : matches.size();
            return new SearchLogsResponse<>(total, matches);
        }

        var page = matches.size() <= pageSize ? matches : matches.subList((pageNumber - 1) * pageSize, pageNumber * pageSize);

        return new SearchLogsResponse<>(matches.size(), page);
    }

    @Override
    public Optional<ConnectionLogDetail> searchApiConnectionLog(ExecutionContext executionContext, String apiId, String requestId) {
        Predicate<ConnectionLogDetail> predicate = connectionLog -> true;
        if (null != apiId && !apiId.isEmpty()) {
            predicate = predicate.and(connectionLog -> connectionLog.getApiId().equals(apiId));
        }

        if (null != requestId && !requestId.isEmpty()) {
            predicate = predicate.and(connectionLog -> connectionLog.getRequestId().equals(requestId));
        }

        return connectionLogDetails.storage().stream().filter(predicate).findFirst();
    }

    @Override
    public void initWith(List<Object> items) {
        connectionLogs.initWith(items.stream().filter(BaseConnectionLog.class::isInstance).map(BaseConnectionLog.class::cast).toList());
        connectionLogDetails.initWith(
            items.stream().filter(ConnectionLogDetail.class::isInstance).map(ConnectionLogDetail.class::cast).toList()
        );
    }

    public void initWithConnectionLogs(List<BaseConnectionLog> items) {
        connectionLogs.initWith(items);
    }

    public void initWithConnectionLogDetails(List<ConnectionLogDetail> items) {
        connectionLogDetails.initWith(items);
    }

    @Override
    public void reset() {
        connectionLogs.reset();
        connectionLogDetails.reset();
    }

    @Override
    public List<Object> storage() {
        final ArrayList<Object> merge = new ArrayList<>();
        merge.addAll(connectionLogs.storage());
        merge.addAll(connectionLogDetails.storage());
        return merge;
    }

    private static Predicate<BaseConnectionLog> getBaseConnectionLogPredicate(SearchLogsFilters logsFilters) {
        Predicate<BaseConnectionLog> predicate = _ignored -> true;

        if (!CollectionUtils.isEmpty(logsFilters.apiIds())) {
            predicate = predicate.and(connectionLog -> logsFilters.apiIds().contains(connectionLog.getApiId()));
        }

        if (!CollectionUtils.isEmpty(logsFilters.requestIds())) {
            predicate = predicate.and(connectionLog -> logsFilters.requestIds().contains(connectionLog.getRequestId()));
        }

        if (null != logsFilters.from()) {
            predicate = predicate.and(connectionLog -> Instant.parse(connectionLog.getTimestamp()).toEpochMilli() >= logsFilters.from());
        }

        if (null != logsFilters.to()) {
            predicate = predicate.and(connectionLog -> Instant.parse(connectionLog.getTimestamp()).toEpochMilli() <= logsFilters.to());
        }

        if (!CollectionUtils.isEmpty(logsFilters.transactionIds())) {
            predicate = predicate.and(connectionLog -> logsFilters.transactionIds().contains(connectionLog.getTransactionId()));
        }

        if (!CollectionUtils.isEmpty(logsFilters.applicationIds())) {
            predicate = predicate.and(connectionLog -> logsFilters.applicationIds().contains(connectionLog.getApplicationId()));
        }

        if (!CollectionUtils.isEmpty(logsFilters.planIds())) {
            predicate = predicate.and(connectionLog -> logsFilters.planIds().contains(connectionLog.getPlanId()));
        }

        if (!CollectionUtils.isEmpty(logsFilters.methods())) {
            predicate = predicate.and(connectionLog -> logsFilters.methods().contains(connectionLog.getMethod()));
        }

        if (!CollectionUtils.isEmpty(logsFilters.statuses())) {
            predicate = predicate.and(connectionLog -> logsFilters.statuses().contains(connectionLog.getStatus()));
        }

        if (!CollectionUtils.isEmpty(logsFilters.entrypointIds())) {
            predicate = predicate.and(connectionLog -> logsFilters.entrypointIds().contains(connectionLog.getEntrypointId()));
        }

        if (!CollectionUtils.isEmpty(logsFilters.responseTimeRanges())) {
            predicate = predicate.and(connectionLog ->
                logsFilters
                    .responseTimeRanges()
                    .stream()
                    .anyMatch(
                        range ->
                            (range.to() == null || range.to() >= connectionLog.getGatewayResponseTime()) &&
                            (range.from() == null || range.from() <= connectionLog.getGatewayResponseTime())
                    )
            );
        }

        var uri = logsFilters.uri();
        if (StringUtils.hasLength(uri)) {
            var normalizedUri = getNormalizedUri(uri);
            predicate = predicate.and(connectionLog -> connectionLog.getUri().startsWith(normalizedUri));
        }

        return predicate;
    }

    private static @NonNull String getNormalizedUri(String uri) {
        var beginningSlash = uri.startsWith("/") ? "" : "/";
        return beginningSlash + (uri.endsWith("*") ? uri.substring(0, uri.length() - 1) : uri);
    }

    private static Predicate<ConnectionLogDetail> getConnectionLogDetailsPredicate(SearchLogsFilters logsFilters) {
        Predicate<ConnectionLogDetail> predicate = _ignored -> true;

        if (logsFilters.apiIds() != null && !logsFilters.apiIds().isEmpty()) {
            predicate = predicate.and(connectionLog -> logsFilters.apiIds().contains(connectionLog.getApiId()));
        }

        if (null != logsFilters.from()) {
            predicate = predicate.and(connectionLog -> Instant.parse(connectionLog.getTimestamp()).toEpochMilli() >= logsFilters.from());
        }

        if (null != logsFilters.to()) {
            predicate = predicate.and(connectionLog -> Instant.parse(connectionLog.getTimestamp()).toEpochMilli() <= logsFilters.to());
        }

        if (!CollectionUtils.isEmpty(logsFilters.methods())) {
            predicate = predicate.and(
                connectionLog ->
                    connectionLog.getEntrypointRequest() != null &&
                    logsFilters.methods().contains(HttpMethod.valueOf(connectionLog.getEntrypointRequest().getMethod()))
            );
        }

        if (!CollectionUtils.isEmpty(logsFilters.statuses())) {
            predicate = predicate.and(
                connectionLog ->
                    connectionLog.getEntrypointResponse() != null &&
                    logsFilters.statuses().contains(connectionLog.getEntrypointResponse().getStatus())
            );
        }

        if (null != logsFilters.bodyText() && !logsFilters.bodyText().isBlank()) {
            predicate = predicate.and(connectionLogDetail -> {
                if (
                    connectionLogDetail.getEntrypointRequest() != null &&
                    connectionLogDetail.getEntrypointRequest().getBody() != null &&
                    connectionLogDetail.getEntrypointRequest().getBody().contains(logsFilters.bodyText())
                ) {
                    return true;
                }
                if (
                    connectionLogDetail.getEntrypointResponse() != null &&
                    connectionLogDetail.getEntrypointResponse().getBody() != null &&
                    connectionLogDetail.getEntrypointResponse().getBody().contains(logsFilters.bodyText())
                ) {
                    return true;
                }
                if (
                    connectionLogDetail.getEndpointRequest() != null &&
                    connectionLogDetail.getEndpointRequest().getBody() != null &&
                    connectionLogDetail.getEndpointRequest().getBody().contains(logsFilters.bodyText())
                ) {
                    return true;
                }
                return (
                    connectionLogDetail.getEndpointResponse() != null &&
                    connectionLogDetail.getEndpointResponse().getBody() != null &&
                    connectionLogDetail.getEndpointResponse().getBody().contains(logsFilters.bodyText())
                );
            });
        }

        return predicate;
    }

    static class InMemoryConnectionLogs implements InMemoryAlternative<BaseConnectionLog> {

        private final List<BaseConnectionLog> storage = new ArrayList<>();

        @Override
        public void initWith(List<BaseConnectionLog> items) {
            storage.addAll(items);
        }

        @Override
        public void reset() {
            storage.clear();
        }

        @Override
        public List<BaseConnectionLog> storage() {
            return Collections.unmodifiableList(storage);
        }
    }

    static class InMemoryConnectionLogDetails implements InMemoryAlternative<ConnectionLogDetail> {

        private final List<ConnectionLogDetail> storage = new ArrayList<>();

        @Override
        public void initWith(List<ConnectionLogDetail> items) {
            storage.addAll(items);
        }

        @Override
        public void reset() {
            storage.clear();
        }

        @Override
        public List<ConnectionLogDetail> storage() {
            return Collections.unmodifiableList(storage);
        }
    }
}
