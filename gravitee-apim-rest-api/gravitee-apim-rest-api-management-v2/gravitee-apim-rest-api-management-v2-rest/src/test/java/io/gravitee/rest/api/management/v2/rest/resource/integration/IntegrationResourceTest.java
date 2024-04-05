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
package io.gravitee.rest.api.management.v2.rest.resource.integration;

import static assertions.MAPIAssertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import fixtures.core.model.IntegrationFixture;
import inmemory.IntegrationCrudServiceInMemory;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.management.v2.rest.model.Integration;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class IntegrationResourceTest extends AbstractResourceTest {

    @Autowired
    IntegrationCrudServiceInMemory integrationCrudServiceInMemory;

    static final String ENVIRONMENT = "my-env";
    static final String INTEGRATION_NAME = "test-name";
    static final String INTEGRATION_DESCRIPTION = "integration-description";
    static final String INTEGRATION_PROVIDER = "test-provider";
    static final String INTEGRATION_ID = "generated-id";

    WebTarget target;

    @Override
    protected String contextPath() {
        return "/environments/" + ENVIRONMENT + "/integrations";
    }

    @BeforeEach
    public void init() throws TechnicalException {
        target = rootTarget();

        EnvironmentEntity environmentEntity = EnvironmentEntity.builder().id(ENVIRONMENT).organizationId(ORGANIZATION).build();
        doReturn(environmentEntity).when(environmentService).findById(ENVIRONMENT);
        doReturn(environmentEntity).when(environmentService).findByOrgAndIdOrHrid(ORGANIZATION, ENVIRONMENT);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);

        UuidString.overrideGenerator(() -> INTEGRATION_ID);
    }

    @AfterEach
    public void tearDown() {
        integrationCrudServiceInMemory.reset();
    }

    @Nested
    class GetIntegration {

        @Test
        public void should_get_integration() {
            //Given
            target = rootTarget(INTEGRATION_ID);
            var integration = List.of(IntegrationFixture.anIntegration());
            integrationCrudServiceInMemory.initWith(integration);

            //When
            Response response = target.request().get();

            //Then
            assertThat(response)
                .hasStatus(HttpStatusCode.OK_200)
                .asEntity(Integration.class)
                .isEqualTo(
                    Integration
                        .builder()
                        .id(INTEGRATION_ID)
                        .name(INTEGRATION_NAME)
                        .description(INTEGRATION_DESCRIPTION)
                        .provider(INTEGRATION_PROVIDER)
                        .agentStatus(Integration.AgentStatusEnum.DISCONNECTED)
                        .build()
                );
        }

        @Test
        public void should_throw_error_when_integration_not_found() {
            //Given
            var notExistingId = "not-existing-id";
            target = rootTarget(notExistingId);

            //When
            Response response = target.request().get();

            //Then
            assertThat(response).hasStatus(HttpStatusCode.NOT_FOUND_404);
        }

        @Test
        public void should_return_403_when_incorrect_permission() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.ENVIRONMENT_INTEGRATION),
                    eq(ENVIRONMENT),
                    eq(RolePermissionAction.READ)
                )
            )
                .thenReturn(false);

            Response response = target.request().get();
            assertThat(response).hasStatus(HttpStatusCode.FORBIDDEN_403);
        }
    }
}
