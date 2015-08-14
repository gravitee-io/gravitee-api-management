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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.gravitee.repositories.mongodb.internal.application.ApplicationMongoRepository;
import io.gravitee.repositories.mongodb.internal.model.ApiKeyMongo;
import io.gravitee.repositories.mongodb.internal.model.ApplicationMongo;
import io.gravitee.repositories.mongodb.mapper.GraviteeMapper;
import io.gravitee.repository.api.ApiKeyRepository;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.model.ApiKey;

@Component
public class ApiKeyRepositoryImpl implements ApiKeyRepository{

		
	@Autowired
	private GraviteeMapper mapper;
	
	@Autowired
	private ApplicationMongoRepository internalApplicationRepo;

	private Logger logger = LoggerFactory.getLogger(ApiRepositoryImpl.class);
	
	@Override
	public boolean invalidateKey(String applicationName) throws TechnicalException{
		
		logger.debug("Invalidate application key [{}]", applicationName);
		
		ApplicationMongo applicationMongo = internalApplicationRepo.findOne(applicationName);
		
		if(applicationMongo == null){
			throw new IllegalArgumentException(String.format("Invalid application name [%s]", applicationName));
		}
		applicationMongo.setKey(null);
		
		internalApplicationRepo.save(applicationMongo);
		
		logger.debug("Invalidate application key [{}] - Done", applicationName);
		
		return true;
	}

	@Override
	public ApiKey createKey(String applicationName, ApiKey key) throws TechnicalException {
		
		logger.debug("Create application key [{}]", applicationName);
		
		ApplicationMongo applicationMongo = internalApplicationRepo.findOne(applicationName);
		
		ApiKeyMongo apiKeyMongo = mapper.map(key, ApiKeyMongo.class);
		applicationMongo.setKey(apiKeyMongo);
		
		internalApplicationRepo.save(applicationMongo);
		
		logger.debug("Create application key [{}] - Done", applicationName);
		return key;
	}


	@Override
	public ApiKey getKey(String apiKey, String apiName) throws TechnicalException {
	
		logger.debug("Get api key [{}]", apiName);
		
		ApplicationMongo applicationMongo = internalApplicationRepo.findByKey(apiKey, apiName);
		ApiKey key = null;
		
		if(applicationMongo == null){
			return null;
			//throw new IllegalArgumentException(String.format("Invalid key for api [%s]", apiName));
		}
		key = mapper.map(applicationMongo.getKey(), ApiKey.class);

		logger.debug("Get api key [{}] - Done", apiName);
		
		return key;
	}
	
}
