/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.repository.proxy;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipMemberType;
import io.gravitee.repository.management.model.MembershipReferenceType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com) 
 * @author GraviteeSource Team
 */
@Component
public class MembershipRepositoryProxy extends AbstractProxy<MembershipRepository> implements MembershipRepository {
    @Override
    public Membership create(Membership membership) throws TechnicalException {
        return target.create(membership);
    }

    @Override
    public Membership update(Membership membership) throws TechnicalException {
        return target.update(membership);
    }

    @Override
    public void delete(String membershipId) throws TechnicalException {
        target.delete(membershipId);
    }

    @Override
    public void deleteMembers(MembershipReferenceType referenceType, String referenceId) throws TechnicalException {
        target.deleteMembers(referenceType, referenceId);
    }

    @Override
    public Set<Membership> findByIds(Set<String> membershipIds) throws TechnicalException {
        return target.findByIds(membershipIds);
    }

    @Override
    public Optional<Membership> findById(String membershipId) throws TechnicalException {
        return target.findById(membershipId);
    }

    @Override
    public Set<Membership> findByReferenceAndRoleId(MembershipReferenceType referenceType, String referenceId, String roleId) throws TechnicalException {
        return target.findByReferenceAndRoleId(referenceType, referenceId, roleId);
    }

    @Override
    public Set<Membership> findByMemberIdAndMemberTypeAndReferenceType(String memberId, MembershipMemberType memberType, MembershipReferenceType referenceType) throws TechnicalException {
        return target.findByMemberIdAndMemberTypeAndReferenceType(memberId, memberType, referenceType);
    }

    @Override
    public Set<Membership> findByMemberIdAndMemberTypeAndReferenceTypeAndRoleId(String memberId, MembershipMemberType memberType, MembershipReferenceType referenceType, String roleId) throws TechnicalException {
        return target.findByMemberIdAndMemberTypeAndReferenceTypeAndRoleId(memberId, memberType, referenceType, roleId);
    }

    @Override
    public Set<Membership> findByReferencesAndRoleId(MembershipReferenceType referenceType, List<String> referenceIds, String roleId) throws TechnicalException {
        return target.findByReferencesAndRoleId(referenceType, referenceIds, roleId);
    }

    @Override
    public Set<Membership> findByMemberIdAndMemberType(String memberId, MembershipMemberType memberType) throws TechnicalException {
        return target.findByMemberIdAndMemberType(memberId, memberType);
    }

    @Override
    public Set<Membership> findByRoleId(String roleId) throws TechnicalException {
        return target.findByRoleId(roleId);
    }

    @Override
    public Set<Membership> findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceIdAndRoleId(String memberId, MembershipMemberType memberType, MembershipReferenceType referenceType, String referenceId, String roleId) throws TechnicalException {
        return target.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceIdAndRoleId(memberId, memberType, referenceType, referenceId, roleId);
    }

    @Override
    public Set<Membership> findByMemberIdsAndMemberTypeAndReferenceType(List<String> memberIds,
            MembershipMemberType memberType, MembershipReferenceType referenceType) throws TechnicalException {
        return target.findByMemberIdsAndMemberTypeAndReferenceType(memberIds, memberType, referenceType);
    }

    @Override
    public Set<Membership> findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(String memberId,
            MembershipMemberType memberType, MembershipReferenceType referenceType, String referenceId)
            throws TechnicalException {
        return target.findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(memberId, memberType, referenceType, referenceId);
    }
}
