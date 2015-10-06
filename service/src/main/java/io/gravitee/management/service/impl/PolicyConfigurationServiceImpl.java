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

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.management.model.PolicyConfigurationEntity;
import io.gravitee.management.service.PolicyConfigurationService;
import io.gravitee.management.service.exceptions.NoValidDesciptorException;
import io.gravitee.management.service.exceptions.TechnicalManagementException;
import io.gravitee.repository.api.management.ApiRepository;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.model.management.Api;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
@Component
public class PolicyConfigurationServiceImpl extends TransactionalService implements PolicyConfigurationService {

    /**
     * Logger.
     */
    private final Logger LOGGER = LoggerFactory.getLogger(PolicyConfigurationServiceImpl.class);

    @Autowired
    private ApiRepository apiRepository;

    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public List<PolicyConfigurationEntity> getPolicies(String apiName) {
        try {
            LOGGER.debug("Get policies for API: {}", apiName);
            final String descriptorByApi = apiRepository.findDescriptorByApi(apiName);
            if (descriptorByApi == null || descriptorByApi.isEmpty()) {
                return Collections.emptyList();
            }
            try {
                return mapper.readValue(descriptorByApi, List.class);
            } catch (final Exception e) {
                LOGGER.error(e.getMessage(), e);
                throw new NoValidDesciptorException(descriptorByApi);
            }
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to get policies for API: {}", apiName, ex);
            throw new TechnicalManagementException("An error occurs while trying to get policies for API: " + apiName, ex);
        }
    }

    @Override
    public void updatePolicyConfigurations(String apiName, List<PolicyConfigurationEntity> policyConfigurationEntities) {
        try {
            LOGGER.debug("Update policies for API: {}", apiName);

            try {
                apiRepository.updateDescriptor(apiName, mapper.writeValueAsString(policyConfigurationEntities));
            } catch (final Exception e) {
                LOGGER.error(e.getMessage(), e);
                throw new TechnicalManagementException("Error while updating the API JSON descriptor", e);
            }

            Api api = apiRepository.findByName(apiName).get();
            api.setUpdatedAt(new Date());
            apiRepository.update(api);
        } catch (TechnicalException ex) {
            LOGGER.error("An error occurs while trying to update policies for API: {}", apiName, ex);
            throw new TechnicalManagementException("An error occurs while trying to update policies for API: " + apiName, ex);
        }
    }
}
