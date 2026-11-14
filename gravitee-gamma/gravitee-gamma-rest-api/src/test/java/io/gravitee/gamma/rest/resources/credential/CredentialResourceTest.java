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
import static io.gravitee.common.http.HttpStatusCode.CONFLICT_409;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.NO_CONTENT_204;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static jakarta.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.credential.model.CredentialConsumer;
import io.gravitee.gamma.rest.resources.credential.dto.CredentialDto;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CredentialResourceTest extends AbstractCredentialResourceTest {

    private CredentialDto givenACredential() {
        return given(bearer("OpenAI"));
    }

    private CredentialDto givenAManagedCredential() {
        return given(managedBearer("Anthropic", "sk-ant-123"));
    }

    @Nested
    class GetCredential {

        @Test
        void should_return_the_credential() {
            var credential = givenACredential();

            var response = rootTarget(credential.id()).request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);
            assertThat(response.readEntity(CredentialDto.class)).isEqualTo(credential);
        }

        @Test
        void should_return_the_display_name_of_the_owner() {
            var credential = givenACredential();
            givenTheCallerIsNamed("Ada", "Lovelace");

            var response = rootTarget(credential.id()).request().get();

            assertThat(response.readEntity(CredentialDto.class).ownerName()).isEqualTo("Ada Lovelace");
        }

        @Test
        void should_never_return_the_value_of_a_managed_credential() {
            var credential = givenAManagedCredential();

            var response = rootTarget(credential.id()).request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);
            assertThat(response.readEntity(String.class)).doesNotContain("sk-ant-123");
        }

        @Test
        void should_tell_whether_the_caller_can_manage_the_credential() {
            var credential = givenACredential();

            var response = rootTarget(credential.id()).request().get();

            assertThat(response.readEntity(JsonNode.class).get("manageable").asBoolean()).isTrue();
        }

        @Test
        void should_return_404_for_an_unknown_credential() {
            var response = rootTarget("unknown").request().get();

            assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);
        }

        @Test
        void should_return_403_without_the_read_permission() {
            var credential = givenACredential();

            shouldReturn403(RolePermission.ENVIRONMENT_CREDENTIAL, RolePermissionAction.READ, () ->
                rootTarget(credential.id()).request().get()
            );
        }
    }

    @Nested
    class UpdateCredential {

        @Test
        void should_update_the_credential() {
            var credential = givenACredential();

            var response = rootTarget(credential.id())
                .request()
                .put(json(Map.of("name", "OpenAI production", "references", Map.of("token", "/vault/secret/openai:prod"))));

            assertThat(response.getStatus()).isEqualTo(OK_200);
            var updated = response.readEntity(CredentialDto.class);
            assertThat(updated.name()).isEqualTo("OpenAI production");
            assertThat(updated.secretReferences().get("token").reference()).isEqualTo("/vault/secret/openai:prod");
        }

        @Test
        void should_keep_the_references_of_a_managed_credential_when_none_are_given() {
            var credential = givenAManagedCredential();

            var response = rootTarget(credential.id()).request().put(json(Map.of("name", "Anthropic production")));

            assertThat(response.getStatus()).isEqualTo(OK_200);
            var updated = response.readEntity(CredentialDto.class);
            assertThat(updated.managed()).isTrue();
            assertThat(updated.secretReferences()).isEqualTo(credential.secretReferences());
        }

        @Test
        void should_keep_the_references_of_a_managed_credential_when_the_ones_read_are_sent_back() {
            var credential = givenAManagedCredential();
            var references = Map.of("token", credential.secretReferences().get("token").reference());

            var response = rootTarget(credential.id())
                .request()
                .put(json(Map.of("name", "Anthropic production", "references", references)));

            assertThat(response.getStatus()).isEqualTo(OK_200);
            assertThat(response.readEntity(CredentialDto.class).secretReferences()).isEqualTo(credential.secretReferences());
        }

        @Test
        void should_refuse_a_missing_name() {
            var credential = givenACredential();

            var response = rootTarget(credential.id()).request().put(json(Map.of()));

            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        }

        @Test
        void should_return_403_without_the_update_permission() {
            var credential = givenACredential();

            shouldReturn403(RolePermission.ENVIRONMENT_CREDENTIAL, RolePermissionAction.UPDATE, () ->
                rootTarget(credential.id()).request().put(json(Map.of("name", "OpenAI production")))
            );
        }
    }

    @Nested
    class RotateCredential {

        @Test
        void should_rotate_the_values_without_returning_them() {
            var credential = givenAManagedCredential();
            auditCrudService.reset();

            var response = rootTarget(credential.id())
                .path("_rotate")
                .request()
                .post(json(Map.of("values", Map.of("token", "sk-ant-456"))));

            assertThat(response.getStatus()).isEqualTo(OK_200);
            response.bufferEntity();
            assertThat(response.readEntity(String.class)).doesNotContain("sk-ant-456");
            assertThat(response.readEntity(CredentialDto.class).rotatedAt()).isNotNull();
            assertThat(auditCrudService.storage())
                .extracting(AuditEntity::getPatch)
                .noneMatch(patch -> patch != null && patch.contains("sk-ant-456"));
        }

        @Test
        void should_refuse_to_rotate_a_credential_that_references_its_secrets() {
            var credential = givenACredential();

            var response = rootTarget(credential.id()).path("_rotate").request().post(json(Map.of("values", Map.of("token", "sk-new"))));

            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        }

        @Test
        void should_refuse_a_rotation_without_values() {
            var credential = givenAManagedCredential();

            var response = rootTarget(credential.id()).path("_rotate").request().post(json(Map.of()));

            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        }

        @Test
        void should_return_403_without_the_update_permission() {
            var credential = givenAManagedCredential();

            shouldReturn403(RolePermission.ENVIRONMENT_CREDENTIAL, RolePermissionAction.UPDATE, () ->
                rootTarget(credential.id()).path("_rotate").request().post(json(Map.of("values", Map.of("token", "sk-new"))))
            );
        }
    }

    @Nested
    class DeleteCredential {

        @Test
        void should_delete_the_credential_and_its_secrets() {
            var credential = givenAManagedCredential();

            var response = rootTarget(credential.id()).request().delete();

            assertThat(response.getStatus()).isEqualTo(NO_CONTENT_204);
            assertThat(credentialCrudService.storage()).isEmpty();
            assertThat(secretCrudService.storage()).isEmpty();
        }

        @Test
        void should_refuse_to_delete_a_credential_in_use() {
            var credential = givenACredential();
            credentialConsumers.given(ENV_ID, credential.id(), new CredentialConsumer("aim", "agent", "agent-1", "Support agent"));

            var response = rootTarget(credential.id()).request().delete();

            assertThat(response.getStatus()).isEqualTo(CONFLICT_409);
            assertThat(credentialCrudService.storage()).hasSize(1);
        }

        @Test
        void should_return_403_without_the_delete_permission() {
            var credential = givenACredential();

            shouldReturn403(RolePermission.ENVIRONMENT_CREDENTIAL, RolePermissionAction.DELETE, () ->
                rootTarget(credential.id()).request().delete()
            );
            assertThat(credentialCrudService.storage()).hasSize(1);
        }
    }

    @Nested
    class ListCredentialConsumers {

        @Test
        void should_list_what_every_module_reports_using_the_credential() {
            var credential = givenACredential();
            credentialConsumers.given(ENV_ID, credential.id(), new CredentialConsumer("aim", "agent", "agent-1", "Support agent"));

            var response = rootTarget(credential.id()).path("consumers").request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);
            var consumer = response.readEntity(JsonNode.class).get("data").get(0);
            assertThat(consumer.get("module").asText()).isEqualTo("aim");
            assertThat(consumer.get("type").asText()).isEqualTo("agent");
            assertThat(consumer.get("id").asText()).isEqualTo("agent-1");
            assertThat(consumer.get("name").asText()).isEqualTo("Support agent");
        }

        @Test
        void should_return_403_without_the_read_permission() {
            var credential = givenACredential();

            shouldReturn403(RolePermission.ENVIRONMENT_CREDENTIAL, RolePermissionAction.READ, () ->
                rootTarget(credential.id()).path("consumers").request().get()
            );
        }
    }
}
