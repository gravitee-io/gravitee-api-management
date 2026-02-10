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
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data MongoDB repository for subscription forms.
 *
 * @author Gravitee.io Team
 */
@Repository
public interface SubscriptionFormMongoRepository extends MongoRepository<SubscriptionFormMongo, String> {
    /**
     * Finds a subscription form by ID and environment ID.
     *
     * @param id the subscription form ID
     * @param environmentId the environment ID
     * @return Optional containing the form if found and belonging to the environment
     */
    Optional<SubscriptionFormMongo> findByIdAndEnvironmentId(String id, String environmentId);

    /**
     * Finds a subscription form by environment ID.
     *
     * @param environmentId the environment ID
     * @return Optional containing the form if found
     */
    Optional<SubscriptionFormMongo> findByEnvironmentId(String environmentId);

    void deleteByEnvironmentId(String environmentId);
}
