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
package io.gravitee.repository.api.management;

import java.util.Optional;
import java.util.Set;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.model.management.Team;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface TeamRepository {

    /**
     * List teams
     *
     * @param publicOnly
     * @return
     */
    Set<Team> findAll(boolean publicOnly) throws TechnicalException;

    /**
     * Find a {@link Team} by name
     * 
     * @param name team name
     * @return {@link Optional} {@link Team} found
     */
    Optional<Team> findByName(String name) throws TechnicalException;

    /**
     * Create a {@link Team}
     * 
     * @param team team to create
     * @return Team created
     */
    Team create(Team team) throws TechnicalException;
  
    /**
     * Update a {@link Team}
     * 
     * @param team team to update
     * @return Team updated
     */
    Team update(Team team) throws TechnicalException;

    /**
     * Delete a team by name
     * 
     * @param name Team name.
     */
    void delete(String name) throws TechnicalException;
}
