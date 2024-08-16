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

import io.gravitee.apim.core.member.model.Member;
import io.gravitee.apim.core.member.model.MembershipMember;
import io.gravitee.apim.core.member.model.MembershipMemberType;
import io.gravitee.apim.core.member.model.MembershipReference;
import io.gravitee.apim.core.member.model.MembershipReferenceType;
import io.gravitee.apim.core.member.model.MembershipRole;
import io.gravitee.apim.core.member.query_service.MemberQueryService;
import io.gravitee.apim.infra.adapter.MemberAdapter;
import io.gravitee.apim.infra.adapter.MembershipMemberAdapter;
import io.gravitee.apim.infra.adapter.MembershipReferenceAdapter;
import io.gravitee.apim.infra.adapter.MembershipRoleAdapter;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@Service
public class MemberQueryServiceLegacyWrapper implements MemberQueryService {

    private final MembershipService membershipService;

    public MemberQueryServiceLegacyWrapper(MembershipService membershipService) {
        this.membershipService = membershipService;
    }

    @Override
    public Set<Member> getMembersByReference(MembershipReferenceType referenceType, String referenceId) {
        return membershipService
            .getMembersByReference(
                GraviteeContext.getExecutionContext(),
                io.gravitee.rest.api.model.MembershipReferenceType.valueOf(referenceType.name()),
                referenceId
            )
            .stream()
            .map(MemberAdapter.INSTANCE::toMember)
            .collect(Collectors.toSet());
    }

    @Override
    public Member getUserMember(MembershipReferenceType referenceType, String referenceId, String userId) {
        var userMember = membershipService.getUserMember(
            GraviteeContext.getExecutionContext(),
            io.gravitee.rest.api.model.MembershipReferenceType.valueOf(referenceType.name()),
            referenceId,
            userId
        );
        return MemberAdapter.INSTANCE.toMember(userMember);
    }

    @Override
    public Member updateRoleToMemberOnReference(MembershipReference reference, MembershipMember member, MembershipRole role) {
        var memberEntity = membershipService.updateRoleToMemberOnReference(
            GraviteeContext.getExecutionContext(),
            MembershipReferenceAdapter.INSTANCE.map(reference),
            MembershipMemberAdapter.INSTANCE.map(member),
            MembershipRoleAdapter.INSTANCE.map(role)
        );
        return MemberAdapter.INSTANCE.toMember(memberEntity);
    }

    @Override
    public void addRoleToMemberOnReference(MembershipReference reference, MembershipMember member, MembershipRole role) {
        membershipService.addRoleToMemberOnReference(
            GraviteeContext.getExecutionContext(),
            MembershipReferenceAdapter.INSTANCE.map(reference),
            MembershipMemberAdapter.INSTANCE.map(member),
            MembershipRoleAdapter.INSTANCE.map(role)
        );
    }

    @Override
    public void deleteReferenceMember(
        MembershipReferenceType referenceType,
        String referenceId,
        MembershipMemberType memberType,
        String memberId
    ) {
        membershipService.deleteReferenceMember(
            GraviteeContext.getExecutionContext(),
            io.gravitee.rest.api.model.MembershipReferenceType.valueOf(referenceType.name()),
            referenceId,
            io.gravitee.rest.api.model.MembershipMemberType.valueOf(memberType.name()),
            memberId
        );
    }
}
