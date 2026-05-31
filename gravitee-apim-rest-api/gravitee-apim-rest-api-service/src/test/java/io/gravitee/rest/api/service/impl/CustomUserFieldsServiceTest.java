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

import static org.junit.jupiter.api.Assertions.*;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
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

        CustomUserFieldEntity createdEntity = service.create(GraviteeContext.getExecutionContext(), newFieldEntity);

        verify(customUserFieldsRepository).create(fieldCaptor.capture());
        verify(auditService).createOrganizationAuditLog(eq(GraviteeContext.getExecutionContext()), any());

        assertEquals(newFieldEntity.getKey().toLowerCase(), createdEntity.getKey(), "CustomUserField.key");
        assertEquals(newFieldEntity.getLabel(), createdEntity.getLabel(), "CustomUserField.label");
        assertEquals(newFieldEntity.isRequired(), createdEntity.isRequired(), "CustomUserField.required");
        if (newFieldEntity.getValues() != null) {
            assertNotNull(createdEntity.getValues(), "CustomUserField.values");
            assertEquals(1, createdEntity.getValues().size(), "CustomUserField.values.size");
            assertEquals(newFieldEntity.getValues().get(0), createdEntity.getValues().get(0), "CustomUserField.values.get(0)");
        }

        final CustomUserField customFieldRecord = fieldCaptor.getValue();
        assertNotNull(customFieldRecord);
        assertNotNull(customFieldRecord.getCreatedAt(), "CustomUserField.createAt");
        assertNotNull(customFieldRecord.getUpdatedAt(), "CustomUserField.updateAt");
        assertEquals(newFieldEntity.isRequired(), customFieldRecord.isRequired(), "CustomUserField.required");
        assertEquals(newFieldEntity.getKey().toLowerCase(), customFieldRecord.getKey(), "CustomUserField.key");
        assertEquals(newFieldEntity.getLabel(), customFieldRecord.getLabel(), "CustomUserField.label");
        assertEquals(ORG_ID, customFieldRecord.getReferenceId(), "CustomUserField.organization");
        assertEquals(REF_TYPE, customFieldRecord.getReferenceType(), "CustomUserField.refType");
        assertEquals(MetadataFormat.STRING, customFieldRecord.getFormat(), "CustomUserField.format");
        if (newFieldEntity.getValues() != null) {
            assertTrue(customFieldRecord.getValues() != null && customFieldRecord.getValues().contains("test"), "CustomUserField.values");
        }
    }

    @Test
    public void shouldNotCreateExistingField() throws Exception {
        assertThrows(CustomUserFieldAlreadyExistException.class, () -> {
            final CustomUserFieldEntity newFieldEntity = new CustomUserFieldEntity();
            newFieldEntity.setKey("NEWKEYUPPERCASE");
            newFieldEntity.setLabel("New Field Label");
            newFieldEntity.setRequired(true);
            newFieldEntity.setValues(Arrays.asList("test"));
            when(customUserFieldsRepository.findById(anyString(), anyString(), any())).thenReturn(Optional.of(mock(CustomUserField.class)));

            service.create(GraviteeContext.getExecutionContext(), newFieldEntity);

            verify(customUserFieldsRepository, never()).create(any());
        });
    }

    @Test
    public void shouldNotCreateInvalidKey_spaces() throws Exception {
        assertThrows(CustomUserFieldException.class, () -> {
            final CustomUserFieldEntity newFieldEntity = new CustomUserFieldEntity();
            newFieldEntity.setKey("NEW KEY UPPERCASE");
            newFieldEntity.setLabel("New Field Label");
            newFieldEntity.setRequired(true);
            newFieldEntity.setValues(Arrays.asList("test"));

            service.create(GraviteeContext.getExecutionContext(), newFieldEntity);

            verify(customUserFieldsRepository, never()).create(any());
        });
    }

    @Test
    public void shouldNotCreateInvalidKey_tooLong() throws Exception {
        assertThrows(CustomUserFieldException.class, () -> {
            final CustomUserFieldEntity newFieldEntity = new CustomUserFieldEntity();
            newFieldEntity.setKey(
                "abcdefghijklmnopqrstuvwxz_-5648521389794abcdefghijklmnopqrstuvwxz_-5648521389794abcdefghijklmnopqrstuvwxz_-5648521389794abcdefghijklmnopqrstuvwxz_-5648521389794abcdefghijklmnopqrstuvwxz_-5648521389794"
            );
            newFieldEntity.setLabel("New Field Label");
            newFieldEntity.setRequired(true);
            newFieldEntity.setValues(Arrays.asList("test"));

            service.create(GraviteeContext.getExecutionContext(), newFieldEntity);

            verify(customUserFieldsRepository, never()).create(any());
        });
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

        CustomUserFieldEntity updatedEntity = service.update(GraviteeContext.getExecutionContext(), toUpdateFieldEntity);

        verify(customUserFieldsRepository).update(fieldCaptor.capture());
        verify(auditService).createOrganizationAuditLog(eq(GraviteeContext.getExecutionContext()), any());

        assertEquals(toUpdateFieldEntity.getKey().toLowerCase(), updatedEntity.getKey(), "updatedCustomField.key");
        assertEquals(toUpdateFieldEntity.getLabel(), updatedEntity.getLabel(), "updatedCustomField.label");
        assertEquals(toUpdateFieldEntity.isRequired(), updatedEntity.isRequired(), "updatedCustomField.required");
        assertNotNull(updatedEntity.getValues(), "updatedCustomField.values");
        assertEquals(1, updatedEntity.getValues().size(), "updatedCustomField.values.size");
        assertEquals(toUpdateFieldEntity.getValues().get(0), updatedEntity.getValues().get(0), "updatedCustomField.values.get(0)");

        final CustomUserField customFieldRecord = fieldCaptor.getValue();
        assertNotNull(customFieldRecord);
        assertEquals(existingField.getCreatedAt(), customFieldRecord.getCreatedAt(), "update parameter createAt");
        assertEquals(existingField.getKey(), customFieldRecord.getKey(), "update parameter key");
        assertEquals(ORG_ID, customFieldRecord.getReferenceId(), "update parameter refId");
        assertEquals(REF_TYPE, customFieldRecord.getReferenceType(), "update parameter refType");
        assertEquals(MetadataFormat.STRING, customFieldRecord.getFormat(), "update parameter format");
        assertNotEquals(existingField.getUpdatedAt(), customFieldRecord.getUpdatedAt(), "update parameter updateAt");
        assertNotEquals(existingField.isRequired(), customFieldRecord.isRequired(), "update parameter required");
        assertNotEquals(existingField.getLabel(), customFieldRecord.getLabel(), "update parameter label");
        assertNotEquals(existingField.getValues(), customFieldRecord.getValues(), "update parameter values");
    }

    @Test
    public void shouldNotUpdateField_NotFound() throws Exception {
        assertThrows(CustomUserFieldNotFoundException.class, () -> {
            service.update(GraviteeContext.getExecutionContext(), mock(CustomUserFieldEntity.class));

            verify(customUserFieldsRepository, never()).update(any());
            verify(auditService, never()).createOrganizationAuditLog(GraviteeContext.getExecutionContext(), any());
        });
    }

    @Test
    public void shouldNotDeleteField_NotExist() throws Exception {
        service.delete(GraviteeContext.getExecutionContext(), "unknown");

        verify(customUserFieldsRepository, never()).delete(anyString(), anyString(), any());
        verify(auditService, never()).createOrganizationAuditLog(eq(GraviteeContext.getExecutionContext()), any());
    }

    @Test
    public void shouldDeleteField() throws Exception {
        final CustomUserField fieldMock = mock(CustomUserField.class);
        when(fieldMock.getKey()).thenReturn("validkey");
        when(fieldMock.getReferenceId()).thenReturn(ORG_ID);
        when(fieldMock.getReferenceType()).thenReturn(REF_TYPE);
        when(customUserFieldsRepository.findById(anyString(), anyString(), any())).thenReturn(Optional.of(fieldMock));

        service.delete(GraviteeContext.getExecutionContext(), "validKEY"); // no issue with upper case here, we want to test the sanitizer on the key

        verify(customUserFieldsRepository).delete("validkey", ORG_ID, REF_TYPE);
        verify(auditService).createOrganizationAuditLog(eq(GraviteeContext.getExecutionContext()), any());
        verify(ueUserMetadataService).deleteAllByCustomFieldId(GraviteeContext.getExecutionContext(), "validkey", ORG_ID, REF_TYPE);
    }

    @Test
    public void shouldListAllEnvField() throws Exception {
        final CustomUserField existingField1 = mock(CustomUserField.class);
        when(existingField1.getKey()).thenReturn("key1");
        when(existingField1.getFormat()).thenReturn(MetadataFormat.STRING);
        final CustomUserField existingField2 = mock(CustomUserField.class);
        when(existingField2.getKey()).thenReturn("key2");
        when(existingField2.getFormat()).thenReturn(MetadataFormat.STRING);
        when(customUserFieldsRepository.findByReferenceIdAndReferenceType(ORG_ID, REF_TYPE)).thenReturn(
            Arrays.asList(existingField1, existingField2)
        );

        List<CustomUserFieldEntity> entities = service.listAllFields(GraviteeContext.getExecutionContext());

        verify(customUserFieldsRepository).findByReferenceIdAndReferenceType(ORG_ID, REF_TYPE);
        assertNotNull(entities, "Fields");
        Assertions.assertThat(entities.stream().map(CustomUserFieldEntity::getKey).collect(Collectors.toList())).containsExactlyInAnyOrder(
            "key1",
            "key2"
        );
    }
}
