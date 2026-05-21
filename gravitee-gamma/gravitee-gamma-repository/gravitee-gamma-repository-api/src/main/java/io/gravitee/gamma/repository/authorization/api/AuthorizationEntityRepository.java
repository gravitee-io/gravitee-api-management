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
import io.gravitee.gamma.repository.authorization.model.AuthorizationEntity;
import io.gravitee.gamma.repository.authorization.model.AuthorizationEntityKind;
import io.gravitee.gamma.repository.exceptions.TechnicalException;
import io.gravitee.gamma.repository.paging.Pageable;
import io.gravitee.gamma.repository.paging.PagedResult;
import java.util.List;
import java.util.Optional;

public interface AuthorizationEntityRepository extends CrudRepository<AuthorizationEntity, String> {
    Optional<AuthorizationEntity> findByEnvironmentIdAndId(String environmentId, String id) throws TechnicalException;

    Optional<AuthorizationEntity> findByEnvironmentIdAndEntityId(String environmentId, String entityId) throws TechnicalException;

    List<AuthorizationEntity> findAllByEnvironmentId(String environmentId) throws TechnicalException;

    List<AuthorizationEntity> findAllByEnvironmentIdAndKind(String environmentId, AuthorizationEntityKind kind) throws TechnicalException;

    List<AuthorizationEntity> findAllByEnvironmentIdAndEntityIdStartingWith(String environmentId, String entityIdPrefix)
        throws TechnicalException;

    long deleteByEnvironmentIdAndId(String environmentId, String id) throws TechnicalException;

    long deleteByEnvironmentIdAndEntityId(String environmentId, String entityId) throws TechnicalException;

    default PagedResult<AuthorizationEntity> findPage(
        String environmentId,
        AuthorizationEntityKind kind,
        String source,
        String entityIdPrefix,
        Pageable pageable
    ) throws TechnicalException {
        List<AuthorizationEntity> base = entityIdPrefix != null
            ? findAllByEnvironmentIdAndEntityIdStartingWith(environmentId, entityIdPrefix)
            : findAllByEnvironmentId(environmentId);
        List<AuthorizationEntity> matching = base
            .stream()
            .filter(e -> kind == null || e.getKind() == kind)
            .filter(e -> source == null || source.equals(e.getSource()))
            .toList();
        return PagedResult.of(matching, pageable);
    }
}
