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
package io.gravitee.apim.rest.api.mcp.core;

import io.gravitee.apim.core.analytics_engine.model.ApiSpec;
import io.gravitee.apim.core.analytics_engine.model.FacetMetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetSpec;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.logs_engine.model.FilterName;
import io.gravitee.apim.core.logs_engine.model.Operator;
import io.gravitee.apim.core.observability.model.FilterOperator;
import java.util.Locale;

/**
 * Turns the names a model writes into the engine's enums, case ignored, with an error that names the catalog.
 */
public final class Names {

    private Names() {}

    public static ApiSpec.Name apiType(String name) {
        return parse(ApiSpec.Name.class, name, "API type", "Call describe_metrics to list them.");
    }

    public static MetricSpec.Name metric(String name) {
        return parse(MetricSpec.Name.class, name, "metric", "Call describe_metrics to list them.");
    }

    public static TriageException unknownMetric(String name) {
        return new TriageException("Unknown metric '" + name + "'. Call describe_metrics to list them.");
    }

    public static MetricSpec.Measure measure(String name) {
        return parse(MetricSpec.Measure.class, name, "measure", "Call describe_metrics with metric=<name> for the measures it takes.");
    }

    public static FacetSpec.Name facet(String name) {
        return parse(FacetSpec.Name.class, name, "facet", "Call describe_metrics with metric=<name> for the facets it takes.");
    }

    public static FilterSpec.Name filter(String name) {
        return parse(FilterSpec.Name.class, name, "filter", "Call describe_metrics with metric=<name> for the filters it takes.");
    }

    public static FilterOperator operator(String name) {
        return parse(FilterOperator.class, name, "operator", "Use EQ, IN, LTE, GTE or CONTAINS.");
    }

    public static FacetMetricMeasuresRequest.Sort.Order order(String name) {
        return parse(FacetMetricMeasuresRequest.Sort.Order.class, name, "sort order", "Use ASC or DESC.");
    }

    public static FilterName logFilter(String name) {
        return parse(FilterName.class, name, "log filter", "See the search_logs contract for the filters it takes.");
    }

    public static Operator logOperator(String name) {
        return parse(Operator.class, name, "operator", "Use EQ, IN, LTE, GTE or CONTAINS.");
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String name, String what, String hint) {
        if (name != null) {
            for (var candidate : type.getEnumConstants()) {
                if (candidate.name().equalsIgnoreCase(name.trim())) {
                    return candidate;
                }
            }
        }
        throw new TriageException("Unknown " + what + " '" + name + "'. " + hint);
    }

    /** {@code HTTP_REQUESTS} as a model reads it: unchanged; kept in one place should the wire names ever differ. */
    public static String wire(Enum<?> value) {
        return value.name().toUpperCase(Locale.ROOT);
    }
}
