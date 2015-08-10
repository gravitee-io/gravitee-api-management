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
package io.gravitee.repository.api;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import io.gravitee.repository.model.Api;
import io.gravitee.repository.model.PolicyConfiguration;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface ApiRepository {

    /**
     * Get an API using its name.
     *
     * @param apiName The name of the API to retrieve.
     * @return An {@link Optional} API.
     */
    Optional<Api> findByName(String apiName);

    /**
     * List all public APIs.
     *
     * @return All public APIs.
     */
    Set<Api> findAll();

    /**
     * List APIs (public and/or private) hold by a {@link io.gravitee.repository.model.Team}.
     *
     * @param teamName The name of the team.
     * @param publicOnly List only public APIs.
     * @return List APIs from a team.
     */
    Set<Api> findByTeam(String teamName, boolean publicOnly);

    /**
     * List APIs (public and private) hold by a {@link io.gravitee.repository.model.User}.
     *
     * @param username The name of the user.
     * @param publicOnly List only public APIs.
     * @return List APIs from a user.
     */
    Set<Api> findByUser(String username, boolean publicOnly);

    /**
     * Create an API
     * 
     * @param api api to create
     * @return api creaded
     */
    Api create(Api api);

    /**
     * Update an API
     * 
     * @param api api to update
     * @return api updated
     */
    Api update(Api api);

    /**
     * Delete an API
     * 
     * @param apiName api name to delete
     */
    void delete(String apiName);

    /**
     * Count all APIs (public and private) owned by a given {@link io.gravitee.repository.model.User}
     * 
     * @param username owner user name 
     * @param publicOnly List only public APIs.
     * @return counted APIs
     */
    int countByUser(String username, boolean publicOnly);
   
    /**
    * Count all APIs (public and private) owned by a given {@link io.gravitee.repository.model.Team}
    * 
    * @param teamName owner team name 
    * @param publicOnly List only public APIs.
    * @return counted APIs
    */
    int countByTeam(String teamName, boolean publicOnly);

    /**
     * Update an API policies
     * 
     * @param apiName API name
     * @param policyConfigurations Ordered list of {@link PolicyConfiguration} to set to the API
     */
    void updatePoliciesConfiguration(String apiName, List<PolicyConfiguration> policyConfigurations);
  
    /**
     * Update a API policy
     * 
     * @param apiName API name
     * @param policyConfiguration {@link PolicyConfiguration} to update
     */
    void updatePolicyConfiguration(String apiName, PolicyConfiguration policyConfiguration);

    /**
     * Give all {@link PolicyConfiguration} for an API
     * 
     * @param apiName API name
     * @return API policies configuration
     */
    List<PolicyConfiguration> findPoliciesByApi(String apiName);
}
