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
package io.gravitee.rest.api.service.v4.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.ResponseTemplate;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.failover.Failover;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.execution.FlowExecution;
import io.gravitee.definition.model.v4.flow.execution.FlowMode;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.nativeapi.NativeApiServices;
import io.gravitee.definition.model.v4.nativeapi.NativeEndpointGroup;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import io.gravitee.definition.model.v4.nativeapi.kafka.KafkaListener;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.definition.model.v4.service.ApiServices;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.Visibility;
import io.gravitee.rest.api.model.ApiMetadataEntity;
import io.gravitee.rest.api.model.CategoryEntity;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.NewApiEntity;
import io.gravitee.rest.api.model.v4.api.UpdateApiEntity;
import io.gravitee.rest.api.model.v4.api.properties.PropertyEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.WorkflowService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.common.ReferenceContext;
import io.gravitee.rest.api.service.converter.CategoryMapper;
import io.gravitee.rest.api.service.v4.FlowService;
import io.gravitee.rest.api.service.v4.PlanService;
import java.util.Date;
import java.util.HashMap;
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
public class ApiMapperTest {

    private ApiMapper apiMapper;

    @Mock
    private PlanService planService;

    @Mock
    private FlowService flowService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private ParameterService parameterService;

    @Mock
    private WorkflowService workflowService;

    private ObjectMapper objectMapper;

    @Before
    public void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        apiMapper = new ApiMapper(objectMapper, planService, flowService, parameterService, workflowService, categoryMapper);
    }

    @Test
    public void shouldCreateEntityFromApiDefinition() throws JsonProcessingException {
        io.gravitee.definition.model.v4.Api apiDefinition = new io.gravitee.definition.model.v4.Api();
        apiDefinition.setDefinitionVersion(DefinitionVersion.V4);
        apiDefinition.setListeners(List.of(new HttpListener()));
        apiDefinition.setEndpointGroups(List.of(new EndpointGroup()));
        apiDefinition.setServices(new ApiServices());
        apiDefinition.setResources(List.of(new Resource()));
        apiDefinition.setProperties(List.of(new Property("key", "value")));
        apiDefinition.setTags(Set.of("tag"));
        apiDefinition.setFlowExecution(new FlowExecution());
        apiDefinition.setFlows(List.of(new Flow(), new Flow()));
        apiDefinition.setResponseTemplates(Map.of("/", Map.of("/", new ResponseTemplate())));
        apiDefinition.setFailover(
            Failover.builder().enabled(true).perSubscription(false).maxFailures(3).openStateDuration(11000).slowCallDuration(500).build()
        );

        Api api = new Api();
        api.setId("id");
        api.setCrossId("crossId");
        api.setType(ApiType.PROXY);
        api.setName("name");
        api.setVersion("version");
        api.setUpdatedAt(new Date());
        api.setDeployedAt(new Date());
        api.setCreatedAt(new Date());
        api.setDescription("description");
        api.setGroups(Set.of("group1"));
        api.setEnvironmentId("environmentId");
        api.setCategories(Set.of("category"));
        api.setPicture("picture");
        api.setBackground("background");
        api.setLabels(List.of("label"));
        api.setLifecycleState(LifecycleState.STARTED);
        api.setVisibility(Visibility.PUBLIC);
        api.setApiLifecycleState(ApiLifecycleState.CREATED);

        api.setDefinition(objectMapper.writeValueAsString(apiDefinition));

        when(categoryMapper.toCategoryKey(any(), eq(api.getCategories()))).thenReturn(api.getCategories());

        io.gravitee.rest.api.model.v4.api.ApiEntity apiEntity = apiMapper.toEntity(api, new PrimaryOwnerEntity());

        assertThat(apiEntity.getId()).isEqualTo("id");
        assertThat(apiEntity.getCrossId()).isEqualTo("crossId");
        assertThat(apiEntity.getType()).isEqualTo(ApiType.PROXY);
        assertThat(apiEntity.getName()).isEqualTo("name");
        assertThat(apiEntity.getApiVersion()).isEqualTo("version");
        assertThat(apiEntity.getUpdatedAt()).isNotNull();
        assertThat(apiEntity.getDeployedAt()).isNotNull();
        assertThat(apiEntity.getCreatedAt()).isNotNull();
        assertThat(apiEntity.getDescription()).isEqualTo("description");
        assertThat(apiEntity.getGroups().size()).isEqualTo(1);
        assertThat(apiEntity.getReferenceType()).isEqualTo(ReferenceContext.Type.ENVIRONMENT.name());
        assertThat(apiEntity.getReferenceId()).isEqualTo("environmentId");
        assertThat(apiEntity.getCategories().size()).isEqualTo(1);
        assertThat(apiEntity.getPicture()).isEqualTo("picture");
        assertThat(apiEntity.getBackground()).isEqualTo("background");
        assertThat(apiEntity.getLabels().size()).isEqualTo(1);
        assertThat(apiEntity.getLifecycleState()).isEqualTo(io.gravitee.rest.api.model.api.ApiLifecycleState.CREATED);
        assertThat(apiEntity.getState()).isEqualTo(Lifecycle.State.STARTED);
        assertThat(apiEntity.getVisibility()).isEqualTo(io.gravitee.rest.api.model.Visibility.PUBLIC);

        assertThat(apiEntity.getDefinitionVersion()).isEqualTo(DefinitionVersion.V4);
        assertThat(apiEntity.getListeners()).isNotNull();
        assertThat(apiEntity.getListeners().size()).isEqualTo(1);
        assertThat(apiEntity.getEndpointGroups()).isNotNull();
        assertThat(apiEntity.getEndpointGroups().size()).isEqualTo(1);
        assertThat(apiEntity.getServices()).isNotNull();
        assertThat(apiEntity.getResources()).isNotNull();
        assertThat(apiEntity.getResources().size()).isEqualTo(1);
        assertThat(apiEntity.getProperties()).isNotNull();
        assertThat(apiEntity.getProperties().size()).isEqualTo(1);
        assertThat(apiEntity.getTags()).isNotNull();
        assertThat(apiEntity.getTags().size()).isEqualTo(1);
        assertThat(apiEntity.getFlowExecution()).isNotNull();
        assertThat(apiEntity.getFlowExecution().getMode()).isEqualTo(FlowMode.DEFAULT);
        assertThat(apiEntity.getFlows()).isNotNull();
        assertThat(apiEntity.getFlows().size()).isEqualTo(2);
        assertThat(apiEntity.getResponseTemplates()).isNotNull();
        assertThat(apiEntity.getResponseTemplates().size()).isEqualTo(1);
        assertThat(apiEntity.getFailover())
            .isEqualTo(
                Failover
                    .builder()
                    .enabled(true)
                    .perSubscription(false)
                    .maxFailures(3)
                    .openStateDuration(11000)
                    .slowCallDuration(500)
                    .build()
            );
    }

    @Test
    public void shouldCreateEntityIgnoringWrongApiDefinition() throws JsonProcessingException {
        Api api = new Api();
        api.setId("id");
        api.setCrossId("crossId");
        api.setType(ApiType.PROXY);
        api.setName("name");
        api.setVersion("version");
        api.setUpdatedAt(new Date());
        api.setDeployedAt(new Date());
        api.setCreatedAt(new Date());
        api.setDescription("description");
        api.setGroups(Set.of("group1"));
        api.setEnvironmentId("environmentId");
        api.setCategories(Set.of("category"));
        api.setPicture("picture");
        api.setBackground("background");
        api.setLabels(List.of("label"));
        api.setLifecycleState(LifecycleState.STARTED);
        api.setVisibility(Visibility.PUBLIC);
        api.setApiLifecycleState(ApiLifecycleState.CREATED);
        api.setDefinition("wrong api definition");

        when(categoryMapper.toCategoryKey(eq(api.getEnvironmentId()), eq(api.getCategories()))).thenReturn(api.getCategories());

        io.gravitee.rest.api.model.v4.api.ApiEntity apiEntity = apiMapper.toEntity(api, null);

        assertThat(apiEntity.getId()).isEqualTo("id");
        assertThat(apiEntity.getCrossId()).isEqualTo("crossId");
        assertThat(apiEntity.getType()).isEqualTo(ApiType.PROXY);
        assertThat(apiEntity.getName()).isEqualTo("name");
        assertThat(apiEntity.getApiVersion()).isEqualTo("version");
        assertThat(apiEntity.getUpdatedAt()).isNotNull();
        assertThat(apiEntity.getDeployedAt()).isNotNull();
        assertThat(apiEntity.getCreatedAt()).isNotNull();
        assertThat(apiEntity.getDescription()).isEqualTo("description");
        assertThat(apiEntity.getGroups().size()).isEqualTo(1);
        assertThat(apiEntity.getReferenceType()).isEqualTo(ReferenceContext.Type.ENVIRONMENT.name());
        assertThat(apiEntity.getReferenceId()).isEqualTo("environmentId");
        assertThat(apiEntity.getCategories().size()).isEqualTo(1);
        assertThat(apiEntity.getPicture()).isEqualTo("picture");
        assertThat(apiEntity.getBackground()).isEqualTo("background");
        assertThat(apiEntity.getLabels().size()).isEqualTo(1);
        assertThat(apiEntity.getLifecycleState()).isEqualTo(io.gravitee.rest.api.model.api.ApiLifecycleState.CREATED);
        assertThat(apiEntity.getState()).isEqualTo(Lifecycle.State.STARTED);
        assertThat(apiEntity.getVisibility()).isEqualTo(io.gravitee.rest.api.model.Visibility.PUBLIC);

        assertThat(apiEntity.getDefinitionVersion()).isNull();
        assertThat(apiEntity.getListeners()).isNull();
        assertThat(apiEntity.getEndpointGroups()).isNull();
        assertThat(apiEntity.getServices()).isNull();
        assertThat(apiEntity.getResources()).isEmpty();
        assertThat(apiEntity.getProperties()).isEmpty();
        assertThat(apiEntity.getTags()).isEmpty();
        assertThat(apiEntity.getFlows()).isNull();
        assertThat(apiEntity.getResponseTemplates()).isEmpty();
        assertThat(apiEntity.getFailover()).isNull();
    }

    @Test
    public void shouldCreateRepositoryApiFromNewEntity() throws JsonProcessingException {
        NewApiEntity newApiEntity = new NewApiEntity();
        newApiEntity.setName("name");
        newApiEntity.setApiVersion("version");
        newApiEntity.setType(ApiType.MESSAGE);
        newApiEntity.setTags(Set.of("tag"));
        newApiEntity.setGroups(Set.of("group1"));
        newApiEntity.setListeners(List.of(new HttpListener()));
        newApiEntity.setEndpointGroups(List.of(new EndpointGroup()));
        newApiEntity.setFlowExecution(new FlowExecution());
        newApiEntity.setFlows(List.of(new Flow(), new Flow()));
        newApiEntity.setFailover(
            Failover.builder().enabled(true).perSubscription(false).maxFailures(3).openStateDuration(11000).slowCallDuration(500).build()
        );

        Api api = apiMapper.toRepository(GraviteeContext.getExecutionContext(), newApiEntity);
        assertThat(api.getDescription()).isNull();

        newApiEntity.setDescription("description");
        api = apiMapper.toRepository(GraviteeContext.getExecutionContext(), newApiEntity);

        assertThat(api.getId()).isNotNull();
        assertThat(api.getType()).isEqualTo(ApiType.MESSAGE);
        assertThat(api.getName()).isEqualTo("name");
        assertThat(api.getVersion()).isEqualTo("version");
        assertThat(api.getCreatedAt()).isNotNull();
        assertThat(api.getUpdatedAt()).isEqualTo(api.getCreatedAt());
        assertThat(api.getDeployedAt()).isNull();
        assertThat(api.getDescription()).isEqualTo("description");
        assertThat(api.getGroups().size()).isEqualTo(1);
        assertThat(api.getEnvironmentId()).isEqualTo("DEFAULT");
        assertThat(api.getApiLifecycleState()).isEqualTo(ApiLifecycleState.CREATED);
        assertThat(api.getLifecycleState()).isEqualTo(LifecycleState.STOPPED);
        assertThat(api.getVisibility()).isEqualTo(Visibility.PRIVATE);
        assertThat(api.getDefinitionVersion()).isEqualTo(DefinitionVersion.V4);

        io.gravitee.definition.model.v4.Api apiDefinition = new io.gravitee.definition.model.v4.Api();
        apiDefinition.setId(api.getId());
        apiDefinition.setName(api.getName());
        apiDefinition.setType(ApiType.MESSAGE);
        apiDefinition.setApiVersion(api.getVersion());
        apiDefinition.setDefinitionVersion(DefinitionVersion.V4);
        apiDefinition.setListeners(List.of(new HttpListener()));
        apiDefinition.setEndpointGroups(List.of(new EndpointGroup()));
        apiDefinition.setTags(Set.of("tag"));
        apiDefinition.setFlowExecution(new FlowExecution());
        apiDefinition.setFlows(List.of(new Flow(), new Flow()));
        apiDefinition.setFailover(
            Failover.builder().enabled(true).perSubscription(false).maxFailures(3).openStateDuration(11000).slowCallDuration(500).build()
        );
        assertThat(api.getDefinition()).isEqualTo(objectMapper.writeValueAsString(apiDefinition));
    }

    @Test
    public void shouldCreateRepositoryApiFromUpdateEntity() throws JsonProcessingException {
        UpdateApiEntity updateApiEntity = new UpdateApiEntity();
        updateApiEntity.setId("id");
        updateApiEntity.setCrossId("crossId");
        updateApiEntity.setName("name");
        updateApiEntity.setApiVersion("version");
        updateApiEntity.setDefinitionVersion(DefinitionVersion.V4);
        updateApiEntity.setType(ApiType.MESSAGE);
        updateApiEntity.setDescription("description");
        updateApiEntity.setVisibility(io.gravitee.rest.api.model.Visibility.PUBLIC);
        updateApiEntity.setTags(Set.of("tag1", "tag2"));
        updateApiEntity.setPicture("my-picture");
        updateApiEntity.setPictureUrl("/path/to/my/picture");
        updateApiEntity.setBackground("my-background");
        updateApiEntity.setBackgroundUrl("/path/to/my/background");
        updateApiEntity.setCategories(Set.of("existingCatId", "existingCatKey", "unknownCat"));
        updateApiEntity.setLabels(List.of("label1", "label2"));
        updateApiEntity.setGroups(Set.of("group1", "group2"));
        updateApiEntity.setListeners(List.of(new HttpListener()));
        updateApiEntity.setEndpointGroups(List.of(new EndpointGroup()));
        updateApiEntity.setFlowExecution(new FlowExecution());
        updateApiEntity.setFlows(List.of(new Flow(), new Flow()));
        updateApiEntity.setMetadata(List.of(new ApiMetadataEntity()));
        updateApiEntity.setLifecycleState(io.gravitee.rest.api.model.api.ApiLifecycleState.UNPUBLISHED);
        updateApiEntity.setDisableMembershipNotifications(true);
        updateApiEntity.setFailover(
            Failover.builder().enabled(true).perSubscription(false).maxFailures(3).openStateDuration(11000).slowCallDuration(500).build()
        );
        updateApiEntity.setProperties(
            List.of(
                new PropertyEntity("propKey", "propValue", false, false),
                new PropertyEntity("dynPropKey", "dynPropValue", false, false, true)
            )
        );
        updateApiEntity.setResources(List.of(new Resource()));
        updateApiEntity.setPlans(Set.of(new PlanEntity()));

        CategoryEntity existingCategoryByIdEntity = new CategoryEntity();
        existingCategoryByIdEntity.setId("existingCatId");
        CategoryEntity existingCategoryByKeyEntity = new CategoryEntity();
        existingCategoryByKeyEntity.setKey("existingCatKey");

        when(categoryMapper.toCategoryId(any(), eq(updateApiEntity.getCategories()))).thenReturn(updateApiEntity.getCategories());

        Api api = apiMapper.toRepository(GraviteeContext.getExecutionContext(), updateApiEntity);

        assertThat(api.getId()).isEqualTo("id");
        assertThat(api.getEnvironmentId()).isEqualTo("DEFAULT");
        assertThat(api.getCrossId()).isEqualTo("crossId");
        assertThat(api.getName()).isEqualTo("name");
        assertThat(api.getDescription()).isEqualTo("description");
        assertThat(api.getVersion()).isEqualTo("version");
        assertThat(api.getDefinitionVersion()).isEqualTo(DefinitionVersion.V4);
        assertThat(api.getType()).isEqualTo(ApiType.MESSAGE);
        assertThat(api.getDeployedAt()).isNull();
        assertThat(api.getCreatedAt()).isNull();
        assertThat(api.getUpdatedAt()).isNotNull();
        assertThat(api.getVisibility()).isEqualTo(Visibility.PUBLIC);
        assertThat(api.getPicture()).isEqualTo(updateApiEntity.getPicture());
        assertThat(api.getBackground()).isEqualTo(updateApiEntity.getBackground());
        assertThat(api.getGroups().size()).isEqualTo(2);
        assertThat(api.getCategories().size()).isEqualTo(3);
        assertThat(api.getLabels().size()).isEqualTo(2);
        assertThat(api.isDisableMembershipNotifications()).isTrue();
        assertThat(api.getApiLifecycleState()).isEqualTo(ApiLifecycleState.UNPUBLISHED);

        io.gravitee.definition.model.v4.Api apiDefinition = new io.gravitee.definition.model.v4.Api();
        apiDefinition.setId(api.getId());
        apiDefinition.setName(api.getName());
        apiDefinition.setType(ApiType.MESSAGE);
        apiDefinition.setApiVersion(api.getVersion());
        apiDefinition.setDefinitionVersion(DefinitionVersion.V4);
        apiDefinition.setTags(Set.of("tag1", "tag2"));
        apiDefinition.setListeners(List.of(new HttpListener()));
        apiDefinition.setEndpointGroups(List.of(new EndpointGroup()));
        apiDefinition.setProperties(
            List.of(new Property("propKey", "propValue", false, false), new Property("dynPropKey", "dynPropValue", false, true))
        );
        apiDefinition.setFailover(
            Failover.builder().enabled(true).perSubscription(false).maxFailures(3).openStateDuration(11000).slowCallDuration(500).build()
        );
        apiDefinition.setResources(List.of(new Resource()));
        apiDefinition.setFlowExecution(new FlowExecution());
        apiDefinition.setFlows(List.of(new Flow(), new Flow()));
        apiDefinition.setResponseTemplates(new HashMap<>());
        assertThat(api.getDefinition()).isEqualTo(objectMapper.writeValueAsString(apiDefinition));
    }

    @Test
    public void shouldCreateRepositoryApiFromApiEntity() throws JsonProcessingException {
        ApiEntity apiEntity = new ApiEntity();
        apiEntity.setId("id");
        apiEntity.setCrossId("crossId");
        apiEntity.setName("name");
        apiEntity.setApiVersion("version");
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        apiEntity.setType(ApiType.MESSAGE);
        apiEntity.setVisibility(io.gravitee.rest.api.model.Visibility.PUBLIC);
        apiEntity.setTags(Set.of("tag1", "tag2"));
        apiEntity.setPicture("my-picture");
        apiEntity.setPictureUrl("/path/to/my/picture");
        apiEntity.setBackground("my-background");
        apiEntity.setBackgroundUrl("/path/to/my/background");
        apiEntity.setCategories(Set.of("existingCatId", "existingCatKey", "unknownCat"));
        apiEntity.setLabels(List.of("label1", "label2"));
        apiEntity.setGroups(Set.of("group1", "group2"));
        apiEntity.setListeners(List.of(new HttpListener()));
        apiEntity.setEndpointGroups(List.of(new EndpointGroup()));
        apiEntity.setFlowExecution(new FlowExecution());
        apiEntity.setFlows(List.of(new Flow(), new Flow()));
        apiEntity.setMetadata(Map.of("key", "value"));
        apiEntity.setLifecycleState(io.gravitee.rest.api.model.api.ApiLifecycleState.UNPUBLISHED);
        apiEntity.setDisableMembershipNotifications(true);
        apiEntity.setProperties(null);
        apiEntity.setResources(List.of(new Resource()));
        apiEntity.setPlans(Set.of(new PlanEntity()));
        apiEntity.setUpdatedAt(new Date());
        apiEntity.setFailover(
            Failover.builder().enabled(true).perSubscription(false).maxFailures(3).openStateDuration(11000).slowCallDuration(500).build()
        );

        CategoryEntity existingCategoryByIdEntity = new CategoryEntity();
        existingCategoryByIdEntity.setId("existingCatId");
        CategoryEntity existingCategoryByKeyEntity = new CategoryEntity();
        existingCategoryByKeyEntity.setKey("existingCatKey");

        when(categoryMapper.toCategoryId(any(), eq(apiEntity.getCategories()))).thenReturn(apiEntity.getCategories());

        Api api = apiMapper.toRepository(GraviteeContext.getExecutionContext(), apiEntity);
        assertThat(api.getDescription()).isNull();

        apiEntity.setDescription("description");
        api = apiMapper.toRepository(GraviteeContext.getExecutionContext(), apiEntity);

        assertThat(api.getId()).isEqualTo("id");
        assertThat(api.getEnvironmentId()).isEqualTo("DEFAULT");
        assertThat(api.getCrossId()).isEqualTo("crossId");
        assertThat(api.getName()).isEqualTo("name");
        assertThat(api.getDescription()).isEqualTo("description");
        assertThat(api.getVersion()).isEqualTo("version");
        assertThat(api.getDefinitionVersion()).isEqualTo(DefinitionVersion.V4);
        assertThat(api.getType()).isEqualTo(ApiType.MESSAGE);
        assertThat(api.getDeployedAt()).isNull();
        assertThat(api.getCreatedAt()).isNull();
        assertThat(api.getUpdatedAt()).isNotNull();
        assertThat(api.getVisibility()).isEqualTo(Visibility.PUBLIC);
        assertThat(api.getLifecycleState()).isEqualTo(LifecycleState.STOPPED);
        assertThat(api.getPicture()).isEqualTo(apiEntity.getPicture());
        assertThat(api.getBackground()).isEqualTo(apiEntity.getBackground());
        assertThat(api.getGroups().size()).isEqualTo(2);
        assertThat(api.getCategories().size()).isEqualTo(3);
        assertThat(api.getLabels().size()).isEqualTo(2);
        assertThat(api.isDisableMembershipNotifications()).isTrue();
        assertThat(api.getApiLifecycleState()).isEqualTo(ApiLifecycleState.UNPUBLISHED);

        io.gravitee.definition.model.v4.Api apiDefinition = new io.gravitee.definition.model.v4.Api();
        apiDefinition.setId(api.getId());
        apiDefinition.setName(api.getName());
        apiDefinition.setType(ApiType.MESSAGE);
        apiDefinition.setApiVersion(api.getVersion());
        apiDefinition.setDefinitionVersion(DefinitionVersion.V4);
        apiDefinition.setTags(Set.of("tag1", "tag2"));
        apiDefinition.setListeners(List.of(new HttpListener()));
        apiDefinition.setEndpointGroups(List.of(new EndpointGroup()));
        apiDefinition.setProperties(null);
        apiDefinition.setResources(List.of(new Resource()));
        apiDefinition.setFlowExecution(new FlowExecution());
        apiDefinition.setFlows(List.of(new Flow(), new Flow()));
        apiDefinition.setResponseTemplates(new HashMap<>());
        apiDefinition.setFailover(
            Failover.builder().enabled(true).perSubscription(false).maxFailures(3).openStateDuration(11000).slowCallDuration(500).build()
        );
        assertThat(api.getDefinition()).isEqualTo(objectMapper.writeValueAsString(apiDefinition));
    }

    @Test
    public void shouldCreateNativeEntityFromApiRepository() throws JsonProcessingException {
        var apiDefinition = new io.gravitee.definition.model.v4.nativeapi.NativeApi();
        apiDefinition.setDefinitionVersion(DefinitionVersion.V4);
        apiDefinition.setListeners(List.of(new KafkaListener()));
        apiDefinition.setEndpointGroups(List.of(new NativeEndpointGroup()));
        apiDefinition.setServices(new NativeApiServices());
        apiDefinition.setResources(List.of(new Resource()));
        apiDefinition.setProperties(List.of(new Property("key", "value")));
        apiDefinition.setTags(Set.of("tag"));
        apiDefinition.setFlows(List.of(new NativeFlow(), new NativeFlow()));

        Api api = new Api();
        api.setId("id");
        api.setCrossId("crossId");
        api.setType(ApiType.NATIVE);
        api.setName("name");
        api.setVersion("version");
        api.setUpdatedAt(new Date());
        api.setDeployedAt(new Date());
        api.setCreatedAt(new Date());
        api.setDescription("description");
        api.setGroups(Set.of("group1"));
        api.setEnvironmentId("environmentId");
        api.setCategories(Set.of("category"));
        api.setPicture("picture");
        api.setBackground("background");
        api.setLabels(List.of("label"));
        api.setLifecycleState(LifecycleState.STARTED);
        api.setVisibility(Visibility.PUBLIC);
        api.setApiLifecycleState(ApiLifecycleState.CREATED);

        api.setDefinition(objectMapper.writeValueAsString(apiDefinition));

        when(categoryMapper.toCategoryKey(any(), eq(api.getCategories()))).thenReturn(api.getCategories());

        var nativeEntity = apiMapper.toNativeEntity(api, new PrimaryOwnerEntity());

        assertThat(nativeEntity.getId()).isEqualTo("id");
        assertThat(nativeEntity.getCrossId()).isEqualTo("crossId");
        assertThat(nativeEntity.getType()).isEqualTo(ApiType.NATIVE);
        assertThat(nativeEntity.getName()).isEqualTo("name");
        assertThat(nativeEntity.getApiVersion()).isEqualTo("version");
        assertThat(nativeEntity.getUpdatedAt()).isNotNull();
        assertThat(nativeEntity.getDeployedAt()).isNotNull();
        assertThat(nativeEntity.getCreatedAt()).isNotNull();
        assertThat(nativeEntity.getDescription()).isEqualTo("description");
        assertThat(nativeEntity.getGroups().size()).isEqualTo(1);
        assertThat(nativeEntity.getReferenceType()).isEqualTo(ReferenceContext.Type.ENVIRONMENT.name());
        assertThat(nativeEntity.getReferenceId()).isEqualTo("environmentId");
        assertThat(nativeEntity.getCategories().size()).isEqualTo(1);
        assertThat(nativeEntity.getPicture()).isEqualTo("picture");
        assertThat(nativeEntity.getBackground()).isEqualTo("background");
        assertThat(nativeEntity.getLabels().size()).isEqualTo(1);
        assertThat(nativeEntity.getLifecycleState()).isEqualTo(io.gravitee.rest.api.model.api.ApiLifecycleState.CREATED);
        assertThat(nativeEntity.getState()).isEqualTo(Lifecycle.State.STARTED);
        assertThat(nativeEntity.getVisibility()).isEqualTo(io.gravitee.rest.api.model.Visibility.PUBLIC);

        assertThat(nativeEntity.getDefinitionVersion()).isEqualTo(DefinitionVersion.V4);
        assertThat(nativeEntity.getListeners()).isNotNull();
        assertThat(nativeEntity.getListeners().size()).isEqualTo(1);
        assertThat(nativeEntity.getEndpointGroups()).isNotNull();
        assertThat(nativeEntity.getEndpointGroups().size()).isEqualTo(1);
        assertThat(nativeEntity.getServices()).isNotNull();
        assertThat(nativeEntity.getResources()).isNotNull();
        assertThat(nativeEntity.getResources().size()).isEqualTo(1);
        assertThat(nativeEntity.getProperties()).isNotNull();
        assertThat(nativeEntity.getProperties().size()).isEqualTo(1);
        assertThat(nativeEntity.getTags()).isNotNull();
        assertThat(nativeEntity.getTags().size()).isEqualTo(1);
        assertThat(nativeEntity.getFlows()).isNotNull();
        assertThat(nativeEntity.getFlows().size()).isEqualTo(2);
    }
}
