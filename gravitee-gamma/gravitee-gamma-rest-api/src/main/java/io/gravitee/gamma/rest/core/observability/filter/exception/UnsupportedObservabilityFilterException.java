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
package io.gravitee.gamma.rest.core.observability.filter.exception;

import io.gravitee.apim.core.exception.ValidationDomainException;
import io.gravitee.gamma.rest.core.observability.filter.model.Signal;

/**
 * Raised when an observability query receives a {@code FilterCondition} that references an unknown
 * filter name or an unsupported operator. Maps to HTTP 400 via the apim
 * {@code ValidationDomainExceptionMapper}.
 *
 * @author GraviteeSource Team
 */
public class UnsupportedObservabilityFilterException extends ValidationDomainException {

    public static UnsupportedObservabilityFilterException unknownName(String name) {
        return new UnsupportedObservabilityFilterException(
            "Filter '" + name + "' is not supported by this backend",
            "observability.filter.unknown_name"
        );
    }

    public static UnsupportedObservabilityFilterException unsupportedOperator(String filterName, String operator) {
        return new UnsupportedObservabilityFilterException(
            "Operator '" + operator + "' is not supported yet for filter '" + filterName + "'",
            "observability.filter.unsupported_operator"
        );
    }

    public static UnsupportedObservabilityFilterException signalMismatch(String filterName, Signal requiredSignal) {
        return new UnsupportedObservabilityFilterException(
            "Filter '" + filterName + "' does not apply to signal " + requiredSignal,
            "observability.filter.signal_mismatch"
        );
    }

    public static UnsupportedObservabilityFilterException blankValue(String filterName) {
        return new UnsupportedObservabilityFilterException(
            "Filter '" + filterName + "' requires a non-blank value",
            "observability.filter.blank_value"
        );
    }

    public static UnsupportedObservabilityFilterException valueListingNotSupported(String filterName, String type) {
        return new UnsupportedObservabilityFilterException(
            "Filter '" + filterName + "' of type " + type + " does not support value listing",
            "observability.filter.value_listing_not_supported"
        );
    }

    public static UnsupportedObservabilityFilterException searchTranslationNotSupported(String filterName) {
        return new UnsupportedObservabilityFilterException(
            "Filter '" + filterName + "' is not yet supported for log search",
            "observability.filter.search_translation_not_supported"
        );
    }

    private UnsupportedObservabilityFilterException(String message, String technicalCode) {
        super(message, technicalCode);
    }
}
