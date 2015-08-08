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

import io.gravitee.repository.model.Application;

import java.util.Optional;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface ApplicationRepository {

    /**
     * List all applications.
     *
     * @return All public applications.
     */
    Set<Application> findAll();

    /**
     * List all applications hold by a {@link io.gravitee.repository.model.Team}.
     *
     * @param teamName The name of the team.
     * @return All applications from a team.
     */
    Set<Application> findByTeam(String teamName);

    /**
     * List all applications hold by a {@link io.gravitee.repository.model.User}.
     *
     * @param username The name of the user.
     * @return All applications from a user.
     */
    Set<Application> findByUser(String username);

    Application create(Application application);

    Application update(Application application);

    /**
     * Get an application using its name.
     *
     * @param applicationName The name of the application to retrieve.
     * @return An {@link Optional} application.
     */
    Optional<Application> findByName(String applicationName);

    void delete(String apiName);

    int countByUser(String username);

    int countByTeam(String teamName);
}
