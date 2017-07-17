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
import io.gravitee.repository.management.model.Visibility;
import io.gravitee.repository.mongodb.management.internal.api.ApiMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.ApiMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoApiRepository implements ApiRepository {

	@Autowired
	private ApiMongoRepository internalApiRepo;

	@Autowired
	private GraviteeMapper mapper;
	
	@Override
	public Optional<Api> findById(String apiId) throws TechnicalException {
		ApiMongo apiMongo =  internalApiRepo.findOne(apiId);
		return Optional.ofNullable(mapApi(apiMongo));
	}

	@Override
	public Set<Api> findByVisibility(Visibility visibility) throws TechnicalException {
		return mapApis(internalApiRepo.findByVisibility(visibility.name()));
	}

	@Override
	public Set<Api> findByIds(List<String> ids) throws TechnicalException {
		return mapApis(internalApiRepo.findByIds(ids));
	}

	@Override
	public Set<Api> findByGroups(List<String> groupIds) throws TechnicalException {
		return mapApis(internalApiRepo.findByGroups(groupIds));
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
		ApiMongo apiMongo = internalApiRepo.findOne(api.getId());

		// Update, but don't change invariant other creation information
		apiMongo.setName(api.getName());
		apiMongo.setDescription(api.getDescription());
		apiMongo.setUpdatedAt(api.getUpdatedAt());
		apiMongo.setLifecycleState(api.getLifecycleState().toString());
		apiMongo.setDefinition(api.getDefinition());
		apiMongo.setVisibility(api.getVisibility().toString());
		apiMongo.setVersion(api.getVersion());
		apiMongo.setDeployedAt(api.getDeployedAt());
		apiMongo.setPicture(api.getPicture());
        apiMongo.setGroups(api.getGroups());
		apiMongo.setViews(api.getViews());
		apiMongo.setLabels(api.getLabels());

		ApiMongo applicationMongoUpdated = internalApiRepo.save(apiMongo);
		return mapApi(applicationMongoUpdated);
	}

	@Override
	public void delete(String apiId) throws TechnicalException {
		internalApiRepo.delete(apiId);
	}

	private Set<Api> mapApis(Collection<ApiMongo> apis) {
		return apis.stream().map(this::mapApi).collect(Collectors.toSet());
	}

	private ApiMongo mapApi(Api api){
		return (api == null) ? null : mapper.map(api, ApiMongo.class);
	}

	private Api mapApi(ApiMongo apiMongo){
		return (apiMongo == null) ? null : mapper.map(apiMongo, Api.class);
	}
}
