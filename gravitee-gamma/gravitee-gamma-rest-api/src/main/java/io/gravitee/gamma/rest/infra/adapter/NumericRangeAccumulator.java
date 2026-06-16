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

import io.gravitee.apim.core.exception.ValidationDomainException;

/**
 * Accumulates inclusive GTE/LTE bounds for a single numeric range, then validates
 * and returns the result. Has no knowledge of filter operators or domain concepts.
 *
 * @param <T> numeric type ({@link Integer}, {@link Long}, etc.)
 */
final class NumericRangeAccumulator<T extends Comparable<T>> {

    record OpenBounds<T>(T gte, T lte) {}

    private final String filterName;
    private T gte;
    private T lte;

    NumericRangeAccumulator(String filterName) {
        this.filterName = filterName;
    }

    void setGte(T value) {
        this.gte = value;
    }

    void setLte(T value) {
        this.lte = value;
    }

    void setBoth(T value) {
        this.gte = value;
        this.lte = value;
    }

    boolean hasValue() {
        return gte != null || lte != null;
    }

    OpenBounds<T> build() {
        if (gte != null && lte != null && gte.compareTo(lte) > 0) {
            throw new ValidationDomainException(
                "Invalid " + filterName + " range: 'gte' (" + gte + ") must not be greater than 'lte' (" + lte + ")."
            );
        }
        return new OpenBounds<>(gte, lte);
    }
}
