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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.gravitee.gamma.authorization.repository.InMemoryAuthzSchemaRepository;
import io.gravitee.gamma.authorization.service.exception.AuthzInvalidArgumentException;
import org.junit.jupiter.api.Test;

class AuthzSchemaServiceImplTest {

    @Test
    void getSchema_is_empty_when_nothing_stored() {
        AuthzSchemaServiceImpl service = new AuthzSchemaServiceImpl(new InMemoryAuthzSchemaRepository());
        assertThat(service.getSchema("env-1")).isEmpty();
    }

    @Test
    void saveSchema_then_getSchema_returns_it() {
        AuthzSchemaServiceImpl service = new AuthzSchemaServiceImpl(new InMemoryAuthzSchemaRepository());
        service.saveSchema("env-1", "entity Edited {};");
        assertThat(service.getSchema("env-1")).contains("entity Edited {};");
    }

    @Test
    void deleteSchema_removes_it() {
        AuthzSchemaServiceImpl service = new AuthzSchemaServiceImpl(new InMemoryAuthzSchemaRepository());
        service.saveSchema("env-1", "entity Edited {};");
        assertThat(service.deleteSchema("env-1")).isTrue();
        assertThat(service.getSchema("env-1")).isEmpty();
    }

    @Test
    void deleteSchema_returns_false_when_nothing_stored() {
        AuthzSchemaServiceImpl service = new AuthzSchemaServiceImpl(new InMemoryAuthzSchemaRepository());
        assertThat(service.deleteSchema("env-1")).isFalse();
    }

    @Test
    void getSchema_rejects_null_environmentId() {
        AuthzSchemaServiceImpl service = new AuthzSchemaServiceImpl(new InMemoryAuthzSchemaRepository());
        assertThatThrownBy(() -> service.getSchema(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void saveSchema_rejects_null_environmentId() {
        AuthzSchemaServiceImpl service = new AuthzSchemaServiceImpl(new InMemoryAuthzSchemaRepository());
        assertThatThrownBy(() -> service.saveSchema(null, "entity X {};")).isInstanceOf(NullPointerException.class);
    }

    @Test
    void saveSchema_rejects_null_schemaText() {
        AuthzSchemaServiceImpl service = new AuthzSchemaServiceImpl(new InMemoryAuthzSchemaRepository());
        assertThatThrownBy(() -> service.saveSchema("env-1", null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void deleteSchema_rejects_null_environmentId() {
        AuthzSchemaServiceImpl service = new AuthzSchemaServiceImpl(new InMemoryAuthzSchemaRepository());
        assertThatThrownBy(() -> service.deleteSchema(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void parsedSchema_is_empty_object_when_nothing_stored() {
        AuthzSchemaServiceImpl service = new AuthzSchemaServiceImpl(new InMemoryAuthzSchemaRepository());
        assertThat(service.parsedSchema("env-1")).isEqualTo("{}");
    }

    @Test
    void parsedSchema_returns_engine_json_after_save() {
        AuthzSchemaServiceImpl service = new AuthzSchemaServiceImpl(new InMemoryAuthzSchemaRepository());
        service.saveSchema("env-1", "entity User {};");
        assertThat(service.parsedSchema("env-1")).contains("entityTypes").contains("User");
    }

    @Test
    void validate_returns_empty_for_valid_schema() {
        AuthzSchemaServiceImpl service = new AuthzSchemaServiceImpl(new InMemoryAuthzSchemaRepository());
        assertThat(service.validate("entity X {};")).isEmpty();
    }

    @Test
    void validate_returns_errors_for_invalid_schema() {
        AuthzSchemaServiceImpl service = new AuthzSchemaServiceImpl(new InMemoryAuthzSchemaRepository());
        assertThat(service.validate("@@@ not gapl")).isNotEmpty();
    }

    @Test
    void saveSchema_rejects_invalid_schema_and_stores_nothing() {
        AuthzSchemaServiceImpl service = new AuthzSchemaServiceImpl(new InMemoryAuthzSchemaRepository());
        assertThatThrownBy(() -> service.saveSchema("env-1", "@@@ not gapl"))
            .isInstanceOf(AuthzInvalidArgumentException.class);
        assertThat(service.getSchema("env-1")).isEmpty();
    }

    @Test
    void parsedSchema_returns_empty_object_when_stored_schema_is_unparseable() {
        InMemoryAuthzSchemaRepository repository = new InMemoryAuthzSchemaRepository();
        // Seed directly to bypass save-time validation, emulating legacy/externally-written data.
        repository.save("env-1", "@@@ not valid gapl", java.time.Instant.now());
        AuthzSchemaServiceImpl service = new AuthzSchemaServiceImpl(repository);
        assertThat(service.parsedSchema("env-1")).isEqualTo("{}");
    }
}
