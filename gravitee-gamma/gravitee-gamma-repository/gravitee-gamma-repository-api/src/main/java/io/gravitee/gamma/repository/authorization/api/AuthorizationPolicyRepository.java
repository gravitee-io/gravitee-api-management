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
package io.gravitee.gamma.repository.authorization.api;

import io.gravitee.gamma.repository.api.CrudRepository;
import io.gravitee.gamma.repository.authorization.model.AuthorizationPolicy;
import io.gravitee.gamma.repository.authorization.model.AuthorizationPolicyKind;
import io.gravitee.gamma.repository.authorization.model.AuthorizationPolicyStatus;
import io.gravitee.gamma.repository.exceptions.TechnicalException;
import io.gravitee.gamma.repository.paging.Pageable;
import io.gravitee.gamma.repository.paging.PagedResult;
import java.util.List;
import java.util.Optional;

public interface AuthorizationPolicyRepository extends CrudRepository<AuthorizationPolicy, String> {
    Optional<AuthorizationPolicy> findByEnvironmentIdAndId(String environmentId, String id) throws TechnicalException;

    List<AuthorizationPolicy> findAllByEnvironmentId(String environmentId) throws TechnicalException;

    List<AuthorizationPolicy> findAllByEnvironmentIdAndKind(String environmentId, AuthorizationPolicyKind kind) throws TechnicalException;

    List<AuthorizationPolicy> findAllByEnvironmentIdAndEntityId(String environmentId, String entityId) throws TechnicalException;

    long deleteByEnvironmentIdAndId(String environmentId, String id) throws TechnicalException;

    default PagedResult<AuthorizationPolicy> findPage(
        String environmentId,
        AuthorizationPolicyKind kind,
        String entityId,
        AuthorizationPolicyStatus status,
        Pageable pageable
    ) throws TechnicalException {
        List<AuthorizationPolicy> base;
        if (entityId != null) {
            base = findAllByEnvironmentIdAndEntityId(environmentId, entityId);
        } else if (kind != null) {
            base = findAllByEnvironmentIdAndKind(environmentId, kind);
        } else {
            base = findAllByEnvironmentId(environmentId);
        }
        List<AuthorizationPolicy> matching = base
            .stream()
            .filter(p -> kind == null || p.getKind() == kind)
            .filter(p -> status == null || p.getStatus() == status)
            .toList();
        return PagedResult.of(matching, pageable);
    }
}
