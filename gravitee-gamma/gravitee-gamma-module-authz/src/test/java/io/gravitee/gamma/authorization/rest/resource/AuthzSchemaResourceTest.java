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
import static org.mockito.Mockito.when;

import io.gravitee.gamma.authorization.rest.dto.AuthzSchemaResponse;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

class AuthzSchemaResourceTest extends AbstractAuthorizationResourceTest {

    private static final String ENV = "test-env";

    @Test
    void get_schema_returns_payload_from_service() {
        when(schemaService.currentGaplSchema(ENV)).thenReturn("entity Api {\n  owner: String\n}\n");

        try (Response response = target("/schema").request().get()) {
            assertThat(response.getStatus()).isEqualTo(200);
            AuthzSchemaResponse body = response.readEntity(AuthzSchemaResponse.class);
            assertThat(body.schema()).contains("entity Api {").contains("owner: String");
        }
    }

    @Test
    void get_schema_when_environment_empty_returns_placeholder() {
        when(schemaService.currentGaplSchema(ENV)).thenReturn("// No entities or policies defined yet.\n");

        try (Response response = target("/schema").request().get()) {
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.readEntity(AuthzSchemaResponse.class).schema()).isEqualTo("// No entities or policies defined yet.\n");
        }
    }
}
