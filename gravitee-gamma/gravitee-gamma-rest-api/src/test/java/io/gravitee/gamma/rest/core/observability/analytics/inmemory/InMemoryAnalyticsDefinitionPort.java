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
package io.gravitee.gamma.rest.core.observability.analytics.inmemory;

import static io.gravitee.gamma.rest.core.observability.analytics.model.MetricDefinitionFixtures.aMetric;

import io.gravitee.gamma.rest.core.observability.analytics.model.MetricDefinition;
import io.gravitee.gamma.rest.core.observability.analytics.port.service_provider.AnalyticsDefinitionPort;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import java.util.ArrayList;
import java.util.List;

/** An analytics catalog holding the metrics a test gives it, in the order it gives them. */
public class InMemoryAnalyticsDefinitionPort implements AnalyticsDefinitionPort {

    private final List<MetricDefinition> metrics = new ArrayList<>();

    public MetricDefinition givenMetric(String name, List<ApiType> apiTypes, String... facets) {
        var metric = aMetric(name, apiTypes, facets);
        metrics.add(metric);
        return metric;
    }

    @Override
    public List<MetricDefinition> metricsOf(ApiType apiKind) {
        return metrics
            .stream()
            .filter(metric -> metric.apiTypes().contains(apiKind))
            .toList();
    }
}
