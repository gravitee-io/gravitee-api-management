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

import io.gravitee.gamma.authorization.domain.AuthzEntity;
import io.gravitee.gamma.authorization.domain.AuthzEntityKind;
import io.gravitee.gamma.authorization.paging.Pageable;
import io.gravitee.gamma.authorization.paging.PagedResult;
import io.gravitee.gamma.authorization.service.AuthzEntityFilter;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

public interface AuthzEntityRepository {
    AuthzEntity save(AuthzEntity entity);

    default List<AuthzEntity> saveAll(Collection<AuthzEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        List<AuthzEntity> saved = new java.util.ArrayList<>(entities.size());
        for (AuthzEntity entity : entities) {
            saved.add(save(entity));
        }
        return saved;
    }

    Optional<AuthzEntity> findById(String environmentId, String id);

    Optional<AuthzEntity> findByEntityId(String environmentId, String entityId);

    List<AuthzEntity> findAll(String environmentId);

    List<AuthzEntity> findByKind(String environmentId, AuthzEntityKind kind);

    List<AuthzEntity> findByEntityIdPrefix(String environmentId, String prefix);

    default List<AuthzEntity> findByEntityIds(String environmentId, Collection<String> entityIds) {
        if (entityIds == null || entityIds.isEmpty()) {
            return List.of();
        }
        List<AuthzEntity> result = new java.util.ArrayList<>(entityIds.size());
        for (String id : entityIds) {
            findByEntityId(environmentId, id).ifPresent(result::add);
        }
        return result;
    }

    default List<AuthzEntity> findByAnyEntityIdPrefix(String environmentId, Collection<String> prefixes) {
        if (prefixes == null || prefixes.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<AuthzEntity> merged = new LinkedHashSet<>();
        for (String prefix : prefixes) {
            merged.addAll(findByEntityIdPrefix(environmentId, prefix));
        }
        return List.copyOf(merged);
    }

    boolean deleteById(String environmentId, String id);

    boolean deleteByEntityId(String environmentId, String entityId);

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

    default PagedResult<AuthzEntity> findPage(String environmentId, AuthzEntityFilter filter, Pageable pageable) {
        AuthzEntityFilter f = filter == null ? AuthzEntityFilter.none() : filter;
        List<AuthzEntity> base = f.entityIdPrefix() != null
            ? findByEntityIdPrefix(environmentId, f.entityIdPrefix())
            : findAll(environmentId);
        List<AuthzEntity> matching = base
            .stream()
            .filter(e -> f.kind() == null || e.kind() == f.kind())
            .filter(e -> f.source() == null || f.source().equals(e.source()))
            .filter(e -> f.excludeEntityIdPrefix() == null || !e.entityId().startsWith(f.excludeEntityIdPrefix()))
            .toList();
        return PagedResult.of(matching, pageable);
    }
}
