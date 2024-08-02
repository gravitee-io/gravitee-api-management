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
package io.gravitee.repository.mongodb.management.internal.membership;

import io.gravitee.repository.mongodb.management.internal.model.MembershipMongo;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public interface MembershipMongoRepository extends MongoRepository<MembershipMongo, String>, MembershipMongoRepositoryCustom {
    @Query("{ 'id': {$in: ?0} }")
    Set<MembershipMongo> findByIds(Set<String> membershipIds);

    @Query("{ 'referenceType' : ?0, 'referenceId' : ?1 }")
    Set<MembershipMongo> findByReference(String referenceType, String referenceId);

    @Query("{ 'referenceType' : ?0, 'referenceId' : ?1, 'roleId' : ?2 }")
    Set<MembershipMongo> findByReferenceAndRoleId(String referenceType, String referenceId, String roleId);

    @Query("{ 'referenceType' : ?0, 'referenceId' : { $in : ?1 } }")
    Set<MembershipMongo> findByReferences(String referenceType, List<String> referenceId);

    @Query("{ 'referenceType' : ?0, 'referenceId' : { $in : ?1 }, 'roleId' : ?2 }")
    Set<MembershipMongo> findByReferencesAndRoleId(String referenceType, List<String> referenceId, String roleId);

    @Query("{ 'roleId' : ?0 }")
    Set<MembershipMongo> findByRoleId(String roleId);

    @Query("{ 'memberId' : ?0, 'memberType' : ?1, 'referenceType' : ?2 }")
    Set<MembershipMongo> findByMemberIdAndMemberTypeAndReferenceType(String memberId, String memberType, String referenceType);

    @Query("{ 'memberId' : { $in : ?0 }, 'memberType' : ?1, 'referenceType' : ?2 }")
    Set<MembershipMongo> findByMemberIdsAndMemberTypeAndReferenceType(List<String> memberIds, String memberType, String referenceType);

    @Query("{ 'memberId' : ?0, 'memberType' : ?1, 'referenceType' : ?2, 'roleId' : ?3 }")
    Set<MembershipMongo> findByMemberIdAndMemberTypeAndReferenceTypeAndRoleId(
        String memberId,
        String memberType,
        String referenceType,
        String roleId
    );

    @Query("{ 'memberId' : ?0, 'memberType' : ?1, 'referenceType' : ?2, 'roleId' : { $in: ?3 } }")
    Set<MembershipMongo> findByMemberIdAndMemberTypeAndReferenceTypeAndRoleIdIn(
        String memberId,
        String memberType,
        String referenceType,
        Collection<String> roleIds
    );

    @Query(value = "{ 'memberId' : ?0, 'memberType' : ?1, 'referenceType' : ?2, 'roleId' : { $in: ?3 } }", fields = "{ 'referenceId' : 1 }")
    Set<MembershipMongo> findRefIdByMemberAndRefTypeAndRoleIdIn(
        String memberId,
        String memberType,
        String referenceType,
        Collection<String> roleIds
    );

    @Query("{ 'memberId' : ?0, 'memberType' : ?1, 'referenceType' : ?2, 'source' : ?3 }")
    Set<MembershipMongo> findByMemberIdAndMemberTypeAndReferenceTypeAndSource(
        String memberId,
        String memberType,
        String referenceType,
        String source
    );

    @Query("{ 'memberId' : ?0, 'memberType' : ?1, 'referenceType' : ?2, 'referenceId' : ?3 }")
    Set<MembershipMongo> findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceId(
        String memberId,
        String memberType,
        String referenceType,
        String referenceId
    );

    @Query("{ 'memberId' : ?0, 'memberType' : ?1, 'referenceType' : ?2, 'referenceId' : ?3, 'roleId' : ?4 }")
    Set<MembershipMongo> findByMemberIdAndMemberTypeAndReferenceTypeAndReferenceIdAndRoleId(
        String memberId,
        String memberType,
        String referenceType,
        String referenceId,
        String roleId
    );

    @Query("{ 'memberId' : ?0, 'memberType' : ?1 }")
    Set<MembershipMongo> findByMemberIdAndMemberType(String memberId, String memberType);

    @Query(value = "{ 'referenceId': ?0, 'referenceType': ?1 }", fields = "{ _id : 1 }", delete = true)
    List<MembershipMongo> deleteByReferenceIdAndReferenceType(String referenceId, String referenceType);
}
