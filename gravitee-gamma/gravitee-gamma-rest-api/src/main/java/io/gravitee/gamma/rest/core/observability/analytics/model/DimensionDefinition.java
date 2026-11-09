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

import io.gravitee.gamma.rest.core.observability.catalog.model.CatalogOrigin;
import io.gravitee.gamma.rest.core.observability.filter.model.ApiType;
import java.util.Set;

/**
 * A facet a metric can be grouped by, with the label of the analytics catalog.
 *
 * @param namespace the namespace of an additional metric field, as the catalog serves it
 *             ({@code AdditionalMetricNamespace}); {@code null} on a built-in and when unknown
 * @param apiTypes the kinds the facet is tied to, as the filter catalog serves them for the filter of the same name:
 *             on an additional metric field, those its namespace ties it to, none when the namespace says nothing;
 *             on a built-in, those of the filter of its name, none when there is no such filter
 * @author GraviteeSource Team
 */
public record DimensionDefinition(String name, String label, String namespace, Set<ApiType> apiTypes, CatalogOrigin origin) {
    /** A facet tied to no kind: a built-in, or an additional metric field of no known namespace. */
    public DimensionDefinition(String name, String label, CatalogOrigin origin) {
        this(name, label, null, Set.of(), origin);
    }
}
