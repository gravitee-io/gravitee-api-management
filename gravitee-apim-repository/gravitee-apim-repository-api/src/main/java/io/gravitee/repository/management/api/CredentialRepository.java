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
package io.gravitee.repository.management.api;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Credential;
import java.util.Optional;

public interface CredentialRepository {
    Credential create(Credential credential) throws TechnicalException;

    Optional<Credential> findById(String organizationId, String environmentId, String id) throws TechnicalException;

    /**
     * Changes every field but the scope, the strategy and the creation date.
     *
     * @throws TechnicalException when no credential has this id in this organization and environment
     */
    Credential update(Credential credential) throws TechnicalException;

    void delete(String organizationId, String environmentId, String id) throws TechnicalException;

    /**
     * Sorted by name, with the first page as page 0.
     *
     * @param query matched as literal text anywhere in the name, ignoring case; {@code null} matches every credential
     */
    Page<Credential> search(String organizationId, String environmentId, String query, Pageable pageable) throws TechnicalException;

    void deleteByEnvironmentId(String environmentId) throws TechnicalException;
}
