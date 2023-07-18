/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.services.Services;
import io.gravitee.rest.api.model.DeploymentRequired;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    private final ObjectMapper objectMapper;

    public SynchronizationService(final ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> boolean checkSynchronization(final Class<T> entityClass, final T deployedEntity, final T entityToDeploy) {
        List<Object> requiredFieldsDeployedApi = new ArrayList<Object>();
        List<Object> requiredFieldsApiToDeploy = new ArrayList<Object>();
        for (Field f : entityClass.getDeclaredFields()) {
            if (f.getAnnotation(DeploymentRequired.class) != null) {
                boolean previousAccessibleState = f.isAccessible();
                f.setAccessible(true);
                try {
                    if (f.get(deployedEntity) instanceof io.gravitee.definition.model.Proxy) {
                        // FIXME - dirty check to ensure the Proxy -> Group -> Endpoint comparison is correct
                        Endpoint deployedEntityProxyGroupEndpoint =
                            ((Proxy) f.get(deployedEntity)).getGroups()
                                .stream()
                                .findFirst()
                                .get()
                                .getEndpoints()
                                .stream()
                                .findFirst()
                                .get();
                        Endpoint entityToDeployProxyGroupEndpoint =
                            ((Proxy) f.get(entityToDeploy)).getGroups()
                                .stream()
                                .findFirst()
                                .get()
                                .getEndpoints()
                                .stream()
                                .findFirst()
                                .get();

                        if (!(deployedEntityProxyGroupEndpoint.equals(entityToDeployProxyGroupEndpoint))) {
                            return false;
                        }
                    } else {
                        // FIXME dirty hack to ignore null pointer and empty object comparisions on Properties & Services
                        if (
                            (f.get(deployedEntity) instanceof io.gravitee.definition.model.Properties) &&
                            ((Properties) f.get(deployedEntity)).getValues().size() == 0 &&
                            ((Properties) f.get(entityToDeploy)).getValues().size() == 0
                        ) {
                            continue;
                        }
                        if (
                            (f.get(deployedEntity) instanceof io.gravitee.definition.model.services.Services) &&
                            ((Services) f.get(deployedEntity)).isEmpty() &&
                            ((Services) f.get(entityToDeploy)).isEmpty()
                        ) {
                            continue;
                        }
                        requiredFieldsDeployedApi.add(f.get(deployedEntity));
                        requiredFieldsApiToDeploy.add(f.get(entityToDeploy));
                    }
                } catch (Exception e) {
                    LOGGER.error("Error access entity required deployment fields", e);
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
