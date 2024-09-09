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

import static io.gravitee.rest.api.service.V4EmulationEngineService.DefaultMode.YES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.api.domain_service.VerifyApiPathDomainService;
import io.gravitee.apim.core.validation.Validator;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.ExecutionMode;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.repository.management.model.Visibility;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.model.MembershipMemberType;
import io.gravitee.rest.api.model.MembershipReferenceType;
import io.gravitee.rest.api.model.MetadataFormat;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.alert.AlertReferenceType;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.NewApiEntity;
import io.gravitee.rest.api.model.permissions.RoleScope;
import io.gravitee.rest.api.model.permissions.SystemRole;
import io.gravitee.rest.api.service.AlertService;
import io.gravitee.rest.api.service.ApiMetadataService;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.ConnectorService;
import io.gravitee.rest.api.service.GenericNotificationConfigService;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.MembershipService;
import io.gravitee.rest.api.service.MetadataService;
import io.gravitee.rest.api.service.PageService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.PolicyService;
import io.gravitee.rest.api.service.RoleService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.V4EmulationEngineService;
import io.gravitee.rest.api.service.WorkflowService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.CategoryMapper;
import io.gravitee.rest.api.service.exceptions.ApiAlreadyExistsException;
import io.gravitee.rest.api.service.exceptions.ApiDefinitionVersionNotSupportedException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.notification.NotificationTemplateService;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.v4.ApiCategoryService;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import io.gravitee.rest.api.service.v4.validation.AnalyticsValidationService;
import io.gravitee.rest.api.service.v4.validation.CorsValidationService;
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
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Azize Elamrani (azize dot elamrani at gmail dot com)
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiService_CreateTest {

    private static final String API_ID = "id-api";
    private static final String API_NAME = "myAPI";
    private static final String USER_NAME = "myUser";
    private static final String SOURCE = "source";

    @InjectMocks
    private ApiServiceImpl apiService = new ApiServiceImpl();

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private MembershipService membershipService;

    @Spy
    private ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private NewApiEntity newApi;

    @Mock
    private Api api;

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
    private VerifyApiPathDomainService verifyApiPathDomainService;

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

    @Spy
    private CategoryMapper categoryMapper = new CategoryMapper(mock(CategoryService.class));

    @Mock
    private ApiCategoryService apiCategoryService;

    @InjectMocks
    private ApiConverter apiConverter = Mockito.spy(
        new ApiConverter(objectMapper, planService, flowService, categoryMapper, parameterService, mock(WorkflowService.class))
    );

    @Spy
    private V4EmulationEngineService v4EmulationEngine = new V4EmulationEngineServiceImpl(YES.getLabel());

    @Mock
    private PrimaryOwnerService primaryOwnerService;

    @Mock
    private CorsValidationService corsValidationService;

    @Mock
    private AnalyticsValidationService loggingValidationService;

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
        //        when(notificationTemplateService.resolveInlineTemplateWithParam(anyString(), anyString(), any(Reader.class), any()))
        //            .thenReturn("toDecode=decoded-value");
        //        when(parameterService.find(GraviteeContext.getExecutionContext(), Key.API_PRIMARY_OWNER_MODE, ParameterReferenceType.ENVIRONMENT))
        //            .thenReturn("USER");
        when(verifyApiPathDomainService.validateAndSanitize(any()))
            .thenAnswer(invocation -> Validator.Result.ofValue(invocation.getArgument(0)));
        reset(searchEngineService);
        UserEntity admin = new UserEntity();
        admin.setId(USER_NAME);
        when(primaryOwnerService.getPrimaryOwner(any(), any(), any())).thenReturn(new PrimaryOwnerEntity(admin));
    }

    @Test(expected = ApiDefinitionVersionNotSupportedException.class)
    public void shouldNotCreateWithOldDefinitionVersion() {
        when(newApi.getGraviteeDefinitionVersion()).thenReturn(DefinitionVersion.V1.getLabel());
        apiService.create(GraviteeContext.getExecutionContext(), newApi, USER_NAME);
    }

    @Test
    public void shouldCreateForUser() throws TechnicalException {
        when(api.getId()).thenReturn(API_ID);
        when(api.getName()).thenReturn(API_NAME);
        when(api.getVisibility()).thenReturn(Visibility.PRIVATE);
        when(api.getLifecycleState()).thenReturn(LifecycleState.STARTED);
        when(api.getApiLifecycleState()).thenReturn(ApiLifecycleState.PUBLISHED);
        when(apiRepository.findById(anyString())).thenReturn(Optional.empty());
        when(apiRepository.create(any())).thenReturn(api);
        when(newApi.getName()).thenReturn(API_NAME);

        when(newApi.getVersion()).thenReturn("v1");
        when(newApi.getDescription()).thenReturn("Ma description");
        when(newApi.getContextPath()).thenReturn("/context");

        when(groupService.findByEvent(eq(GraviteeContext.getCurrentEnvironment()), any())).thenReturn(Collections.emptySet());

        final ApiEntity apiEntity = apiService.create(GraviteeContext.getExecutionContext(), newApi, USER_NAME);

        assertNotNull(apiEntity);
        assertEquals(API_NAME, apiEntity.getName());
        verify(searchEngineService, times(1)).index(eq(GraviteeContext.getExecutionContext()), any(), eq(false));
        verify(alertService, times(1)).createDefaults(GraviteeContext.getExecutionContext(), AlertReferenceType.API, API_ID);
    }

    @Test(expected = ApiAlreadyExistsException.class)
    public void shouldNotCreateForUserBecauseExists() throws TechnicalException {
        // FIXME Should be remove. This test doesn't make any sense because there is no id given when creating an api
        // this is because we mock every id when calling findById that an error is thrown...
        when(apiRepository.findById(anyString())).thenReturn(Optional.of(api));
        when(newApi.getName()).thenReturn(API_NAME);

        when(newApi.getVersion()).thenReturn("v1");
        when(newApi.getDescription()).thenReturn("Ma description");

        apiService.create(GraviteeContext.getExecutionContext(), newApi, USER_NAME);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotCreateForUserBecauseTechnicalException() throws TechnicalException {
        when(apiRepository.findById(anyString())).thenThrow(TechnicalException.class);
        when(newApi.getName()).thenReturn(API_NAME);

        when(newApi.getVersion()).thenReturn("v1");
        when(newApi.getDescription()).thenReturn("Ma description");
        //        when(userService.findByUsername(USER_NAME, false)).thenReturn(new UserEntity());

        apiService.create(GraviteeContext.getExecutionContext(), newApi, USER_NAME);
    }

    @Test
    public void shouldCreateWithDefaultPath() throws TechnicalException {
        when(api.getId()).thenReturn(API_ID);
        when(api.getName()).thenReturn(API_NAME);
        when(api.getVisibility()).thenReturn(Visibility.PRIVATE);
        when(apiRepository.findById(anyString())).thenReturn(Optional.empty());
        when(apiRepository.create(any())).thenReturn(api);
        when(newApi.getName()).thenReturn(API_NAME);
        when(newApi.getVersion()).thenReturn("v1");
        when(newApi.getDescription()).thenReturn("Ma description");
        when(newApi.getContextPath()).thenReturn("/context");

        final ApiEntity apiEntity = apiService.create(GraviteeContext.getExecutionContext(), newApi, USER_NAME);

        assertNotNull(apiEntity);
        assertEquals(API_NAME, apiEntity.getName());
        assertNotNull(apiEntity.getPaths());

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
        verify(alertService, times(1)).createDefaults(GraviteeContext.getExecutionContext(), AlertReferenceType.API, API_ID);
    }

    @Test
    public void shouldCreate_AndSetupGenericNotifConfig() throws TechnicalException {
        when(api.getId()).thenReturn(API_ID);
        when(api.getName()).thenReturn(API_NAME);
        when(api.getVisibility()).thenReturn(Visibility.PRIVATE);
        when(apiRepository.findById(anyString())).thenReturn(Optional.empty());
        when(apiRepository.create(any())).thenReturn(api);
        when(newApi.getName()).thenReturn(API_NAME);
        when(newApi.getVersion()).thenReturn("v1");
        when(newApi.getDescription()).thenReturn("Ma description");
        when(newApi.getContextPath()).thenReturn("/context");

        apiService.create(GraviteeContext.getExecutionContext(), newApi, USER_NAME);

        verify(genericNotificationConfigService, times(1))
            .create(argThat(notifConfig -> notifConfig.getNotifier().equals(NotifierServiceImpl.DEFAULT_EMAIL_NOTIFIER_ID)));
        verify(alertService, times(1)).createDefaults(GraviteeContext.getExecutionContext(), AlertReferenceType.API, API_ID);
    }

    @Test
    public void shouldCreate_AndSetupEmailMetadata() throws TechnicalException {
        when(api.getId()).thenReturn(API_ID);
        when(api.getName()).thenReturn(API_NAME);
        when(api.getVisibility()).thenReturn(Visibility.PRIVATE);
        when(apiRepository.findById(anyString())).thenReturn(Optional.empty());
        when(apiRepository.create(any())).thenReturn(api);
        when(newApi.getName()).thenReturn(API_NAME);
        when(newApi.getVersion()).thenReturn("v1");
        when(newApi.getDescription()).thenReturn("Ma description");
        when(newApi.getContextPath()).thenReturn("/context");

        apiService.create(GraviteeContext.getExecutionContext(), newApi, USER_NAME);

        verify(apiMetadataService, times(1))
            .create(
                eq(GraviteeContext.getExecutionContext()),
                argThat(newApiMetadataEntity ->
                    newApiMetadataEntity.getFormat().equals(MetadataFormat.MAIL) &&
                    newApiMetadataEntity.getName().equals(MetadataService.METADATA_EMAIL_SUPPORT_KEY)
                )
            );
        verify(alertService, times(1)).createDefaults(GraviteeContext.getExecutionContext(), AlertReferenceType.API, API_ID);
    }

    @Test
    public void shouldCreate_AndCallSearchEngineIndexation() throws TechnicalException {
        when(api.getId()).thenReturn(API_ID);
        when(api.getName()).thenReturn(API_NAME);
        when(api.getVisibility()).thenReturn(Visibility.PRIVATE);
        when(apiRepository.findById(anyString())).thenReturn(Optional.empty());
        when(apiRepository.create(any())).thenReturn(api);
        when(newApi.getName()).thenReturn(API_NAME);
        when(newApi.getVersion()).thenReturn("v1");
        when(newApi.getDescription()).thenReturn("Ma description");
        when(newApi.getContextPath()).thenReturn("/context");

        apiService.create(GraviteeContext.getExecutionContext(), newApi, USER_NAME);

        verify(searchEngineService, times(1)).index(eq(GraviteeContext.getExecutionContext()), any(), eq(false));
        verify(alertService, times(1)).createDefaults(GraviteeContext.getExecutionContext(), AlertReferenceType.API, API_ID);
    }

    @Test
    public void shouldCreate_AndAddPrimaryOwner() throws TechnicalException {
        when(api.getId()).thenReturn(API_ID);
        when(api.getName()).thenReturn(API_NAME);
        when(api.getVisibility()).thenReturn(Visibility.PRIVATE);
        when(apiRepository.findById(anyString())).thenReturn(Optional.empty());
        when(apiRepository.create(any())).thenReturn(api);
        when(newApi.getName()).thenReturn(API_NAME);
        when(newApi.getVersion()).thenReturn("v1");
        when(newApi.getDescription()).thenReturn("Ma description");
        when(newApi.getContextPath()).thenReturn("/context");

        apiService.create(GraviteeContext.getExecutionContext(), newApi, USER_NAME);

        verify(membershipService, times(1))
            .addRoleToMemberOnReference(
                GraviteeContext.getExecutionContext(),
                new MembershipService.MembershipReference(MembershipReferenceType.API, API_ID),
                new MembershipService.MembershipMember(USER_NAME, null, MembershipMemberType.USER),
                new MembershipService.MembershipRole(RoleScope.API, SystemRole.PRIMARY_OWNER.name())
            );
        verify(alertService, times(1)).createDefaults(GraviteeContext.getExecutionContext(), AlertReferenceType.API, API_ID);
    }

    @Test
    public void shouldCreate_AndSaveFlows() throws TechnicalException {
        when(api.getId()).thenReturn(API_ID);
        when(apiRepository.findById(anyString())).thenReturn(Optional.empty());
        when(apiRepository.create(any())).thenReturn(api);

        when(newApi.getName()).thenReturn(API_NAME);
        when(newApi.getVersion()).thenReturn("v1");
        when(newApi.getDescription()).thenReturn("Ma description");

        List<Flow> apiFlows = List.of(mock(Flow.class), mock(Flow.class));
        when(newApi.getFlows()).thenReturn(apiFlows);

        apiService.create(GraviteeContext.getExecutionContext(), newApi, USER_NAME);

        verify(flowService, times(1)).save(FlowReferenceType.API, API_ID, apiFlows);
        verify(alertService, times(1)).createDefaults(GraviteeContext.getExecutionContext(), AlertReferenceType.API, API_ID);
    }

    @Test
    public void shouldCreateWithNoExecutionMode() throws TechnicalException {
        when(apiRepository.findById(anyString())).thenReturn(Optional.empty());
        when(apiRepository.create(any())).thenAnswer(invocation -> invocation.getArgument(0));

        NewApiEntity apiToCreate = new NewApiEntity();
        apiToCreate.setName(API_NAME);
        apiToCreate.setVersion("v1");
        apiToCreate.setDescription("Ma description");
        apiToCreate.setContextPath("/context");

        ApiEntity apiEntity = apiService.create(GraviteeContext.getExecutionContext(), apiToCreate, USER_NAME);

        assertEquals(ExecutionMode.V4_EMULATION_ENGINE, apiEntity.getExecutionMode());
        verify(alertService, times(1)).createDefaults(any(ExecutionContext.class), eq(AlertReferenceType.API), any());
    }

    @Test
    public void shouldSanitizeUnsafeApiDescription() throws TechnicalException {
        when(apiRepository.findById(anyString())).thenReturn(Optional.empty());
        when(apiRepository.create(any())).thenReturn(api);

        NewApiEntity apiToCreate = new NewApiEntity();
        apiToCreate.setName(API_NAME);
        apiToCreate.setVersion("v1");
        apiToCreate.setDescription("\"A<img src=\\\"../../../image.png\\\"> Description\"");
        apiToCreate.setContextPath("/context");

        apiService.create(GraviteeContext.getExecutionContext(), apiToCreate, USER_NAME);
        verify(apiRepository).create(argThat(api -> api.getDescription().equals("\"A Description\"")));
    }

    @Test
    public void shouldNotModifySafeApiDescription() throws TechnicalException {
        when(apiRepository.findById(anyString())).thenReturn(Optional.empty());
        when(apiRepository.create(any())).thenReturn(api);

        NewApiEntity apiToCreate = new NewApiEntity();
        apiToCreate.setName(API_NAME);
        apiToCreate.setVersion("v1");
        apiToCreate.setDescription("# This is my description\n## In markdown");
        apiToCreate.setContextPath("/context");

        apiService.create(GraviteeContext.getExecutionContext(), apiToCreate, USER_NAME);
        verify(apiRepository).create(argThat(api -> api.getDescription().equals(apiToCreate.getDescription())));
    }
}
