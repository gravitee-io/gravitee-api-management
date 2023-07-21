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
import io.gravitee.rest.api.model.DeploymentRequired;
import java.lang.reflect.Field;
import java.util.ArrayList;
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

    /**
     * Check synchronization between two entities by only comparing the required fields necessary for comparision
     *
     * @param entityClass    entity class type
     * @param deployedEntity currently deployed entity state
     * @param entityToDeploy proposed entity state to be deployed
     * @return the synchronization status
     */
    public <T> boolean checkSynchronization(final Class<T> entityClass, final T deployedEntity, final T entityToDeploy) {
        try {
            String requiredFieldsDeployedApiDefinition = objectMapper.writeValueAsString(
                getRequiredFieldsForComparison(entityClass, deployedEntity)
            );
            String requiredFieldsApiToDeployDefinition = objectMapper.writeValueAsString(
                getRequiredFieldsForComparison(entityClass, entityToDeploy)
            );

            return objectMapper
                .readTree(requiredFieldsDeployedApiDefinition)
                .equals(objectMapper.readTree(requiredFieldsApiToDeployDefinition));
        } catch (Exception e) {
            LOGGER.error("Unexpected error while generating API deployment required fields definition", e);
            return false;
        }
    }

    /**
     * Get required entity fields for synchronization checks
     *
     * @param entityClass entity class type
     * @param entity
     * @return the list of required entity fields
     */
    public <T> List<Object> getRequiredFieldsForComparison(final Class<T> entityClass, final T entity) {
        List<Object> requiredEntityFields = new ArrayList<>();

        for (Field entityField : entityClass.getDeclaredFields()) {
            addRequiredEntityFieldToList(entityField, entity, requiredEntityFields);
        }

        return requiredEntityFields;
    }

    /**
     * Add an entity field from the supplied entity to the given list
     * if the entity field is a required field
     *
     * @param entityField
     * @param entity
     * @param requiredEntityFields
     */
    public <T> void addRequiredEntityFieldToList(Field entityField, final T entity, List<Object> requiredEntityFields) {
        if (isFieldRequiredForDeployment(entityField)) {
            boolean previousAccessibleState = entityField.isAccessible();
            entityField.setAccessible(true);

            try {
                requiredEntityFields.add(entityField.get(entity));
            } catch (Exception e) {
                LOGGER.error("Error access entity required deployment fields", e);
            } finally {
                entityField.setAccessible(previousAccessibleState);
            }
        }
    }

    /**
     * Check if the field within a class has the DeploymentRequired
     * annotation
     *
     * @param classField The field within a class
     * @return whether the field is required
     */
    public boolean isFieldRequiredForDeployment(Field classField) {
        return classField.getAnnotation(DeploymentRequired.class) != null;
    }
}
