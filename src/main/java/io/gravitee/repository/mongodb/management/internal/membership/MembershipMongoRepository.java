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
package io.gravitee.repository.mongodb.management.internal.membership;

import io.gravitee.repository.mongodb.management.internal.model.MembershipMongo;
import io.gravitee.repository.mongodb.management.internal.model.MembershipPkMongo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public interface MembershipMongoRepository extends MongoRepository<MembershipMongo, MembershipPkMongo> {

    @Query("{ 'id.userId': ?0, 'id.referenceType': ?1, 'id.referenceId': {$in: ?2} }")
    Set<MembershipMongo> findByIds(String userId, String referenceType, Set<String> referenceIds);

    @Query("{ 'id.referenceType' : ?0, 'id.referenceId' : ?1 }")
    Set<MembershipMongo> findByReference(String referenceType, String referenceId);

    @Query("{ 'id.referenceType' : ?0, 'id.referenceId' : ?1, 'roles' : ?2 }")
    Set<MembershipMongo> findByReferenceAndMembershipType(String referenceType, String referenceId, String membershipType);

    @Query("{ 'id.referenceType' : ?0, 'id.referenceId' : { $in : ?1 } }")
    Set<MembershipMongo> findByReferences(String referenceType, List<String> referenceId);

    @Query("{ 'id.referenceType' : ?0, 'id.referenceId' : { $in : ?1 }, 'roles' : ?2 }")
    Set<MembershipMongo> findByReferencesAndMembershipType(String referenceType, List<String>  referenceId, String membershipType);

    @Query("{ 'roles' : ?0 }")
    Set<MembershipMongo> findByMembershipType(String membershipType);

    @Query("{ 'id.userId' : ?0, 'id.referenceType' : ?1 }")
    Set<MembershipMongo> findByUserAndReferenceType(String userId, String referenceType);

    @Query("{ 'id.userId' : ?0, 'id.referenceType' : ?1, 'roles' : ?2 }")
    Set<MembershipMongo> findByUserAndReferenceTypeAndMembershipType(String userId, String referenceType, String membershipType);

    @Query("{ 'id.userId' : ?0 }")
    Set<MembershipMongo> findByUser(String userId);
}
