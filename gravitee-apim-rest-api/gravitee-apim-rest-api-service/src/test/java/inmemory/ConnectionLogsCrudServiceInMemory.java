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
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.analytics.SearchLogsFilters;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.v4.log.SearchLogsResponse;
import io.gravitee.rest.api.model.v4.log.connection.BaseConnectionLog;
import io.gravitee.rest.api.model.v4.log.connection.ConnectionLogDetail;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;
import org.springframework.util.CollectionUtils;

public class ConnectionLogsCrudServiceInMemory implements ConnectionLogsCrudService, InMemoryAlternative<Object> {

    private final InMemoryConnectionLogs connectionLogs = new InMemoryConnectionLogs();
    private final InMemoryConnectionLogDetails connectionLogDetails = new InMemoryConnectionLogDetails();

    @Override
    public SearchLogsResponse<BaseConnectionLog> searchApiConnectionLogs(
        ExecutionContext executionContext,
        String apiId,
        SearchLogsFilters logsFilters,
        Pageable pageable,
        List<DefinitionVersion> definitionVersions
    ) {
        var predicate = getBaseConnectionLogPredicate(logsFilters.toBuilder().apiIds(Set.of(apiId)).build());

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
        var predicate = getBaseConnectionLogPredicate(logsFilters.toBuilder().applicationIds(Set.of(applicationId)).build());

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

        if (logsFilters.apiIds() != null && !logsFilters.apiIds().isEmpty()) {
            predicate = predicate.and(connectionLog -> logsFilters.apiIds().contains(connectionLog.getApiId()));
        }

        if (null != logsFilters.from()) {
            predicate = predicate.and(connectionLog -> Instant.parse(connectionLog.getTimestamp()).toEpochMilli() >= logsFilters.from());
        }

        if (null != logsFilters.to()) {
            predicate = predicate.and(connectionLog -> Instant.parse(connectionLog.getTimestamp()).toEpochMilli() <= logsFilters.to());
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
