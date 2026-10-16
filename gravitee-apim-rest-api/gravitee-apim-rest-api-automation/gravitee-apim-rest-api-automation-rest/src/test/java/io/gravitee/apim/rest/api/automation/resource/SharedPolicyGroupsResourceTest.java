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

import static io.gravitee.rest.api.model.permissions.RolePermission.ENVIRONMENT_SHARED_POLICY_GROUP;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.CREATE;
import static io.gravitee.rest.api.model.permissions.RolePermissionAction.UPDATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import inmemory.SharedPolicyGroupCrudServiceInMemory;
import io.gravitee.apim.core.shared_policy_group.model.SharedPolicyGroup;
import io.gravitee.apim.core.shared_policy_group.use_case.CreateSharedPolicyGroupUseCase;
import io.gravitee.apim.core.shared_policy_group.use_case.UpdateSharedPolicyGroupUseCase;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SharedPolicyGroupsResourceTest extends AbstractResourceTest {

    private static final String HRID = "spg-foo";

    @Inject
    SharedPolicyGroupCrudServiceInMemory sharedPolicyGroupCrudService;

    @Inject
    CreateSharedPolicyGroupUseCase createSharedPolicyGroupUseCase;

    @Inject
    UpdateSharedPolicyGroupUseCase updateSharedPolicyGroupUseCase;

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/shared-policy-groups";
    }

    @Nested
    class Authorization {

        private final HRIDToUUID.TopLevelResult ids = HRIDToUUID.sharedPolicyGroup()
            .context(new ExecutionContext(ORGANIZATION, ENVIRONMENT))
            .hrid(HRID);

        @BeforeEach
        void setUp() {
            when(createSharedPolicyGroupUseCase.execute(any(CreateSharedPolicyGroupUseCase.Input.class))).thenAnswer(invocation -> {
                CreateSharedPolicyGroupUseCase.Input input = invocation.getArgument(0);
                var toCreate = input.sharedPolicyGroupToCreate();
                return new CreateSharedPolicyGroupUseCase.Output(
                    SharedPolicyGroup.builder().id(toCreate.getId()).crossId(toCreate.getCrossId()).environmentId(ENVIRONMENT).build()
                );
            });
            when(updateSharedPolicyGroupUseCase.execute(any(UpdateSharedPolicyGroupUseCase.Input.class))).thenAnswer(invocation -> {
                UpdateSharedPolicyGroupUseCase.Input input = invocation.getArgument(0);
                return new UpdateSharedPolicyGroupUseCase.Output(
                    SharedPolicyGroup.builder().id(input.sharedPolicyGroupId()).environmentId(ENVIRONMENT).build()
                );
            });
        }

        @AfterEach
        void tearDown() {
            sharedPolicyGroupCrudService.reset();
            reset(permissionService, createSharedPolicyGroupUseCase, updateSharedPolicyGroupUseCase);
        }

        @Test
        void should_return_403_when_the_caller_cannot_update_the_existing_shared_policy_group() {
            givenSharedPolicyGroup(ids.id(), ids.crossId());
            denySharedPolicyGroupPermission(UPDATE);

            assertThat(put(readJSON("shared-policy-group.json"), false, false)).isEqualTo(403);
            verify(updateSharedPolicyGroupUseCase, never()).execute(any());
            verify(createSharedPolicyGroupUseCase, never()).execute(any());
            assertThat(sharedPolicyGroupCrudService.storage()).extracting(SharedPolicyGroup::getName).containsExactly("existing");
        }

        @Test
        void should_return_403_on_dry_run_when_the_caller_cannot_update_the_existing_shared_policy_group() {
            givenSharedPolicyGroup(ids.id(), ids.crossId());
            denySharedPolicyGroupPermission(UPDATE);

            assertThat(put(readJSON("shared-policy-group.json"), true, false)).isEqualTo(403);
        }

        @Test
        void should_return_403_when_a_cross_id_names_an_existing_shared_policy_group_the_caller_cannot_update() {
            givenSharedPolicyGroup("existing-id", "spg-cross-id");
            denySharedPolicyGroupPermission(UPDATE);

            assertThat(put(readJSON("shared-policy-group-with-cross-id-and-hrid.json"), false, false)).isEqualTo(403);
            verify(updateSharedPolicyGroupUseCase, never()).execute(any());
        }

        @Test
        void should_return_403_when_a_legacy_id_names_an_existing_shared_policy_group_the_caller_cannot_update() {
            givenSharedPolicyGroup("legacy-id", "spg-cross-id");
            denySharedPolicyGroupPermission(UPDATE);

            var spec = readJSON("shared-policy-group-with-cross-id-and-hrid.json").replace("\"spg-foo\"", "\"legacy-id\"");
            assertThat(put(spec, false, true)).isEqualTo(403);
            verify(updateSharedPolicyGroupUseCase, never()).execute(any());
        }

        @Test
        void should_update_the_existing_shared_policy_group_with_update_and_no_create() {
            givenSharedPolicyGroup(ids.id(), ids.crossId());
            denySharedPolicyGroupPermission(CREATE);

            assertThat(put(readJSON("shared-policy-group.json"), false, false)).isEqualTo(200);
            verify(updateSharedPolicyGroupUseCase).execute(any());
        }

        @Test
        void should_create_a_new_shared_policy_group_with_create_and_no_update() {
            denySharedPolicyGroupPermission(UPDATE);

            assertThat(put(readJSON("shared-policy-group.json"), false, false)).isEqualTo(200);
            verify(createSharedPolicyGroupUseCase).execute(any());
        }

        @Test
        void should_return_403_when_the_caller_cannot_create_a_new_shared_policy_group() {
            denySharedPolicyGroupPermission(CREATE);

            assertThat(put(readJSON("shared-policy-group.json"), false, false)).isEqualTo(403);
            verify(createSharedPolicyGroupUseCase, never()).execute(any());
        }

        private void givenSharedPolicyGroup(String id, String crossId) {
            sharedPolicyGroupCrudService.initWith(
                List.of(
                    SharedPolicyGroup.builder()
                        .id(id)
                        .crossId(crossId)
                        .name("existing")
                        .environmentId(ENVIRONMENT)
                        .organizationId(ORGANIZATION)
                        .build()
                )
            );
        }

        private void denySharedPolicyGroupPermission(RolePermissionAction action) {
            when(permissionService.hasPermission(any(), eq(ENVIRONMENT_SHARED_POLICY_GROUP), eq(ENVIRONMENT), eq(action))).thenReturn(
                false
            );
        }

        private int put(String spec, boolean dryRun, boolean hridContainsUUID) {
            try (
                var response = rootTarget()
                    .queryParam("dryRun", dryRun)
                    .queryParam("hridContainsUUID", hridContainsUUID)
                    .request()
                    .put(Entity.json(spec))
            ) {
                return response.getStatus();
            }
        }
    }
}
