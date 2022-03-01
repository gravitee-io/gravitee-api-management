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
package io.gravitee.rest.api.service.cockpit.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.model.api.*;
import io.gravitee.rest.api.model.documentation.PageQuery;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.cockpit.model.DeploymentMode;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.PageConverter;
import io.gravitee.rest.api.service.exceptions.ApiContextPathAlreadyExistsException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.powermock.core.classloader.annotations.PrepareForTest;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
@PrepareForTest(GraviteeContext.class)
public class ApiServiceCockpitImplTest {

    private static final String API_ID = "api#id";
    private static final List<String> LABELS = List.of("label1", "label2");
    private static final String USER_ID = "user#id";
    private static final String ENVIRONMENT_ID = "environment#id";
    private static final String PAGE_ID = "page#id";
    private static final String SWAGGER_DEFINITION = "";

    @Mock
    private ApiService apiService;

    @Mock
    private SwaggerService swaggerService;

    @Mock
    private PageService pageService;

    @Mock
    private ApiMetadataService apiMetadataService;

    @Mock
    private PlanService planService;

    @Mock
    private VirtualHostService virtualHostService;

    private ApiServiceCockpitImpl service;

    @Captor
    private ArgumentCaptor<ImportSwaggerDescriptorEntity> descriptorCaptor;

    @Captor
    private ArgumentCaptor<ObjectNode> apiDefinitionCaptor;

    @Captor
    private ArgumentCaptor<NewPlanEntity> newPlanCaptor;

    @Captor
    private ArgumentCaptor<ApiDeploymentEntity> apiDeploymentCaptor;

    @Captor
    private ArgumentCaptor<UpdateApiEntity> updateApiCaptor;

    @Captor
    private ArgumentCaptor<UpdatePageEntity> updatePageCaptor;

    private ApiConverter apiConverter = new ApiConverter();

    private PageConverter pageConverter = new PageConverter();

    @Before
    public void setUp() throws Exception {
        service =
            new ApiServiceCockpitImpl(
                new ObjectMapper(),
                apiService,
                swaggerService,
                pageService,
                apiMetadataService,
                planService,
                virtualHostService,
                apiConverter,
                pageConverter
            );
    }

    @Test
    public void should_create_documented_api() {
        ImportSwaggerDescriptorEntity expectedDescriptor = new ImportSwaggerDescriptorEntity();
        expectedDescriptor.setPayload(SWAGGER_DEFINITION);
        expectedDescriptor.setWithDocumentation(true);
        expectedDescriptor.setWithPolicyPaths(true);

        SwaggerApiEntity swaggerApi = new SwaggerApiEntity();
        swaggerApi.setMetadata(new ArrayList<>());
        Proxy proxy = new Proxy();
        VirtualHost virtualHost = new VirtualHost();
        proxy.setVirtualHosts(List.of(virtualHost));
        swaggerApi.setProxy(proxy);

        ApiEntity api = new ApiEntity();
        api.setId(API_ID);
        api.setLabels(LABELS);

        when(swaggerService.createAPI(any(ImportSwaggerDescriptorEntity.class), eq(DefinitionVersion.V2))).thenReturn(swaggerApi);
        when(apiService.createWithApiDefinition(eq(swaggerApi), eq(USER_ID), any(ObjectNode.class))).thenReturn(api);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);

        service.createApi(API_ID, USER_ID, SWAGGER_DEFINITION, ENVIRONMENT_ID, DeploymentMode.API_DOCUMENTED, LABELS);

        verify(swaggerService).createAPI(descriptorCaptor.capture(), eq(DefinitionVersion.V2));
        assertThat(descriptorCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedDescriptor);

        verify(apiService).createWithApiDefinition(eq(swaggerApi), eq(USER_ID), apiDefinitionCaptor.capture());
        assertThat(apiDefinitionCaptor.getValue().get("id")).isEqualTo(new JsonNodeFactory(false).textNode(API_ID));

        verify(pageService).createAsideFolder(API_ID, ENVIRONMENT_ID);
        verify(pageService).createOrUpdateSwaggerPage(eq(API_ID), any(ImportSwaggerDescriptorEntity.class), eq(true));
        verify(apiMetadataService).create(same(swaggerApi.getMetadata()), eq(API_ID));
    }

    @Test
    public void should_not_start_a_documented_api() {
        SwaggerApiEntity swaggerApi = new SwaggerApiEntity();
        swaggerApi.setMetadata(new ArrayList<>());
        Proxy proxy = new Proxy();
        VirtualHost virtualHost = new VirtualHost();
        proxy.setVirtualHosts(List.of(virtualHost));
        swaggerApi.setProxy(proxy);

        ApiEntity api = new ApiEntity();
        api.setId(API_ID);

        when(swaggerService.createAPI(any(ImportSwaggerDescriptorEntity.class), eq(DefinitionVersion.V2))).thenReturn(swaggerApi);
        when(apiService.createWithApiDefinition(eq(swaggerApi), eq(USER_ID), any(ObjectNode.class))).thenReturn(api);

        service.createApi(API_ID, USER_ID, SWAGGER_DEFINITION, ENVIRONMENT_ID, DeploymentMode.API_DOCUMENTED, LABELS);

        verifyNoInteractions(planService);
        verify(apiService, never()).start(anyString(), anyString());
    }

    @Test
    public void should_create_a_mocked_api() {
        ImportSwaggerDescriptorEntity expectedDescriptor = new ImportSwaggerDescriptorEntity();
        expectedDescriptor.setPayload(SWAGGER_DEFINITION);
        expectedDescriptor.setWithDocumentation(true);
        expectedDescriptor.setWithPolicyPaths(true);
        expectedDescriptor.setWithPolicies(List.of("mock"));

        SwaggerApiEntity swaggerApi = new SwaggerApiEntity();
        swaggerApi.setMetadata(new ArrayList<>());
        Proxy proxy = new Proxy();
        VirtualHost virtualHost = new VirtualHost();
        proxy.setVirtualHosts(List.of(virtualHost));
        swaggerApi.setProxy(proxy);

        ApiEntity api = new ApiEntity();
        api.setId(API_ID);

        when(swaggerService.createAPI(any(ImportSwaggerDescriptorEntity.class), eq(DefinitionVersion.V2))).thenReturn(swaggerApi);
        when(apiService.createWithApiDefinition(eq(swaggerApi), eq(USER_ID), any(ObjectNode.class))).thenReturn(api);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);

        service.createApi(API_ID, USER_ID, SWAGGER_DEFINITION, ENVIRONMENT_ID, DeploymentMode.API_MOCKED, LABELS);

        verify(swaggerService).createAPI(descriptorCaptor.capture(), eq(DefinitionVersion.V2));
        assertThat(descriptorCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedDescriptor);

        verify(apiService).createWithApiDefinition(eq(swaggerApi), eq(USER_ID), apiDefinitionCaptor.capture());
        assertThat(apiDefinitionCaptor.getValue().get("id")).isEqualTo(new JsonNodeFactory(false).textNode(API_ID));

        verify(pageService).createAsideFolder(API_ID, ENVIRONMENT_ID);
        verify(pageService).createOrUpdateSwaggerPage(eq(API_ID), any(ImportSwaggerDescriptorEntity.class), eq(true));
        verify(apiMetadataService).create(same(swaggerApi.getMetadata()), eq(API_ID));
    }

    @Test
    public void should_start_a_mocked_api() {
        SwaggerApiEntity swaggerApi = new SwaggerApiEntity();
        swaggerApi.setMetadata(new ArrayList<>());
        Proxy proxy = new Proxy();
        VirtualHost virtualHost = new VirtualHost();
        proxy.setVirtualHosts(List.of(virtualHost));
        swaggerApi.setProxy(proxy);

        ApiEntity api = new ApiEntity();
        api.setId(API_ID);

        when(swaggerService.createAPI(any(ImportSwaggerDescriptorEntity.class), eq(DefinitionVersion.V2))).thenReturn(swaggerApi);
        when(apiService.createWithApiDefinition(eq(swaggerApi), eq(USER_ID), any(ObjectNode.class))).thenReturn(api);

        service.createApi(API_ID, USER_ID, SWAGGER_DEFINITION, ENVIRONMENT_ID, DeploymentMode.API_MOCKED, LABELS);

        verify(planService).create(newPlanCaptor.capture());
        assertThat(newPlanCaptor.getValue())
            .extracting(NewPlanEntity::getApi, NewPlanEntity::getSecurity, NewPlanEntity::getStatus)
            .containsExactly(API_ID, PlanSecurityType.KEY_LESS, PlanStatus.PUBLISHED);

        verify(apiService).start(API_ID, USER_ID);
    }

    @Test
    public void should_create_a_published_api() {
        ImportSwaggerDescriptorEntity expectedDescriptor = new ImportSwaggerDescriptorEntity();
        expectedDescriptor.setPayload(SWAGGER_DEFINITION);
        expectedDescriptor.setWithDocumentation(true);
        expectedDescriptor.setWithPolicyPaths(true);
        expectedDescriptor.setWithPolicies(List.of("mock"));

        SwaggerApiEntity swaggerApi = new SwaggerApiEntity();
        swaggerApi.setMetadata(new ArrayList<>());
        Proxy proxy = new Proxy();
        VirtualHost virtualHost = new VirtualHost();
        proxy.setVirtualHosts(List.of(virtualHost));
        swaggerApi.setProxy(proxy);

        ApiEntity api = new ApiEntity();
        api.setId(API_ID);

        when(swaggerService.createAPI(any(ImportSwaggerDescriptorEntity.class), eq(DefinitionVersion.V2))).thenReturn(swaggerApi);
        when(apiService.createWithApiDefinition(eq(swaggerApi), eq(USER_ID), any(ObjectNode.class))).thenReturn(api);
        when(apiService.start(API_ID, USER_ID)).thenReturn(api);

        preparePageServiceMock();

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);

        service.createApi(API_ID, USER_ID, SWAGGER_DEFINITION, ENVIRONMENT_ID, DeploymentMode.API_PUBLISHED, LABELS);

        verify(swaggerService).createAPI(descriptorCaptor.capture(), eq(DefinitionVersion.V2));
        assertThat(descriptorCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedDescriptor);

        verify(apiService).createWithApiDefinition(eq(swaggerApi), eq(USER_ID), apiDefinitionCaptor.capture());
        assertThat(apiDefinitionCaptor.getValue().get("id")).isEqualTo(new JsonNodeFactory(false).textNode(API_ID));

        verify(pageService).createAsideFolder(API_ID, ENVIRONMENT_ID);
        verify(pageService).createOrUpdateSwaggerPage(eq(API_ID), any(ImportSwaggerDescriptorEntity.class), eq(true));
        verify(apiMetadataService).create(same(swaggerApi.getMetadata()), eq(API_ID));
    }

    @Test
    public void should_start_an_published_api() {
        SwaggerApiEntity swaggerApi = new SwaggerApiEntity();
        swaggerApi.setMetadata(new ArrayList<>());
        Proxy proxy = new Proxy();
        VirtualHost virtualHost = new VirtualHost();
        proxy.setVirtualHosts(List.of(virtualHost));
        swaggerApi.setProxy(proxy);

        ApiEntity api = new ApiEntity();
        api.setId(API_ID);

        when(swaggerService.createAPI(any(ImportSwaggerDescriptorEntity.class), eq(DefinitionVersion.V2))).thenReturn(swaggerApi);
        when(apiService.createWithApiDefinition(eq(swaggerApi), eq(USER_ID), any(ObjectNode.class))).thenReturn(api);
        when(apiService.start(API_ID, USER_ID)).thenReturn(api);

        preparePageServiceMock();

        service.createApi(API_ID, USER_ID, SWAGGER_DEFINITION, ENVIRONMENT_ID, DeploymentMode.API_PUBLISHED, LABELS);

        verify(planService).create(newPlanCaptor.capture());
        assertThat(newPlanCaptor.getValue())
            .extracting(NewPlanEntity::getApi, NewPlanEntity::getSecurity, NewPlanEntity::getStatus)
            .containsExactly(API_ID, PlanSecurityType.KEY_LESS, PlanStatus.PUBLISHED);

        verify(apiService).start(API_ID, USER_ID);
    }

    @Test
    public void should_publish_an_published_api() {
        SwaggerApiEntity swaggerApi = new SwaggerApiEntity();
        swaggerApi.setMetadata(new ArrayList<>());
        Proxy proxy = new Proxy();
        VirtualHost virtualHost = new VirtualHost();
        proxy.setVirtualHosts(List.of(virtualHost));
        swaggerApi.setProxy(proxy);

        ApiEntity api = new ApiEntity();
        api.setId(API_ID);

        when(swaggerService.createAPI(any(ImportSwaggerDescriptorEntity.class), eq(DefinitionVersion.V2))).thenReturn(swaggerApi);
        when(apiService.createWithApiDefinition(eq(swaggerApi), eq(USER_ID), any(ObjectNode.class))).thenReturn(api);
        when(apiService.start(API_ID, USER_ID)).thenReturn(api);

        preparePageServiceMock();

        service.createApi(API_ID, USER_ID, SWAGGER_DEFINITION, ENVIRONMENT_ID, DeploymentMode.API_PUBLISHED, LABELS);

        verify(apiService).update(eq(API_ID), updateApiCaptor.capture());
        assertThat(updateApiCaptor.getValue())
            .extracting(UpdateApiEntity::getLifecycleState, UpdateApiEntity::getVisibility)
            .containsExactly(ApiLifecycleState.PUBLISHED, Visibility.PUBLIC);
    }

    @Test
    public void should_publish_swagger_documentation_of_an_published_api() {
        SwaggerApiEntity swaggerApi = new SwaggerApiEntity();
        swaggerApi.setMetadata(new ArrayList<>());
        Proxy proxy = new Proxy();
        VirtualHost virtualHost = new VirtualHost();
        proxy.setVirtualHosts(List.of(virtualHost));
        swaggerApi.setProxy(proxy);

        ApiEntity api = new ApiEntity();
        api.setId(API_ID);

        when(swaggerService.createAPI(any(ImportSwaggerDescriptorEntity.class), eq(DefinitionVersion.V2))).thenReturn(swaggerApi);
        when(apiService.createWithApiDefinition(eq(swaggerApi), eq(USER_ID), any(ObjectNode.class))).thenReturn(api);
        when(apiService.start(API_ID, USER_ID)).thenReturn(api);

        preparePageServiceMock();

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);

        service.createApi(API_ID, USER_ID, SWAGGER_DEFINITION, ENVIRONMENT_ID, DeploymentMode.API_PUBLISHED, LABELS);

        verify(pageService).update(eq(PAGE_ID), updatePageCaptor.capture());
        assertThat(updatePageCaptor.getValue()).extracting(UpdatePageEntity::isPublished).isEqualTo(true);
    }

    @Test
    public void should_update_documented_api() {
        ImportSwaggerDescriptorEntity expectedDescriptor = new ImportSwaggerDescriptorEntity();
        expectedDescriptor.setPayload(SWAGGER_DEFINITION);
        expectedDescriptor.setWithDocumentation(true);
        expectedDescriptor.setWithPolicyPaths(true);

        SwaggerApiEntity swaggerApi = new SwaggerApiEntity();
        swaggerApi.setMetadata(new ArrayList<>());
        Proxy proxy = new Proxy();
        VirtualHost virtualHost = new VirtualHost();
        proxy.setVirtualHosts(List.of(virtualHost));
        swaggerApi.setProxy(proxy);

        ApiEntity api = new ApiEntity();
        api.setId(API_ID);

        when(swaggerService.createAPI(any(ImportSwaggerDescriptorEntity.class), eq(DefinitionVersion.V2))).thenReturn(swaggerApi);

        ApiEntity updatedApiEntity = new ApiEntity();
        updatedApiEntity.setName("updated api");
        updatedApiEntity.setProxy(proxy);
        when(apiService.updateFromSwagger(eq(API_ID), eq(swaggerApi), any(ImportSwaggerDescriptorEntity.class)))
            .thenReturn(updatedApiEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);

        final var result = service.updateApi(API_ID, USER_ID, SWAGGER_DEFINITION, ENVIRONMENT_ID, DeploymentMode.API_DOCUMENTED, LABELS);

        verify(swaggerService).createAPI(descriptorCaptor.capture(), eq(DefinitionVersion.V2));
        assertThat(descriptorCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedDescriptor);

        verify(apiService, times(0)).createWithApiDefinition(any(UpdateApiEntity.class), anyString(), any(ObjectNode.class));
        verify(apiService, times(1)).updateFromSwagger(eq(API_ID), eq(swaggerApi), any(ImportSwaggerDescriptorEntity.class));
        assertThat(result.getApi()).isEqualTo(updatedApiEntity);
    }

    @Test
    public void should_update_a_mocked_api() {
        ImportSwaggerDescriptorEntity expectedDescriptor = new ImportSwaggerDescriptorEntity();
        expectedDescriptor.setPayload(SWAGGER_DEFINITION);
        expectedDescriptor.setWithDocumentation(true);
        expectedDescriptor.setWithPolicyPaths(true);
        expectedDescriptor.setWithPolicies(List.of("mock"));

        Proxy proxy = new Proxy();
        VirtualHost virtualHost = new VirtualHost();
        proxy.setVirtualHosts(List.of(virtualHost));

        SwaggerApiEntity swaggerApi = new SwaggerApiEntity();
        swaggerApi.setMetadata(new ArrayList<>());
        swaggerApi.setProxy(proxy);

        ApiEntity api = new ApiEntity();
        api.setId(API_ID);
        api.setProxy(proxy);

        when(swaggerService.createAPI(any(ImportSwaggerDescriptorEntity.class), eq(DefinitionVersion.V2))).thenReturn(swaggerApi);
        when(virtualHostService.sanitizeAndValidate(any(), eq(API_ID))).thenReturn(List.of(virtualHost));

        ApiEntity updatedApiEntity = new ApiEntity();
        updatedApiEntity.setName("updated api");
        updatedApiEntity.setProxy(proxy);

        when(apiService.updateFromSwagger(eq(API_ID), eq(swaggerApi), any(ImportSwaggerDescriptorEntity.class)))
            .thenReturn(updatedApiEntity);
        when(apiService.deploy(anyString(), anyString(), any(EventType.class), any(ApiDeploymentEntity.class)))
            .thenReturn(updatedApiEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);

        final var result = service.updateApi(API_ID, USER_ID, SWAGGER_DEFINITION, ENVIRONMENT_ID, DeploymentMode.API_MOCKED, LABELS);
        assertThat(result.getApi()).isEqualTo(updatedApiEntity);

        verify(swaggerService).createAPI(descriptorCaptor.capture(), eq(DefinitionVersion.V2));
        assertThat(descriptorCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedDescriptor);

        verify(apiService, never()).createWithApiDefinition(any(UpdateApiEntity.class), anyString(), any(ObjectNode.class));
    }

    @Test
    public void should_deploy_an_updated_mocked_api() {
        SwaggerApiEntity swaggerApi = new SwaggerApiEntity();
        swaggerApi.setMetadata(new ArrayList<>());

        ApiEntity api = new ApiEntity();
        api.setId(API_ID);
        Proxy proxy = new Proxy();
        VirtualHost virtualHost = new VirtualHost();
        proxy.setVirtualHosts(List.of(virtualHost));
        api.setProxy(proxy);
        swaggerApi.setProxy(proxy);

        when(swaggerService.createAPI(any(ImportSwaggerDescriptorEntity.class), eq(DefinitionVersion.V2))).thenReturn(swaggerApi);

        ApiEntity updatedApiEntity = new ApiEntity();
        updatedApiEntity.setName("updated api");
        when(apiService.updateFromSwagger(eq(API_ID), eq(swaggerApi), any(ImportSwaggerDescriptorEntity.class)))
            .thenReturn(updatedApiEntity);

        service.updateApi(API_ID, USER_ID, SWAGGER_DEFINITION, ENVIRONMENT_ID, DeploymentMode.API_MOCKED, LABELS);

        verify(apiService).deploy(eq(API_ID), eq(USER_ID), eq(EventType.PUBLISH_API), apiDeploymentCaptor.capture());
        assertThat(apiDeploymentCaptor.getValue().getDeploymentLabel()).isEqualTo("Model updated");
    }

    @Test
    public void should_update_a_published_api() {
        ImportSwaggerDescriptorEntity expectedDescriptor = new ImportSwaggerDescriptorEntity();
        expectedDescriptor.setPayload(SWAGGER_DEFINITION);
        expectedDescriptor.setWithDocumentation(true);
        expectedDescriptor.setWithPolicyPaths(true);
        expectedDescriptor.setWithPolicies(List.of("mock"));

        Proxy proxy = new Proxy();
        VirtualHost virtualHost = new VirtualHost();
        proxy.setVirtualHosts(List.of(virtualHost));

        SwaggerApiEntity swaggerApi = new SwaggerApiEntity();
        swaggerApi.setMetadata(new ArrayList<>());
        swaggerApi.setProxy(proxy);

        ApiEntity api = new ApiEntity();
        api.setId(API_ID);
        api.setProxy(proxy);

        when(swaggerService.createAPI(any(ImportSwaggerDescriptorEntity.class), eq(DefinitionVersion.V2))).thenReturn(swaggerApi);

        ApiEntity updatedApiEntity = new ApiEntity();
        updatedApiEntity.setName("updated api");
        updatedApiEntity.setProxy(proxy);
        when(apiService.updateFromSwagger(eq(API_ID), eq(swaggerApi), any(ImportSwaggerDescriptorEntity.class)))
            .thenReturn(updatedApiEntity);
        when(apiService.deploy(anyString(), anyString(), any(EventType.class), any(ApiDeploymentEntity.class)))
            .thenReturn(updatedApiEntity);
        when(apiService.update(eq(API_ID), any(UpdateApiEntity.class))).thenReturn(updatedApiEntity);

        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);

        final var result = service.updateApi(API_ID, USER_ID, SWAGGER_DEFINITION, ENVIRONMENT_ID, DeploymentMode.API_PUBLISHED, LABELS);
        assertThat(result.getApi()).isEqualTo(updatedApiEntity);

        verify(swaggerService).createAPI(descriptorCaptor.capture(), eq(DefinitionVersion.V2));
        assertThat(descriptorCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedDescriptor);

        verify(apiService, never()).createWithApiDefinition(any(UpdateApiEntity.class), anyString(), any(ObjectNode.class));
    }

    @Test
    public void should_deploy_an_updated_published_api() {
        Proxy proxy = new Proxy();
        VirtualHost virtualHost = new VirtualHost();
        proxy.setVirtualHosts(List.of(virtualHost));

        SwaggerApiEntity swaggerApi = new SwaggerApiEntity();
        swaggerApi.setMetadata(new ArrayList<>());
        swaggerApi.setProxy(proxy);

        ApiEntity api = new ApiEntity();
        api.setId(API_ID);
        api.setProxy(proxy);

        when(swaggerService.createAPI(any(ImportSwaggerDescriptorEntity.class), eq(DefinitionVersion.V2))).thenReturn(swaggerApi);
        when(virtualHostService.sanitizeAndValidate(any(), eq(API_ID))).thenReturn(List.of(virtualHost));

        ApiEntity updatedApiEntity = new ApiEntity();
        updatedApiEntity.setName("updated api");
        when(apiService.updateFromSwagger(eq(API_ID), eq(swaggerApi), any(ImportSwaggerDescriptorEntity.class)))
            .thenReturn(updatedApiEntity);
        when(apiService.deploy(eq(API_ID), eq(USER_ID), eq(EventType.PUBLISH_API), any(ApiDeploymentEntity.class)))
            .thenReturn(updatedApiEntity);

        service.updateApi(API_ID, USER_ID, SWAGGER_DEFINITION, ENVIRONMENT_ID, DeploymentMode.API_PUBLISHED, LABELS);

        verify(apiService).deploy(eq(API_ID), eq(USER_ID), eq(EventType.PUBLISH_API), apiDeploymentCaptor.capture());
        assertThat(apiDeploymentCaptor.getValue().getDeploymentLabel()).isEqualTo("Model updated");
    }

    @Test
    public void should_upgrade_documented_to_mocked_api() {
        SwaggerApiEntity swaggerApi = new SwaggerApiEntity();
        swaggerApi.setMetadata(new ArrayList<>());
        ApiEntity api = new ApiEntity();
        api.setId(API_ID);
        Proxy proxy = new Proxy();
        VirtualHost virtualHost = new VirtualHost();
        proxy.setVirtualHosts(List.of(virtualHost));
        api.setProxy(proxy);
        swaggerApi.setProxy(proxy);
        when(swaggerService.createAPI(any(ImportSwaggerDescriptorEntity.class), eq(DefinitionVersion.V2))).thenReturn(swaggerApi);

        ApiEntity updatedApiEntity = new ApiEntity();
        updatedApiEntity.setName("updated api");
        updatedApiEntity.setState(Lifecycle.State.STOPPED);
        when(apiService.updateFromSwagger(eq(API_ID), eq(swaggerApi), any(ImportSwaggerDescriptorEntity.class)))
            .thenReturn(updatedApiEntity);
        when(apiService.deploy(eq(API_ID), eq(USER_ID), eq(EventType.PUBLISH_API), any(ApiDeploymentEntity.class)))
            .thenReturn(updatedApiEntity);

        when(apiService.start(API_ID, USER_ID)).thenReturn(updatedApiEntity);
        when(planService.findByApi(API_ID)).thenReturn(null);
        when(virtualHostService.sanitizeAndValidate(any(), eq(API_ID))).thenReturn(List.of(virtualHost));

        service.updateApi(API_ID, USER_ID, SWAGGER_DEFINITION, ENVIRONMENT_ID, DeploymentMode.API_PUBLISHED, LABELS);

        verify(planService).findByApi(eq(API_ID));
        verify(planService).create(newPlanCaptor.capture());
        assertThat(newPlanCaptor.getValue())
            .extracting(NewPlanEntity::getApi, NewPlanEntity::getSecurity, NewPlanEntity::getStatus)
            .containsExactly(API_ID, PlanSecurityType.KEY_LESS, PlanStatus.PUBLISHED);
        verify(apiService).start(eq(API_ID), eq(USER_ID));
    }

    @Test
    public void should_upgrade_documented_to_published_api() {
        SwaggerApiEntity swaggerApi = new SwaggerApiEntity();
        swaggerApi.setMetadata(new ArrayList<>());

        ApiEntity api = new ApiEntity();
        api.setId(API_ID);
        Proxy proxy = new Proxy();
        VirtualHost virtualHost = new VirtualHost();
        proxy.setVirtualHosts(List.of(virtualHost));
        api.setProxy(proxy);
        swaggerApi.setProxy(proxy);
        when(swaggerService.createAPI(any(ImportSwaggerDescriptorEntity.class), eq(DefinitionVersion.V2))).thenReturn(swaggerApi);

        ApiEntity updatedApiEntity = new ApiEntity();
        updatedApiEntity.setName("updated api");
        updatedApiEntity.setState(Lifecycle.State.STOPPED);
        when(apiService.updateFromSwagger(eq(API_ID), eq(swaggerApi), any(ImportSwaggerDescriptorEntity.class)))
            .thenReturn(updatedApiEntity);
        when(apiService.deploy(eq(API_ID), eq(USER_ID), eq(EventType.PUBLISH_API), any(ApiDeploymentEntity.class)))
            .thenReturn(updatedApiEntity);
        when(apiService.start(API_ID, USER_ID)).thenReturn(updatedApiEntity);

        when(planService.findByApi(API_ID)).thenReturn(null);
        when(virtualHostService.sanitizeAndValidate(any(), eq(API_ID))).thenReturn(List.of(virtualHost));

        preparePageServiceMock();
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);

        service.updateApi(API_ID, USER_ID, SWAGGER_DEFINITION, ENVIRONMENT_ID, DeploymentMode.API_PUBLISHED, LABELS);

        verify(planService).findByApi(eq(API_ID));
        verify(planService).create(newPlanCaptor.capture());
        assertThat(newPlanCaptor.getValue())
            .extracting(NewPlanEntity::getApi, NewPlanEntity::getSecurity, NewPlanEntity::getStatus)
            .containsExactly(API_ID, PlanSecurityType.KEY_LESS, PlanStatus.PUBLISHED);

        verify(apiService).start(eq(API_ID), eq(USER_ID));
        verify(apiService).update(eq(API_ID), updateApiCaptor.capture());
        assertThat(updateApiCaptor.getValue())
            .extracting(UpdateApiEntity::getLifecycleState, UpdateApiEntity::getVisibility)
            .containsExactly(ApiLifecycleState.PUBLISHED, Visibility.PUBLIC);

        verify(pageService).update(eq(PAGE_ID), updatePageCaptor.capture());
        assertThat(updatePageCaptor.getValue()).extracting(UpdatePageEntity::isPublished).isEqualTo(true);
    }

    @Test
    public void should_upgrade_mocked_to_published_api() {
        SwaggerApiEntity swaggerApi = new SwaggerApiEntity();
        swaggerApi.setMetadata(new ArrayList<>());
        ApiEntity api = new ApiEntity();
        api.setId(API_ID);
        Proxy proxy = new Proxy();
        VirtualHost virtualHost = new VirtualHost();
        proxy.setVirtualHosts(List.of(virtualHost));
        api.setProxy(proxy);
        swaggerApi.setProxy(proxy);
        when(swaggerService.createAPI(any(ImportSwaggerDescriptorEntity.class), eq(DefinitionVersion.V2))).thenReturn(swaggerApi);

        ApiEntity updatedApiEntity = new ApiEntity();
        updatedApiEntity.setName("updated api");
        updatedApiEntity.setState(Lifecycle.State.STARTED);
        when(apiService.deploy(eq(API_ID), eq(USER_ID), eq(EventType.PUBLISH_API), any(ApiDeploymentEntity.class)))
            .thenReturn(updatedApiEntity);

        when(planService.findByApi(API_ID)).thenReturn(Collections.singleton(new PlanEntity()));
        when(virtualHostService.sanitizeAndValidate(any(), eq(API_ID))).thenReturn(List.of(virtualHost));

        preparePageServiceMock();
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);

        service.updateApi(API_ID, USER_ID, SWAGGER_DEFINITION, ENVIRONMENT_ID, DeploymentMode.API_PUBLISHED, LABELS);

        verify(planService).findByApi(eq(API_ID));
        verify(planService, never()).create(any(NewPlanEntity.class));

        verify(apiService, never()).start(eq(API_ID), eq(USER_ID));
        verify(apiService).update(eq(API_ID), updateApiCaptor.capture());
        assertThat(updateApiCaptor.getValue())
            .extracting(UpdateApiEntity::getLifecycleState, UpdateApiEntity::getVisibility)
            .containsExactly(ApiLifecycleState.PUBLISHED, Visibility.PUBLIC);

        verify(pageService).update(eq(PAGE_ID), updatePageCaptor.capture());
        assertThat(updatePageCaptor.getValue()).extracting(UpdatePageEntity::isPublished).isEqualTo(true);
    }

    @Test
    public void should_check_context_path_unique() {
        SwaggerApiEntity api = new SwaggerApiEntity();
        Proxy proxy = new Proxy();
        VirtualHost virtualHost = new VirtualHost();
        proxy.setVirtualHosts(List.of(virtualHost));
        api.setProxy(proxy);

        when(virtualHostService.sanitizeAndValidate(any(Collection.class))).thenReturn(List.of(virtualHost));
        var message = service.checkContextPath(api);

        verify(virtualHostService).sanitizeAndValidate(eq(List.of(virtualHost)));
        assertThat(message.isPresent()).isFalse();
    }

    @Test
    public void should_check_context_path_not_unique() {
        SwaggerApiEntity api = new SwaggerApiEntity();
        Proxy proxy = new Proxy();
        VirtualHost virtualHost = new VirtualHost();
        proxy.setVirtualHosts(List.of(virtualHost));
        api.setProxy(proxy);

        when(virtualHostService.sanitizeAndValidate(any(Collection.class)))
            .thenThrow(new ApiContextPathAlreadyExistsException("contextPath"));
        var message = service.checkContextPath(api);

        verify(virtualHostService).sanitizeAndValidate(eq(List.of(virtualHost)));
        assertThat(message.isPresent()).isTrue();
        assertThat(message.get()).isEqualTo("The path [contextPath] is already covered by an other API.");
    }

    private void preparePageServiceMock() {
        PageEntity page = new PageEntity();
        page.setId(PAGE_ID);
        page.setType(PageType.SWAGGER.name());

        when(
            pageService.search(
                argThat((PageQuery query) -> query.getApi().equals(API_ID) && query.getType().equals(PageType.SWAGGER)),
                eq(ENVIRONMENT_ID)
            )
        )
            .thenReturn(List.of(page));
    }
}
