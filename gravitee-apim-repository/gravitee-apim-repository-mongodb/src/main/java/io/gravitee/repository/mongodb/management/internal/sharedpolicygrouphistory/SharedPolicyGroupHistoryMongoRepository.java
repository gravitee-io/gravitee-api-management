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
package io.gravitee.repository.mongodb.management.internal.sharedpolicygrouphistory;

import io.gravitee.repository.mongodb.management.internal.model.SharedPolicyGroupHistoryMongo;
import java.util.Optional;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SharedPolicyGroupHistoryMongoRepository
    extends MongoRepository<SharedPolicyGroupHistoryMongo, String>, SharedPolicyGroupHistoryMongoRepositoryCustom {
    // Get SPG binding by environmentId and sharedPolicyGroupId sorted by deployedAt limit 1
    @Aggregation(pipeline = { "{$match: { environmentId : ?0, 'id' : ?1 }}", "{$sort: {updatedAt: -1}}", "{$limit: 1}" })
    Optional<SharedPolicyGroupHistoryMongo> getLatestBySharedPolicyGroupId(String environmentId, String sharedPolicyGroupId);
}
