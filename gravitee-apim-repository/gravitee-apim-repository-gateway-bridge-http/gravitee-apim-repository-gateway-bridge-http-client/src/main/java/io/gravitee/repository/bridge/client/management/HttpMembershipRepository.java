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
package io.gravitee.repository.bridge.client.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipMemberType;
import io.gravitee.repository.management.model.MembershipReferenceType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class HttpMembershipRepository extends AbstractRepository implements MembershipRepository {

    @Override
    public Membership create(Membership membership) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Membership update(Membership membership) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public void delete(String membershipId) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Optional<Membership> findById(String membershipId) throws TechnicalException {
        throw new IllegalStateException();
    }

    public void deleteMembers(MembershipReferenceType referenceType, String referenceId) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<Membership> findByIds(Set<String> membershipIds) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<Membership> findByReferenceAndRoleId(MembershipReferenceType referenceType, String referenceId, String roleId)
        throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<Membership> findByReferencesAndRoleId(MembershipReferenceType referenceType, List<String> referenceIds, String roleId)
        throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<Membership> findByRoleId(String roleId) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<Membership> findByMemberIdAndMemberTypeAndReferenceType(
        String memberId,
        MembershipMemberType memberType,
        MembershipReferenceType referenceType
    ) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<Membership> findByMemberIdAndMemberTypeAndReferenceTypeAndSource(
        String memberId,
        MembershipMemberType memberType,
        MembershipReferenceType referenceType,
        String sourceId
    ) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<Membership> findByMemberIdAndMemberTypeAndReferenceTypeAndRoleId(
        String memberId,
        MembershipMemberType memberType,
        MembershipReferenceType referenceType,
        String roleId
    ) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<Membership> findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceIdAndRoleId(
        String memberId,
        MembershipMemberType memberType,
        MembershipReferenceType referenceType,
        String referenceId,
        String roleId
    ) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<Membership> findByMemberIdAndMemberTypeAndReferenceTypeAndRoleIdIn(
        String memberId,
        MembershipMemberType memberType,
        MembershipReferenceType referenceType,
        Collection<String> roleIds
    ) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<Membership> findByMemberIdAndMemberType(String memberId, MembershipMemberType memberType) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<Membership> findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
        String memberId,
        MembershipMemberType memberType,
        MembershipReferenceType referenceType,
        String referenceId
    ) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<Membership> findByMemberIdsAndMemberTypeAndReferenceType(
        List<String> memberIds,
        MembershipMemberType memberType,
        MembershipReferenceType referenceType
    ) throws TechnicalException {
        throw new IllegalStateException();
    }

    @Override
    public Set<Membership> findAll() throws TechnicalException {
        throw new IllegalStateException();
    }
}
