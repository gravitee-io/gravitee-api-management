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
 * Position marker for keyset / seek pagination over subscriptions. Three seek modes are supported by
 * {@code SubscriptionRepository.searchAfter} depending on the sort field passed alongside (see
 * {@link SubscriptionSearchSort}); the cursor's shape must match that field:
 *
 * <ul>
 *   <li>{@code (updatedAt, id)} — used when the {@code Sortable} field is {@code "updatedAt"} (the
 *       delta sync path). Construct with {@link #byUpdatedAt(long, String)}.</li>
 *   <li>{@code (plan, id)} — used when the {@code Sortable} field is {@code "plan"} (the warmup path
 *       where a {@code plans IN} filter dominates selectivity). Construct with
 *       {@link #byPlanAndId(String, String)}. Sorting/seeking by {@code (plan, _id)} lets the
 *       {@code {plan:1,_id:1}} index scan each plan's range in order without an N-way merge, instead
 *       of an {@code _id}-ordered collection walk that examines unrelated rows.</li>
 *   <li>{@code id} only — used when the {@code Sortable} field is {@code "id"} (legacy / repository
 *       bridge fallback when no plan marker is available). Construct with {@link #byId(String)}.</li>
 * </ul>
 *
 * <p>Use the static factories rather than the canonical constructor to make the seek mode explicit
 * at the call site.
 */
public record SubscriptionCursor(long updatedAt, String id, String plan) {
    public SubscriptionCursor {
        Objects.requireNonNull(id, "SubscriptionCursor.id must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("SubscriptionCursor.id must not be blank");
        }
    }

    /** Cursor for {@code (updatedAt, id)} keyset seek — delta sync path. */
    public static SubscriptionCursor byUpdatedAt(long updatedAt, String id) {
        return new SubscriptionCursor(updatedAt, id, null);
    }

    /** Cursor for {@code (plan, id)} keyset seek — warmup path served by the {@code {plan,_id}} index. */
    public static SubscriptionCursor byPlanAndId(String plan, String id) {
        return new SubscriptionCursor(0L, id, plan);
    }

    /** Cursor for {@code id}-only keyset seek — fallback when no plan marker is available
     *  (e.g. the repository bridge receiving a cursor from an older client). */
    public static SubscriptionCursor byId(String id) {
        return new SubscriptionCursor(0L, id, null);
    }
}
