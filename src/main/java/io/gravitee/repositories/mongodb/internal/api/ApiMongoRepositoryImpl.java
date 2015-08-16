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
package io.gravitee.repositories.mongodb.internal.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import io.gravitee.repositories.mongodb.internal.model.ApiMongo;
import io.gravitee.repositories.mongodb.internal.model.PolicyConfigurationMongo;

public class ApiMongoRepositoryImpl implements ApiMongoRepositoryCustom {

	@Autowired
	private MongoTemplate mongoTemplate;

	private Query getFindByOwnerQuery(String type, String name, boolean publicOnly){
		Query query = new Query();

		query.addCriteria(Criteria.where("owner.$id").is(name));
		query.addCriteria(Criteria.where("owner.$ref").is(type));
		
		if(publicOnly){
			query.addCriteria(Criteria.where("privateApi").is(false));	
		}
		return query;
	}
	

	@Override
	public List<ApiMongo> findByUser(String username, boolean publicOnly) {

		Query query = getFindByOwnerQuery("users", username, publicOnly);
		List<ApiMongo> apis = mongoTemplate.find(query, ApiMongo.class);
		
		return apis;

	}
	
	@Override
	public List<ApiMongo> findByTeam(String teamname, boolean publicOnly) {

		Query query = getFindByOwnerQuery("teams", teamname, publicOnly);	
		List<ApiMongo> apis = mongoTemplate.find(query, ApiMongo.class);
		
		return apis;
	}


	@Override
	public long countByUser(String username, boolean publicOnly) {
	
		Query query = getFindByOwnerQuery("users", username, publicOnly);
		return mongoTemplate.count(query, ApiMongo.class);	
	}
	
	@Override
	public long countByTeam(String teamname, boolean publicOnly) {
		
		Query query = getFindByOwnerQuery("teams", teamname, publicOnly);				
		return mongoTemplate.count(query, ApiMongo.class);
		
	}



	@Override
	public void updatePoliciesConfiguration(String apiName, List<PolicyConfigurationMongo> policyConfigurations) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public void updatePolicyConfiguration(String apiName, PolicyConfigurationMongo policyConfiguration) {
		// TODO Auto-generated method stub
		
	}


	@Override
	public List<PolicyConfigurationMongo> findPoliciesByApi(String apiName) {
		
		ApiMongo api = mongoTemplate.findById(apiName, ApiMongo.class);
		return api.getPolicies();
	}
	
}
