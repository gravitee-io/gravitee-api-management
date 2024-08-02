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

import static assertions.MAPIAssertions.assertThat;
import static fixtures.SharedPolicyGroupFixtures.aCreateSharedPolicyGroup;
import static io.gravitee.common.http.HttpStatusCode.BAD_REQUEST_400;
import static io.gravitee.common.http.HttpStatusCode.CREATED_201;
import static io.gravitee.common.http.HttpStatusCode.FORBIDDEN_403;
import static jakarta.ws.rs.client.Entity.json;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import assertions.MAPIAssertions;
import fixtures.core.model.SharedPolicyGroupFixtures;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.shared_policy_group.use_case.CreateSharedPolicyGroupUseCase;
import io.gravitee.rest.api.management.v2.rest.model.ApiType;
import io.gravitee.rest.api.management.v2.rest.model.CreateSharedPolicyGroup;
import io.gravitee.rest.api.management.v2.rest.model.ExecutionPhase;
import io.gravitee.rest.api.management.v2.rest.model.SharedPolicyGroupLifecycleState;
import io.gravitee.rest.api.management.v2.rest.model.StepV4;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

public class SharedPolicyGroupsResource_CreateTest extends AbstractResourceTest {

    private static final String ENV_ID = "my-env";

    @Inject
    CreateSharedPolicyGroupUseCase createSharedPolicyGroupUseCase;

    @Override
    protected String contextPath() {
        return "/environments/" + ENV_ID + "/shared-policy-groups";
    }

    @BeforeEach
    void init() {
        super.setUp();
        GraviteeContext.cleanContext();

        EnvironmentEntity environmentEntity = new EnvironmentEntity();
        environmentEntity.setId(ENV_ID);
        environmentEntity.setOrganizationId(ORGANIZATION);
        doReturn(environmentEntity).when(environmentService).findById(ENV_ID);
        doReturn(environmentEntity).when(environmentService).findByOrgAndIdOrHrid(ORGANIZATION, ENV_ID);

        GraviteeContext.setCurrentEnvironment(ENV_ID);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        reset(createSharedPolicyGroupUseCase);
    }

    @Test
    void should_create_shared_policy_group() {
        var createSharedPolicyGroup = aCreateSharedPolicyGroup();

        when(createSharedPolicyGroupUseCase.execute(any()))
            .thenReturn(new CreateSharedPolicyGroupUseCase.Output(SharedPolicyGroupFixtures.aSharedPolicyGroup()));

        final Response response = rootTarget().request().post(json(createSharedPolicyGroup));
        assertThat(response.getStatus()).isEqualTo(CREATED_201);
        var createdSharedPolicyGroup = response.readEntity(io.gravitee.rest.api.management.v2.rest.model.SharedPolicyGroup.class);

        assertThat(createdSharedPolicyGroup)
            .isNotNull()
            .hasFieldOrPropertyWithValue("id", SharedPolicyGroupFixtures.aSharedPolicyGroup().getId())
            .hasFieldOrPropertyWithValue("crossId", SharedPolicyGroupFixtures.aSharedPolicyGroup().getCrossId())
            .hasFieldOrPropertyWithValue("name", SharedPolicyGroupFixtures.aSharedPolicyGroup().getName())
            .hasFieldOrPropertyWithValue("description", SharedPolicyGroupFixtures.aSharedPolicyGroup().getDescription())
            .hasFieldOrPropertyWithValue("version", SharedPolicyGroupFixtures.aSharedPolicyGroup().getVersion())
            .hasFieldOrPropertyWithValue(
                "lifecycleState",
                SharedPolicyGroupLifecycleState.fromValue(SharedPolicyGroupFixtures.aSharedPolicyGroup().getLifecycleState().name())
            )
            .hasFieldOrPropertyWithValue("apiType", ApiType.fromValue(SharedPolicyGroupFixtures.aSharedPolicyGroup().getApiType().name()))
            .hasFieldOrPropertyWithValue(
                "phase",
                ExecutionPhase.fromValue(SharedPolicyGroupFixtures.aSharedPolicyGroup().getPhase().name())
            )
            .satisfies(sharedPolicyGroup -> {
                Assertions
                    .assertThat(sharedPolicyGroup.getSteps())
                    .hasSize(1)
                    .containsAll(
                        List.of(
                            StepV4
                                .builder()
                                .policy("policyId")
                                .name("Step name")
                                .enabled(true)
                                .configuration(Map.ofEntries(Map.entry("key", "value")))
                                .build()
                        )
                    );
            })
            .hasFieldOrProperty("deployedAt")
            .hasFieldOrProperty("createdAt")
            .hasFieldOrProperty("updatedAt");

        var captor = ArgumentCaptor.forClass(CreateSharedPolicyGroupUseCase.Input.class);
        verify(createSharedPolicyGroupUseCase).execute(captor.capture());
        SoftAssertions.assertSoftly(soft -> {
            var input = captor.getValue();
            soft.assertThat(input.sharedPolicyGroupToCreate().getName()).isEqualTo(createSharedPolicyGroup.getName());
            soft.assertThat(input.auditInfo()).isInstanceOf(AuditInfo.class);
        });
    }

    @Test
    void should_return_400_if_execute_fails_with_invalid_data_exception() {
        var createSharedPolicyGroup = SharedPolicyGroupFixtures.aCreateSharedPolicyGroup();

        when(createSharedPolicyGroupUseCase.execute(any())).thenThrow(new InvalidDataException("Name is required."));

        final Response response = rootTarget().request().post(json(createSharedPolicyGroup));
        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
    }

    @Test
    void should_return_400_if_missing_body() {
        final Response response = rootTarget().request().post(json(null));

        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
    }

    @Test
    void should_return_400_if_name_not_specified() {
        final Response response = rootTarget().request().post(json(new CreateSharedPolicyGroup()));

        assertThat(response.getStatus()).isEqualTo(BAD_REQUEST_400);
    }

    @Test
    public void should_return_403_if_incorrect_permissions() {
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.SHARED_POLICY_GROUP),
                eq(ENV_ID),
                eq(RolePermissionAction.CREATE)
            )
        )
            .thenReturn(false);

        final Response response = rootTarget().request().post(json(SharedPolicyGroupFixtures.aCreateSharedPolicyGroup()));

        MAPIAssertions
            .assertThat(response)
            .hasStatus(FORBIDDEN_403)
            .asError()
            .hasHttpStatus(FORBIDDEN_403)
            .hasMessage("You do not have sufficient rights to access this resource");
    }
}
