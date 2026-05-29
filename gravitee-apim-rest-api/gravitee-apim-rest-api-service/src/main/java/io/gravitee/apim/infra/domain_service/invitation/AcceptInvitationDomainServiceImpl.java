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

import io.gravitee.apim.core.invitation.domain_service.AcceptInvitationDomainService;
import io.gravitee.apim.core.invitation.model.ApplicationInvitation;
import io.gravitee.apim.core.invitation.model.GroupInvitation;
import io.gravitee.apim.core.invitation.model.Invitation;
import io.gravitee.apim.core.member.model.MembershipReferenceType;
import io.gravitee.apim.core.membership.domain_service.MembershipDomainService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AcceptInvitationDomainServiceImpl implements AcceptInvitationDomainService {

    private final MembershipDomainService membershipDomainService;

    @Override
    public void addMember(ExecutionContext executionContext, Invitation invitation, String userId) {
        switch (invitation) {
            case GroupInvitation g -> membershipDomainService.addGroupMemberships(
                executionContext,
                g.referenceId(),
                userId,
                g.apiRole(),
                g.applicationRole()
            );
            case ApplicationInvitation a -> membershipDomainService.createNewMembership(
                executionContext,
                MembershipReferenceType.APPLICATION,
                a.applicationId(),
                userId,
                null,
                a.roleName()
            );
        }
    }
}
