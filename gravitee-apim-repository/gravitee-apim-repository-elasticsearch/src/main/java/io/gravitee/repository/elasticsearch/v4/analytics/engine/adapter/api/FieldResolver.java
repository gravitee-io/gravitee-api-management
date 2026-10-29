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
package io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.api;

import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.AdditionalMetricField;
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.query.FacetName;
import io.gravitee.repository.analytics.engine.api.query.Filter;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface FieldResolver {
    String fromMetric(Metric metric);
    String fromFilter(Filter.Name name);
    String fromFacet(Facet facet);

    /**
     * The field an additional metric is stored in. Only the resolvers of the documents the reporter writes the
     * {@code additional-metrics} map on answer; any other refuses the field rather than resolve it to a path its
     * index does not hold, which Elasticsearch would answer with no hits instead of an error.
     */
    default String fromAdditionalMetric(AdditionalMetricField field) {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " does not support additional metric '" + field.name() + "'");
    }

    default String fromFilter(Filter filter) {
        return switch (filter.name()) {
            case Filter.Name name -> fromFilter(name);
            case AdditionalMetricField field -> fromAdditionalMetric(field);
        };
    }

    default String fromFacet(FacetName facet) {
        return switch (facet) {
            case Facet builtIn -> fromFacet(builtIn);
            case AdditionalMetricField field -> fromAdditionalMetric(field);
        };
    }

    default String entityTypeFromFilter(Filter filter) {
        throw new UnsupportedOperationException(
            getClass().getSimpleName() + " does not support entity type filter '" + filter.name() + "'"
        );
    }
}
