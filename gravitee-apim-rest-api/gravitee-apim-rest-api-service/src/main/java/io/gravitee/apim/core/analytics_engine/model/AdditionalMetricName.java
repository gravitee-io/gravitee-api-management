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
import java.util.Optional;

/**
 * The analytics name of an additional metric field: a filter or a facet the closed vocabularies do not hold, read
 * by the engines from its value type and raw key alone. Discovery is never consulted, so a well-formed name with no
 * data reads as an empty result.
 */
public record AdditionalMetricName(AdditionalMetricField field) implements FilterName, FacetName {
    static Optional<AdditionalMetricName> parse(String name) {
        return AdditionalMetricField.parse(name).map(AdditionalMetricName::new);
    }

    @Override
    @JsonValue
    public String name() {
        return field.name();
    }
}
