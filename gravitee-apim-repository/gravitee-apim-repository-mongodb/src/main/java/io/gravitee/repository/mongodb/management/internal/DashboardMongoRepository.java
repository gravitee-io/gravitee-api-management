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
package io.gravitee.repository.mongodb.management.internal;

import io.gravitee.repository.mongodb.management.internal.model.DashboardMongo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@Repository
public interface DashboardMongoRepository extends MongoRepository<DashboardMongo, String> {
    List<DashboardMongo> findByReferenceTypeAndReferenceId(String referenceType, String referenceId);
    List<DashboardMongo> findByReferenceTypeAndReferenceIdAndTypeOrderByOrder(String referenceType, String referenceId, String type);

    Optional<DashboardMongo> findByReferenceTypeAndReferenceIdAndId(String referenceType, String referenceId, String id);

    @Query(value = "{ 'referenceId': ?0, 'referenceType': ?1  }", fields = "{ _id : 1 }", delete = true)
    List<DashboardMongo> deleteByReferenceIdAndReferenceType(String referenceId, String referenceType);
}
