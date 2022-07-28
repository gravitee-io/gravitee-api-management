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
package io.gravitee.rest.api.service.v4.impl;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.listener.http.ListenerHttp;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiQualityRuleRepository;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.MetadataFormat;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.IndexableApi;
import io.gravitee.rest.api.model.v4.api.NewApiEntity;
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
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.exceptions.ApiNotDeletableException;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.ApiRunningStateException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.impl.NotifierServiceImpl;
import io.gravitee.rest.api.service.impl.upgrade.DefaultMetadataUpgrader;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.v4.*;
import io.gravitee.rest.api.service.v4.mapper.ApiMapper;
import io.gravitee.rest.api.service.v4.mapper.IndexableApiMapper;
import io.gravitee.rest.api.service.v4.validation.ApiValidationService;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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
    private SubscriptionService subscriptionService;

    @Mock
    private EventService eventService;

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

    @InjectMocks
    private ApiConverter apiConverter = Mockito.spy(new ApiConverter());

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

    private ApiService apiService;

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
        ApiMapper apiMapper = new ApiMapper(
            new ObjectMapper(),
            planService,
            flowService,
            categoryService,
            parameterService,
            workflowService
        );
        apiService =
            new ApiServiceImpl(
                apiRepository,
                apiMapper,
                new IndexableApiMapper(apiMapper, apiConverter),
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
                subscriptionService,
                eventService,
                pageService,
                topApiService,
                portalNotificationConfigService,
                alertService,
                apiQualityRuleRepository,
                mediaService,
                propertiesService,
                apiNotificationService
            );
        //        when(virtualHostService.sanitizeAndValidate(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
        reset(searchEngineService);
        UserEntity admin = new UserEntity();
        admin.setId(USER_NAME);
        when(primaryOwnerService.getPrimaryOwner(any(), any(), any())).thenReturn(new PrimaryOwnerEntity(admin));
    }

    @Test
    public void shouldCreateWithListener() throws TechnicalException {
        when(apiRepository.create(any()))
            .thenAnswer(
                invocation -> {
                    Api api = invocation.getArgument(0);
                    api.setId(API_ID);
                    return api;
                }
            );
        NewApiEntity newApiEntity = new NewApiEntity();
        newApiEntity.setName(API_NAME);
        newApiEntity.setApiVersion("v1");
        newApiEntity.setType(ApiType.SYNC);
        newApiEntity.setDescription("Ma description");
        ListenerHttp listenerHttp = new ListenerHttp();
        listenerHttp.setPaths(List.of(new Path("/context")));
        newApiEntity.setListeners(List.of(listenerHttp));

        final ApiEntity apiEntity = apiService.create(GraviteeContext.getExecutionContext(), newApiEntity, USER_NAME);

        assertThat(apiEntity).isNotNull();
        assertThat(apiEntity.getId()).isNotNull();
        assertThat(apiEntity.getName()).isEqualTo(API_NAME);
        assertThat(apiEntity.getApiVersion()).isEqualTo("v1");
        assertThat(apiEntity.getType()).isEqualTo(ApiType.SYNC);
        assertThat(apiEntity.getDescription()).isEqualTo("Ma description");
        assertThat(apiEntity.getListeners()).isNotNull();
        assertThat(apiEntity.getListeners().size()).isEqualTo(1);
        assertThat(apiEntity.getListeners().get(0)).isInstanceOf(ListenerHttp.class);
        ListenerHttp listenerHttpCreated = (ListenerHttp) apiEntity.getListeners().get(0);
        assertThat(listenerHttpCreated.getPaths().size()).isEqualTo(1);
        assertThat(listenerHttpCreated.getPaths().get(0).getHost()).isNull();
        assertThat(listenerHttpCreated.getPaths().get(0).getPath()).isEqualTo("/context");

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
                argThat(
                    newApiMetadataEntity ->
                        newApiMetadataEntity.getFormat().equals(MetadataFormat.MAIL) &&
                        newApiMetadataEntity.getName().equals(DefaultMetadataUpgrader.METADATA_EMAIL_SUPPORT_KEY)
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
            .thenAnswer(
                invocation -> {
                    Api api = invocation.getArgument(0);
                    api.setId(API_ID);
                    return api;
                }
            );
        NewApiEntity newApiEntity = new NewApiEntity();
        newApiEntity.setName(API_NAME);
        newApiEntity.setApiVersion("v1");
        newApiEntity.setType(ApiType.SYNC);
        newApiEntity.setDescription("Ma description");
        ListenerHttp listenerHttp = new ListenerHttp();
        listenerHttp.setPaths(List.of(new Path("/context")));
        newApiEntity.setListeners(List.of(listenerHttp));

        List<Flow> apiFlows = List.of(new Flow(), new Flow());
        newApiEntity.setFlows(apiFlows);

        final ApiEntity apiEntity = apiService.create(GraviteeContext.getExecutionContext(), newApiEntity, USER_NAME);

        assertThat(apiEntity).isNotNull();
        assertThat(apiEntity.getId()).isNotNull();
        assertThat(apiEntity.getName()).isEqualTo(API_NAME);
        assertThat(apiEntity.getApiVersion()).isEqualTo("v1");
        assertThat(apiEntity.getType()).isEqualTo(ApiType.SYNC);
        assertThat(apiEntity.getDescription()).isEqualTo("Ma description");
        assertThat(apiEntity.getListeners()).isNotNull();
        assertThat(apiEntity.getListeners().size()).isEqualTo(1);
        assertThat(apiEntity.getListeners().get(0)).isInstanceOf(ListenerHttp.class);
        ListenerHttp listenerHttpCreated = (ListenerHttp) apiEntity.getListeners().get(0);
        assertThat(listenerHttpCreated.getPaths().size()).isEqualTo(1);
        assertThat(listenerHttpCreated.getPaths().get(0).getHost()).isNull();
        assertThat(listenerHttpCreated.getPaths().get(0).getPath()).isEqualTo("/context");

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
                argThat(
                    newApiMetadataEntity ->
                        newApiMetadataEntity.getFormat().equals(MetadataFormat.MAIL) &&
                        newApiMetadataEntity.getName().equals(DefaultMetadataUpgrader.METADATA_EMAIL_SUPPORT_KEY)
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

    @Test
    public void shouldFindById() throws TechnicalException {
        Api api = new Api();
        api.setId(API_ID);
        api.setDefinitionVersion(DefinitionVersion.V4);
        api.setType(ApiType.ASYNC);
        api.setDefinition(
            "{\"definitionVersion\" : \"4.0.0\", " +
            "\"type\": \"async\", " +
            "\"listeners\" : " +
            "   [{ \"type\" : \"http\", \"paths\" : [{ \"path\": \"/context\"}]" +
            "}] }"
        );
        api.setEnvironmentId("DEFAULT");

        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        UserEntity userEntity = new UserEntity();
        userEntity.setId("user");
        when(primaryOwnerService.getPrimaryOwner(any(), eq(API_ID))).thenReturn(new PrimaryOwnerEntity(userEntity));

        final ApiEntity apiEntity = apiService.findById(GraviteeContext.getExecutionContext(), API_ID);

        assertThat(apiEntity).isNotNull();
        assertThat(apiEntity.getId()).isEqualTo(API_ID);
        assertThat(apiEntity.getDefinitionVersion()).isEqualTo(DefinitionVersion.V4);
        assertThat(apiEntity.getType()).isEqualTo(ApiType.ASYNC);
        assertThat(apiEntity.getListeners()).isNotNull();
        assertThat(apiEntity.getListeners().size()).isEqualTo(1);
        assertThat(apiEntity.getListeners().get(0)).isInstanceOf(ListenerHttp.class);
        ListenerHttp listenerHttpCreated = (ListenerHttp) apiEntity.getListeners().get(0);
        assertThat(listenerHttpCreated.getPaths().size()).isEqualTo(1);
        assertThat(listenerHttpCreated.getPaths().get(0).getHost()).isNull();
        assertThat(listenerHttpCreated.getPaths().get(0).getPath()).isEqualTo("/context");
    }

    @Test
    public void shouldFindByIdWithFlows() throws TechnicalException {
        Api api = new Api();
        api.setId(API_ID);
        api.setDefinitionVersion(DefinitionVersion.V4);
        api.setType(ApiType.ASYNC);
        api.setDefinition(
            "{\"definitionVersion\" : \"4.0.0\", " +
            "\"type\": \"async\", " +
            "\"listeners\" : " +
            "   [{ \"type\" : \"http\", \"paths\" : [{ \"path\": \"/context\"}]" +
            "}] }"
        );
        api.setEnvironmentId("DEFAULT");

        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        Flow flow1 = new Flow();
        flow1.setName("flow1");
        Flow flow2 = new Flow();
        flow1.setName("flow2");
        List<Flow> apiFlows = List.of(flow1, flow2);
        when(flowService.findByReference(FlowReferenceType.API, API_ID)).thenReturn(apiFlows);

        UserEntity userEntity = new UserEntity();
        userEntity.setId("user");
        when(primaryOwnerService.getPrimaryOwner(any(), eq(API_ID))).thenReturn(new PrimaryOwnerEntity(userEntity));

        final ApiEntity apiEntity = apiService.findById(GraviteeContext.getExecutionContext(), API_ID);

        assertThat(apiEntity).isNotNull();
        assertThat(apiEntity.getId()).isEqualTo(API_ID);
        assertThat(apiEntity.getDefinitionVersion()).isEqualTo(DefinitionVersion.V4);
        assertThat(apiEntity.getType()).isEqualTo(ApiType.ASYNC);
        assertThat(apiEntity.getListeners()).isNotNull();
        assertThat(apiEntity.getListeners().size()).isEqualTo(1);
        assertThat(apiEntity.getListeners().get(0)).isInstanceOf(ListenerHttp.class);
        ListenerHttp listenerHttpCreated = (ListenerHttp) apiEntity.getListeners().get(0);
        assertThat(listenerHttpCreated.getPaths().size()).isEqualTo(1);
        assertThat(listenerHttpCreated.getPaths().get(0).getHost()).isNull();
        assertThat(listenerHttpCreated.getPaths().get(0).getPath()).isEqualTo("/context");
        assertSame(apiFlows, apiEntity.getFlows());
        verify(flowService, times(1)).findByReference(FlowReferenceType.API, API_ID);
        verifyNoMoreInteractions(flowService);
    }

    @Test
    public void shouldFindV4IndexableApiWithDefinitionVersionV4() throws TechnicalException {
        Api api = new Api();
        api.setId(API_ID);
        api.setDefinitionVersion(DefinitionVersion.V4);
        api.setEnvironmentId("DEFAULT");

        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        UserEntity userEntity = new UserEntity();
        userEntity.setId("user");
        when(primaryOwnerService.getPrimaryOwner(any(), eq(API_ID))).thenReturn(new PrimaryOwnerEntity(userEntity));

        final IndexableApi indexableApi = apiService.findIndexableApiById(GraviteeContext.getExecutionContext(), API_ID);

        assertThat(indexableApi).isNotNull();
        assertThat(indexableApi).isInstanceOf(ApiEntity.class);
    }

    @Test
    public void shouldFindV2IndexableApiWithNoDefinitionVersion() throws TechnicalException {
        Api api = new Api();
        api.setId(API_ID);
        api.setEnvironmentId("DEFAULT");

        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        UserEntity userEntity = new UserEntity();
        userEntity.setId("user");
        when(primaryOwnerService.getPrimaryOwner(any(), eq(API_ID))).thenReturn(new PrimaryOwnerEntity(userEntity));

        final IndexableApi indexableApi = apiService.findIndexableApiById(GraviteeContext.getExecutionContext(), API_ID);

        assertThat(indexableApi).isNotNull();
        assertThat(indexableApi).isInstanceOf(io.gravitee.rest.api.model.api.ApiEntity.class);
    }

    @Test
    public void shouldFindV2IndexableApiWithV2DefinitionVersion() throws TechnicalException {
        Api api = new Api();
        api.setId(API_ID);
        api.setEnvironmentId("DEFAULT");
        api.setDefinitionVersion(DefinitionVersion.V2);

        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        UserEntity userEntity = new UserEntity();
        userEntity.setId("user");
        when(primaryOwnerService.getPrimaryOwner(any(), eq(API_ID))).thenReturn(new PrimaryOwnerEntity(userEntity));

        final IndexableApi indexableApi = apiService.findIndexableApiById(GraviteeContext.getExecutionContext(), API_ID);

        assertThat(indexableApi).isNotNull();
        assertThat(indexableApi).isInstanceOf(io.gravitee.rest.api.model.api.ApiEntity.class);
    }

    @Test(expected = ApiNotFoundException.class)
    public void shouldNotFindBecauseNotExists() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.empty());

        apiService.findById(GraviteeContext.getExecutionContext(), API_ID);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindBecauseTechnicalException() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenThrow(TechnicalException.class);

        apiService.findById(GraviteeContext.getExecutionContext(), API_ID);
    }

    @Test
    public void shouldExists() throws TechnicalException {
        Api api = new Api();
        api.setId(API_ID);

        when(apiRepository.existById(API_ID)).thenReturn(true);

        boolean exists = apiService.exists(API_ID);
        assertThat(exists).isTrue();
    }

    @Test(expected = ApiRunningStateException.class)
    public void shouldNotDeleteBecauseRunningState() throws TechnicalException {
        Api api = new Api();
        api.setId(API_ID);
        api.setLifecycleState(LifecycleState.STARTED);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        apiService.delete(GraviteeContext.getExecutionContext(), API_ID);
    }

    @Test
    public void shouldDeleteWithNoPlan() throws TechnicalException {
        Api api = new Api();
        api.setId(API_ID);
        api.setLifecycleState(LifecycleState.STOPPED);

        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(planService.findByApi(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(Collections.emptySet());

        apiService.delete(GraviteeContext.getExecutionContext(), API_ID);

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
        when(planService.findByApi(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(Collections.singleton(planEntity));

        apiService.delete(GraviteeContext.getExecutionContext(), API_ID);
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
        when(planService.findByApi(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(Collections.singleton(planEntity));

        apiService.delete(GraviteeContext.getExecutionContext(), API_ID);

        verify(planService, times(1)).delete(GraviteeContext.getExecutionContext(), PLAN_ID);
        verify(membershipService, times(1)).deleteReference(GraviteeContext.getExecutionContext(), MembershipReferenceType.API, API_ID);
        verify(flowService, times(1)).save(FlowReferenceType.API, API_ID, null);
    }

    @Test
    public void shouldDeleteWithStatingPlan() throws TechnicalException {
        Api api = new Api();
        api.setId(API_ID);
        api.setLifecycleState(LifecycleState.STOPPED);

        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        PlanEntity planEntity = new PlanEntity();
        planEntity.setId(PLAN_ID);
        planEntity.setStatus(PlanStatus.STAGING);
        when(planService.findByApi(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(Collections.singleton(planEntity));

        apiService.delete(GraviteeContext.getExecutionContext(), API_ID);

        verify(planService, times(1)).delete(GraviteeContext.getExecutionContext(), PLAN_ID);
        verify(apiQualityRuleRepository, times(1)).deleteByApi(API_ID);
        verify(membershipService, times(1)).deleteReference(GraviteeContext.getExecutionContext(), MembershipReferenceType.API, API_ID);
        verify(mediaService, times(1)).deleteAllByApi(API_ID);
        verify(apiMetadataService, times(1)).deleteAllByApi(eq(GraviteeContext.getExecutionContext()), eq(API_ID));
        verify(flowService, times(1)).save(FlowReferenceType.API, API_ID, null);
    }
}
