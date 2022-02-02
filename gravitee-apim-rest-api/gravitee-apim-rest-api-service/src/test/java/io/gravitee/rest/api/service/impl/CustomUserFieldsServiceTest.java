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
package io.gravitee.rest.api.service.impl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.CustomUserFieldsRepository;
import io.gravitee.repository.management.model.CustomUserField;
import io.gravitee.repository.management.model.CustomUserFieldReferenceType;
import io.gravitee.repository.management.model.MetadataFormat;
import io.gravitee.rest.api.model.CustomUserFieldEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.UserMetadataService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.CustomUserFieldAlreadyExistException;
import io.gravitee.rest.api.service.exceptions.CustomUserFieldException;
import io.gravitee.rest.api.service.exceptions.CustomUserFieldNotFoundException;
import io.gravitee.rest.api.service.impl.CustomUserFieldsServiceImpl;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class CustomUserFieldsServiceTest {

    private static final String ORG_ID = "DEFAULT";
    private static final CustomUserFieldReferenceType REF_TYPE = CustomUserFieldReferenceType.ORGANIZATION;

    @InjectMocks
    private CustomUserFieldsServiceImpl service;

    @Spy
    private ObjectMapper mapper = new ObjectMapper();

    @Mock
    private AuditService auditService;

    @Mock
    private CustomUserFieldsRepository customUserFieldsRepository;

    @Mock
    private UserMetadataService ueUserMetadataService;

    @Test
    public void shouldCreateNewField() throws Exception {
        innerShouldCreateField(true);
    }

    @Test
    public void shouldCreateNewField_NullValues() throws Exception {
        innerShouldCreateField(false);
    }

    private void innerShouldCreateField(boolean withValues) throws TechnicalException {
        final CustomUserFieldEntity newFieldEntity = new CustomUserFieldEntity();
        newFieldEntity.setKey("NEWKEYUPPERCASE");
        newFieldEntity.setLabel("New Field Label");
        newFieldEntity.setRequired(true);
        if (withValues) {
            newFieldEntity.setValues(Arrays.asList("test"));
        }

        when(customUserFieldsRepository.findById(anyString(), anyString(), any())).thenReturn(Optional.empty());
        when(customUserFieldsRepository.create(any())).thenAnswer(i -> i.getArguments()[0]);
        ArgumentCaptor<CustomUserField> fieldCaptor = ArgumentCaptor.forClass(CustomUserField.class);

        CustomUserFieldEntity createdEntity = service.create(newFieldEntity);

        verify(customUserFieldsRepository).create(fieldCaptor.capture());
        verify(auditService).createOrganizationAuditLog(eq(GraviteeContext.getCurrentOrganization()), anyMap(), any(), any(), any(), any());

        assertEquals("CustomUserField.key", newFieldEntity.getKey().toLowerCase(), createdEntity.getKey());
        assertEquals("CustomUserField.label", newFieldEntity.getLabel(), createdEntity.getLabel());
        assertEquals("CustomUserField.required", newFieldEntity.isRequired(), createdEntity.isRequired());
        if (newFieldEntity.getValues() != null) {
            assertNotNull("CustomUserField.values", createdEntity.getValues());
            assertEquals("CustomUserField.values.size", 1, createdEntity.getValues().size());
            assertEquals("CustomUserField.values.get(0)", newFieldEntity.getValues().get(0), createdEntity.getValues().get(0));
        }

        final CustomUserField customFieldRecord = fieldCaptor.getValue();
        assertNotNull(customFieldRecord);
        assertNotNull("CustomUserField.createAt", customFieldRecord.getCreatedAt());
        assertNotNull("CustomUserField.updateAt", customFieldRecord.getUpdatedAt());
        assertEquals("CustomUserField.required", newFieldEntity.isRequired(), customFieldRecord.isRequired());
        assertEquals("CustomUserField.key", newFieldEntity.getKey().toLowerCase(), customFieldRecord.getKey());
        assertEquals("CustomUserField.label", newFieldEntity.getLabel(), customFieldRecord.getLabel());
        assertEquals("CustomUserField.organization", ORG_ID, customFieldRecord.getReferenceId());
        assertEquals("CustomUserField.refType", REF_TYPE, customFieldRecord.getReferenceType());
        assertEquals("CustomUserField.format", MetadataFormat.STRING, customFieldRecord.getFormat());
        if (newFieldEntity.getValues() != null) {
            assertTrue("CustomUserField.values", customFieldRecord.getValues() != null && customFieldRecord.getValues().contains("test"));
        }
    }

    @Test(expected = CustomUserFieldAlreadyExistException.class)
    public void shouldNotCreateExistingField() throws Exception {
        final CustomUserFieldEntity newFieldEntity = new CustomUserFieldEntity();
        newFieldEntity.setKey("NEWKEYUPPERCASE");
        newFieldEntity.setLabel("New Field Label");
        newFieldEntity.setRequired(true);
        newFieldEntity.setValues(Arrays.asList("test"));
        when(customUserFieldsRepository.findById(anyString(), anyString(), any())).thenReturn(Optional.of(mock(CustomUserField.class)));

        service.create(newFieldEntity);

        verify(customUserFieldsRepository, never()).create(any());
    }

    @Test(expected = CustomUserFieldException.class)
    public void shouldNotCreateInvalidKey_spaces() throws Exception {
        final CustomUserFieldEntity newFieldEntity = new CustomUserFieldEntity();
        newFieldEntity.setKey("NEW KEY UPPERCASE");
        newFieldEntity.setLabel("New Field Label");
        newFieldEntity.setRequired(true);
        newFieldEntity.setValues(Arrays.asList("test"));

        service.create(newFieldEntity);

        verify(customUserFieldsRepository, never()).create(any());
    }

    @Test(expected = CustomUserFieldException.class)
    public void shouldNotCreateInvalidKey_tooLong() throws Exception {
        final CustomUserFieldEntity newFieldEntity = new CustomUserFieldEntity();
        newFieldEntity.setKey(
            "abcdefghijklmnopqrstuvwxz_-5648521389794abcdefghijklmnopqrstuvwxz_-5648521389794abcdefghijklmnopqrstuvwxz_-5648521389794abcdefghijklmnopqrstuvwxz_-5648521389794abcdefghijklmnopqrstuvwxz_-5648521389794"
        );
        newFieldEntity.setLabel("New Field Label");
        newFieldEntity.setRequired(true);
        newFieldEntity.setValues(Arrays.asList("test"));

        service.create(newFieldEntity);

        verify(customUserFieldsRepository, never()).create(any());
    }

    @Test
    public void shouldUpdateField() throws Exception {
        final CustomUserField existingField = new CustomUserField();
        final String KEY = "NEWKEYUPPERCASE";
        existingField.setKey(KEY.toLowerCase());
        existingField.setLabel("Field Label");
        existingField.setReferenceId(ORG_ID);
        existingField.setReferenceType(REF_TYPE);
        existingField.setRequired(true);
        existingField.setFormat(MetadataFormat.STRING);
        existingField.setValues(Arrays.asList("test"));
        Date createdAt = new Date(Instant.now().minus(1, ChronoUnit.HOURS).toEpochMilli());
        existingField.setCreatedAt(createdAt);
        existingField.setUpdatedAt(createdAt);

        final CustomUserFieldEntity toUpdateFieldEntity = new CustomUserFieldEntity();
        toUpdateFieldEntity.setKey(KEY);
        toUpdateFieldEntity.setLabel("Updated Field Label");
        toUpdateFieldEntity.setRequired(false);
        toUpdateFieldEntity.setValues(Arrays.asList("test2"));

        when(customUserFieldsRepository.findById(anyString(), anyString(), any())).thenReturn(Optional.of(existingField));
        when(customUserFieldsRepository.update(any())).thenAnswer(i -> i.getArguments()[0]);
        ArgumentCaptor<CustomUserField> fieldCaptor = ArgumentCaptor.forClass(CustomUserField.class);

        CustomUserFieldEntity updatedEntity = service.update(toUpdateFieldEntity);

        verify(customUserFieldsRepository).update(fieldCaptor.capture());
        verify(auditService).createOrganizationAuditLog(eq(GraviteeContext.getCurrentOrganization()), anyMap(), any(), any(), any(), any());

        assertEquals("updatedCustomField.key", toUpdateFieldEntity.getKey().toLowerCase(), updatedEntity.getKey());
        assertEquals("updatedCustomField.label", toUpdateFieldEntity.getLabel(), updatedEntity.getLabel());
        assertEquals("updatedCustomField.required", toUpdateFieldEntity.isRequired(), updatedEntity.isRequired());
        assertNotNull("updatedCustomField.values", updatedEntity.getValues());
        assertEquals("updatedCustomField.values.size", 1, updatedEntity.getValues().size());
        assertEquals("updatedCustomField.values.get(0)", toUpdateFieldEntity.getValues().get(0), updatedEntity.getValues().get(0));

        final CustomUserField customFieldRecord = fieldCaptor.getValue();
        assertNotNull(customFieldRecord);
        assertEquals("update parameter createAt", existingField.getCreatedAt(), customFieldRecord.getCreatedAt());
        assertEquals("update parameter key", existingField.getKey(), customFieldRecord.getKey());
        assertEquals("update parameter refId", ORG_ID, customFieldRecord.getReferenceId());
        assertEquals("update parameter refType", REF_TYPE, customFieldRecord.getReferenceType());
        assertEquals("update parameter format", MetadataFormat.STRING, customFieldRecord.getFormat());
        assertNotEquals("update parameter updateAt", existingField.getUpdatedAt(), customFieldRecord.getUpdatedAt());
        assertNotEquals("update parameter required", existingField.isRequired(), customFieldRecord.isRequired());
        assertNotEquals("update parameter label", existingField.getLabel(), customFieldRecord.getLabel());
        assertNotEquals("update parameter values", existingField.getValues(), customFieldRecord.getValues());
    }

    @Test(expected = CustomUserFieldNotFoundException.class)
    public void shouldNotUpdateField_NotFound() throws Exception {
        service.update(mock(CustomUserFieldEntity.class));

        verify(customUserFieldsRepository, never()).update(any());
        verify(auditService, never())
            .createOrganizationAuditLog(GraviteeContext.getCurrentOrganization(), anyMap(), any(), any(), any(), any());
    }

    @Test
    public void shouldNotDeleteField_NotExist() throws Exception {
        service.delete("unknown");

        verify(customUserFieldsRepository, never()).delete(anyString(), anyString(), any());
        verify(auditService, never())
            .createOrganizationAuditLog(eq(GraviteeContext.getCurrentOrganization()), anyMap(), any(), any(), any(), any());
    }

    @Test
    public void shouldDeleteField() throws Exception {
        final CustomUserField fieldMock = mock(CustomUserField.class);
        when(fieldMock.getKey()).thenReturn("validkey");
        when(fieldMock.getReferenceId()).thenReturn(ORG_ID);
        when(fieldMock.getReferenceType()).thenReturn(REF_TYPE);
        when(customUserFieldsRepository.findById(anyString(), anyString(), any())).thenReturn(Optional.of(fieldMock));

        service.delete("validKEY"); // no issue with upper case here, we want to test the sanitizer on the key

        verify(customUserFieldsRepository).delete("validkey", ORG_ID, REF_TYPE);
        verify(auditService).createOrganizationAuditLog(eq(GraviteeContext.getCurrentOrganization()), anyMap(), any(), any(), any(), any());
        verify(ueUserMetadataService).deleteAllByCustomFieldId("validkey", ORG_ID, REF_TYPE);
    }

    @Test
    public void shouldListAllEnvField() throws Exception {
        final CustomUserField existingField1 = mock(CustomUserField.class);
        when(existingField1.getKey()).thenReturn("key1");
        when(existingField1.getFormat()).thenReturn(MetadataFormat.STRING);
        final CustomUserField existingField2 = mock(CustomUserField.class);
        when(existingField2.getKey()).thenReturn("key2");
        when(existingField2.getFormat()).thenReturn(MetadataFormat.STRING);
        when(customUserFieldsRepository.findByReferenceIdAndReferenceType(ORG_ID, REF_TYPE))
            .thenReturn(Arrays.asList(existingField1, existingField2));

        List<CustomUserFieldEntity> entities = service.listAllFields();

        verify(customUserFieldsRepository).findByReferenceIdAndReferenceType(ORG_ID, REF_TYPE);
        assertNotNull("Fields", entities);
        Assertions
            .assertThat(entities.stream().map(CustomUserFieldEntity::getKey).collect(Collectors.toList()))
            .containsExactlyInAnyOrder("key1", "key2");
    }
}
