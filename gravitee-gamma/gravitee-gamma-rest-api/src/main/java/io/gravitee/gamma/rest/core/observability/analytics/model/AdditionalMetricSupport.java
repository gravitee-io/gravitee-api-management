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
 * How the analytics engine serves any additional metric field holding a number, the same way for every one: the API
 * kinds it is served for, its measures (the default first), the facets it is grouped by and the filters that narrow
 * it. The definition publishes each numeric field of the environment with exactly these, narrowed to the kinds its
 * namespace ties it to.
 *
 * @param dimensionsOf the built-in metric whose facets every additional metric field takes, read like them from the
 *                     request documents; the environment's keyword and bool keys are added to its facets as well
 * @author GraviteeSource Team
 */
public record AdditionalMetricSupport(
    List<ApiType> apiTypes,
    List<String> measures,
    List<DimensionDefinition> facets,
    List<String> filters,
    String dimensionsOf
) {
    public AdditionalMetricSupport {
        apiTypes = List.copyOf(apiTypes);
        measures = List.copyOf(measures);
        facets = List.copyOf(facets);
        filters = List.copyOf(filters);
    }

    /** Whether a request on these kinds admits additional metrics: one of them is a kind they are served for. */
    public boolean servesAnyOf(Iterable<ApiType> kinds) {
        for (var kind : kinds) {
            if (apiTypes.contains(kind)) {
                return true;
            }
        }
        return false;
    }
}
