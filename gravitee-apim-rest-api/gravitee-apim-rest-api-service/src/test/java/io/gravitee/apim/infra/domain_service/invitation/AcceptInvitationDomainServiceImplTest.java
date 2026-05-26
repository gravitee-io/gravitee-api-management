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
package io.gravitee.apim.infra.domain_service.invitation;

import static fixtures.core.model.ApplicationInvitationFixtures.anApplicationInvitation;
import static fixtures.core.model.GroupInvitationFixtures.aGroupInvitation;
import static org.mockito.Mockito.verify;

import io.gravitee.apim.core.member.model.MembershipReferenceType;
import io.gravitee.apim.core.membership.domain_service.MembershipDomainService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AcceptInvitationDomainServiceImplTest {

    private static final ExecutionContext EXECUTION_CONTEXT = new ExecutionContext("org-id", "env-id");
    private static final String USER_ID = "user-id";
    private static final String INVITATION_ID = "00000000-0000-0000-0000-000000000001";

    @Mock
    MembershipDomainService membershipDomainService;

    AcceptInvitationDomainServiceImpl cut;

    @BeforeEach
    void setUp() {
        cut = new AcceptInvitationDomainServiceImpl(membershipDomainService);
    }

    @Test
    void should_add_member_for_group_invitation() {
        var invitation = aGroupInvitation(INVITATION_ID, "group-1", "alice@example.com", "API_USER", "APP_USER");

        cut.addMember(EXECUTION_CONTEXT, invitation, USER_ID);

        verify(membershipDomainService).addGroupMemberships(EXECUTION_CONTEXT, "group-1", USER_ID, "API_USER", "APP_USER");
    }

    @Test
    void should_add_member_for_application_invitation() {
        var invitation = anApplicationInvitation(INVITATION_ID, "app-1", "alice@example.com", "USER");

        cut.addMember(EXECUTION_CONTEXT, invitation, USER_ID);

        verify(membershipDomainService).createNewMembership(EXECUTION_CONTEXT, MembershipReferenceType.APPLICATION, "app-1", USER_ID, null, "USER");
    }
}
