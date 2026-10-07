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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.group.model.crd.GroupCRDStatus;
import io.gravitee.apim.core.group.use_case.ImportGroupCRDUseCase;
import io.gravitee.apim.core.group.use_case.ValidateGroupCRDUseCase;
import io.gravitee.apim.core.member.model.RoleScope;
import io.gravitee.apim.rest.api.automation.model.GroupState;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class GroupsResourceTest extends AbstractResourceTest {

    @Inject
    private ImportGroupCRDUseCase importGroupCRDUseCase;

    @Inject
    private ValidateGroupCRDUseCase validateGroupCRDUseCase;

    @AfterEach
    void tearDown() {
        reset(importGroupCRDUseCase);
        reset(validateGroupCRDUseCase);
    }

    @Nested
    class Run {

        @BeforeEach
        void setUp() {
            when(importGroupCRDUseCase.execute(any(ImportGroupCRDUseCase.Input.class))).thenReturn(
                new ImportGroupCRDUseCase.Output(GroupCRDStatus.builder().id("group-id").members(0).build())
            );
        }

        @Test
        void should_return_state_from_name() {
            var state = expectEntity("group-with-name.json");
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getId()).isEqualTo("group-id");
                soft.assertThat(state.getHrid()).isEqualTo("my-group");
                soft.assertThat(state.getName()).isEqualTo("my-group");
                soft.assertThat(state.getNotifyMembers()).isTrue();
                soft.assertThat(state.getOrganizationId()).isEqualTo(ORGANIZATION);
                soft.assertThat(state.getEnvironmentId()).isEqualTo(ENVIRONMENT);
            });
        }

        @Test
        void should_return_state_with_members() {
            when(importGroupCRDUseCase.execute(any(ImportGroupCRDUseCase.Input.class))).thenReturn(
                new ImportGroupCRDUseCase.Output(GroupCRDStatus.builder().id("group-id").members(1).build())
            );

            var state = expectEntity("group-with-members.json");
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getId()).isEqualTo("group-id");
                soft.assertThat(state.getMemberCount()).isEqualTo(1L);
                soft.assertThat(state.getMembers()).hasSize(1);
            });
        }

        @Test
        void should_use_name_as_id_when_hrid_contains_uuid() {
            when(importGroupCRDUseCase.execute(any(ImportGroupCRDUseCase.Input.class))).thenAnswer(call -> {
                ImportGroupCRDUseCase.Input input = call.getArgument(0, ImportGroupCRDUseCase.Input.class);
                return new ImportGroupCRDUseCase.Output(GroupCRDStatus.builder().id(input.spec().getId()).members(0).build());
            });

            var state = expectEntity("group-with-name.json", false, true);
            assertThat(state.getId()).isEqualTo("my-group");
        }
    }

    @Nested
    class DryRun {

        boolean dryRun = true;

        @Test
        void should_return_state_from_validation() {
            when(validateGroupCRDUseCase.execute(any(ImportGroupCRDUseCase.Input.class))).thenReturn(
                new ImportGroupCRDUseCase.Output(GroupCRDStatus.builder().id("generated-id").members(0).build())
            );

            var state = expectEntity("group-with-name.json", dryRun);
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getId()).isEqualTo("generated-id");
                soft.assertThat(state.getName()).isEqualTo("my-group");
            });
        }

        @Test
        void should_return_state_with_errors() {
            when(validateGroupCRDUseCase.execute(any(ImportGroupCRDUseCase.Input.class))).thenReturn(
                new ImportGroupCRDUseCase.Output(
                    GroupCRDStatus.builder()
                        .id("generated-id")
                        .members(0)
                        .errors(new GroupCRDStatus.Errors(java.util.List.of(), java.util.List.of("unknown role")))
                        .build()
                )
            );

            var state = expectEntity("group-with-name.json", dryRun);
            SoftAssertions.assertSoftly(soft -> {
                soft.assertThat(state.getId()).isEqualTo("generated-id");
                soft.assertThat(state.getErrors()).isNotNull();
                soft.assertThat(state.getErrors().getWarning()).contains("unknown role");
            });
        }
    }

    @Nested
    class DefaultMemberRolesAndIgnoreMembers {

        @BeforeEach
        void setUp() {
            when(importGroupCRDUseCase.execute(any(ImportGroupCRDUseCase.Input.class))).thenReturn(
                new ImportGroupCRDUseCase.Output(GroupCRDStatus.builder().id("group-id").members(0).build())
            );
            when(validateGroupCRDUseCase.execute(any(ImportGroupCRDUseCase.Input.class))).thenReturn(
                new ImportGroupCRDUseCase.Output(GroupCRDStatus.builder().id("group-id").members(0).build())
            );
        }

        @Test
        void should_pass_default_member_roles_to_the_use_case_and_echo_them() {
            var state = expectEntity("group-with-default-member-roles.json");

            var input = ArgumentCaptor.forClass(ImportGroupCRDUseCase.Input.class);
            verify(importGroupCRDUseCase).execute(input.capture());
            SoftAssertions.assertSoftly(soft -> {
                soft
                    .assertThat(input.getValue().spec().getDefaultMemberRoles())
                    .containsExactlyInAnyOrderEntriesOf(
                        Map.of(RoleScope.API, "USER", RoleScope.APPLICATION, "USER", RoleScope.API_PRODUCT, "USER")
                    );
                soft.assertThat(input.getValue().spec().isIgnoreMembers()).isFalse();
                soft.assertThat(state.getDefaultMemberRoles()).containsEntry("API_PRODUCT", "USER");
            });
        }

        @ParameterizedTest
        @ValueSource(strings = { "group-with-name.json", "group-with-empty-default-member-roles.json" })
        void should_leave_default_member_roles_undeclared_when_absent_or_empty(String fixture) {
            expectEntity(fixture);

            var input = ArgumentCaptor.forClass(ImportGroupCRDUseCase.Input.class);
            verify(importGroupCRDUseCase).execute(input.capture());
            assertThat(input.getValue().spec().getDefaultMemberRoles()).isNull();
        }

        @ParameterizedTest
        @ValueSource(booleans = { true, false })
        void should_reject_a_scope_that_is_not_a_group_default_role_scope(boolean dryRun) {
            try (
                var response = rootTarget()
                    .queryParam("dryRun", dryRun)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("group-with-integration-default-member-role.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(400);
                assertThat(response.readEntity(String.class)).contains("scope [INTEGRATION] is not a group default role scope");
            }
        }

        @ParameterizedTest
        @ValueSource(booleans = { true, false })
        void should_pass_ignore_members_to_the_use_case(boolean dryRun) {
            try (
                var response = rootTarget()
                    .queryParam("dryRun", dryRun)
                    .queryParam("ignoreMembers", true)
                    .request()
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .put(Entity.json(readJSON("group-with-members.json")))
            ) {
                assertThat(response.getStatus()).isEqualTo(200);
            }

            var input = ArgumentCaptor.forClass(ImportGroupCRDUseCase.Input.class);
            if (dryRun) {
                verify(validateGroupCRDUseCase).execute(input.capture());
            } else {
                verify(importGroupCRDUseCase).execute(input.capture());
            }
            assertThat(input.getValue().spec().isIgnoreMembers()).isTrue();
        }
    }

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/groups";
    }

    private GroupState expectEntity(String spec) {
        return expectEntity(spec, false, false);
    }

    private GroupState expectEntity(String spec, boolean dryRun) {
        return expectEntity(spec, dryRun, false);
    }

    private GroupState expectEntity(String spec, boolean dryRun, boolean hridContainsUUID) {
        try (
            var response = rootTarget()
                .queryParam("dryRun", dryRun)
                .queryParam("hridContainsUUID", hridContainsUUID)
                .request()
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(readJSON(spec)))
        ) {
            assertThat(response.getStatus()).isEqualTo(200);
            return response.readEntity(GroupState.class);
        }
    }
}
