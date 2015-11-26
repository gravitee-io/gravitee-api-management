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
package io.gravitee.management.service.impl;

import io.gravitee.management.model.PolicyEntity;
import io.gravitee.management.service.PolicyService;
import io.gravitee.plugin.policy.PolicyDefinition;
import io.gravitee.plugin.policy.PolicyManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Component
public class PolicyServiceImpl extends TransactionalService implements PolicyService {

    @Autowired
    private PolicyManager policyManager;

    @Override
    public Set<PolicyEntity> findAll() {
        final Collection<PolicyDefinition> policyDefinitions = policyManager.getPolicyDefinitions();

        return policyDefinitions.stream()
                .map(plugin -> convert(plugin))
                .collect(Collectors.toSet());
    }

    private PolicyEntity convert(PolicyDefinition policyDefinition) {
        PolicyEntity entity = new PolicyEntity();

        entity.setId(policyDefinition.id());
        entity.setDescription(policyDefinition.plugin().manifest().description());
        entity.setName(policyDefinition.plugin().manifest().name());
        entity.setVersion(policyDefinition.plugin().manifest().version());
        
        return entity;
    }
}
