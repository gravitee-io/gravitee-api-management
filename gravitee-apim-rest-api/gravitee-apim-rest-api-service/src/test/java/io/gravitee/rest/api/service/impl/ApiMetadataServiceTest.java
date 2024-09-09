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
package io.gravitee.rest.api.service.impl;

import static io.gravitee.repository.management.model.Metadata.AuditEvent.*;
import static io.gravitee.repository.management.model.MetadataFormat.STRING;
import static io.gravitee.repository.management.model.MetadataReferenceType.API;
import static io.gravitee.repository.management.model.MetadataReferenceType.ENVIRONMENT;
import static io.gravitee.rest.api.service.impl.MetadataServiceImpl.getDefaultReferenceId;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MetadataRepository;
import io.gravitee.repository.management.model.Metadata;
import io.gravitee.repository.management.model.MetadataReferenceType;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.MetadataService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.DuplicateMetadataNameException;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiMetadataServiceTest {

    private static final String ENV_ID = "env#1";
    private static final String API_ID = "API123";
    private static final String METADATA_KEY = "MET123";
    private static final String DEFAULT_METADATA_KEY = "NOOVERRIDE";
    private static final String METADATA_NAME = "METNAME";
    private static final String METADATA_VALUE = "METVALUE";
    private static final Date METADATA_DATE = new Date();
    private static final String API_METADATA_VALUE = "APIMETVALUE";
    private static final String API_ID_NO_VALUE = "API456";
    private static final String METADATA_KEY_NO_VALUE = "MET456";

    @InjectMocks
    private final ApiMetadataService apiMetadataService = new ApiMetadataServiceImpl();

    @Mock
    private MetadataService metadataService;

    @Mock
    private ApiSearchService apiSearchService;

    @Mock
    private AuditService auditService;

    @Mock
    private MetadataRepository metadataRepository;

    @Mock
    private Metadata defaultMetadata;

    @Mock
    private Metadata apiMetadata;

    @Mock
    private Metadata apiMetadataWithoutValue;

    @Mock
    private MetadataEntity defaultMetadataEntity;

    @Mock
    private MetadataEntity defaultMetadataEntityWithoutOverride;

    @Mock
    private SearchEngineService searchEngineService;

    @Mock
    private NotificationTemplateService notificationTemplateService;

    @Before
    public void init() throws TechnicalException {
        mockMetadata(defaultMetadata, METADATA_KEY, ENVIRONMENT, ENV_ID, METADATA_VALUE);
        mockMetadata(apiMetadata, METADATA_KEY, API, API_ID, API_METADATA_VALUE);
        mockMetadata(apiMetadataWithoutValue, METADATA_KEY_NO_VALUE, API, API_ID_NO_VALUE, null);

        when(metadataRepository.findById(METADATA_KEY, API_ID, API)).thenReturn(of(apiMetadata));
        when(metadataRepository.findByReferenceTypeAndReferenceId(API, API_ID)).thenReturn(singletonList(apiMetadata));
        when(metadataRepository.findByReferenceTypeAndReferenceId(API, API_ID_NO_VALUE)).thenReturn(singletonList(apiMetadataWithoutValue));

        when(defaultMetadataEntity.getKey()).thenReturn(METADATA_KEY);
        when(defaultMetadataEntity.getFormat()).thenReturn(MetadataFormat.STRING);
        when(defaultMetadataEntity.getName()).thenReturn(METADATA_NAME);
        when(defaultMetadataEntity.getValue()).thenReturn(METADATA_VALUE);

        when(defaultMetadataEntityWithoutOverride.getKey()).thenReturn(DEFAULT_METADATA_KEY);
        when(defaultMetadataEntityWithoutOverride.getFormat()).thenReturn(MetadataFormat.STRING);
        when(defaultMetadataEntityWithoutOverride.getName()).thenReturn(METADATA_NAME);
        when(defaultMetadataEntityWithoutOverride.getValue()).thenReturn(METADATA_VALUE);

        when(metadataService.findByReferenceTypeAndReferenceId(ENVIRONMENT, ENV_ID))
            .thenReturn(Arrays.asList(defaultMetadataEntity, defaultMetadataEntityWithoutOverride));

        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId(API_ID);
        when(apiSearchService.findGenericById(any(), any())).thenReturn(apiEntity);
        GraviteeContext.setCurrentEnvironment(ENV_ID);
    }

    @After
    public void tearDown() throws TechnicalException {
        GraviteeContext.cleanContext();
    }

    private void mockMetadata(Metadata apiMetadata, String key, MetadataReferenceType api, String apiId, String apiMetadataValue) {
        when(apiMetadata.getKey()).thenReturn(key);
        when(apiMetadata.getFormat()).thenReturn(STRING);
        when(apiMetadata.getName()).thenReturn(METADATA_NAME);
        when(apiMetadata.getValue()).thenReturn(apiMetadataValue);
        when(apiMetadata.getUpdatedAt()).thenReturn(METADATA_DATE);
    }

    @Test
    public void shouldFindByIdAndApi() {
        final ApiMetadataEntity apiMetadataEntity = apiMetadataService.findByIdAndApi(
            GraviteeContext.getExecutionContext(),
            METADATA_KEY,
            API_ID
        );

        assertEquals(METADATA_KEY, apiMetadataEntity.getKey());
        assertEquals(API_ID, apiMetadataEntity.getApiId());
        assertEquals(METADATA_VALUE, apiMetadataEntity.getDefaultValue());
        assertEquals(METADATA_NAME, apiMetadataEntity.getName());
        assertEquals(API_METADATA_VALUE, apiMetadataEntity.getValue());
        assertEquals(MetadataFormat.STRING, apiMetadataEntity.getFormat());
    }

    @Test
    public void shouldFindAllByApi() {
        final List<ApiMetadataEntity> apiMetadataEntities = apiMetadataService.findAllByApi(GraviteeContext.getExecutionContext(), API_ID);

        assertNotNull(apiMetadataEntities);
        assertEquals(2, apiMetadataEntities.size());
        assertEquals(METADATA_KEY, apiMetadataEntities.get(1).getKey());
        assertEquals(API_ID, apiMetadataEntities.get(1).getApiId());
        assertEquals(METADATA_VALUE, apiMetadataEntities.get(1).getDefaultValue());
        assertEquals(METADATA_NAME, apiMetadataEntities.get(1).getName());
        assertEquals(API_METADATA_VALUE, apiMetadataEntities.get(1).getValue());
        assertEquals(MetadataFormat.STRING, apiMetadataEntities.get(1).getFormat());
    }

    @Test
    public void shouldFindByIdAndApiWithoutValue() {
        final ApiMetadataEntity apiMetadataEntity = apiMetadataService.findByIdAndApi(
            GraviteeContext.getExecutionContext(),
            METADATA_KEY_NO_VALUE,
            API_ID_NO_VALUE
        );

        assertEquals(METADATA_KEY_NO_VALUE, apiMetadataEntity.getKey());
        assertEquals(API_ID_NO_VALUE, apiMetadataEntity.getApiId());
        assertNull(apiMetadataEntity.getDefaultValue());
        assertEquals(METADATA_NAME, apiMetadataEntity.getName());
        assertEquals("", apiMetadataEntity.getValue());
        assertEquals(MetadataFormat.STRING, apiMetadataEntity.getFormat());
    }

    @Test
    public void shouldFindAllByApiWithoutValue() {
        final List<ApiMetadataEntity> apiMetadataEntities = apiMetadataService.findAllByApi(
            GraviteeContext.getExecutionContext(),
            API_ID_NO_VALUE
        );

        assertNotNull(apiMetadataEntities);
        assertEquals(3, apiMetadataEntities.size());
        assertEquals(METADATA_KEY_NO_VALUE, apiMetadataEntities.get(2).getKey());
        assertEquals(API_ID_NO_VALUE, apiMetadataEntities.get(2).getApiId());
        assertNull(apiMetadataEntities.get(2).getDefaultValue());
        assertEquals(METADATA_NAME, apiMetadataEntities.get(2).getName());
        assertEquals("", apiMetadataEntities.get(2).getValue());
        assertEquals(MetadataFormat.STRING, apiMetadataEntities.get(2).getFormat());
    }

    @Test(expected = DuplicateMetadataNameException.class)
    public void shouldNotCreateDuplicate() {
        final NewApiMetadataEntity newApiMetadataEntity = new NewApiMetadataEntity();
        newApiMetadataEntity.setApiId(API_ID);
        newApiMetadataEntity.setFormat(MetadataFormat.STRING);
        newApiMetadataEntity.setName(METADATA_NAME);
        newApiMetadataEntity.setValue(METADATA_VALUE);

        apiMetadataService.create(GraviteeContext.getExecutionContext(), newApiMetadataEntity);
    }

    @Test
    public void shouldCreate() throws TechnicalException {
        final NewApiMetadataEntity newApiMetadataEntity = new NewApiMetadataEntity();
        newApiMetadataEntity.setApiId(API_ID);
        newApiMetadataEntity.setFormat(MetadataFormat.STRING);
        final String metadataName = "New metadata";
        newApiMetadataEntity.setName(metadataName);
        newApiMetadataEntity.setValue(METADATA_VALUE);

        final ApiMetadataEntity createdApiMetadata = apiMetadataService.create(GraviteeContext.getExecutionContext(), newApiMetadataEntity);

        assertEquals("new-metadata", createdApiMetadata.getKey());
        assertEquals(MetadataFormat.STRING, createdApiMetadata.getFormat());
        assertEquals(metadataName, createdApiMetadata.getName());
        assertEquals(METADATA_VALUE, createdApiMetadata.getValue());

        final Metadata newApiMetadata = new Metadata();
        newApiMetadata.setKey("new-metadata");
        newApiMetadata.setReferenceType(API);
        newApiMetadata.setReferenceId(API_ID);
        newApiMetadata.setFormat(STRING);
        newApiMetadata.setName(metadataName);
        newApiMetadata.setValue(METADATA_VALUE);

        verify(metadataRepository).create(newApiMetadata);
        verify(auditService)
            .createApiAuditLog(
                eq(GraviteeContext.getExecutionContext()),
                eq(API_ID),
                any(),
                eq(METADATA_CREATED),
                any(),
                eq(null),
                eq(newApiMetadata)
            );
    }

    @Test
    public void shouldCreateMany() {
        final ApiMetadataEntity metadata1 = new ApiMetadataEntity();
        metadata1.setFormat(MetadataFormat.STRING);
        metadata1.setName("metadata1");
        metadata1.setValue("metadata1");

        final ApiMetadataEntity metadata2 = new ApiMetadataEntity();
        metadata2.setFormat(MetadataFormat.STRING);
        metadata2.setName("metadata2");
        metadata2.setValue("metadata2");

        final List<ApiMetadataEntity> created = apiMetadataService.create(
            GraviteeContext.getExecutionContext(),
            List.of(metadata1, metadata2),
            API_ID
        );

        Assertions
            .assertThat(created)
            .extracting(ApiMetadataEntity::getFormat, ApiMetadataEntity::getName, ApiMetadataEntity::getValue, ApiMetadataEntity::getApiId)
            .contains(
                Tuple.tuple(MetadataFormat.STRING, "metadata1", "metadata1", API_ID),
                Tuple.tuple(MetadataFormat.STRING, "metadata2", "metadata2", API_ID)
            );
    }

    @Test
    public void shouldUpdate() throws TechnicalException {
        final UpdateApiMetadataEntity updateApiMetadataEntity = new UpdateApiMetadataEntity();
        updateApiMetadataEntity.setKey(METADATA_KEY);
        updateApiMetadataEntity.setApiId(API_ID);
        updateApiMetadataEntity.setFormat(MetadataFormat.STRING);
        updateApiMetadataEntity.setName(METADATA_NAME);
        final String newValue = "new value";
        updateApiMetadataEntity.setValue(newValue);

        when(apiMetadata.getValue()).thenReturn(newValue);
        when(metadataRepository.update(any())).thenReturn(apiMetadata);

        final ApiMetadataEntity updatedApiMetadata = apiMetadataService.update(
            GraviteeContext.getExecutionContext(),
            updateApiMetadataEntity
        );

        assertEquals(METADATA_KEY, updatedApiMetadata.getKey());
        assertEquals(MetadataFormat.STRING, updatedApiMetadata.getFormat());
        assertEquals(METADATA_NAME, updatedApiMetadata.getName());
        assertEquals(newValue, updatedApiMetadata.getValue());

        final Metadata newApiMetadata = new Metadata();
        newApiMetadata.setKey(METADATA_KEY);
        newApiMetadata.setReferenceType(API);
        newApiMetadata.setReferenceId(API_ID);
        newApiMetadata.setFormat(STRING);
        newApiMetadata.setName(METADATA_NAME);
        newApiMetadata.setValue(newValue);

        verify(metadataRepository).update(newApiMetadata);
        verify(auditService)
            .createApiAuditLog(
                eq(GraviteeContext.getExecutionContext()),
                eq(API_ID),
                any(),
                eq(METADATA_UPDATED),
                any(),
                eq(apiMetadata),
                eq(newApiMetadata)
            );
        verify(searchEngineService, times(1)).index(eq(GraviteeContext.getExecutionContext()), any(), eq(false));
    }

    @Test
    public void shouldUpdateWithoutKey() throws TechnicalException {
        final UpdateApiMetadataEntity updateApiMetadataEntity = new UpdateApiMetadataEntity();
        updateApiMetadataEntity.setApiId(API_ID);
        updateApiMetadataEntity.setFormat(MetadataFormat.STRING);
        updateApiMetadataEntity.setName(METADATA_NAME);
        final String newValue = "new value";
        updateApiMetadataEntity.setValue(newValue);

        when(apiMetadata.getValue()).thenReturn(newValue);
        when(metadataRepository.findById(eq("metname"), any(), any())).thenReturn(Optional.of(apiMetadata));
        when(metadataRepository.update(any())).thenReturn(apiMetadata);

        final ApiMetadataEntity updatedApiMetadata = apiMetadataService.update(
            GraviteeContext.getExecutionContext(),
            updateApiMetadataEntity
        );

        assertEquals(METADATA_KEY, updatedApiMetadata.getKey());
        assertEquals(MetadataFormat.STRING, updatedApiMetadata.getFormat());
        assertEquals(METADATA_NAME, updatedApiMetadata.getName());
        assertEquals(newValue, updatedApiMetadata.getValue());

        final Metadata newApiMetadata = new Metadata();
        newApiMetadata.setReferenceType(API);
        newApiMetadata.setReferenceId(API_ID);
        newApiMetadata.setFormat(STRING);
        newApiMetadata.setName(METADATA_NAME);
        newApiMetadata.setKey("metname");
        newApiMetadata.setValue(newValue);

        verify(metadataRepository).update(newApiMetadata);
        verify(auditService)
            .createApiAuditLog(
                eq(GraviteeContext.getExecutionContext()),
                eq(API_ID),
                any(),
                eq(METADATA_UPDATED),
                any(),
                eq(apiMetadata),
                eq(newApiMetadata)
            );
        verify(searchEngineService, times(1)).index(eq(GraviteeContext.getExecutionContext()), any(), eq(false));
    }

    @Test
    public void shouldDelete() throws TechnicalException {
        apiMetadataService.delete(GraviteeContext.getExecutionContext(), METADATA_KEY, API_ID);

        verify(metadataRepository).delete(METADATA_KEY, API_ID, API);
        verify(auditService)
            .createApiAuditLog(
                eq(GraviteeContext.getExecutionContext()),
                eq(API_ID),
                any(),
                eq(METADATA_DELETED),
                any(),
                eq(apiMetadata),
                eq(null)
            );
    }

    @Test
    public void shouldDeleteAllByApi() throws TechnicalException {
        apiMetadataService.deleteAllByApi(GraviteeContext.getExecutionContext(), API_ID);

        verify(metadataRepository, times(1)).delete(any(), eq(API_ID), eq(MetadataReferenceType.API));
    }
}
