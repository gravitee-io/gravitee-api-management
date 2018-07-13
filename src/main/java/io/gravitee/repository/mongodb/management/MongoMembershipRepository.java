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
package io.gravitee.repository.mongodb.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MembershipRepository;
import io.gravitee.repository.management.model.Membership;
import io.gravitee.repository.management.model.MembershipReferenceType;
import io.gravitee.repository.management.model.RoleScope;
import io.gravitee.repository.mongodb.management.internal.membership.MembershipMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.MembershipMongo;
import io.gravitee.repository.mongodb.management.internal.model.MembershipPkMongo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;


/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoMembershipRepository implements MembershipRepository {

    private final Logger logger = LoggerFactory.getLogger(MongoMembershipRepository.class);

    @Autowired
    private MembershipMongoRepository internalMembershipRepo;

    @Override
    public Membership create(Membership membership) throws TechnicalException {
        logger.debug("Create membership [{}, {}, {}]", membership.getUserId(), membership.getReferenceType(), membership.getReferenceId());
        Membership m = map(internalMembershipRepo.insert(map(membership)));
        logger.debug("Create membership [{}, {}, {}] - Done", m.getUserId(), m.getReferenceType(), m.getReferenceId());
        return m;
    }

    @Override
    public Membership update(Membership membership) throws TechnicalException {
        if (membership == null || membership.getUserId() == null || membership.getReferenceId() == null || membership.getReferenceType() == null) {
            throw new IllegalStateException("Membership to update must have an user id, a reference id and type");
        }

        final MembershipPkMongo id = mapPk(membership);
        MembershipMongo membershipMongo = internalMembershipRepo.findById(id).orElse(null);

        if (membershipMongo == null) {
            throw new IllegalStateException(String.format("No membership found with id [%s]", id));
        }

        logger.debug("Update membership [{}, {}, {}]", membership.getUserId(), membership.getReferenceType(), membership.getReferenceId());
        Membership m = map(internalMembershipRepo.save(map(membership)));
        logger.debug("Update membership [{}, {}, {}] - Done", membership.getUserId(), membership.getReferenceType(), membership.getReferenceId());
        return m;
    }

    @Override
    public void delete(Membership membership) throws TechnicalException {
        logger.debug("Delete membership [{}, {}, {}]", membership.getUserId(), membership.getReferenceType(), membership.getReferenceId());
        internalMembershipRepo.deleteById(mapPk(membership));
        logger.debug("Delete membership [{}, {}, {}] - Done", membership.getUserId(), membership.getReferenceType(), membership.getReferenceId());
    }

    @Override
    public Optional<Membership> findById(String userId, MembershipReferenceType referenceType, String referenceId) throws TechnicalException {
        logger.debug("Find membership by ID [{}, {}, {}]", userId, referenceType, referenceId);

        MembershipPkMongo membershipPkMongo = new MembershipPkMongo();
        membershipPkMongo.setUserId(userId);
        membershipPkMongo.setReferenceType(referenceType.name());
        membershipPkMongo.setReferenceId(referenceId);
        MembershipMongo membershipMongo = internalMembershipRepo.findById(membershipPkMongo).orElse(null);

        logger.debug("Find membership by ID [{}, {}, {}]", userId, referenceType, referenceId);
        return Optional.ofNullable(map(membershipMongo));
    }

    @Override
    public Set<Membership> findByIds(String userId, MembershipReferenceType referenceType, Set<String> referenceIds) throws TechnicalException {
        logger.debug("Find membership by IDs [{}, {}, {}]", userId, referenceType, referenceIds);

        Set<Membership> memberships = internalMembershipRepo.findByIds(userId, referenceType.name(), referenceIds).
                stream().
                map(this::map).
                collect(Collectors.toSet());

        logger.debug("Find membership by IDs [{}, {}, {}]", userId, referenceType, referenceIds);
        return memberships;
    }

    @Override
    public Set<Membership> findByReferenceAndRole(MembershipReferenceType referenceType, String referenceId, RoleScope roleScope, String roleName) throws TechnicalException {
        logger.debug("Find membership by reference [{}, {}]", referenceType, referenceId);
        Set<MembershipMongo> membershipMongos;
        String membershipType = convertRoleToType(roleScope, roleName);
        if (membershipType == null) {
            membershipMongos = internalMembershipRepo.findByReference(referenceType.name(), referenceId);
        } else {
            membershipMongos = internalMembershipRepo.findByReferenceAndMembershipType(referenceType.name(), referenceId, membershipType);
        }
        Set<Membership> memberships = membershipMongos.stream().map(this::map).collect(Collectors.toSet());
        logger.debug("Find membership by reference [{}, {}] = {}", referenceType, referenceId, memberships);
        return memberships;
    }

    @Override
    public Set<Membership> findByReferencesAndRole(MembershipReferenceType referenceType, List<String> referenceIds, RoleScope roleScope, String roleName) throws TechnicalException {
        logger.debug("Find membership by references [{}, {}]", referenceType, referenceIds);
        Set<MembershipMongo> membershipMongos;
        String membershipType = convertRoleToType(roleScope, roleName);
        if (membershipType == null) {
            membershipMongos = internalMembershipRepo.findByReferences(referenceType.name(), referenceIds);
        } else {
            membershipMongos = internalMembershipRepo.findByReferencesAndMembershipType(referenceType.name(), referenceIds, membershipType);
        }
        Set<Membership> memberships = membershipMongos.stream().map(this::map).collect(Collectors.toSet());
        logger.debug("Find membership by references [{}, {}] = {}", referenceType, referenceIds, memberships);
        return memberships;
    }

    @Override
    public Set<Membership> findByUserAndReferenceType(String userId, MembershipReferenceType referenceType) throws TechnicalException {
        logger.debug("Find membership by user and referenceType [{}, {}]", userId, referenceType);
        Set<Membership> memberships = internalMembershipRepo.findByUserAndReferenceType(userId, referenceType.name()).
                stream().
                map(this::map).
                collect(Collectors.toSet());
        logger.debug("Find membership by user and referenceType [{}, {}] = {}", userId, referenceType, memberships);
        return memberships;
    }

    @Override
    public Set<Membership> findByRole(RoleScope roleScope, String roleName) throws TechnicalException {
        logger.debug("Find membership by role [{}, {}]", roleScope, roleName);
        String membershipType = convertRoleToType(roleScope, roleName);
        Set<Membership> memberships = internalMembershipRepo.findByMembershipType(membershipType).
                stream().
                map(this::map).
                collect(Collectors.toSet());
        logger.debug("Find membership by role [{}, {}]", roleScope, roleName, memberships);
        return memberships;
    }

    @Override
    public Set<Membership> findByUserAndReferenceTypeAndRole(String userId, MembershipReferenceType referenceType, RoleScope roleScope, String roleName) throws TechnicalException {
        String membershipType = convertRoleToType(roleScope, roleName);
        logger.debug("Find membership by user and referenceType and membershipType [{}, {}, {}]", userId, referenceType, membershipType);
        Set<Membership> memberships = internalMembershipRepo.findByUserAndReferenceTypeAndMembershipType(userId, referenceType.name(), membershipType).
                stream().
                map(this::map).
                collect(Collectors.toSet());
        logger.debug("Find membership by user and referenceType and membershipType [{}, {}, {}] = {}", userId, referenceType, membershipType, memberships);
        return memberships;
    }


    @Override
    public Set<Membership> findByUser(String userId) throws TechnicalException {
        logger.debug("Find membership by user [{}]", userId);
        Set<Membership> memberships = internalMembershipRepo.findByUser(userId).
                stream().
                map(this::map).
                collect(Collectors.toSet());
        logger.debug("Find membership by user [{}] = {}", userId, memberships);
        return memberships;
    }

    private Membership map(MembershipMongo membershipMongo) {
        if (membershipMongo == null) {
            return null;
        }
        Membership membership = new Membership();
        membership.setUserId(membershipMongo.getId().getUserId());
        membership.setReferenceType(MembershipReferenceType.valueOf(membershipMongo.getId().getReferenceType()));
        membership.setReferenceId(membershipMongo.getId().getReferenceId());
        if (membershipMongo.getRoles() != null) {
            Map<Integer, String> roles = new HashMap<>(membershipMongo.getRoles().size());
            for (String roleAsString : membershipMongo.getRoles()) {
                String[] role = convertTypeToRole(roleAsString);
                roles.put(Integer.valueOf(role[0]), role[1]);
            }
            membership.setRoles(roles);
        }
        membership.setCreatedAt(membershipMongo.getCreatedAt());
        membership.setUpdatedAt(membershipMongo.getUpdatedAt());
        return membership;
    }

    private MembershipMongo map(Membership membership) {
        if (membership == null) {
            return null;
        }
        MembershipMongo membershipMongo = new MembershipMongo();
        membershipMongo.setId(mapPk(membership));
        if (membership.getRoles() != null) {
            List<String> roles = new ArrayList<>(membership.getRoles().size());
            for (Map.Entry<Integer, String> roleEntry : membership.getRoles().entrySet()) {
                roles.add(convertRoleToType(roleEntry.getKey(), roleEntry.getValue()));
            }
            membershipMongo.setRoles(roles);
        }
        membershipMongo.setCreatedAt(membership.getCreatedAt());
        membershipMongo.setUpdatedAt(membership.getUpdatedAt());
        return membershipMongo;
    }

    private MembershipPkMongo mapPk(Membership membership) {
        MembershipPkMongo membershipPkMongo = new MembershipPkMongo();
        membershipPkMongo.setUserId(membership.getUserId());
        membershipPkMongo.setReferenceType(membership.getReferenceType().name());
        membershipPkMongo.setReferenceId(membership.getReferenceId());
        return membershipPkMongo;
    }

    private String convertRoleToType(RoleScope roleScope, String roleName) {
        if (roleName == null) {
            return null;
        }
        return convertRoleToType(roleScope.getId(), roleName);
    }

    private String convertRoleToType(int roleScope, String roleName) {
        return roleScope + ":" + roleName;
    }

    private String[] convertTypeToRole(String type) {
        if (type == null) {
            return null;
        }
        String[] role = type.split(":");
        if (role.length != 2) {
            return null;
        }
        return role;
    }
}
