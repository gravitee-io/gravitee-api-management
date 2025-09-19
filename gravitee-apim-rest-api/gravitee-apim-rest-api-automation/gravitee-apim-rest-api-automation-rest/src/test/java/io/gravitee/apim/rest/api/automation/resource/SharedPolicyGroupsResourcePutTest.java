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
package io.gravitee.apim.rest.api.automation.resource;

import static io.gravitee.apim.rest.api.automation.resource.SharedPolicyGroupResourceGetTest.HRID;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup;
import io.gravitee.apim.core.shared_policy_group.use_case.CreateSharedPolicyGroupUseCase;
import io.gravitee.apim.rest.api.automation.model.SharedPolicyGroupState;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.IdBuilder;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SharedPolicyGroupsResourcePutTest extends AbstractResourceTest {

    static final String SPG_ID = "e2b59e74-11c7-3866-a475-e8df59419663";

    @Inject
    private CreateSharedPolicyGroupUseCase createSharedPolicyGroupUseCase;

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/shared-policy-groups";
    }

    @BeforeEach
    void setUp() {
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);

        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.ENVIRONMENT_SHARED_POLICY_GROUP),
                eq(ENVIRONMENT),
                eq(RolePermissionAction.CREATE),
                eq(RolePermissionAction.UPDATE)
            )
        ).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        reset(createSharedPolicyGroupUseCase);
    }

    @Nested
    class Run {

        @BeforeEach
        void setUp() {
            when(createSharedPolicyGroupUseCase.execute(any(CreateSharedPolicyGroupUseCase.Input.class))).thenAnswer(invocation -> {
                CreateSharedPolicyGroupUseCase.Input argument = invocation.getArgument(0);
                var spgToCreate = argument.sharedPolicyGroupToCreate();
                return new CreateSharedPolicyGroupUseCase.Output(
                    SharedPolicyGroup.builder()
                        .id(spgToCreate.getId())
                        .crossId(spgToCreate.getCrossId())
                        .hrid(spgToCreate.getHrid())
                        .environmentId(ENVIRONMENT)
                        .build()
                );
            });
        }

        @Test
        void should_be_forbidden_without_permission() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.ENVIRONMENT_SHARED_POLICY_GROUP),
                    eq(ENVIRONMENT),
                    eq(RolePermissionAction.CREATE),
                    eq(RolePermissionAction.UPDATE)
                )
            ).thenReturn(false);

            expectForbidden("shared-policy-group.json");
        }

        @Test
        void should_return_state_from_hrid() {
            var state = expectEntity("shared-policy-group.json");
            IdBuilder builder = IdBuilder.builder(new ExecutionContext(ORGANIZATION, ENVIRONMENT), HRID);
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getCrossId()).isEqualTo(builder.buildCrossId());
                soft.assertThat(state.getId()).isEqualTo(builder.buildId());
                soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                soft.assertThat(state.getHrid()).isEqualTo("spg-foo");
            });
        }

        @Test
        void should_return_state_from_cross_id() {
            var state = expectEntity("shared-policy-group-with-cross-id-and-hrid.json");
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getHrid()).isEqualTo("spg-foo");
                soft.assertThat(state.getCrossId()).isEqualTo("spg-cross-id");
                soft.assertThat(state.getId()).isNotBlank();
                soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
            });
        }

        @Test
        void should_reject_with_cross_id_and_no_hrid() {
            try (
                var response = rootTarget()
                    .queryParam("dryRun", false)
                    .request()
                    .put(Entity.json(readJSON("shared-policy-group-with-cross-id-and-no-hrid.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(400);
            }
        }
    }

    @Nested
    class DryRun {

        boolean dryRun = true;

        @Test
        void should_be_forbidden_without_permission() {
            when(
                permissionService.hasPermission(
                    eq(GraviteeContext.getExecutionContext()),
                    eq(RolePermission.ENVIRONMENT_SHARED_POLICY_GROUP),
                    eq(ENVIRONMENT),
                    eq(RolePermissionAction.CREATE),
                    eq(RolePermissionAction.UPDATE)
                )
            ).thenReturn(false);

            expectForbidden("shared-policy-group.json", dryRun);
        }

        @Test
        void should_return_state_from_hrid() {
            var state = expectEntity("shared-policy-group.json", dryRun);
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getCrossId()).isNotBlank();
                soft.assertThat(state.getId()).isNotBlank();
                soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
                soft.assertThat(state.getHrid()).isEqualTo("spg-foo");
            });
        }

        @Test
        void should_return_state_from_cross_id_no_hrid() {
            try (
                var response = rootTarget()
                    .queryParam("dryRun", dryRun)
                    .request()
                    .put(Entity.json(readJSON("shared-policy-group-with-cross-id-and-no-hrid.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(400);
            }
        }
    }

    private void expectForbidden(String spec) {
        expectForbidden(spec, false);
    }

    private void expectForbidden(String spec, boolean dryRun) {
        try (var response = rootTarget().queryParam("dryRun", dryRun).request().put(Entity.json(readJSON(spec)))) {
            assertThat(response.getStatus()).isEqualTo(403);
        }
    }

    private SharedPolicyGroupState expectEntity(String spec) {
        return expectEntity(spec, false);
    }

    private SharedPolicyGroupState expectEntity(String spec, boolean dryRun) {
        try (var response = rootTarget().queryParam("dryRun", dryRun).request().put(Entity.json(readJSON(spec)))) {
            assertThat(response.getStatus()).isEqualTo(200);
            return response.readEntity(SharedPolicyGroupState.class);
        }
    }
}
