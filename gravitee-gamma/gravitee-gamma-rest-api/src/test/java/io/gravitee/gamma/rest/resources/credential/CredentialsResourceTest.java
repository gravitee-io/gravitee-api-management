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
package io.gravitee.gamma.rest.resources.credential;

import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.credential.model.CredentialStrategy;
import io.gravitee.gamma.rest.resources.credential.dto.CredentialDto;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import jakarta.ws.rs.client.WebTarget;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CredentialsResourceTest extends AbstractCredentialResourceTest {

    @Nested
    class CreateCredential {

        @Test
        void should_create_a_credential_that_references_its_secrets() {
            var response = create(bearer("OpenAI"));

            assertThat(response.getStatus()).isEqualTo(CREATED_201);
            var created = response.readEntity(CredentialDto.class);
            assertThat(created.name()).isEqualTo("OpenAI");
            assertThat(created.strategy()).isEqualTo(CredentialStrategy.BEARER);
            assertThat(created.ownerId()).isEqualTo(USER_NAME);
            assertThat(created.managed()).isFalse();
            assertThat(created.secretReferences().get("token").reference()).isEqualTo("/vault/secret/openai:token");
            assertThat(response.getLocation().toString()).endsWith("/credentials/" + created.id());
            assertThat(credentialCrudService.storage())
                .extracting(c -> c.id())
                .containsExactly(created.id());
        }

        @Test
        void should_write_timestamps_as_iso_8601_strings() {
            var response = create(bearer("OpenAI"));

            var body = response.readEntity(JsonNode.class);
            assertThat(body.get("createdAt").isTextual()).isTrue();
            assertThat(body.get("createdAt").asText()).endsWith("Z");
        }

        @Test
        void should_store_typed_values_as_secrets_and_never_return_them() {
            var response = create(managedBearer("Anthropic", "sk-ant-123"));

            assertThat(response.getStatus()).isEqualTo(CREATED_201);
            response.bufferEntity();
            assertThat(response.readEntity(String.class)).doesNotContain("sk-ant-123");
            var created = response.readEntity(CredentialDto.class);
            assertThat(created.managed()).isTrue();
            assertThat(created.secretReferences().get("token").managed()).isTrue();
            assertThat(secretCrudService.storage()).hasSize(1);
            assertThat(auditCrudService.storage())
                .extracting(AuditEntity::getPatch)
                .noneMatch(patch -> patch != null && patch.contains("sk-ant-123"));
        }

        @Test
        void should_refuse_an_invalid_credential() {
            var response = create(Map.of("name", "OpenAI", "strategy", "BEARER"));

            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
            assertThat(credentialCrudService.storage()).isEmpty();
        }

        @Test
        void should_refuse_a_missing_strategy() {
            var response = create(Map.of("name", "OpenAI", "references", Map.of("token", "/vault/secret/openai:token")));

            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
            assertThat(credentialCrudService.storage()).isEmpty();
        }

        @Test
        void should_refuse_an_unknown_property() {
            var payload = new HashMap<>(bearer("OpenAI"));
            payload.put("environmentId", "other-env");

            var response = create(payload);

            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
            assertThat(credentialCrudService.storage()).isEmpty();
        }

        @Test
        void should_refuse_typed_values_without_the_secret_create_permission() {
            withoutPermission(RolePermission.ENVIRONMENT_SECRET, RolePermissionAction.CREATE);

            var response = create(managedBearer("Anthropic", "sk-ant-123"));

            assertThat(response.getStatus()).isEqualTo(FORBIDDEN_403);
            assertThat(credentialCrudService.storage()).isEmpty();
            assertThat(secretCrudService.storage()).isEmpty();
        }

        @Test
        void should_return_403_without_the_create_permission() {
            shouldReturn403(RolePermission.ENVIRONMENT_CREDENTIAL, RolePermissionAction.CREATE, () -> create(bearer("OpenAI")));
            assertThat(credentialCrudService.storage()).isEmpty();
        }
    }

    @Nested
    class ListCredentials {

        @Test
        void should_page_the_credentials_of_the_environment_sorted_by_name() {
            given(bearer("Cohere"));
            given(bearer("Anthropic"));
            given(bearer("Bedrock"));

            var response = rootTarget().queryParam("page", 1).queryParam("perPage", 2).request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);
            var body = response.readEntity(JsonNode.class);
            assertThat(names(body)).containsExactly("Anthropic", "Bedrock");
            assertThat(body.get("pagination").get("page").asInt()).isEqualTo(1);
            assertThat(body.get("pagination").get("totalCount").asLong()).isEqualTo(3);
            assertThat(body.get("pagination").get("pageCount").asInt()).isEqualTo(2);
        }

        @Test
        void should_filter_on_the_name_the_strategies_and_the_service() {
            given(bearer("OpenAI production"));
            given(
                Map.of(
                    "name",
                    "Anthropic",
                    "strategy",
                    "API_KEY",
                    "serviceId",
                    "anthropic",
                    "configuration",
                    Map.of("headerName", "x-api-key"),
                    "references",
                    Map.of("value", "/vault/secret/anthropic:key")
                )
            );

            assertThat(names(rootTarget().queryParam("q", " openai "))).containsExactly("OpenAI production");
            assertThat(names(rootTarget().queryParam("strategy", "API_KEY"))).containsExactly("Anthropic");
            assertThat(names(rootTarget().queryParam("strategy", "API_KEY,BEARER"))).containsExactly("Anthropic", "OpenAI production");
            assertThat(names(rootTarget().queryParam("strategy", "API_KEY").queryParam("strategy", "BEARER"))).containsExactly(
                "Anthropic",
                "OpenAI production"
            );
            assertThat(names(rootTarget().queryParam("serviceId", "openai"))).containsExactly("OpenAI production");
            assertThat(names(rootTarget().queryParam("strategy", "BASIC").queryParam("serviceId", "anthropic"))).isEmpty();
        }

        @Test
        void should_never_return_the_value_of_a_managed_credential() {
            given(managedBearer("Anthropic", "sk-ant-123"));

            var response = rootTarget().request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);
            assertThat(response.readEntity(String.class)).doesNotContain("sk-ant-123");
        }

        @Test
        void should_refuse_an_unknown_strategy() {
            var response = rootTarget().queryParam("strategy", "DIGEST").request().get();

            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        }

        @Test
        void should_refuse_a_page_of_more_than_100_credentials() {
            var response = rootTarget().queryParam("perPage", 101).request().get();

            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        }

        @Test
        void should_refuse_a_page_before_the_first() {
            var response = rootTarget().queryParam("page", 0).request().get();

            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        }

        @Test
        void should_refuse_a_page_whose_offset_overflows() {
            var response = rootTarget().queryParam("page", 21474838).queryParam("perPage", 100).request().get();

            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        }

        @Test
        void should_return_403_without_the_read_permission() {
            shouldReturn403(RolePermission.ENVIRONMENT_CREDENTIAL, RolePermissionAction.READ, () -> rootTarget().request().get());
        }

        private List<String> names(WebTarget target) {
            var response = target.request().get();
            assertThat(response.getStatus()).isEqualTo(OK_200);
            return names(response.readEntity(JsonNode.class));
        }

        private List<String> names(JsonNode body) {
            return StreamSupport.stream(body.get("data").spliterator(), false)
                .map(c -> c.get("name").asText())
                .toList();
        }
    }
}
