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

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.api.domain_service.VerifyApiPathDomainService;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.*;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.TagReferenceType;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.alert.AlertReferenceType;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.UpdateApiEntity;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.CategoryMapper;
import io.gravitee.rest.api.service.exceptions.ApiDefinitionVersionNotSupportedException;
import io.gravitee.rest.api.service.exceptions.EndpointGroupNameAlreadyExistsException;
import io.gravitee.rest.api.service.exceptions.EndpointNameAlreadyExistsException;
import io.gravitee.rest.api.service.exceptions.TagNotAllowedException;
import io.gravitee.rest.api.service.exceptions.TagNotFoundException;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.v4.ApiCategoryService;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import io.gravitee.rest.api.service.v4.validation.AnalyticsValidationService;
import io.gravitee.rest.api.service.v4.validation.CorsValidationService;
import java.io.InputStream;
import java.util.Set;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiService_CreateWithDefinitionTest {

    private static final ObjectMapper MAPPER = new GraviteeMapper();
    private static final String USERNAME = "user-name";

    @InjectMocks
    private final ApiService apiService = new ApiServiceImpl();

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private VerifyApiPathDomainService verifyApiPathDomainService;

    @Mock
    private ConnectorService connectorService;

    @Mock
    private ParameterService parameterService;

    @Mock
    private UserService userService;

    @Mock
    private V4EmulationEngineService v4EmulationEngine;

    @Mock
    private GroupService groupService;

    @Mock
    private AuditService auditService;

    @Mock
    private MembershipService membershipService;

    @Mock
    private GenericNotificationConfigService notificationConfigService;

    @Mock
    private SearchEngineService searchEngineService;

    @Mock
    private ApiMetadataService apiMetadataService;

    @Mock
    private FlowService flowService;

    @Mock
    private ApiConverter apiConverter;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PlanService planService;

    @Mock
    private NotificationTemplateService notificationTemplateService;

    @Mock
    private AnalyticsValidationService loggingValidationService;

    @Mock
    private PrimaryOwnerService primaryOwnerService;

    @Mock
    private TagService tagService;

    @Mock
    private AlertService alertService;

    @Mock
    private CorsValidationService corsValidationService;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private ApiCategoryService apiCategoryService;

    @AfterClass
    public static void cleanSecurityContextHolder() {
        // reset authentication to avoid side effect during test executions.
        SecurityContextHolder.setContext(
            new SecurityContext() {
                @Override
                public Authentication getAuthentication() {
                    return null;
                }

                @Override
                public void setAuthentication(Authentication authentication) {}
            }
        );
    }

    @Before
    public void init() {
        final SecurityContext securityContext = mock(SecurityContext.class);
        final Authentication authentication = mock(Authentication.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetails(USERNAME, "", emptyList()));
        SecurityContextHolder.setContext(securityContext);
        when(verifyApiPathDomainService.validateAndSanitize(any()))
            .thenAnswer(invocation -> Validator.Result.ofValue(invocation.getArgument(0)));
    }

    @Test
    public void shouldStartApiIfManagedByKubernetes() throws Exception {
        JsonNode definition = readDefinition("/io/gravitee/rest/api/management/service/import-new-kubernetes-api.definition.json");
        UpdateApiEntity api = new UpdateApiEntity();
        Proxy proxy = new Proxy();
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("endpointGroupName");
        Endpoint endpoint = Endpoint.builder().name("endpointName").build();
        endpointGroup.setEndpoints(singleton(endpoint));
        proxy.setGroups(singleton(endpointGroup));
        proxy.setVirtualHosts(singletonList(new VirtualHost("/context")));
        api.setProxy(proxy);
        api.setVersion("1.0");
        api.setName("k8s basic");
        api.setDescription("k8s basic example");

        when(primaryOwnerService.getPrimaryOwner(any(), any(), any())).thenReturn(new PrimaryOwnerEntity(new UserEntity()));

        Api updateApiDefinition = new Api();
        when(objectMapper.readValue(any(String.class), eq(Api.class))).thenReturn(updateApiDefinition);

        when(apiRepository.create(any())).thenReturn(new io.gravitee.repository.management.model.Api());

        when(apiConverter.toApiEntity(any(), any(), any(), anyBoolean())).thenReturn(new ApiEntity());

        when(apiMetadataService.fetchMetadataForApi(any(), any())).thenReturn(new ApiEntity());

        apiService.createWithApiDefinition(GraviteeContext.getExecutionContext(), api, USERNAME, definition);

        verify(apiRepository, times(1)).create(argThat(arg -> arg.getLifecycleState().equals(LifecycleState.STARTED)));
        verify(alertService, times(1)).createDefaults(any(ExecutionContext.class), eq(AlertReferenceType.API), any());
    }

    @Test(expected = ApiDefinitionVersionNotSupportedException.class)
    public void shouldNotCreateIfDefinitionV1() throws Exception {
        UpdateApiEntity api = new UpdateApiEntity();
        api.setGraviteeDefinitionVersion("1.0.0");

        apiService.createWithApiDefinition(GraviteeContext.getExecutionContext(), api, "", null);
    }

    @Test
    public void shouldNotStartApiIfNotManagedByKubernetesAndStateIsStopped() throws Exception {
        JsonNode definition = readDefinition("/io/gravitee/rest/api/management/service/import-new-kubernetes-api.definition.json");
        UpdateApiEntity api = new UpdateApiEntity();
        Proxy proxy = new Proxy();
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("endpointGroupName");
        Endpoint endpoint = Endpoint.builder().name("endpointName").build();
        endpointGroup.setEndpoints(singleton(endpoint));
        proxy.setGroups(singleton(endpointGroup));
        proxy.setVirtualHosts(singletonList(new VirtualHost("/context")));
        api.setProxy(proxy);
        api.setVersion("1.0");
        api.setName("k8s basic");
        api.setDescription("k8s basic example");
        api.setState(Lifecycle.State.STOPPED);

        when(primaryOwnerService.getPrimaryOwner(any(), any(), any())).thenReturn(new PrimaryOwnerEntity(new UserEntity()));

        Api updateApiDefinition = new Api();
        when(objectMapper.readValue(any(String.class), eq(Api.class))).thenReturn(updateApiDefinition);

        when(apiRepository.create(any())).thenReturn(new io.gravitee.repository.management.model.Api());

        when(apiConverter.toApiEntity(any(), any(), any(), anyBoolean())).thenReturn(new ApiEntity());

        when(apiMetadataService.fetchMetadataForApi(any(), any())).thenReturn(new ApiEntity());

        apiService.createWithApiDefinition(GraviteeContext.getExecutionContext(), api, USERNAME, definition);

        verify(apiRepository, times(1)).create(argThat(arg -> arg.getLifecycleState().equals(LifecycleState.STOPPED)));
        verify(alertService, times(1)).createDefaults(any(ExecutionContext.class), eq(AlertReferenceType.API), any());
    }

    @Test
    public void shouldNotStartApiIfNotManagedByKubernetes() throws Exception {
        JsonNode definition = readDefinition("/io/gravitee/rest/api/management/service/import-new-api.definition.json");
        UpdateApiEntity api = new UpdateApiEntity();
        Proxy proxy = new Proxy();
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("endpointGroupName");
        Endpoint endpoint = Endpoint.builder().name("endpointName").build();
        endpointGroup.setEndpoints(singleton(endpoint));
        proxy.setGroups(singleton(endpointGroup));
        proxy.setVirtualHosts(singletonList(new VirtualHost("/context")));
        api.setProxy(proxy);
        api.setVersion("1.0");
        api.setName("k8s basic");
        api.setDescription("k8s basic example");

        when(primaryOwnerService.getPrimaryOwner(any(), any(), any())).thenReturn(new PrimaryOwnerEntity(new UserEntity()));

        Api updateApiDefinition = new Api();
        when(objectMapper.readValue(any(String.class), eq(Api.class))).thenReturn(updateApiDefinition);

        when(apiRepository.create(any())).thenReturn(new io.gravitee.repository.management.model.Api());

        when(apiConverter.toApiEntity(any(), any(), any(), anyBoolean())).thenReturn(new ApiEntity());

        when(apiMetadataService.fetchMetadataForApi(any(), any())).thenReturn(new ApiEntity());

        apiService.createWithApiDefinition(GraviteeContext.getExecutionContext(), api, USERNAME, definition);

        verify(apiRepository, times(1)).create(argThat(arg -> arg.getLifecycleState().equals(LifecycleState.STOPPED)));
        verify(alertService, times(1)).createDefaults(any(ExecutionContext.class), eq(AlertReferenceType.API), any());
    }

    @Test
    public void should_not_create_if_tag_does_not_exist() throws Exception {
        JsonNode definition = readDefinition("/io/gravitee/rest/api/management/service/import-api.definition+plans.json");
        UpdateApiEntity api = new UpdateApiEntity();
        Proxy proxy = new Proxy();
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("endpointGroupName");
        Endpoint endpoint = Endpoint.builder().name("endpointName").build();
        endpointGroup.setEndpoints(singleton(endpoint));
        proxy.setGroups(singleton(endpointGroup));
        proxy.setVirtualHosts(singletonList(new VirtualHost("/context")));
        api.setProxy(proxy);
        api.setVersion("1.0");
        api.setName("tag test");
        api.setDescription("tag test example");
        api.setTags(Set.of("unit-tests"));

        doThrow(TagNotFoundException.class)
            .when(tagService)
            .checkTagsExist(Set.of("unit-tests"), GraviteeContext.getCurrentEnvironment(), TagReferenceType.ORGANIZATION);

        assertThrows(
            TagNotFoundException.class,
            () -> apiService.createWithApiDefinition(GraviteeContext.getExecutionContext(), api, USERNAME, definition)
        );
        verify(apiRepository, never()).create(any());
        verify(alertService, never()).createDefaults(any(ExecutionContext.class), eq(AlertReferenceType.API), any());
    }

    @Test
    public void should_create_if_tag_exists() throws Exception {
        JsonNode definition = readDefinition("/io/gravitee/rest/api/management/service/import-api.definition+plans.json");
        UpdateApiEntity api = new UpdateApiEntity();
        Proxy proxy = new Proxy();
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("endpointGroupName");
        Endpoint endpoint = Endpoint.builder().name("endpointName").build();
        endpointGroup.setEndpoints(singleton(endpoint));
        proxy.setGroups(singleton(endpointGroup));
        proxy.setVirtualHosts(singletonList(new VirtualHost("/context")));
        api.setProxy(proxy);
        api.setVersion("1.0");
        api.setName("tag test basic");
        api.setDescription("tag test basic example");
        api.setTags(Set.of("unit-tests"));

        when(primaryOwnerService.getPrimaryOwner(any(), any(), any())).thenReturn(new PrimaryOwnerEntity(new UserEntity()));

        Api updateApiDefinition = new Api();
        when(objectMapper.readValue(any(String.class), eq(Api.class))).thenReturn(updateApiDefinition);

        when(apiRepository.create(any())).thenReturn(new io.gravitee.repository.management.model.Api());

        when(apiConverter.toApiEntity(any(), any(), any(), anyBoolean())).thenReturn(new ApiEntity());

        when(apiMetadataService.fetchMetadataForApi(any(), any())).thenReturn(new ApiEntity());

        when(tagService.findByUser(USERNAME, GraviteeContext.getCurrentEnvironment(), TagReferenceType.ORGANIZATION))
            .thenReturn(Set.of("a-tag", "unit-tests"));

        apiService.createWithApiDefinition(GraviteeContext.getExecutionContext(), api, USERNAME, definition);

        verify(tagService, times(1))
            .checkTagsExist(Set.of("unit-tests"), GraviteeContext.getCurrentEnvironment(), TagReferenceType.ORGANIZATION);

        verify(apiRepository, times(1)).create(argThat(arg -> arg.getId().equals(definition.get("id").asText())));
        verify(alertService, times(1)).createDefaults(any(ExecutionContext.class), eq(AlertReferenceType.API), any());
    }

    @Test
    public void should_not_create_if_user_does_not_have_associated_tags() throws Exception {
        JsonNode definition = readDefinition("/io/gravitee/rest/api/management/service/import-api.definition+plans.json");
        UpdateApiEntity api = new UpdateApiEntity();
        Proxy proxy = new Proxy();
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("endpointGroupName");
        Endpoint endpoint = Endpoint.builder().name("endpointName").build();
        endpointGroup.setEndpoints(singleton(endpoint));
        proxy.setGroups(singleton(endpointGroup));
        proxy.setVirtualHosts(singletonList(new VirtualHost("/context")));
        api.setProxy(proxy);
        api.setVersion("1.0");
        api.setName("tag test basic");
        api.setDescription("tag test basic example");
        api.setTags(Set.of("unit-tests"));

        when(tagService.findByUser(USERNAME, GraviteeContext.getCurrentEnvironment(), TagReferenceType.ORGANIZATION))
            .thenReturn(Set.of("a-tag"));

        assertThrows(
            TagNotAllowedException.class,
            () -> apiService.createWithApiDefinition(GraviteeContext.getExecutionContext(), api, USERNAME, definition)
        );
        verify(apiRepository, never()).create(any());
        verify(alertService, never()).createDefaults(any(ExecutionContext.class), eq(AlertReferenceType.API), any());
    }

    private JsonNode readDefinition(String resourcePath) throws Exception {
        InputStream resourceAsStream = getClass().getResourceAsStream(resourcePath);
        return MAPPER.readTree(resourceAsStream);
    }

    @Test(expected = EndpointNameAlreadyExistsException.class)
    public void shouldNotCreateApiBecauseOfEndpointGroupAndInnerEndpointHaveSameName() throws TechnicalException {
        Endpoint endpoint = Endpoint.builder().name("endpointGroupName").build();
        EndpointGroup endpointGroup = EndpointGroup.builder().name("endpointGroupName").endpoints(singleton(endpoint)).build();
        Proxy proxy = Proxy.builder().groups(singleton(endpointGroup)).virtualHosts(singletonList(new VirtualHost("/context"))).build();

        UpdateApiEntity api = new UpdateApiEntity();
        api.setProxy(proxy);
        api.setVersion("1.0");
        api.setName("tag test basic");
        api.setDescription("tag test basic example");

        apiService.createWithApiDefinition(GraviteeContext.getExecutionContext(), api, USERNAME, null);
    }

    @Test(expected = EndpointGroupNameAlreadyExistsException.class)
    public void shouldNotCreateApiBecauseOfEndpointGroupAndEndpointOfAnotherGroupHaveSameName() throws TechnicalException {
        Endpoint endpoint = Endpoint.builder().name("endpointName").build();
        EndpointGroup endpointGroup = EndpointGroup.builder().name("endpointGroupName").endpoints(singleton(endpoint)).build();
        Endpoint endpoint2 = Endpoint.builder().name("endpointGroupName").build();
        EndpointGroup endpointGroup2 = EndpointGroup.builder().name("endpointName").endpoints(singleton(endpoint2)).build();
        Proxy proxy = Proxy
            .builder()
            .groups(Set.of(endpointGroup, endpointGroup2))
            .virtualHosts(singletonList(new VirtualHost("/context")))
            .build();

        UpdateApiEntity api = new UpdateApiEntity();
        api.setProxy(proxy);
        api.setVersion("1.0");
        api.setName("tag test basic");
        api.setDescription("tag test basic example");

        apiService.createWithApiDefinition(GraviteeContext.getExecutionContext(), api, USERNAME, null);
    }
}
