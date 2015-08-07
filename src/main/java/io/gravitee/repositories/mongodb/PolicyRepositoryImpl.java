package io.gravitee.repositories.mongodb;

import java.util.List;
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
	public Policy findById(String id) {

		PolicyMongo policy = internalPolicyRepo.findOne(id);
		return mapper.map(policy, Policy.class);
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
