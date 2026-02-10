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
package io.gravitee.repository.management.api;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.model.SubscriptionForm;
import java.util.Optional;

/**
 * Repository interface for subscription forms.
 *
 * @author Gravitee.io Team
 */
public interface SubscriptionFormRepository extends CrudRepository<SubscriptionForm, String> {
    /**
     * Finds a subscription form by ID and environment. Returns empty if the form does not exist
     * or does not belong to the given environment.
     *
     * @param id the subscription form ID
     * @param environmentId the environment ID
     * @return Optional containing the form if found and belonging to the environment
     * @throws TechnicalException if a technical error occurs
     */
    Optional<SubscriptionForm> findByIdAndEnvironmentId(String id, String environmentId) throws TechnicalException;

    /**
     * Finds the subscription form for a given environment.
     *
     * @param environmentId the environment ID
     * @return Optional containing the form if found
     * @throws TechnicalException if a technical error occurs
     */
    Optional<SubscriptionForm> findByEnvironmentId(String environmentId) throws TechnicalException;

    /**
     * Deletes the subscription form for a given environment.
     *
     * @param environmentId the environment ID
     * @throws TechnicalException if a technical error occurs
     */
    void deleteByEnvironmentId(String environmentId) throws TechnicalException;
}
