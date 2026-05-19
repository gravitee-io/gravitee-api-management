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

import io.gravitee.gamma.authorization.domain.Entity;
import io.gravitee.gamma.authorization.service.CascadeResult;
import io.gravitee.gamma.authorization.service.CreateOrReplaceEntityCommand;
import io.gravitee.gamma.authorization.service.EntityFilter;
import io.gravitee.gamma.authorization.service.UpdateEntityCommand;
import io.gravitee.gamma.authorization.service.UpsertResult;
import io.gravitee.gamma.repository.paging.Pageable;
import io.gravitee.gamma.repository.paging.PagedResult;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EntityAdminApi {
    UpsertResult upsert(AuthzCallerContext caller, CreateOrReplaceEntityCommand command);

    /**
     * Batch variant of {@link #upsert(AuthzCallerContext, CreateOrReplaceEntityCommand)}
     * for hot paths (e.g. API deployment events) that touch N entities for
     * the same environment in one go.
     *
     * <p>Folds the per-entity {@code findByEntityId} into a single {@code $in}
     * lookup and emits a single {@code schemaService.invalidate} at the end;
     * audit entries and entity-upserted events are still produced per entity.
     * All commands must target the caller's environment.
     */
    List<UpsertResult> bulkUpsert(AuthzCallerContext caller, List<CreateOrReplaceEntityCommand> commands);

    Entity update(AuthzCallerContext caller, String entityId, UpdateEntityCommand command);

    Optional<Entity> findByEntityId(String environmentId, String entityId);

    /**
     * Unpaged lookup. Kept for callers that genuinely need every row in
     * one pass — SCIM reconcile is the main one, where the alternative is
     * treating page-2+ entities as orphans on the next sync.
     */
    List<Entity> find(String environmentId, EntityFilter filter);

    /**
     * Paginated lookup of entities matching {@code filter}. Use this for
     * any UI-driven listing; the unpaged {@link #find(String, EntityFilter)}
     * is for whole-env scans.
     */
    PagedResult<Entity> findPage(String environmentId, EntityFilter filter, Pageable pageable);

    Set<String> findApiAliases(String environmentId, String apiId);

    CascadeResult delete(AuthzCallerContext caller, String entityId);
}
