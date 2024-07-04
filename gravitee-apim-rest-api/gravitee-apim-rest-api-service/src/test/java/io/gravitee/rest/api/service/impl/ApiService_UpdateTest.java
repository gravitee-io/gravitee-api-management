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

import static io.gravitee.rest.api.model.WorkflowReferenceType.API;
import static io.gravitee.rest.api.model.WorkflowType.REVIEW;
import static io.gravitee.rest.api.model.api.ApiLifecycleState.ARCHIVED;
import static io.gravitee.rest.api.model.api.ApiLifecycleState.CREATED;
import static io.gravitee.rest.api.model.api.ApiLifecycleState.DEPRECATED;
import static io.gravitee.rest.api.model.api.ApiLifecycleState.PUBLISHED;
import static io.gravitee.rest.api.model.api.ApiLifecycleState.UNPUBLISHED;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.collections.Sets.newSet;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import io.gravitee.apim.core.api.domain_service.VerifyApiPathDomainService;
import io.gravitee.common.component.Lifecycle;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.EndpointGroup;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.definition.model.Policy;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.Rule;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.plugins.resources.Resource;
import io.gravitee.definition.model.services.Services;
import io.gravitee.definition.model.services.healthcheck.HealthCheckService;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.Workflow;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.idp.api.authentication.UserDetails;
import io.gravitee.rest.api.model.GroupEntity;
import io.gravitee.rest.api.model.MembershipEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PlanStatus;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.ReviewEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.WorkflowState;
import io.gravitee.rest.api.model.api.*;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.ApiPermission;
import io.gravitee.rest.api.model.permissions.RolePermissionAction;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.builder.EmailNotificationBuilder;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.CategoryMapper;
import io.gravitee.rest.api.service.exceptions.*;
import io.gravitee.rest.api.service.jackson.filter.ApiPermissionFilter;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.v4.ApiCategoryService;
import io.gravitee.rest.api.service.v4.ApiEntrypointService;
import io.gravitee.rest.api.service.v4.ApiNotificationService;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import io.gravitee.rest.api.service.v4.validation.AnalyticsValidationService;
import io.gravitee.rest.api.service.v4.validation.CorsValidationService;
import io.gravitee.rest.api.service.v4.validation.TagsValidationService;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.internal.util.collections.Sets;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiService_UpdateTest {

    public static final String API_DEFINITION =
        "{\n" +
        "  \"id\" : \"id-api\",\n" +
        "  \"name\" : \"myAPI\",\n" +
        "  \"description\" : \"Gravitee.io\",\n" +
        "  \"paths\" : { },\n" +
        "  \"path_mappings\":[],\n" +
        "  \"proxy\": {\n" +
        "    \"virtual_hosts\": [{\n" +
        "      \"path\": \"/test\"\n" +
        "    }],\n" +
        "    \"strip_context_path\": false,\n" +
        "    \"preserve_host\":false,\n" +
        "    \"logging\": {\n" +
        "      \"mode\":\"CLIENT_PROXY\",\n" +
        "      \"condition\":\"condition\"\n" +
        "    },\n" +
        "    \"groups\": [\n" +
        "      {\n" +
        "        \"name\": \"default-group\",\n" +
        "        \"endpoints\": [\n" +
        "          {\n" +
        "            \"name\": \"default\",\n" +
        "            \"target\": \"http://test\",\n" +
        "            \"weight\": 1,\n" +
        "            \"backup\": false,\n" +
        "            \"type\": \"HTTP\",\n" +
        "            \"http\": {\n" +
        "              \"connectTimeout\": 5000,\n" +
        "              \"idleTimeout\": 60000,\n" +
        "              \"keepAliveTimeout\": 30000,\n" +
        "              \"keepAlive\": true,\n" +
        "              \"readTimeout\": 10000,\n" +
        "              \"pipelining\": false,\n" +
        "              \"maxConcurrentConnections\": 100,\n" +
        "              \"useCompression\": true,\n" +
        "              \"followRedirects\": false,\n" +
        "              \"encodeURI\":false\n" +
        "            }\n" +
        "          }\n" +
        "        ],\n" +
        "        \"load_balancing\": {\n" +
        "          \"type\": \"ROUND_ROBIN\"\n" +
        "        },\n" +
        "        \"http\": {\n" +
        "          \"connectTimeout\": 5000,\n" +
        "          \"idleTimeout\": 60000,\n" +
        "          \"keepAliveTimeout\": 30000,\n" +
        "          \"keepAlive\": true,\n" +
        "          \"readTimeout\": 10000,\n" +
        "          \"pipelining\": false,\n" +
        "          \"maxConcurrentConnections\": 100,\n" +
        "          \"useCompression\": true,\n" +
        "          \"followRedirects\": false,\n" +
        "          \"encodeURI\":false\n" +
        "        }\n" +
        "      }\n" +
        "    ]\n" +
        "  }\n" +
        "}\n";
    private static final String API_ID = "id-api";
    private static final String DEFAULT_ENVIRONMENT_ID = "DEFAULT";
    private static final String API_NAME = "myAPI";
    private static final String USER_NAME = "myUser";

    @InjectMocks
    private ApiServiceImpl apiService = new ApiServiceImpl();

    @Mock
    private ApiEntrypointService apiEntrypointService;

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private MembershipService membershipService;

    @Mock
    private RoleService roleService;

    @Spy
    private ObjectMapper objectMapper = new GraviteeMapper();

    private UpdateApiEntity updateApiEntity;

    private Api api;
    private Api updatedApi;

    @Mock
    private UserService userService;

    @Mock
    private AuditService auditService;

    @Mock
    private SearchEngineService searchEngineService;

    @Mock
    private TagService tagService;

    @Mock
    private ParameterService parameterService;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private VerifyApiPathDomainService verifyApiPathDomainService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private ApiCategoryService apiCategoryService;

    @Mock
    private NotifierService notifierService;

    @Mock
    private PolicyService policyService;

    @Mock
    private ResourceService resourceService;

    @Mock
    private GroupService groupService;

    @Mock
    private ApiMetadataService apiMetadataService;

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationTemplateService notificationTemplateService;

    @Mock
    private ConnectorService connectorService;

    @Mock
    private PlanService planService;

    @Mock
    private FlowService flowService;

    @Mock
    private PrimaryOwnerService primaryOwnerService;

    @Spy
    private CategoryMapper categoryMapper = new CategoryMapper(mock(CategoryService.class));

    @InjectMocks
    private ApiConverter apiConverter = Mockito.spy(
        new ApiConverter(objectMapper, planService, flowService, categoryMapper, parameterService, workflowService)
    );

    @Mock
    private ApiNotificationService apiNotificationService;

    @Mock
    private CorsValidationService corsValidationService;

    @Mock
    private AnalyticsValidationService loggingValidationService;

    @Mock
    private TagsValidationService tagsValidationService;

    @Mock
    private PageService pageService;

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
    public void setUp() {
        PropertyFilter apiMembershipTypeFilter = new ApiPermissionFilter();
        MockitoAnnotations.openMocks(this);
        objectMapper.setFilterProvider(
            new SimpleFilterProvider(Collections.singletonMap("apiMembershipTypeFilter", apiMembershipTypeFilter))
        );
        GraviteeContext.setCurrentEnvironment(DEFAULT_ENVIRONMENT_ID);

        final SecurityContext securityContext = mock(SecurityContext.class);
        final Authentication authentication = mock(Authentication.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(new UserDetails("username", "", emptyList()));
        SecurityContextHolder.setContext(securityContext);
        when(userService.findById(eq(GraviteeContext.getExecutionContext()), any())).thenReturn(new UserEntity());

        api = new Api();
        api.setId(API_ID);
        api.setDefinition("{\"id\": \"" + API_ID + "\",\"name\": \"" + API_NAME + "\",\"proxy\": {\"context_path\": \"/old\"}}");
        api.setEnvironmentId(DEFAULT_ENVIRONMENT_ID);

        updateApiEntity = new UpdateApiEntity();
        updateApiEntity.setVersion("v1");
        updateApiEntity.setName(API_NAME);
        updateApiEntity.setDescription("Ma description");

        updatedApi = new Api();
        updatedApi.setId(API_ID);
        updatedApi.setName(API_NAME);
        updatedApi.setEnvironmentId(DEFAULT_ENVIRONMENT_ID);

        when(primaryOwnerService.getPrimaryOwner(any(), any())).thenReturn(new PrimaryOwnerEntity(new UserEntity()));
        reset(searchEngineService);
        when(verifyApiPathDomainService.checkAndSanitizeApiPaths(any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(2));
        when(apiMetadataService.fetchMetadataForApi(any(ExecutionContext.class), any(ApiEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(1));
    }

    @After
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @Test(expected = ApiDefinitionVersionNotSupportedException.class)
    public void shouldNotUpdateWithOldDefinitionVersion() throws TechnicalException {
        String apiId = "outdatedApi";
        Api apiToUpdate = new Api();
        apiToUpdate.setDefinitionVersion(DefinitionVersion.V1);
        when(apiRepository.findById(apiId)).thenReturn(Optional.of(apiToUpdate));

        UpdateApiEntity payload = new UpdateApiEntity();
        apiService.update(GraviteeContext.getExecutionContext(), apiId, payload);
    }

    @Test
    public void shouldUpdate() throws TechnicalException {
        prepareUpdate();

        final ApiEntity apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);

        assertNotNull(apiEntity);
        assertEquals(API_NAME, apiEntity.getName());

        // Picture management as a dedicated service, so we should reuse the same picture as the one saved
        verify(apiRepository)
            .update(
                argThat(apiToUpdate ->
                    Objects.equals(apiToUpdate.getPicture(), api.getPicture()) &&
                    Objects.equals(apiToUpdate.getBackground(), api.getBackground())
                )
            );
        verify(searchEngineService, times(1)).index(eq(GraviteeContext.getExecutionContext()), any(), eq(false));
    }

    @Test
    public void shouldUpdatePlans() throws TechnicalException {
        prepareUpdate();

        PlanEntity plan1 = new PlanEntity();
        plan1.setId("plan1");
        PlanEntity plan2 = new PlanEntity();
        plan2.setId("plan2");
        updateApiEntity.setPlans(Set.of(plan1, plan2));

        apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);

        verify(planService, times(1)).createOrUpdatePlan(any(), same(plan1));
        verify(planService, times(1)).createOrUpdatePlan(any(), same(plan2));
    }

    @Test
    public void shouldUpdateFlows() throws TechnicalException {
        prepareUpdate();

        List<Flow> apiFlows = List.of(mock(Flow.class), mock(Flow.class));
        updateApiEntity.setFlows(apiFlows);

        apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);

        verify(flowService, times(1)).save(FlowReferenceType.API, API_ID, apiFlows);
    }

    @Test(expected = ApiNotFoundException.class)
    public void shouldNotUpdateBecauseNotFound() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.empty());

        apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotUpdateBecauseTechnicalException() throws TechnicalException {
        updateApiEntity.setVersion("v1");
        updateApiEntity.setName(API_NAME);
        updateApiEntity.setDescription("Ma description");
        final Proxy proxy = new Proxy();
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("endpointGroupName");
        Endpoint endpoint = Endpoint.builder().name("endpointName").build();
        endpointGroup.setEndpoints(singleton(endpoint));
        proxy.setGroups(singleton(endpointGroup));
        updateApiEntity.setProxy(proxy);
        proxy.setVirtualHosts(Collections.singletonList(new VirtualHost("/context")));
        updateApiEntity.setLifecycleState(CREATED);

        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        api.setApiLifecycleState(ApiLifecycleState.CREATED);
        when(apiRepository.update(any())).thenThrow(TechnicalException.class);

        apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);
    }

    @Test(expected = EndpointNameInvalidException.class)
    public void shouldNotUpdateWithInvalidEndpointGroupName() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        final Proxy proxy = new Proxy();
        updateApiEntity.setProxy(proxy);
        proxy.setVirtualHosts(Collections.singletonList(new VirtualHost("/new")));
        final EndpointGroup group = new EndpointGroup();
        group.setName("inva:lid");
        proxy.setGroups(singleton(group));
        group.setEndpoints(singleton(mock(Endpoint.class)));

        apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);

        fail("should throw EndpointNameInvalidException");
    }

    @Test(expected = EndpointNameInvalidException.class)
    public void shouldNotUpdateWithInvalidEndpointName() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        Endpoint endpoint = mock(Endpoint.class);
        when(endpoint.getName()).thenReturn("inva:lid");

        final EndpointGroup group = new EndpointGroup();
        group.setName("group");
        group.setEndpoints(singleton(endpoint));

        final Proxy proxy = new Proxy();
        proxy.setVirtualHosts(Collections.singletonList(new VirtualHost("/new")));
        proxy.setGroups(singleton(group));
        updateApiEntity.setProxy(proxy);

        apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);

        fail("should throw EndpointNameInvalidException");
    }

    @Test(expected = EndpointNameAlreadyExistsException.class)
    public void shouldNotCreateApiBecauseOfEndpointGroupAndInnerEndpointHaveSameName() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        Endpoint endpoint = Endpoint.builder().name("endpointGroupName").build();
        EndpointGroup endpointGroup = EndpointGroup.builder().name("endpointGroupName").endpoints(singleton(endpoint)).build();
        Proxy proxy = Proxy.builder().groups(singleton(endpointGroup)).virtualHosts(singletonList(new VirtualHost("/context"))).build();

        UpdateApiEntity api = new UpdateApiEntity();
        api.setProxy(proxy);
        api.setVersion("1.0");
        api.setName("tag test basic");
        api.setDescription("tag test basic example");

        apiService.update(GraviteeContext.getExecutionContext(), API_ID, api);
    }

    @Test(expected = EndpointGroupNameAlreadyExistsException.class)
    public void shouldNotCreateApiBecauseOfEndpointGroupAndEndpointOfAnotherGroupHaveSameName() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

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

        apiService.update(GraviteeContext.getExecutionContext(), API_ID, api);
    }

    @Test(expected = InvalidDataException.class)
    public void shouldNotUpdateWithInvalidPolicyConfiguration() throws TechnicalException {
        prepareUpdate();

        HashMap<String, List<Rule>> paths = new HashMap<>();
        ArrayList<Rule> rules = new ArrayList<>();
        Rule rule = new Rule();
        Policy policy = new Policy();
        rule.setPolicy(policy);
        rule.setEnabled(true);
        rules.add(rule);
        paths.put("/", rules);

        updateApiEntity.setPaths(paths);
        doThrow(new InvalidDataException()).when(policyService).validatePolicyConfiguration(any(Policy.class));

        apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);

        fail("should throw InvalidDataException");
    }

    @Test(expected = InvalidDataException.class)
    public void shouldNotUpdateWithPOGroups() throws TechnicalException {
        prepareUpdate();

        Set<String> groups = Sets.newSet("group-with-po");
        updateApiEntity.setGroups(groups);

        GroupEntity poGroup = new GroupEntity();
        poGroup.setId("group-with-po");
        poGroup.setApiPrimaryOwner("a-api-po-user");
        Set<GroupEntity> groupEntitySet = Sets.newSet(poGroup);
        //        when(groupService.findByIds(groups)).thenReturn(groupEntitySet);
        when(groupService.findByIds(groups)).thenThrow(GroupsNotFoundException.class);

        apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);

        fail("should throw InvalidDataException");
    }

    private void prepareUpdate(DefinitionVersion existingAPIDefinitionVersion, DefinitionVersion updateAPIDefinitionVersion)
        throws TechnicalException {
        prepareUpdate();
        if (existingAPIDefinitionVersion != null) {
            api.setDefinition(
                "{\"id\": \"" +
                API_ID +
                "\",\"name\": \"" +
                API_NAME +
                "\",\"gravitee\": \"" +
                existingAPIDefinitionVersion.getLabel() +
                "\",\"proxy\": {\"context_path\": \"/old\"}}"
            );
        }
        if (updateAPIDefinitionVersion != null) {
            updateApiEntity.setGraviteeDefinitionVersion(updateAPIDefinitionVersion.getLabel());
        }
    }

    @Test
    public void shouldUpdateWithPOGroupsWhenGroupsAreNotProvided() throws TechnicalException {
        prepareUpdate();

        PrimaryOwnerEntity po = mock(PrimaryOwnerEntity.class);
        when(po.getType()).thenReturn(MembershipMemberType.GROUP.name());
        when(po.getId()).thenReturn("group-with-po");
        when(primaryOwnerService.getPrimaryOwner(any(), eq(API_ID))).thenReturn(po);

        updateApiEntity.setGroups(Sets.newSet());

        final ApiEntity apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);

        assertNotNull(apiEntity);
        assertEquals(API_NAME, apiEntity.getName());
        verify(apiRepository).update(argThat(api -> api.getId().equals(API_ID) && api.getGroups().equals(Sets.newSet("group-with-po"))));
    }

    private void prepareUpdate() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(apiRepository.update(any())).thenReturn(updatedApi);

        api.setName(API_NAME);
        api.setApiLifecycleState(ApiLifecycleState.CREATED);
        api.setPicture("picture");
        api.setBackground("background");
        api.setVersion("2.0.0");

        updateApiEntity.setName(API_NAME);
        updateApiEntity.setVersion("v1");
        updateApiEntity.setDescription("Ma description");

        final Proxy proxy = new Proxy();
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("endpointGroupName");
        Endpoint endpoint = Endpoint.builder().name("endpointName").build();
        endpointGroup.setEndpoints(singleton(endpoint));
        proxy.setGroups(singleton(endpointGroup));
        updateApiEntity.setProxy(proxy);
        updateApiEntity.setLifecycleState(CREATED);
        proxy.setVirtualHosts(Collections.singletonList(new VirtualHost("/context")));
    }

    @Test
    public void shouldUpdateWithAllowedTag() throws TechnicalException {
        prepareUpdate();
        updateApiEntity.setTags(singleton("public"));
        api.setDefinition(
            "{\"id\": \"" + API_ID + "\",\"name\": \"" + API_NAME + "\",\"proxy\": {\"context_path\": \"/old\"} ,\"tags\": [\"public\"]}"
        );
        final ApiEntity apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);
        assertNotNull(apiEntity);
        assertEquals(API_NAME, apiEntity.getName());
        verify(searchEngineService, times(1)).index(eq(GraviteeContext.getExecutionContext()), any(), eq(false));
    }

    @Test
    public void shouldNotUpdateWithRemovedTagPresentInPlan() throws TechnicalException {
        prepareUpdate();

        PlanEntity updatedPlan = new PlanEntity();
        updatedPlan.setId("Plan");
        updatedPlan.setName("Plan Name");
        updatedPlan.setTags(Set.of("plan tag"));
        updatedPlan.setStatus(PlanStatus.PUBLISHED);
        updateApiEntity.setPlans(Set.of(updatedPlan));
        updateApiEntity.setTags(singleton("public"));

        PlanEntity originalPlan = new PlanEntity();
        originalPlan.setId("Plan");
        originalPlan.setStatus(PlanStatus.PUBLISHED);
        when(planService.findByApi(any(), eq(API_ID))).thenReturn(Set.of(originalPlan));

        api.setDefinition(
            "{\"id\": \"" + API_ID + "\", \"gravitee\": \"2.0.0\", \"name\": \"" + API_NAME + "\",\"proxy\": {\"context_path\": \"/old\"}}"
        );

        when(tagService.findByUser(any(), any(), any())).thenReturn(Set.of("public"));

        doThrow(new TagNotAllowedException(new String[0]))
            .when(tagsValidationService)
            .validatePlanTagsAgainstApiTags(any(Set.class), any(Set.class));

        assertThatThrownBy(() -> apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity, true))
            .isInstanceOf(InvalidDataException.class);

        verify(apiRepository, never()).update(any());
    }

    @Test
    public void shouldNotDuplicateLabels() throws TechnicalException {
        prepareUpdate();
        updateApiEntity.setLabels(asList("label1", "label1"));
        api.setDefinition(
            "{\"id\": \"" + API_ID + "\",\"name\": \"" + API_NAME + "\",\"proxy\": {\"context_path\": \"/old\"} ,\"labels\": [\"public\"]}"
        );
        final ApiEntity apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);
        verify(apiRepository).update(argThat(api -> api.getLabels().size() == 1));
        assertNotNull(apiEntity);
        verify(searchEngineService, times(1)).index(eq(GraviteeContext.getExecutionContext()), any(), eq(false));
    }

    @Test
    public void shouldUpdateWithExistingAllowedTag() throws TechnicalException {
        prepareUpdate();
        updateApiEntity.setTags(singleton("private"));
        Proxy proxy = new Proxy();
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("endpointGroupName");
        Endpoint endpoint = Endpoint.builder().name("endpointName").build();
        endpointGroup.setEndpoints(singleton(endpoint));
        proxy.setGroups(singleton(endpointGroup));
        updateApiEntity.setProxy(proxy);
        api.setDefinition(
            "{\"id\": \"" + API_ID + "\",\"name\": \"" + API_NAME + "\",\"proxy\": {\"context_path\": \"/old\"} ,\"tags\": [\"public\"]}"
        );
        proxy.setVirtualHosts(Collections.singletonList(new VirtualHost("/context")));
        when(verifyApiPathDomainService.checkAndSanitizeApiPaths(any(), any(), any())).thenAnswer(invocation -> invocation.getArgument(2));
        when(tagService.findByUser(any(), any(), any())).thenReturn(Sets.newSet("public", "private"));
        final ApiEntity apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);
        assertNotNull(apiEntity);
        assertEquals(API_NAME, apiEntity.getName());
        verify(searchEngineService, times(1)).index(eq(GraviteeContext.getExecutionContext()), any(), eq(false));
        verify(apiNotificationService, times(1)).triggerUpdateNotification(GraviteeContext.getExecutionContext(), apiEntity);
    }

    @Test
    public void shouldUpdateWithExistingAllowedTags() throws TechnicalException {
        prepareUpdate();
        updateApiEntity.setTags(newSet("public", "private"));
        Proxy proxy = new Proxy();
        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName("endpointGroupName");
        Endpoint endpoint = Endpoint.builder().name("endpointName").build();
        endpointGroup.setEndpoints(singleton(endpoint));
        proxy.setGroups(singleton(endpointGroup));
        updateApiEntity.setProxy(proxy);
        api.setDefinition(
            "{\"id\": \"" + API_ID + "\",\"name\": \"" + API_NAME + "\",\"proxy\": {\"context_path\": \"/old\"} ,\"tags\": [\"public\"]}"
        );
        proxy.setVirtualHosts(Collections.singletonList(new VirtualHost("/context")));
        when(tagService.findByUser(any(), any(), any())).thenReturn(Sets.newSet("public", "private"));
        final ApiEntity apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);
        assertNotNull(apiEntity);
        assertEquals(API_NAME, apiEntity.getName());
        verify(searchEngineService, times(1)).index(eq(GraviteeContext.getExecutionContext()), any(), eq(false));
        verify(apiNotificationService, times(1)).triggerUpdateNotification(eq(GraviteeContext.getExecutionContext()), eq(apiEntity));
    }

    @Test
    public void shouldUpdateWithExistingNotAllowedTag() throws TechnicalException {
        prepareUpdate();
        updateApiEntity.setTags(newSet("public", "private"));
        api.setDefinition(
            "{\"id\": \"" +
            API_ID +
            "\",\"name\": \"" +
            API_NAME +
            "\",\"proxy\": {\"context_path\": \"/old\"} ,\"tags\": [\"public\", \"private\"]}"
        );
        final ApiEntity apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);
        assertNotNull(apiEntity);
        assertEquals(API_NAME, apiEntity.getName());
        verify(searchEngineService, times(1)).index(eq(GraviteeContext.getExecutionContext()), any(), eq(false));
        verify(apiNotificationService, times(1)).triggerUpdateNotification(eq(GraviteeContext.getExecutionContext()), eq(apiEntity));
    }

    @Test(expected = InvalidDataException.class)
    public void shouldNotUpdate_NewPlanNotAllowed() throws TechnicalException {
        prepareUpdate();
        PlanEntity plan = new PlanEntity();
        plan.setName("Plan Malicious");
        plan.setStatus(PlanStatus.PUBLISHED);
        updateApiEntity.setPlans(Set.of(plan));

        api.setDefinition("{\"id\": \"" + API_ID + "\",\"name\": \"" + API_NAME + "\",\"proxy\": {\"context_path\": \"/old\"}}");
        ApiEntity apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity, true);
        verify(apiNotificationService, times(1)).triggerUpdateNotification(eq(GraviteeContext.getExecutionContext()), eq(apiEntity));
    }

    @Test(expected = InvalidDataException.class)
    public void shouldNotUpdate_PlanClosed() throws TechnicalException {
        prepareUpdate();

        PlanEntity updatedPlan = new PlanEntity();
        updatedPlan.setId("MALICIOUS");
        updatedPlan.setName("Plan Malicious");
        updatedPlan.setStatus(PlanStatus.PUBLISHED);
        updateApiEntity.setPlans(Set.of(updatedPlan));

        PlanEntity originalPlan = new PlanEntity();
        originalPlan.setId("MALICIOUS");
        originalPlan.setStatus(PlanStatus.CLOSED);
        when(planService.findByApi(any(), eq(API_ID))).thenReturn(Set.of(originalPlan));

        api.setDefinition(
            "{\"id\": \"" + API_ID + "\", \"gravitee\": \"2.0.0\", \"name\": \"" + API_NAME + "\",\"proxy\": {\"context_path\": \"/old\"}}"
        );
        ApiEntity apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity, true);
        verify(apiNotificationService, times(1)).triggerUpdateNotification(eq(GraviteeContext.getExecutionContext()), eq(apiEntity));
    }

    @Test
    public void shouldUpdate_PlanStatusNotChanged() throws TechnicalException {
        prepareUpdate();

        PlanEntity updatedPlan = new PlanEntity();
        updatedPlan.setId("MALICIOUS");
        updatedPlan.setName("Plan Malicious");
        updatedPlan.setStatus(PlanStatus.CLOSED);
        updateApiEntity.setPlans(Set.of(updatedPlan));

        PlanEntity originalPlan = new PlanEntity();
        originalPlan.setId("MALICIOUS");
        originalPlan.setStatus(PlanStatus.CLOSED);
        when(planService.findByApi(any(), eq(API_ID))).thenReturn(Set.of(originalPlan));

        api.setDefinition(
            "{\"id\": \"" + API_ID + "\", \"gravitee\": \"2.0.0\", \"name\": \"" + API_NAME + "\",\"proxy\": {\"context_path\": \"/old\"}}"
        );
        ApiEntity apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity, true);
        verify(apiNotificationService, times(1)).triggerUpdateNotification(eq(GraviteeContext.getExecutionContext()), eq(apiEntity));
    }

    @Test
    public void shouldUpdate_PlanStatusChanged_authorized() throws TechnicalException {
        prepareUpdate();

        PlanEntity updatedPlan = new PlanEntity();
        updatedPlan.setId("VALID");
        updatedPlan.setName("Plan VALID");
        updatedPlan.setStatus(PlanStatus.CLOSED);
        updateApiEntity.setPlans(Set.of(updatedPlan));

        PlanEntity originalPlan = new PlanEntity();
        originalPlan.setId("VALID");
        originalPlan.setStatus(PlanStatus.PUBLISHED);
        when(planService.findByApi(any(), eq(API_ID))).thenReturn(Set.of(originalPlan));

        api.setDefinition(
            "{\"id\": \"" + API_ID + "\", \"gravitee\": \"2.0.0\", \"name\": \"" + API_NAME + "\",\"proxy\": {\"context_path\": \"/old\"}}"
        );
        ApiEntity apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity, true);
        verify(apiNotificationService, times(1)).triggerUpdateNotification(eq(GraviteeContext.getExecutionContext()), eq(apiEntity));
    }

    @Test(expected = TagNotAllowedException.class)
    public void shouldNotUpdateWithNotAllowedTag() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        api.setDefinition(
            "{\"id\": \"" + API_ID + "\",\"name\": \"" + API_NAME + "\",\"proxy\": {\"context_path\": \"/old\"} ,\"tags\": [\"public\"]}"
        );
        updateApiEntity.setTags(singleton("private"));
        final Proxy proxy = new Proxy();
        EndpointGroup endpointGroup = new EndpointGroup();
        Endpoint endpoint = Endpoint.builder().name("default").build();
        endpointGroup.setEndpoints(singleton(endpoint));
        proxy.setGroups(singleton(endpointGroup));
        updateApiEntity.setProxy(proxy);
        proxy.setVirtualHosts(Collections.singletonList(new VirtualHost("/context")));
        when(tagService.findByUser(any(), any(), any())).thenReturn(emptySet());
        ApiEntity apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);
        verify(apiNotificationService, times(1)).triggerUpdateNotification(eq(GraviteeContext.getExecutionContext()), eq(apiEntity));
    }

    @Test(expected = TagNotAllowedException.class)
    public void shouldNotUpdateWithExistingNotAllowedTag() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        api.setDefinition(
            "{\"id\": \"" + API_ID + "\",\"name\": \"" + API_NAME + "\",\"proxy\": {\"context_path\": \"/old\"} ,\"tags\": [\"public\"]}"
        );
        updateApiEntity.setTags(singleton("private"));
        final Proxy proxy = new Proxy();
        EndpointGroup endpointGroup = new EndpointGroup();
        Endpoint endpoint = Endpoint.builder().name("default").build();
        endpointGroup.setEndpoints(singleton(endpoint));
        proxy.setGroups(singleton(endpointGroup));
        updateApiEntity.setProxy(proxy);
        proxy.setVirtualHosts(Collections.singletonList(new VirtualHost("/context")));
        when(tagService.findByUser(any(), any(), any())).thenReturn(singleton("public"));
        ApiEntity apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);
        verify(apiNotificationService, times(1)).triggerUpdateNotification(eq(GraviteeContext.getExecutionContext()), eq(apiEntity));
    }

    @Test(expected = TagNotAllowedException.class)
    public void shouldNotUpdateWithExistingNotAllowedTags() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        api.setDefinition(
            "{\"id\": \"" +
            API_ID +
            "\",\"name\": \"" +
            API_NAME +
            "\",\"proxy\": {\"context_path\": \"/old\"} ,\"tags\": [\"public\", \"private\"]}"
        );
        updateApiEntity.setTags(emptySet());

        final Proxy proxy = new Proxy();
        EndpointGroup endpointGroup = new EndpointGroup();
        Endpoint endpoint = Endpoint.builder().name("default").build();
        endpointGroup.setEndpoints(singleton(endpoint));
        proxy.setGroups(singleton(endpointGroup));
        proxy.setVirtualHosts(Collections.singletonList(new VirtualHost("/context")));

        updateApiEntity.setProxy(proxy);
        when(tagService.findByUser(any(), any(), any())).thenReturn(singleton("private"));
        ApiEntity apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);
        verify(apiNotificationService, times(1)).triggerUpdateNotification(eq(GraviteeContext.getExecutionContext()), eq(apiEntity));
    }

    @Test(expected = InvalidDataException.class)
    public void shouldNotUpdateWithInvalidSchedule() throws TechnicalException {
        prepareUpdate();
        Services services = new Services();
        HealthCheckService healthCheckService = mock(HealthCheckService.class);
        when(healthCheckService.getSchedule()).thenReturn("**");
        services.put(HealthCheckService.class, healthCheckService);
        updateApiEntity.setServices(services);
        final ApiEntity apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);

        assertNotNull(apiEntity);
        assertEquals(API_NAME, apiEntity.getName());
        verify(apiNotificationService, times(1)).triggerUpdateNotification(eq(GraviteeContext.getExecutionContext()), eq(apiEntity));
    }

    @Test
    public void shouldUpdateWithValidSchedule() throws TechnicalException {
        prepareUpdate();
        Services services = new Services();
        HealthCheckService healthCheckService = new HealthCheckService();
        healthCheckService.setSchedule("1,2 */100 5-8 * * *");
        services.put(HealthCheckService.class, healthCheckService);
        updateApiEntity.setServices(services);
        final ApiEntity apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);

        assertNotNull(apiEntity);
        assertEquals(API_NAME, apiEntity.getName());
        verify(searchEngineService, times(1)).index(eq(GraviteeContext.getExecutionContext()), any(), eq(false));
        verify(apiNotificationService, times(1)).triggerUpdateNotification(eq(GraviteeContext.getExecutionContext()), eq(apiEntity));
    }

    @Test
    public void shouldPublishApi() throws TechnicalException {
        prepareUpdate();
        // from UNPUBLISHED state
        api.setApiLifecycleState(ApiLifecycleState.UNPUBLISHED);
        updateApiEntity.setLifecycleState(PUBLISHED);
        updatedApi.setApiLifecycleState(ApiLifecycleState.PUBLISHED);
        ApiEntity apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);
        assertNotNull(apiEntity);
        assertEquals(io.gravitee.rest.api.model.api.ApiLifecycleState.PUBLISHED, apiEntity.getLifecycleState());

        verify(apiRepository).update(argThat(api -> api.getApiLifecycleState().equals(ApiLifecycleState.PUBLISHED)));
        clearInvocations(apiRepository);

        // from CREATED state
        api.setApiLifecycleState(ApiLifecycleState.CREATED);
        updateApiEntity.setLifecycleState(PUBLISHED);
        updatedApi.setApiLifecycleState(ApiLifecycleState.PUBLISHED);

        apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);
        assertNotNull(apiEntity);
        assertEquals(io.gravitee.rest.api.model.api.ApiLifecycleState.PUBLISHED, apiEntity.getLifecycleState());
        verify(apiRepository).update(argThat(api -> api.getApiLifecycleState().equals(ApiLifecycleState.PUBLISHED)));

        verify(apiNotificationService, times(2)).triggerUpdateNotification(eq(GraviteeContext.getExecutionContext()), eq(apiEntity));
    }

    @Test
    public void shouldUnpublishApi() throws TechnicalException {
        prepareUpdate();
        api.setApiLifecycleState(ApiLifecycleState.PUBLISHED);
        updateApiEntity.setLifecycleState(UNPUBLISHED);
        updatedApi.setApiLifecycleState(ApiLifecycleState.UNPUBLISHED);

        final ApiEntity apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);
        assertNotNull(apiEntity);
        assertEquals(UNPUBLISHED, apiEntity.getLifecycleState());

        verify(apiRepository).update(argThat(api -> api.getApiLifecycleState().equals(ApiLifecycleState.UNPUBLISHED)));
        verify(searchEngineService, times(1)).index(eq(GraviteeContext.getExecutionContext()), any(), eq(false));
        verify(apiNotificationService, times(1)).triggerUpdateNotification(eq(GraviteeContext.getExecutionContext()), eq(apiEntity));
    }

    @Test
    public void shouldNotChangeLifecycleStateFromUnpublishedToCreated() throws TechnicalException {
        prepareUpdate();
        assertUpdate(ApiLifecycleState.UNPUBLISHED, CREATED, true);
        assertUpdate(ApiLifecycleState.UNPUBLISHED, PUBLISHED, false);
        assertUpdate(ApiLifecycleState.UNPUBLISHED, UNPUBLISHED, false);
        assertUpdate(ApiLifecycleState.UNPUBLISHED, ARCHIVED, false);
    }

    @Test
    public void shouldNotUpdateADeprecatedApiWithSameDefinitionVersion_V1() throws TechnicalException {
        prepareUpdate(DefinitionVersion.V1, DefinitionVersion.V1);
        assertUpdate(ApiLifecycleState.DEPRECATED, CREATED, true);
        assertUpdate(ApiLifecycleState.DEPRECATED, PUBLISHED, true);
        assertUpdate(ApiLifecycleState.DEPRECATED, UNPUBLISHED, true);
        assertUpdate(ApiLifecycleState.DEPRECATED, ARCHIVED, true);
        assertUpdate(ApiLifecycleState.DEPRECATED, DEPRECATED, true);
    }

    @Test
    public void shouldNotUpdateADeprecatedApiWithSameDefinitionVersion_V2() throws TechnicalException {
        prepareUpdate(DefinitionVersion.V2, DefinitionVersion.V2);
        assertUpdate(ApiLifecycleState.DEPRECATED, CREATED, true);
        assertUpdate(ApiLifecycleState.DEPRECATED, PUBLISHED, true);
        assertUpdate(ApiLifecycleState.DEPRECATED, UNPUBLISHED, true);
        assertUpdate(ApiLifecycleState.DEPRECATED, ARCHIVED, true);
        assertUpdate(ApiLifecycleState.DEPRECATED, DEPRECATED, true);
    }

    @Test
    public void shouldUpdateADeprecatedApiIfDuringAConversion() throws TechnicalException {
        prepareUpdate(DefinitionVersion.V1, DefinitionVersion.V2);
        assertUpdate(ApiLifecycleState.DEPRECATED, CREATED, true);
        assertUpdate(ApiLifecycleState.DEPRECATED, PUBLISHED, true);
        assertUpdate(ApiLifecycleState.DEPRECATED, UNPUBLISHED, true);
        assertUpdate(ApiLifecycleState.DEPRECATED, ARCHIVED, true);
        assertUpdate(ApiLifecycleState.DEPRECATED, DEPRECATED, false);
    }

    @Test
    public void shouldNotChangeLifecycleStateFromArchived() throws TechnicalException {
        prepareUpdate();
        assertUpdate(ApiLifecycleState.ARCHIVED, CREATED, true);
        assertUpdate(ApiLifecycleState.ARCHIVED, PUBLISHED, true);
        assertUpdate(ApiLifecycleState.ARCHIVED, UNPUBLISHED, true);
        assertUpdate(ApiLifecycleState.ARCHIVED, DEPRECATED, true);
    }

    @Test
    public void shouldTraceReviewReject() throws TechnicalException {
        prepareReviewAuditTest();

        final ReviewEntity reviewEntity = new ReviewEntity();
        reviewEntity.setMessage("Test Review msg");
        apiService.rejectReview(GraviteeContext.getExecutionContext(), API_ID, USER_NAME, reviewEntity);

        verify(auditService)
            .createApiAuditLog(
                eq(GraviteeContext.getExecutionContext()),
                argThat(apiId -> apiId.equals(API_ID)),
                anyMap(),
                argThat(Workflow.AuditEvent.API_REVIEW_REJECTED::equals),
                any(),
                any(),
                any()
            );
        verify(apiNotificationService, times(0)).triggerUpdateNotification(eq(GraviteeContext.getExecutionContext()), any(ApiEntity.class));
    }

    @Test
    public void shouldTraceReviewAsked() throws TechnicalException {
        prepareReviewAuditTest();

        when(roleService.findByScope(RoleScope.API, GraviteeContext.getCurrentOrganization()))
            .thenReturn(Collections.singletonList(mock(RoleEntity.class)));
        final RolePermissionAction[] acls = { RolePermissionAction.UPDATE };
        when(roleService.hasPermission(any(), eq(ApiPermission.REVIEWS), eq(acls))).thenReturn(true);

        MembershipEntity membershipEntity = mock(MembershipEntity.class);
        when(membershipEntity.getMemberType()).thenReturn(MembershipMemberType.USER);
        when(membershipEntity.getMemberId()).thenReturn("reviewerID");
        when(membershipService.getMembershipsByReferenceAndRole(eq(MembershipReferenceType.API), eq(API_ID), any()))
            .thenReturn(Sets.newSet(membershipEntity));

        UserEntity reviewerEntity = mock(UserEntity.class);
        when(reviewerEntity.getEmail()).thenReturn("Reviewer@ema.il");
        when(userService.findById(GraviteeContext.getExecutionContext(), "reviewerID")).thenReturn(reviewerEntity);

        final ReviewEntity reviewEntity = new ReviewEntity();
        reviewEntity.setMessage("Test Review msg");
        apiService.askForReview(GraviteeContext.getExecutionContext(), API_ID, USER_NAME, reviewEntity);
        verify(auditService)
            .createApiAuditLog(
                eq(GraviteeContext.getExecutionContext()),
                argThat(apiId -> apiId.equals(API_ID)),
                anyMap(),
                argThat(evt -> Workflow.AuditEvent.API_REVIEW_ASKED.equals(evt)),
                any(),
                any(),
                any()
            );
        verify(emailService)
            .sendAsyncEmailNotification(
                eq(GraviteeContext.getExecutionContext()),
                argThat(emailNotification ->
                    emailNotification
                        .getTemplate()
                        .equals(EmailNotificationBuilder.EmailTemplate.API_ASK_FOR_REVIEW.getLinkedHook().getTemplate()) &&
                    emailNotification.getTo().length == 1 &&
                    emailNotification.getTo()[0].equals("Reviewer@ema.il")
                )
            );
        verify(roleService).findByScope(RoleScope.API, GraviteeContext.getCurrentOrganization());
        verify(apiNotificationService, times(0)).triggerUpdateNotification(eq(GraviteeContext.getExecutionContext()), any(ApiEntity.class));
    }

    @Test
    public void shouldTraceReviewAccepted() throws TechnicalException {
        prepareReviewAuditTest();

        final ReviewEntity reviewEntity = new ReviewEntity();
        reviewEntity.setMessage("Test Review msg");
        apiService.acceptReview(GraviteeContext.getExecutionContext(), API_ID, USER_NAME, reviewEntity);
        verify(auditService)
            .createApiAuditLog(
                eq(GraviteeContext.getExecutionContext()),
                argThat(apiId -> apiId.equals(API_ID)),
                anyMap(),
                argThat(evt -> Workflow.AuditEvent.API_REVIEW_ACCEPTED.equals(evt)),
                any(),
                any(),
                any()
            );
        verify(apiNotificationService, times(0)).triggerUpdateNotification(eq(GraviteeContext.getExecutionContext()), any(ApiEntity.class));
    }

    private void prepareReviewAuditTest() throws TechnicalException {
        api.setDefinition(API_DEFINITION);
        api.setId(API_ID);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        when(userService.findById(GraviteeContext.getExecutionContext(), USER_NAME)).thenReturn(mock(UserEntity.class));

        final Workflow workflow = new Workflow();
        workflow.setState(WorkflowState.REQUEST_FOR_CHANGES.name());
        when(workflowService.create(any(), any(), any(), any(), any(), any())).thenReturn(workflow);

        try {
            URL defaultEntrypoint = new URL(Key.PORTAL_ENTRYPOINT.defaultValue());
            when(apiEntrypointService.getApiEntrypoints(eq(GraviteeContext.getExecutionContext()), any(GenericApiEntity.class)))
                .thenReturn(List.of(new ApiEntrypointEntity(defaultEntrypoint.getPath(), defaultEntrypoint.getHost())));
        } catch (MalformedURLException e) {
            // Ignore this anyway
        }
    }

    @Test
    public void shouldNotChangeLifecycleStateFromCreatedInReview() throws TechnicalException {
        prepareUpdate();
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.API_REVIEW_ENABLED,
                "DEFAULT",
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(true);
        final Workflow workflow = new Workflow();
        workflow.setState("IN_REVIEW");
        when(workflowService.findByReferenceAndType(API, API_ID, REVIEW)).thenReturn(singletonList(workflow));

        assertUpdate(ApiLifecycleState.CREATED, CREATED, false);
        assertUpdate(ApiLifecycleState.CREATED, PUBLISHED, true);
        assertUpdate(ApiLifecycleState.CREATED, UNPUBLISHED, true);
        assertUpdate(ApiLifecycleState.CREATED, DEPRECATED, true);
        verify(apiNotificationService, times(1)).triggerUpdateNotification(eq(GraviteeContext.getExecutionContext()), any(ApiEntity.class));
    }

    @Test(expected = DefinitionVersionException.class)
    public void shouldNotDowngradeDefinitionVersion() throws TechnicalException {
        prepareUpdate();
        updateApiEntity.setGraviteeDefinitionVersion(DefinitionVersion.V1.getLabel());
        api.setDefinition(
            "{\"id\": \"" +
            API_ID +
            "\",\"name\": \"" +
            API_NAME +
            "\",\"gravitee\": \"2.0.0\"" +
            ",\"proxy\": {\"context_path\": \"/old\"} ,\"labels\": [\"public\"]}"
        );

        ApiEntity apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);

        verify(apiNotificationService, times(1)).triggerUpdateNotification(eq(GraviteeContext.getExecutionContext()), eq(apiEntity));
    }

    @Test(expected = InvalidDataException.class)
    public void shouldNotUseInvalidDefinitionVersion() throws TechnicalException {
        prepareUpdate();
        updateApiEntity.setGraviteeDefinitionVersion("0.0.0");
        api.setDefinition(
            "{\"id\": \"" +
            API_ID +
            "\",\"name\": \"" +
            API_NAME +
            "\",\"gravitee\": \"2.0.0\"" +
            ",\"proxy\": {\"context_path\": \"/old\"} ,\"labels\": [\"public\"]}"
        );

        ApiEntity apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);

        verify(apiNotificationService, times(1)).triggerUpdateNotification(eq(GraviteeContext.getExecutionContext()), eq(apiEntity));
    }

    @Test(expected = InvalidDataException.class)
    public void shouldNotUpdateWithInvalidResourceConfiguration() throws TechnicalException {
        prepareUpdate();
        Resource resource = new Resource();
        updateApiEntity.setResources(List.of(resource));
        doThrow(new InvalidDataException()).when(resourceService).validateResourceConfiguration(any(Resource.class));

        apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);

        fail("should throw InvalidDataException");
    }

    @Test
    public void shouldCreateAuditApiLoggingDisabledWhenSwitchingLogging() throws TechnicalException {
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.LOGGING_AUDIT_TRAIL_ENABLED,
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(true);

        api.setDefinition(
            "{\"id\": \"" +
            API_ID +
            "\",\"name\": \"" +
            API_NAME +
            "\"," +
            "   \"proxy\": {" +
            "    \"logging\": {\n" +
            "      \"mode\":\"CLIENT_PROXY\",\n" +
            "      \"condition\":\"condition\"\n" +
            "    },\n" +
            "\"context_path\": \"/old\"}}"
        );
        updatedApi.setDefinition("{\"id\": \"" + API_ID + "\",\"name\": \"" + API_NAME + "\",\"proxy\": {\"context_path\": \"/old\"}}");

        prepareUpdate();

        updateApiEntity.getProxy().setLogging(null);

        apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);

        verify(auditService)
            .createApiAuditLog(
                eq(GraviteeContext.getExecutionContext()),
                eq(API_ID),
                eq(emptyMap()),
                eq(Api.AuditEvent.API_LOGGING_DISABLED),
                any(Date.class),
                any(),
                any()
            );
    }

    @Test
    public void shouldCreateAuditApiLoggingEnabledWhenSwitchingLogging() throws TechnicalException {
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.LOGGING_AUDIT_TRAIL_ENABLED,
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(true);

        api.setDefinition("{\"id\": \"" + API_ID + "\",\"name\": \"" + API_NAME + "\",\"proxy\": {\"context_path\": \"/old\"}}");
        updatedApi.setDefinition(
            "{\"id\": \"" +
            API_ID +
            "\",\"name\": \"" +
            API_NAME +
            "\"," +
            "   \"proxy\": {" +
            "    \"logging\": {\n" +
            "      \"mode\":\"CLIENT_PROXY\",\n" +
            "      \"condition\":\"condition\"\n" +
            "    },\n" +
            "\"context_path\": \"/old\"}}"
        );

        prepareUpdate();

        updateApiEntity.getProxy().setLogging(null);

        apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);

        verify(auditService)
            .createApiAuditLog(
                eq(GraviteeContext.getExecutionContext()),
                eq(API_ID),
                eq(emptyMap()),
                eq(Api.AuditEvent.API_LOGGING_ENABLED),
                any(Date.class),
                any(),
                any()
            );
    }

    @Test
    public void shouldCreateAuditApiLoggingUpdatedWhenSwitchingLogging() throws TechnicalException {
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.LOGGING_AUDIT_TRAIL_ENABLED,
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(true);

        api.setDefinition(
            "{\"id\": \"" +
            API_ID +
            "\",\"name\": \"" +
            API_NAME +
            "\"," +
            "   \"proxy\": {" +
            "    \"logging\": {\n" +
            "      \"mode\":\"CLIENT_PROXY\",\n" +
            "      \"condition\":\"condition\"\n" +
            "    },\n" +
            "\"context_path\": \"/old\"}}"
        );
        updatedApi.setDefinition(
            "{\"id\": \"" +
            API_ID +
            "\",\"name\": \"" +
            API_NAME +
            "\"," +
            "   \"proxy\": {" +
            "    \"logging\": {\n" +
            "      \"mode\":\"CLIENT_PROXY\",\n" +
            "      \"condition\":\"condition2\"\n" +
            "    },\n" +
            "\"context_path\": \"/old\"}}"
        );

        prepareUpdate();

        updateApiEntity.getProxy().setLogging(null);

        ApiEntity apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);

        verify(auditService)
            .createApiAuditLog(
                eq(GraviteeContext.getExecutionContext()),
                eq(API_ID),
                eq(emptyMap()),
                eq(Api.AuditEvent.API_LOGGING_UPDATED),
                any(Date.class),
                any(),
                any()
            );
        verify(apiNotificationService, times(1)).triggerUpdateNotification(eq(GraviteeContext.getExecutionContext()), eq(apiEntity));
    }

    @Test
    public void shouldUpdateExistingApiWithV3ExecutionModeWhenNoExecutionMode() throws TechnicalException {
        prepareUpdate();
        when(apiRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
        final ApiEntity apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);

        assertNotNull(apiEntity);
        assertEquals(ExecutionMode.V3, apiEntity.getExecutionMode());
    }

    @Test
    public void shouldUpdateExistingApiWithV3ExecutionMode() throws TechnicalException {
        prepareUpdate();
        updateApiEntity.setExecutionMode(ExecutionMode.V3);
        when(apiRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
        final ApiEntity apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);

        assertNotNull(apiEntity);
        assertEquals(ExecutionMode.V3, apiEntity.getExecutionMode());
    }

    @Test
    public void shouldUpdateExistingApiWithV4EmulationEngineExecutionMode() throws TechnicalException {
        prepareUpdate();
        updateApiEntity.setExecutionMode(ExecutionMode.V4_EMULATION_ENGINE);
        when(apiRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));
        final ApiEntity apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);

        assertNotNull(apiEntity);
        assertEquals(ExecutionMode.V4_EMULATION_ENGINE, apiEntity.getExecutionMode());
    }

    @Test
    public void shouldKeepApiDefinitionContext() throws TechnicalException {
        api.setOrigin(Api.ORIGIN_KUBERNETES);
        api.setMode(Api.MODE_FULLY_MANAGED);
        prepareUpdate();
        when(apiRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);

        verify(apiRepository)
            .update(
                argThat(api ->
                    api.getId().equals(API_ID) &&
                    api.getOrigin().equals(Api.ORIGIN_KUBERNETES) &&
                    api.getMode().equals(Api.MODE_FULLY_MANAGED) &&
                    api.getLifecycleState().equals(LifecycleState.STARTED)
                )
            );
    }

    @Test
    public void shouldKeepApiStoppedStateForKubernetesApi() throws TechnicalException {
        api.setOrigin(Api.ORIGIN_KUBERNETES);
        api.setMode(Api.MODE_FULLY_MANAGED);
        prepareUpdate();

        updateApiEntity.setState(Lifecycle.State.STOPPED);

        when(apiRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);

        verify(apiRepository)
            .update(
                argThat(api ->
                    api.getId().equals(API_ID) &&
                    api.getOrigin().equals(Api.ORIGIN_KUBERNETES) &&
                    api.getMode().equals(Api.MODE_FULLY_MANAGED) &&
                    api.getLifecycleState().equals(LifecycleState.STOPPED)
                )
            );
    }

    @Test
    public void shouldKeepApiStartedStateForKubernetesApi() throws TechnicalException {
        api.setOrigin(Api.ORIGIN_KUBERNETES);
        api.setMode(Api.MODE_FULLY_MANAGED);
        prepareUpdate();
        when(apiRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        updateApiEntity.setState(Lifecycle.State.STARTED);

        apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);

        verify(apiRepository)
            .update(
                argThat(api ->
                    api.getId().equals(API_ID) &&
                    api.getOrigin().equals(Api.ORIGIN_KUBERNETES) &&
                    api.getMode().equals(Api.MODE_FULLY_MANAGED) &&
                    api.getLifecycleState().equals(LifecycleState.STARTED)
                )
            );
    }

    @Test(expected = ApiDefinitionVersionNotSupportedException.class)
    public void shouldNotUpdateIfDefinitionV1() throws TechnicalException, JsonProcessingException {
        Proxy proxy = new Proxy();
        proxy.setVirtualHosts(List.of(new VirtualHost("localhost")));

        io.gravitee.definition.model.Api definition = new io.gravitee.definition.model.Api();
        definition.setProxy(proxy);
        definition.setDefinitionVersion(DefinitionVersion.V1);

        Api apiEntityToUpdate = new Api();
        apiEntityToUpdate.setDefinition(objectMapper.writeValueAsString(definition));
        apiEntityToUpdate.setEnvironmentId(GraviteeContext.getCurrentEnvironment());

        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(apiEntityToUpdate));

        apiService.updateFromSwagger(GraviteeContext.getExecutionContext(), API_ID, null, null);
    }

    @Test
    public void shouldSanitizeUnsafeApiDescriptionDuringUpdate() throws TechnicalException {
        prepareUpdate();
        when(apiRepository.update(any())).thenAnswer(invocation -> invocation.getArgument(0));

        updateApiEntity.setDescription("\"A<img src=\\\"../../../image.png\\\"> Description\"");

        apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);

        verify(apiRepository).update(argThat(api -> api.getId().equals(API_ID) && api.getDescription().equals("\"A Description\"")));
    }

    @Test
    public void shouldSanitizeUnsafeApiDescriptionDuringUpdateFromSwagger() throws TechnicalException {
        prepareUpdate(DefinitionVersion.V2, DefinitionVersion.V2);
        updateApiEntity.setDescription("\"A<img src=\\\"../../../image.png\\\"> Description\"");

        SwaggerApiEntity swaggerApiEntity = new SwaggerApiEntity();
        swaggerApiEntity.setName(updateApiEntity.getName());
        swaggerApiEntity.setDescription(updateApiEntity.getDescription());
        swaggerApiEntity.setVersion(updateApiEntity.getVersion());
        swaggerApiEntity.setProxy(updateApiEntity.getProxy());
        swaggerApiEntity.setLifecycleState(updateApiEntity.getLifecycleState());
        swaggerApiEntity.setGraviteeDefinitionVersion(updateApiEntity.getGraviteeDefinitionVersion());

        apiService.updateFromSwagger(GraviteeContext.getExecutionContext(), API_ID, swaggerApiEntity, null);

        verify(apiRepository).update(argThat(api -> api.getId().equals(API_ID) && api.getDescription().equals("\"A Description\"")));
    }

    private void assertUpdate(
        final ApiLifecycleState fromLifecycleState,
        final io.gravitee.rest.api.model.api.ApiLifecycleState lifecycleState,
        final boolean shouldFail
    ) {
        api.setApiLifecycleState(fromLifecycleState);
        updateApiEntity.setLifecycleState(lifecycleState);
        boolean failed = false;
        try {
            apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity);
        } catch (final LifecycleStateChangeNotAllowedException ise) {
            failed = true;
        }
        if (!failed && shouldFail) {
            fail("Should not be possible to change the lifecycle state of a " + fromLifecycleState + " API to " + lifecycleState);
        }
    }
}
