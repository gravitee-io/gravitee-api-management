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
package io.gravitee.repository.mongodb.management.internal.application;

import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.mongodb.management.internal.model.ApplicationMongo;
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
public interface ApplicationMongoRepository extends MongoRepository<ApplicationMongo, String>, ApplicationMongoRepositoryCustom {
    default Set<ApplicationMongo> findByIds(Collection<String> ids) {
        return this.findByIds(ids, null);
    }

    @Query("{ groups: {$in: ?0} }")
    Set<ApplicationMongo> findByGroups(List<String> ids);

    @Query("{ groups: {$in: ?0}, status: {$in: ?1} }")
    Set<ApplicationMongo> findByGroups(List<String> ids, List<ApplicationStatus> statuses);

    @Query("{ name: { $regex: ?0, $options: 'i'}}")
    Set<ApplicationMongo> findByName(String name);

    @Query(value = "{ name: { $regex: ?0, $options: 'i'}, status: {$in: ?1} }", fields = "{ 'background' : 0, 'picture': 0}")
    Set<ApplicationMongo> findByNameAndStatuses(String name, List<ApplicationStatus> statuses);

    @Query(value = "{ status: {$in: ?0} }", fields = "{ 'background' : 0, 'picture': 0}")
    List<ApplicationMongo> findAll(List<ApplicationStatus> statuses);

    @Query("{ environmentId: ?0, status: {$in: ?1} }")
    List<ApplicationMongo> findAllByEnvironmentId(String environmentId, List<ApplicationStatus> statuses);

    @Query("{ environmentId: ?0 }")
    List<ApplicationMongo> findAllByEnvironmentId(String environmentId);

    @Query(value = "{ environmentId: ?0 }", fields = "{ _id : 1 }", delete = true)
    List<ApplicationMongo> deleteByEnvironmentId(String environmentId);
}
