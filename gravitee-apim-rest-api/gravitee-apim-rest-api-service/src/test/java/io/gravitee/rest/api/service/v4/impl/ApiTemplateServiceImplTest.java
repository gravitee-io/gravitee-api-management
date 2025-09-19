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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.nativeapi.NativeApiServices;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.rest.api.model.ApiMetadataEntity;
import io.gravitee.rest.api.model.ApiModel;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.v4.api.GenericApiModel;
import io.gravitee.rest.api.model.v4.nativeapi.NativeApiEntity;
import io.gravitee.rest.api.model.v4.nativeapi.NativeApiModel;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.ApiTemplateService;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import java.io.Reader;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        apiTemplateService = new ApiTemplateServiceImpl(
            apiSearchService,
            apiMetadataService,
            primaryOwnerService,
            notificationTemplateService
        );
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

        when(notificationTemplateService.resolveInlineTemplateWithParam(any(), any(), any(Reader.class), any())).thenReturn(
            "{key=value resolved}"
        );

        when(primaryOwnerService.getPrimaryOwnerEmail(any(), any())).thenReturn("support@gravitee.test");

        GenericApiModel genericApiModel = apiTemplateService.findByIdForTemplates(GraviteeContext.getExecutionContext(), "api", true);
        assertTrue(genericApiModel instanceof io.gravitee.rest.api.model.v4.api.ApiModel);
        assertEquals("value resolved", genericApiModel.getMetadata().get("key"));
        assertEquals("support@gravitee.test", genericApiModel.getMetadata().get("email-support"));
    }

    @Test
    public void shouldReturnNativeApiModelWithV4DefinitionVersion() {
        NativeApiEntity apiEntity = new NativeApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        apiEntity.setId("api-id");
        apiEntity.setName("Test API");
        apiEntity.setDescription("Test Description");
        apiEntity.setCreatedAt(new Date());
        apiEntity.setUpdatedAt(new Date());
        apiEntity.setDeployedAt(new Date());
        apiEntity.setGroups(Set.of("group1", "group2"));
        apiEntity.setVisibility(Visibility.PUBLIC);
        apiEntity.setCategories(Set.of("category1", "category2"));
        apiEntity.setApiVersion("1.0");
        apiEntity.setState(Lifecycle.State.STARTED);
        apiEntity.setTags(Set.of("tag1", "tag2"));
        apiEntity.setPicture("pictureData");
        apiEntity.setPrimaryOwner(new PrimaryOwnerEntity());
        apiEntity.setProperties(List.of(new Property("key", "value")));
        apiEntity.setLifecycleState(ApiLifecycleState.PUBLISHED);
        apiEntity.setDisableMembershipNotifications(true);
        apiEntity.setServices(new NativeApiServices());
        apiEntity.setListeners(List.of());
        apiEntity.setEndpointGroups(List.of());

        when(apiSearchService.findGenericById(GraviteeContext.getExecutionContext(), "api-id")).thenReturn(apiEntity);

        GenericApiModel genericApiModel = apiTemplateService.findByIdForTemplates(GraviteeContext.getExecutionContext(), "api-id");

        assertTrue(genericApiModel instanceof NativeApiModel);
        NativeApiModel apiModel = (NativeApiModel) genericApiModel;
        assertEquals("api-id", apiModel.getId());
        assertEquals("Test API", apiModel.getName());
        assertEquals("Test Description", apiModel.getDescription());
        assertEquals(apiEntity.getCreatedAt(), apiModel.getCreatedAt());
        assertEquals(apiEntity.getUpdatedAt(), apiModel.getUpdatedAt());
        assertEquals(apiEntity.getDeployedAt(), apiModel.getDeployedAt());
        assertEquals(apiEntity.getGroups(), apiModel.getGroups());
        assertEquals(apiEntity.getVisibility(), apiModel.getVisibility());
        assertEquals(apiEntity.getCategories(), apiModel.getCategories());
        assertEquals(apiEntity.getApiVersion(), apiModel.getApiVersion());
        assertEquals(apiEntity.getState(), apiModel.getState());
        assertEquals(apiEntity.getTags(), apiModel.getTags());
        assertEquals(apiEntity.getPicture(), apiModel.getPicture());
        assertEquals(apiEntity.getPrimaryOwner(), apiModel.getPrimaryOwner());
        assertEquals(apiEntity.getProperties(), apiModel.getProperties());
        assertEquals(apiEntity.getLifecycleState(), apiModel.getLifecycleState());
        assertEquals(apiEntity.isDisableMembershipNotifications(), apiModel.isDisableMembershipNotifications());
        assertEquals(apiEntity.getServices(), apiModel.getServices());
        assertEquals(apiEntity.getListeners(), apiModel.getListeners());
        assertEquals(apiEntity.getEndpointGroups(), apiModel.getEndpointGroups());
    }

    @Test
    public void shouldReturnDecodedMetadataForNativeApiEntity() {
        NativeApiEntity apiEntity = new NativeApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        apiEntity.setId("api-id");
        when(apiSearchService.findGenericById(GraviteeContext.getExecutionContext(), "api-id")).thenReturn(apiEntity);

        ApiMetadataEntity apiMetadataEntity = new ApiMetadataEntity();
        apiMetadataEntity.setApiId("api-id");
        apiMetadataEntity.setKey("key");
        apiMetadataEntity.setValue("value");
        when(apiMetadataService.findAllByApi(GraviteeContext.getExecutionContext(), "api-id")).thenReturn(List.of(apiMetadataEntity));

        when(notificationTemplateService.resolveInlineTemplateWithParam(any(), any(), any(Reader.class), any())).thenReturn(
            "{key=value resolved}"
        );

        when(primaryOwnerService.getPrimaryOwnerEmail(any(), any())).thenReturn("support@gravitee.test");

        GenericApiModel genericApiModel = apiTemplateService.findByIdForTemplates(GraviteeContext.getExecutionContext(), "api-id", true);

        assertTrue(genericApiModel instanceof NativeApiModel);
        assertEquals("value resolved", genericApiModel.getMetadata().get("key"));
        assertEquals("support@gravitee.test", genericApiModel.getMetadata().get("email-support"));
    }
}
