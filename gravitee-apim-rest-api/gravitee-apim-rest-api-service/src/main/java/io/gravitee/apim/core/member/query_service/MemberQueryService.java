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
package io.gravitee.apim.core.member.query_service;

import io.gravitee.apim.core.member.model.Member;
import io.gravitee.apim.core.member.model.MembershipMember;
import io.gravitee.apim.core.member.model.MembershipMemberType;
import io.gravitee.apim.core.member.model.MembershipReference;
import io.gravitee.apim.core.member.model.MembershipReferenceType;
import io.gravitee.apim.core.member.model.MembershipRole;
import java.util.Set;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface MemberQueryService {
    Set<Member> getMembersByReference(MembershipReferenceType referenceType, String referenceId);

    Member getUserMember(MembershipReferenceType referenceType, String referenceId, String userId);

    Member updateRoleToMemberOnReference(MembershipReference reference, MembershipMember member, MembershipRole role);

    void addRoleToMemberOnReference(MembershipReference reference, MembershipMember member, MembershipRole role);

    void deleteReferenceMember(MembershipReferenceType referenceType, String referenceId, MembershipMemberType memberType, String memberId);
}
