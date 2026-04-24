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
package io.gravitee.rest.api.portal.rest.mapper;

import io.gravitee.apim.core.analytics_engine.model.FacetBucketResponse;
import io.gravitee.apim.core.analytics_engine.model.FacetMetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetSpec;
import io.gravitee.apim.core.analytics_engine.model.FacetsRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetsResponse;
import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.Measure;
import io.gravitee.apim.core.analytics_engine.model.MeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MeasuresResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricFacetsResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MetricMeasuresResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.model.TimeRange;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesBucketResponse;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesMetricResponse;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesRequest;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesResponse;
import io.gravitee.apim.core.observability.model.FilterOperator;
import io.gravitee.apim.core.observability.model.NumberRange;
import io.gravitee.rest.api.portal.rest.model.AnalyticsArrayFilter;
import io.gravitee.rest.api.portal.rest.model.AnalyticsComputationMetricRequest;
import io.gravitee.rest.api.portal.rest.model.AnalyticsFacetBucket;
import io.gravitee.rest.api.portal.rest.model.AnalyticsFacetBucketGroup;
import io.gravitee.rest.api.portal.rest.model.AnalyticsFacetBucketLeaf;
import io.gravitee.rest.api.portal.rest.model.AnalyticsFacetMetricRequest;
import io.gravitee.rest.api.portal.rest.model.AnalyticsFacetName;
import io.gravitee.rest.api.portal.rest.model.AnalyticsFacetsRequest;
import io.gravitee.rest.api.portal.rest.model.AnalyticsFacetsResponse;
import io.gravitee.rest.api.portal.rest.model.AnalyticsFacetsResponseMetric;
import io.gravitee.rest.api.portal.rest.model.AnalyticsFilter;
import io.gravitee.rest.api.portal.rest.model.AnalyticsMeasure;
import io.gravitee.rest.api.portal.rest.model.AnalyticsMeasureName;
import io.gravitee.rest.api.portal.rest.model.AnalyticsMeasuresRequest;
import io.gravitee.rest.api.portal.rest.model.AnalyticsMeasuresResponse;
import io.gravitee.rest.api.portal.rest.model.AnalyticsMeasuresResponseMetric;
import io.gravitee.rest.api.portal.rest.model.AnalyticsMetricName;
import io.gravitee.rest.api.portal.rest.model.AnalyticsNumberFilter;
import io.gravitee.rest.api.portal.rest.model.AnalyticsNumberRange;
import io.gravitee.rest.api.portal.rest.model.AnalyticsSort;
import io.gravitee.rest.api.portal.rest.model.AnalyticsStringFilter;
import io.gravitee.rest.api.portal.rest.model.AnalyticsTimeRange;
import io.gravitee.rest.api.portal.rest.model.AnalyticsTimeSeriesBucket;
import io.gravitee.rest.api.portal.rest.model.AnalyticsTimeSeriesBucketGroup;
import io.gravitee.rest.api.portal.rest.model.AnalyticsTimeSeriesBucketLeaf;
import io.gravitee.rest.api.portal.rest.model.AnalyticsTimeSeriesRequest;
import io.gravitee.rest.api.portal.rest.model.AnalyticsTimeSeriesResponse;
import io.gravitee.rest.api.portal.rest.model.AnalyticsTimeSeriesResponseMetric;
import io.gravitee.rest.api.portal.rest.model.AnalyticsUnitName;
import jakarta.ws.rs.BadRequestException;
import java.time.OffsetDateTime;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * Converts portal analytics computation DTOs.
 *
 * @author GraviteeSource Team
 */
@Mapper
public interface PortalAnalyticsMeasuresMapper {
    PortalAnalyticsMeasuresMapper INSTANCE = Mappers.getMapper(PortalAnalyticsMeasuresMapper.class);

    default MeasuresRequest toCoreRequest(AnalyticsMeasuresRequest request) {
        return new MeasuresRequest(
            toCoreTimeRange(request.getTimeRange()),
            toCoreFilters(request.getFilters()),
            request.getMetrics().stream().map(this::toCoreMetric).toList()
        );
    }

    default FacetsRequest toCoreRequest(AnalyticsFacetsRequest request) {
        return new FacetsRequest(
            toCoreTimeRange(request.getTimeRange()),
            toCoreFilters(request.getFilters()),
            request.getMetrics().stream().map(this::toCoreMetric).toList(),
            toCoreFacets(request.getBy()),
            request.getLimit(),
            toCoreNumberRanges(request.getRanges())
        );
    }

    default TimeSeriesRequest toCoreRequest(AnalyticsTimeSeriesRequest request) {
        return new TimeSeriesRequest(
            toCoreTimeRange(request.getTimeRange()),
            request.getInterval().toMillis(),
            toCoreFilters(request.getFilters()),
            request.getMetrics().stream().map(this::toCoreMetric).toList(),
            toCoreFacets(request.getBy()),
            request.getLimit(),
            toCoreNumberRanges(request.getRanges())
        );
    }

    default MetricMeasuresRequest toCoreMetric(AnalyticsComputationMetricRequest metric) {
        return new MetricMeasuresRequest(
            MetricSpec.Name.valueOf(metric.getName().name()),
            toCoreMeasures(metric.getMeasures()),
            toCoreFilters(metric.getFilters())
        );
    }

    default FacetMetricMeasuresRequest toCoreMetric(AnalyticsFacetMetricRequest metric) {
        var sorts = metric.getSorts() == null
            ? List.<FacetMetricMeasuresRequest.Sort>of()
            : metric.getSorts().stream().map(this::toCoreSort).toList();
        return new FacetMetricMeasuresRequest(
            MetricSpec.Name.valueOf(metric.getName().name()),
            toCoreMeasures(metric.getMeasures()),
            toCoreFilters(metric.getFilters()),
            sorts
        );
    }

    AnalyticsMeasuresResponse toPortalResponse(MeasuresResponse response);

    AnalyticsFacetsResponse toPortalResponse(FacetsResponse response);

    AnalyticsTimeSeriesResponse toPortalResponse(TimeSeriesResponse response);

    default TimeRange toCoreTimeRange(AnalyticsTimeRange timeRange) {
        return new TimeRange(timeRange.getFrom().toInstant(), timeRange.getTo().toInstant());
    }

    default Filter toCoreFilter(AnalyticsFilter filter) {
        return switch (filter.getActualInstance()) {
            case AnalyticsStringFilter s -> new Filter(
                FilterSpec.Name.valueOf(s.getName().name()),
                FilterOperator.valueOf(s.getOperator().name()),
                s.getValue()
            );
            case AnalyticsNumberFilter n -> new Filter(
                FilterSpec.Name.valueOf(n.getName().name()),
                FilterOperator.valueOf(n.getOperator().name()),
                n.getValue()
            );
            case AnalyticsArrayFilter a -> new Filter(
                FilterSpec.Name.valueOf(a.getName().name()),
                FilterOperator.valueOf(a.getOperator().name()),
                a.getValue()
            );
            case null, default -> throw new BadRequestException("Unknown or unsupported analytics filter shape");
        };
    }

    default NumberRange toCoreNumberRange(AnalyticsNumberRange range) {
        return new NumberRange(range.getFrom(), range.getTo());
    }

    default FacetMetricMeasuresRequest.Sort toCoreSort(AnalyticsSort sort) {
        return new FacetMetricMeasuresRequest.Sort(
            MetricSpec.Measure.valueOf(sort.getMeasure().name()),
            FacetMetricMeasuresRequest.Sort.Order.valueOf(sort.getOrder().name())
        );
    }

    default List<Filter> toCoreFilters(List<AnalyticsFilter> filters) {
        return filters == null ? List.of() : filters.stream().map(this::toCoreFilter).toList();
    }

    default List<NumberRange> toCoreNumberRanges(List<AnalyticsNumberRange> ranges) {
        return ranges == null ? List.of() : ranges.stream().map(this::toCoreNumberRange).toList();
    }

    default List<MetricSpec.Measure> toCoreMeasures(List<AnalyticsMeasureName> measures) {
        return measures == null
            ? List.of()
            : measures
                .stream()
                .map(m -> MetricSpec.Measure.valueOf(m.name()))
                .toList();
    }

    default List<FacetSpec.Name> toCoreFacets(List<AnalyticsFacetName> by) {
        return by == null
            ? List.of()
            : by
                .stream()
                .map(f -> FacetSpec.Name.valueOf(f.name()))
                .toList();
    }

    default AnalyticsMeasuresResponseMetric toPortalMetric(MetricMeasuresResponse metric) {
        var dto = new AnalyticsMeasuresResponseMetric();
        dto.setName(AnalyticsMetricName.fromValue(metric.name().name()));
        if (metric.unit() != null) {
            dto.setUnit(AnalyticsUnitName.fromValue(metric.unit().name()));
        }
        dto.setMeasures(toPortalMeasures(metric.measures()));
        return dto;
    }

    default AnalyticsFacetsResponseMetric toPortalMetric(MetricFacetsResponse metric) {
        var dto = new AnalyticsFacetsResponseMetric();
        dto.setName(AnalyticsMetricName.fromValue(metric.metric().name()));
        if (metric.unit() != null) {
            dto.setUnit(AnalyticsUnitName.fromValue(metric.unit().name()));
        }
        if (metric.buckets() != null) {
            dto.setBuckets(metric.buckets().stream().map(this::toPortalFacetBucket).toList());
        }
        return dto;
    }

    default AnalyticsTimeSeriesResponseMetric toPortalMetric(TimeSeriesMetricResponse metric) {
        var dto = new AnalyticsTimeSeriesResponseMetric();
        dto.setName(AnalyticsMetricName.fromValue(metric.name().name()));
        if (metric.unit() != null) {
            dto.setUnit(AnalyticsUnitName.fromValue(metric.unit().name()));
        }
        if (metric.buckets() != null) {
            dto.setBuckets(metric.buckets().stream().map(this::toPortalTimeSeriesBucket).toList());
        }
        return dto;
    }

    default AnalyticsFacetBucket toPortalFacetBucket(FacetBucketResponse bucket) {
        var wrapper = new AnalyticsFacetBucket();
        if (bucket.buckets() == null) {
            var leaf = new AnalyticsFacetBucketLeaf()
                .type(AnalyticsFacetBucketLeaf.TypeEnum.LEAF)
                .key(bucket.key())
                .name(bucket.name() != null ? bucket.name() : bucket.key())
                .measures(toPortalMeasures(bucket.measures()));
            wrapper.setActualInstance(leaf);
            return wrapper;
        }
        var group = new AnalyticsFacetBucketGroup()
            .type(AnalyticsFacetBucketGroup.TypeEnum.GROUP)
            .key(bucket.key())
            .name(bucket.name() != null ? bucket.name() : bucket.key());
        group.setBuckets(bucket.buckets().stream().map(this::toPortalFacetBucket).toList());
        wrapper.setActualInstance(group);
        return wrapper;
    }

    default AnalyticsTimeSeriesBucket toPortalTimeSeriesBucket(TimeSeriesBucketResponse bucket) {
        var wrapper = new AnalyticsTimeSeriesBucket();
        if (bucket.buckets() == null) {
            var leaf = new AnalyticsTimeSeriesBucketLeaf()
                .type(AnalyticsTimeSeriesBucketLeaf.TypeEnum.LEAF)
                .key(OffsetDateTime.parse(bucket.key()))
                .timestamp(bucket.timestamp())
                .measures(toPortalMeasures(bucket.measures()));
            wrapper.setActualInstance(leaf);
            return wrapper;
        }
        var group = new AnalyticsTimeSeriesBucketGroup()
            .type(AnalyticsTimeSeriesBucketGroup.TypeEnum.GROUP)
            .key(OffsetDateTime.parse(bucket.key()))
            .timestamp(bucket.timestamp());
        group.setBuckets(bucket.buckets().stream().map(this::toPortalFacetBucket).toList());
        wrapper.setActualInstance(group);
        return wrapper;
    }

    default AnalyticsMeasure toPortalMeasure(Measure measure) {
        return new AnalyticsMeasure().name(AnalyticsMeasureName.fromValue(measure.name().name())).value(measure.value());
    }

    List<AnalyticsMeasure> toPortalMeasures(List<Measure> measures);
}
