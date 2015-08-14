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

import java.util.Optional;
import java.util.Set;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.model.Api;
import io.gravitee.repository.model.Application;
import io.gravitee.repository.model.Team;
import io.gravitee.repository.model.User;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface ApplicationRepository {

    /**
     * List all applications.
     *
     * @return All public applications.
     */
    Set<Application> findAll() throws TechnicalException;

    /**
     * List all applications hold by a {@link io.gravitee.repository.model.Team}.
     *
     * @param teamName The name of the team.
     * @return All applications from a team.
     */
    Set<Application> findByTeam(String teamName) throws TechnicalException;

    /**
     * List all applications hold by a {@link io.gravitee.repository.model.User}.
     *
     * @param userName The name of the user.
     * @return All applications from a user.
     */
    Set<Application> findByUser(String userName) throws TechnicalException;

    /**
     * Create an {@link Application}
     * 
     * @param application Application to create
     * @return Application created
     */
    Application create(Application application) throws TechnicalException;

    /**
     * Update an {@link Application}
     * 
     * @param application Application to update
     * @return Application updated
     */
    Application update(Application application) throws TechnicalException;

    /**
     * Get an application using its name.
     *
     * @param applicationName The name of the application to retrieve.
     * @return An {@link Optional} application.
     */
    Optional<Application> findByName(String applicationName) throws TechnicalException;

    /**
     * Delete an {@link Application}
     * 
     * @param applicationName Application name to delete
     */
    void delete(String applicationName) throws TechnicalException;

    /**
     * Count {@link Application} owner by a given {@link User}
     * 
     * @param userName Application user owner name
     * @return Counted application
     */
    int countByUser(String userName) throws TechnicalException;

    /**
     * Count {@link Team} owner by a given {@link User}
     * 
     * @param teamName Application user owner team
     * @return Counted application
     */
    int countByTeam(String teamName) throws TechnicalException;
    
    /**
     * Associate an Api with an Application.
     * 
     * @param applicationName Application name
     * @param apiName Name of the Api to associate 
     * @return true success, false otherwise
     */
    boolean associate(String applicationName, String apiName) throws TechnicalException;
    
    /**
     * Remove an association between an {@link Application} and an {@link Api}
     * 
     * @param applicationName Application name
     * @param apiName Name of the Api to dissociate 
     * @return true success, false otherwise
     */
    boolean dissociate(String applicationName, String apiName) throws TechnicalException;


}
