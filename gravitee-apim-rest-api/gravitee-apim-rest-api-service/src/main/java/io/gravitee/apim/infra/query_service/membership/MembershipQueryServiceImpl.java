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
package io.gravitee.apim.infra.query_service.membership;

import io.gravitee.apim.core.exception.TechnicalDomainException;
import io.gravitee.apim.core.membership.model.Membership;
import io.gravitee.apim.core.membership.query_service.MembershipQueryService;
import io.gravitee.apim.infra.adapter.MembershipAdapter;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.MembershipMemberType;
import io.gravitee.repository.management.model.MembershipReferenceType;
import java.util.Collection;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class MembershipQueryServiceImpl implements MembershipQueryService {

    private final MembershipRepository membershipRepository;

    public MembershipQueryServiceImpl(@Lazy MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @Override
    public Collection<Membership> findByReferenceAndRoleId(Membership.ReferenceType referenceType, String referenceId, String roleId) {
        try {
            return membershipRepository
                .findByReferenceAndRoleId(MembershipReferenceType.valueOf(referenceType.name()), referenceId, roleId)
                .stream()
                .map(MembershipAdapter.INSTANCE::toEntity)
                .toList();
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(String.format("An error occurs while trying to find %s membership", referenceType), e);
        }
    }

    @Override
    public Collection<Membership> findByReferencesAndRoleId(
        Membership.ReferenceType referenceType,
        List<String> referenceIds,
        String roleId
    ) {
        try {
            return membershipRepository
                .findByReferencesAndRoleId(MembershipReferenceType.valueOf(referenceType.name()), referenceIds, roleId)
                .stream()
                .map(MembershipAdapter.INSTANCE::toEntity)
                .toList();
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(String.format("An error occurs while trying to find %s membership", referenceType), e);
        }
    }

    @Override
    public Collection<Membership> findByReference(Membership.ReferenceType referenceType, String referenceId) {
        return findByReferenceAndRoleId(referenceType, referenceId, null);
    }

    @Override
    public List<String> findClustersIdsThatUserBelongsTo(String memberId) {
        try {
            return membershipRepository
                .findByMemberIdAndMemberTypeAndReferenceType(memberId, MembershipMemberType.USER, MembershipReferenceType.CLUSTER)
                .stream()
                .map(io.gravitee.repository.management.model.Membership::getReferenceId)
                .toList();
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                String.format("An error occured while trying to find clusters ids of member %s", memberId),
                e
            );
        }
    }

    @Override
    public Collection<Membership> findGroupsThatUserBelongsTo(String memberId) {
        try {
            return membershipRepository
                .findByMemberIdAndMemberTypeAndReferenceType(memberId, MembershipMemberType.USER, MembershipReferenceType.GROUP)
                .stream()
                .map(MembershipAdapter.INSTANCE::toEntity)
                .toList();
        } catch (TechnicalException e) {
            throw new TechnicalDomainException(
                String.format("An error occurs while trying to find Group memberships of member %s", memberId),
                e
            );
        }
    }
}
