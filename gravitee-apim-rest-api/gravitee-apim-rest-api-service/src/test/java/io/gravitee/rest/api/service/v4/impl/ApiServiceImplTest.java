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

import static io.gravitee.definition.model.DefinitionContext.ORIGIN_MANAGEMENT;
import static io.gravitee.rest.api.model.api.ApiLifecycleState.CREATED;
import static io.gravitee.rest.api.model.api.ApiLifecycleState.PUBLISHED;
import static io.gravitee.rest.api.model.api.ApiLifecycleState.UNPUBLISHED;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.DefinitionContext;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.analytics.Analytics;
import io.gravitee.definition.model.v4.analytics.logging.Logging;
import io.gravitee.definition.model.v4.analytics.logging.LoggingMode;
import io.gravitee.definition.model.v4.endpointgroup.Endpoint;
import io.gravitee.definition.model.v4.endpointgroup.EndpointGroup;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.flow.selector.ChannelSelector;
import io.gravitee.definition.model.v4.flow.selector.ConditionSelector;
import io.gravitee.definition.model.v4.flow.selector.HttpSelector;
import io.gravitee.definition.model.v4.flow.step.Step;
import io.gravitee.definition.model.v4.listener.ListenerType;
import io.gravitee.definition.model.v4.listener.entrypoint.Dlq;
import io.gravitee.definition.model.v4.listener.entrypoint.Entrypoint;
import io.gravitee.definition.model.v4.listener.entrypoint.Qos;
import io.gravitee.definition.model.v4.listener.http.HttpListener;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.definition.model.v4.listener.subscription.SubscriptionListener;
import io.gravitee.definition.model.v4.listener.tcp.TcpListener;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.definition.model.v4.resource.Resource;
import io.gravitee.definition.model.v4.service.ApiServices;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiQualityRuleRepository;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.GroupEvent;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.NotificationReferenceType;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.model.EventEntity;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.MemberEntity;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.MetadataFormat;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.RoleEntity;
import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.Visibility;
import io.gravitee.rest.api.model.api.ApiDeploymentEntity;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.model.v4.api.NewApiEntity;
import io.gravitee.rest.api.model.v4.api.UpdateApiEntity;
import io.gravitee.rest.api.model.v4.plan.PlanEntity;
import io.gravitee.rest.api.service.AlertService;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.ConnectorService;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.GenericNotificationConfigService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MediaService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PolicyService;
import io.gravitee.rest.api.service.PortalNotificationConfigService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.TopApiService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.VirtualHostService;
import io.gravitee.rest.api.service.WorkflowService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.CategoryMapper;
import io.gravitee.rest.api.service.exceptions.ApiNotDeletableException;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.ApiRunningStateException;
import io.gravitee.rest.api.service.exceptions.EndpointNameInvalidException;
import io.gravitee.rest.api.service.exceptions.InvalidDataException;
import io.gravitee.rest.api.service.exceptions.TagNotAllowedException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.NotifierServiceImpl;
import io.gravitee.rest.api.service.impl.upgrade.initializer.DefaultMetadataInitializer;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import io.gravitee.rest.api.service.processor.SynchronizationService;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.v4.*;
import io.gravitee.rest.api.service.v4.mapper.ApiMapper;
import io.gravitee.rest.api.service.v4.mapper.GenericApiMapper;
import io.gravitee.rest.api.service.v4.validation.ApiValidationService;
import io.gravitee.rest.api.service.v4.validation.TagsValidationService;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiServiceImplTest {

    private static final String API_ID = "id-api";
    private static final String API_NAME = "myAPI";
    private static final String USER_NAME = "myUser";
    private static final String PLAN_ID = "my-plan";
    private static final String SUBSCRIPTION_ID = "my-subscription";
    private final ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private MembershipService membershipService;

    @Mock
    private GroupService groupService;

    @Mock
    private PageService pageService;

    @Mock
    private UserService userService;

    @Mock
    private AuditService auditService;

    @Mock
    private SearchEngineService searchEngineService;

    @Mock
    private ParameterService parameterService;

    @Mock
    private GenericNotificationConfigService genericNotificationConfigService;

    @Mock
    private ApiMetadataService apiMetadataService;

    @Mock
    private VirtualHostService virtualHostService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private AlertService alertService;

    @Mock
    private RoleService roleService;

    @Mock
    private PolicyService policyService;

    @Mock
    private NotificationTemplateService notificationTemplateService;

    @Mock
    private ConnectorService connectorService;

    @Mock
    private PlanService planService;

    @Mock
    private PlanSearchService planSearchService;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private EventService eventService;

    @Mock
    private EventLatestRepository eventLatestRepository;

    @Mock
    private TopApiService topApiService;

    @Mock
    private FlowService flowService;

    @Mock
    private PortalNotificationConfigService portalNotificationConfigService;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private ApiQualityRuleRepository apiQualityRuleRepository;

    @Mock
    private ApiConverter apiConverter;

    @Mock
    private PrimaryOwnerService primaryOwnerService;

    @Mock
    private ApiValidationService apiValidationService;

    @Mock
    private MediaService mediaService;

    @Mock
    private PropertiesService propertiesService;

    @Mock
    private ApiNotificationService apiNotificationService;

    @Mock
    private ApiAuthorizationService apiAuthorizationService;

    @Mock
    private TagsValidationService tagsValidationService;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private SynchronizationService synchronizationService = Mockito.spy(new SynchronizationService(this.objectMapper));

    private ApiMapper apiMapper;
    private ApiService apiService;
    private ApiSearchService apiSearchService;
    private UpdateApiEntity updateApiEntity;
    private Api api;
    private Api updatedApi;
    private ApiStateService apiStateService;

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
        apiMapper =
            new ApiMapper(
                new ObjectMapper(),
                planService,
                flowService,
                parameterService,
                workflowService,
                new CategoryMapper(categoryService)
            );
        GenericApiMapper genericApiMapper = new GenericApiMapper(apiMapper, apiConverter);
        apiService =
            new ApiServiceImpl(
                apiRepository,
                apiMapper,
                genericApiMapper,
                primaryOwnerService,
                apiValidationService,
                parameterService,
                workflowService,
                auditService,
                membershipService,
                genericNotificationConfigService,
                apiMetadataService,
                flowService,
                searchEngineService,
                planService,
                planSearchService,
                subscriptionService,
                eventService,
                pageService,
                topApiService,
                portalNotificationConfigService,
                alertService,
                apiQualityRuleRepository,
                mediaService,
                propertiesService,
                apiNotificationService,
                tagsValidationService,
                apiAuthorizationService,
                groupService,
                categoryMapper
            );
        apiSearchService =
            new ApiSearchServiceImpl(
                apiRepository,
                apiMapper,
                genericApiMapper,
                primaryOwnerService,
                categoryService,
                searchEngineService,
                apiAuthorizationService
            );
        apiStateService =
            new ApiStateServiceImpl(
                apiSearchService,
                apiRepository,
                apiMapper,
                genericApiMapper,
                apiNotificationService,
                primaryOwnerService,
                auditService,
                eventService,
                eventLatestRepository,
                objectMapper,
                apiMetadataService,
                apiValidationService,
                planSearchService,
                apiConverter,
                synchronizationService
            );
        //        when(virtualHostService.sanitizeAndValidate(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
        reset(searchEngineService);
        UserEntity admin = new UserEntity();
        admin.setId(USER_NAME);
        when(primaryOwnerService.getPrimaryOwner(any(), any(), any())).thenReturn(new PrimaryOwnerEntity(admin));

        updateApiEntity = new UpdateApiEntity();
        updateApiEntity.setId(API_ID);
        updateApiEntity.setApiVersion("v1");
        updateApiEntity.setName(API_NAME);
        updateApiEntity.setDescription("Ma description");

        api = new Api();
        api.setId(API_ID);
        api.setEnvironmentId(GraviteeContext.getExecutionContext().getEnvironmentId());
        api.setDefinitionVersion(DefinitionVersion.V4);

        updatedApi = new Api();
        updatedApi.setId(API_ID);
        updatedApi.setName(API_NAME);
        updatedApi.setEnvironmentId(GraviteeContext.getExecutionContext().getEnvironmentId());

        when(apiMetadataService.fetchMetadataForApi(any(ExecutionContext.class), any(ApiEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(1));
    }

    @Test
    public void shouldCreateWithListener() throws TechnicalException {
        when(apiRepository.create(any()))
            .thenAnswer(invocation -> {
                Api api = invocation.getArgument(0);
                api.setId(API_ID);
                return api;
            });
        NewApiEntity newApiEntity = new NewApiEntity();
        newApiEntity.setName(API_NAME);
        newApiEntity.setApiVersion("v1");
        newApiEntity.setType(ApiType.PROXY);
        newApiEntity.setDescription("Ma description");
        HttpListener httpListener = new HttpListener();
        httpListener.setPaths(List.of(new Path("/context")));
        newApiEntity.setListeners(List.of(httpListener));

        final ApiEntity apiEntity = apiService.create(GraviteeContext.getExecutionContext(), newApiEntity, USER_NAME);

        assertThat(apiEntity).isNotNull();
        assertThat(apiEntity.getId()).isNotNull();
        assertThat(apiEntity.getName()).isEqualTo(API_NAME);
        assertThat(apiEntity.getApiVersion()).isEqualTo("v1");
        assertThat(apiEntity.getType()).isEqualTo(ApiType.PROXY);
        assertThat(apiEntity.getDescription()).isEqualTo("Ma description");
        assertThat(apiEntity.getDefinitionContext().getOrigin()).isEqualTo("management");
        assertThat(apiEntity.getDefinitionContext().getMode()).isEqualTo("fully_managed");
        assertThat(apiEntity.getListeners()).isNotNull();
        assertThat(apiEntity.getListeners().size()).isEqualTo(1);
        assertThat(apiEntity.getListeners().get(0)).isInstanceOf(HttpListener.class);
        HttpListener httpListenerCreated = (HttpListener) apiEntity.getListeners().get(0);
        assertThat(httpListenerCreated.getPaths().size()).isEqualTo(1);
        assertThat(httpListenerCreated.getPaths().get(0).getHost()).isNull();
        assertThat(httpListenerCreated.getPaths().get(0).getPath()).isEqualTo("/context");

        verify(searchEngineService, times(1)).index(eq(GraviteeContext.getExecutionContext()), any(), eq(false));
        verify(apiRepository, times(1)).create(any());
        verify(auditService, times(1))
            .createApiAuditLog(
                eq(GraviteeContext.getExecutionContext()),
                any(),
                any(),
                eq(Api.AuditEvent.API_CREATED),
                any(),
                eq(null),
                any()
            );
        verify(genericNotificationConfigService, times(1))
            .create(argThat(notifConfig -> notifConfig.getNotifier().equals(NotifierServiceImpl.DEFAULT_EMAIL_NOTIFIER_ID)));
        verify(apiMetadataService, times(1))
            .create(
                eq(GraviteeContext.getExecutionContext()),
                argThat(newApiMetadataEntity ->
                    newApiMetadataEntity.getFormat().equals(MetadataFormat.MAIL) &&
                    newApiMetadataEntity.getName().equals(DefaultMetadataInitializer.METADATA_EMAIL_SUPPORT_KEY)
                )
            );
        verify(membershipService, times(1))
            .addRoleToMemberOnReference(
                GraviteeContext.getExecutionContext(),
                new MembershipService.MembershipReference(MembershipReferenceType.API, API_ID),
                new MembershipService.MembershipMember(USER_NAME, null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.API, SystemRole.PRIMARY_OWNER.name())
            );
    }

    @Test
    public void shouldCreateWithListenerAndFlows() throws TechnicalException {
        when(apiRepository.create(any()))
            .thenAnswer(invocation -> {
                Api api = invocation.getArgument(0);
                api.setId(API_ID);
                return api;
            });
        NewApiEntity newApiEntity = new NewApiEntity();
        newApiEntity.setName(API_NAME);
        newApiEntity.setApiVersion("v1");
        newApiEntity.setType(ApiType.PROXY);
        newApiEntity.setDescription("Ma description");
        HttpListener httpListener = new HttpListener();
        httpListener.setPaths(List.of(new Path("/context")));
        newApiEntity.setListeners(List.of(httpListener));

        List<Flow> apiFlows = List.of(new Flow(), new Flow());
        newApiEntity.setFlows(apiFlows);

        final ApiEntity apiEntity = apiService.create(GraviteeContext.getExecutionContext(), newApiEntity, USER_NAME);

        assertThat(apiEntity).isNotNull();
        assertThat(apiEntity.getId()).isNotNull();
        assertThat(apiEntity.getName()).isEqualTo(API_NAME);
        assertThat(apiEntity.getApiVersion()).isEqualTo("v1");
        assertThat(apiEntity.getType()).isEqualTo(ApiType.PROXY);
        assertThat(apiEntity.getDescription()).isEqualTo("Ma description");
        assertThat(apiEntity.getListeners()).isNotNull();
        assertThat(apiEntity.getListeners().size()).isEqualTo(1);
        assertThat(apiEntity.getListeners().get(0)).isInstanceOf(HttpListener.class);
        HttpListener httpListenerCreated = (HttpListener) apiEntity.getListeners().get(0);
        assertThat(httpListenerCreated.getPaths().size()).isEqualTo(1);
        assertThat(httpListenerCreated.getPaths().get(0).getHost()).isNull();
        assertThat(httpListenerCreated.getPaths().get(0).getPath()).isEqualTo("/context");

        verify(searchEngineService, times(1)).index(eq(GraviteeContext.getExecutionContext()), any(), eq(false));
        verify(apiRepository, times(1)).create(any());
        verify(auditService, times(1))
            .createApiAuditLog(
                eq(GraviteeContext.getExecutionContext()),
                any(),
                any(),
                eq(Api.AuditEvent.API_CREATED),
                any(),
                eq(null),
                any()
            );
        verify(genericNotificationConfigService, times(1))
            .create(argThat(notifConfig -> notifConfig.getNotifier().equals(NotifierServiceImpl.DEFAULT_EMAIL_NOTIFIER_ID)));
        verify(apiMetadataService, times(1))
            .create(
                eq(GraviteeContext.getExecutionContext()),
                argThat(newApiMetadataEntity ->
                    newApiMetadataEntity.getFormat().equals(MetadataFormat.MAIL) &&
                    newApiMetadataEntity.getName().equals(DefaultMetadataInitializer.METADATA_EMAIL_SUPPORT_KEY)
                )
            );
        verify(membershipService, times(1))
            .addRoleToMemberOnReference(
                GraviteeContext.getExecutionContext(),
                new MembershipService.MembershipReference(MembershipReferenceType.API, API_ID),
                new MembershipService.MembershipMember(USER_NAME, null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.API, SystemRole.PRIMARY_OWNER.name())
            );
        verify(flowService, times(1)).save(FlowReferenceType.API, API_ID, apiFlows);
    }

    @Test(expected = ApiRunningStateException.class)
    public void shouldNotDeleteBecauseRunningState() throws TechnicalException {
        Api api = new Api();
        api.setId(API_ID);
        api.setLifecycleState(LifecycleState.STARTED);
        api.setOrigin(ORIGIN_MANAGEMENT);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        apiService.delete(GraviteeContext.getExecutionContext(), API_ID, false);
    }

    @Test
    public void shouldDeleteWithNoPlan() throws TechnicalException {
        Api api = new Api();
        api.setId(API_ID);
        api.setLifecycleState(LifecycleState.STOPPED);

        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(planService.findByApi(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(Collections.emptySet());

        apiService.delete(GraviteeContext.getExecutionContext(), API_ID, false);

        verify(membershipService, times(1)).deleteReference(GraviteeContext.getExecutionContext(), MembershipReferenceType.API, API_ID);
        verify(mediaService, times(1)).deleteAllByApi(API_ID);
        verify(apiMetadataService, times(1)).deleteAllByApi(eq(GraviteeContext.getExecutionContext()), eq(API_ID));
        verify(flowService, times(1)).save(FlowReferenceType.API, API_ID, null);
    }

    @Test(expected = ApiNotDeletableException.class)
    public void shouldNotDeleteBecausePublishedPlan() throws TechnicalException {
        Api api = new Api();
        api.setId(API_ID);
        api.setLifecycleState(LifecycleState.STOPPED);

        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        PlanEntity planEntity = new PlanEntity();
        planEntity.setId(PLAN_ID);
        planEntity.setStatus(PlanStatus.PUBLISHED);
        when(planSearchService.findByApi(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(singleton(planEntity));

        apiService.delete(GraviteeContext.getExecutionContext(), API_ID, false);
        verify(membershipService, times(1)).deleteReference(GraviteeContext.getExecutionContext(), MembershipReferenceType.API, API_ID);
    }

    @Test
    public void shouldDeleteWithClosedPlan() throws TechnicalException {
        Api api = new Api();
        api.setId(API_ID);
        api.setLifecycleState(LifecycleState.STOPPED);

        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        PlanEntity planEntity = new PlanEntity();
        planEntity.setId(PLAN_ID);
        planEntity.setStatus(PlanStatus.CLOSED);
        when(planSearchService.findByApi(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(singleton(planEntity));

        apiService.delete(GraviteeContext.getExecutionContext(), API_ID, false);

        verify(planService, times(1)).delete(GraviteeContext.getExecutionContext(), PLAN_ID);
        verify(membershipService, times(1)).deleteReference(GraviteeContext.getExecutionContext(), MembershipReferenceType.API, API_ID);
        verify(flowService, times(1)).save(FlowReferenceType.API, API_ID, null);
    }

    @Test
    public void shouldDeleteWithStatingPlanV4() throws TechnicalException {
        Api api = new Api();
        api.setId(API_ID);
        api.setLifecycleState(LifecycleState.STOPPED);

        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        PlanEntity planEntity = new PlanEntity();
        planEntity.setId(PLAN_ID);
        planEntity.setStatus(PlanStatus.STAGING);
        when(planSearchService.findByApi(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(singleton(planEntity));

        apiService.delete(GraviteeContext.getExecutionContext(), API_ID, false);

        verify(planService, times(1)).delete(GraviteeContext.getExecutionContext(), PLAN_ID);
        verify(apiQualityRuleRepository, times(1)).deleteByApi(API_ID);
        verify(membershipService, times(1)).deleteReference(GraviteeContext.getExecutionContext(), MembershipReferenceType.API, API_ID);
        verify(mediaService, times(1)).deleteAllByApi(API_ID);
        verify(apiMetadataService, times(1)).deleteAllByApi(eq(GraviteeContext.getExecutionContext()), eq(API_ID));
        verify(flowService, times(1)).save(FlowReferenceType.API, API_ID, null);
    }

    @Test
    public void shouldDeleteWithStatingPlanV2() throws TechnicalException {
        Api api = new Api();
        api.setId(API_ID);
        api.setLifecycleState(LifecycleState.STOPPED);

        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        io.gravitee.rest.api.model.PlanEntity planEntity = new io.gravitee.rest.api.model.PlanEntity();
        planEntity.setId(PLAN_ID);
        planEntity.setStatus(io.gravitee.rest.api.model.PlanStatus.STAGING);
        when(planSearchService.findByApi(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(singleton(planEntity));

        apiService.delete(GraviteeContext.getExecutionContext(), API_ID, false);

        verify(planService, times(1)).delete(GraviteeContext.getExecutionContext(), PLAN_ID);
        verify(apiQualityRuleRepository, times(1)).deleteByApi(API_ID);
        verify(membershipService, times(1)).deleteReference(GraviteeContext.getExecutionContext(), MembershipReferenceType.API, API_ID);
        verify(mediaService, times(1)).deleteAllByApi(API_ID);
        verify(apiMetadataService, times(1)).deleteAllByApi(eq(GraviteeContext.getExecutionContext()), eq(API_ID));
        verify(flowService, times(1)).save(FlowReferenceType.API, API_ID, null);
    }

    @Test
    public void shouldDeleteStoppedApiWithKubernetesOrigin() throws Exception {
        api.setOrigin(Api.ORIGIN_KUBERNETES);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        apiService.delete(GraviteeContext.getExecutionContext(), API_ID, false);

        verify(apiRepository).findById(eq(API_ID));
        verify(apiRepository).delete(eq(API_ID));
        verify(pageService).deleteAllByApi(eq(GraviteeContext.getExecutionContext()), eq(API_ID));
        verify(topApiService).delete(eq(GraviteeContext.getExecutionContext()), eq(API_ID));
        verify(searchEngineService).delete(eq(GraviteeContext.getExecutionContext()), argThat(_api -> _api.getId().equals(API_ID)));
        verify(membershipService).deleteReference(eq(GraviteeContext.getExecutionContext()), eq(MembershipReferenceType.API), eq(API_ID));
        verify(genericNotificationConfigService).deleteReference(eq(NotificationReferenceType.API), eq(API_ID));
        verify(portalNotificationConfigService).deleteReference(eq(NotificationReferenceType.API), eq(API_ID));
        verify(apiQualityRuleRepository).deleteByApi(eq(API_ID));
        verify(mediaService).deleteAllByApi(eq(API_ID));
        verify(apiMetadataService).deleteAllByApi(eq(GraviteeContext.getExecutionContext()), eq(API_ID));
    }

    @Test
    public void shouldCloseAndDeletePlansForApiWithKubernetesOrigin() throws Exception {
        api.setOrigin(Api.ORIGIN_KUBERNETES);

        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        final PlanEntity planEntity = new PlanEntity();
        planEntity.setId(PLAN_ID);
        planEntity.setStatus(PlanStatus.PUBLISHED);
        final PlanEntity closedPlan = new PlanEntity();
        closedPlan.setId(PLAN_ID);
        closedPlan.setStatus(PlanStatus.CLOSED);
        when(planSearchService.findByApi(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(Collections.singleton(planEntity));
        when(planService.close(GraviteeContext.getExecutionContext(), PLAN_ID)).thenReturn(closedPlan);

        apiService.delete(GraviteeContext.getExecutionContext(), API_ID, true);

        verify(planService).close(eq(GraviteeContext.getExecutionContext()), eq(PLAN_ID));
        verify(planService).delete(eq(GraviteeContext.getExecutionContext()), eq(PLAN_ID));
        verify(apiRepository).delete(eq(API_ID));
    }

    @Test
    public void shouldDeleteSubscriptionsAndApiWithKubernetesOrigin() throws Exception {
        api.setOrigin(Api.ORIGIN_KUBERNETES);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        SubscriptionEntity subscription = new SubscriptionEntity();
        subscription.setId(SUBSCRIPTION_ID);
        when(subscriptionService.findByApi(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(Collections.singleton(subscription));

        apiService.delete(GraviteeContext.getExecutionContext(), API_ID, false);

        verify(subscriptionService).findByApi(eq(GraviteeContext.getExecutionContext()), eq(API_ID));
        verify(subscriptionService).delete(eq(GraviteeContext.getExecutionContext()), eq(SUBSCRIPTION_ID));
        verify(apiRepository).delete(eq(API_ID));
    }

    /*
    Create by import tests
     */
    @Test
    public void shouldCreateFromImport() throws TechnicalException {
        ApiEntity apiEntity = fakeApiEntityV4();
        ExecutionContext executionContext = GraviteeContext.getExecutionContext();
        doReturn(Optional.empty()).when(apiRepository).findById(anyString());
        doReturn(apiEntity.getPrimaryOwner())
            .when(primaryOwnerService)
            .getPrimaryOwner(executionContext, USER_NAME, apiEntity.getPrimaryOwner());
        doReturn(emptySet()).when(groupService).findByEvent(GraviteeContext.getCurrentEnvironment(), GroupEvent.API_CREATE);
        doReturn(new ApiEntity()).when(apiMetadataService).fetchMetadataForApi(any(), any());
        doReturn(false).when(parameterService).findAsBoolean(executionContext, Key.API_REVIEW_ENABLED, ParameterReferenceType.ENVIRONMENT);

        Api createdApi = new Api();
        createdApi.setId(API_ID);
        createdApi.setCreatedAt(new Date());
        doReturn(createdApi).when(apiRepository).create(any());
        var result = apiService.createWithImport(executionContext, apiEntity, USER_NAME);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isNotEmpty();

        verify(primaryOwnerService).getPrimaryOwner(executionContext, USER_NAME, apiEntity.getPrimaryOwner());
        verify(apiValidationService).validateAndSanitizeImportApiForCreation(executionContext, apiEntity, apiEntity.getPrimaryOwner());

        ArgumentCaptor<Api> repositoryApiCaptor = ArgumentCaptor.forClass(Api.class);
        verify(apiRepository).create(repositoryApiCaptor.capture());

        testRepositoryApi(repositoryApiCaptor.getValue());

        verify(auditService)
            .createApiAuditLog(
                eq(executionContext),
                eq(API_ID),
                eq(emptyMap()),
                eq(Api.AuditEvent.API_CREATED),
                any(Date.class),
                isNull(),
                any(Api.class)
            );
        verify(membershipService)
            .addRoleToMemberOnReference(
                GraviteeContext.getExecutionContext(),
                new MembershipService.MembershipReference(MembershipReferenceType.API, API_ID),
                new MembershipService.MembershipMember(USER_NAME, null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.API, SystemRole.PRIMARY_OWNER.name())
            );
        verify(genericNotificationConfigService)
            .create(argThat(notifConfig -> notifConfig.getNotifier().equals(NotifierServiceImpl.DEFAULT_EMAIL_NOTIFIER_ID)));
        verify(apiMetadataService)
            .create(
                eq(GraviteeContext.getExecutionContext()),
                argThat(newApiMetadataEntity ->
                    newApiMetadataEntity.getFormat().equals(MetadataFormat.MAIL) &&
                    newApiMetadataEntity.getName().equals(DefaultMetadataInitializer.METADATA_EMAIL_SUPPORT_KEY)
                )
            );
        verify(flowService).save(FlowReferenceType.API, API_ID, apiEntity.getFlows());
        verify(apiMetadataService).fetchMetadataForApi(eq(executionContext), any(ApiEntity.class));
        verify(searchEngineService).index(eq(executionContext), any(GenericApiEntity.class), eq(false));
    }

    private void testRepositoryApi(Api api) {
        assertNotNull(api.getId());
        assertFalse(api.getId().isEmpty());
        assertEquals(GraviteeContext.getCurrentEnvironment(), api.getEnvironmentId());
        assertNotNull(api.getCreatedAt());
        assertEquals(api.getCreatedAt(), api.getUpdatedAt());
        assertEquals(ApiLifecycleState.CREATED, api.getApiLifecycleState());
        assertEquals(LifecycleState.STOPPED, api.getLifecycleState());
        assertEquals(io.gravitee.repository.management.model.Visibility.PUBLIC, api.getVisibility());
        assertNull(api.getGroups());
    }

    /*
    Update tests
     */
    @Test
    public void shouldUpdate() throws TechnicalException {
        prepareUpdate();

        final ApiEntity apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity, USER_NAME);

        assertNotNull(apiEntity);
        assertEquals(API_NAME, apiEntity.getName());
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

        apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity, USER_NAME);

        verify(planService, times(1)).createOrUpdatePlan(any(), same(plan1));
        verify(planService, times(1)).createOrUpdatePlan(any(), same(plan2));
    }

    @Test
    public void shouldUpdateFlows() throws TechnicalException {
        prepareUpdate();

        List<Flow> apiFlows = List.of(mock(Flow.class), mock(Flow.class));
        updateApiEntity.setFlows(apiFlows);

        apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity, USER_NAME);

        verify(flowService, times(1)).save(FlowReferenceType.API, API_ID, apiFlows);
    }

    @Test(expected = ApiNotFoundException.class)
    public void shouldNotUpdateBecauseNotFound() throws TechnicalException {
        prepareUpdate();

        when(apiRepository.findById(API_ID)).thenReturn(Optional.empty());

        apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity, USER_NAME);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotUpdateBecauseTechnicalException() throws TechnicalException {
        prepareUpdate();

        when(apiRepository.update(any())).thenThrow(TechnicalException.class);

        apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity, USER_NAME);
    }

    @Test(expected = EndpointNameInvalidException.class)
    public void shouldNotUpdateBecauseValidationException() throws TechnicalException {
        prepareUpdate();
        doThrow(EndpointNameInvalidException.class).when(apiValidationService).validateAndSanitizeUpdateApi(any(), any(), any(), any());

        apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity, USER_NAME);
    }

    @Test
    public void shouldNotDuplicateLabels() throws TechnicalException {
        prepareUpdate();
        updateApiEntity.setLabels(asList("label1", "label1"));

        final ApiEntity apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity, USER_NAME);
        verify(apiRepository).update(argThat(api -> api.getLabels().size() == 1));
        assertNotNull(apiEntity);
        verify(searchEngineService, times(1)).index(eq(GraviteeContext.getExecutionContext()), any(), eq(false));
    }

    @Test
    public void update_shouldNotChangeImages() throws TechnicalException {
        prepareUpdate();
        updateApiEntity.setLabels(asList("label1", "label1"));

        final ApiEntity apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity, USER_NAME);
        verify(apiRepository)
            .update(
                argThat(updateArg -> {
                    // Update should not change images, as there is a dedicated resource for that
                    assertThat(updateArg.getPicture()).isEqualTo(api.getPicture());
                    assertThat(updateArg.getBackground()).isEqualTo(api.getBackground());
                    return true;
                })
            );
        assertNotNull(apiEntity);
        verify(searchEngineService, times(1)).index(eq(GraviteeContext.getExecutionContext()), any(), eq(false));
    }

    @Test(expected = InvalidDataException.class)
    public void shouldNotUpdate_NewPlanNotAllowed() throws TechnicalException {
        prepareUpdate();
        PlanEntity updatedPlan = new PlanEntity();
        updatedPlan.setName("Plan Malicious");
        updatedPlan.setStatus(PlanStatus.PUBLISHED);
        updateApiEntity.setPlans(Set.of(updatedPlan));

        ApiEntity apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity, true, USER_NAME);
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

        ApiEntity apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity, true, USER_NAME);
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

        ApiEntity apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity, true, USER_NAME);
        verify(apiNotificationService, times(1)).triggerUpdateNotification(eq(GraviteeContext.getExecutionContext()), eq(apiEntity));
    }

    @Test
    public void shouldNotUpdate_TagUsedByPlanHasBeenRemoved() throws TechnicalException {
        prepareUpdate();

        api.setName(API_NAME);
        api.setApiLifecycleState(ApiLifecycleState.CREATED);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        PlanEntity updatedPlan = new PlanEntity();
        updatedPlan.setId("TAGPLAN");
        updatedPlan.setName("Plan");
        updatedPlan.setTags(Set.of("tag"));
        updatedPlan.setStatus(PlanStatus.PUBLISHED);
        updateApiEntity.setPlans(Set.of(updatedPlan));

        PlanEntity originalPlan = new PlanEntity();
        originalPlan.setId("TAGPLAN");
        originalPlan.setStatus(PlanStatus.PUBLISHED);
        when(planService.findByApi(any(), eq(API_ID))).thenReturn(Set.of(originalPlan));

        doThrow(new TagNotAllowedException(new String[0]))
            .when(tagsValidationService)
            .validatePlanTagsAgainstApiTags(any(Set.class), any(Set.class));

        assertThatThrownBy(() -> apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity, true, USER_NAME))
            .isInstanceOf(InvalidDataException.class);

        verify(apiRepository, never()).update(any());
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

        ApiEntity apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity, true, USER_NAME);
        verify(apiNotificationService, times(1)).triggerUpdateNotification(eq(GraviteeContext.getExecutionContext()), eq(apiEntity));
    }

    // TODO FCY: should be moved into right test class once healthcheck is moved to HTTP endpoint plugin
    //    @Test(expected = InvalidDataException.class)
    //    public void shouldNotUpdateWithInvalidSchedule() throws TechnicalException {
    //        prepareUpdate();
    //        Services services = new Services();
    //        HealthCheckService healthCheckService = mock(HealthCheckService.class);
    //        when(healthCheckService.getSchedule()).thenReturn("**");
    //        services.put(HealthCheckService.class, healthCheckService);
    //        updateApiEntity.setServices(services);
    //        final io.gravitee.rest.api.model.api.ApiEntity apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity, USER_NAME);
    //
    //        assertNotNull(apiEntity);
    //        assertEquals(API_NAME, apiEntity.getName());
    //        verify(apiNotificationService, times(1)).triggerUpdateNotification(eq(GraviteeContext.getExecutionContext()), eq(apiEntity));
    //    }
    //
    //    @Test
    //    public void shouldUpdateWithValidSchedule() throws TechnicalException {
    //        prepareUpdate();
    //        Services services = new Services();
    //        HealthCheckService healthCheckService = new HealthCheckService();
    //        healthCheckService.setSchedule("1,2 */100 5-8 * * *");
    //        services.put(HealthCheckService.class, healthCheckService);
    //        updateApiEntity.setServices(services);
    //        final io.gravitee.rest.api.model.api.ApiEntity apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity, USER_NAME);
    //
    //        assertNotNull(apiEntity);
    //        assertEquals(API_NAME, apiEntity.getName());
    //        verify(searchEngineService, times(1)).index(eq(GraviteeContext.getExecutionContext()), any(), eq(false));
    //        verify(apiNotificationService, times(1)).triggerUpdateNotification(eq(GraviteeContext.getExecutionContext()), eq(apiEntity));
    //    }

    @Test
    public void shouldPublishApi() throws TechnicalException {
        prepareUpdate();
        // from UNPUBLISHED state
        api.setApiLifecycleState(ApiLifecycleState.UNPUBLISHED);
        updateApiEntity.setLifecycleState(PUBLISHED);
        updatedApi.setApiLifecycleState(ApiLifecycleState.PUBLISHED);
        ApiEntity apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity, USER_NAME);
        assertNotNull(apiEntity);
        assertEquals(io.gravitee.rest.api.model.api.ApiLifecycleState.PUBLISHED, apiEntity.getLifecycleState());

        verify(apiRepository).update(argThat(api -> api.getApiLifecycleState().equals(ApiLifecycleState.PUBLISHED)));
        clearInvocations(apiRepository);

        // from CREATED state
        api.setApiLifecycleState(ApiLifecycleState.CREATED);
        updateApiEntity.setLifecycleState(PUBLISHED);
        updatedApi.setApiLifecycleState(ApiLifecycleState.PUBLISHED);

        apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity, USER_NAME);
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

        final ApiEntity apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity, USER_NAME);
        assertNotNull(apiEntity);
        assertEquals(UNPUBLISHED, apiEntity.getLifecycleState());

        verify(apiRepository).update(argThat(api -> api.getApiLifecycleState().equals(ApiLifecycleState.UNPUBLISHED)));
        verify(searchEngineService, times(1)).index(eq(GraviteeContext.getExecutionContext()), any(), eq(false));
        verify(apiNotificationService, times(1)).triggerUpdateNotification(eq(GraviteeContext.getExecutionContext()), eq(apiEntity));
    }

    @Test
    public void shouldCreateAuditApiLoggingDisabledWhenSwitchingLogging() throws TechnicalException, JsonProcessingException {
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.LOGGING_AUDIT_TRAIL_ENABLED,
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(true);

        prepareUpdate();

        io.gravitee.definition.model.v4.Api apiDefinition = new io.gravitee.definition.model.v4.Api();
        apiDefinition.setId(API_ID);
        apiDefinition.setName(API_NAME);

        HttpListener httpListener = new HttpListener();
        httpListener.setPaths(singletonList(new Path("/old")));
        apiDefinition.setListeners(singletonList(httpListener));
        Analytics analytics = new Analytics();
        analytics.setEnabled(true);
        Logging logging = new Logging();
        logging.setMode(LoggingMode.builder().entrypoint(true).endpoint(true).build());
        logging.setCondition("condition");
        analytics.setLogging(logging);
        apiDefinition.setAnalytics(analytics);
        api.setDefinition(objectMapper.writeValueAsString(apiDefinition));

        Analytics updatedAnalytics = new Analytics();
        updatedAnalytics.setEnabled(true);
        updateApiEntity.setAnalytics(updatedAnalytics);

        apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity, USER_NAME);

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
    public void shouldCreateAuditApiLoggingEnabledWhenSwitchingLogging() throws TechnicalException, JsonProcessingException {
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.LOGGING_AUDIT_TRAIL_ENABLED,
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(true);

        prepareUpdate();

        io.gravitee.definition.model.v4.Api apiDefinition = new io.gravitee.definition.model.v4.Api();
        apiDefinition.setId(API_ID);
        apiDefinition.setName(API_NAME);

        HttpListener httpListener = new HttpListener();
        httpListener.setPaths(singletonList(new Path("/old")));
        apiDefinition.setListeners(singletonList(httpListener));
        api.setDefinition(objectMapper.writeValueAsString(apiDefinition));

        Analytics analytics = new Analytics();
        analytics.setEnabled(true);
        Logging logging = new Logging();
        logging.setMode(LoggingMode.builder().entrypoint(true).endpoint(true).build());
        logging.setCondition("condition");
        analytics.setLogging(logging);
        updateApiEntity.setAnalytics(analytics);

        apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity, USER_NAME);

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
    public void shouldCreateAuditApiLoggingUpdatedWhenSwitchingLogging() throws TechnicalException, JsonProcessingException {
        when(
            parameterService.findAsBoolean(
                GraviteeContext.getExecutionContext(),
                Key.LOGGING_AUDIT_TRAIL_ENABLED,
                ParameterReferenceType.ENVIRONMENT
            )
        )
            .thenReturn(true);

        prepareUpdate();

        io.gravitee.definition.model.v4.Api apiDefinition = new io.gravitee.definition.model.v4.Api();
        apiDefinition.setId(API_ID);
        apiDefinition.setName(API_NAME);

        HttpListener httpListener = new HttpListener();
        httpListener.setPaths(singletonList(new Path("/old")));

        Analytics analytics = new Analytics();
        analytics.setEnabled(true);
        Logging logging = new Logging();
        logging.setMode(LoggingMode.builder().endpoint(true).entrypoint(true).build());
        logging.setCondition("condition");
        analytics.setLogging(logging);
        apiDefinition.setAnalytics(analytics);
        api.setDefinition(objectMapper.writeValueAsString(apiDefinition));

        logging.setCondition("condition2");
        updateApiEntity.setAnalytics(analytics);

        ApiEntity apiEntity = apiService.update(GraviteeContext.getExecutionContext(), API_ID, updateApiEntity, USER_NAME);

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
    public void shouldDeployApi() throws TechnicalException {
        final Event previousPublishedEvent = new Event();
        previousPublishedEvent.setProperties(Map.of(Event.EventProperties.DEPLOYMENT_NUMBER.getValue(), "3"));

        when(apiValidationService.canDeploy(any(), any())).thenReturn(true);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(apiRepository.update(api)).thenReturn(api);
        when(
            eventLatestRepository.search(
                any(EventCriteria.class),
                eq(io.gravitee.repository.management.model.Event.EventProperties.API_ID),
                eq(0L),
                eq(1L)
            )
        )
            .thenReturn(List.of(previousPublishedEvent));

        final ApiDeploymentEntity apiDeploymentEntity = new ApiDeploymentEntity();
        apiDeploymentEntity.setDeploymentLabel("deploy-label");
        final ApiEntity result = (ApiEntity) apiStateService.deploy(
            GraviteeContext.getExecutionContext(),
            API_ID,
            USER_NAME,
            apiDeploymentEntity
        );

        verify(eventService)
            .createApiEvent(
                any(ExecutionContext.class),
                any(Set.class),
                eq(EventType.PUBLISH_API),
                eq(api),
                argThat(properties ->
                    properties.get(Event.EventProperties.USER.getValue()).equals(USER_NAME) &&
                    properties.get(Event.EventProperties.DEPLOYMENT_NUMBER.getValue()).equals("4") &&
                    properties.get(Event.EventProperties.DEPLOYMENT_LABEL.getValue()).equals(apiDeploymentEntity.getDeploymentLabel())
                )
            );
        verify(apiNotificationService).triggerDeployNotification(any(ExecutionContext.class), eq(result));
    }

    @Test
    public void shouldFindByEnvironmentIdAndCrossId() throws TechnicalException {
        when(apiRepository.findByEnvironmentIdAndCrossId("environment", "api-cross-id")).thenReturn(Optional.of(api));

        final Optional<ApiEntity> result = apiService.findByEnvironmentIdAndCrossId("environment", "api-cross-id");
        assertThat(result).isNotEmpty();
        assertThat(result.get().getId()).isEqualTo(api.getId());
    }

    private void prepareUpdate() throws TechnicalException {
        prepareUpdate("endpointGroupName", "endpointName", "/context");
    }

    private void prepareUpdate(String endpointGroupName, String endpointName, String path) throws TechnicalException {
        prepareUpdateApiEntity(endpointGroupName, endpointName, path);

        when(apiRepository.update(any())).thenReturn(updatedApi);

        api.setName(API_NAME);
        api.setApiLifecycleState(ApiLifecycleState.CREATED);
        api.setPicture("picture");
        api.setBackground("background");
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        RoleEntity poRoleEntity = new RoleEntity();
        poRoleEntity.setName(SystemRole.PRIMARY_OWNER.name());
        poRoleEntity.setScope(RoleScope.API);

        MemberEntity po = new MemberEntity();
        po.setId(USER_NAME);
        po.setReferenceId(API_ID);
        po.setReferenceType(MembershipReferenceType.API);
        po.setRoles(Collections.singletonList(poRoleEntity));
    }

    private void prepareUpdateApiEntity(String endpointGroupName, String endpointName, String path) {
        updateApiEntity.setName(API_NAME);
        updateApiEntity.setApiVersion("v1");
        updateApiEntity.setDescription("Ma description");
        updateApiEntity.setDefinitionVersion(DefinitionVersion.V4);
        updateApiEntity.setType(ApiType.MESSAGE);

        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setName(endpointGroupName);
        Endpoint endpoint = new Endpoint();
        endpoint.setName(endpointName);
        endpointGroup.setEndpoints(singletonList(endpoint));
        updateApiEntity.setEndpointGroups(singletonList(endpointGroup));
        updateApiEntity.setLifecycleState(CREATED);

        HttpListener listener = new HttpListener();
        listener.setPaths(singletonList(new Path(path)));
        updateApiEntity.setListeners(singletonList(listener));
    }

    private ApiEntity fakeApiEntityV4() {
        var apiEntity = new ApiEntity();
        apiEntity.setDefinitionVersion(DefinitionVersion.V4);
        apiEntity.setId("");
        apiEntity.setCrossId(API_ID);
        apiEntity.setName(API_NAME);
        apiEntity.setApiVersion("v1.0");
        apiEntity.setBackground("background");
        apiEntity.setPicture("picture");
        apiEntity.setDescription("description");
        apiEntity.setLabels(List.of("label"));
        apiEntity.setType(ApiType.PROXY);
        apiEntity.setVisibility(Visibility.PUBLIC);

        DefinitionContext context = new DefinitionContext();
        context.setMode("READ_ONLY");
        context.setOrigin("MANAGEMENT");
        apiEntity.setDefinitionContext(context);
        PrimaryOwnerEntity primaryOwnerEntity = new PrimaryOwnerEntity();
        primaryOwnerEntity.setId(USER_NAME);
        primaryOwnerEntity.setType("USER");
        primaryOwnerEntity.setDisplayName(USER_NAME);
        apiEntity.setPrimaryOwner(primaryOwnerEntity);

        HttpListener httpListener = new HttpListener();
        httpListener.setPaths(List.of(new Path("my.fake.host", "/test")));
        httpListener.setPathMappings(Set.of("/test"));

        SubscriptionListener subscriptionListener = new SubscriptionListener();
        Entrypoint entrypoint = new Entrypoint();
        entrypoint.setType("Entrypoint type");
        entrypoint.setQos(Qos.AT_LEAST_ONCE);
        entrypoint.setDlq(new Dlq("my-endpoint"));
        entrypoint.setConfiguration("{\"nice\": \"configuration\"}");
        subscriptionListener.setEntrypoints(List.of(entrypoint));
        subscriptionListener.setType(ListenerType.SUBSCRIPTION);

        TcpListener tcpListener = new TcpListener();
        tcpListener.setType(ListenerType.TCP);
        tcpListener.setEntrypoints(List.of(entrypoint));

        apiEntity.setListeners(List.of(httpListener, subscriptionListener, tcpListener));
        apiEntity.setProperties(List.of(new Property()));
        apiEntity.setServices(new ApiServices());
        apiEntity.setResources(List.of(new Resource()));
        apiEntity.setResponseTemplates(Map.of("key", new HashMap<>()));
        apiEntity.setUpdatedAt(new Date());
        apiEntity.setAnalytics(new Analytics());

        EndpointGroup endpointGroup = new EndpointGroup();
        endpointGroup.setType("http-get");
        Endpoint endpoint = new Endpoint();
        endpoint.setType("http-get");
        endpoint.setConfiguration(
            "{\n" +
            "                        \"bootstrapServers\": \"kafka:9092\",\n" +
            "                        \"topics\": [\n" +
            "                            \"demo\"\n" +
            "                        ],\n" +
            "                        \"producer\": {\n" +
            "                            \"enabled\": false\n" +
            "                        },\n" +
            "                        \"consumer\": {\n" +
            "                            \"encodeMessageId\": true,\n" +
            "                            \"enabled\": true,\n" +
            "                            \"autoOffsetReset\": \"earliest\"\n" +
            "                        }\n" +
            "                    }"
        );
        endpointGroup.setEndpoints(List.of(endpoint));
        apiEntity.setEndpointGroups(List.of(endpointGroup));

        Flow flow = new Flow();
        flow.setName("flowName");
        flow.setEnabled(true);

        Step step = new Step();
        step.setEnabled(true);
        step.setPolicy("my-policy");
        step.setCondition("my-condition");
        flow.setRequest(List.of(step));
        flow.setTags(Set.of("tag1", "tag2"));

        HttpSelector httpSelector = new HttpSelector();
        httpSelector.setPath("/test");
        httpSelector.setMethods(Set.of(HttpMethod.GET, HttpMethod.POST));
        httpSelector.setPathOperator(Operator.STARTS_WITH);

        ChannelSelector channelSelector = new ChannelSelector();
        channelSelector.setChannel("my-channel");
        channelSelector.setChannelOperator(Operator.STARTS_WITH);
        channelSelector.setOperations(Set.of(ChannelSelector.Operation.SUBSCRIBE));
        channelSelector.setEntrypoints(Set.of("my-entrypoint"));

        ConditionSelector conditionSelector = new ConditionSelector();
        conditionSelector.setCondition("my-condition");

        flow.setSelectors(List.of(httpSelector, channelSelector, conditionSelector));
        apiEntity.setFlows(List.of(flow));

        return apiEntity;
    }
}
