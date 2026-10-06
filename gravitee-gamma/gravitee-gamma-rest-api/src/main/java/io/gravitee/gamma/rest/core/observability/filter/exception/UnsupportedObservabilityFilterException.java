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
import java.util.List;

/**
 * Raised when an observability query or dashboard receives a {@code FilterCondition} the filter catalog
 * refuses: an unknown name, a filter outside the signal, an unadvertised operator, a malformed or
 * out-of-range value, the wrong number of values, or a repeated filter. Maps to HTTP 400 via the apim
 * {@code ValidationDomainExceptionMapper}, with one technical code per rule.
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

    public static UnsupportedObservabilityFilterException unknownEnumValue(String filterName, String value) {
        return new UnsupportedObservabilityFilterException(
            "Value '" + value + "' is not a valid value for filter '" + filterName + "'",
            "observability.filter.unknown_enum_value"
        );
    }

    public static UnsupportedObservabilityFilterException blankValue(String filterName) {
        return new UnsupportedObservabilityFilterException(
            "Filter '" + filterName + "' requires a non-blank value",
            "observability.filter.blank_value"
        );
    }

    public static UnsupportedObservabilityFilterException invalidValueShape(String filterName) {
        return new UnsupportedObservabilityFilterException(
            "Filter '" + filterName + "' takes a scalar or an array of scalars as value, with no null element",
            "observability.filter.invalid_value_shape"
        );
    }

    public static UnsupportedObservabilityFilterException invalidArity(String filterName, String operator, List<String> values) {
        return new UnsupportedObservabilityFilterException(
            "Operator '" +
                operator +
                "' on filter '" +
                filterName +
                "' takes exactly one value, got " +
                values.size() +
                " values " +
                values +
                ". Use IN to match several values.",
            "observability.filter.invalid_arity"
        );
    }

    public static UnsupportedObservabilityFilterException invalidNumber(String filterName, String value) {
        return new UnsupportedObservabilityFilterException(
            "Value '" + value + "' is not a whole number, as filter '" + filterName + "' requires",
            "observability.filter.invalid_number"
        );
    }

    public static UnsupportedObservabilityFilterException valueOutOfRange(String filterName, String value, Number min, Number max) {
        return new UnsupportedObservabilityFilterException(
            "Value '" + value + "' is outside the range [" + min + ", " + max + "] of filter '" + filterName + "'",
            "observability.filter.value_out_of_range"
        );
    }

    public static UnsupportedObservabilityFilterException invertedRange(String filterName, long lower, long upper) {
        return new UnsupportedObservabilityFilterException(
            "Filter '" + filterName + "' asks for values >= " + lower + " and <= " + upper + ", which nothing matches",
            "observability.filter.inverted_range"
        );
    }

    public static UnsupportedObservabilityFilterException repeated(String filterName, List<String> operators) {
        return new UnsupportedObservabilityFilterException(
            "Filter '" +
                filterName +
                "' appears " +
                operators.size() +
                " times " +
                operators +
                ". Send one condition per filter, or one GTE and one LTE for a numeric range.",
            "observability.filter.repeated"
        );
    }

    public static UnsupportedObservabilityFilterException valueListingNotSupported(String filterName, String type) {
        return new UnsupportedObservabilityFilterException(
            "Filter '" + filterName + "' of type " + type + " does not support value listing",
            "observability.filter.value_listing_not_supported"
        );
    }

    public static UnsupportedObservabilityFilterException valuesPageOutOfRange(int page, int max) {
        return new UnsupportedObservabilityFilterException(
            "page must be between 1 and " + max + ", got " + page,
            "observability.filter.values_page_out_of_range"
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
