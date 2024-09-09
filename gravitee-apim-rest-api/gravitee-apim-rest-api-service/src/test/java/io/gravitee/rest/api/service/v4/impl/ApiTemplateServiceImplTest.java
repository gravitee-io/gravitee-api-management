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
package io.gravitee.rest.api.service.v4.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.rest.api.model.ApiMetadataEntity;
import io.gravitee.rest.api.model.ApiModel;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiModel;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.ApiTemplateService;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import java.io.Reader;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiTemplateServiceImplTest {

    @Mock
    private ApiSearchService apiSearchService;

    @Mock
    private ApiMetadataService apiMetadataService;

    @Mock
    private PrimaryOwnerService primaryOwnerService;

    @Mock
    private NotificationTemplateService notificationTemplateService;

    private ApiTemplateService apiTemplateService;

    @Before
    public void before() {
        apiTemplateService =
            new ApiTemplateServiceImpl(apiSearchService, apiMetadataService, primaryOwnerService, notificationTemplateService);
    }

    @Test
    public void shouldReturnApiModelV2WithNoDefinitionVersion() {
        ApiEntity apiEntity = new ApiEntity();
        when(apiSearchService.findGenericById(GraviteeContext.getExecutionContext(), "api")).thenReturn(apiEntity);

        GenericApiModel genericApiModel = apiTemplateService.findByIdForTemplates(GraviteeContext.getExecutionContext(), "api");
        assertTrue(genericApiModel instanceof ApiModel);
    }

    @Test
    public void shouldReturnApiModelV2WithV2DefinitionVersion() {
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setGraviteeDefinitionVersion("2.0.0");
        when(apiSearchService.findGenericById(GraviteeContext.getExecutionContext(), "api")).thenReturn(apiEntity);

        GenericApiModel genericApiModel = apiTemplateService.findByIdForTemplates(GraviteeContext.getExecutionContext(), "api");
        assertTrue(genericApiModel instanceof ApiModel);
    }

    @Test
    public void shouldReturnApiModelV4WithV4DefinitionVersion() {
        io.gravitee.rest.api.model.v4.api.ApiEntity apiEntity = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        when(apiSearchService.findGenericById(GraviteeContext.getExecutionContext(), "api")).thenReturn(apiEntity);

        GenericApiModel genericApiModel = apiTemplateService.findByIdForTemplates(GraviteeContext.getExecutionContext(), "api");
        assertTrue(genericApiModel instanceof io.gravitee.rest.api.model.v4.api.ApiModel);
    }

    @Test
    public void shouldReturnDecodedMetadata() {
        io.gravitee.rest.api.model.v4.api.ApiEntity apiEntity = new io.gravitee.rest.api.model.v4.api.ApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        when(apiSearchService.findGenericById(GraviteeContext.getExecutionContext(), "api")).thenReturn(apiEntity);

        ApiMetadataEntity apiMetadataEntity = new ApiMetadataEntity();
        apiMetadataEntity.setApiId("api");
        apiMetadataEntity.setKey("key");
        apiMetadataEntity.setValue("value");
        when(apiMetadataService.findAllByApi(GraviteeContext.getExecutionContext(), "api")).thenReturn(List.of(apiMetadataEntity));

        when(notificationTemplateService.resolveInlineTemplateWithParam(any(), any(), any(Reader.class), any()))
            .thenReturn("{key=value resolved}");

        when(primaryOwnerService.getPrimaryOwnerEmail(any(), any())).thenReturn("support@gravitee.test");

        GenericApiModel genericApiModel = apiTemplateService.findByIdForTemplates(GraviteeContext.getExecutionContext(), "api", true);
        assertTrue(genericApiModel instanceof io.gravitee.rest.api.model.v4.api.ApiModel);
        assertEquals("value resolved", genericApiModel.getMetadata().get("key"));
        assertEquals("support@gravitee.test", genericApiModel.getMetadata().get("email-support"));
    }
}
