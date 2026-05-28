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
package io.gravitee.gamma.rest.core.tracing.exception;

import io.gravitee.apim.core.exception.ValidationDomainException;

/**
 * Raised when the trace search endpoint receives a {@code FilterCondition} the slim translator
 * doesn't handle yet — either an unknown filter name (the spec wasn't ever exposed by the discovery
 * endpoint) or an operator that's listed for the filter but not yet wired through to ES.
 *
 * <p>Maps to HTTP 400 via the apim {@code ValidationDomainExceptionMapper} already registered in
 * {@code GammaModuleApplication}. The {@code technicalCode} lets a UI distinguish "unknown filter"
 * (probably a stale UI / new field) from "unsupported operator" (UI raced ahead of the backend's
 * operator support).
 *
 * @author GraviteeSource Team
 */
public class UnsupportedFilterException extends ValidationDomainException {

    public static UnsupportedFilterException unknownName(String name) {
        return new UnsupportedFilterException("Filter '" + name + "' is not supported by this backend", "tracing.filter.unknown_name");
    }

    public static UnsupportedFilterException unsupportedOperator(String filterName, String operator) {
        return new UnsupportedFilterException(
            "Operator '" + operator + "' is not supported yet for filter '" + filterName + "'",
            "tracing.filter.unsupported_operator"
        );
    }

    /**
     * Raised when the values endpoint is called for a filter whose type doesn't support value
     * listing — today {@code NUMBER} / {@code STRING} / {@code BOOLEAN}. The
     * {@code technicalCode} lets a UI distinguish "the filter exists but has no value pool to
     * paginate" from the unknown-name and unsupported-operator cases.
     */
    public static UnsupportedFilterException valueListingNotSupported(String filterName, String filterType) {
        return new UnsupportedFilterException(
            "Filter type '" + filterType + "' does not support value listing (filter '" + filterName + "')",
            "tracing.filter.value_listing_not_supported"
        );
    }

    private UnsupportedFilterException(String message, String technicalCode) {
        super(message, technicalCode);
    }
}
