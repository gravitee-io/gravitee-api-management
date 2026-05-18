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
package io.gravitee.gamma.authorization.service;

import io.gravitee.gamma.repository.authorization.model.AuthorizationPolicyKind;
import io.gravitee.gamma.repository.authorization.model.AuthorizationPolicyStatus;

/**
 * Filters for paginated policy listings.
 *
 * <p>All fields are optional — null means "any value matches". Matches the
 * shape of {@link EntityFilter} so REST resources can render both lists
 * with the same pattern.
 *
 * <p>{@code status} is included here even though no current repo method
 * filters by it; the platform port keeps the filter container narrow and
 * the REST layer applies the status filter client-side after fetching.
 * Promote to a repo-level filter the day there's a query plan for it.
 */
public record PolicyFilter(AuthorizationPolicyKind kind, String entityId, AuthorizationPolicyStatus status) {
    public static PolicyFilter none() {
        return new PolicyFilter(null, null, null);
    }
}
