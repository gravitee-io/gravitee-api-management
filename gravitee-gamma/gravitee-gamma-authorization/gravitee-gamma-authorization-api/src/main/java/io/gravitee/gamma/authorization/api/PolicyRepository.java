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

import io.gravitee.gamma.authorization.domain.Policy;
import io.gravitee.gamma.authorization.domain.PolicyKind;
import io.gravitee.gamma.authorization.service.PolicyFilter;
import io.gravitee.gamma.repository.paging.Pageable;
import io.gravitee.gamma.repository.paging.PagedResult;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

public interface PolicyRepository {
    Policy save(Policy policy);

    Optional<Policy> findById(String environmentId, String id);

    List<Policy> findAll(String environmentId);

    List<Policy> findByKind(String environmentId, PolicyKind kind);

    List<Policy> findByEntityId(String environmentId, String entityId);

    /**
     * Batch lookup of policies attached to any of the given entityIds.
     * Used by cascade-delete to fetch all impacted policies in one query.
     * Empty/null collection yields an empty list.
     */
    default List<Policy> findByEntityIds(String environmentId, Collection<String> entityIds) {
        if (entityIds == null || entityIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Policy> merged = new LinkedHashSet<>();
        for (String entityId : entityIds) {
            merged.addAll(findByEntityId(environmentId, entityId));
        }
        return List.copyOf(merged);
    }

    boolean deleteById(String environmentId, String id);

    /**
     * Batch delete by id. Returns the number of deleted policies.
     * Empty/null collection deletes nothing and returns {@code 0}.
     */
    default long deleteByIds(String environmentId, Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0L;
        }
        long deleted = 0L;
        for (String id : ids) {
            if (deleteById(environmentId, id)) {
                deleted++;
            }
        }
        return deleted;
    }

    /**
     * Paginated lookup of policies matching {@code filter}.
     *
     * <p>Default implementation loads via the existing {@code findAll}
     * / {@code findByKind} / {@code findByEntityId} methods, applies the
     * remaining filter fields in-memory, then slices the page. Mongo
     * adapter overrides with native {@code skip/limit + count}.
     */
    default PagedResult<Policy> findPage(String environmentId, PolicyFilter filter, Pageable pageable) {
        PolicyFilter f = filter == null ? PolicyFilter.none() : filter;
        List<Policy> base;
        if (f.entityId() != null) {
            base = findByEntityId(environmentId, f.entityId());
        } else if (f.kind() != null) {
            base = findByKind(environmentId, f.kind());
        } else {
            base = findAll(environmentId);
        }
        List<Policy> matching = base
            .stream()
            .filter(p -> f.kind() == null || p.kind() == f.kind())
            .filter(p -> f.status() == null || p.status() == f.status())
            .toList();
        return PagedResult.of(matching, pageable);
    }
}
