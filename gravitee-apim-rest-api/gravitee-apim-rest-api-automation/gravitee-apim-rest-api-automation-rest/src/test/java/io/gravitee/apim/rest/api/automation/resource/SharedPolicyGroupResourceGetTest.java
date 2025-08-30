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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import inmemory.SharedPolicyGroupCrudServiceInMemory;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup;
import io.gravitee.apim.rest.api.automation.model.SharedPolicyGroupState;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.IdBuilder;
import jakarta.inject.Inject;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
class SharedPolicyGroupResourceGetTest extends AbstractResourceTest {

    static final String HRID = "spg-foo";
    static final String SPG_ID = "e2b59e74-11c7-3866-a475-e8df59419663";

    @Inject
    SharedPolicyGroupCrudServiceInMemory sharedPolicyGroupCrudService;

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
                any(RolePermissionAction.class)
            )
        )
            .thenReturn(true);

        IdBuilder builder = IdBuilder.builder(new ExecutionContext(ORGANIZATION, ENVIRONMENT), HRID);
        sharedPolicyGroupCrudService.initWith(
            List.of(
                SharedPolicyGroup
                    .builder()
                    .id(builder.buildId())
                    .crossId(builder.buildCrossId())
                    .hrid(HRID)
                    .environmentId(ENVIRONMENT)
                    .organizationId(ORGANIZATION)
                    .steps(List.of(Step.builder().name("step-name").build()))
                    .build()
            )
        );
    }

    @AfterEach
    void tearDown() {
        sharedPolicyGroupCrudService.reset();
    }

    @Test
    void should_be_forbidden_without_permission() {
        when(
            permissionService.hasPermission(
                eq(GraviteeContext.getExecutionContext()),
                eq(RolePermission.ENVIRONMENT_SHARED_POLICY_GROUP),
                eq(ENVIRONMENT),
                any(RolePermissionAction.class)
            )
        )
            .thenReturn(false);

        expectForbidden(HRID);
    }

    @Test
    void should_get_shared_policy_group_from_known_hrid() {
        var state = expectEntity(HRID);
        SoftAssertions.assertSoftly(soft -> {
            assertThat(state.getId()).isNotBlank();
            assertThat(state.getCrossId()).isNotBlank();
            assertThat(state.getHrid()).isEqualTo(HRID);
            assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
            assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
            assertThat(state.getSteps()).isNotEmpty();
        });
    }

    @Test
    void should_return_a_404_status_code_with_unknown_hrid() {
        expectNotFound("unknown");
        assertThat(sharedPolicyGroupCrudService.storage()).isNotEmpty();
    }

    private void expectNotFound(String hrid) {
        try (var response = rootTarget().path(hrid).request().get()) {
            assertThat(response.getStatus()).isEqualTo(404);
        }
    }

    private void expectForbidden(String hrid) {
        try (var response = rootTarget().path(hrid).request().get()) {
            assertThat(response.getStatus()).isEqualTo(403);
        }
    }

    private SharedPolicyGroupState expectEntity(String hrid) {
        try (var response = rootTarget().path(hrid).request().get()) {
            assertThat(response.getStatus()).isEqualTo(200);
            return response.readEntity(SharedPolicyGroupState.class);
        }
    }
}
