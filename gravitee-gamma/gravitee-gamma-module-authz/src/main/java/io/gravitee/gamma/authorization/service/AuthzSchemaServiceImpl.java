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
package io.gravitee.gamma.authorization.service;

import io.gravitee.common.utils.TimeProvider;
import io.gravitee.gamma.authorization.api.AuthzSchemaAdminApi;
import io.gravitee.gamma.authorization.api.AuthzSchemaRepository;
import java.util.Objects;
import java.util.Optional;

public class AuthzSchemaServiceImpl implements AuthzSchemaAdminApi {

    private final AuthzSchemaRepository schemaRepository;

    public AuthzSchemaServiceImpl(AuthzSchemaRepository schemaRepository) {
        this.schemaRepository = Objects.requireNonNull(schemaRepository, "schemaRepository must not be null");
    }

    @Override
    public Optional<String> getSchema(String environmentId) {
        Objects.requireNonNull(environmentId, "environmentId must not be null");
        return schemaRepository.find(environmentId);
    }

    @Override
    public void saveSchema(String environmentId, String schemaText) {
        Objects.requireNonNull(environmentId, "environmentId must not be null");
        Objects.requireNonNull(schemaText, "schemaText must not be null");
        schemaRepository.save(environmentId, schemaText, TimeProvider.instantNow());
    }

    @Override
    public boolean deleteSchema(String environmentId) {
        Objects.requireNonNull(environmentId, "environmentId must not be null");
        return schemaRepository.delete(environmentId);
    }
}
