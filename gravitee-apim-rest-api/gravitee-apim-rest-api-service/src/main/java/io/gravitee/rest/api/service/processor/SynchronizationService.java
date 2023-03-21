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
package io.gravitee.rest.api.service.processor;

import io.gravitee.definition.jackson.datatype.DeploymentRequiredMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at gravitee.io)
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class SynchronizationService {

    private final Logger LOGGER = LoggerFactory.getLogger(SynchronizationService.class);

    private final DeploymentRequiredMapper deploymentRequiredMapper;

    public SynchronizationService() {
        this.deploymentRequiredMapper = new DeploymentRequiredMapper();
    }

    public <T> boolean checkSynchronization(final T deployedEntity, final T entityToDeploy) {
        try {
            String requiredFieldsDeployedApiDefinition = deploymentRequiredMapper.writeValueAsString(deployedEntity);
            String requiredFieldsApiToDeployDefinition = deploymentRequiredMapper.writeValueAsString(entityToDeploy);

            return requiredFieldsDeployedApiDefinition.equals(requiredFieldsApiToDeployDefinition);
        } catch (Exception e) {
            LOGGER.error("Unexpected error while generating API deployment required fields definition", e);
            return false;
        }
    }
}
