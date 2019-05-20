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
package io.gravitee.repository.mongodb.management.internal.role;

import java.util.List;
import java.util.Set;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import io.gravitee.repository.mongodb.management.internal.model.RoleMongo;
import io.gravitee.repository.mongodb.management.internal.model.RolePkMongo;

/**
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public interface RoleMongoRepository extends MongoRepository<RoleMongo, RolePkMongo> {

    @Query("{ 'id.scope' : ?0 }")
    Set<RoleMongo> findByScope(int scopeId);
    
    @Query("{ 'id.scope' : ?0, referenceId: ?1, referenceType: ?2 }")
    Set<RoleMongo> findByScopeAndReferenceIdAndReferenceType(int scopeId, String referenceId, String referenceType);
    
    @Query("{ referenceId: ?0, referenceType: ?1 }")
    List<RoleMongo> findByReferenceIdAndReferenceType(String referenceId, String referenceType);
}


