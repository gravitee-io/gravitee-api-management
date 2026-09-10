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
package io.gravitee.gamma.rest.core.observability.logs.model;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * Which documents a logs search selects by entrypoint: everything but the excluded ids when the caller sent
 * no {@code ENTRYPOINT} condition (a default that keeps unknown entrypoints and documents without an
 * entrypoint id), or exactly the values the caller sent.
 *
 * @param ids the excluded ids, or the exact values, where {@code StaticFilters.NO_ENTRYPOINT_VALUE} stands
 *            for documents written without an entrypoint id
 */
public record EntrypointScope(Kind kind, List<String> ids) {
    public EntrypointScope {
        Objects.requireNonNull(kind, "An entrypoint scope needs a kind");
        ids = List.copyOf(ids);
    }

    public enum Kind {
        EXCLUDING,
        EXACTLY,
    }

    public static EntrypointScope excluding(Collection<String> ids) {
        return new EntrypointScope(Kind.EXCLUDING, List.copyOf(ids));
    }

    public static EntrypointScope exactly(Collection<String> values) {
        return new EntrypointScope(Kind.EXACTLY, List.copyOf(values));
    }
}
