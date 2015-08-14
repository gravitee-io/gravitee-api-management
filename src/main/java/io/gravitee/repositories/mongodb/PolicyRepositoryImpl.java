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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.gravitee.repositories.mongodb.internal.model.PolicyMongo;
import io.gravitee.repositories.mongodb.internal.policy.PolicyMongoRepository;
import io.gravitee.repositories.mongodb.mapper.GraviteeMapper;
import io.gravitee.repository.api.PolicyRepository;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.model.Policy;

@Component
public class PolicyRepositoryImpl implements PolicyRepository{

	@Autowired
	private PolicyMongoRepository internalPolicyRepo;

	@Autowired
	private GraviteeMapper mapper;
	
	private Logger logger = LoggerFactory.getLogger(ApiRepositoryImpl.class);

	@Override
	public Set<Policy> findAll() throws TechnicalException {
		
		try{
			logger.error("Find all policy");
			
			List<PolicyMongo> policies = internalPolicyRepo.findAll();
			Set<Policy> res = mapper.collection2set(policies, PolicyMongo.class, Policy.class);
			
			logger.error("Find all policy - Done");
			return res;
			
		}catch(Exception e){
			
			logger.error("Find all policy - Error", e);
			throw new TechnicalException(e);
		}
	}

	@Override
	public Optional<Policy> findById(String id) throws TechnicalException {

		PolicyMongo policy = internalPolicyRepo.findOne(id);
		return Optional.ofNullable(mapper.map(policy, Policy.class));
	}

	@Override
	public Policy create(Policy policy) throws TechnicalException {
		
		PolicyMongo policyMongo = mapper.map(policy, PolicyMongo.class);
		PolicyMongo savedPolicy = internalPolicyRepo.insert(policyMongo);
		
		return mapper.map(savedPolicy, Policy.class);
	}

	@Override
	public Policy update(Policy policy) throws TechnicalException {
		
		PolicyMongo policyMongo = mapper.map(policy, PolicyMongo.class);
		PolicyMongo savedPolicy = internalPolicyRepo.save(policyMongo);
		
		return mapper.map(savedPolicy, Policy.class);
	}

	@Override
	public void delete(String id) throws TechnicalException {
		internalPolicyRepo.delete(id);
	}
}
