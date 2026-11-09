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
import static io.gravitee.common.http.HttpStatusCode.CONFLICT_409;
import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static jakarta.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import inmemory.AuditCrudServiceInMemory;
import inmemory.SecretCrudServiceInMemory;
import inmemory.SecretQueryServiceInMemory;
import io.gravitee.apim.core.secret.model.Secret;
import io.gravitee.apim.core.secret.service_provider.SecretValueEncryptor;
import io.gravitee.rest.api.management.v2.rest.model.CreateSecret;
import io.gravitee.rest.api.management.v2.rest.model.SecretsResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SecretsResourceTest extends AbstractResourceTest {

    private static final String ENV_ID = "my-env";
    private static final Instant CREATED_AT = Instant.parse("2026-11-06T10:00:00Z");

    @Inject
    private SecretCrudServiceInMemory secretCrudService;

    @Inject
    private SecretQueryServiceInMemory secretQueryService;

    @Inject
    private AuditCrudServiceInMemory auditCrudService;

    @Inject
    private SecretValueEncryptor secretValueEncryptor;

    @Override
    protected String contextPath() {
        return "/environments/" + ENV_ID + "/secrets";
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
        secretQueryService.reset();
        auditCrudService.reset();
    }

    private Secret givenASecret(String id, String key) {
        var secret = Secret.create(id, ORGANIZATION, ENV_ID, key, CREATED_AT);
        return secretCrudService.create(secret, secretValueEncryptor.encrypt(secret, "sk-ant-123"));
    }

    @Nested
    class CreateSecretTest {

        @Test
        void should_create_a_secret_and_never_return_its_value() {
            var response = rootTarget().request().post(json(new CreateSecret().key("anthropic-key").value("sk-ant-123")));

            assertThat(response.getStatus()).isEqualTo(CREATED_201);
            response.bufferEntity();
            assertThat(response.readEntity(String.class)).doesNotContain("sk-ant-123");
            var created = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.Secret.class);
            assertThat(created.getKey()).isEqualTo("anthropic-key");
            assertThat(created.getPath()).isEqualTo("/gravitee/anthropic-key");

            var stored = secretCrudService.storage().getFirst();
            assertThat(created.getId()).isEqualTo(stored.id());
            assertThat(response.getLocation().toString()).endsWith("/secrets/" + stored.id());
            assertThat(secretCrudService.encryptedValueOf(stored.id()).orElseThrow()).doesNotContain("sk-ant-123");
            assertThat(auditCrudService.storage()).hasSize(1);
        }

        @Test
        void should_refuse_a_key_that_the_environment_already_holds() {
            givenASecret("secret-1", "anthropic-key");

            var response = rootTarget().request().post(json(new CreateSecret().key("anthropic-key").value("sk-ant-123")));

            assertThat(response.getStatus()).isEqualTo(CONFLICT_409);
        }

        @Test
        void should_refuse_an_invalid_key() {
            var response = rootTarget().request().post(json(new CreateSecret().key("with space").value("sk-ant-123")));

            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
            assertThat(secretCrudService.storage()).isEmpty();
        }

        @Test
        void should_refuse_a_missing_value() {
            var response = rootTarget().request().post(json(new CreateSecret().key("anthropic-key")));

            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        }

        @Test
        void should_refuse_an_empty_value() {
            var response = rootTarget().request().post(json(new CreateSecret().key("anthropic-key").value("")));

            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        }

        @Test
        void should_return_403_without_the_create_permission() {
            shouldReturn403(RolePermission.ENVIRONMENT_SECRET, ENV_ID, RolePermissionAction.CREATE, () ->
                rootTarget().request().post(json(new CreateSecret().key("anthropic-key").value("sk-ant-123")))
            );
        }
    }

    @Nested
    class ListSecretsTest {

        @Test
        void should_page_the_secrets_of_the_environment() {
            secretQueryService.initWith(
                List.of(
                    Secret.create("s1", ORGANIZATION, ENV_ID, "mistral-key", CREATED_AT),
                    Secret.create("s2", ORGANIZATION, ENV_ID, "anthropic-key", CREATED_AT),
                    Secret.create("s3", ORGANIZATION, ENV_ID, "cohere-key", CREATED_AT)
                )
            );

            var response = rootTarget().queryParam("page", 1).queryParam("perPage", 2).request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);
            var page = response.readEntity(SecretsResponse.class);
            assertThat(page.getData())
                .extracting(io.gravitee.rest.api.management.v2.rest.model.Secret::getKey)
                .containsExactly("anthropic-key", "cohere-key");
            assertThat(page.getPagination().getTotalCount()).isEqualTo(3);
            assertThat(page.getPagination().getPageCount()).isEqualTo(2);
        }

        @Test
        void should_refuse_a_page_of_more_than_100_secrets() {
            var response = rootTarget().queryParam("perPage", 101).request().get();

            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        }

        @Test
        void should_refuse_a_page_whose_offset_overflows() {
            var response = rootTarget().queryParam("page", 21474838).queryParam("perPage", 100).request().get();

            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        }

        @Test
        void should_return_403_without_the_read_permission() {
            shouldReturn403(RolePermission.ENVIRONMENT_SECRET, ENV_ID, RolePermissionAction.READ, () -> rootTarget().request().get());
        }
    }
}
