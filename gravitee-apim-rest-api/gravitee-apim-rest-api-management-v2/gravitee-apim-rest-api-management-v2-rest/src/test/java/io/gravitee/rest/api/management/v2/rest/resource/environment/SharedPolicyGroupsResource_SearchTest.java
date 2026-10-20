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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import fixtures.core.model.SharedPolicyGroupFixtures;
import io.gravitee.apim.core.shared_policy_group.use_case.SearchSharedPolicyGroupUseCase;
import io.gravitee.common.data.domain.Page;
import io.gravitee.rest.api.management.v2.rest.model.SharedPolicyGroupsResponse;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResourceTest;
import io.gravitee.rest.api.model.EnvironmentEntity;
import io.gravitee.rest.api.model.permissions.RolePermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SharedPolicyGroupsResource_SearchTest extends AbstractResourceTest {

    private static final String ENV_ID = "my-env";

    @Inject
    SearchSharedPolicyGroupUseCase searchSharedPolicyGroupUseCase;

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
        when(environmentService.findById(ENV_ID)).thenReturn(environmentEntity);
        when(environmentService.findByOrgAndIdOrHrid(ORGANIZATION, ENV_ID)).thenReturn(environmentEntity);

        GraviteeContext.setCurrentEnvironment(ENV_ID);
        GraviteeContext.setCurrentOrganization(ORGANIZATION);

        when(searchSharedPolicyGroupUseCase.execute(any())).thenReturn(
            new SearchSharedPolicyGroupUseCase.Output(new Page<>(List.of(SharedPolicyGroupFixtures.aSharedPolicyGroup()), 1, 1, 1))
        );
    }

    @AfterEach
    public void tearDown() {
        super.tearDown();
        GraviteeContext.cleanContext();
        reset(searchSharedPolicyGroupUseCase);
    }

    @Test
    void should_return_steps_to_a_caller_who_can_update_shared_policy_groups() {
        var response = rootTarget().request().get();

        assertThat(response)
            .hasStatus(200)
            .asEntity(SharedPolicyGroupsResponse.class)
            .satisfies(page ->
                assertThat(page.getData())
                    .singleElement()
                    .satisfies(spg -> assertThat(spg.getSteps()).hasSize(1))
            );
    }

    @Test
    void should_hide_steps_from_a_caller_who_cannot_update_shared_policy_groups() {
        when(
            permissionService.hasPermission(
                any(),
                eq(RolePermission.ENVIRONMENT_SHARED_POLICY_GROUP),
                eq(ENV_ID),
                eq(RolePermissionAction.UPDATE)
            )
        ).thenReturn(false);

        var response = rootTarget().request().get();

        assertThat(response)
            .hasStatus(200)
            .asEntity(Map.class)
            .satisfies(page ->
                assertThat((List<Map<String, Object>>) page.get("data"))
                    .singleElement()
                    .satisfies(spg -> assertThat(spg).containsKey("name").doesNotContainKey("steps"))
            );
    }
}
