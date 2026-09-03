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
package io.gravitee.repository.mongodb.management.internal.performancetarget;

import io.gravitee.repository.mongodb.management.internal.model.PerformanceTargetEvaluationMongo;
import java.util.Collection;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PerformanceTargetEvaluationMongoRepository
    extends MongoRepository<PerformanceTargetEvaluationMongo, String>, PerformanceTargetEvaluationMongoRepositoryCustom {
    @Query(value = "{ environmentId: ?0, reference: ?1, latest: true }")
    List<PerformanceTargetEvaluationMongo> findLatestByEnvironmentIdAndReference(String environmentId, String reference);

    @Query(value = "{ environmentId: ?0, reference: { $in: ?1 }, latest: true }")
    List<PerformanceTargetEvaluationMongo> findLatestByEnvironmentIdAndReferenceIn(String environmentId, Collection<String> references);

    @Query(value = "{ environmentId: ?0, reference: ?1 }", fields = "{ _id : 1 }", delete = true)
    List<PerformanceTargetEvaluationMongo> deleteByEnvironmentIdAndReference(String environmentId, String reference);

    void deleteByTargetId(String targetId);

    @Query(value = "{ environmentId: ?0 }", fields = "{ _id : 1 }", delete = true)
    List<PerformanceTargetEvaluationMongo> deleteByEnvironmentId(String environmentId);
}
