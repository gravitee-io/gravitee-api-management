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
package io.gravitee.repositories.mongodb;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.gravitee.repositories.mongodb.internal.api.ApiMongoRepository;
import io.gravitee.repositories.mongodb.internal.model.ApiMongo;
import io.gravitee.repositories.mongodb.internal.model.PolicyConfigurationMongo;
import io.gravitee.repositories.mongodb.internal.model.UserMongo;
import io.gravitee.repositories.mongodb.internal.team.TeamMongoRepository;
import io.gravitee.repositories.mongodb.internal.user.UserMongoRepository;
import io.gravitee.repositories.mongodb.mapper.GraviteeMapper;
import io.gravitee.repository.api.ApiRepository;
import io.gravitee.repository.model.Api;
import io.gravitee.repository.model.OwnerType;
import io.gravitee.repository.model.PolicyConfiguration;

@Component
public class ApiRepositoryImpl implements ApiRepository {

	@Autowired
	private ApiMongoRepository internalApiRepo;

	@Autowired
	private TeamMongoRepository internalTeamRepo;
	
	@Autowired
	private UserMongoRepository internalUserRepo;
	
	@Autowired
	private GraviteeMapper mapper;
	
	
	@Override
	public Optional<Api> findByName(String apiName) {
		
		ApiMongo apiMongo =  internalApiRepo.findOne(apiName);
		return Optional.ofNullable(mapApi(apiMongo));
	}

	@Override
	public Set<Api> findAll() {
		
		List<ApiMongo> apis = internalApiRepo.findAll();
		return mapiApis(apis);
	}

	
	@Override
	public Api create(Api api) {
		
		ApiMongo apiMongo = mapApi(api);
		ApiMongo apiMongoCreated = internalApiRepo.insert(apiMongo);
		return mapApi(apiMongoCreated);
	}

	@Override
	public Api update(Api api) {
		
		ApiMongo apiMongo =	mapApi(api);
		ApiMongo apiMongoUpdated = internalApiRepo.save(apiMongo);
		return mapApi(apiMongoUpdated);
	}

	@Override
	public void delete(String apiName) {
		internalApiRepo.delete(apiName);
	}



	@Override
	public Set<Api> findByUser(String username, boolean publicOnly) {
		List<ApiMongo> apis = internalApiRepo.findByUser(username, publicOnly);
		return mapiApis(apis);
	}


	@Override
	public Set<Api> findByTeam(String teamName,  boolean publicOnly) {
	
		List<ApiMongo> apis = internalApiRepo.findByTeam(teamName, publicOnly);
		return mapiApis(apis);
	}
	
	@Override
	public int countByUser(String username,  boolean publicOnly) {
		return (int) internalApiRepo.countByUser(username, publicOnly);

	}

	@Override
	public int countByTeam(String teamName,  boolean publicOnly) {
		return (int) internalApiRepo.countByTeam(teamName, publicOnly);	
	}
	

	
	private Set<Api> mapiApis(Collection<ApiMongo> apis){
	
		Set<Api> res = new HashSet<>();
		for (ApiMongo api : apis) {
			res.add(mapApi(api));
		}
		return res;
	}
	

	private ApiMongo mapApi(Api api){
		
		ApiMongo apiMongo = null;
		if(api != null){
			apiMongo = mapper.map(api, ApiMongo.class);
			
			if(OwnerType.USER.equals(api.getOwnerType())){
				apiMongo.setOwner(internalUserRepo.findOne(api.getOwner()));
			}else{
				apiMongo.setOwner(internalTeamRepo.findOne(api.getOwner()));
			}
		}
		return apiMongo;
	}

	private Api mapApi(ApiMongo apiMongo){
		
		Api api = null;
		if(apiMongo != null){
			api = mapper.map(apiMongo, Api.class);
		
			if(apiMongo.getOwner() != null){
				api.setOwner(apiMongo.getOwner().getName());
				if(apiMongo.getOwner() instanceof UserMongo){
					api.setOwnerType(OwnerType.USER);
				}else{
					api.setOwnerType(OwnerType.TEAM);
				}
			}
		}
		return api;
	}

	@Override
	public void updatePoliciesConfiguration(String apiName, List<PolicyConfiguration> policyConfigs) {
		
		List<PolicyConfigurationMongo> policiesConfigsMongo = mapper.collection2list(policyConfigs, PolicyConfiguration.class, PolicyConfigurationMongo.class);
		this.internalApiRepo.updatePoliciesConfiguration(apiName, policiesConfigsMongo);
		
	}

	@Override
	public void updatePolicyConfiguration(String apiName, PolicyConfiguration policyConfig) {
		
		PolicyConfigurationMongo policyConfigMongo = mapper.map(policyConfig, PolicyConfigurationMongo.class);
		this.internalApiRepo.updatePolicyConfiguration(apiName, policyConfigMongo);
		
	}

	@Override
	public List<PolicyConfiguration> findPoliciesByApi(String apiName) {
		
		 List<PolicyConfigurationMongo>  policies = this.internalApiRepo.findPoliciesByApi(apiName);
		 return  mapper.collection2list(policies, PolicyConfigurationMongo.class, PolicyConfiguration.class);
	}

	
}
