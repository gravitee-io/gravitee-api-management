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
package io.gravitee.repository.mongodb.management.internal.user;

import io.gravitee.repository.mongodb.management.internal.model.UserMongo;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public interface UserMongoRepository extends MongoRepository<UserMongo, String>, UserMongoRepositoryCustom {
    @Query(value = "{ _id: {$in: ?0} }", fields = "{'picture': 0}")
    Set<UserMongo> findByIds(Collection<String> ids);

    @Query(value = "{ 'source': ?0, 'sourceId': {$regex: '^?1$', $options: 'i'}, 'organizationId': ?2 }")
    UserMongo findBySourceAndSourceId(String source, String sourceId, String organizationId);

    @Query(value = "{ 'email': {$regex: '^?0$', $options: 'i'}, 'organizationId': ?1 }")
    UserMongo findByEmail(String email, String organizationId);

    @Query(value = "{ 'organizationId': ?0 }", fields = "{ _id : 1 }", delete = true)
    List<UserMongo> deleteByOrganizationId(String organizationId);
}
