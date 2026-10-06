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
package io.gravitee.gamma.rest.resources.observability.dto;

import io.gravitee.gamma.rest.core.observability.exception.InvalidObservabilityQueryException;
import java.util.List;
import java.util.function.Function;

/**
 * Maps the entries of a request array to the core model. A JSON {@code null} entry deserializes to a null
 * element, which the mapping would turn into a 500: it is refused with a 400 naming the array.
 *
 * @author GraviteeSource Team
 */
public final class RequestEntries {

    private RequestEntries() {}

    public static <T, R> List<R> map(List<T> entries, String field, Function<T, R> mapper) {
        if (entries == null) {
            return List.of();
        }
        return entries
            .stream()
            .map(entry -> {
                if (entry == null) {
                    throw InvalidObservabilityQueryException.nullEntry(field);
                }
                return mapper.apply(entry);
            })
            .toList();
    }
}
