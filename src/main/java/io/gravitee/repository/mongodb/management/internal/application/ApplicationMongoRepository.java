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
package io.gravitee.repository.mongodb.management.internal.application;

import java.util.List;
import java.util.Set;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import io.gravitee.repository.management.model.ApplicationStatus;
import io.gravitee.repository.mongodb.management.internal.model.ApplicationMongo;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public interface ApplicationMongoRepository extends MongoRepository<ApplicationMongo, String> {

    @Query("{ _id: {$in: ?0} }")
    Set<ApplicationMongo> findByIds(List<String> ids);

    @Query("{ groups: {$in: ?0} }")
    Set<ApplicationMongo> findByGroups(List<String> ids);

    @Query("{ groups: {$in: ?0}, status: {$in: ?1} }")
    Set<ApplicationMongo> findByGroups(List<String> ids, List<ApplicationStatus> statuses);

    @Query("{ name: { $regex: ?0, $options: 'i'}}")
    Set<ApplicationMongo> findByName(String name);

    @Query("{ status: {$in: ?0} }")
    List<ApplicationMongo> findAll(List<ApplicationStatus> statuses);
    
    @Query("{ environment: ?0, status: {$in: ?1} }")
    List<ApplicationMongo> findAllByEnvironment(String environment, List<ApplicationStatus> statuses);
    
    @Query("{ environment: ?0 }")
    List<ApplicationMongo> findAllByEnvironment(String environment);
}



