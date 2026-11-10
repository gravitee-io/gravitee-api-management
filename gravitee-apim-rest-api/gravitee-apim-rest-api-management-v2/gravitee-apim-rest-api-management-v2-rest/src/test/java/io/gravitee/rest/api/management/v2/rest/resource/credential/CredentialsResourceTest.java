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
import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static io.gravitee.common.http.HttpStatusCode.OK_200;
import static jakarta.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import inmemory.AuditCrudServiceInMemory;
import inmemory.CredentialCrudServiceInMemory;
import inmemory.SecretCrudServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditEntity;
import io.gravitee.rest.api.management.v2.rest.model.CreateCredential;
import io.gravitee.rest.api.management.v2.rest.model.Credential;
import io.gravitee.rest.api.management.v2.rest.model.CredentialStrategy;
import io.gravitee.rest.api.management.v2.rest.model.CredentialsResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CredentialsResourceTest extends AbstractResourceTest {

    private static final String ENV_ID = "my-env";

    @Inject
    private CredentialCrudServiceInMemory credentialCrudService;

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
        secretCrudService.reset();
        auditCrudService.reset();
    }

    private static CreateCredential bearer(String name) {
        return new CreateCredential()
            .name(name)
            .strategy(CredentialStrategy.BEARER)
            .serviceId("openai")
            .references(Map.of("token", "/vault/secret/openai:token"));
    }

    private Response create(CreateCredential createCredential) {
        return rootTarget().request().post(json(createCredential));
    }

    @Nested
    class CreateCredentialTest {

        @Test
        void should_create_a_credential_that_references_its_secrets() {
            var response = create(bearer("OpenAI"));

            assertThat(response.getStatus()).isEqualTo(CREATED_201);
            var created = response.readEntity(Credential.class);
            assertThat(created.getName()).isEqualTo("OpenAI");
            assertThat(created.getStrategy()).isEqualTo(CredentialStrategy.BEARER);
            assertThat(created.getOwnerId()).isEqualTo(USER_NAME);
            assertThat(created.getManaged()).isFalse();
            assertThat(created.getSecretReferences().get("token").getReference()).isEqualTo("/vault/secret/openai:token");
            assertThat(response.getLocation().toString()).endsWith("/credentials/" + created.getId());
            assertThat(credentialCrudService.storage())
                .extracting(c -> c.id())
                .containsExactly(created.getId());
        }

        @Test
        void should_store_typed_values_as_secrets_and_never_return_them() {
            var response = create(
                new CreateCredential().name("Anthropic").strategy(CredentialStrategy.BEARER).values(Map.of("token", "sk-ant-123"))
            );

            assertThat(response.getStatus()).isEqualTo(CREATED_201);
            response.bufferEntity();
            assertThat(response.readEntity(String.class)).doesNotContain("sk-ant-123");
            var created = response.readEntity(Credential.class);
            assertThat(created.getManaged()).isTrue();
            assertThat(created.getSecretReferences().get("token").getManaged()).isTrue();
            assertThat(secretCrudService.storage()).hasSize(1);
            assertThat(auditCrudService.storage())
                .extracting(AuditEntity::getPatch)
                .noneMatch(patch -> patch != null && patch.contains("sk-ant-123"));
        }

        @Test
        void should_refuse_an_invalid_credential() {
            var response = create(new CreateCredential().name("OpenAI").strategy(CredentialStrategy.BEARER));

            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
            assertThat(credentialCrudService.storage()).isEmpty();
        }

        @Test
        void should_refuse_typed_values_without_the_secret_create_permission() {
            when(
                permissionService.hasPermission(
                    GraviteeContext.getExecutionContext(),
                    RolePermission.ENVIRONMENT_SECRET,
                    ENV_ID,
                    RolePermissionAction.CREATE
                )
            ).thenReturn(false);

            var response = create(
                new CreateCredential().name("Anthropic").strategy(CredentialStrategy.BEARER).values(Map.of("token", "sk-ant-123"))
            );

            assertThat(response.getStatus()).isEqualTo(FORBIDDEN_403);
            assertThat(credentialCrudService.storage()).isEmpty();
            assertThat(secretCrudService.storage()).isEmpty();
        }

        @Test
        void should_return_403_without_the_create_permission() {
            shouldReturn403(RolePermission.ENVIRONMENT_CREDENTIAL, ENV_ID, RolePermissionAction.CREATE, () -> create(bearer("OpenAI")));
        }
    }

    @Nested
    class ListCredentialsTest {

        @Test
        void should_page_the_credentials_of_the_environment_sorted_by_name() {
            create(bearer("Cohere"));
            create(bearer("Anthropic"));
            create(bearer("Bedrock"));

            var response = rootTarget().queryParam("page", 1).queryParam("perPage", 2).request().get();

            assertThat(response.getStatus()).isEqualTo(OK_200);
            var page = response.readEntity(CredentialsResponse.class);
            assertThat(page.getData()).extracting(Credential::getName).containsExactly("Anthropic", "Bedrock");
            assertThat(page.getPagination().getPage()).isEqualTo(1);
            assertThat(page.getPagination().getTotalCount()).isEqualTo(3);
            assertThat(page.getPagination().getPageCount()).isEqualTo(2);
        }

        @Test
        void should_filter_on_the_name_the_strategies_and_the_service() {
            create(bearer("OpenAI production"));
            create(
                new CreateCredential()
                    .name("Anthropic")
                    .strategy(CredentialStrategy.API_KEY)
                    .serviceId("anthropic")
                    .configuration(Map.of("headerName", "x-api-key"))
                    .references(Map.of("value", "/vault/secret/anthropic:key"))
            );

            assertThat(names(rootTarget().queryParam("q", " openai "))).containsExactly("OpenAI production");
            assertThat(names(rootTarget().queryParam("strategy", "API_KEY"))).containsExactly("Anthropic");
            assertThat(names(rootTarget().queryParam("strategy", "API_KEY,BEARER"))).containsExactly("Anthropic", "OpenAI production");
            assertThat(names(rootTarget().queryParam("serviceId", "openai"))).containsExactly("OpenAI production");
            assertThat(names(rootTarget().queryParam("strategy", "BASIC").queryParam("serviceId", "anthropic"))).isEmpty();
        }

        private List<String> names(WebTarget target) {
            var response = target.request().get();
            assertThat(response.getStatus()).isEqualTo(OK_200);
            return response.readEntity(CredentialsResponse.class).getData().stream().map(Credential::getName).toList();
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
        void should_refuse_a_page_whose_offset_overflows() {
            var response = rootTarget().queryParam("page", 21474838).queryParam("perPage", 100).request().get();

            assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
        }

        @Test
        void should_return_403_without_the_read_permission() {
            shouldReturn403(RolePermission.ENVIRONMENT_CREDENTIAL, ENV_ID, RolePermissionAction.READ, () -> rootTarget().request().get());
        }
    }
}
