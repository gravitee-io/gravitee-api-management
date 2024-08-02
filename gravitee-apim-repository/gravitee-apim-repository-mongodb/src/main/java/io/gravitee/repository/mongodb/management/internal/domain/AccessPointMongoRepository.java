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
package io.gravitee.repository.mongodb.management.internal.domain;

import io.gravitee.repository.management.model.AccessPointStatus;
import io.gravitee.repository.mongodb.management.internal.model.AccessPointMongo;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public interface AccessPointMongoRepository extends MongoRepository<AccessPointMongo, String>, AccessPointMongoRepositoryCustom {
    @Query(value = "{ 'host': ?0, 'status': ?1 }")
    AccessPointMongo findByHostAndStatus(String host, AccessPointStatus status);

    @Query(value = "{ 'target': ?0, 'status': ?1 }")
    List<AccessPointMongo> findAllByTargetAndStatus(final String target, AccessPointStatus status);

    @Query(value = "{ 'referenceType': ?0, 'referenceId': ?1 , 'target': ?2, 'status': ?3 }")
    List<AccessPointMongo> findAllByReferenceAndTargetAndStatus(
        final String referenceType,
        final String referenceIds,
        final String target,
        AccessPointStatus status
    );

    @Query(value = "{ 'referenceType': ?0, 'referenceId': ?1}", fields = "{ _id: 1 }", delete = true)
    List<AccessPointMongo> deleteAllByReference(final String referenceType, final String referenceId);
}
