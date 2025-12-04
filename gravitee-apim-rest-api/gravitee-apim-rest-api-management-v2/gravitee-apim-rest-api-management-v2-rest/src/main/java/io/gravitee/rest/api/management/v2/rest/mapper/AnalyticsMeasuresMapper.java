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

import io.gravitee.apim.core.analytics_engine.model.FacetBucketResponse;
import io.gravitee.apim.core.analytics_engine.model.FacetMetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetsRequest;
import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.MeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MetricFacetsResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.TimeRange;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesBucketResponse;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesMetricResponse;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesRequest;
import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.ArrayFilter;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.Bucket;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.BucketGroup;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.BucketLeaf;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FacetMetricRequest;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FacetsResponse;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FacetsResponseMetricsInner;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FilterName;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.Interval;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.Measure;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MeasuresResponse;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MetricRequest;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.NumberFilter;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.Operator;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.StringFilter;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.TimeSeriesBucket;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.TimeSeriesBucketGroup;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.TimeSeriesBucketLeaf;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.TimeSeriesResponse;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.TimeSeriesResponseMetricsInner;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Mapper
public interface AnalyticsMeasuresMapper {
    AnalyticsMeasuresMapper INSTANCE = Mappers.getMapper(AnalyticsMeasuresMapper.class);

    MeasuresResponse fromResponseModel(io.gravitee.apim.core.analytics_engine.model.MeasuresResponse responseModel);

    List<Measure> fromResponseModel(List<io.gravitee.apim.core.analytics_engine.model.Measure> responseModel);

    FacetsResponse fromResponseModel(io.gravitee.apim.core.analytics_engine.model.FacetsResponse responseModel);

    @Mapping(source = "metric", target = "name")
    FacetsResponseMetricsInner fromResponseModel(MetricFacetsResponse responseModel);

    default Bucket fromResponseModel(FacetBucketResponse responseModel) {
        var bucket = new Bucket();
        if (responseModel.buckets() == null) {
            var leaf = new BucketLeaf()
                .type(BucketLeaf.TypeEnum.LEAF)
                .key(responseModel.key())
                .name(responseModel.name() != null ? responseModel.name() : responseModel.key())
                .measures(fromResponseModel(responseModel.measures()));
            bucket.setActualInstance(leaf);
            return bucket;
        }
        var group = new BucketGroup()
            .type(BucketGroup.TypeEnum.GROUP)
            .key(responseModel.key())
            .name(responseModel.name() != null ? responseModel.name() : responseModel.key());
        group.setBuckets(responseModel.buckets().stream().map(this::fromResponseModel).toList());
        bucket.setActualInstance(group);
        return bucket;
    }

    MeasuresRequest fromRequestEntity(io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MeasuresRequest requestEntity);

    FacetMetricMeasuresRequest fromRequestEntity(FacetMetricRequest facetMetricRequest);

    MetricMeasuresRequest fromRequestEntity(MetricRequest metricRequest);

    @Mapping(target = "facets", source = "by")
    FacetsRequest fromRequestEntity(io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FacetsRequest requestEntity);

    default TimeRange fromTimeRangeEntity(io.gravitee.rest.api.management.v2.rest.model.analytics.engine.TimeRange timeRangeEntity) {
        return new TimeRange(toInstant(timeRangeEntity.getFrom()), toInstant(timeRangeEntity.getTo()));
    }

    default Filter fromFilterEntity(io.gravitee.rest.api.management.v2.rest.model.analytics.engine.Filter filterEntity) {
        var instance = filterEntity.getActualInstance();
        return switch (instance) {
            case NumberFilter n -> new Filter(mapFilterName(n.getName()), mapOperator(n.getOperator()), n.getValue());
            case StringFilter s -> new Filter(mapFilterName(s.getName()), mapOperator(s.getOperator()), s.getValue());
            case ArrayFilter a -> new Filter(mapFilterName(a.getName()), mapOperator(a.getOperator()), a.getValue());
            default -> throw new ValidationDomainException("unknown filter type");
        };
    }

    default FilterSpec.Name mapFilterName(FilterName filterName) {
        return FilterSpec.Name.valueOf(filterName.name());
    }

    default FilterSpec.Operator mapOperator(Operator operator) {
        return FilterSpec.Operator.valueOf(operator.name());
    }

    default Instant toInstant(Object timeRangeBound) {
        return switch (timeRangeBound) {
            case Number n -> Instant.ofEpochMilli(n.longValue());
            case OffsetDateTime odt -> odt.toInstant();
            default -> throw new ValidationDomainException("unknown value type for timeRange bound");
        };
    }

    TimeSeriesResponse fromResponseModel(io.gravitee.apim.core.analytics_engine.model.TimeSeriesResponse responseModel);

    TimeSeriesResponseMetricsInner fromResponseModel(TimeSeriesMetricResponse responseModel);

    default TimeSeriesBucket fromResponseModel(TimeSeriesBucketResponse responseModel) {
        var bucket = new TimeSeriesBucket();
        if (responseModel.buckets() == null) {
            var leaf = new TimeSeriesBucketLeaf()
                .type(TimeSeriesBucketLeaf.TypeEnum.LEAF)
                .key(OffsetDateTime.parse(responseModel.key()))
                .timestamp(responseModel.timestamp())
                .measures(fromResponseModel(responseModel.measures()));
            bucket.setActualInstance(leaf);
            return bucket;
        }
        var group = new TimeSeriesBucketGroup()
            .type(TimeSeriesBucketGroup.TypeEnum.GROUP)
            .key(OffsetDateTime.parse(responseModel.key()))
            .timestamp(responseModel.timestamp());
        group.setBuckets(responseModel.buckets().stream().map(this::fromResponseModel).toList());
        bucket.setActualInstance(group);
        return bucket;
    }

    @Mapping(target = "facets", source = "by")
    TimeSeriesRequest fromRequestEntity(io.gravitee.rest.api.management.v2.rest.model.analytics.engine.TimeSeriesRequest requestEntity);

    default Long parseInterval(Interval interval) {
        return parseIntervalDuration(interval).toMillis();
    }

    default Duration parseIntervalDuration(Interval interval) {
        return switch (interval.getActualInstance()) {
            case Number n -> Duration.ofMillis(n.longValue());
            case String s -> parseDurationString(s);
            default -> throw new TechnicalDomainException("unknown value type for interval");
        };
    }

    default Duration parseDurationString(String s) {
        if (s.endsWith("d")) {
            return Duration.parse("P" + s.toUpperCase());
        }
        return Duration.parse("PT" + s);
    }
}
