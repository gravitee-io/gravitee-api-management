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
import io.gravitee.repository.management.api.ApiKeyRepository;
import io.gravitee.repository.management.model.ApiKey;
import io.gravitee.repository.mongodb.management.internal.api.ApiMongoRepository;
import io.gravitee.repository.mongodb.management.internal.application.ApplicationMongoRepository;
import io.gravitee.repository.mongodb.management.internal.key.ApiKeyMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.ApiAssociationMongo;
import io.gravitee.repository.mongodb.management.internal.model.ApiKeyMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class MongoApiKeyRepository implements ApiKeyRepository {

	private final static Logger logger = LoggerFactory.getLogger(MongoApiKeyRepository.class);

	@Autowired
	private GraviteeMapper mapper;

	@Autowired
	private ApiKeyMongoRepository internalApiKeyRepo;
	
	@Autowired
	private ApiMongoRepository internalApiRepo;	
	
	@Autowired
	private ApplicationMongoRepository internalApplicationRepo;

	private Set<ApiKey> map(Collection<ApiAssociationMongo> apiAssociationMongos){
		if (apiAssociationMongos == null){
			return Collections.emptySet();
		}
		
		return apiAssociationMongos.stream().map(apiAssociationMongo -> {
            ApiKey key = mapper.map(apiAssociationMongo.getKey(), ApiKey.class);
            key.setApi(apiAssociationMongo.getApi().getId());
			key.setApplication(apiAssociationMongo.getApplication().getId());

            return key;
        }).collect(Collectors.toSet());
	}

	@Override
	public Set<ApiKey> findByApplicationAndApi(String applicationId, String apiId) throws TechnicalException {
		List<ApiAssociationMongo> apiAssociationMongos = internalApiKeyRepo.findByApplicationAndApi(applicationId, apiId);
		
		return map(apiAssociationMongos);
	}

	@Override
	public Set<ApiKey> findByApplication(String applicationId) throws TechnicalException {
		List<ApiAssociationMongo> apiAssociationMongos = internalApiKeyRepo.findByApplication(applicationId);
		
		return map(apiAssociationMongos);
	}


	@Override
	public ApiKey create(String applicationId, String apiId, ApiKey key) throws TechnicalException {
		ApiAssociationMongo apiAssociationMongo = new ApiAssociationMongo();
		apiAssociationMongo.setApi(internalApiRepo.findOne(apiId));
		apiAssociationMongo.setApplication(internalApplicationRepo.findOne(applicationId));
		apiAssociationMongo.setKey(mapper.map(key, ApiKeyMongo.class));
		
		internalApiKeyRepo.insert(apiAssociationMongo);
		
		return key;
	}

	@Override
	public ApiKey update(ApiKey key) throws TechnicalException {
		ApiAssociationMongo associationMongo = internalApiKeyRepo.retrieve(key.getKey());
		associationMongo.getKey().setCreatedAt(key.getCreatedAt());
		associationMongo.getKey().setExpiration(key.getExpiration());
		associationMongo.getKey().setRevoked(key.isRevoked());
		associationMongo.getKey().setRevokeAt(key.getRevokeAt());

		internalApiKeyRepo.save(associationMongo);
		
		return key;
	}

	@Override
	public Optional<ApiKey> retrieve(String apiKey) throws TechnicalException {
		ApiAssociationMongo apiAssociationMongo = internalApiKeyRepo.retrieve(apiKey);

		if(apiAssociationMongo != null) {
			ApiKey retKey = mapper.map(apiAssociationMongo.getKey(), ApiKey.class);
			retKey.setApi(apiAssociationMongo.getApi().getId());
			retKey.setApplication(apiAssociationMongo.getApplication().getId());
			return Optional.of(retKey);
		}
		
		return Optional.empty();
	}

	@Override
	public Set<ApiKey> findByApi(String apiId) {
		Collection<ApiAssociationMongo> apiAssociationsMongo = internalApiKeyRepo.findByApi(apiId);
		return map(apiAssociationsMongo);
	}
}
