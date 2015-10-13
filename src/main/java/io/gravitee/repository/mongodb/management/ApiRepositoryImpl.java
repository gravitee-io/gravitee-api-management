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
package io.gravitee.repository.mongodb.management;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.OwnerType;
import io.gravitee.repository.mongodb.management.internal.api.ApiMongoRepository;
import io.gravitee.repository.mongodb.management.internal.key.ApiKeyMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.ApiAssociationMongo;
import io.gravitee.repository.mongodb.management.internal.model.ApiMongo;
import io.gravitee.repository.mongodb.management.internal.model.UserMongo;
import io.gravitee.repository.mongodb.management.internal.team.TeamMongoRepository;
import io.gravitee.repository.mongodb.management.internal.user.UserMongoRepository;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ApiRepositoryImpl implements ApiRepository {
	

	@Autowired
	private ApiKeyMongoRepository internalApiKeyRepo;
	
	@Autowired
	private ApiMongoRepository internalApiRepo;

	@Autowired
	private TeamMongoRepository internalTeamRepo;
	
	@Autowired
	private UserMongoRepository internalUserRepo;
	
	@Autowired
	private GraviteeMapper mapper;
	
	@Override
	public Optional<Api> findByName(String apiName) throws TechnicalException {
		
		ApiMongo apiMongo =  internalApiRepo.findOne(apiName);
		return Optional.ofNullable(mapApi(apiMongo));
	}

	@Override
	public Set<Api> findAll() throws TechnicalException {
		
		List<ApiMongo> apis = internalApiRepo.findAll();
		return mapApis(apis);
	}

	
	@Override
	public Api create(Api api) throws TechnicalException {
		
		ApiMongo apiMongo = mapApi(api);
		ApiMongo apiMongoCreated = internalApiRepo.insert(apiMongo);
		return mapApi(apiMongoCreated);
	}

	@Override
	public Api update(Api api) throws TechnicalException {
		
		ApiMongo apiMongo =	mapApi(api);
		ApiMongo apiMongoUpdated = internalApiRepo.save(apiMongo);
		return mapApi(apiMongoUpdated);
	}

	@Override
	public void delete(String apiName) throws TechnicalException {
		internalApiRepo.delete(apiName);
	}



	@Override
	public Set<Api> findByUser(String username, boolean publicOnly) throws TechnicalException {
		List<ApiMongo> apis = internalApiRepo.findByUser(username, publicOnly);
		return mapApis(apis);
	}


	@Override
	public Set<Api> findByTeam(String teamName,  boolean publicOnly) throws TechnicalException {
	
		List<ApiMongo> apis = internalApiRepo.findByTeam(teamName, publicOnly);
		return mapApis(apis);
	}
	

	@Override
	public Set<Api> findByCreator(String userName) throws TechnicalException {

		List<ApiMongo> apis = internalApiRepo.findByCreator(userName);
		return mapApis(apis);
	}
	
	
	@Override
	public int countByUser(String username,  boolean publicOnly) throws TechnicalException {
		return (int) internalApiRepo.countByUser(username, publicOnly);
	}

	@Override
	public int countByTeam(String teamName,  boolean publicOnly) throws TechnicalException {
		return (int) internalApiRepo.countByTeam(teamName, publicOnly);	
	}
	

	
	private Set<Api> mapApis(Collection<ApiMongo> apis){
	
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
			apiMongo.setCreator(internalUserRepo.findOne(api.getCreator()));
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
			if(apiMongo.getCreator() != null){
				api.setCreator(apiMongo.getCreator().getName());
			}
		}
		return api;
	}

	@Override
	public Set<Api> findByApplication(String application) throws TechnicalException {
		
		List<ApiAssociationMongo> apiAssociationMongos = internalApiKeyRepo.findByApplication(application);
		
		return apiAssociationMongos.stream().map(new Function<ApiAssociationMongo, Api>() {

			@Override
			public Api apply(ApiAssociationMongo t) {
				return mapper.map(t.getApi(), Api.class);
			}
		}).collect(Collectors.toSet());
		
	}


	
}
