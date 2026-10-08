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
package io.gravitee.apim.core.analytics_engine.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * @param ignoredFilters top-level filters the search path skipped for this metric, so its values are
 *     not filtered on them; empty when every filter applied, never null
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public record MetricFacetsResponse(
    MetricSpec.Name metric,
    MetricSpec.Unit unit,
    List<FacetBucketResponse> buckets,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) List<FilterSpec.Name> ignoredFilters
) {
    public MetricFacetsResponse {
        ignoredFilters = ignoredFilters == null ? List.of() : List.copyOf(ignoredFilters);
    }

    public MetricFacetsResponse withIgnoredFilters(List<FilterSpec.Name> ignoredFilters) {
        return new MetricFacetsResponse(metric, unit, buckets, ignoredFilters);
    }
}
