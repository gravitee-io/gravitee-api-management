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
package io.gravitee.apim.infra.crud_service.application_metadata;

import static org.mockito.Mockito.verify;

import io.gravitee.definition.model.Origin;
import io.gravitee.rest.api.model.ApplicationMetadataEntity;
import io.gravitee.rest.api.model.NewApplicationMetadataEntity;
import io.gravitee.rest.api.model.UpdateApplicationMetadataEntity;
import io.gravitee.rest.api.service.ApplicationMetadataService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.UuidString;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Kamiel Ahmadpour (kamiel.ahmadpour at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
class ApplicationMetadataCrudServiceLegacyWrapperTest {

    @Mock
    ApplicationMetadataService applicationMetadataService;

    ApplicationMetadataCrudServiceLegacyWrapper service;
    private static final String ORGANIZATION_ID = "DEFAULT";
    private static final String ENVIRONMENT_ID = "DEFAULT";
    private static final String APP_METADATA_NAME = "test_app_metadata";
    private static final String APP_METADATA_NAME_VALUE = "test_app_metadata_value";
    private static final String APP_ID = UuidString.generateRandom();

    @BeforeEach
    void setUp() {
        service = new ApplicationMetadataCrudServiceLegacyWrapper(applicationMetadataService);
    }

    @Test
    void should_call_legacy_service_for_creation() {
        NewApplicationMetadataEntity newApplicationEntity = new NewApplicationMetadataEntity();
        newApplicationEntity.setName(APP_METADATA_NAME);
        newApplicationEntity.setOrigin(Origin.KUBERNETES);
        newApplicationEntity.setValue(APP_METADATA_NAME_VALUE);

        service.create(newApplicationEntity);

        verify(applicationMetadataService).create(new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID), newApplicationEntity);
    }

    @Test
    void should_call_legacy_service_for_update() {
        UpdateApplicationMetadataEntity updateApplicationMetadataEntity = new UpdateApplicationMetadataEntity();
        updateApplicationMetadataEntity.setName(APP_METADATA_NAME);
        updateApplicationMetadataEntity.setValue(APP_METADATA_NAME_VALUE);

        service.update(updateApplicationMetadataEntity);

        verify(applicationMetadataService).update(new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID), updateApplicationMetadataEntity);
    }

    @Test
    void should_call_legacy_service_for_delete() {
        ApplicationMetadataEntity metadata = new ApplicationMetadataEntity();
        metadata.setName(APP_METADATA_NAME);
        metadata.setValue(APP_METADATA_NAME_VALUE);
        metadata.setApplicationId(APP_ID);
        metadata.setKey(APP_METADATA_NAME);

        service.delete(metadata);

        verify(applicationMetadataService).delete(
            new ExecutionContext(ORGANIZATION_ID, ENVIRONMENT_ID),
            metadata.getKey(),
            metadata.getApplicationId()
        );
    }
}
