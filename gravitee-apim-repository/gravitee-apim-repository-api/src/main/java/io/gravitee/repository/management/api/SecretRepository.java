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
import io.gravitee.repository.management.model.Secret;
import java.util.Optional;

public interface SecretRepository {
    Secret create(Secret secret) throws TechnicalException;

    Optional<Secret> findById(String organizationId, String environmentId, String id) throws TechnicalException;

    Optional<Secret> findByKey(String organizationId, String environmentId, String key) throws TechnicalException;

    Secret update(Secret secret) throws TechnicalException;

    void delete(String organizationId, String environmentId, String id) throws TechnicalException;

    Page<Secret> search(String organizationId, String environmentId, Pageable pageable) throws TechnicalException;

    void deleteByEnvironmentId(String environmentId) throws TechnicalException;
}
