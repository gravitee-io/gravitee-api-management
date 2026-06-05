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

import io.gravitee.authz.engine.parser.AuthzParseException;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.gamma.authorization.api.AuthzSchemaAdminApi;
import io.gravitee.gamma.authorization.api.AuthzSchemaRepository;
import io.gravitee.gamma.authorization.service.exception.AuthzInvalidArgumentException;
import java.util.List;
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
    public String parsedSchema(String environmentId) {
        Objects.requireNonNull(environmentId, "environmentId must not be null");
        String text = getSchema(environmentId).orElse("");
        if (text.isBlank()) {
            return "{}";
        }
        try {
            return AuthzSchemaParsing.toJson(text);
        } catch (IllegalArgumentException | IllegalStateException | AuthzParseException e) {
            // Legacy/externally-written schemas may predate save-time validation; don't 500 the read path.
            return "{}";
        }
    }

    @Override
    public List<String> validate(String schemaText) {
        Objects.requireNonNull(schemaText, "schemaText must not be null");
        return AuthzSchemaParsing.validate(schemaText);
    }

    @Override
    public void saveSchema(String environmentId, String schemaText) {
        Objects.requireNonNull(environmentId, "environmentId must not be null");
        Objects.requireNonNull(schemaText, "schemaText must not be null");
        List<String> errors = AuthzSchemaParsing.validate(schemaText);
        if (!errors.isEmpty()) {
            throw new AuthzInvalidArgumentException(String.join("; ", errors));
        }
        schemaRepository.save(environmentId, schemaText, TimeProvider.instantNow());
    }

    @Override
    public boolean deleteSchema(String environmentId) {
        Objects.requireNonNull(environmentId, "environmentId must not be null");
        return schemaRepository.delete(environmentId);
    }
}
