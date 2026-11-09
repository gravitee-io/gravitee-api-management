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
package io.gravitee.repository.mongodb.management.internal.secret;

import io.gravitee.repository.mongodb.management.internal.model.SecretMongo;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SecretMongoRepository extends MongoRepository<SecretMongo, String> {
    @Query("{ '_id': ?2, 'organizationId': ?0, 'environmentId': ?1 }")
    Optional<SecretMongo> findById(String organizationId, String environmentId, String id);

    @Query("{ 'organizationId': ?0, 'environmentId': ?1, 'key': ?2 }")
    Optional<SecretMongo> findByKey(String organizationId, String environmentId, String key);

    @Query(value = "{ '_id': ?2, 'organizationId': ?0, 'environmentId': ?1 }", delete = true)
    void delete(String organizationId, String environmentId, String id);

    @Query("{ 'organizationId': ?0, 'environmentId': ?1 }")
    Page<SecretMongo> search(String organizationId, String environmentId, Pageable pageable);

    @Query(value = "{ 'environmentId': ?0 }", delete = true)
    void deleteByEnvironmentId(String environmentId);
}
