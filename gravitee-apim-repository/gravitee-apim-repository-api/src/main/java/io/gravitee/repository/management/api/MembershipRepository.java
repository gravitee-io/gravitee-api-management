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
package io.gravitee.repository.management.api;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.*;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public interface MembershipRepository extends FindAllRepository<Membership> {
    Membership create(Membership membership) throws TechnicalException;
    Membership update(Membership membership) throws TechnicalException;
    void delete(String membershipId) throws TechnicalException;

    /**
     * Delete by reference
     * @param referenceId
     * @param referenceType
     * @return List of IDs for deleted membership
     * @throws TechnicalException
     */
    List<String> deleteByReferenceIdAndReferenceType(String referenceId, MembershipReferenceType referenceType) throws TechnicalException;

    /**
     * find membership by id.
     * @param membershipId the membership id
     * @return an optional membership
     * @throws TechnicalException if something goes wrong, should never happen.
     */
    Optional<Membership> findById(String membershipId) throws TechnicalException;

    /**
     * find membership by ids.
     * @param membershipIds the membership ids
     * @return the list of memberships, or an empty set
     * @throws TechnicalException if something goes wrong, should never happen.
     */
    Set<Membership> findByIds(Set<String> membershipIds) throws TechnicalException;

    /**
     * find all memberships for a specific reference
     * => find all members of an api
     * @param referenceType the reference type
     * @param referenceId the reference id
     * @param roleId the role id, could be null
     * @return the list of memberships, or an empty set
     * @throws TechnicalException if something goes wrong, should never happen.
     */
    Set<Membership> findByReferenceAndRoleId(MembershipReferenceType referenceType, String referenceId, String roleId)
        throws TechnicalException;

    /**
     * find all memberships for a specific reference
     * => find all primary owner of a list of apis
     * @param referenceType the reference type
     * @param referenceIds the reference ids
     * @param roleId the role id, could be null
     * @return the list of memberships, or an empty set
     * @throws TechnicalException if something goes wrong, should never happen.
     */
    Set<Membership> findByReferencesAndRoleId(MembershipReferenceType referenceType, List<String> referenceIds, String roleId)
        throws TechnicalException;

    /**
     * find all memberships for a member and a referenceType
     * => find all apis of a user
     * @param memberId the member
     * @param memberType the member type. Can be USER or GROUP.
     * @param referenceType the referenceType
     * @return the list of memberships, or an empty set
     * @throws TechnicalException if something goes wrong, should never happen.
     */
    Set<Membership> findByMemberIdAndMemberTypeAndReferenceType(
        String memberId,
        MembershipMemberType memberType,
        MembershipReferenceType referenceType
    ) throws TechnicalException;

    Stream<String> findRefIdsByMemberIdAndMemberTypeAndReferenceType(
        String memberId,
        MembershipMemberType memberType,
        MembershipReferenceType referenceType
    ) throws TechnicalException;
    /**
     * find all memberships for a member and a referenceType and SourceId
     * @param memberId the member
     * @param memberType the member type.
     * @param referenceType the referenceType
     * @param sourceId the source Id
     * @return the list of memberships, or an empty set
     * @throws TechnicalException if something goes wrong, should never happen.
     */
    Set<Membership> findByMemberIdAndMemberTypeAndReferenceTypeAndSource(
        String memberId,
        MembershipMemberType memberType,
        MembershipReferenceType referenceType,
        String sourceId
    ) throws TechnicalException;

    /**
     * find all memberships for a list of member and a referenceType
     * => find all apis of a user
     * @param memberIds the members
     * @param memberType the member type. Can be USER or GROUP.
     * @param referenceType the referenceType
     * @return the list of memberships, or an empty set
     * @throws TechnicalException if something goes wrong, should never happen.
     */
    Set<Membership> findByMemberIdsAndMemberTypeAndReferenceType(
        List<String> memberIds,
        MembershipMemberType memberType,
        MembershipReferenceType referenceType
    ) throws TechnicalException;

    /**
     * find all memberships for a role
     * @param roleId the role Id
     * @return the list of memberships, or an empty set
     * @throws TechnicalException if something goes wrong, should never happen.
     */
    Set<Membership> findByRoleId(String roleId) throws TechnicalException;

    /**
     * find all memberships for a member, a referenceType and a role
     * => find all apis of a user where he is a primary owner
     * @param memberId the member
     * @param memberType the member type. Can be USER or GROUP.
     * @param referenceType the referenceType
     * @param roleId the role id, could be null
     * @return the list of memberships, or an empty set
     * @throws TechnicalException if something goes wrong, should never happen.
     */
    Set<Membership> findByMemberIdAndMemberTypeAndReferenceTypeAndRoleId(
        String memberId,
        MembershipMemberType memberType,
        MembershipReferenceType referenceType,
        String roleId
    ) throws TechnicalException;

    /**
     * find all memberships for a member, a referenceType and a list of roles
     * @param memberId the member
     * @param memberType the member type. Can be USER or GROUP.
     * @param referenceType the referenceType
     * @param roleIds the role id list
     * @return the list of memberships, or an empty set
     * @throws TechnicalException if something goes wrong, should never happen.
     */
    Set<Membership> findByMemberIdAndMemberTypeAndReferenceTypeAndRoleIdIn(
        String memberId,
        MembershipMemberType memberType,
        MembershipReferenceType referenceType,
        Collection<String> roleIds
    ) throws TechnicalException;

    /**
     * find all reference ids for a member, a referenceType and a list of roles
     * @param memberId the member
     * @param memberType the member type. Can be USER or GROUP.
     * @param referenceType the referenceType
     * @param roleIds the role id list
     * @return the list of reference id, or an empty set
     * @throws TechnicalException if something goes wrong, should never happen.
     */
    Set<String> findRefIdByMemberAndRefTypeAndRoleIdIn(
        String memberId,
        MembershipMemberType memberType,
        MembershipReferenceType referenceType,
        Collection<String> roleIds
    ) throws TechnicalException;

    /**
     * find all memberships for a member, a referenceType, a referenceId and a role
     * => determine whether or not a user has a specific role for a specific ref
     * @param memberId the member
     * @param memberType the member type. Can be USER or GROUP.
     * @param referenceType the referenceType
     * @param referenceId the referenceId
     * @param roleId the role id
     * @return a list of memberships, or an empty set
     * @throws TechnicalException if something goes wrong, should never happen.
     */
    Set<Membership> findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceIdAndRoleId(
        String memberId,
        MembershipMemberType memberType,
        MembershipReferenceType referenceType,
        String referenceId,
        String roleId
    ) throws TechnicalException;

    /**
     * find all memberships for a member, a referenceType and a referenceId
     * => get all the role of a member on a specific reference
     * @param memberId the member
     * @param memberType the member type. Can be USER or GROUP.
     * @param referenceType the referenceType
     * @param referenceId the referenceId
     * @return a list of memberships, or an empty set
     * @throws TechnicalException if something goes wrong, should never happen.
     */
    Set<Membership> findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
        String memberId,
        MembershipMemberType memberType,
        MembershipReferenceType referenceType,
        String referenceId
    ) throws TechnicalException;

    /**
     * find all memberships for a member
     * @param memberId the member
     * @param memberType the member type. Can be USER or GROUP.
     * @return the list of memberships, or an empty set
     * @throws TechnicalException if something goes wrong, should never happen.
     */
    Set<Membership> findByMemberIdAndMemberType(String memberId, MembershipMemberType memberType) throws TechnicalException;
}
