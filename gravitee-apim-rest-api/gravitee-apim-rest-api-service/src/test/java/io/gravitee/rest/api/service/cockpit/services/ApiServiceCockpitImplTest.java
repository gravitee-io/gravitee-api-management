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
package io.gravitee.rest.api.service.cockpit.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity;
import io.gravitee.rest.api.model.NewPlanEntity;
import io.gravitee.rest.api.model.PageEntity;
import io.gravitee.rest.api.model.PageType;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PlanSecurityType;
import io.gravitee.rest.api.model.PlanStatus;
import io.gravitee.rest.api.model.UpdatePageEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiDeploymentEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiLifecycleState;
import io.gravitee.rest.api.model.api.SwaggerApiEntity;
import io.gravitee.rest.api.model.api.UpdateApiEntity;
import io.gravitee.rest.api.model.documentation.PageQuery;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.ApiService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.SwaggerService;
import io.gravitee.rest.api.service.VirtualHostService;
import io.gravitee.rest.api.service.WorkflowService;
import io.gravitee.rest.api.service.cockpit.model.DeploymentMode;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.CategoryMapper;
import io.gravitee.rest.api.service.converter.PageConverter;
import io.gravitee.rest.api.service.exceptions.ApiContextPathAlreadyExistsException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiServiceCockpitImplTest {

    private static final String API_ID = "api#id";
    private static final String API_CROSS_ID = "api#crossId";
    private static final List<String> LABELS = List.of("label1", "label2");
    private static final String USER_ID = "user#id";
    private static final String ENVIRONMENT_ID = "environment#id";
    private static final String PAGE_ID = "page#id";
    private static final String SWAGGER_DEFINITION = "";
    private final PageConverter pageConverter = new PageConverter();

    @Mock
    private ApiConverter apiConverter;

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

    @Before
    public void setUp() throws Exception {
        apiConverter =
            new ApiConverter(
                new ObjectMapper(),
                mock(PlanService.class),
                mock(FlowService.class),
                mock(CategoryMapper.class),
                mock(ParameterService.class),
                mock(WorkflowService.class)
            );
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
        ExecutionContext executionContext = new ExecutionContext(GraviteeContext.getCurrentOrganization(), ENVIRONMENT_ID);

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
        api.setCrossId(API_CROSS_ID);
        api.setId(API_ID);
        api.setLabels(LABELS);

        when(swaggerService.createAPI(eq(executionContext), any(ImportSwaggerDescriptorEntity.class), eq(DefinitionVersion.V2)))
            .thenReturn(swaggerApi);
        when(apiService.createWithApiDefinition(eq(executionContext), eq(swaggerApi), eq(USER_ID), any(ObjectNode.class))).thenReturn(api);

        service.createApi(
            executionContext,
            API_CROSS_ID,
            USER_ID,
            SWAGGER_DEFINITION,
            ENVIRONMENT_ID,
            DeploymentMode.API_DOCUMENTED,
            LABELS
        );

        verify(swaggerService).createAPI(eq(executionContext), descriptorCaptor.capture(), eq(DefinitionVersion.V2));
        assertThat(descriptorCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedDescriptor);

        verify(apiService).createWithApiDefinition(eq(executionContext), eq(swaggerApi), eq(USER_ID), apiDefinitionCaptor.capture());
        assertThat(apiDefinitionCaptor.getValue().get("crossId")).isEqualTo(new JsonNodeFactory(false).textNode(API_CROSS_ID));

        verify(pageService).createAsideFolder(executionContext, API_ID);
        verify(pageService).createOrUpdateSwaggerPage(eq(executionContext), eq(API_ID), any(ImportSwaggerDescriptorEntity.class), eq(true));
        verify(apiMetadataService).create(eq(executionContext), same(swaggerApi.getMetadata()), eq(API_ID));
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

        when(
            swaggerService.createAPI(
                eq(GraviteeContext.getExecutionContext()),
                any(ImportSwaggerDescriptorEntity.class),
                eq(DefinitionVersion.V2)
            )
        )
            .thenReturn(swaggerApi);
        when(
            apiService.createWithApiDefinition(
                eq(GraviteeContext.getExecutionContext()),
                eq(swaggerApi),
                eq(USER_ID),
                any(ObjectNode.class)
            )
        )
            .thenReturn(api);

        service.createApi(
            GraviteeContext.getExecutionContext(),
            API_ID,
            USER_ID,
            SWAGGER_DEFINITION,
            ENVIRONMENT_ID,
            DeploymentMode.API_DOCUMENTED,
            LABELS
        );

        verifyNoInteractions(planService);
        verify(apiService, never()).start(eq(GraviteeContext.getExecutionContext()), anyString(), anyString());
    }

    @Test
    public void should_create_a_mocked_api() {
        ExecutionContext executionContext = new ExecutionContext(GraviteeContext.getCurrentOrganization(), ENVIRONMENT_ID);
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

        when(swaggerService.createAPI(eq(executionContext), any(ImportSwaggerDescriptorEntity.class), eq(DefinitionVersion.V2)))
            .thenReturn(swaggerApi);
        when(apiService.createWithApiDefinition(eq(executionContext), eq(swaggerApi), eq(USER_ID), any(ObjectNode.class))).thenReturn(api);

        service.createApi(executionContext, API_CROSS_ID, USER_ID, SWAGGER_DEFINITION, ENVIRONMENT_ID, DeploymentMode.API_MOCKED, LABELS);

        verify(swaggerService).createAPI(eq(executionContext), descriptorCaptor.capture(), eq(DefinitionVersion.V2));
        assertThat(descriptorCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedDescriptor);

        verify(apiService).createWithApiDefinition(eq(executionContext), eq(swaggerApi), eq(USER_ID), apiDefinitionCaptor.capture());
        assertThat(apiDefinitionCaptor.getValue().get("crossId")).isEqualTo(new JsonNodeFactory(false).textNode(API_CROSS_ID));

        verify(pageService).createAsideFolder(executionContext, API_ID);
        verify(pageService).createOrUpdateSwaggerPage(eq(executionContext), eq(API_ID), any(ImportSwaggerDescriptorEntity.class), eq(true));
        verify(apiMetadataService).create(eq(executionContext), same(swaggerApi.getMetadata()), eq(API_ID));
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

        when(
            swaggerService.createAPI(
                eq(GraviteeContext.getExecutionContext()),
                any(ImportSwaggerDescriptorEntity.class),
                eq(DefinitionVersion.V2)
            )
        )
            .thenReturn(swaggerApi);
        when(
            apiService.createWithApiDefinition(
                eq(GraviteeContext.getExecutionContext()),
                eq(swaggerApi),
                eq(USER_ID),
                any(ObjectNode.class)
            )
        )
            .thenReturn(api);

        service.createApi(
            GraviteeContext.getExecutionContext(),
            API_ID,
            USER_ID,
            SWAGGER_DEFINITION,
            ENVIRONMENT_ID,
            DeploymentMode.API_MOCKED,
            LABELS
        );

        verify(planService).create(eq(GraviteeContext.getExecutionContext()), newPlanCaptor.capture());
        assertThat(newPlanCaptor.getValue())
            .extracting(NewPlanEntity::getApi, NewPlanEntity::getSecurity, NewPlanEntity::getStatus)
            .containsExactly(API_ID, PlanSecurityType.KEY_LESS, PlanStatus.PUBLISHED);

        verify(apiService).start(GraviteeContext.getExecutionContext(), API_ID, USER_ID);
    }

    @Test
    public void should_create_a_published_api() {
        ExecutionContext executionContext = new ExecutionContext(GraviteeContext.getCurrentOrganization(), ENVIRONMENT_ID);
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
        api.setCrossId(API_CROSS_ID);

        when(swaggerService.createAPI(eq(executionContext), any(ImportSwaggerDescriptorEntity.class), eq(DefinitionVersion.V2)))
            .thenReturn(swaggerApi);
        when(apiService.createWithApiDefinition(eq(executionContext), eq(swaggerApi), eq(USER_ID), any(ObjectNode.class))).thenReturn(api);
        when(apiService.start(executionContext, API_ID, USER_ID)).thenReturn(api);

        preparePageServiceMock();

        service.createApi(
            executionContext,
            API_CROSS_ID,
            USER_ID,
            SWAGGER_DEFINITION,
            ENVIRONMENT_ID,
            DeploymentMode.API_PUBLISHED,
            LABELS
        );

        verify(swaggerService).createAPI(eq(executionContext), descriptorCaptor.capture(), eq(DefinitionVersion.V2));
        assertThat(descriptorCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedDescriptor);

        verify(apiService).createWithApiDefinition(eq(executionContext), eq(swaggerApi), eq(USER_ID), apiDefinitionCaptor.capture());
        assertThat(apiDefinitionCaptor.getValue().get("crossId")).isEqualTo(new JsonNodeFactory(false).textNode(API_CROSS_ID));

        verify(pageService).createAsideFolder(executionContext, API_ID);
        verify(pageService).createOrUpdateSwaggerPage(eq(executionContext), eq(API_ID), any(ImportSwaggerDescriptorEntity.class), eq(true));
        verify(apiMetadataService).create(eq(executionContext), same(swaggerApi.getMetadata()), eq(API_ID));
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

        when(
            swaggerService.createAPI(
                eq(GraviteeContext.getExecutionContext()),
                any(ImportSwaggerDescriptorEntity.class),
                eq(DefinitionVersion.V2)
            )
        )
            .thenReturn(swaggerApi);
        when(
            apiService.createWithApiDefinition(
                eq(GraviteeContext.getExecutionContext()),
                eq(swaggerApi),
                eq(USER_ID),
                any(ObjectNode.class)
            )
        )
            .thenReturn(api);
        when(apiService.start(GraviteeContext.getExecutionContext(), API_ID, USER_ID)).thenReturn(api);

        preparePageServiceMock();

        service.createApi(
            GraviteeContext.getExecutionContext(),
            API_ID,
            USER_ID,
            SWAGGER_DEFINITION,
            ENVIRONMENT_ID,
            DeploymentMode.API_PUBLISHED,
            LABELS
        );

        verify(planService).create(eq(GraviteeContext.getExecutionContext()), newPlanCaptor.capture());
        assertThat(newPlanCaptor.getValue())
            .extracting(NewPlanEntity::getApi, NewPlanEntity::getSecurity, NewPlanEntity::getStatus)
            .containsExactly(API_ID, PlanSecurityType.KEY_LESS, PlanStatus.PUBLISHED);

        verify(apiService).start(GraviteeContext.getExecutionContext(), API_ID, USER_ID);
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

        when(
            swaggerService.createAPI(
                eq(GraviteeContext.getExecutionContext()),
                any(ImportSwaggerDescriptorEntity.class),
                eq(DefinitionVersion.V2)
            )
        )
            .thenReturn(swaggerApi);
        when(
            apiService.createWithApiDefinition(
                eq(GraviteeContext.getExecutionContext()),
                eq(swaggerApi),
                eq(USER_ID),
                any(ObjectNode.class)
            )
        )
            .thenReturn(api);
        when(apiService.start(GraviteeContext.getExecutionContext(), API_ID, USER_ID)).thenReturn(api);

        preparePageServiceMock();

        service.createApi(
            GraviteeContext.getExecutionContext(),
            API_ID,
            USER_ID,
            SWAGGER_DEFINITION,
            ENVIRONMENT_ID,
            DeploymentMode.API_PUBLISHED,
            LABELS
        );

        verify(apiService).update(eq(GraviteeContext.getExecutionContext()), eq(API_ID), updateApiCaptor.capture());
        assertThat(updateApiCaptor.getValue())
            .extracting(UpdateApiEntity::getLifecycleState, UpdateApiEntity::getVisibility)
            .containsExactly(ApiLifecycleState.PUBLISHED, Visibility.PUBLIC);
    }

    @Test
    public void should_publish_swagger_documentation_of_an_published_api() {
        ExecutionContext executionContext = new ExecutionContext(GraviteeContext.getCurrentOrganization(), ENVIRONMENT_ID);
        SwaggerApiEntity swaggerApi = new SwaggerApiEntity();
        swaggerApi.setMetadata(new ArrayList<>());
        Proxy proxy = new Proxy();
        VirtualHost virtualHost = new VirtualHost();
        proxy.setVirtualHosts(List.of(virtualHost));
        swaggerApi.setProxy(proxy);

        ApiEntity api = new ApiEntity();
        api.setId(API_ID);

        when(swaggerService.createAPI(eq(executionContext), any(ImportSwaggerDescriptorEntity.class), eq(DefinitionVersion.V2)))
            .thenReturn(swaggerApi);
        when(apiService.createWithApiDefinition(eq(executionContext), eq(swaggerApi), eq(USER_ID), any(ObjectNode.class))).thenReturn(api);
        when(apiService.start(executionContext, API_ID, USER_ID)).thenReturn(api);

        preparePageServiceMock();

        service.createApi(executionContext, API_ID, USER_ID, SWAGGER_DEFINITION, ENVIRONMENT_ID, DeploymentMode.API_PUBLISHED, LABELS);

        verify(pageService).update(eq(executionContext), eq(PAGE_ID), updatePageCaptor.capture());
        assertThat(updatePageCaptor.getValue()).extracting(UpdatePageEntity::isPublished).isEqualTo(true);
    }

    @Test
    public void should_update_documented_api() {
        ExecutionContext executionContext = new ExecutionContext(GraviteeContext.getCurrentOrganization(), ENVIRONMENT_ID);
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

        when(swaggerService.createAPI(eq(executionContext), any(ImportSwaggerDescriptorEntity.class), eq(DefinitionVersion.V2)))
            .thenReturn(swaggerApi);

        ApiEntity updatedApiEntity = new ApiEntity();
        updatedApiEntity.setName("updated api");
        updatedApiEntity.setProxy(proxy);
        when(apiService.updateFromSwagger(eq(executionContext), eq(API_ID), eq(swaggerApi), any(ImportSwaggerDescriptorEntity.class)))
            .thenReturn(updatedApiEntity);

        final var result = service.updateApi(
            executionContext,
            API_ID,
            USER_ID,
            SWAGGER_DEFINITION,
            ENVIRONMENT_ID,
            DeploymentMode.API_DOCUMENTED,
            LABELS
        );

        verify(swaggerService).createAPI(eq(executionContext), descriptorCaptor.capture(), eq(DefinitionVersion.V2));
        assertThat(descriptorCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedDescriptor);

        verify(apiService, times(0))
            .createWithApiDefinition(eq(executionContext), any(UpdateApiEntity.class), anyString(), any(ObjectNode.class));
        verify(apiService, times(1))
            .updateFromSwagger(eq(executionContext), eq(API_ID), eq(swaggerApi), any(ImportSwaggerDescriptorEntity.class));
        assertThat(result.getApi()).isEqualTo(updatedApiEntity);
    }

    @Test
    public void should_update_a_mocked_api() {
        ExecutionContext executionContext = new ExecutionContext(GraviteeContext.getCurrentOrganization(), ENVIRONMENT_ID);
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

        when(swaggerService.createAPI(eq(executionContext), any(ImportSwaggerDescriptorEntity.class), eq(DefinitionVersion.V2)))
            .thenReturn(swaggerApi);
        when(virtualHostService.sanitizeAndValidate(any(), any(), eq(API_ID))).thenReturn(List.of(virtualHost));

        ApiEntity updatedApiEntity = new ApiEntity();
        updatedApiEntity.setName("updated api");
        updatedApiEntity.setProxy(proxy);

        when(apiService.updateFromSwagger(eq(executionContext), eq(API_ID), eq(swaggerApi), any(ImportSwaggerDescriptorEntity.class)))
            .thenReturn(updatedApiEntity);
        when(apiService.deploy(eq(executionContext), anyString(), anyString(), any(EventType.class), any(ApiDeploymentEntity.class)))
            .thenReturn(updatedApiEntity);

        final var result = service.updateApi(
            executionContext,
            API_ID,
            USER_ID,
            SWAGGER_DEFINITION,
            ENVIRONMENT_ID,
            DeploymentMode.API_MOCKED,
            LABELS
        );
        assertThat(result.getApi()).isEqualTo(updatedApiEntity);

        verify(swaggerService).createAPI(eq(executionContext), descriptorCaptor.capture(), eq(DefinitionVersion.V2));
        assertThat(descriptorCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedDescriptor);

        verify(apiService, never())
            .createWithApiDefinition(eq(executionContext), any(UpdateApiEntity.class), anyString(), any(ObjectNode.class));
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

        when(
            swaggerService.createAPI(
                eq(GraviteeContext.getExecutionContext()),
                any(ImportSwaggerDescriptorEntity.class),
                eq(DefinitionVersion.V2)
            )
        )
            .thenReturn(swaggerApi);

        ApiEntity updatedApiEntity = new ApiEntity();
        updatedApiEntity.setName("updated api");
        when(
            apiService.updateFromSwagger(
                eq(GraviteeContext.getExecutionContext()),
                eq(API_ID),
                eq(swaggerApi),
                any(ImportSwaggerDescriptorEntity.class)
            )
        )
            .thenReturn(updatedApiEntity);

        service.updateApi(
            GraviteeContext.getExecutionContext(),
            API_ID,
            USER_ID,
            SWAGGER_DEFINITION,
            ENVIRONMENT_ID,
            DeploymentMode.API_MOCKED,
            LABELS
        );

        verify(apiService)
            .deploy(
                eq(GraviteeContext.getExecutionContext()),
                eq(API_ID),
                eq(USER_ID),
                eq(EventType.PUBLISH_API),
                apiDeploymentCaptor.capture()
            );
        assertThat(apiDeploymentCaptor.getValue().getDeploymentLabel()).isEqualTo("Model updated");
    }

    @Test
    public void should_update_a_published_api() {
        ExecutionContext executionContext = new ExecutionContext(GraviteeContext.getCurrentOrganization(), ENVIRONMENT_ID);
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

        when(swaggerService.createAPI(eq(executionContext), any(ImportSwaggerDescriptorEntity.class), eq(DefinitionVersion.V2)))
            .thenReturn(swaggerApi);

        ApiEntity updatedApiEntity = new ApiEntity();
        updatedApiEntity.setName("updated api");
        updatedApiEntity.setProxy(proxy);
        when(apiService.updateFromSwagger(eq(executionContext), eq(API_ID), eq(swaggerApi), any(ImportSwaggerDescriptorEntity.class)))
            .thenReturn(updatedApiEntity);
        when(apiService.deploy(eq(executionContext), anyString(), anyString(), any(EventType.class), any(ApiDeploymentEntity.class)))
            .thenReturn(updatedApiEntity);
        when(apiService.update(eq(executionContext), eq(API_ID), any(UpdateApiEntity.class))).thenReturn(updatedApiEntity);

        final var result = service.updateApi(
            executionContext,
            API_ID,
            USER_ID,
            SWAGGER_DEFINITION,
            ENVIRONMENT_ID,
            DeploymentMode.API_PUBLISHED,
            LABELS
        );
        assertThat(result.getApi()).isEqualTo(updatedApiEntity);

        verify(swaggerService).createAPI(eq(executionContext), descriptorCaptor.capture(), eq(DefinitionVersion.V2));
        assertThat(descriptorCaptor.getValue()).usingRecursiveComparison().isEqualTo(expectedDescriptor);

        verify(apiService, never())
            .createWithApiDefinition(eq(executionContext), any(UpdateApiEntity.class), anyString(), any(ObjectNode.class));
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

        ExecutionContext executionContext = new ExecutionContext(GraviteeContext.getCurrentOrganization(), ENVIRONMENT_ID);

        when(swaggerService.createAPI(eq(executionContext), any(ImportSwaggerDescriptorEntity.class), eq(DefinitionVersion.V2)))
            .thenReturn(swaggerApi);
        when(virtualHostService.sanitizeAndValidate(any(), any(), eq(API_ID))).thenReturn(List.of(virtualHost));

        ApiEntity updatedApiEntity = new ApiEntity();
        updatedApiEntity.setName("updated api");
        when(apiService.updateFromSwagger(eq(executionContext), eq(API_ID), eq(swaggerApi), any(ImportSwaggerDescriptorEntity.class)))
            .thenReturn(updatedApiEntity);
        when(apiService.deploy(eq(executionContext), eq(API_ID), eq(USER_ID), eq(EventType.PUBLISH_API), any(ApiDeploymentEntity.class)))
            .thenReturn(updatedApiEntity);

        service.updateApi(executionContext, API_ID, USER_ID, SWAGGER_DEFINITION, ENVIRONMENT_ID, DeploymentMode.API_PUBLISHED, LABELS);

        verify(apiService).deploy(eq(executionContext), eq(API_ID), eq(USER_ID), eq(EventType.PUBLISH_API), apiDeploymentCaptor.capture());
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
        when(
            swaggerService.createAPI(
                eq(GraviteeContext.getExecutionContext()),
                any(ImportSwaggerDescriptorEntity.class),
                eq(DefinitionVersion.V2)
            )
        )
            .thenReturn(swaggerApi);

        ApiEntity updatedApiEntity = new ApiEntity();
        updatedApiEntity.setName("updated api");
        updatedApiEntity.setState(Lifecycle.State.STOPPED);
        when(
            apiService.updateFromSwagger(
                eq(GraviteeContext.getExecutionContext()),
                eq(API_ID),
                eq(swaggerApi),
                any(ImportSwaggerDescriptorEntity.class)
            )
        )
            .thenReturn(updatedApiEntity);
        when(
            apiService.deploy(
                eq(GraviteeContext.getExecutionContext()),
                eq(API_ID),
                eq(USER_ID),
                eq(EventType.PUBLISH_API),
                any(ApiDeploymentEntity.class)
            )
        )
            .thenReturn(updatedApiEntity);

        when(apiService.start(GraviteeContext.getExecutionContext(), API_ID, USER_ID)).thenReturn(updatedApiEntity);
        when(planService.findByApi(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(null);
        when(virtualHostService.sanitizeAndValidate(any(), any(), eq(API_ID))).thenReturn(List.of(virtualHost));

        service.updateApi(
            GraviteeContext.getExecutionContext(),
            API_ID,
            USER_ID,
            SWAGGER_DEFINITION,
            ENVIRONMENT_ID,
            DeploymentMode.API_PUBLISHED,
            LABELS
        );

        verify(planService).findByApi(eq(GraviteeContext.getExecutionContext()), eq(API_ID));
        verify(planService).create(eq(GraviteeContext.getExecutionContext()), newPlanCaptor.capture());
        assertThat(newPlanCaptor.getValue())
            .extracting(NewPlanEntity::getApi, NewPlanEntity::getSecurity, NewPlanEntity::getStatus)
            .containsExactly(API_ID, PlanSecurityType.KEY_LESS, PlanStatus.PUBLISHED);
        verify(apiService).start(eq(GraviteeContext.getExecutionContext()), eq(API_ID), eq(USER_ID));
    }

    @Test
    public void should_upgrade_documented_to_published_api() {
        SwaggerApiEntity swaggerApi = new SwaggerApiEntity();
        swaggerApi.setMetadata(new ArrayList<>());
        ExecutionContext executionContext = new ExecutionContext(GraviteeContext.getCurrentOrganization(), ENVIRONMENT_ID);
        ApiEntity api = new ApiEntity();
        api.setId(API_ID);
        Proxy proxy = new Proxy();
        VirtualHost virtualHost = new VirtualHost();
        proxy.setVirtualHosts(List.of(virtualHost));
        api.setProxy(proxy);
        swaggerApi.setProxy(proxy);
        when(swaggerService.createAPI(eq(executionContext), any(ImportSwaggerDescriptorEntity.class), eq(DefinitionVersion.V2)))
            .thenReturn(swaggerApi);

        ApiEntity updatedApiEntity = new ApiEntity();
        updatedApiEntity.setName("updated api");
        updatedApiEntity.setState(Lifecycle.State.STOPPED);
        when(apiService.updateFromSwagger(any(), eq(API_ID), eq(swaggerApi), any(ImportSwaggerDescriptorEntity.class)))
            .thenReturn(updatedApiEntity);
        when(apiService.deploy(any(), eq(API_ID), eq(USER_ID), eq(EventType.PUBLISH_API), any(ApiDeploymentEntity.class)))
            .thenReturn(updatedApiEntity);
        when(apiService.start(any(), eq(API_ID), eq(USER_ID))).thenReturn(updatedApiEntity);

        when(virtualHostService.sanitizeAndValidate(any(), any(), eq(API_ID))).thenReturn(List.of(virtualHost));

        preparePageServiceMock();

        service.updateApi(executionContext, API_ID, USER_ID, SWAGGER_DEFINITION, ENVIRONMENT_ID, DeploymentMode.API_PUBLISHED, LABELS);

        verify(planService).findByApi(eq(executionContext), eq(API_ID));
        verify(planService).create(eq(executionContext), newPlanCaptor.capture());
        assertThat(newPlanCaptor.getValue())
            .extracting(NewPlanEntity::getApi, NewPlanEntity::getSecurity, NewPlanEntity::getStatus)
            .containsExactly(API_ID, PlanSecurityType.KEY_LESS, PlanStatus.PUBLISHED);

        verify(apiService).start(eq(executionContext), eq(API_ID), eq(USER_ID));
        verify(apiService).update(eq(executionContext), eq(API_ID), updateApiCaptor.capture());
        assertThat(updateApiCaptor.getValue())
            .extracting(UpdateApiEntity::getLifecycleState, UpdateApiEntity::getVisibility)
            .containsExactly(ApiLifecycleState.PUBLISHED, Visibility.PUBLIC);

        verify(pageService).update(eq(executionContext), eq(PAGE_ID), updatePageCaptor.capture());
        assertThat(updatePageCaptor.getValue()).extracting(UpdatePageEntity::isPublished).isEqualTo(true);
    }

    @Test
    public void should_upgrade_mocked_to_published_api() {
        ExecutionContext executionContext = new ExecutionContext(GraviteeContext.getCurrentOrganization(), ENVIRONMENT_ID);
        SwaggerApiEntity swaggerApi = new SwaggerApiEntity();
        swaggerApi.setMetadata(new ArrayList<>());
        ApiEntity api = new ApiEntity();
        api.setId(API_ID);
        Proxy proxy = new Proxy();
        VirtualHost virtualHost = new VirtualHost();
        proxy.setVirtualHosts(List.of(virtualHost));
        api.setProxy(proxy);
        swaggerApi.setProxy(proxy);
        when(swaggerService.createAPI(eq(executionContext), any(ImportSwaggerDescriptorEntity.class), eq(DefinitionVersion.V2)))
            .thenReturn(swaggerApi);

        ApiEntity updatedApiEntity = new ApiEntity();
        updatedApiEntity.setName("updated api");
        updatedApiEntity.setState(Lifecycle.State.STARTED);
        when(apiService.deploy(eq(executionContext), eq(API_ID), eq(USER_ID), eq(EventType.PUBLISH_API), any(ApiDeploymentEntity.class)))
            .thenReturn(updatedApiEntity);

        when(planService.findByApi(executionContext, API_ID)).thenReturn(Collections.singleton(new PlanEntity()));
        when(virtualHostService.sanitizeAndValidate(any(), any(), eq(API_ID))).thenReturn(List.of(virtualHost));

        preparePageServiceMock();

        service.updateApi(executionContext, API_ID, USER_ID, SWAGGER_DEFINITION, ENVIRONMENT_ID, DeploymentMode.API_PUBLISHED, LABELS);

        verify(planService).findByApi(eq(executionContext), eq(API_ID));
        verify(planService, never()).create(eq(executionContext), any(NewPlanEntity.class));

        verify(apiService, never()).start(eq(executionContext), eq(API_ID), eq(USER_ID));
        verify(apiService).update(eq(executionContext), eq(API_ID), updateApiCaptor.capture());
        assertThat(updateApiCaptor.getValue())
            .extracting(UpdateApiEntity::getLifecycleState, UpdateApiEntity::getVisibility)
            .containsExactly(ApiLifecycleState.PUBLISHED, Visibility.PUBLIC);

        verify(pageService).update(eq(executionContext), eq(PAGE_ID), updatePageCaptor.capture());
        assertThat(updatePageCaptor.getValue()).extracting(UpdatePageEntity::isPublished).isEqualTo(true);
    }

    @Test
    public void should_check_context_path_unique() {
        SwaggerApiEntity api = new SwaggerApiEntity();
        Proxy proxy = new Proxy();
        VirtualHost virtualHost = new VirtualHost();
        proxy.setVirtualHosts(List.of(virtualHost));
        api.setProxy(proxy);

        when(virtualHostService.sanitizeAndValidate(any(), anyList(), eq(null))).thenReturn(List.of(virtualHost));
        var message = service.checkContextPath(GraviteeContext.getExecutionContext(), api);

        verify(virtualHostService).sanitizeAndValidate(any(), eq(List.of(virtualHost)), eq(null));
        assertThat(message.isPresent()).isFalse();
    }

    @Test
    public void should_check_context_path_not_unique() {
        SwaggerApiEntity api = new SwaggerApiEntity();
        Proxy proxy = new Proxy();
        VirtualHost virtualHost = new VirtualHost();
        proxy.setVirtualHosts(List.of(virtualHost));
        api.setProxy(proxy);

        when(virtualHostService.sanitizeAndValidate(any(), anyList(), eq(null)))
            .thenThrow(new ApiContextPathAlreadyExistsException("contextPath"));
        var message = service.checkContextPath(GraviteeContext.getExecutionContext(), api);

        verify(virtualHostService).sanitizeAndValidate(any(), eq(List.of(virtualHost)), eq(null));
        assertThat(message.isPresent()).isTrue();
        assertThat(message.get())
            .isEqualTo("The context [contextPath] automatically generated from the name is already covered by another API.");
    }

    private void preparePageServiceMock() {
        PageEntity page = new PageEntity();
        page.setId(PAGE_ID);
        page.setType(PageType.SWAGGER.name());

        when(
            pageService.search(
                eq(ENVIRONMENT_ID),
                argThat((PageQuery query) -> query.getApi().equals(API_ID) && query.getType().equals(PageType.SWAGGER))
            )
        )
            .thenReturn(List.of(page));
    }
}
