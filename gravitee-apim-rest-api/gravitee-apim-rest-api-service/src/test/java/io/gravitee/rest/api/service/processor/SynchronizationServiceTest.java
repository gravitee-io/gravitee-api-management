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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.services.Services;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import java.util.*;
import org.junit.Test;
import org.mockito.Mock;

/**
 * @author GraviteeSource Team
 */
public class SynchronizationServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ObjectMapper objectMapperMock;

    private final SynchronizationService synchronizationService = new SynchronizationService(objectMapper);

    /**
     * GIVEN an entity
     * WHEN the required entity fields are retrieved
     * THEN only the correct amount of required fields should be returned
     */
    @Test
    public void thenTheRequiredFieldsShouldBeReturned() {
        ApiEntity entity = new ApiEntity();

        entity.setCrossId("c38d779e-6e7e-472b-8d77-9e6e7e172b08");
        entity.setUpdatedAt(new Date());
        entity.setState(Lifecycle.State.INITIALIZED);
        entity.setPrimaryOwner(new PrimaryOwnerEntity());
        entity.setProperties(new Properties());
        entity.setServices(new Services());
        entity.setEntrypoints(new ArrayList<>());

        int apiEntityRequiredFieldCount = 13;

        List<Object> requiredFields = synchronizationService.getRequiredFieldsForComparison(ApiEntity.class, entity);

        assertThat(requiredFields.size() == apiEntityRequiredFieldCount, is(true));
    }

    /**
     * GIVEN two equal entities
     * WHEN the synchronization check is invoked
     * THEN the two equal entities should be deemed as in sync
     */
    @Test
    public void thenTwoEqualEntitiesShouldBeSynchronized() {
        ApiEntity deployedEntity = new ApiEntity();

        deployedEntity.setCrossId("c38d779e-6e7e-472b-8d77-9e6e7e172b08");
        deployedEntity.setUpdatedAt(new Date());
        deployedEntity.setState(Lifecycle.State.INITIALIZED);
        deployedEntity.setPrimaryOwner(new PrimaryOwnerEntity());
        deployedEntity.setProperties(new Properties());
        deployedEntity.setServices(new Services());
        deployedEntity.setEntrypoints(new ArrayList<>());

        ApiEntity entityToDeploy = new ApiEntity();

        entityToDeploy.setCrossId("c38d779e-6e7e-472b-8d77-9e6e7e172b08");
        entityToDeploy.setUpdatedAt(new Date());
        entityToDeploy.setState(Lifecycle.State.INITIALIZED);
        entityToDeploy.setPrimaryOwner(new PrimaryOwnerEntity());
        entityToDeploy.setProperties(new Properties());
        entityToDeploy.setServices(new Services());
        entityToDeploy.setEntrypoints(new ArrayList<>());

        boolean isSynchronized = synchronizationService.checkSynchronization(ApiEntity.class, deployedEntity, entityToDeploy);

        assertThat(isSynchronized, is(true));
    }

    /**
     * GIVEN two unequal entities
     * WHEN the synchronization check is invoked
     * THEN the two unequal entities should be out of sync
     */
    @Test
    public void thenTwoUnequalEntitiesShouldNotBeSynchronized() {
        ApiEntity deployedEntity = new ApiEntity();

        deployedEntity.setCrossId("c38d779e-6e7e-472b-8d77-9e6e7e172b08");
        deployedEntity.setUpdatedAt(new Date());
        deployedEntity.setState(Lifecycle.State.INITIALIZED);
        deployedEntity.setPrimaryOwner(new PrimaryOwnerEntity());
        deployedEntity.setProperties(new Properties());
        deployedEntity.setServices(new Services());
        deployedEntity.setEntrypoints(new ArrayList<>());
        deployedEntity.setId("1");

        ApiEntity entityToDeploy = new ApiEntity();

        entityToDeploy.setCrossId(null);
        entityToDeploy.setUpdatedAt(null);
        entityToDeploy.setState(null);
        entityToDeploy.setPrimaryOwner(null);
        entityToDeploy.setProperties(null);
        entityToDeploy.setServices(null);
        entityToDeploy.setEntrypoints(null);
        entityToDeploy.setId("2");

        boolean isSynchronized = synchronizationService.checkSynchronization(ApiEntity.class, deployedEntity, entityToDeploy);

        assertThat(isSynchronized, is(false));
    }

    /**
     * GIVEN an invalid field
     * WHEN we attempt to extract the field from the entity
     * THEN an error should be thrown regarding access to entity fields
     */
    @Test(expected = Exception.class)
    public void thenAnErrorShouldBeThrownRegardingAccessToEntityFields() {
        synchronizationService.addRequiredEntityFieldToList(null, null, null);

        fail("should throw Exception regarding access to entity fields");
    }

    /**
     * GIVEN an issue has occurred during synchronization checks
     * WHEN we attempt to check synchronization between two entities
     * THEN an error is thrown regarding field definition generation
     */
    @Test(expected = Exception.class)
    public void thenAnErrorShouldBeThrownRegardingFieldDefinitionGeneration() throws JsonProcessingException {
        doThrow(new RuntimeException()).when(objectMapperMock).writeValueAsString(any());

        SynchronizationService synchronizationServiceWithMock = new SynchronizationService(objectMapperMock);

        synchronizationServiceWithMock.checkSynchronization(null, null, null);

        fail("should throw Exception regarding field definition generation");
    }
}
