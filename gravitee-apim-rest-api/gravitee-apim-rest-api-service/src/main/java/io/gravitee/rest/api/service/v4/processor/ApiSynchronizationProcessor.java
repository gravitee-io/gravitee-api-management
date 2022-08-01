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
package io.gravitee.rest.api.service.v4.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.rest.api.model.DeploymentRequired;
import io.gravitee.rest.api.model.api.ApiEntity;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at gravitee.io)
 * @author GraviteeSource Team
 */
@Component("ApiSynchronizationProcessorV4")
public class ApiSynchronizationProcessor {

    private final Logger LOGGER = LoggerFactory.getLogger(ApiSynchronizationProcessor.class);

    @Autowired
    private ObjectMapper objectMapper;

    public boolean processCheckSynchronization(ApiEntity deployedApi, ApiEntity apiToDeploy) {
        Class<ApiEntity> cl = ApiEntity.class;
        List<Object> requiredFieldsDeployedApi = new ArrayList<Object>();
        List<Object> requiredFieldsApiToDeploy = new ArrayList<Object>();
        for (Field f : cl.getDeclaredFields()) {
            if (f.getAnnotation(DeploymentRequired.class) != null) {
                boolean previousAccessibleState = f.isAccessible();
                f.setAccessible(true);
                try {
                    requiredFieldsDeployedApi.add(f.get(deployedApi));
                    requiredFieldsApiToDeploy.add(f.get(apiToDeploy));
                } catch (Exception e) {
                    LOGGER.error("Error access API required deployment fields", e);
                } finally {
                    f.setAccessible(previousAccessibleState);
                }
            }
        }

        try {
            String requiredFieldsDeployedApiDefinition = objectMapper.writeValueAsString(requiredFieldsDeployedApi);
            String requiredFieldsApiToDeployDefinition = objectMapper.writeValueAsString(requiredFieldsApiToDeploy);

            return requiredFieldsDeployedApiDefinition.equals(requiredFieldsApiToDeployDefinition);
        } catch (Exception e) {
            LOGGER.error("Unexpected error while generating API deployment required fields definition", e);
            return false;
        }
    }
}
