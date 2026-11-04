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

/**
 * The unit Gamma serves for a built-in metric, in the vocabulary of {@link AdditionalMetricUnit#NAMED_UNITS}. The
 * engine's {@link MetricSpec.Unit} stays as it is: the Management API v2 and the Portal API serve it unchanged.
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

    /**
     * Rewrites every {@code unit} field of a serialized response, at any depth, so that no response shape escapes the
     * mapping. Only a text value naming a {@link MetricSpec.Unit} is rewritten: a declared record's free suffix and a
     * named unit already served as such are left as they are.
     */
    static void serveIn(JsonNode response) {
        for (var parent : response.findParents("unit")) {
            var unit = parent.get("unit");
            if (parent instanceof ObjectNode entry && unit.isTextual()) {
                EnumNames.find(MetricSpec.Unit.class, unit.asText()).ifPresent(engineUnit -> entry.put("unit", of(engineUnit)));
            }
        }
    }
}
