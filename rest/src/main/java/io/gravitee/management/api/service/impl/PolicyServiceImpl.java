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
