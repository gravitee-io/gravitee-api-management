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
package io.gravitee.rest.api.management.v2.rest.resource.api.analytics;

import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsCountAggregate;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsDateHistoAggregate;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsField;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsGroupByAggregate;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsGroupByOrder;
import io.gravitee.repository.log.v4.model.analytics.ApiAnalyticsStatsAggregate;
import io.gravitee.rest.api.management.v2.rest.model.ApiUnifiedAnalyticsDateHistoPayload;
import io.gravitee.rest.api.management.v2.rest.model.ApiUnifiedAnalyticsDateHistoSeries;
import io.gravitee.rest.api.management.v2.rest.model.ApiUnifiedAnalyticsGroupByOrderParam;
import io.gravitee.rest.api.management.v2.rest.model.ApiUnifiedAnalyticsGroupByPayload;
import io.gravitee.rest.api.management.v2.rest.model.ApiUnifiedAnalyticsItemMetadata;
import io.gravitee.rest.api.management.v2.rest.model.ApiUnifiedAnalyticsQueryType;
import io.gravitee.rest.api.management.v2.rest.model.ApiUnifiedAnalyticsResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiUnifiedAnalyticsStatsPayload;
import jakarta.ws.rs.BadRequestException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Validation and mapping for {@code GET .../analytics} (unified V4 metrics).
 */
final class ApiUnifiedAnalyticsSupport {

    static final int DEFAULT_GROUP_BY_SIZE = 10;
    static final long MAX_HISTOGRAM_BUCKETS = 500L;

    private ApiUnifiedAnalyticsSupport() {}

    static ApiUnifiedAnalyticsQueryType parseQueryType(String typeParam) {
        if (typeParam == null || typeParam.isBlank()) {
            throw new BadRequestException("Query parameter 'type' is required");
        }
        try {
            return ApiUnifiedAnalyticsQueryType.fromValue(typeParam.trim());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unsupported analytics type: " + typeParam);
        }
    }

    static void validateTimeRangeMillis(Long from, Long to) {
        if (from == null || to == null) {
            throw new BadRequestException("Query parameters 'from' and 'to' are required");
        }
        if (from > to) {
            throw new BadRequestException("'from' must be less than or equal to 'to'");
        }
    }

    static ApiAnalyticsField requireField(String field, String contextLabel) {
        if (field == null || field.isBlank()) {
            throw new BadRequestException("Query parameter 'field' is required for " + contextLabel);
        }
        return ApiAnalyticsField.fromPrdName(field).orElseThrow(() -> new BadRequestException("Unsupported field: " + field));
    }

    static Optional<ApiAnalyticsField> optionalField(String field) {
        if (field == null || field.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(ApiAnalyticsField.fromPrdName(field).orElseThrow(() -> new BadRequestException("Unsupported field: " + field)));
    }

    static int resolveGroupBySize(Integer size) {
        if (size == null) {
            return DEFAULT_GROUP_BY_SIZE;
        }
        if (size < 1) {
            throw new BadRequestException("Query parameter 'size' must be at least 1");
        }
        return size;
    }

    static Optional<ApiAnalyticsGroupByOrder> parseGroupByOrder(String orderParam) {
        if (orderParam == null || orderParam.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(
                switch (ApiUnifiedAnalyticsGroupByOrderParam.fromValue(orderParam.trim())) {
                    case COUNT_DESC -> ApiAnalyticsGroupByOrder.COUNT_DESC;
                    case COUNT_ASC -> ApiAnalyticsGroupByOrder.COUNT_ASC;
                }
            );
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unsupported order: " + orderParam);
        }
    }

    static void validateDateHistogram(long fromMs, long toMs, Long intervalMs) {
        if (intervalMs == null) {
            throw new BadRequestException("Query parameter 'interval' is required for DATE_HISTO");
        }
        if (intervalMs <= 0) {
            throw new BadRequestException("Query parameter 'interval' must be positive");
        }
        long rangeMs = toMs - fromMs;
        if (intervalMs > rangeMs) {
            throw new BadRequestException("Query parameter 'interval' must not exceed the time range (to - from)");
        }
        long bucketCount = (rangeMs + intervalMs - 1) / intervalMs;
        if (bucketCount > MAX_HISTOGRAM_BUCKETS) {
            throw new BadRequestException(
                "Histogram would exceed maximum bucket count (" + MAX_HISTOGRAM_BUCKETS + "); increase interval or narrow the time range"
            );
        }
    }

    static ApiUnifiedAnalyticsResponse toCountResponse(Optional<ApiAnalyticsCountAggregate> aggregate) {
        var response = new ApiUnifiedAnalyticsResponse().type(ApiUnifiedAnalyticsQueryType.COUNT);
        aggregate.ifPresent(a -> response.total(a.getCount()));
        return response;
    }

    static ApiUnifiedAnalyticsResponse toStatsResponse(Optional<ApiAnalyticsStatsAggregate> aggregate) {
        var response = new ApiUnifiedAnalyticsResponse().type(ApiUnifiedAnalyticsQueryType.STATS);
        aggregate.ifPresent(a ->
            response.stats(
                new ApiUnifiedAnalyticsStatsPayload().count(a.getCount()).min(a.getMin()).max(a.getMax()).avg(a.getAvg()).sum(a.getSum())
            )
        );
        return response;
    }

    static ApiUnifiedAnalyticsResponse toGroupByResponse(Optional<ApiAnalyticsGroupByAggregate> aggregate) {
        var response = new ApiUnifiedAnalyticsResponse().type(ApiUnifiedAnalyticsQueryType.GROUP_BY);
        aggregate.ifPresent(a -> {
            var payload = new ApiUnifiedAnalyticsGroupByPayload();
            if (a.getValues() != null) {
                payload.setValues(new HashMap<>(a.getValues()));
            }
            if (a.getMetadata() != null && !a.getMetadata().isEmpty()) {
                Map<String, ApiUnifiedAnalyticsItemMetadata> meta = a
                    .getMetadata()
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> new ApiUnifiedAnalyticsItemMetadata().name(e.getValue().name())));
                payload.setMetadata(meta);
            }
            response.groupBy(payload);
        });
        return response;
    }

    static ApiUnifiedAnalyticsResponse toDateHistoResponse(Optional<ApiAnalyticsDateHistoAggregate> aggregate) {
        var response = new ApiUnifiedAnalyticsResponse().type(ApiUnifiedAnalyticsQueryType.DATE_HISTO);
        aggregate.ifPresent(a -> {
            var payload = new ApiUnifiedAnalyticsDateHistoPayload();
            if (a.getTimestamps() != null) {
                payload.setTimestamps(new ArrayList<>(a.getTimestamps()));
            }
            if (a.getValues() != null) {
                List<ApiUnifiedAnalyticsDateHistoSeries> series = new ArrayList<>();
                for (ApiAnalyticsDateHistoAggregate.Series s : a.getValues()) {
                    var row = new ApiUnifiedAnalyticsDateHistoSeries().field(s.getField());
                    if (s.getBuckets() != null) {
                        row.setBuckets(new ArrayList<>(s.getBuckets()));
                    }
                    if (s.getMetadata() != null) {
                        row.setMetadata(new ApiUnifiedAnalyticsItemMetadata().name(s.getMetadata().name()));
                    }
                    series.add(row);
                }
                payload.setSeries(series);
            }
            response.dateHistogram(payload);
        });
        return response;
    }
}
