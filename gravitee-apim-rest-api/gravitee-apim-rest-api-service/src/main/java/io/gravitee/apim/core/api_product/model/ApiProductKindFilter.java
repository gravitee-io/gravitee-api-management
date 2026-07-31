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
package io.gravitee.apim.core.api_product.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Which kinds of API Product a listing wants. A classic API Product carries no kind, so {@code includeClassic}
 * is a separate flag rather than a value in {@code kinds}.
 */
public record ApiProductKindFilter(Set<ApiProductKind> kinds, boolean includeClassic) {
    public ApiProductKindFilter {
        kinds = kinds == null ? Set.of() : Set.copyOf(kinds);
    }

    public static ApiProductKindFilter any() {
        return new ApiProductKindFilter(EnumSet.allOf(ApiProductKind.class), true);
    }

    public static ApiProductKindFilter classicOnly() {
        return new ApiProductKindFilter(Set.of(), true);
    }

    public boolean matches(ApiProduct apiProduct) {
        return apiProduct.getKind() == null ? includeClassic : kinds.contains(apiProduct.getKind());
    }

    /**
     * The specialized kinds a store-backed search must hide: every kind not explicitly included. A new
     * {@link ApiProductKind} is therefore excluded from a classic listing until it is opted in.
     */
    public Set<ApiProductKind> excludedKinds() {
        var excluded = EnumSet.allOf(ApiProductKind.class);
        excluded.removeAll(kinds);
        return excluded;
    }
}
