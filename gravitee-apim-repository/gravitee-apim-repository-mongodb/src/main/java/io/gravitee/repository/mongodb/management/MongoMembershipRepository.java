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
package io.gravitee.repository.mongodb.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipMemberType;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.mongodb.management.internal.membership.MembershipMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.MembershipMongo;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Teams
 */
@Component
public class MongoMembershipRepository implements MembershipRepository {

    private final Logger logger = LoggerFactory.getLogger(MongoMembershipRepository.class);

    @Autowired
    private MembershipMongoRepository internalMembershipRepo;

    @Override
    public Membership create(Membership membership) throws TechnicalException {
        logger.debug("Create membership [{}]", membership);
        Membership m = map(internalMembershipRepo.insert(map(membership)));
        logger.debug("Create membership [{}] - Done", membership);
        return m;
    }

    @Override
    public Membership update(Membership membership) throws TechnicalException {
        if (
            membership == null ||
            membership.getMemberId() == null ||
            membership.getReferenceId() == null ||
            membership.getReferenceType() == null ||
            membership.getRoleId() == null
        ) {
            throw new IllegalStateException("Membership to update must have an user id, a reference id and type, and a role");
        }

        String id = membership.getId();
        MembershipMongo membershipMongo = internalMembershipRepo.findById(id).orElse(null);

        if (membershipMongo == null) {
            throw new IllegalStateException(String.format("No membership found with id [%s]", id));
        }

        logger.debug(
            "Update membership [{}, {}, {}, {}, {}]",
            membership.getMemberId(),
            membership.getMemberType().name(),
            membership.getReferenceType(),
            membership.getReferenceId(),
            membership.getRoleId()
        );
        Membership m = map(internalMembershipRepo.save(map(membership)));
        logger.debug(
            "Update membership [{}, {}, {}, {}, {}] - Done",
            membership.getMemberId(),
            membership.getMemberType().name(),
            membership.getReferenceType(),
            membership.getReferenceId(),
            membership.getRoleId()
        );
        return m;
    }

    @Override
    public void delete(String membershipId) throws TechnicalException {
        logger.debug("Delete membership [{}]", membershipId);
        internalMembershipRepo.deleteById(membershipId);
        logger.debug("Delete membership [{}] - Done", membershipId);
    }

    @Override
    public List<String> deleteByReferenceIdAndReferenceType(String referenceId, MembershipReferenceType referenceType)
        throws TechnicalException {
        logger.debug("Delete memberships by reference [{}/{}]", referenceId, referenceType);
        try {
            final var fields = internalMembershipRepo
                .deleteByReferenceIdAndReferenceType(referenceId, referenceType.name())
                .stream()
                .map(MembershipMongo::getId)
                .toList();
            logger.debug("Delete memberships by reference [{}/{}] - Done", referenceId, referenceType);
            return fields;
        } catch (Exception ex) {
            logger.error("Failed to delete memberships by ref: {}/{}", referenceId, referenceType, ex);
            throw new TechnicalException("Failed to delete memberships by ref");
        }
    }

    @Override
    public Optional<Membership> findById(String membershipId) throws TechnicalException {
        logger.debug("Find membership by ID [{}]", membershipId);

        MembershipMongo membershipMongo = internalMembershipRepo.findById(membershipId).orElse(null);

        logger.debug("Find membership by ID [{}]", membershipId);
        return Optional.ofNullable(map(membershipMongo));
    }

    @Override
    public Set<Membership> findByIds(Set<String> membershipIds) {
        logger.debug("Find membership by IDs [{}]", membershipIds);

        Set<Membership> memberships = internalMembershipRepo.findByIds(membershipIds).stream().map(this::map).collect(Collectors.toSet());

        logger.debug("Find membership by IDs [{}]", membershipIds);
        return memberships;
    }

    @Override
    public Set<Membership> findByReferenceAndRoleId(MembershipReferenceType referenceType, String referenceId, String roleId) {
        logger.debug("Find membership by reference and roleId [{}, {}, {}]", referenceType, referenceId, roleId);
        Set<MembershipMongo> membershipMongos;
        if (roleId == null) {
            membershipMongos = internalMembershipRepo.findByReference(referenceType.name(), referenceId);
        } else {
            membershipMongos = internalMembershipRepo.findByReferenceAndRoleId(referenceType.name(), referenceId, roleId);
        }
        Set<Membership> memberships = membershipMongos.stream().map(this::map).collect(Collectors.toSet());
        logger.debug("Find membership by reference and roleId [{}, {}, {}] = {}", referenceType, referenceId, roleId, memberships);
        return memberships;
    }

    @Override
    public Set<Membership> findByReferencesAndRoleId(MembershipReferenceType referenceType, List<String> referenceIds, String roleId) {
        logger.debug("Find membership by references and roleId [{}, {}, {}]", referenceType, referenceIds, roleId);
        Set<MembershipMongo> membershipMongos;
        if (roleId == null) {
            membershipMongos = internalMembershipRepo.findByReferences(referenceType.name(), referenceIds);
        } else {
            membershipMongos = internalMembershipRepo.findByReferencesAndRoleId(referenceType.name(), referenceIds, roleId);
        }
        Set<Membership> memberships = membershipMongos.stream().map(this::map).collect(Collectors.toSet());
        logger.debug("Find membership by references and roleId [{}, {}, {}] = {}", referenceType, referenceIds, roleId, memberships);
        return memberships;
    }

    @Override
    public Set<Membership> findByMemberIdAndMemberTypeAndReferenceType(
        String memberId,
        MembershipMemberType memberType,
        MembershipReferenceType referenceType
    ) {
        logger.debug("Find membership by user and referenceType [{}, {}, {}]", memberId, memberType, referenceType);
        Set<Membership> memberships = internalMembershipRepo
            .findByMemberIdAndMemberTypeAndReferenceType(memberId, memberType.name(), referenceType.name())
            .stream()
            .map(this::map)
            .collect(Collectors.toSet());
        logger.debug("Find membership by user and referenceType [{}, {}, {}] = {}", memberId, memberType, referenceType, memberships);
        return memberships;
    }

    @Override
    public Stream<String> findRefIdsByMemberIdAndMemberTypeAndReferenceType(
        String memberId,
        MembershipMemberType memberType,
        MembershipReferenceType referenceType
    ) throws TechnicalException {
        return internalMembershipRepo.findRefIdsByMemberIdAndMemberTypeAndReferenceType(memberId, memberType.name(), referenceType.name());
    }

    @Override
    public Set<Membership> findByRoleId(String roleId) {
        logger.debug("Find membership by roleId [{}]", roleId);
        Set<Membership> memberships = internalMembershipRepo.findByRoleId(roleId).stream().map(this::map).collect(Collectors.toSet());
        logger.debug("Find membership by roleId [{}] = {}", roleId, memberships);
        return memberships;
    }

    @Override
    public Set<Membership> findByMemberIdAndMemberTypeAndReferenceTypeAndRoleId(
        String memberId,
        MembershipMemberType memberType,
        MembershipReferenceType referenceType,
        String roleId
    ) {
        logger.debug("Find membership by user and referenceType and roleId [{}, {}, {}, {}]", memberId, memberType, referenceType, roleId);
        Set<Membership> memberships = internalMembershipRepo
            .findByMemberIdAndMemberTypeAndReferenceTypeAndRoleId(memberId, memberType.name(), referenceType.name(), roleId)
            .stream()
            .map(this::map)
            .collect(Collectors.toSet());
        logger.debug(
            "Find membership by user and referenceType and roleId [{}, {}, {}, {}] = {}",
            memberId,
            memberType,
            referenceType,
            roleId,
            memberships
        );
        return memberships;
    }

    @Override
    public Set<Membership> findByMemberIdAndMemberTypeAndReferenceTypeAndSource(
        String memberId,
        MembershipMemberType memberType,
        MembershipReferenceType referenceType,
        String sourceId
    ) {
        logger.debug(
            "Find membership by user and referenceType and sourceId [{}, {}, {}, {}]",
            memberId,
            memberType,
            referenceType,
            sourceId
        );
        Set<Membership> memberships = internalMembershipRepo
            .findByMemberIdAndMemberTypeAndReferenceTypeAndSource(memberId, memberType.name(), referenceType.name(), sourceId)
            .stream()
            .map(this::map)
            .collect(Collectors.toSet());
        logger.debug(
            "Find membership by user and referenceType and sourceId [{}, {}, {}, {}] = {}",
            memberId,
            memberType,
            referenceType,
            sourceId,
            memberships
        );
        return memberships;
    }

    @Override
    public Set<Membership> findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceIdAndRoleId(
        String memberId,
        MembershipMemberType memberType,
        MembershipReferenceType referenceType,
        String referenceId,
        String roleId
    ) {
        logger.debug(
            "Find membership by user and referenceType and referenceId and roleId [{}, {}, {}, {}, {}]",
            memberId,
            memberType,
            referenceType,
            referenceId,
            roleId
        );
        Set<Membership> memberships = internalMembershipRepo
            .findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceIdAndRoleId(
                memberId,
                memberType.name(),
                referenceType.name(),
                referenceId,
                roleId
            )
            .stream()
            .map(this::map)
            .collect(Collectors.toSet());
        logger.debug(
            "Find membership by user and referenceType and referenceId, roleId [{}, {}, {}, {}, {}] = {}",
            memberId,
            memberType,
            referenceType,
            referenceId,
            roleId,
            memberships
        );
        return memberships;
    }

    @Override
    public Set<Membership> findByMemberIdAndMemberTypeAndReferenceTypeAndRoleIdIn(
        String memberId,
        MembershipMemberType memberType,
        MembershipReferenceType referenceType,
        Collection<String> roleIds
    ) throws TechnicalException {
        logger.debug(
            "Find membership by user and referenceType and referenceId and roleId in [{}, {}, {}, {}, {}]",
            memberId,
            memberType,
            referenceType,
            roleIds
        );
        Set<Membership> memberships = internalMembershipRepo
            .findByMemberIdAndMemberTypeAndReferenceTypeAndRoleIdIn(memberId, memberType.name(), referenceType.name(), roleIds)
            .stream()
            .map(this::map)
            .collect(Collectors.toSet());
        logger.debug(
            "Find membership by user and referenceType and referenceId, roleId in [{}, {}, {}, {}, {}] = {}",
            memberId,
            memberType,
            referenceType,
            roleIds,
            memberships
        );
        return memberships;
    }

    @Override
    public Set<String> findRefIdByMemberAndRefTypeAndRoleIdIn(
        String memberId,
        MembershipMemberType memberType,
        MembershipReferenceType referenceType,
        Collection<String> roleIds
    ) throws TechnicalException {
        logger.debug(
            "Find membership by user and referenceType and referenceId and roleId in [{}, {}, {}, {}, {}]",
            memberId,
            memberType,
            referenceType,
            roleIds
        );
        Set<String> memberships = internalMembershipRepo
            .findRefIdByMemberAndRefTypeAndRoleIdIn(memberId, memberType.name(), referenceType.name(), roleIds)
            .stream()
            .map(MembershipMongo::getReferenceId)
            .collect(Collectors.toSet());
        logger.debug(
            "Find membership by user and referenceType and referenceId, roleId in [{}, {}, {}, {}, {}] = {}",
            memberId,
            memberType,
            referenceType,
            roleIds,
            memberships
        );
        return memberships;
    }

    @Override
    public Set<Membership> findByMemberIdAndMemberType(String memberId, MembershipMemberType memberType) {
        logger.debug("Find membership by user [{}, {}]", memberId, memberType);
        Set<Membership> memberships = internalMembershipRepo
            .findByMemberIdAndMemberType(memberId, memberType.name())
            .stream()
            .map(this::map)
            .collect(Collectors.toSet());
        logger.debug("Find membership by user [{}, {}] = {}", memberId, memberType, memberships);
        return memberships;
    }

    @Override
    public Set<Membership> findByMemberIdsAndMemberTypeAndReferenceType(
        List<String> memberIds,
        MembershipMemberType memberType,
        MembershipReferenceType referenceType
    ) {
        logger.debug("Find membership by members and reference type [{}, {}, {}]", memberIds, memberType, referenceType);
        Set<Membership> memberships = internalMembershipRepo
            .findByMemberIdsAndMemberTypeAndReferenceType(memberIds, memberType.name(), referenceType.name())
            .stream()
            .map(this::map)
            .collect(Collectors.toSet());
        logger.debug("Find membership by members and reference type [{}, {}, {}] = {}", memberIds, memberType, referenceType, memberships);
        return memberships;
    }

    @Override
    public Set<Membership> findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
        String memberId,
        MembershipMemberType memberType,
        MembershipReferenceType referenceType,
        String referenceId
    ) {
        logger.debug("Find membership by member and reference [{}, {}, {}, {}]", memberId, memberType, referenceId, referenceType);
        Set<Membership> memberships = internalMembershipRepo
            .findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(memberId, memberType.name(), referenceType.name(), referenceId)
            .stream()
            .map(this::map)
            .collect(Collectors.toSet());
        logger.debug("Find membership by user [{}, {}, {}, {}] = {}", memberId, memberType, referenceId, referenceType, memberships);
        return memberships;
    }

    private Membership map(MembershipMongo membershipMongo) {
        if (membershipMongo == null) {
            return null;
        }
        Membership membership = new Membership();
        membership.setId(membershipMongo.getId());
        membership.setMemberId(membershipMongo.getMemberId());
        membership.setMemberType(MembershipMemberType.valueOf(membershipMongo.getMemberType()));
        membership.setReferenceType(MembershipReferenceType.valueOf(membershipMongo.getReferenceType()));
        membership.setReferenceId(membershipMongo.getReferenceId());
        membership.setRoleId(membershipMongo.getRoleId());
        membership.setSource(membershipMongo.getSource());
        membership.setCreatedAt(membershipMongo.getCreatedAt());
        membership.setUpdatedAt(membershipMongo.getUpdatedAt());
        return membership;
    }

    private MembershipMongo map(Membership membership) {
        if (membership == null) {
            return null;
        }
        MembershipMongo membershipMongo = new MembershipMongo();
        membershipMongo.setId(membership.getId());
        membershipMongo.setMemberId(membership.getMemberId());
        membershipMongo.setMemberType(membership.getMemberType().name());
        membershipMongo.setReferenceId(membership.getReferenceId());
        membershipMongo.setReferenceType(membership.getReferenceType().name());
        membershipMongo.setRoleId(membership.getRoleId());
        membershipMongo.setSource(membership.getSource());
        membershipMongo.setCreatedAt(membership.getCreatedAt());
        membershipMongo.setUpdatedAt(membership.getUpdatedAt());
        return membershipMongo;
    }

    @Override
    public Set<Membership> findAll() throws TechnicalException {
        return internalMembershipRepo.findAll().stream().map(this::map).collect(Collectors.toSet());
    }
}
