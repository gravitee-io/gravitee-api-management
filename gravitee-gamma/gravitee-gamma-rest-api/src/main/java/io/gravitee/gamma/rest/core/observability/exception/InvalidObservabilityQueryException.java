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
package io.gravitee.gamma.rest.core.observability.exception;

import io.gravitee.apim.core.exception.ValidationDomainException;

/**
 * A logs or analytics query whose shape, outside its filters, cannot be answered: a missing time bound, a
 * missing interval, no metric, or a metric, measure, facet or sort the engine does not know. Translated to
 * HTTP 400 by the apim {@code ValidationDomainExceptionMapper}, with a technical code per rule.
 *
 * @author GraviteeSource Team
 */
public class InvalidObservabilityQueryException extends ValidationDomainException {

    public static InvalidObservabilityQueryException missingTimeRangeBound(String bound) {
        return new InvalidObservabilityQueryException(
            "Time range bound '" + bound + "' is required",
            "observability.query.time_range_required"
        );
    }

    public static InvalidObservabilityQueryException invalidTimeRange() {
        return new InvalidObservabilityQueryException(
            "Invalid time range: 'from' must be before 'to'.",
            "observability.query.invalid_time_range"
        );
    }

    public static InvalidObservabilityQueryException invalidInterval(Long interval) {
        return new InvalidObservabilityQueryException(
            "'interval' must be a positive number of milliseconds, was " + interval,
            "observability.query.invalid_interval"
        );
    }

    public static InvalidObservabilityQueryException missingMetrics() {
        return new InvalidObservabilityQueryException("At least one metric is required", "observability.query.metrics_required");
    }

    public static InvalidObservabilityQueryException nullEntry(String field) {
        return new InvalidObservabilityQueryException("'" + field + "' must not contain null entries", "observability.query.null_entry");
    }

    public static InvalidObservabilityQueryException unknownMetric(String metric) {
        return new InvalidObservabilityQueryException("Metric '" + metric + "' is not supported", "observability.query.unknown_metric");
    }

    public static InvalidObservabilityQueryException unknownMeasure(String metric, String measure) {
        return new InvalidObservabilityQueryException(
            "Measure '" + measure + "' is not supported for metric '" + metric + "'",
            "observability.query.unknown_measure"
        );
    }

    public static InvalidObservabilityQueryException unknownFacet(String facet) {
        return new InvalidObservabilityQueryException("Facet '" + facet + "' is not supported", "observability.query.unknown_facet");
    }

    public static InvalidObservabilityQueryException unknownSortOrder(String order) {
        return new InvalidObservabilityQueryException(
            "Sort order '" + order + "' is not supported, expected ASC or DESC",
            "observability.query.unknown_sort_order"
        );
    }

    private InvalidObservabilityQueryException(String message, String technicalCode) {
        super(message, technicalCode);
    }
}
