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
package io.gravitee.rest.api.management.v2.rest.resource.secret;

import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.NOT_FOUND_404;
import static io.gravitee.common.http.HttpStatusCode.NO_CONTENT_204;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static jakarta.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import inmemory.AuditCrudServiceInMemory;
import inmemory.SecretCrudServiceInMemory;
import io.gravitee.apim.core.secret.model.Secret;
import io.gravitee.apim.core.secret.service_provider.SecretValueEncryptor;
import io.gravitee.rest.api.management.v2.rest.model.UpdateSecret;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SecretResourceTest extends AbstractResourceTest {

    private static final String ENV_ID = "my-env";
    private static final String SECRET_ID = "secret-1";
    private static final Instant CREATED_AT = Instant.parse("2026-11-06T10:00:00Z");

    @Inject
    private SecretCrudServiceInMemory secretCrudService;

    @Inject
    private AuditCrudServiceInMemory auditCrudService;

    @Inject
    private SecretValueEncryptor secretValueEncryptor;

    @Override
    protected String contextPath() {
        return "/environments/" + ENV_ID + "/secrets/" + SECRET_ID;
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
        secretCrudService.reset();
        auditCrudService.reset();
    }

    private Secret givenTheSecret() {
        return givenTheSecret("anthropic-key");
    }

    private Secret givenTheSecret(String key) {
        var secret = Secret.create(SECRET_ID, ORGANIZATION, ENV_ID, key, CREATED_AT);
        return secretCrudService.create(secret, secretValueEncryptor.encrypt(secret, "sk-ant-123"));
    }

    @Nested
    class GetSecretTest {

        @Test
        void should_get_a_secret() {
            givenTheSecret();

            var response = rootTarget().request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);
            var secret = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.Secret.class);
            assertThat(secret.getId()).isEqualTo(SECRET_ID);
            assertThat(secret.getPath()).isEqualTo("/gravitee/anthropic-key:value");
        }

        @Test
        void should_return_404_for_an_unknown_secret() {
            assertThat(rootTarget().request().get().getStatus()).isEqualTo(NOT_FOUND_404);
        }

        @Test
        void should_return_403_without_the_read_permission() {
            shouldReturn403(RolePermission.ENVIRONMENT_SECRET, ENV_ID, RolePermissionAction.READ, () -> rootTarget().request().get());
        }
    }

    @Nested
    class UpdateSecretTest {

        @Test
        void should_replace_the_value() {
            var secret = givenTheSecret();

            var response = rootTarget().request().put(json(new UpdateSecret().value("new-value")));

            assertThat(response.getStatus()).isEqualTo(OK_200);
            assertThat(response.readEntity(String.class)).doesNotContain("new-value");
            var stored = secretCrudService.encryptedValueOf(SECRET_ID).orElseThrow();
            assertThat(secretValueEncryptor.decrypt(secret, stored)).isEqualTo("new-value");
        }

        @Test
        void should_refuse_to_change_the_value_of_a_credential() {
            var secret = givenTheSecret("credential.cred-1.token");

            var response = rootTarget().request().put(json(new UpdateSecret().value("new-value")));

            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
            var stored = secretCrudService.encryptedValueOf(SECRET_ID).orElseThrow();
            assertThat(secretValueEncryptor.decrypt(secret, stored)).isEqualTo("sk-ant-123");
        }

        @Test
        void should_return_404_for_an_unknown_secret() {
            var response = rootTarget().request().put(json(new UpdateSecret().value("new-value")));

            assertThat(response.getStatus()).isEqualTo(NOT_FOUND_404);
        }

        @Test
        void should_return_403_without_the_update_permission() {
            shouldReturn403(RolePermission.ENVIRONMENT_SECRET, ENV_ID, RolePermissionAction.UPDATE, () ->
                rootTarget().request().put(json(new UpdateSecret().value("new-value")))
            );
        }
    }

    @Nested
    class DeleteSecretTest {

        @Test
        void should_delete_a_secret() {
            givenTheSecret();

            var response = rootTarget().request().delete();

            assertThat(response.getStatus()).isEqualTo(NO_CONTENT_204);
            assertThat(secretCrudService.storage()).isEmpty();
        }

        @Test
        void should_refuse_to_delete_the_value_of_a_credential() {
            givenTheSecret("credential.cred-1.token");

            var response = rootTarget().request().delete();

            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
            assertThat(secretCrudService.storage()).hasSize(1);
        }

        @Test
        void should_return_404_for_an_unknown_secret() {
            assertThat(rootTarget().request().delete().getStatus()).isEqualTo(NOT_FOUND_404);
        }

        @Test
        void should_return_403_without_the_delete_permission() {
            shouldReturn403(RolePermission.ENVIRONMENT_SECRET, ENV_ID, RolePermissionAction.DELETE, () -> rootTarget().request().delete());
        }
    }
}
