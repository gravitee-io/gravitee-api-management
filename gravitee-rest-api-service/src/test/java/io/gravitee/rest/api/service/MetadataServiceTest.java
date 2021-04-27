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

import static io.gravitee.repository.management.model.MetadataReferenceType.API;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import io.gravitee.rest.api.model.MetadataFormat;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.MetadataServiceImpl;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import java.io.Reader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class MetadataServiceTest {

    @InjectMocks
    private MetadataServiceImpl metadataService;

    @Mock
    private NotificationTemplateService notificationTemplateService;

    @Test(expected = TechnicalManagementException.class)
    public void checkMetadataFormat_badEmailFormat() {
        metadataService.checkMetadataFormat(MetadataFormat.MAIL, "test");
    }

    @Test(expected = TechnicalManagementException.class)
    public void checkMetadataFormat_badURLFormat() {
        metadataService.checkMetadataFormat(MetadataFormat.URL, "test");
    }

    @Test(expected = TechnicalManagementException.class)
    public void checkMetadataFormat_badEmailFormat_EL() {
        when(this.notificationTemplateService.resolveInlineTemplateWithParam(anyString(), any(Reader.class), any())).thenReturn("test");
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail("test");
        PrimaryOwnerEntity primaryOwnerEntity = new PrimaryOwnerEntity(userEntity);
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setPrimaryOwner(primaryOwnerEntity);
        metadataService.checkMetadataFormat(MetadataFormat.MAIL, "${api.primaryOwner.email}", API, apiEntity);
    }

    @Test
    public void checkMetadataFormat_nominalCase_EL() {
        UserEntity userEntity = new UserEntity();
        userEntity.setEmail("gs@support.com");
        PrimaryOwnerEntity primaryOwnerEntity = new PrimaryOwnerEntity(userEntity);
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setPrimaryOwner(primaryOwnerEntity);
        metadataService.checkMetadataFormat(MetadataFormat.MAIL, "${api.primaryOwner.email}", API, apiEntity);
    }

    @Test
    public void checkMetadataFormat_userWithoutEmail() {
        UserEntity userEntity = new UserEntity();
        PrimaryOwnerEntity primaryOwnerEntity = new PrimaryOwnerEntity(userEntity);
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setPrimaryOwner(primaryOwnerEntity);
        metadataService.checkMetadataFormat(MetadataFormat.MAIL, "${(api.primaryOwner.email)!''}", API, apiEntity);
    }
}
