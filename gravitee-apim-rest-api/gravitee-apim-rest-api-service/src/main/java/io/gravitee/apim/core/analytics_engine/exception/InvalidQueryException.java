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
package io.gravitee.apim.core.analytics_engine.exception;

import io.gravitee.apim.core.exception.ValidationDomainException;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class InvalidQueryException extends ValidationDomainException {

    public InvalidQueryException(String message) {
        super(message);
    }

    public static InvalidQueryException forMaximumFacetsExceeded(int allowed, int actual) {
        return new InvalidQueryException("Cannot query for more than " + allowed + " facets, got " + actual);
    }

    public static InvalidQueryException forEmptyFacets() {
        return new InvalidQueryException("Query must provide at least one facet in the by clause");
    }

    public static InvalidQueryException forZeroOrNegativeInterval(Long interval) {
        return new InvalidQueryException("Negative or zero intervals cannot be used in a time series query");
    }

    public static InvalidQueryException forInvalidSort(String measure) {
        return new InvalidQueryException("Trying to sort by " + measure + " but it has not been queried");
    }

    public static InvalidQueryException forIncompatibleFacet(String facet, String metric) {
        return new InvalidQueryException("Facet " + facet + " is not supported for metric " + metric);
    }

    public static InvalidQueryException forForbiddenRanges(String facet) {
        return new InvalidQueryException("Facet " + facet + " does not support ranging");
    }

    public static InvalidQueryException forInvalidTimeRangeBounds() {
        return new InvalidQueryException("Time range upper bound must be greater than lower bound");
    }

    public static InvalidQueryException forInvalidMeasure(String metric, String measure) {
        return new InvalidQueryException("Measure " + measure + " is not supported for metric " + metric);
    }

    public static InvalidQueryException forUnknownAPIType(String apiType) {
        return new InvalidQueryException("Unknown API type " + apiType);
    }
}
