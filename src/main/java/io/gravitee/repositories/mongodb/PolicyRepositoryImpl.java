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

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.gravitee.repositories.mongodb.internal.model.PolicyMongo;
import io.gravitee.repositories.mongodb.internal.policy.PolicyMongoRepository;
import io.gravitee.repositories.mongodb.mapper.GraviteeMapper;
import io.gravitee.repository.api.PolicyRepository;
import io.gravitee.repository.model.Policy;

@Component
public class PolicyRepositoryImpl implements PolicyRepository{

	@Autowired
	private PolicyMongoRepository internalPolicyRepo;

	@Autowired
	private GraviteeMapper mapper;

	@Override
	public Set<Policy> findAll() {
	
		List<PolicyMongo> policies = internalPolicyRepo.findAll();
		Set<Policy> res = mapper.collection2set(policies, PolicyMongo.class, Policy.class);
		return res;
	}

	@Override
	public Optional<Policy> findById(String id) {

		PolicyMongo policy = internalPolicyRepo.findOne(id);
		return Optional.ofNullable(mapper.map(policy, Policy.class));
	}

	@Override
	public Set<Policy> findByApi(String apiName) {
	
		//List<PolicyMongo> policies = internalPolicyRepo.findByApi(apiName);
		//Set<Policy> res = mapper.collection2set(policies, PolicyMongo.class, Policy.class);
		//return res;
		
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getConfigurationSchema() {
		// TODO Auto-generated method stub
		return null;
	}

	
}
