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
package io.gravitee.gamma.rest.core.observability.filter.model;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The union of all host-owned filter names: {@link StaticFilters} ∪ {@link ExtensibleFilters}.
 *
 * <p>Java enums can't be unioned into a third enum, so this is a small aggregator rather than an
 * enum. Its job is the **collision guard**: the registry rejects any module-contributed filter
 * whose name is host-owned (a module must not redefine {@code API}, {@code API_TYPE}, …).
 *
 * @author GraviteeSource Team
 */
public final class CommonFilters {

    private CommonFilters() {}

    /** Names owned by the host (static + extensible) — modules cannot ship a filter with these names. */
    public static Set<String> names() {
        return Stream.concat(
            Arrays.stream(StaticFilters.values()).map(StaticFilters::filterName),
            Arrays.stream(ExtensibleFilters.values()).map(ExtensibleFilters::filterName)
        ).collect(Collectors.toUnmodifiableSet());
    }
}
