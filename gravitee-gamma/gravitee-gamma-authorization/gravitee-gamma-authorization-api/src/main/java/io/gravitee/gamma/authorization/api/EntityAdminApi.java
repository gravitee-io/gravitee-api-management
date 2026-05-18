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
package io.gravitee.gamma.authorization.api;

import io.gravitee.gamma.authorization.service.CascadeResult;
import io.gravitee.gamma.authorization.service.CreateOrReplaceEntityCommand;
import io.gravitee.gamma.authorization.service.EntityFilter;
import io.gravitee.gamma.authorization.service.Pageable;
import io.gravitee.gamma.authorization.service.PagedResult;
import io.gravitee.gamma.authorization.service.UpdateEntityCommand;
import io.gravitee.gamma.authorization.service.UpsertResult;
import io.gravitee.gamma.repository.authorization.model.AuthorizationEntity;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EntityAdminApi {
    UpsertResult upsert(AuthzCallerContext caller, CreateOrReplaceEntityCommand command);

    AuthorizationEntity update(AuthzCallerContext caller, String entityId, UpdateEntityCommand command);

    Optional<AuthorizationEntity> findByEntityId(String environmentId, String entityId);

    /**
     * Unpaged lookup. Kept for callers that genuinely need every row in
     * one pass — SCIM reconcile is the main one, where the alternative is
     * treating page-2+ entities as orphans on the next sync.
     */
    List<AuthorizationEntity> find(String environmentId, EntityFilter filter);

    /**
     * Paginated lookup of entities matching {@code filter}. Use this for
     * any UI-driven listing; the unpaged {@link #find(String, EntityFilter)}
     * is for whole-env scans.
     */
    PagedResult<AuthorizationEntity> findPage(String environmentId, EntityFilter filter, Pageable pageable);

    Set<String> findApiAliases(String environmentId, String apiId);

    CascadeResult delete(AuthzCallerContext caller, String entityId);
}
