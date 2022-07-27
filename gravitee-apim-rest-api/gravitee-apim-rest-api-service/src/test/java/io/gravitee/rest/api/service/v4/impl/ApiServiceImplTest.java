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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.flow.Flow;
import io.gravitee.definition.model.v4.listener.http.ListenerHttp;
import io.gravitee.definition.model.v4.listener.http.Path;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.MetadataFormat;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.model.v4.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.NewApiEntity;
import io.gravitee.rest.api.service.AlertService;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.ConnectorService;
import io.gravitee.rest.api.service.GenericNotificationConfigService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PolicyService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.VirtualHostService;
import io.gravitee.rest.api.service.WorkflowService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.impl.NotifierServiceImpl;
import io.gravitee.rest.api.service.impl.upgrade.DefaultMetadataUpgrader;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.v4.ApiService;
import io.gravitee.rest.api.service.v4.FlowService;
import io.gravitee.rest.api.service.v4.PlanService;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import io.gravitee.rest.api.service.v4.mapper.ApiMapper;
import io.gravitee.rest.api.service.v4.mapper.IndexableApiMapper;
import io.gravitee.rest.api.service.v4.validation.ApiValidationService;
import java.util.List;
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
    private FlowService flowService;

    @Mock
    private WorkflowService workflowService;

    @InjectMocks
    private ApiConverter apiConverter = Mockito.spy(new ApiConverter());

    @Mock
    private PrimaryOwnerService primaryOwnerService;

    @Mock
    private ApiValidationService apiValidationService;

    private ApiMapper apiMapper;
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
        apiMapper = new ApiMapper(new ObjectMapper(), planService, flowService, categoryService, parameterService, workflowService);
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
                null,
                null,
                pageService,
                null,
                null,
                alertService,
                null,
                null
            );
        //        when(virtualHostService.sanitizeAndValidate(any(), any())).thenAnswer(invocation -> invocation.getArgument(1));
        reset(searchEngineService);
        UserEntity admin = new UserEntity();
        admin.setId(USER_NAME);
        when(primaryOwnerService.getPrimaryOwner(any(), any(), any())).thenReturn(new PrimaryOwnerEntity(admin));
    }

    @Test
    public void shouldCreateForUserWithListener() throws TechnicalException {
        when(apiRepository.create(any()))
            .thenAnswer(invocation -> {
                Api api = invocation.getArgument(0);
                api.setId(API_ID);
                return api;
            });
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
                argThat(newApiMetadataEntity ->
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
            .thenAnswer(invocation -> {
                Api api = invocation.getArgument(0);
                api.setId(API_ID);
                return api;
            });
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
                argThat(newApiMetadataEntity ->
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
}
