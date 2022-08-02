/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.jupiter.core.condition;

import io.gravitee.gateway.jupiter.api.context.HttpExecutionContext;
import io.reactivex.Maybe;
import java.util.Arrays;
import java.util.List;

/**
 * Composite {@link ConditionFilter} composed of multiple {@link ConditionFilter} allowing to filter elements that are not passing all the given filters.
 *
 * @author Jeoffrey HAEYAERT (jeoffrey.HAEYAERT at graviteesource.com)
 * @author GraviteeSource Team
 */
public class CompositeConditionFilter<T> implements ConditionFilter<T> {

    private final List<ConditionFilter<T>> filters;

    @SafeVarargs
    public CompositeConditionFilter(ConditionFilter<T>... filters) {
        this.filters = Arrays.asList(filters);
    }

    @Override
    public Maybe<T> filter(HttpExecutionContext ctx, T elt) {
        Maybe<T> filtered = Maybe.just(elt);

        for (ConditionFilter<T> filter : filters) {
            filtered = filtered.flatMap(filteredElt -> filter.filter(ctx, filteredElt));
        }

        return filtered;
    }
}
