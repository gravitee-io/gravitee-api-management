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
import io.gravitee.gamma.authorization.domain.EntityKind;
import io.gravitee.gamma.authorization.service.EntityFilter;
import io.gravitee.gamma.repository.paging.Pageable;
import io.gravitee.gamma.repository.paging.PagedResult;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

public interface EntityRepository {
    Entity save(Entity entity);

    Optional<Entity> findById(String environmentId, String id);

    Optional<Entity> findByEntityId(String environmentId, String entityId);

    List<Entity> findAll(String environmentId);

    List<Entity> findByKind(String environmentId, EntityKind kind);

    List<Entity> findByEntityIdPrefix(String environmentId, String prefix);

    /**
     * Batch lookup by entityId in a single round-trip when supported. Order
     * of returned entities is not guaranteed; missing entityIds are simply
     * omitted. Empty/null collection yields an empty list.
     */
    default List<Entity> findByEntityIds(String environmentId, Collection<String> entityIds) {
        if (entityIds == null || entityIds.isEmpty()) {
            return List.of();
        }
        List<Entity> result = new java.util.ArrayList<>(entityIds.size());
        for (String id : entityIds) {
            findByEntityId(environmentId, id).ifPresent(result::add);
        }
        return result;
    }

    /**
     * Batch lookup by entityId prefix: returns entities whose entityId starts
     * with any of the given prefixes. Used by cascade computation to merge
     * disjoint prefix scans into one query. Empty/null collection yields an
     * empty list.
     */
    default List<Entity> findByAnyEntityIdPrefix(String environmentId, Collection<String> prefixes) {
        if (prefixes == null || prefixes.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Entity> merged = new LinkedHashSet<>();
        for (String prefix : prefixes) {
            merged.addAll(findByEntityIdPrefix(environmentId, prefix));
        }
        return List.copyOf(merged);
    }

    boolean deleteById(String environmentId, String id);

    boolean deleteByEntityId(String environmentId, String entityId);

    /**
     * Batch delete by entityId. Returns the number of deleted entities.
     * Empty/null collection deletes nothing and returns {@code 0}.
     */
    default long deleteByEntityIds(String environmentId, Collection<String> entityIds) {
        if (entityIds == null || entityIds.isEmpty()) {
            return 0L;
        }
        long deleted = 0L;
        for (String entityId : entityIds) {
            if (deleteByEntityId(environmentId, entityId)) {
                deleted++;
            }
        }
        return deleted;
    }

    /**
     * Paginated lookup of entities matching {@code filter} within the env.
     *
     * <p>The default implementation loads every matching entity into memory
     * via {@code findAll} / {@code findByEntityIdPrefix}, applies the
     * in-process filter, then slices the page client-side. That's fine for
     * adapters where filtering is cheap (in-memory test doubles, etc) but
     * production stores should override with native {@code skip/limit +
     * count} so the database does the work — see
     * {@code MongoEntityRepository}.
     */
    default PagedResult<Entity> findPage(String environmentId, EntityFilter filter, Pageable pageable) {
        EntityFilter f = filter == null ? EntityFilter.none() : filter;
        List<Entity> base = f.entityIdPrefix() != null ? findByEntityIdPrefix(environmentId, f.entityIdPrefix()) : findAll(environmentId);
        List<Entity> matching = base
            .stream()
            .filter(e -> f.kind() == null || e.kind() == f.kind())
            .filter(e -> f.source() == null || f.source().equals(e.source()))
            .toList();
        return PagedResult.of(matching, pageable);
    }
}
