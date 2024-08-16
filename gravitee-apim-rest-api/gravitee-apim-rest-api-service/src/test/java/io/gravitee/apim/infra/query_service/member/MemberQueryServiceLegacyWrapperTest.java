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
package io.gravitee.apim.infra.query_service.member;

import static org.mockito.Mockito.verify;

import io.gravitee.apim.core.member.model.MembershipMember;
import io.gravitee.apim.core.member.model.MembershipReference;
import io.gravitee.apim.core.member.model.MembershipReferenceType;
import io.gravitee.apim.core.member.model.MembershipRole;
import io.gravitee.apim.core.member.model.RoleScope;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.UuidString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class MemberQueryServiceLegacyWrapperTest {

    @Mock
    MembershipService membershipService;

    MemberQueryServiceLegacyWrapper service;

    private static final MembershipReferenceType MEMBERSHIP_REFERENCE_TYPE = MembershipReferenceType.APPLICATION;
    private static final String REFERENCE_ID = UuidString.generateRandom();
    private static final String USER_ID = UuidString.generateRandom();
    private static final MembershipReference MEMBERSHIP_REFERENCE = new MembershipReference(MEMBERSHIP_REFERENCE_TYPE, USER_ID);
    private static final MembershipMember MEMBERSHIP_MEMBER = new MembershipMember(
        USER_ID,
        REFERENCE_ID,
        io.gravitee.apim.core.member.model.MembershipMemberType.USER
    );
    private static final MembershipRole MEMBERSHIP_ROLE = new MembershipRole(RoleScope.APPLICATION, "test");

    @BeforeEach
    void setUp() {
        service = new MemberQueryServiceLegacyWrapper(membershipService);
    }

    @Test
    public void getMembersByReference() {
        service.getMembersByReference(MEMBERSHIP_REFERENCE_TYPE, REFERENCE_ID);

        verify(membershipService)
            .getMembersByReference(
                GraviteeContext.getExecutionContext(),
                io.gravitee.rest.api.model.MembershipReferenceType.valueOf(MEMBERSHIP_REFERENCE_TYPE.name()),
                REFERENCE_ID
            );
    }

    @Test
    public void getUserMember() {
        service.getUserMember(MEMBERSHIP_REFERENCE_TYPE, REFERENCE_ID, USER_ID);

        verify(membershipService)
            .getUserMember(
                GraviteeContext.getExecutionContext(),
                io.gravitee.rest.api.model.MembershipReferenceType.valueOf(MEMBERSHIP_REFERENCE_TYPE.name()),
                REFERENCE_ID,
                USER_ID
            );
    }

    @Test
    public void updateRoleToMemberOnReference() {
        service.updateRoleToMemberOnReference(MEMBERSHIP_REFERENCE, MEMBERSHIP_MEMBER, MEMBERSHIP_ROLE);

        verify(membershipService)
            .updateRoleToMemberOnReference(
                GraviteeContext.getExecutionContext(),
                new MembershipService.MembershipReference(io.gravitee.rest.api.model.MembershipReferenceType.APPLICATION, USER_ID),
                new MembershipService.MembershipMember(USER_ID, REFERENCE_ID, MembershipMemberType.USER),
                new MembershipService.MembershipRole(io.gravitee.rest.api.model.permissions.RoleScope.APPLICATION, "test")
            );
    }

    @Test
    public void addRoleToMemberOnReference() {
        service.addRoleToMemberOnReference(MEMBERSHIP_REFERENCE, MEMBERSHIP_MEMBER, MEMBERSHIP_ROLE);

        verify(membershipService)
            .addRoleToMemberOnReference(
                GraviteeContext.getExecutionContext(),
                new MembershipService.MembershipReference(io.gravitee.rest.api.model.MembershipReferenceType.APPLICATION, USER_ID),
                new MembershipService.MembershipMember(USER_ID, REFERENCE_ID, MembershipMemberType.USER),
                new MembershipService.MembershipRole(io.gravitee.rest.api.model.permissions.RoleScope.APPLICATION, "test")
            );
    }

    @Test
    public void deleteReferenceMember() {
        service.deleteReferenceMember(
            MEMBERSHIP_REFERENCE_TYPE,
            REFERENCE_ID,
            io.gravitee.apim.core.member.model.MembershipMemberType.USER,
            USER_ID
        );

        verify(membershipService)
            .deleteReferenceMember(
                GraviteeContext.getExecutionContext(),
                io.gravitee.rest.api.model.MembershipReferenceType.APPLICATION,
                REFERENCE_ID,
                MembershipMemberType.USER,
                USER_ID
            );
    }
}
