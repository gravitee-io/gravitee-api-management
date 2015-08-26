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

import io.gravitee.management.model.PolicyConfigurationEntity;
import io.gravitee.management.service.PolicyConfigurationService;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.repository.api.ApiRepository;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.model.Api;
import io.gravitee.repository.model.PolicyConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Component
public class PolicyConfigurationServiceImpl implements PolicyConfigurationService {

    /**
     * Logger.
     */
    private final Logger LOGGER = LoggerFactory.getLogger(PolicyConfigurationServiceImpl.class);

    @Autowired
    private ApiRepository apiRepository;

    @Override
    public List<PolicyConfigurationEntity> getPolicies(String apiName) {
        try {
            LOGGER.debug("Get policies for API: {}", apiName);
            List<PolicyConfiguration> policyConfigurations = apiRepository.findPoliciesByApi(apiName);
            List<PolicyConfigurationEntity> policyConfigurationEntities = new ArrayList<>(policyConfigurations.size());

            for(PolicyConfiguration policyConfiguration : policyConfigurations) {
                policyConfigurationEntities.add(convert(policyConfiguration));
            }

            return policyConfigurationEntities;
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get policies for API: {}", apiName, ex);
            throw new TechnicalManagementException("An error occurs while trying to get policies for API: " + apiName, ex);
        }
    }

    @Override
    public void updatePolicyConfigurations(String apiName, List<PolicyConfigurationEntity> policyConfigurationEntities) {
        try {
            LOGGER.debug("Update policies for API: {}", apiName);
            List<PolicyConfiguration> policyConfigurations = new ArrayList<>(policyConfigurationEntities.size());

            for(PolicyConfigurationEntity policyConfigurationEntity : policyConfigurationEntities) {
                PolicyConfiguration singleConfiguration = convert(policyConfigurationEntity);
                singleConfiguration.setCreatedAt(new Date());
                policyConfigurations.add(convert(policyConfigurationEntity));
            }

            apiRepository.updatePoliciesConfiguration(apiName, policyConfigurations);
            Api api = apiRepository.findByName(apiName).get();
            api.setUpdatedAt(new Date());
            apiRepository.update(api);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to update policies for API: {}", apiName, ex);
            throw new TechnicalManagementException("An error occurs while trying to update policies for API: " + apiName, ex);
        }
    }

    private static PolicyConfigurationEntity convert(PolicyConfiguration policyConfiguration) {
        PolicyConfigurationEntity policyConfigurationEntity = new PolicyConfigurationEntity();

        policyConfigurationEntity.setPolicy(policyConfiguration.getPolicy());
        policyConfigurationEntity.setConfiguration(policyConfiguration.getConfiguration());
        policyConfigurationEntity.setCreatedAt(policyConfiguration.getCreatedAt());

        return policyConfigurationEntity;
    }

    private static PolicyConfiguration convert(PolicyConfigurationEntity policyConfigurationEntity) {
        PolicyConfiguration policyConfiguration = new PolicyConfiguration();

        policyConfiguration.setPolicy(policyConfigurationEntity.getPolicy());
        policyConfiguration.setConfiguration(policyConfigurationEntity.getConfiguration());

        return policyConfiguration;
    }
}
