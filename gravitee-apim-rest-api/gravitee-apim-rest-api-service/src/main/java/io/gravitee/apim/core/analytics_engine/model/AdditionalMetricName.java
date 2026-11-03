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

import com.fasterxml.jackson.annotation.JsonValue;
import io.gravitee.apim.core.observability.model.AdditionalMetricField;
import java.util.List;
import java.util.Optional;

/**
 * The analytics name of an additional metric field: a filter, a facet or a metric the closed vocabularies do not hold,
 * read by the engines from its value type and raw key alone. Discovery is never consulted, so a well-formed name with
 * no data reads as an empty result.
 */
public record AdditionalMetricName(AdditionalMetricField field) implements FilterName, FacetName, MetricName {
    /**
     * The measures of a field holding a number, the default first. {@code COUNT} counts the requests carrying the field;
     * a percentage of a field means nothing.
     */
    public static final List<MetricSpec.Measure> MEASURES = List.of(
        MetricSpec.Measure.AVG,
        MetricSpec.Measure.SUM,
        MetricSpec.Measure.MIN,
        MetricSpec.Measure.MAX,
        MetricSpec.Measure.P50,
        MetricSpec.Measure.P90,
        MetricSpec.Measure.P95,
        MetricSpec.Measure.P99,
        MetricSpec.Measure.COUNT
    );

    /**
     * The built-in whose dimensions a metric of this kind is grouped and filtered by: both are read from the request
     * documents.
     */
    public static final MetricSpec.Name DIMENSIONS_OF = MetricSpec.Name.HTTP_REQUESTS;

    static Optional<AdditionalMetricName> parse(String name) {
        return AdditionalMetricField.parse(name).map(AdditionalMetricName::new);
    }

    @Override
    @JsonValue
    public String name() {
        return field.name();
    }
}
