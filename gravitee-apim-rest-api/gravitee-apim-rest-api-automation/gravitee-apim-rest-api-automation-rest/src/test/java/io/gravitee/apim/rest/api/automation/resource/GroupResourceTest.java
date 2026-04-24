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
import static org.mockito.Mockito.*;

import inmemory.GroupQueryServiceInMemory;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.rest.api.automation.model.GroupState;
import io.gravitee.apim.rest.api.automation.resource.base.AbstractResourceTest;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.HRIDToUUID;
import io.gravitee.rest.api.service.exceptions.GroupNotFoundException;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Set;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
class GroupResourceTest extends AbstractResourceTest {

    @Inject
    private UserService userService;

    static final String HRID = "my-group";
    static final String GUID = "raw-legacy-uuid";
    static final AuditInfo auditInfo = AuditInfo.builder().organizationId(ORGANIZATION).environmentId(ENVIRONMENT).build();

    @AfterEach
    void tearDown() {
        groupQueryServiceInMemory.reset();
        reset(groupService, membershipService, userService);
    }

    @Override
    protected String contextPath() {
        return "/organizations/" + ORGANIZATION + "/environments/" + ENVIRONMENT + "/groups";
    }

    private String groupIdFromHrid(String hrid) {
        return HRIDToUUID.group().context(new ExecutionContext(ORGANIZATION, ENVIRONMENT)).hrid(hrid).id();
    }

    private void givenExistingGroup() {
        groupQueryServiceInMemory.initWith(
            List.of(
                Group.builder()
                    .id(groupIdFromHrid(HRID))
                    .name("My Group")
                    .environmentId(ENVIRONMENT)
                    .disableMembershipNotifications(false)
                    .build()
            )
        );
    }

    private void givenExistingGroupWithLegacyId() {
        groupQueryServiceInMemory.initWith(
            List.of(Group.builder().id(GUID).name("Legacy Group").environmentId(ENVIRONMENT).disableMembershipNotifications(false).build())
        );
    }

    @Nested
    class Get {

        @Test
        void should_get_group_from_known_hrid() {
            givenExistingGroup();
            givenNoMembers();

            try (var response = rootTarget().path(HRID).request().get()) {
                assertThat(response.getStatus()).isEqualTo(200);
                var state = response.readEntity(GroupState.class);
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(state.getStatus().getId()).isEqualTo(groupIdFromHrid(HRID));
                    soft.assertThat(state.getSpec().getName()).isEqualTo("My Group");
                    soft.assertThat(state.getSpec().getNotifyMembers()).isTrue();
                });
            }
        }

        @Test
        void should_get_group_from_known_guid() {
            givenExistingGroupWithLegacyId();
            givenNoMembers();

            try (var response = rootTarget().path(GUID).queryParam("hridContainsUUID", true).request().get()) {
                assertThat(response.getStatus()).isEqualTo(200);
                var state = response.readEntity(GroupState.class);
                assertThat(state.getStatus().getId()).isEqualTo(GUID);
            }
        }

        @Test
        void should_get_group_with_members() {
            givenExistingGroup();
            givenGroupMembers();

            try (var response = rootTarget().path(HRID).request().get()) {
                assertThat(response.getStatus()).isEqualTo(200);
                var state = response.readEntity(GroupState.class);
                SoftAssertions.assertSoftly(soft -> {
                    soft.assertThat(state.getSpec().getMembers()).hasSize(1);
                    soft.assertThat(state.getStatus().getMembers()).isEqualTo(1L);
                });
            }
        }

        @Test
        void should_return_a_404_status_code_with_unknown_hrid() {
            try (var response = rootTarget().path("unknown").request().get()) {
                assertThat(response.getStatus()).isEqualTo(404);
            }
        }

        private void givenNoMembers() {
            when(membershipService.getMembersByReference(any(), eq(MembershipReferenceType.GROUP), any())).thenReturn(Set.of());
        }

        private void givenGroupMembers() {
            var groupId = groupIdFromHrid(HRID);
            var role = RoleEntity.builder().scope(RoleScope.API).name("USER").build();
            var member = MemberEntity.builder()
                .id("user-1")
                .type(MembershipMemberType.USER)
                .referenceType(MembershipReferenceType.GROUP)
                .referenceId(groupId)
                .roles(List.of(role))
                .build();

            when(membershipService.getMembersByReference(any(), eq(MembershipReferenceType.GROUP), eq(groupId))).thenReturn(Set.of(member));

            var user = new UserEntity();
            user.setId("user-1");
            user.setSource("gravitee");
            user.setSourceId("user-source-1");

            when(userService.findByIds(any(), eq(Set.of("user-1")))).thenReturn(Set.of(user));
        }
    }

    @Nested
    class Delete {

        @Test
        void should_delete_group_and_return_no_content() {
            try (var response = rootTarget().path(HRID).request().delete()) {
                assertThat(response.getStatus()).isEqualTo(204);
            }

            verify(groupService).delete(any(), eq(groupIdFromHrid(HRID)));
        }

        @Test
        void should_delete_group_with_guid() {
            try (var response = rootTarget().path(GUID).queryParam("hridContainsUUID", true).request().delete()) {
                assertThat(response.getStatus()).isEqualTo(204);
            }

            verify(groupService).delete(any(), eq(GUID));
        }

        @Test
        void should_return_a_404_status_code_with_unknown_hrid() {
            doThrow(new GroupNotFoundException("unknown")).when(groupService).delete(any(), eq(groupIdFromHrid("unknown")));

            try (var response = rootTarget().path("unknown").request().delete()) {
                assertThat(response.getStatus()).isEqualTo(404);
            }
        }
    }
}
