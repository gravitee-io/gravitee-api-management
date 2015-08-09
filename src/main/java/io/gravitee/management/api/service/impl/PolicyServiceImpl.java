package io.gravitee.management.api.service.impl;

import io.gravitee.management.api.model.PolicyEntity;
import io.gravitee.management.api.service.PolicyService;
import io.gravitee.repository.api.PolicyRepository;
import io.gravitee.repository.model.Policy;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Set;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class PolicyServiceImpl implements PolicyService {

    @Autowired
    private PolicyRepository policyRepository;

    @Override
    public Set<PolicyEntity> findAll() {
        Set<Policy> policies = policyRepository.findAll();
        Set<PolicyEntity> policyEntities = new HashSet<>(policies.size());

        for(Policy policy : policies) {
            policyEntities.add(convert(policy));
        }

        return policyEntities;
    }

    private PolicyEntity convert(Policy policy) {
        PolicyEntity entity = new PolicyEntity();

        entity.setId(policy.getId());
        entity.setName(policy.getName());
        entity.setDescription(policy.getDescription());
        entity.setVersion(policy.getVersion());

        return entity;
    }
}
