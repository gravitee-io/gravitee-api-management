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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Adds the conditions Gamma did not hand to the engine to each metric's {@code ignoredFilters}, after
 * the ones the engine reported. The engine's metric records name their metric under {@code name} on
 * measures and time series, under {@code metric} on facets.
 */
final class ConditionsNotAppliedEnricher {

    private ConditionsNotAppliedEnricher() {}

    static void enrichMeasures(JsonNode response, Map<String, List<String>> notApplied) {
        enrich(response, notApplied, "name");
    }

    static void enrichFacets(JsonNode response, Map<String, List<String>> notApplied) {
        enrich(response, notApplied, "metric");
    }

    static void enrichTimeSeries(JsonNode response, Map<String, List<String>> notApplied) {
        enrich(response, notApplied, "name");
    }

    private static void enrich(JsonNode response, Map<String, List<String>> notApplied, String nameField) {
        if (notApplied == null || notApplied.isEmpty() || !(response.get("metrics") instanceof ArrayNode metrics)) {
            return;
        }
        for (var metric : metrics) {
            var extra = notApplied.get(metric.path(nameField).asText());
            if (extra == null || !(metric instanceof ObjectNode metricNode)) {
                continue;
            }
            var names = new LinkedHashSet<String>();
            metricNode.path("ignoredFilters").forEach(name -> names.add(name.asText()));
            names.addAll(extra);
            var array = metricNode.putArray("ignoredFilters");
            names.forEach(array::add);
        }
    }
}
