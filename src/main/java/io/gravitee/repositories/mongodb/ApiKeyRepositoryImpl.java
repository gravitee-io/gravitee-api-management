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

import io.gravitee.repositories.mongodb.internal.application.ApplicationMongoRepository;
import io.gravitee.repositories.mongodb.internal.model.ApplicationMongo;
import io.gravitee.repositories.mongodb.internal.model.UserMongo;
import io.gravitee.repositories.mongodb.internal.team.TeamMongoRepository;
import io.gravitee.repositories.mongodb.internal.user.UserMongoRepository;
import io.gravitee.repositories.mongodb.mapper.GraviteeMapper;
import io.gravitee.repository.api.ApiKeyRepository;
import io.gravitee.repository.api.ApplicationRepository;
import io.gravitee.repository.model.ApiKey;
import io.gravitee.repository.model.Application;
import io.gravitee.repository.model.OwnerType;

@Component
public class ApiKeyRepositoryImpl implements ApiKeyRepository{

		
	@Autowired
	private GraviteeMapper mapper;

	
	@Override
	public ApiKey generateKey(String application) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ApiKey getApiKey(String apiKey, String apiName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean invalidateKey(String applicationName) {
		// TODO Auto-generated method stub
		return false;
	}
	
}
