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
package io.gravitee.gamma.rest.core.observability.analytics.model;

import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import java.util.List;

/** Catalog metrics for tests: each name and facet is labelled "{@code <name> label}". */
public final class MetricDefinitionFixtures {

    private MetricDefinitionFixtures() {}

    public static MetricDefinition aMetric(String name, List<ApiType> apiTypes, String... facets) {
        return new MetricDefinition(
            name,
            name + " label",
            "NUMBER",
            apiTypes,
            List.of("COUNT", "AVG"),
            List.of(facets)
                .stream()
                .map(facet -> new DimensionDefinition(facet, facet + " label", CatalogOrigin.BUILT_IN))
                .toList(),
            List.of("API", "API_TYPE"),
            CatalogOrigin.BUILT_IN
        );
    }
}
