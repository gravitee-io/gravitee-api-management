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
package fixtures.core.model;

import io.gravitee.apim.core.analytics_engine.model.ApiSpec;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.observability.model.FilterOperator;
import io.gravitee.apim.core.observability.model.FilterType;
import io.gravitee.apim.core.observability.model.NumberRange;
import io.gravitee.apim.core.observability.model.Signal;
import java.util.List;
import java.util.Set;

/**
 * Builds {@link FilterSpec} for tests that stub the analytics definition catalog.
 *
 * <p>{@code FilterSpec} is a record, so every positional {@code new FilterSpec(...)} in a test breaks as soon
 * as the catalog gains an axis — which is how the {@code signals} axis landed. Going through these helpers
 * keeps the next axis a one-file change.
 *
 * @author GraviteeSource Team
 */
public final class FilterSpecFixtures {

    private FilterSpecFixtures() {}

    public static FilterSpec keyword(FilterSpec.Name name, String label) {
        return of(name, label, FilterType.KEYWORD, null, null, List.of(FilterOperator.EQ, FilterOperator.IN));
    }

    public static FilterSpec enumeration(FilterSpec.Name name, String label, List<String> enumValues) {
        return of(name, label, FilterType.ENUM, enumValues, null, List.of(FilterOperator.EQ, FilterOperator.IN));
    }

    public static FilterSpec number(FilterSpec.Name name, String label, NumberRange range) {
        return of(name, label, FilterType.NUMBER, null, range, List.of(FilterOperator.EQ, FilterOperator.LTE, FilterOperator.GTE));
    }

    public static FilterSpec string(FilterSpec.Name name, String label) {
        return of(name, label, FilterType.STRING, null, null, List.of(FilterOperator.EQ));
    }

    /** Same spec, narrowed to the given signals — for tests asserting on catalog narrowing. */
    public static FilterSpec withSignals(FilterSpec spec, Signal... signals) {
        return new FilterSpec(
            spec.name(),
            spec.label(),
            spec.type(),
            spec.enumValues(),
            spec.range(),
            spec.operators(),
            spec.apis(),
            Set.of(signals)
        );
    }

    private static FilterSpec of(
        FilterSpec.Name name,
        String label,
        FilterType type,
        List<String> enumValues,
        NumberRange range,
        List<FilterOperator> operators
    ) {
        // apis null and signals null mirror an unannotated catalog entry: apis are derived from the metrics
        // referencing the filter, signals fall back to Signal.DEFAULT.
        return new FilterSpec(name, label, type, enumValues, range, operators, (List<ApiSpec.Name>) null, null);
    }
}
