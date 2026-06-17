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
package io.gravitee.gamma.rest.core.observability.analytics.port.service_provider;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsFacetMetricQuery;
import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsMetricQuery;
import io.gravitee.gamma.rest.core.observability.analytics.model.AnalyticsNumberRange;
import io.gravitee.gamma.rest.core.observability.analytics.use_case.AnalyticsRequestPipeline;
import io.gravitee.gamma.rest.core.observability.logs.port.service_provider.ObservabilityLogsDataPort.AccessibleApi;
import java.util.List;

/**
 * Core-side port onto the analytics computation engine. Uses only Gamma-native types so the core
 * layer stays independent of the APIM analytics-engine model. The infra adapter handles all
 * translation to the platform compute use cases.
 *
 * <p>Responses are returned as {@link JsonNode} so the rich, nested APIM response hierarchy flows
 * through without requiring duplicate records in the Gamma core. Jackson serializes the node tree
 * directly to the HTTP response body.
 *
 * @author GraviteeSource Team
 */
public interface ObservabilityAnalyticsDataPort {
    List<AccessibleApi> loadAccessibleApis(String organizationId, String environmentId);

    JsonNode computeMeasures(MeasuresQuery query);

    record MeasuresQuery(
        String organizationId,
        String environmentId,
        AnalyticsRequestPipeline.PreparedScope scope,
        List<AnalyticsMetricQuery> metrics
    ) {}

    JsonNode computeFacets(FacetsQuery query);

    record FacetsQuery(
        String organizationId,
        String environmentId,
        AnalyticsRequestPipeline.PreparedScope scope,
        List<String> facets,
        Integer limit,
        List<AnalyticsFacetMetricQuery> metrics,
        List<AnalyticsNumberRange> ranges
    ) {}

    JsonNode computeTimeSeries(TimeSeriesQuery query);

    record TimeSeriesQuery(
        String organizationId,
        String environmentId,
        AnalyticsRequestPipeline.PreparedScope scope,
        Long interval,
        List<String> facets,
        Integer facetSize,
        List<AnalyticsFacetMetricQuery> metrics,
        List<AnalyticsNumberRange> ranges
    ) {}
}
