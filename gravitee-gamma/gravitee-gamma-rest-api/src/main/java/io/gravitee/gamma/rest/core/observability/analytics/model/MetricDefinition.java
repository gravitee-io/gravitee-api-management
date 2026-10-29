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

/**
 * One metric of the analytics catalog and what a query may ask of it: its measures, the facets it is grouped
 * by and the filters that narrow it. Names are the ones the analytics endpoints accept.
 *
 * @author GraviteeSource Team
 */
public record MetricDefinition(
    String name,
    String label,
    String unit,
    List<ApiType> apiTypes,
    List<String> measures,
    List<DimensionDefinition> facets,
    List<String> filters,
    CatalogOrigin origin
) {
    public MetricFamily family() {
        return MetricFamily.of(apiTypes);
    }
}
