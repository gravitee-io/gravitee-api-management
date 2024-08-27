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
package inmemory;

import io.gravitee.apim.core.member.model.Member;
import io.gravitee.apim.core.member.model.MembershipMember;
import io.gravitee.apim.core.member.model.MembershipMemberType;
import io.gravitee.apim.core.member.model.MembershipReference;
import io.gravitee.apim.core.member.model.MembershipReferenceType;
import io.gravitee.apim.core.member.model.MembershipRole;
import io.gravitee.apim.core.member.query_service.MemberQueryService;
import io.gravitee.common.utils.UUID;
import io.gravitee.rest.api.model.permissions.RoleScope;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MemberQueryServiceInMemory implements MemberQueryService, InMemoryAlternative<Member> {

    private final List<Member> storage = new ArrayList<>();

    @Override
    public void initWith(List<Member> items) {
        this.storage.addAll(items);
    }

    @Override
    public void reset() {
        this.storage.clear();
    }

    @Override
    public List<Member> storage() {
        return this.storage;
    }

    @Override
    public Set<Member> getMembersByReference(MembershipReferenceType referenceType, String referenceId) {
        return storage
            .stream()
            .filter(member -> member.getReferenceType().name().equals(referenceType.name()) && member.getReferenceId().equals(referenceId))
            .collect(Collectors.toSet());
    }

    @Override
    public Member getUserMember(MembershipReferenceType referenceType, String referenceId, String userId) {
        return storage
            .stream()
            .filter(member ->
                member.getReferenceType().name().equals(referenceType.name()) &&
                member.getReferenceId().equals(referenceId) &&
                member.getId().equals(userId)
            )
            .findFirst()
            .orElse(null);
    }

    @Override
    public Member updateRoleToMemberOnReference(MembershipReference reference, MembershipMember member, MembershipRole role) {
        Optional<Member> memberOptional = storage.stream().filter(m -> m.getId().equals(member.getMemberId())).findFirst();

        if (memberOptional.isPresent()) {
            memberOptional
                .get()
                .getRoles()
                .add(new Member.Role(UUID.random().toString(), null, null, RoleScope.valueOf(role.getScope().name()), false, false, null));
            return memberOptional.get();
        } else {
            return null;
        }
    }

    @Override
    public void addRoleToMemberOnReference(MembershipReference reference, MembershipMember member, MembershipRole role) {
        this.storage.stream()
            .filter(m -> m.getId().equals(member.getMemberId()))
            .findFirst()
            .ifPresent(m ->
                m
                    .getRoles()
                    .add(
                        new Member.Role(UUID.random().toString(), null, null, RoleScope.valueOf(role.getScope().name()), false, false, null)
                    )
            );
    }

    @Override
    public void deleteReferenceMember(
        MembershipReferenceType referenceType,
        String referenceId,
        MembershipMemberType memberType,
        String memberId
    ) {
        this.storage.removeIf(m -> m.getId().equals(memberId));
    }
}
