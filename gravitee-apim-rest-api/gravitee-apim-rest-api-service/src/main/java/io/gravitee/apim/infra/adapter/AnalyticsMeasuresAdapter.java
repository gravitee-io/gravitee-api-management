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
package io.gravitee.apim.infra.adapter;

import io.gravitee.apim.core.analytics_engine.model.FacetMetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetsRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetsResponse;
import io.gravitee.apim.core.analytics_engine.model.Measure;
import io.gravitee.apim.core.analytics_engine.model.MeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MeasuresResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MetricMeasuresResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesMetricResponse;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesRequest;
import io.gravitee.apim.core.analytics_engine.model.TimeSeriesResponse;
import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.FacetsQuery;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.MeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.MetricMeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.TimeSeriesQuery;
import io.gravitee.repository.analytics.engine.api.result.FacetsResult;
import io.gravitee.repository.analytics.engine.api.result.MeasuresResult;
import io.gravitee.repository.analytics.engine.api.result.MetricMeasuresResult;
import io.gravitee.repository.analytics.engine.api.result.MetricTimeSeriesResult;
import io.gravitee.repository.analytics.engine.api.result.TimeSeriesResult;
import java.util.List;
import java.util.Map;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ValueMapping;
import org.mapstruct.factory.Mappers;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Mapper
public interface AnalyticsMeasuresAdapter {
    AnalyticsMeasuresAdapter INSTANCE = Mappers.getMapper(AnalyticsMeasuresAdapter.class);

    TimeSeriesQuery fromRequest(TimeSeriesRequest request);

    TimeSeriesResponse fromResult(TimeSeriesResult result);

    @Mapping(target = "name", source = "metric")
    TimeSeriesMetricResponse fromResult(MetricTimeSeriesResult result);

    FacetsQuery fromRequest(FacetsRequest request);

    FacetsResponse fromResult(FacetsResult result);

    MeasuresQuery fromRequest(MeasuresRequest request);

    @Mapping(source = "name", target = "metric")
    MetricMeasuresQuery fromRequest(FacetMetricMeasuresRequest request);

    @Mapping(source = "name", target = "metric")
    MetricMeasuresQuery fromRequest(MetricMeasuresRequest request);

    default MeasuresResponse fromResult(MeasuresResult result) {
        if (result == null) {
            return null;
        }
        return new MeasuresResponse(result.measures().stream().map(this::fromResult).toList());
    }

    default MetricMeasuresResponse fromResult(MetricMeasuresResult result) {
        return new MetricMeasuresResponse(fromResult(result.metric()), fromResult(result.measures()));
    }

    default List<Measure> fromResult(Map<io.gravitee.repository.analytics.engine.api.metric.Measure, Number> result) {
        if (result == null) {
            return null;
        }
        return result
            .entrySet()
            .stream()
            .map(entry -> new Measure(fromResult(entry.getKey()), entry.getValue()))
            .toList();
    }

    default MetricSpec.Name fromResult(Metric result) {
        return MetricSpec.Name.valueOf(result.name());
    }

    default MetricSpec.Measure fromResult(io.gravitee.repository.analytics.engine.api.metric.Measure result) {
        return MetricSpec.Measure.valueOf(result.name());
    }

    // API_TYPE should never reach this layer, because it *must* have been transformed to API IDs beforehand.
    @ValueMapping(source = "API_TYPE", target = MappingConstants.THROW_EXCEPTION)
    Filter.Name toFilterName(io.gravitee.apim.core.analytics_engine.model.FilterSpec.Name name);
}
