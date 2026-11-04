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
package io.gravitee.gamma.rest.infra.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.gamma.rest.core.observability.catalog.model.AdditionalMetricUnit;
import java.util.Optional;

/**
 * The unit Gamma serves for a built-in metric, in the vocabulary of {@link AdditionalMetricUnit#NAMED_UNITS}. The
 * engine's {@link MetricSpec.Unit} stays as it is: the Management API v2 and the Portal API serve it unchanged.
 *
 * <p>A unit served as {@code CURRENCY} comes with its companion {@code currency}, the ISO 4217 code of
 * {@link PlatformCurrency#CODE}: only on a built-in, a declared record in {@code CURRENCY} carrying none. A declared
 * record's unit never reaches a data response (the engine serves no unit for an additional metric,
 * {@code UnitEnrichmentPostProcessorImpl.unitOf}), and the definition adapter sets the code on built-ins only.
 *
 * <p>Called by {@link AnalyticsDefinitionPortAdapter} through {@link #of} and by
 * {@link ObservabilityAnalyticsDataPortAdapter} through {@link #serveIn} on each measures, facets and time-series
 * response. Pinned by {@code ServedMetricUnitTest} (every engine unit is a named unit) and
 * by {@code ObservabilityAnalyticsDataPortAdapterTest$ServedUnits} (what each response serves).
 */
final class ServedMetricUnit {

    private ServedMetricUnit() {}

    static String of(MetricSpec.Unit unit) {
        return switch (unit) {
            case NUMBER -> "COUNT";
            case MILLISECONDS, BYTES, PERCENT, TOKENS, CURRENCY -> unit.name();
        };
    }

    /** The currency code served next to a unit: {@link PlatformCurrency#CODE} for {@code CURRENCY}, none otherwise. */
    static Optional<String> currencyOf(MetricSpec.Unit unit) {
        return unit == MetricSpec.Unit.CURRENCY ? Optional.of(PlatformCurrency.CODE) : Optional.empty();
    }

    /**
     * Rewrites every {@code unit} field of a serialized response, at any depth, so that no response shape escapes the
     * mapping, and puts {@code currency} next to a unit mapped to {@code CURRENCY}. Only a text value naming a
     * {@link MetricSpec.Unit} is rewritten: a declared record's free suffix and a named unit already served as such are
     * left as they are. On a data response a text unit is always a built-in's: an additional metric serves none.
     */
    static void serveIn(JsonNode response) {
        for (var parent : response.findParents("unit")) {
            var unit = parent.get("unit");
            if (parent instanceof ObjectNode entry && unit.isTextual()) {
                EnumNames.find(MetricSpec.Unit.class, unit.asText()).ifPresent(engineUnit -> {
                    entry.put("unit", of(engineUnit));
                    currencyOf(engineUnit).ifPresent(code -> entry.put("currency", code));
                });
            }
        }
    }
}
