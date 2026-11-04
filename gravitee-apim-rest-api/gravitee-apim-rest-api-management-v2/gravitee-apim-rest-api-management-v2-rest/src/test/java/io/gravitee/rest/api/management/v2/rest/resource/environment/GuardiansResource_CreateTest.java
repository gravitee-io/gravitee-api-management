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
package io.gravitee.rest.api.management.v2.rest.resource.environment;

import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static jakarta.ws.rs.client.Entity.json;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import assertions.MAPIAssertions;
import fixtures.GuardianFixtures;
import io.gravitee.apim.core.guardian.use_case.CreateGuardianUseCase;
import io.gravitee.rest.api.management.v2.rest.model.ApiType;
import io.gravitee.rest.api.management.v2.rest.model.Guardian;
import io.gravitee.rest.api.management.v2.rest.model.GuardianPhase;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GuardiansResource_CreateTest extends AbstractResourceTest {

    private static final String ENV_ID = "my-env";

    @Inject
    CreateGuardianUseCase createGuardianUseCase;

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

    @Override
    protected String contextPath() {
        return "/environments/" + ENV_ID + "/guardians";
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        reset(createGuardianUseCase);
    }

    @Test
    void should_create_a_guardian() {
        when(createGuardianUseCase.execute(any())).thenReturn(new CreateGuardianUseCase.Output(GuardianFixtures.aGuardian()));

        final Response response = rootTarget().request().post(json(GuardianFixtures.aCreateGuardian()));

        assertThat(response.getStatus()).isEqualTo(CREATED_201);
        assertThat(response.readEntity(Guardian.class))
            .hasFieldOrPropertyWithValue("id", "guardian-id")
            .hasFieldOrPropertyWithValue("name", "Credential leakage")
            .hasFieldOrPropertyWithValue("apiType", ApiType.LLM_PROXY)
            .hasFieldOrPropertyWithValue("phase", GuardianPhase.REQUEST);
    }

    @Test
    void should_return_403_without_the_create_permission() {
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.ENVIRONMENT_GUARDIAN),
                eq(ENV_ID),
                eq(RolePermissionAction.CREATE)
            )
        ).thenReturn(false);

        final Response response = rootTarget().request().post(json(GuardianFixtures.aCreateGuardian()));

        MAPIAssertions.assertThat(response)
            .hasStatus(FORBIDDEN_403)
            .asError()
            .hasHttpStatus(FORBIDDEN_403)
            .hasMessage("You do not have sufficient rights to access this resource");
    }
}
