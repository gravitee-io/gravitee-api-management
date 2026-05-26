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
package io.gravitee.repository.management.api.search;

import java.util.Objects;

/**
 * Position marker for keyset / seek pagination over api keys. Two seek modes are supported by
 * {@code ApiKeyRepository.searchAfter} depending on the sort field passed alongside:
 *
 * <ul>
 *   <li>{@code (updatedAt, id)} — used when the {@code Sortable} field is {@code "updatedAt"} (the
 *       delta sync path). Construct with {@link #byUpdatedAt(long, String)}.</li>
 *   <li>{@code id}-only — used when the {@code Sortable} field is {@code "id"} (the warmup path
 *       where a {@code subscriptions IN} filter dominates selectivity). Construct with
 *       {@link #byId(String)}; the {@code updatedAt} component is ignored by the seek.</li>
 * </ul>
 *
 * <p>Use the static factories rather than the canonical constructor to make the seek mode explicit
 * at the call site.
 */
public record ApiKeyCursor(long updatedAt, String id) {
    public ApiKeyCursor {
        Objects.requireNonNull(id, "ApiKeyCursor.id must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("ApiKeyCursor.id must not be blank");
        }
    }

    /** Cursor for {@code (updatedAt, id)} keyset seek — delta sync path. */
    public static ApiKeyCursor byUpdatedAt(long updatedAt, String id) {
        return new ApiKeyCursor(updatedAt, id);
    }

    /** Cursor for {@code id}-only keyset seek — warmup path. */
    public static ApiKeyCursor byId(String id) {
        return new ApiKeyCursor(0L, id);
    }
}
