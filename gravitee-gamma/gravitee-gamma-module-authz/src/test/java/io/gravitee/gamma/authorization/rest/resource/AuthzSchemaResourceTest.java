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
package io.gravitee.gamma.authorization.rest.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.gamma.authorization.rest.dto.AuthzSchemaRequest;
import io.gravitee.gamma.authorization.rest.dto.AuthzSchemaResponse;
import io.gravitee.gamma.authorization.rest.dto.SchemaValidationResponse;
import io.gravitee.gamma.authorization.service.exception.AuthzInvalidArgumentException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AuthzSchemaResourceTest extends AbstractAuthorizationResourceTest {

    private static final String ENV = "test-env";

    @Test
    void get_schema_returns_payload_from_service() {
        when(schemaService.getSchema(ENV)).thenReturn(Optional.of("entity Api {\n  owner: String\n}\n"));

        try (Response response = target("/schema").request().get()) {
            assertThat(response.getStatus()).isEqualTo(200);
            AuthzSchemaResponse body = response.readEntity(AuthzSchemaResponse.class);
            assertThat(body.schema()).contains("entity Api {").contains("owner: String");
        }
    }

    @Test
    void get_schema_when_nothing_stored_returns_empty_string() {
        when(schemaService.getSchema(ENV)).thenReturn(Optional.empty());

        try (Response response = target("/schema").request().get()) {
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.readEntity(AuthzSchemaResponse.class).schema()).isEmpty();
        }
    }

    @Test
    void put_saves_schema_and_returns_200() {
        when(schemaService.getSchema(any())).thenReturn(java.util.Optional.of("entity Edited {};"));
        try (Response response = target("/schema").request().put(jakarta.ws.rs.client.Entity.json(new AuthzSchemaRequest("entity Edited {};")))) {
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.readEntity(AuthzSchemaResponse.class).schema()).isEqualTo("entity Edited {};");
        }
        verify(schemaService).saveSchema(any(), eq("entity Edited {};"));
    }

    @Test
    void delete_removes_schema_and_returns_204() {
        when(schemaService.deleteSchema(any())).thenReturn(true);
        try (Response response = target("/schema").request().delete()) {
            assertThat(response.getStatus()).isEqualTo(204);
        }
        verify(schemaService).deleteSchema(any());
    }

    @Test
    void put_with_null_schema_returns_400() {
        try (
            Response response = target("/schema")
                .request()
                .put(jakarta.ws.rs.client.Entity.json(new AuthzSchemaRequest(null)))
        ) {
            assertThat(response.getStatus()).isEqualTo(400);
        }
    }

    @Test
    void put_with_invalid_schema_returns_400_and_does_not_persist() {
        doThrow(new AuthzInvalidArgumentException("line 1:7 extraneous input '{'"))
            .when(schemaService)
            .saveSchema(any(), eq("entity { broken"));

        try (Response response = target("/schema").request().put(Entity.json(new AuthzSchemaRequest("entity { broken")))) {
            assertThat(response.getStatus()).isEqualTo(400);
        }
    }

    @Test
    void get_parsed_schema_returns_engine_json_from_service() {
        when(schemaService.parsedSchema(ENV)).thenReturn("{\"\":{\"entityTypes\":{\"User\":{}}}}");

        try (Response response = target("/schema/parsed").request().get()) {
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.readEntity(String.class)).contains("entityTypes").contains("User");
        }
    }

    @Test
    void post_validate_returns_valid_true_when_no_errors() {
        when(schemaService.validate("entity X {};")).thenReturn(List.of());

        try (Response response = target("/schema/validate").request().post(Entity.json(new AuthzSchemaRequest("entity X {};")))) {
            assertThat(response.getStatus()).isEqualTo(200);
            SchemaValidationResponse body = response.readEntity(SchemaValidationResponse.class);
            assertThat(body.valid()).isTrue();
            assertThat(body.errors()).isEmpty();
        }
    }

    @Test
    void post_validate_returns_errors_when_invalid() {
        when(schemaService.validate("bad")).thenReturn(List.of("boom"));

        try (Response response = target("/schema/validate").request().post(Entity.json(new AuthzSchemaRequest("bad")))) {
            assertThat(response.getStatus()).isEqualTo(200);
            SchemaValidationResponse body = response.readEntity(SchemaValidationResponse.class);
            assertThat(body.valid()).isFalse();
            assertThat(body.errors()).containsExactly("boom");
        }
    }
}
