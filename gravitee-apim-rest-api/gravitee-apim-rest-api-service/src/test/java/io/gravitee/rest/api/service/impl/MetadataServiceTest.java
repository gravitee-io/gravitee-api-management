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

import static io.gravitee.repository.management.model.MetadataReferenceType.API;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import freemarker.template.TemplateException;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.MetadataRepository;
import io.gravitee.repository.management.model.Metadata;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class MetadataServiceTest {

    @InjectMocks
    private MetadataServiceImpl metadataService;

    @Mock
    private AuditService auditService;

    @Mock
    private MetadataRepository metadataRepository;

    @Mock
    private NotificationTemplateService notificationTemplateService;

    @Test(expected = TechnicalManagementException.class)
    public void checkMetadataFormat_badEmailFormat() {
        metadataService.checkMetadataFormat(GraviteeContext.getExecutionContext(), MetadataFormat.MAIL, "test");
    }

    @Test(expected = TechnicalManagementException.class)
    public void checkMetadataFormat_badURLFormat() {
        metadataService.checkMetadataFormat(GraviteeContext.getExecutionContext(), MetadataFormat.URL, "test");
    }

    @Test(expected = TechnicalManagementException.class)
    public void checkMetadataFormat_badEmailFormat_EL() throws TemplateException {
        when(this.notificationTemplateService.resolveInlineTemplateWithParam(anyString(), anyString(), any(Reader.class), any()))
            .thenReturn("test");
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail("test");
        PrimaryOwnerEntity primaryOwnerEntity = new PrimaryOwnerEntity(userEntity);
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setPrimaryOwner(primaryOwnerEntity);
        metadataService.checkMetadataFormat(
            GraviteeContext.getExecutionContext(),
            MetadataFormat.MAIL,
            "${api.primaryOwner.email}",
            API,
            apiEntity
        );
    }

    @Test
    public void checkMetadataFormat_nominalCase_EL() {
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail("gs@support.com");
        PrimaryOwnerEntity primaryOwnerEntity = new PrimaryOwnerEntity(userEntity);
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setPrimaryOwner(primaryOwnerEntity);
        metadataService.checkMetadataFormat(
            GraviteeContext.getExecutionContext(),
            MetadataFormat.MAIL,
            "${api.primaryOwner.email}",
            API,
            apiEntity
        );
    }

    @Test
    public void checkMetadataFormat_userWithoutEmail() throws TemplateException {
        when(this.notificationTemplateService.resolveInlineTemplateWithParam(anyString(), anyString(), any(Reader.class), any()))
            .thenReturn("");
        UserEntity userEntity = new UserEntity();
        PrimaryOwnerEntity primaryOwnerEntity = new PrimaryOwnerEntity(userEntity);
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setPrimaryOwner(primaryOwnerEntity);
        metadataService.checkMetadataFormat(
            GraviteeContext.getExecutionContext(),
            MetadataFormat.MAIL,
            "${(api.primaryOwner.email)!''}",
            API,
            apiEntity
        );
    }

    @Test(expected = TechnicalManagementException.class)
    public void checkMetadataFormat_badDateFormat() {
        metadataService.checkMetadataFormat(GraviteeContext.getExecutionContext(), MetadataFormat.DATE, "2015-31-31");
    }

    @Test
    public void checkMetadataFormat_Date() {
        metadataService.checkMetadataFormat(GraviteeContext.getExecutionContext(), MetadataFormat.DATE, "2015-02-24");
    }

    @Test
    public void createWithReference() throws TechnicalException {
        when(metadataRepository.create(any(Metadata.class))).thenAnswer(i -> i.getArgument(0));

        final NewMetadataEntity metadata = new NewMetadataEntity();
        metadata.setFormat(MetadataFormat.STRING);
        metadata.setName("test");
        metadata.setValue("value");

        metadataService.create(GraviteeContext.getExecutionContext(), metadata, API, "apiId");

        verify(metadataRepository)
            .create(
                argThat(meta -> {
                    assertThat(meta.getFormat()).isEqualTo(io.gravitee.repository.management.model.MetadataFormat.STRING);
                    assertThat(meta.getName()).isEqualTo("test");
                    assertThat(meta.getValue()).isEqualTo("value");
                    return true;
                })
            );
    }

    @Test
    public void createWithReference_defaultFormat() throws TechnicalException {
        when(metadataRepository.create(any(Metadata.class))).thenAnswer(i -> i.getArgument(0));

        final NewMetadataEntity metadata = new NewMetadataEntity();
        metadata.setName("test");
        metadata.setValue("value");

        metadataService.create(GraviteeContext.getExecutionContext(), metadata, API, "apiId");

        verify(metadataRepository)
            .create(
                argThat(meta -> {
                    assertThat(meta.getFormat()).isEqualTo(io.gravitee.repository.management.model.MetadataFormat.STRING);
                    assertThat(meta.getName()).isEqualTo("test");
                    assertThat(meta.getValue()).isEqualTo("value");
                    return true;
                })
            );
    }

    @Test(expected = TechnicalManagementException.class)
    public void createWithReference_technicalException() throws TechnicalException {
        when(metadataRepository.create(any(Metadata.class))).thenThrow(new TechnicalException("Mock exception"));

        final NewMetadataEntity metadata = new NewMetadataEntity();
        metadata.setFormat(MetadataFormat.STRING);
        metadata.setName("test");
        metadata.setValue("value");

        metadataService.create(GraviteeContext.getExecutionContext(), metadata, API, "apiId");
    }

    @Test
    public void findByKeyAndReferenceType() throws TechnicalException {
        final ArrayList<Metadata> metadataList = new ArrayList<>();
        final Metadata metadata = new Metadata();
        metadata.setKey("key");
        metadata.setValue("value");
        metadata.setReferenceType(API);
        metadata.setReferenceId("apiId");
        metadata.setName("key");
        metadata.setFormat(io.gravitee.repository.management.model.MetadataFormat.STRING);
        metadataList.add(metadata);

        when(metadataRepository.findByKeyAndReferenceType("key", API)).thenReturn(metadataList);

        final List<MetadataEntity> metadataEntities = metadataService.findByKeyAndReferenceType("key", API);

        assertThat(metadataEntities).hasSize(1);
        final MetadataEntity metadataEntity = metadataEntities.get(0);
        assertThat(metadataEntity.getKey()).isEqualTo("key");
        assertThat(metadataEntity.getValue()).isEqualTo("value");
        assertThat(metadataEntity.getFormat()).isEqualTo(MetadataFormat.STRING);
    }

    @Test(expected = TechnicalManagementException.class)
    public void findByKeyAndReferenceType_technicalException() throws TechnicalException {
        when(metadataRepository.findByKeyAndReferenceType("key", API)).thenThrow(new TechnicalException("Mock exception"));

        metadataService.findByKeyAndReferenceType("key", API);
    }
}
