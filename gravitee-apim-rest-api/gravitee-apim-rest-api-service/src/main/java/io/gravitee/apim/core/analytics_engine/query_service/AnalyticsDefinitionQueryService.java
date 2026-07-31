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
package io.gravitee.apim.core.analytics_engine.query_service;

import io.gravitee.apim.core.analytics_engine.model.ApiSpec;
import io.gravitee.apim.core.analytics_engine.model.FacetSpec;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.apim.core.observability.model.Signal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface AnalyticsDefinitionQueryService {
    List<ApiSpec> getApis();

    List<MetricSpec> getMetrics(ApiSpec.Name apiSpecName);

    /**
     * The whole catalog, every signal included.
     *
     * <p>Callers that resolve a filter by name (value listing, label resolution) must keep using this: they
     * accept any catalog name regardless of the surface it is advertised on. Consumers that <em>offer</em>
     * filters to a user want {@link #getFilters(Set)} instead.
     */
    List<FilterSpec> getAllFilters();

    /** The catalog narrowed to the filters advertised for at least one of {@code signals}. Empty means no narrowing. */
    List<FilterSpec> getFilters(Set<Signal> signals);

    List<FilterSpec> getFilters(MetricSpec.Name metricSpecName);

    List<FacetSpec> getFacets(MetricSpec.Name metricSpecName);

    Optional<MetricSpec> findMetric(MetricSpec.Name metricName);

    /**
     * The catalog entry for {@code filterName}, or empty when the catalog does not describe it.
     *
     * <p>Prefer this over scanning {@link #getAllFilters()}: validation resolves a filter per condition per
     * request, which a scan turns into work proportional to the size of the catalog.
     */
    Optional<FilterSpec> findFilter(FilterSpec.Name filterName);

    default ApiSpec.Name validateApiName(String apiName) {
        try {
            return ApiSpec.Name.valueOf(apiName);
        } catch (IllegalArgumentException e) {
            throw new ValidationDomainException(
                "Invalid api name",
                Map.of("invalidName", apiName, "validNames", Arrays.toString(ApiSpec.Name.values()))
            );
        }
    }

    default MetricSpec.Name validateMetricName(String metricName) {
        try {
            return MetricSpec.Name.valueOf(metricName);
        } catch (IllegalArgumentException e) {
            throw new ValidationDomainException(
                "Invalid metric name",
                Map.of("invalidName", metricName, "validNames", Arrays.toString(MetricSpec.Name.values()))
            );
        }
    }
}
