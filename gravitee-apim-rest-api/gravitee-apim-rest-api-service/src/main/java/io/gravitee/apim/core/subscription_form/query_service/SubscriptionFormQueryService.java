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
package io.gravitee.apim.core.subscription_form.query_service;

import io.gravitee.apim.core.subscription_form.model.SubscriptionForm;
import io.gravitee.apim.core.subscription_form.model.SubscriptionFormId;
import java.util.List;
import java.util.Optional;

/**
 * Query service interface for subscription form read operations.
 *
 * @author Gravitee.io Team
 */
public interface SubscriptionFormQueryService {
    /**
     * Finds a subscription form by ID and environment. Returns empty if the form does not exist
     * or does not belong to the given environment.
     *
     * @param environmentId the environment ID
     * @param subscriptionFormId the subscription form ID
     * @return Optional containing the form if found and belonging to the environment
     */
    Optional<SubscriptionForm> findByIdAndEnvironmentId(String environmentId, SubscriptionFormId subscriptionFormId);

    /**
     * Lists every subscription form of an environment.
     *
     * @param environmentId the environment ID
     * @return the forms of the environment, empty when it has none
     */
    List<SubscriptionForm> findAllByEnvironmentId(String environmentId);

    /**
     * Finds the default subscription form of an environment, the one used for every API without
     * a dedicated form.
     *
     * @param environmentId the environment ID
     * @return Optional containing the default form if the environment has one, empty otherwise
     */
    Optional<SubscriptionForm> findDefaultForEnvironmentId(String environmentId);
}
