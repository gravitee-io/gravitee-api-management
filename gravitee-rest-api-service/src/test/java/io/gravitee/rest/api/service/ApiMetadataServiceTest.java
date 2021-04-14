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
package io.gravitee.rest.api.service;

import static io.gravitee.repository.management.model.Metadata.AuditEvent.*;
import static io.gravitee.repository.management.model.MetadataFormat.STRING;
import static io.gravitee.repository.management.model.MetadataReferenceType.API;
import static io.gravitee.repository.management.model.MetadataReferenceType.DEFAULT;
import static io.gravitee.rest.api.service.impl.MetadataServiceImpl.getDefaultReferenceId;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MetadataRepository;
import io.gravitee.repository.management.model.Metadata;
import io.gravitee.repository.management.model.MetadataReferenceType;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.service.exceptions.DuplicateMetadataNameException;
import io.gravitee.rest.api.service.impl.ApiMetadataServiceImpl;
import java.util.Date;
import java.util.List;
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

    private static final String API_ID = "API123";
    private static final String METADATA_KEY = "MET123";
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
    private ApiService apiService;

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

    @Before
    public void init() throws TechnicalException {
        mockMetadata(defaultMetadata, METADATA_KEY, DEFAULT, getDefaultReferenceId(), METADATA_VALUE);
        mockMetadata(apiMetadata, METADATA_KEY, API, API_ID, API_METADATA_VALUE);
        mockMetadata(apiMetadataWithoutValue, METADATA_KEY_NO_VALUE, API, API_ID_NO_VALUE, null);

        when(metadataRepository.findById(METADATA_KEY, API_ID, API)).thenReturn(of(apiMetadata));
        when(metadataRepository.findByReferenceTypeAndReferenceId(API, API_ID)).thenReturn(singletonList(apiMetadata));
        when(metadataRepository.findByReferenceTypeAndReferenceId(API, API_ID_NO_VALUE)).thenReturn(singletonList(apiMetadataWithoutValue));

        when(defaultMetadataEntity.getKey()).thenReturn(METADATA_KEY);
        when(defaultMetadataEntity.getFormat()).thenReturn(MetadataFormat.STRING);
        when(defaultMetadataEntity.getName()).thenReturn(METADATA_NAME);
        when(defaultMetadataEntity.getValue()).thenReturn(METADATA_VALUE);

        when(metadataService.findAllDefault()).thenReturn(singletonList(defaultMetadataEntity));
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
        final ApiMetadataEntity apiMetadataEntity = apiMetadataService.findByIdAndApi(METADATA_KEY, API_ID);

        assertEquals(METADATA_KEY, apiMetadataEntity.getKey());
        assertEquals(API_ID, apiMetadataEntity.getApiId());
        assertEquals(METADATA_VALUE, apiMetadataEntity.getDefaultValue());
        assertEquals(METADATA_NAME, apiMetadataEntity.getName());
        assertEquals(API_METADATA_VALUE, apiMetadataEntity.getValue());
        assertEquals(MetadataFormat.STRING, apiMetadataEntity.getFormat());
    }

    @Test
    public void shouldFindAllByApi() {
        final List<ApiMetadataEntity> apiMetadataEntities = apiMetadataService.findAllByApi(API_ID);

        assertNotNull(apiMetadataEntities);
        assertEquals(1, apiMetadataEntities.size());
        assertEquals(METADATA_KEY, apiMetadataEntities.get(0).getKey());
        assertEquals(API_ID, apiMetadataEntities.get(0).getApiId());
        assertEquals(METADATA_VALUE, apiMetadataEntities.get(0).getDefaultValue());
        assertEquals(METADATA_NAME, apiMetadataEntities.get(0).getName());
        assertEquals(API_METADATA_VALUE, apiMetadataEntities.get(0).getValue());
        assertEquals(MetadataFormat.STRING, apiMetadataEntities.get(0).getFormat());
    }

    @Test
    public void shouldFindByIdAndApiWithoutValue() {
        final ApiMetadataEntity apiMetadataEntity = apiMetadataService.findByIdAndApi(METADATA_KEY_NO_VALUE, API_ID_NO_VALUE);

        assertEquals(METADATA_KEY_NO_VALUE, apiMetadataEntity.getKey());
        assertEquals(API_ID_NO_VALUE, apiMetadataEntity.getApiId());
        assertNull(apiMetadataEntity.getDefaultValue());
        assertEquals(METADATA_NAME, apiMetadataEntity.getName());
        assertEquals("", apiMetadataEntity.getValue());
        assertEquals(MetadataFormat.STRING, apiMetadataEntity.getFormat());
    }

    @Test
    public void shouldFindAllByApiWithoutValue() {
        final List<ApiMetadataEntity> apiMetadataEntities = apiMetadataService.findAllByApi(API_ID_NO_VALUE);

        assertNotNull(apiMetadataEntities);
        assertEquals(2, apiMetadataEntities.size());
        assertEquals(METADATA_KEY_NO_VALUE, apiMetadataEntities.get(1).getKey());
        assertEquals(API_ID_NO_VALUE, apiMetadataEntities.get(1).getApiId());
        assertNull(apiMetadataEntities.get(1).getDefaultValue());
        assertEquals(METADATA_NAME, apiMetadataEntities.get(1).getName());
        assertEquals("", apiMetadataEntities.get(1).getValue());
        assertEquals(MetadataFormat.STRING, apiMetadataEntities.get(1).getFormat());
    }

    @Test(expected = DuplicateMetadataNameException.class)
    public void shouldNotCreateDuplicate() {
        final NewApiMetadataEntity newApiMetadataEntity = new NewApiMetadataEntity();
        newApiMetadataEntity.setApiId(API_ID);
        newApiMetadataEntity.setFormat(MetadataFormat.STRING);
        newApiMetadataEntity.setName(METADATA_NAME);
        newApiMetadataEntity.setValue(METADATA_VALUE);

        apiMetadataService.create(newApiMetadataEntity);
    }

    @Test
    public void shouldCreate() throws TechnicalException {
        final NewApiMetadataEntity newApiMetadataEntity = new NewApiMetadataEntity();
        newApiMetadataEntity.setApiId(API_ID);
        newApiMetadataEntity.setFormat(MetadataFormat.STRING);
        final String metadataName = "New metadata";
        newApiMetadataEntity.setName(metadataName);
        newApiMetadataEntity.setValue(METADATA_VALUE);

        final ApiMetadataEntity createdApiMetadata = apiMetadataService.create(newApiMetadataEntity);

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
        verify(auditService).createApiAuditLog(eq(API_ID), any(), eq(METADATA_CREATED), any(), eq(null), eq(newApiMetadata));
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

        final ApiMetadataEntity updatedApiMetadata = apiMetadataService.update(updateApiMetadataEntity);

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
        verify(auditService).createApiAuditLog(eq(API_ID), any(), eq(METADATA_UPDATED), any(), eq(apiMetadata), eq(newApiMetadata));
    }

    @Test
    public void shouldDelete() throws TechnicalException {
        apiMetadataService.delete(METADATA_KEY, API_ID);

        verify(metadataRepository).delete(METADATA_KEY, API_ID, API);
        verify(auditService).createApiAuditLog(eq(API_ID), any(), eq(METADATA_DELETED), any(), eq(apiMetadata), eq(null));
    }
}
