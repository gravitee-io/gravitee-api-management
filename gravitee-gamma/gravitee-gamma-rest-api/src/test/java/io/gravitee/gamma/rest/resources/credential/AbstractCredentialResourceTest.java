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

import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static jakarta.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import inmemory.AuditCrudServiceInMemory;
import inmemory.CredentialConsumersInMemory;
import inmemory.CredentialCrudServiceInMemory;
import inmemory.SecretCrudServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import io.gravitee.gamma.rest.resource.AbstractResourceTest;
import io.gravitee.gamma.rest.resources.credential.dto.CredentialDto;
import io.gravitee.gamma.rest.spring.ResourceContextConfiguration;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = { ResourceContextConfiguration.class, CredentialsTestConfiguration.class })
abstract class AbstractCredentialResourceTest extends AbstractResourceTest {

    static final String ENV_ID = "my-env";

    @Inject
    CredentialCrudServiceInMemory credentialCrudService;

    @Inject
    CredentialConsumersInMemory credentialConsumers;

    @Inject
    SecretCrudServiceInMemory secretCrudService;

    @Inject
    AuditCrudServiceInMemory auditCrudService;

    @Inject
    UserCrudServiceInMemory userCrudService;

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENV_ID + "/credentials";
    }

    @BeforeEach
    void prepareEnvironment() {
        var environment = new EnvironmentEntity();
        environment.setId(ENV_ID);
        environment.setOrganizationId(ORGANIZATION);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENV_ID)).thenReturn(environment);
    }

    @AfterEach
    void resetStores() {
        credentialCrudService.reset();
        credentialConsumers.reset();
        secretCrudService.reset();
        auditCrudService.reset();
        userCrudService.reset();
    }

    void givenTheCallerIsNamed(String firstname, String lastname) {
        userCrudService.initWith(
            List.of(BaseUserEntity.builder().id(USER_NAME).organizationId(ORGANIZATION).firstname(firstname).lastname(lastname).build())
        );
    }

    static Map<String, Object> bearer(String name) {
        return Map.of(
            "name",
            name,
            "strategy",
            "BEARER",
            "serviceId",
            "openai",
            "references",
            Map.of("token", "/vault/secret/openai:token")
        );
    }

    static Map<String, Object> managedBearer(String name, String token) {
        return Map.of("name", name, "strategy", "BEARER", "values", Map.of("token", token));
    }

    Response create(Map<String, Object> payload) {
        return rootTarget().request().post(json(payload));
    }

    CredentialDto given(Map<String, Object> payload) {
        var response = create(payload);
        assertThat(response.getStatus()).isEqualTo(CREATED_201);
        return response.readEntity(CredentialDto.class);
    }

    void withoutPermission(RolePermission permission, RolePermissionAction action) {
        when(permissionService.hasPermission(any(), eq(permission), eq(ENV_ID), eq(action))).thenReturn(false);
    }

    void shouldReturn403(RolePermission permission, RolePermissionAction action, Supplier<Response> call) {
        withoutPermission(permission, action);

        assertThat(call.get().getStatus()).isEqualTo(FORBIDDEN_403);
    }
}
