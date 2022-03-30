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

import static io.gravitee.repository.management.model.Audit.AuditProperties.METADATA;
import static io.gravitee.repository.management.model.Metadata.AuditEvent.METADATA_CREATED;
import static io.gravitee.repository.management.model.Metadata.AuditEvent.METADATA_UPDATED;
import static io.gravitee.repository.management.model.MetadataFormat.STRING;
import static io.gravitee.repository.management.model.MetadataReferenceType.USER;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.gravitee.common.data.domain.Page;
import io.gravitee.common.util.Maps;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MetadataRepository;
import io.gravitee.repository.management.api.UserRepository;
import io.gravitee.repository.management.model.Audit;
import io.gravitee.repository.management.model.CustomUserFieldReferenceType;
import io.gravitee.repository.management.model.Metadata;
import io.gravitee.repository.management.model.User;
import io.gravitee.rest.api.model.MetadataFormat;
import io.gravitee.rest.api.model.NewUserMetadataEntity;
import io.gravitee.rest.api.model.UpdateUserMetadataEntity;
import io.gravitee.rest.api.model.UserMetadataEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.MetadataService;
import io.gravitee.rest.api.service.UserMetadataService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Eric LELEU (eric.leleu at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class UserMetadataServiceTest {

    private static final String USER_ID = "USER123";
    private static final String METADATA_VALUE = "METVALUE";

    @InjectMocks
    private final UserMetadataService userMetadataService = new UserMetadataServiceImpl();

    @Mock
    private MetadataService metadataService;

    @Mock
    private AuditService auditService;

    @Mock
    private MetadataRepository metadataRepository;

    @Mock
    private UserRepository userRepository;

    private Metadata userMetadata;
    private Metadata toUserMetadata;

    @Before
    public void init() throws TechnicalException {
        userMetadata = new Metadata();
        userMetadata.setKey("key-metadata");
        userMetadata.setReferenceType(USER);
        userMetadata.setReferenceId(USER_ID);
        userMetadata.setFormat(STRING);
        userMetadata.setName(userMetadata.getKey());
        userMetadata.setValue(METADATA_VALUE);
        when(metadataRepository.findByReferenceTypeAndReferenceId(USER, USER_ID)).thenReturn(singletonList(userMetadata));

        toUserMetadata = new Metadata();
        toUserMetadata.setKey("key-metadata-toupdate");
        toUserMetadata.setReferenceType(USER);
        toUserMetadata.setReferenceId(USER_ID);
        toUserMetadata.setFormat(STRING);
        toUserMetadata.setName(toUserMetadata.getKey());
        toUserMetadata.setValue(METADATA_VALUE);
        when(metadataRepository.findById("key-metadata-toupdate", USER_ID, USER)).thenReturn(of(toUserMetadata));
    }

    @Test
    public void shouldCreate() throws TechnicalException {
        final NewUserMetadataEntity newUserMetadataEntity = new NewUserMetadataEntity();
        newUserMetadataEntity.setUserId(USER_ID);
        newUserMetadataEntity.setFormat(MetadataFormat.STRING);
        final String metadataName = "New-metadata";
        newUserMetadataEntity.setName(metadataName);
        newUserMetadataEntity.setValue(METADATA_VALUE);

        final UserMetadataEntity createdUserMetadata = userMetadataService.create(
            GraviteeContext.getExecutionContext(),
            newUserMetadataEntity
        );

        assertEquals("new-metadata", createdUserMetadata.getKey());
        assertEquals(MetadataFormat.STRING, createdUserMetadata.getFormat());
        assertEquals(metadataName, createdUserMetadata.getName());
        assertEquals(METADATA_VALUE, createdUserMetadata.getValue());

        final Metadata newUserMetadata = new Metadata();
        newUserMetadata.setKey("new-metadata");
        newUserMetadata.setReferenceType(USER);
        newUserMetadata.setReferenceId(USER_ID);
        newUserMetadata.setFormat(STRING);
        newUserMetadata.setName(metadataName);
        newUserMetadata.setValue(METADATA_VALUE);

        verify(metadataRepository).create(newUserMetadata);
        final Map<Audit.AuditProperties, String> properties = Maps
            .<Audit.AuditProperties, String>builder()
            .put(Audit.AuditProperties.USER, USER_ID)
            .put(METADATA, newUserMetadata.getKey())
            .build();
        verify(auditService)
            .createOrganizationAuditLog(
                eq(GraviteeContext.getExecutionContext()),
                eq(GraviteeContext.getCurrentOrganization()),
                eq(properties),
                eq(METADATA_CREATED),
                any(),
                eq(null),
                eq(newUserMetadata)
            );
    }

    @Test
    public void shouldUpdate() throws TechnicalException {
        final UpdateUserMetadataEntity updateUserMetadataEntity = new UpdateUserMetadataEntity();
        updateUserMetadataEntity.setUserId(USER_ID);
        updateUserMetadataEntity.setFormat(MetadataFormat.STRING);
        updateUserMetadataEntity.setName(toUserMetadata.getName());
        updateUserMetadataEntity.setKey(toUserMetadata.getKey());
        updateUserMetadataEntity.setValue(METADATA_VALUE + "updated");
        when(metadataRepository.update(any())).thenAnswer(a -> a.getArgument(0));

        final UserMetadataEntity updatedUserMetadata = userMetadataService.update(
            GraviteeContext.getExecutionContext(),
            updateUserMetadataEntity
        );

        assertEquals(toUserMetadata.getKey(), updatedUserMetadata.getKey());
        assertEquals(MetadataFormat.STRING, updatedUserMetadata.getFormat());
        assertEquals(toUserMetadata.getKey(), updatedUserMetadata.getName());
        assertEquals(METADATA_VALUE + "updated", updatedUserMetadata.getValue());

        final Metadata updatableMetadata = new Metadata();
        updatableMetadata.setKey(toUserMetadata.getKey());
        updatableMetadata.setReferenceType(USER);
        updatableMetadata.setReferenceId(USER_ID);
        updatableMetadata.setFormat(STRING);
        updatableMetadata.setName(toUserMetadata.getName());
        updatableMetadata.setValue(METADATA_VALUE + "updated");

        verify(metadataRepository).update(updatableMetadata);

        final Map<Audit.AuditProperties, String> properties = Maps
            .<Audit.AuditProperties, String>builder()
            .put(Audit.AuditProperties.USER, USER_ID)
            .put(METADATA, toUserMetadata.getKey())
            .build();
        verify(auditService)
            .createOrganizationAuditLog(
                eq(GraviteeContext.getExecutionContext()),
                eq(GraviteeContext.getCurrentOrganization()),
                eq(properties),
                eq(METADATA_UPDATED),
                any(),
                any(),
                any()
            );
    }

    @Test
    public void shouldFindByUserId() {
        final List<UserMetadataEntity> userMetadataEntities = userMetadataService.findAllByUserId(USER_ID);

        assertNotNull(userMetadataEntities);
        assertEquals(1, userMetadataEntities.size());
        assertEquals(userMetadata.getKey(), userMetadataEntities.get(0).getKey());
        assertEquals(USER_ID, userMetadataEntities.get(0).getUserId());
        assertNull(userMetadataEntities.get(0).getDefaultValue());
        assertEquals(userMetadata.getName(), userMetadataEntities.get(0).getName());
        assertEquals(userMetadata.getValue(), userMetadataEntities.get(0).getValue());
        assertEquals(MetadataFormat.STRING, userMetadataEntities.get(0).getFormat());
    }

    @Test
    public void shouldDeleteUserMetadataUsingMetaDataID() throws Exception {
        final String FIELD_KEY = "field_key";
        final String REF_ID = "ORGAID";
        final CustomUserFieldReferenceType REF_TYPE = CustomUserFieldReferenceType.ORGANIZATION;

        final String USER_ID_WITH_META = "userid";
        final String USER_ID_WITHOUT_META = "userid2";
        User user = new User();
        user.setId(USER_ID_WITH_META);
        User user2 = new User();
        user2.setId(USER_ID_WITHOUT_META);

        Page<User> pageOfUser = new Page<>(Arrays.asList(user, user2), 1, 2, 2);
        Page<User> emptyPage = new Page<>(Collections.emptyList(), 2, 0, 2);
        when(userRepository.search(any(), any())).thenReturn(pageOfUser, emptyPage);

        Metadata md = new Metadata();
        md.setKey(FIELD_KEY);
        md.setName(FIELD_KEY);
        md.setReferenceId(REF_ID);
        md.setReferenceType(USER);
        md.setValue("test value");
        md.setFormat(STRING);

        when(metadataRepository.findById(FIELD_KEY, USER_ID_WITH_META, USER)).thenReturn(of(md));
        when(metadataRepository.findById(FIELD_KEY, USER_ID_WITHOUT_META, USER)).thenReturn(empty());

        userMetadataService.deleteAllByCustomFieldId(GraviteeContext.getExecutionContext(), FIELD_KEY, REF_ID, REF_TYPE);

        verify(metadataRepository).delete(FIELD_KEY, USER_ID_WITH_META, USER);
        verify(metadataRepository, never()).delete(FIELD_KEY, USER_ID_WITHOUT_META, USER);
    }
}
