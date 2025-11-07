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
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public interface AnalyticsDefinitionQueryService {
    List<ApiSpec> getApis();

    List<MetricSpec> getMetrics(ApiSpec.Name apiSpecName);

    List<FilterSpec> getFilters(MetricSpec.Name metricSpecName);

    List<FacetSpec> getFacets(MetricSpec.Name metricSpecName);

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
