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
package io.gravitee.rest.api.management.v2.rest.mapper;

import io.gravitee.apim.core.analytics.model.Bucket;
import io.gravitee.apim.core.analytics.model.ResponseStatusOvertime;
import io.gravitee.apim.core.analytics.model.StatsAnalytics;
import io.gravitee.apim.core.analytics.model.Timestamp;
import io.gravitee.rest.api.management.v2.rest.model.AnalyticTimeRange;
import io.gravitee.rest.api.management.v2.rest.model.AnalyticsType;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsAverageConnectionDurationResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsAverageMessagesPerRequestResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsRequestsCountResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsResponseStatusOvertimeResponse;
import io.gravitee.rest.api.management.v2.rest.model.ApiAnalyticsResponseStatusRangesResponse;
import io.gravitee.rest.api.management.v2.rest.model.CountAnalytics;
import io.gravitee.rest.api.management.v2.rest.model.GroupByAnalytics;
import io.gravitee.rest.api.management.v2.rest.model.HistogramAnalytics;
import io.gravitee.rest.api.management.v2.rest.model.HistogramAnalyticsAllOfValues;
import io.gravitee.rest.api.management.v2.rest.model.HistogramTimestamp;
import io.gravitee.rest.api.model.v4.analytics.AverageConnectionDuration;
import io.gravitee.rest.api.model.v4.analytics.AverageMessagesPerRequest;
import io.gravitee.rest.api.model.v4.analytics.RequestsCount;
import io.gravitee.rest.api.model.v4.analytics.ResponseStatusRanges;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mapper
public interface ApiAnalyticsMapper {
    Logger logger = LoggerFactory.getLogger(ApiAnalyticsMapper.class);
    ApiAnalyticsMapper INSTANCE = Mappers.getMapper(ApiAnalyticsMapper.class);

    @Mapping(target = "countsByEntrypoint", source = "countsByEntrypoint")
    ApiAnalyticsRequestsCountResponse map(RequestsCount requestsCount);

    @Mapping(target = "average", source = "globalAverage")
    @Mapping(target = "averagesByEntrypoint", source = "averagesByEntrypoint")
    ApiAnalyticsAverageMessagesPerRequestResponse map(AverageMessagesPerRequest averageMessagesPerRequest);

    @Mapping(target = "average", source = "globalAverage")
    @Mapping(target = "averagesByEntrypoint", source = "averagesByEntrypoint")
    ApiAnalyticsAverageConnectionDurationResponse map(AverageConnectionDuration averageConnectionDuration);

    @Mapping(target = "ranges", source = "ranges")
    @Mapping(target = "rangesByEntrypoint", source = "statusRangesCountByEntrypoint")
    ApiAnalyticsResponseStatusRangesResponse map(ResponseStatusRanges responseStatusRanges);

    ApiAnalyticsResponseStatusOvertimeResponse map(ResponseStatusOvertime source);

    @Mapping(target = "from", expression = "java(source.from().toEpochMilli())")
    @Mapping(target = "to", expression = "java(source.to().toEpochMilli())")
    @Mapping(target = "interval", expression = "java(source.interval().toMillis())")
    AnalyticTimeRange map(ResponseStatusOvertime.TimeRange source);

    @Mapping(target = "analyticsType", expression = "java(io.gravitee.rest.api.management.v2.rest.model.AnalyticsType.COUNT)")
    @Mapping(target = "count", source = "total")
    CountAnalytics mapToCountAnalytics(RequestsCount requestsCount);

    Map<String, Number> map(Map<String, Long> value);

    default HistogramAnalytics mapHistogramAnalytics(List<Bucket> buckets) {
        if (buckets == null) {
            return null;
        }
        HistogramAnalytics analytics = new HistogramAnalytics();
        analytics.analyticsType(AnalyticsType.HISTOGRAM);
        analytics.setValues(mapBuckets(buckets));
        return analytics;
    }

    List<@Valid HistogramAnalyticsAllOfValues> mapBuckets(List<Bucket> buckets);

    default HistogramTimestamp map(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        HistogramTimestamp range = new HistogramTimestamp();
        range.setFrom(timestamp.getFrom().toEpochMilli());
        range.setTo(timestamp.getTo().toEpochMilli());
        range.setInterval(timestamp.getInterval().toMillis());
        return range;
    }

    default GroupByAnalytics mapGroupByAnalytics(
        io.gravitee.apim.core.analytics.model.GroupByAnalytics source,
        Map<String, Map<String, String>> metadata
    ) {
        if (source == null) {
            return null;
        }
        GroupByAnalytics analytics = new GroupByAnalytics();
        analytics.analyticsType(AnalyticsType.GROUP_BY);
        analytics.setValues(source.getValues());
        analytics.setMetadata(metadata);
        return analytics;
    }

    io.gravitee.rest.api.management.v2.rest.model.StatsAnalytics map(StatsAnalytics statsAnalytics);
}
