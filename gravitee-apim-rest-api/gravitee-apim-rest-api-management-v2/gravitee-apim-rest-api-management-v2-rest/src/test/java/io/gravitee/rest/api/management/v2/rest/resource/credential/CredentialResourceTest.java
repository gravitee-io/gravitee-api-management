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
package io.gravitee.rest.api.management.v2.rest.resource.credential;

import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.CONFLICT_409;
import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.NO_CONTENT_204;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static jakarta.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

import inmemory.AuditCrudServiceInMemory;
import inmemory.CredentialConsumersInMemory;
import inmemory.CredentialCrudServiceInMemory;
import inmemory.SecretCrudServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.apim.core.credential.model.CredentialConsumer;
import io.gravitee.rest.api.management.v2.rest.model.CreateCredential;
import io.gravitee.rest.api.management.v2.rest.model.Credential;
import io.gravitee.rest.api.management.v2.rest.model.CredentialConsumersResponse;
import io.gravitee.rest.api.management.v2.rest.model.CredentialStrategy;
import io.gravitee.rest.api.management.v2.rest.model.RotateCredential;
import io.gravitee.rest.api.management.v2.rest.model.UpdateCredential;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CredentialResourceTest extends AbstractResourceTest {

    private static final String ENV_ID = "my-env";

    @Inject
    private CredentialCrudServiceInMemory credentialCrudService;

    @Inject
    private CredentialConsumersInMemory credentialConsumers;

    @Inject
    private SecretCrudServiceInMemory secretCrudService;

    @Inject
    private AuditCrudServiceInMemory auditCrudService;

    @Override
    protected String contextPath() {
        return "/environments/" + ENV_ID + "/credentials";
    }

    @BeforeEach
    void init() {
        super.setUp();
        GraviteeContext.cleanContext();

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(ENV_ID);
        environmentEntity.setOrganizationId(ORGANIZATION);
        when(environmentService.findById(ENV_ID)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENV_ID)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENV_ID);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        credentialCrudService.reset();
        credentialConsumers.reset();
        secretCrudService.reset();
        auditCrudService.reset();
    }

    private Credential givenACredential() {
        return given(
            new CreateCredential()
                .name("OpenAI")
                .strategy(CredentialStrategy.BEARER)
                .serviceId("openai")
                .references(Map.of("token", "/vault/secret/openai:token"))
        );
    }

    private Credential givenAManagedCredential() {
        return given(new CreateCredential().name("Anthropic").strategy(CredentialStrategy.BEARER).values(Map.of("token", "sk-ant-123")));
    }

    private Credential given(CreateCredential createCredential) {
        var response = rootTarget().request().post(json(createCredential));
        assertThat(response.getStatus()).isEqualTo(CREATED_201);
        return response.readEntity(Credential.class);
    }

    @Nested
    class GetCredentialTest {

        @Test
        void should_return_the_credential() {
            var credential = givenACredential();

            var response = rootTarget(credential.getId()).request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);
            assertThat(response.readEntity(Credential.class)).isEqualTo(credential);
        }

        @Test
        void should_never_return_the_value_of_a_managed_credential() {
            var credential = givenAManagedCredential();

            var response = rootTarget(credential.getId()).request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);
            assertThat(response.readEntity(String.class)).doesNotContain("sk-ant-123");
        }

        @Test
        void should_return_404_for_an_unknown_credential() {
            var response = rootTarget("unknown").request().get();

            assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);
        }

        @Test
        void should_return_403_without_the_read_permission() {
            var credential = givenACredential();

            shouldReturn403(RolePermission.ENVIRONMENT_CREDENTIAL, ENV_ID, RolePermissionAction.READ, () ->
                rootTarget(credential.getId()).request().get()
            );
        }
    }

    @Nested
    class UpdateCredentialTest {

        @Test
        void should_update_the_credential() {
            var credential = givenACredential();

            var response = rootTarget(credential.getId())
                .request()
                .put(json(new UpdateCredential().name("OpenAI production").references(Map.of("token", "/vault/secret/openai:prod"))));

            assertThat(response.getStatus()).isEqualTo(OK_200);
            var updated = response.readEntity(Credential.class);
            assertThat(updated.getName()).isEqualTo("OpenAI production");
            assertThat(updated.getSecretReferences().get("token").getReference()).isEqualTo("/vault/secret/openai:prod");
        }

        @Test
        void should_keep_the_references_of_a_managed_credential_when_none_are_given() {
            var credential = givenAManagedCredential();

            var response = rootTarget(credential.getId()).request().put(json(new UpdateCredential().name("Anthropic production")));

            assertThat(response.getStatus()).isEqualTo(OK_200);
            var updated = response.readEntity(Credential.class);
            assertThat(updated.getManaged()).isTrue();
            assertThat(updated.getSecretReferences()).isEqualTo(credential.getSecretReferences());
        }

        @Test
        void should_refuse_a_missing_name() {
            var credential = givenACredential();

            var response = rootTarget(credential.getId()).request().put(json(new UpdateCredential()));

            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        }

        @Test
        void should_return_403_without_the_update_permission() {
            var credential = givenACredential();

            shouldReturn403(RolePermission.ENVIRONMENT_CREDENTIAL, ENV_ID, RolePermissionAction.UPDATE, () ->
                rootTarget(credential.getId()).request().put(json(new UpdateCredential().name("OpenAI production")))
            );
        }
    }

    @Nested
    class RotateCredentialTest {

        @Test
        void should_rotate_the_values_without_returning_them() {
            var credential = givenAManagedCredential();
            auditCrudService.reset();

            var response = rootTarget(credential.getId())
                .path("_rotate")
                .request()
                .post(json(new RotateCredential().values(Map.of("token", "sk-ant-456"))));

            assertThat(response.getStatus()).isEqualTo(OK_200);
            response.bufferEntity();
            assertThat(response.readEntity(String.class)).doesNotContain("sk-ant-456");
            assertThat(response.readEntity(Credential.class).getRotatedAt()).isNotNull();
            assertThat(auditCrudService.storage())
                .extracting(AuditEntity::getPatch)
                .noneMatch(patch -> patch != null && patch.contains("sk-ant-456"));
        }

        @Test
        void should_refuse_to_rotate_a_credential_that_references_its_secrets() {
            var credential = givenACredential();

            var response = rootTarget(credential.getId())
                .path("_rotate")
                .request()
                .post(json(new RotateCredential().values(Map.of("token", "sk-new"))));

            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        }

        @Test
        void should_return_403_without_the_update_permission() {
            var credential = givenAManagedCredential();

            shouldReturn403(RolePermission.ENVIRONMENT_CREDENTIAL, ENV_ID, RolePermissionAction.UPDATE, () ->
                rootTarget(credential.getId())
                    .path("_rotate")
                    .request()
                    .post(json(new RotateCredential().values(Map.of("token", "sk-new"))))
            );
        }
    }

    @Nested
    class DeleteCredentialTest {

        @Test
        void should_delete_the_credential_and_its_secrets() {
            var credential = givenAManagedCredential();

            var response = rootTarget(credential.getId()).request().delete();

            assertThat(response.getStatus()).isEqualTo(NO_CONTENT_204);
            assertThat(credentialCrudService.storage()).isEmpty();
            assertThat(secretCrudService.storage()).isEmpty();
        }

        @Test
        void should_refuse_to_delete_a_credential_in_use() {
            var credential = givenACredential();
            credentialConsumers.given(ENV_ID, credential.getId(), new CredentialConsumer("aim", "agent", "agent-1", "Support agent"));

            var response = rootTarget(credential.getId()).request().delete();

            assertThat(response.getStatus()).isEqualTo(CONFLICT_409);
            assertThat(credentialCrudService.storage()).hasSize(1);
        }

        @Test
        void should_return_403_without_the_delete_permission() {
            var credential = givenACredential();

            shouldReturn403(RolePermission.ENVIRONMENT_CREDENTIAL, ENV_ID, RolePermissionAction.DELETE, () ->
                rootTarget(credential.getId()).request().delete()
            );
        }
    }

    @Nested
    class ListCredentialConsumersTest {

        @Test
        void should_list_what_every_module_reports_using_the_credential() {
            var credential = givenACredential();
            credentialConsumers.given(ENV_ID, credential.getId(), new CredentialConsumer("aim", "agent", "agent-1", "Support agent"));

            var response = rootTarget(credential.getId()).path("consumers").request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);
            assertThat(response.readEntity(CredentialConsumersResponse.class).getData())
                .extracting("module", "type", "id", "name")
                .containsExactly(tuple("aim", "agent", "agent-1", "Support agent"));
        }

        @Test
        void should_return_403_without_the_read_permission() {
            var credential = givenACredential();

            shouldReturn403(RolePermission.ENVIRONMENT_CREDENTIAL, ENV_ID, RolePermissionAction.READ, () ->
                rootTarget(credential.getId()).path("consumers").request().get()
            );
        }
    }
}
