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
import io.gravitee.repository.mongodb.management.internal.key.ApiKeyMongoRepository;
import io.gravitee.repository.mongodb.management.internal.model.ApiKeyMongo;
import io.gravitee.repository.mongodb.management.mapper.GraviteeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class MongoApiKeyRepository implements ApiKeyRepository {

	@Autowired
	private GraviteeMapper mapper;

	@Autowired
	private ApiKeyMongoRepository internalApiKeyRepo;

	@Override
	public ApiKey create(ApiKey apiKey) throws TechnicalException {
		ApiKeyMongo apiKeyMongo = mapper.map(apiKey, ApiKeyMongo.class);
		apiKeyMongo = internalApiKeyRepo.insert(apiKeyMongo);

		return mapper.map(apiKeyMongo, ApiKey.class);
	}

	@Override
	public ApiKey update(ApiKey apiKey) throws TechnicalException {
		ApiKeyMongo apiKeyMongo = mapper.map(apiKey, ApiKeyMongo.class);
		apiKeyMongo = internalApiKeyRepo.save(apiKeyMongo);

		return mapper.map(apiKeyMongo, ApiKey.class);
	}

	@Override
	public Set<ApiKey> findBySubscription(String subscription) throws TechnicalException {
		return internalApiKeyRepo.findBySubscription(subscription)
				.stream()
				.map(apiKey -> mapper.map(apiKey, ApiKey.class))
				.collect(Collectors.toSet());
	}

	@Override
	public Set<ApiKey> findByPlan(String plan) throws TechnicalException {
		return internalApiKeyRepo.findByPlan(plan)
				.stream()
				.map(apiKey -> mapper.map(apiKey, ApiKey.class))
				.collect(Collectors.toSet());
	}

	@Override
	public Optional<ApiKey> findById(String key) throws TechnicalException {
		ApiKeyMongo apiKey = internalApiKeyRepo.findOne(key);

		return (apiKey != null) ?
				Optional.of(mapper.map(apiKey, ApiKey.class)):
				Optional.empty();
	}
}
