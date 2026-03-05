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
package io.gravitee.apim.core.application_member.use_case;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import inmemory.MemberQueryServiceInMemory;
import inmemory.MembershipDomainServiceInMemory;
import inmemory.RoleQueryServiceInMemory;
import io.gravitee.apim.core.member.model.Member;
import io.gravitee.apim.core.membership.exception.RoleNotFoundException;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.exceptions.MembershipAlreadyExistsException;
import io.gravitee.rest.api.service.exceptions.SinglePrimaryOwnerException;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AddApplicationMemberUseCaseTest {

    private static final String APPLICATION_ID = "application-id";
    private static final String ENVIRONMENT_ID = "environment-id";
    private static final String ORGANIZATION_ID = "organization-id";

    private final MembershipDomainServiceInMemory membershipDomainService = new MembershipDomainServiceInMemory();
    private final RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();
    private final MemberQueryServiceInMemory memberQueryService = new MemberQueryServiceInMemory();

    private AddApplicationMemberUseCase addApplicationMemberUseCase;

    @BeforeEach
    void setUp() {
        membershipDomainService.reset();
        roleQueryService.reset();
        memberQueryService.reset();
        addApplicationMemberUseCase = new AddApplicationMemberUseCase(membershipDomainService, roleQueryService, memberQueryService);
    }

    @Test
    void should_add_single_application_member() {
        roleQueryService.initWith(List.of(aRole("USER", ORGANIZATION_ID)));

        var result = addApplicationMemberUseCase.execute(
            new AddApplicationMemberUseCase.Input(
                APPLICATION_ID,
                List.of(new AddApplicationMemberUseCase.AddMemberRequest("member-1", "ref-1", "USER")),
                true,
                ENVIRONMENT_ID,
                ORGANIZATION_ID
            )
        );

        assertThat(result.createdMembers()).hasSize(1);
        assertThat(result.createdMembers().get(0).getId()).isEqualTo("member-1");
        assertThat(result.createdMembers().get(0).getRoles()).extracting(Member.Role::getName).containsExactly("USER");
        assertThat(membershipDomainService.storage()).hasSize(1);
        assertThat(membershipDomainService.storage().get(0).getId()).isEqualTo("member-1");
    }

    @Test
    void should_add_application_members_in_batch() {
        roleQueryService.initWith(List.of(aRole("USER", ORGANIZATION_ID), aRole("ADMIN", ORGANIZATION_ID)));

        var result = addApplicationMemberUseCase.execute(
            new AddApplicationMemberUseCase.Input(
                APPLICATION_ID,
                List.of(
                    new AddApplicationMemberUseCase.AddMemberRequest("member-1", "ref-1", "USER"),
                    new AddApplicationMemberUseCase.AddMemberRequest("member-2", "ref-2", "ADMIN")
                ),
                false,
                ENVIRONMENT_ID,
                ORGANIZATION_ID
            )
        );

        assertThat(result.createdMembers()).hasSize(2);
        assertThat(result.createdMembers()).extracting(Member::getId).containsExactly("member-1", "member-2");
        assertThat(membershipDomainService.storage()).hasSize(2);
    }

    @Test
    void should_throw_when_member_already_exists() {
        roleQueryService.initWith(List.of(aRole("USER", ORGANIZATION_ID)));
        memberQueryService.initWith(List.of(aMember("member-1", "USER")));

        assertThrows(MembershipAlreadyExistsException.class, () ->
            addApplicationMemberUseCase.execute(
                new AddApplicationMemberUseCase.Input(
                    APPLICATION_ID,
                    List.of(new AddApplicationMemberUseCase.AddMemberRequest("member-1", "ref-1", "USER")),
                    true,
                    ENVIRONMENT_ID,
                    ORGANIZATION_ID
                )
            )
        );
    }

    @Test
    void should_throw_when_role_is_primary_owner() {
        assertThrows(SinglePrimaryOwnerException.class, () ->
            addApplicationMemberUseCase.execute(
                new AddApplicationMemberUseCase.Input(
                    APPLICATION_ID,
                    List.of(new AddApplicationMemberUseCase.AddMemberRequest("member-1", "ref-1", "PRIMARY_OWNER")),
                    true,
                    ENVIRONMENT_ID,
                    ORGANIZATION_ID
                )
            )
        );
    }

    @Test
    void should_throw_when_role_not_found() {
        assertThrows(RoleNotFoundException.class, () ->
            addApplicationMemberUseCase.execute(
                new AddApplicationMemberUseCase.Input(
                    APPLICATION_ID,
                    List.of(new AddApplicationMemberUseCase.AddMemberRequest("member-1", "ref-1", "UNKNOWN")),
                    true,
                    ENVIRONMENT_ID,
                    ORGANIZATION_ID
                )
            )
        );
    }

    private static Member aMember(String id, String roleName) {
        return Member.builder()
            .id(id)
            .displayName("Member")
            .email("member@gravitee.io")
            .type(MembershipMemberType.USER)
            .referenceType(MembershipReferenceType.APPLICATION)
            .referenceId(APPLICATION_ID)
            .createdAt(new Date())
            .updatedAt(new Date())
            .roles(List.of(Member.Role.builder().scope(RoleScope.APPLICATION).name(roleName).build()))
            .build();
    }

    private static Role aRole(String roleName, String organizationId) {
        return Role.builder()
            .id("role-" + roleName.toLowerCase())
            .name(roleName)
            .scope(Role.Scope.APPLICATION)
            .referenceType(Role.ReferenceType.ORGANIZATION)
            .referenceId(organizationId)
            .build();
    }
}
