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

import io.gravitee.gamma.authorization.domain.AuthzPolicy;
import io.gravitee.gamma.authorization.domain.AuthzPolicyKind;
import io.gravitee.gamma.authorization.paging.Pageable;
import io.gravitee.gamma.authorization.paging.PagedResult;
import io.gravitee.gamma.authorization.service.AuthzPolicyFilter;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

public interface AuthzPolicyRepository {
    AuthzPolicy save(AuthzPolicy policy);

    Optional<AuthzPolicy> findById(String environmentId, String id);

    List<AuthzPolicy> findAll(String environmentId);

    List<AuthzPolicy> findByKind(String environmentId, AuthzPolicyKind kind);

    List<AuthzPolicy> findByEntityId(String environmentId, String entityId);

    default List<AuthzPolicy> findByEntityIds(String environmentId, Collection<String> entityIds) {
        if (entityIds == null || entityIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<AuthzPolicy> merged = new LinkedHashSet<>();
        for (String entityId : entityIds) {
            merged.addAll(findByEntityId(environmentId, entityId));
        }
        return List.copyOf(merged);
    }

    boolean deleteById(String environmentId, String id);

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

    default PagedResult<AuthzPolicy> findPage(String environmentId, AuthzPolicyFilter filter, Pageable pageable) {
        AuthzPolicyFilter f = filter == null ? AuthzPolicyFilter.none() : filter;
        List<AuthzPolicy> base;
        if (f.entityId() != null) {
            base = findByEntityId(environmentId, f.entityId());
        } else if (f.kind() != null) {
            base = findByKind(environmentId, f.kind());
        } else {
            base = findAll(environmentId);
        }
        List<AuthzPolicy> matching = base
            .stream()
            .filter(p -> f.kind() == null || p.kind() == f.kind())
            .filter(p -> f.status() == null || p.status() == f.status())
            .toList();
        return PagedResult.of(matching, pageable);
    }
}
