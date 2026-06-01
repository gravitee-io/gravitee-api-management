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

/**
 * Sort/seek mode for {@code SubscriptionRepository.searchAfter}, resolved from the {@link Sortable}
 * field. The mode drives <b>both</b> the {@code order by} and the keyset seek predicate, so the two
 * always agree: a sort that does not match its seek silently skips or duplicates rows across pages.
 *
 * <ul>
 *   <li>{@link #UPDATED_AT} — {@code (updatedAt, id)}, the delta-sync path. Pair with
 *       {@link SubscriptionCursor#byUpdatedAt(long, String)}.</li>
 *   <li>{@link #PLAN_ID} — {@code (plan, id)}, the warmup path served by the {@code {plan,id}}
 *       index. Pair with {@link SubscriptionCursor#byPlanAndId(String, String)}.</li>
 *   <li>{@link #ID} — {@code id} only, the legacy / repository-bridge fallback used when no plan
 *       marker is available (e.g. an older client). Pair with {@link SubscriptionCursor#byId(String)}.</li>
 * </ul>
 */
public enum SubscriptionSearchSort {
    UPDATED_AT,
    PLAN_ID,
    ID;

    /**
     * Resolve the sort mode from a {@link Sortable}. A {@code null} sortable (or {@code null} field)
     * defaults to {@link #UPDATED_AT} to preserve the delta-sync default.
     *
     * @throws IllegalArgumentException if the field is not one of {@code updatedAt}, {@code plan} or {@code id}
     */
    public static SubscriptionSearchSort fromSortable(Sortable sortable) {
        if (sortable == null || sortable.field() == null || "updatedAt".equals(sortable.field())) {
            return UPDATED_AT;
        }
        return switch (sortable.field()) {
            case "plan" -> PLAN_ID;
            case "id" -> ID;
            default -> throw new IllegalArgumentException(
                "searchAfter supports sort field 'updatedAt', 'plan' or 'id' only, got: " + sortable.field()
            );
        };
    }
}
