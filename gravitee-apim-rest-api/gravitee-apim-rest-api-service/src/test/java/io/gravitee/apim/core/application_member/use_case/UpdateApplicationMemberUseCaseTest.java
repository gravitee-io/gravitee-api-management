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
import inmemory.RoleQueryServiceInMemory;
import io.gravitee.apim.core.exception.NotFoundDomainException;
import io.gravitee.apim.core.member.model.Member;
import io.gravitee.apim.core.membership.exception.RoleNotFoundException;
import io.gravitee.apim.core.membership.model.Role;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.exceptions.SinglePrimaryOwnerException;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateApplicationMemberUseCaseTest {

    private static final String APPLICATION_ID = "application-id";
    private static final String ORGANIZATION_ID = "organization-id";
    private static final String MEMBER_ID = "member-id";

    private final MemberQueryServiceInMemory memberQueryService = new MemberQueryServiceInMemory();
    private final RoleQueryServiceInMemory roleQueryService = new RoleQueryServiceInMemory();

    private UpdateApplicationMemberUseCase updateApplicationMemberUseCase;

    @BeforeEach
    void setUp() {
        updateApplicationMemberUseCase = new UpdateApplicationMemberUseCase(memberQueryService, roleQueryService);
        memberQueryService.reset();
        roleQueryService.reset();
    }

    @Test
    void should_update_application_member_role() {
        memberQueryService.initWith(List.of(aMember(MEMBER_ID, "USER")));
        roleQueryService.initWith(List.of(aRole("ADMIN", ORGANIZATION_ID)));

        var result = updateApplicationMemberUseCase.execute(
            new UpdateApplicationMemberUseCase.Input(APPLICATION_ID, MEMBER_ID, "ADMIN", ORGANIZATION_ID)
        );

        assertThat(result.updatedMember().getRoles()).hasSize(1);
        assertThat(result.updatedMember().getRoles().get(0).getName()).isEqualTo("ADMIN");
        assertThat(result.updatedMember().getRoles().get(0).getScope()).isEqualTo(RoleScope.APPLICATION);
    }

    @Test
    void should_throw_when_role_not_found() {
        memberQueryService.initWith(List.of(aMember(MEMBER_ID, "USER")));

        assertThrows(RoleNotFoundException.class, () ->
            updateApplicationMemberUseCase.execute(
                new UpdateApplicationMemberUseCase.Input(APPLICATION_ID, MEMBER_ID, "UNKNOWN", ORGANIZATION_ID)
            )
        );
    }

    @Test
    void should_throw_when_role_is_primary_owner() {
        assertThrows(SinglePrimaryOwnerException.class, () ->
            updateApplicationMemberUseCase.execute(
                new UpdateApplicationMemberUseCase.Input(APPLICATION_ID, MEMBER_ID, "PRIMARY_OWNER", ORGANIZATION_ID)
            )
        );
    }

    @Test
    void should_throw_when_member_not_found() {
        roleQueryService.initWith(List.of(aRole("ADMIN", ORGANIZATION_ID)));

        assertThrows(NotFoundDomainException.class, () ->
            updateApplicationMemberUseCase.execute(
                new UpdateApplicationMemberUseCase.Input(APPLICATION_ID, MEMBER_ID, "ADMIN", ORGANIZATION_ID)
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
