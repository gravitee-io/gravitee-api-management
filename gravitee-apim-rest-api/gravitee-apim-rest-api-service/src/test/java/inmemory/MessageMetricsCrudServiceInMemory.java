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

import io.gravitee.apim.core.metrics.crud_service.MessageMetricsCrudService;
import io.gravitee.apim.core.metrics.model.MessageMetrics;
import io.gravitee.rest.api.model.analytics.SearchMessageMetricsFilters;
import io.gravitee.rest.api.model.common.Pageable;
import io.gravitee.rest.api.model.v4.log.SearchLogsResponse;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MessageMetricsCrudServiceInMemory implements MessageMetricsCrudService, InMemoryAlternative<MessageMetrics> {

    List<MessageMetrics> storage = new ArrayList<>();

    @Override
    public SearchLogsResponse<MessageMetrics> searchApiMessageMetrics(
        ExecutionContext executionContext,
        String apiId,
        SearchMessageMetricsFilters filters,
        Pageable pageable
    ) {
        var predicate = messageMetricsPredicate(apiId, filters);

        var pageNumber = pageable.getPageNumber();
        var pageSize = pageable.getPageSize();

        var matches = storage().stream().filter(predicate).sorted(Comparator.comparing(MessageMetrics::getTimestamp).reversed()).toList();

        var page = matches.size() <= pageSize ? matches : matches.subList((pageNumber - 1) * pageSize, pageNumber * pageSize);

        return new SearchLogsResponse<>(matches.size(), page);
    }

    @Override
    public void initWith(List<MessageMetrics> items) {
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<MessageMetrics> storage() {
        return List.copyOf(storage);
    }

    private static Predicate<MessageMetrics> messageMetricsPredicate(String apiId, SearchMessageMetricsFilters filters) {
        Predicate<MessageMetrics> predicate = _ignored -> true;

        if (apiId != null) {
            predicate = predicate.and(messageMetrics -> Objects.equals(apiId, messageMetrics.getApiId()));
        }

        if (filters.requestId() != null) {
            predicate = predicate.and(messageMetrics -> Objects.equals(filters.requestId(), messageMetrics.getRequestId()));
        }

        if (filters.connectorId() != null) {
            predicate = predicate.and(messageMetrics -> Objects.equals(filters.connectorId(), messageMetrics.getConnectorId()));
        }

        if (filters.connectorType() != null) {
            predicate = predicate.and(messageMetrics -> Objects.equals(filters.connectorType(), messageMetrics.getConnectorType()));
        }

        if (filters.operation() != null) {
            predicate = predicate.and(messageMetrics -> Objects.equals(filters.operation(), messageMetrics.getOperation()));
        }

        long from = filters.from();
        long to = filters.to();
        if (from > 0 && from < to) {
            predicate = predicate.and(messageMetrics -> {
                long timestamp = OffsetDateTime.parse(messageMetrics.getTimestamp()).toInstant().toEpochMilli();
                return timestamp >= from && timestamp < to;
            });
        }

        return predicate;
    }
}
