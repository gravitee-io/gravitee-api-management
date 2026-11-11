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
package io.gravitee.repository.noop.management;

import io.gravitee.common.data.domain.Page;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.SecretRepository;
import io.gravitee.repository.management.api.search.Pageable;
import io.gravitee.repository.management.model.Secret;
import java.util.List;
import java.util.Optional;

public class NoOpSecretRepository implements SecretRepository {

    @Override
    public Secret create(Secret secret) throws TechnicalException {
        return null;
    }

    @Override
    public Optional<Secret> findById(String organizationId, String environmentId, String id) throws TechnicalException {
        return Optional.empty();
    }

    @Override
    public Optional<Secret> findByKey(String organizationId, String environmentId, String key) throws TechnicalException {
        return Optional.empty();
    }

    @Override
    public Secret update(Secret secret) throws TechnicalException {
        return null;
    }

    @Override
    public void delete(String organizationId, String environmentId, String id) throws TechnicalException {}

    @Override
    public Page<Secret> search(String organizationId, String environmentId, Pageable pageable) throws TechnicalException {
        return new Page<>(List.of(), 0, 0, 0L);
    }

    @Override
    public void deleteByEnvironmentId(String environmentId) throws TechnicalException {}

    @Override
    public boolean existsByOrganizationId(String organizationId) throws TechnicalException {
        return false;
    }
}
