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
package io.gravitee.repository.mongodb.management.internal.subscriptionform;

import io.gravitee.repository.mongodb.management.internal.model.SubscriptionFormMongo;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for subscription forms.
 *
 * @author Gravitee.io Team
 */
@Repository
public interface SubscriptionFormMongoRepository extends MongoRepository<SubscriptionFormMongo, String> {
    Optional<SubscriptionFormMongo> findByIdAndEnvironmentId(String id, String environmentId);

    List<SubscriptionFormMongo> findAllByEnvironmentId(String environmentId);

    List<SubscriptionFormMongo> findAllByEnvironmentIdAndDefaultFormTrue(String environmentId);

    @Query("{ 'environmentId': ?0, 'apiIds': ?1 }")
    Optional<SubscriptionFormMongo> findByEnvironmentIdAndApiId(String environmentId, String apiId);

    void deleteByEnvironmentId(String environmentId);
}
