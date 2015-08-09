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

import io.gravitee.repository.model.Api;
import io.gravitee.repository.model.PolicyConfiguration;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    Api create(Api api);

    Api update(Api api);

    void delete(String apiName);

    int countByUser(String username, boolean publicOnly);

    int countByTeam(String teamName, boolean publicOnly);

    void updatePoliciesConfiguration(String apiName, List<PolicyConfiguration> policyConfigurations);

    void updatePolicyConfiguration(String apiName, PolicyConfiguration policyConfiguration);

    List<PolicyConfiguration> findByApi(String apiName);
}
