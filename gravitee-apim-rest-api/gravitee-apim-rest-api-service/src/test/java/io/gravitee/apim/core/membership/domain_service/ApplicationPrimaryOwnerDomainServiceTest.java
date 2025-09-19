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
package io.gravitee.apim.core.membership.domain_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import inmemory.GroupQueryServiceInMemory;
import inmemory.InMemoryAlternative;
import inmemory.MembershipQueryServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import inmemory.UserCrudServiceInMemory;
import io.gravitee.apim.core.group.model.Group;
import io.gravitee.apim.core.membership.exception.ApplicationPrimaryOwnerNotFoundException;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.model.PrimaryOwnerEntity;
import io.gravitee.apim.core.user.model.BaseUserEntity;
import java.util.List;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApplicationPrimaryOwnerDomainServiceTest {

    public static final String ORGANIZATION_ID = "DEFAULT";
    public static final String APPLICATION_PRIMARY_OWNER_ROLE_ID = "app-po-id-DEFAULT";

    GroupQueryServiceInMemory groupQueryService = new GroupQueryServiceInMemory();
    MembershipQueryServiceInMemory membershipQueryService = new MembershipQueryServiceInMemory();
    RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();
    UserCrudServiceInMemory userCrudService = new UserCrudServiceInMemory();

    ApplicationPrimaryOwnerDomainService service;

    @BeforeEach
    void setUp() {
        service = new ApplicationPrimaryOwnerDomainService(groupQueryService, membershipQueryService, roleQueryService, userCrudService);

        roleQueryService.resetSystemRoles(ORGANIZATION_ID);
    }

    @AfterEach
    void tearDown() {
        Stream.of(membershipQueryService, roleQueryService, userCrudService).forEach(InMemoryAlternative::reset);
    }

    @Nested
    class GetApiPrimaryOwner {

        @Test
        @SneakyThrows
        public void should_return_a_user_primary_owner_of_an_application() {
            givenExistingUsers(
                List.of(BaseUserEntity.builder().id("user-id").firstname("Jane").lastname("Doe").email("jane.doe@gravitee.io").build())
            );
            givenExistingMemberships(
                List.of(
                    Membership.builder()
                        .referenceType(Membership.ReferenceType.APPLICATION)
                        .referenceId("application-id")
                        .memberType(Membership.Type.USER)
                        .memberId("user-id")
                        .roleId(APPLICATION_PRIMARY_OWNER_ROLE_ID)
                        .build()
                )
            );

            var result = service.getApplicationPrimaryOwner(ORGANIZATION_ID, "application-id");

            Assertions.assertThat(result).isEqualTo(
                PrimaryOwnerEntity.builder()
                    .id("user-id")
                    .displayName("Jane Doe")
                    .email("jane.doe@gravitee.io")
                    .type(PrimaryOwnerEntity.Type.USER)
                    .build()
            );
        }

        @Test
        @SneakyThrows
        public void should_return_a_group_primary_owner_of_an_api() {
            givenExistingUsers(
                List.of(BaseUserEntity.builder().id("user-id").firstname("Jane").lastname("Doe").email("jane.doe@gravitee.io").build())
            );
            givenExistingGroup(List.of(Group.builder().id("group-id").name("Group name").build()));
            givenExistingMemberships(
                List.of(
                    Membership.builder()
                        .referenceType(Membership.ReferenceType.APPLICATION)
                        .referenceId("application-id")
                        .memberType(Membership.Type.GROUP)
                        .memberId("group-id")
                        .roleId(APPLICATION_PRIMARY_OWNER_ROLE_ID)
                        .build(),
                    Membership.builder()
                        .referenceType(Membership.ReferenceType.GROUP)
                        .referenceId("group-id")
                        .memberType(Membership.Type.USER)
                        .memberId("user-id")
                        .roleId(APPLICATION_PRIMARY_OWNER_ROLE_ID)
                        .build()
                )
            );

            var result = service.getApplicationPrimaryOwner(ORGANIZATION_ID, "application-id");

            Assertions.assertThat(result).isEqualTo(
                PrimaryOwnerEntity.builder()
                    .id("group-id")
                    .displayName("Group name")
                    .email("jane.doe@gravitee.io")
                    .type(PrimaryOwnerEntity.Type.GROUP)
                    .build()
            );
        }

        @Test
        @SneakyThrows
        public void should_return_a_group_primary_owner_with_no_email_when_group_has_no_users() {
            givenExistingGroup(List.of(Group.builder().id("group-id").name("Group name").build()));
            givenExistingMemberships(
                List.of(
                    Membership.builder()
                        .referenceType(Membership.ReferenceType.APPLICATION)
                        .referenceId("application-id")
                        .memberType(Membership.Type.GROUP)
                        .memberId("group-id")
                        .roleId(APPLICATION_PRIMARY_OWNER_ROLE_ID)
                        .build()
                )
            );

            var result = service.getApplicationPrimaryOwner(ORGANIZATION_ID, "application-id");

            Assertions.assertThat(result).isEqualTo(
                PrimaryOwnerEntity.builder().id("group-id").displayName("Group name").type(PrimaryOwnerEntity.Type.GROUP).build()
            );
        }

        @Test
        public void should_return_no_user_primary_owner_found() {
            givenExistingUsers(List.of());
            givenExistingMemberships(
                List.of(
                    Membership.builder()
                        .referenceType(Membership.ReferenceType.APPLICATION)
                        .referenceId("application-id")
                        .memberType(Membership.Type.USER)
                        .memberId("user-id")
                        .roleId(APPLICATION_PRIMARY_OWNER_ROLE_ID)
                        .build()
                )
            );

            Throwable throwable = catchThrowable(() -> service.getApplicationPrimaryOwner(ORGANIZATION_ID, "application-id"));

            assertThat(throwable).isInstanceOf(ApplicationPrimaryOwnerNotFoundException.class);
        }
    }

    private void givenExistingUsers(List<BaseUserEntity> users) {
        userCrudService.initWith(users);
    }

    private void givenExistingMemberships(List<Membership> memberships) {
        membershipQueryService.initWith(memberships);
    }

    @SneakyThrows
    private void givenExistingGroup(List<Group> groups) {
        groupQueryService.initWith(groups);
    }
}
