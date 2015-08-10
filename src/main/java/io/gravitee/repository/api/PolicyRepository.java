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

import io.gravitee.repository.model.Policy;

import java.util.Optional;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public interface PolicyRepository {

	/**
	 * Find All {@link Policy} plugins.
	 * 
	 * @return Policies found
	 */
    Set<Policy> findAll();

    /**
     * Find a {@link Policy} by id
     * 
     * @param id Searched policy id
     * @return {@link Optional} of {@link Policy} found
     */
    Optional<Policy> findById(String id);
    
    /**
     * Create a {@link Policy} plugin.
     * 
     * @param policy Policy to create
     * @return Policy created
     */
    Policy create(Policy policy);
 
    /**
     * Update a {@link Policy} plugin.
     * 
     * @param policy Policy to update
     * @return Policy updated
     */   
    Policy update(Policy policy);
    
    /**
     * Delete a {@link Policy} by Id.
     * 
     * @param id Policy to delete identifier 
     */
    void delete(String id);
}
