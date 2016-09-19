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
import io.gravitee.repository.mongodb.management.internal.membership.MembershipMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.MembershipMongo;
import io.gravitee.repository.mongodb.management.internal.model.MembershipPkMongo;
import io.gravitee.repository.mongodb.management.internal.model.PageMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;


/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoMembershipRepository implements MembershipRepository {

    private final Logger logger = LoggerFactory.getLogger(MongoMembershipRepository.class);

    @Autowired
    private MembershipMongoRepository internalMembershipRepo;

    @Autowired
    private GraviteeMapper mapper;


    @Override
    public Membership create(Membership membership) throws TechnicalException {
        logger.debug("Create membership [{}, {}, {}]", membership.getUserId(), membership.getReferenceType(), membership.getReferenceId());
        Membership m = map(internalMembershipRepo.insert(map(membership)));
        logger.debug("Create membership [{}, {}, {}] - Done", m.getUserId(), m.getReferenceType(), m.getReferenceId());
        return m;
    }

    @Override
    public Membership update(Membership membership) throws TechnicalException {
        logger.debug("Update membership [{}, {}, {}]", membership.getUserId(), membership.getReferenceType(), membership.getReferenceId());
        Membership m = map(internalMembershipRepo.save(map(membership)));
        logger.debug("Update membership [{}, {}, {}] - Done", m.getUserId(), m.getReferenceType(), m.getReferenceId());
        return m;
    }

    @Override
    public void delete(Membership membership) throws TechnicalException {
        logger.debug("Delete membership [{}, {}, {}]", membership.getUserId(), membership.getReferenceType(), membership.getReferenceId());
        internalMembershipRepo.delete(mapPk(membership));
        logger.debug("Delete membership [{}, {}, {}] - Done", membership.getUserId(), membership.getReferenceType(), membership.getReferenceId());
    }

    @Override
    public Optional<Membership> findById(String userId, MembershipReferenceType referenceType, String referenceId) throws TechnicalException {
        logger.debug("Find membership by ID [{}, {}, {}]", userId, referenceType, referenceId);

        MembershipPkMongo membershipPkMongo = new MembershipPkMongo();
        membershipPkMongo.setUserId(userId);
        membershipPkMongo.setReferenceType(referenceType.name());
        membershipPkMongo.setReferenceId(referenceId);
        MembershipMongo membershipMongo = internalMembershipRepo.findOne(membershipPkMongo);

        logger.debug("Find membership by ID [{}, {}, {}]", userId, referenceType, referenceId);
        return Optional.ofNullable(mapper.map(membershipMongo, Membership.class));
    }

    @Override
    public Set<Membership> findByReferenceAndMembershipType(MembershipReferenceType referenceType, String referenceId, String membershipType) throws TechnicalException {
        logger.debug("Find membership by reference [{}, {}]", referenceType, referenceId);
        Set<MembershipMongo> membershipMongos;
        if(membershipType == null) {
            membershipMongos = internalMembershipRepo.findByReference(referenceType.name(), referenceId);
        } else {
            membershipMongos = internalMembershipRepo.findByReferenceAndMembershipType(referenceType.name(), referenceId, membershipType);
        }
        Set<Membership> memberships = mapper.collection2set(membershipMongos, MembershipMongo.class, Membership.class);
        logger.debug("Find membership by reference [{}, {}] = {}", referenceType, referenceId, memberships);
        return memberships;
    }

    @Override
    public Set<Membership> findByReferencesAndMembershipType(MembershipReferenceType referenceType, List<String> referenceIds, String membershipType) throws TechnicalException {
        logger.debug("Find membership by references [{}, {}]", referenceType, referenceIds);
        Set<MembershipMongo> membershipMongos;
        if(membershipType == null) {
            membershipMongos = internalMembershipRepo.findByReferences(referenceType.name(), referenceIds);
        } else {
            membershipMongos = internalMembershipRepo.findByReferencesAndMembershipType(referenceType.name(), referenceIds, membershipType);
        }
        Set<Membership> memberships = mapper.collection2set(membershipMongos, MembershipMongo.class, Membership.class);
        logger.debug("Find membership by references [{}, {}] = {}", referenceType, referenceIds, memberships);
        return memberships;
    }

    @Override
    public Set<Membership> findByUserAndReferenceType(String userId, MembershipReferenceType referenceType) throws TechnicalException {
        logger.debug("Find membership by user and referenceType [{}, {}]", userId, referenceType);
        Set<Membership> memberships = mapper.collection2set(
                internalMembershipRepo.findByUserAndReferenceType(userId, referenceType.name()), MembershipMongo.class, Membership.class);
        logger.debug("Find membership by user and referenceType [{}, {}] = {}", userId, referenceType, memberships);
        return memberships;
    }

    @Override
    public Set<Membership> findByUserAndReferenceTypeAndMembershipType(String userId, MembershipReferenceType referenceType, String membershipType) throws TechnicalException {
        logger.debug("Find membership by user and referenceType and membershipType [{}, {}, {}]", userId, referenceType, membershipType);
        Set<Membership> memberships = mapper.collection2set(
                internalMembershipRepo.findByUserAndReferenceTypeAndMembershipType(userId, referenceType.name(), membershipType), MembershipMongo.class, Membership.class);
        logger.debug("Find membership by user and referenceType and membershipType [{}, {}, {}] = {}", userId, referenceType, membershipType, memberships);
        return memberships;
    }

    private Membership map(MembershipMongo membershipMongo) {
        Membership membership = new Membership();
        membership.setUserId(membershipMongo.getId().getUserId());
        membership.setReferenceType(MembershipReferenceType.valueOf(membershipMongo.getId().getReferenceType()));
        membership.setReferenceId(membershipMongo.getId().getReferenceId());
        membership.setType(membershipMongo.getType());
        membership.setCreatedAt(membershipMongo.getCreatedAt());
        membership.setUpdatedAt(membershipMongo.getUpdatedAt());
        return membership;
    }

    private MembershipMongo map(Membership membership) {
        MembershipMongo membershipMongo = new MembershipMongo();
        membershipMongo.setId(mapPk(membership));
        membershipMongo.setType(membership.getType());
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
}
