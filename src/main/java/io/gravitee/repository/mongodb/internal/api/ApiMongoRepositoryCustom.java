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
package io.gravitee.repository.mongodb.internal.api;

import java.util.List;

import io.gravitee.repository.mongodb.internal.model.ApiMongo;
import io.gravitee.repository.mongodb.internal.model.PolicyConfigurationMongo;

public interface ApiMongoRepositoryCustom {

	/**
	 * Find Apis by team name
	 * @param teamId
	 * @return
	 */
    public List<ApiMongo> findByTeam(String name, boolean publicOnly);
    
	/**
	 * Find Apis by team name
	 * @param teamId
	 * @return
	 */
    public List<ApiMongo> findByUser(String name, boolean publicOnly);
    
    /**
     * Count api by username (owner)
     * @param username
     * @return
     */
	public long countByUser(String username, boolean publicOnly);
    
    /**
     * Count api by username (owner)
     * @param username
     * @return
     */	
	public long countByTeam(String teamname, boolean publicOnly);
	
	/**
	 * Update API policies configuration
	 * 
	 * @param apiName Api to update name 
	 * @param policyConfigurations policies configurations
	 */
	public void updatePoliciesConfiguration(String apiName, List<PolicyConfigurationMongo> policyConfigurations);
	
	/**
	 * Update API policy configuration
	 * 
	 * @param apiName Api to update name 
	 * @param policyConfiguration policy configuration
	 */
	public void updatePolicyConfiguration(String apiName, PolicyConfigurationMongo policyConfiguration);
	
	/**
	 * Return policies for one API
	 * 
	 * @param apiName  Api policy configuration owner name
	 * @return Api Policy configuration
	 */
	public List<PolicyConfigurationMongo> findPoliciesByApi(String apiName);
	
}
