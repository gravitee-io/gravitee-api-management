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

import static io.gravitee.repository.management.model.Api.AuditEvent.API_ENCRYPTED_PROPERTIES_ACCESSED;
import static io.gravitee.repository.management.model.Api.AuditEvent.API_ENCRYPTED_PROPERTIES_REFRESHED;
import static io.gravitee.repository.management.model.Audit.AuditProperties.ENCRYPTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.event.EventManager;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.Property;
import io.gravitee.definition.model.Proxy;
import io.gravitee.definition.model.VirtualHost;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Event;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiDeploymentEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.CategoryMapper;
import io.gravitee.rest.api.service.exceptions.ApiNotDeployableException;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.processor.SynchronizationService;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.v4.*;
import io.gravitee.rest.api.service.v4.PlanService;
import io.gravitee.rest.api.service.v4.mapper.ApiMapper;
import io.gravitee.rest.api.service.v4.mapper.GenericApiMapper;
import io.gravitee.rest.api.service.v4.validation.ApiValidationService;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class ApiStateServiceImpl_DeployTest {

    private static final String API_ID = "id-api";
    private static final String API_NAME = "myAPI";
    private static final String USER_NAME = "myUser";
    private static final String ENCRYPTED_PROPERTY_DEFINITION = """
        {"properties":[{"key":"plain","value":"value","encrypted":false},{"key":"secret","value":"cipher","encrypted":true}]}""";
    private static final String NATIVE_ENCRYPTED_PROPERTY_DEFINITION = """
        {"type":"native","properties":[{"key":"plain","value":"value","encrypted":false},{"key":"secret","value":"cipher","encrypted":true}]}""";
    private static final String ACCESS_PATCH = """
        [{"op":"access","path":"/properties/secret","value":{"value":"<sha256:c806cd9c716cfbfdb4763c71dd1394b3e602fce81291a0338bf8e3225416ac32>","encrypted":true}}]""";
    private static final String PLAIN_PROPERTY_DEFINITION = """
        {"properties":[{"key":"plain","value":"value","encrypted":false}]}""";
    private final ObjectMapper objectMapper = new GraviteeMapper();

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private SearchEngineService searchEngineService;

    @Mock
    private ParameterService parameterService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private PlanService planServiceV4;

    @Mock
    private io.gravitee.rest.api.service.PlanService planService;

    @Mock
    private EventService eventService;

    @Mock
    private EventLatestRepository eventLatestRepository;

    @Mock
    private FlowService flowServiceV4;

    @Mock
    private io.gravitee.rest.api.service.configuration.flow.FlowService flowService;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private PrimaryOwnerService primaryOwnerService;

    @Mock
    private ApiNotificationService apiNotificationService;

    @Mock
    private ApiSearchService apiSearchService;

    @Mock
    private ApiMetadataService apiMetadataService;

    @Mock
    private ApiValidationService apiValidationService;

    @Mock
    private EventManager eventManager;

    @Spy
    private CategoryMapper categoryMapper = new CategoryMapper(mock(CategoryService.class));

    @InjectMocks
    private ApiConverter apiConverter = new ApiConverter(
        objectMapper,
        planService,
        flowService,
        categoryMapper,
        parameterService,
        workflowService
    );

    @Mock
    private PlanSearchService planSearchService;

    @InjectMocks
    private SynchronizationService synchronizationService = Mockito.spy(new SynchronizationService(this.objectMapper));

    private final io.gravitee.apim.core.cluster.domain_service.ValidateApiClusterBindingService validateApiClusterBindingService = mock(
        io.gravitee.apim.core.cluster.domain_service.ValidateApiClusterBindingService.class
    );

    private Api api;
    private Api updatedApi;
    private ApiStateService apiStateService;

    @AfterAll
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

    @BeforeEach
    public void setUp() {
        ApiMapper apiMapper = new ApiMapper(
            new ObjectMapper(),
            planServiceV4,
            flowServiceV4,
            parameterService,
            workflowService,
            new CategoryMapper(categoryService)
        );
        GenericApiMapper genericApiMapper = new GenericApiMapper(apiMapper, apiConverter);
        apiStateService = new ApiStateServiceImpl(
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
            synchronizationService,
            eventManager,
            searchEngineService,
            validateApiClusterBindingService
        );
        reset(searchEngineService, validateApiClusterBindingService);
        UserEntity admin = new UserEntity();
        admin.setId(USER_NAME);

        api = new Api();
        api.setId(API_ID);
        api.setName(API_NAME);
        api.setEnvironmentId(GraviteeContext.getExecutionContext().getEnvironmentId());
        api.setDefinitionVersion(DefinitionVersion.V4);
        api.setType(ApiType.PROXY);

        updatedApi = new Api(api);

        when(apiMetadataService.fetchMetadataForApi(any(ExecutionContext.class), any(GenericApiEntity.class))).then(invocation ->
            invocation.getArgument(1)
        );
    }

    @Test
    public void should_deploy_api_if_managed_by_kubernetes() throws TechnicalException {
        final Event previousPublishedEvent = new Event();
        previousPublishedEvent.setProperties(Map.of(Event.EventProperties.DEPLOYMENT_NUMBER.getValue(), "3"));

        when(apiValidationService.canDeploy(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(true);
        when(apiSearchService.findRepositoryApiById(any(), eq(API_ID))).thenReturn(api);
        when(apiRepository.update(api)).thenReturn(api);
        when(eventLatestRepository.search(any(EventCriteria.class), eq(Event.EventProperties.API_ID), eq(0L), eq(1L))).thenReturn(
            List.of(previousPublishedEvent)
        );

        final ApiDeploymentEntity apiDeploymentEntity = new ApiDeploymentEntity();
        apiDeploymentEntity.setDeploymentLabel("deploy-label");
        final GenericApiEntity result = apiStateService.deploy(
            GraviteeContext.getExecutionContext(),
            API_ID,
            USER_NAME,
            apiDeploymentEntity
        );

        verify(eventService).createApiEvent(
            any(ExecutionContext.class),
            anySet(),
            anyString(),
            eq(EventType.PUBLISH_API),
            eq(api),
            argThat(
                properties ->
                    properties.get(Event.EventProperties.USER.getValue()).equals(USER_NAME) &&
                    properties.get(Event.EventProperties.DEPLOYMENT_NUMBER.getValue()).equals("4") &&
                    properties.get(Event.EventProperties.DEPLOYMENT_LABEL.getValue()).equals(apiDeploymentEntity.getDeploymentLabel())
            )
        );
        verify(apiNotificationService).triggerDeployNotification(any(ExecutionContext.class), eq(result));
    }

    @Test
    public void should_not_deploy_when_no_active_plan_for_api() {
        assertThrows(ApiNotDeployableException.class, () -> {
            when(apiValidationService.canDeploy(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(false);
            when(apiSearchService.findRepositoryApiById(any(), eq(API_ID))).thenReturn(api);
            apiStateService.deploy(GraviteeContext.getExecutionContext(), API_ID, "some-user", new ApiDeploymentEntity());
        });
    }

    @Test
    public void should_not_deploy_when_bound_virtual_cluster_is_not_deployed() throws TechnicalException {
        when(apiValidationService.canDeploy(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(true);
        when(apiSearchService.findRepositoryApiById(any(), eq(API_ID))).thenReturn(api);
        doThrow(new ApiNotDeployableException("bound virtual cluster is not deployed"))
            .when(validateApiClusterBindingService)
            .validateDeployable(eq(API_ID), any(), any(), any());

        assertThrows(ApiNotDeployableException.class, () ->
            apiStateService.deploy(GraviteeContext.getExecutionContext(), API_ID, USER_NAME, new ApiDeploymentEntity())
        );
        verify(apiRepository, never()).update(any());
    }

    @Test
    public void should_deploy_api() throws TechnicalException {
        final Event previousPublishedEvent = new Event();
        previousPublishedEvent.setProperties(Map.of(Event.EventProperties.DEPLOYMENT_NUMBER.getValue(), "3"));

        when(apiValidationService.canDeploy(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(true);
        when(apiSearchService.findRepositoryApiById(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(api);
        when(apiRepository.update(api)).thenReturn(api);
        when(eventLatestRepository.search(any(EventCriteria.class), eq(Event.EventProperties.API_ID), eq(0L), eq(1L))).thenReturn(
            List.of(previousPublishedEvent)
        );

        final ApiDeploymentEntity apiDeploymentEntity = new ApiDeploymentEntity();
        apiDeploymentEntity.setDeploymentLabel("deploy-label");
        final GenericApiEntity result = apiStateService.deploy(
            GraviteeContext.getExecutionContext(),
            API_ID,
            USER_NAME,
            apiDeploymentEntity
        );

        verify(eventService).createApiEvent(
            any(ExecutionContext.class),
            anySet(),
            anyString(),
            eq(EventType.PUBLISH_API),
            eq(api),
            argThat(
                properties ->
                    properties.get(Event.EventProperties.USER.getValue()).equals(USER_NAME) &&
                    properties.get(Event.EventProperties.DEPLOYMENT_NUMBER.getValue()).equals("4") &&
                    properties.get(Event.EventProperties.DEPLOYMENT_LABEL.getValue()).equals(apiDeploymentEntity.getDeploymentLabel())
            )
        );
        verify(apiNotificationService).triggerDeployNotification(any(ExecutionContext.class), eq(result));
    }

    @Test
    public void should_redeploy_api_with_synced_dynamic_properties() throws TechnicalException {
        when(apiValidationService.canDeploy(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(true);
        when(apiSearchService.findRepositoryApiById(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(api);
        when(apiRepository.update(api)).thenReturn(api);

        final GenericApiEntity result = apiStateService.redeployWithSyncedDynamicProperties(
            GraviteeContext.getExecutionContext(),
            updatedApi,
            USER_NAME,
            new ApiDeploymentEntity("http-dynamic-properties sync")
        );

        verify(eventService).createApiEvent(
            any(ExecutionContext.class),
            anySet(),
            anyString(),
            eq(EventType.PUBLISH_API),
            same(updatedApi),
            argThat(properties -> "http-dynamic-properties sync".equals(properties.get(Event.EventProperties.DEPLOYMENT_LABEL.getValue())))
        );
        verify(apiNotificationService).triggerDeployNotification(any(ExecutionContext.class), eq(result));
    }

    @Test
    public void should_audit_encrypted_properties_access_when_deploying_a_v4_http_api() throws TechnicalException {
        given_deployable_api(ApiType.PROXY, ENCRYPTED_PROPERTY_DEFINITION);

        apiStateService.deploy(GraviteeContext.getExecutionContext(), API_ID, USER_NAME, new ApiDeploymentEntity());

        verify_encrypted_properties_access_audited();
    }

    @Test
    public void should_audit_encrypted_properties_access_when_deploying_a_v4_native_api() throws TechnicalException {
        given_deployable_api(ApiType.NATIVE, NATIVE_ENCRYPTED_PROPERTY_DEFINITION);

        apiStateService.deploy(GraviteeContext.getExecutionContext(), API_ID, USER_NAME, new ApiDeploymentEntity());

        verify_encrypted_properties_access_audited();
    }

    @Test
    public void should_audit_the_deployed_definition_not_the_stored_one() throws TechnicalException {
        given_deployable_api(ApiType.PROXY, PLAIN_PROPERTY_DEFINITION);
        updatedApi.setType(ApiType.PROXY);
        updatedApi.setDefinition(ENCRYPTED_PROPERTY_DEFINITION);
        updatedApi.setDeployedAt(new Date(0));

        apiStateService.deploy(GraviteeContext.getExecutionContext(), updatedApi, USER_NAME, new ApiDeploymentEntity());

        verify_encrypted_properties_access_audited();
    }

    @Test
    public void should_keep_the_access_audit_when_the_deploy_notification_fails() throws TechnicalException {
        given_deployable_api(ApiType.PROXY, ENCRYPTED_PROPERTY_DEFINITION);
        doThrow(new IllegalStateException("notifier down")).when(apiNotificationService).triggerDeployNotification(any(), any());

        assertThrows(IllegalStateException.class, () ->
            apiStateService.deploy(GraviteeContext.getExecutionContext(), API_ID, USER_NAME, new ApiDeploymentEntity())
        );

        verify_encrypted_properties_access_audited();
    }

    @Test
    public void should_not_audit_an_api_without_encrypted_property() throws TechnicalException {
        given_deployable_api(ApiType.PROXY, PLAIN_PROPERTY_DEFINITION);

        apiStateService.deploy(GraviteeContext.getExecutionContext(), API_ID, USER_NAME, new ApiDeploymentEntity());

        verify(auditService, never()).createApiAuditLog(any(), any(), any());
    }

    @Test
    public void should_not_audit_an_api_without_properties() throws TechnicalException {
        given_deployable_api(ApiType.PROXY, "{}");

        apiStateService.deploy(GraviteeContext.getExecutionContext(), API_ID, USER_NAME, new ApiDeploymentEntity());

        verify(auditService, never()).createApiAuditLog(any(), any(), any());
    }

    @Test
    public void should_not_audit_a_v2_api() throws Exception {
        Proxy proxy = new Proxy();
        proxy.setVirtualHosts(List.of(new VirtualHost("/v2")));
        io.gravitee.definition.model.Api v2Definition = new io.gravitee.definition.model.Api();
        v2Definition.setDefinitionVersion(DefinitionVersion.V2);
        v2Definition.setProxy(proxy);
        v2Definition.setProperties(new Properties(List.of(new Property("secret", "cipher", true))));
        given_deployable_api(ApiType.PROXY, objectMapper.writeValueAsString(v2Definition));
        api.setDefinitionVersion(DefinitionVersion.V2);

        GenericApiEntity deployed = apiStateService.deploy(
            GraviteeContext.getExecutionContext(),
            API_ID,
            USER_NAME,
            new ApiDeploymentEntity()
        );

        assertTrue(((ApiEntity) deployed).getPropertyList().getFirst().isEncrypted());
        verify(auditService, never()).createApiAuditLog(any(), any(), any());
    }

    @Test
    public void should_not_audit_a_rejected_deploy() {
        api.setDefinition(ENCRYPTED_PROPERTY_DEFINITION);
        when(apiValidationService.canDeploy(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(false);
        when(apiSearchService.findRepositoryApiById(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(api);

        assertThrows(ApiNotDeployableException.class, () ->
            apiStateService.deploy(GraviteeContext.getExecutionContext(), API_ID, USER_NAME, new ApiDeploymentEntity())
        );

        verify(auditService, never()).createApiAuditLog(any(), any(), any());
    }

    @Test
    public void should_audit_encrypted_properties_refresh_when_redeploying_with_synced_dynamic_properties() throws TechnicalException {
        given_deployable_api(ApiType.PROXY, ENCRYPTED_PROPERTY_DEFINITION);
        updatedApi.setType(ApiType.PROXY);
        updatedApi.setDefinition(ENCRYPTED_PROPERTY_DEFINITION);

        apiStateService.redeployWithSyncedDynamicProperties(
            GraviteeContext.getExecutionContext(),
            updatedApi,
            USER_NAME,
            new ApiDeploymentEntity("http-dynamic-properties sync")
        );

        verify(eventService).createApiEvent(any(), anySet(), anyString(), eq(EventType.PUBLISH_API), same(updatedApi), anyMap());
        verify_encrypted_properties_audited(API_ENCRYPTED_PROPERTIES_REFRESHED);
        verify(auditService, never()).createApiAuditLog(
            any(),
            argThat(auditLogData -> auditLogData.getEvent() == API_ENCRYPTED_PROPERTIES_ACCESSED),
            any()
        );
    }

    @Test
    public void should_not_audit_a_redeploy_with_synced_dynamic_properties_of_an_api_without_encrypted_property()
        throws TechnicalException {
        given_deployable_api(ApiType.PROXY, PLAIN_PROPERTY_DEFINITION);
        updatedApi.setType(ApiType.PROXY);
        updatedApi.setDefinition(PLAIN_PROPERTY_DEFINITION);

        apiStateService.redeployWithSyncedDynamicProperties(
            GraviteeContext.getExecutionContext(),
            updatedApi,
            USER_NAME,
            new ApiDeploymentEntity("http-dynamic-properties sync")
        );

        verify(auditService, never()).createApiAuditLog(any(), any(), any());
    }

    @Test
    public void should_throw_technical_exception_during_update() throws TechnicalException {
        assertThrows(TechnicalManagementException.class, () -> {
            when(apiValidationService.canDeploy(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(true);
            when(apiSearchService.findRepositoryApiById(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(api);
            when(apiRepository.update(api)).thenThrow(new TechnicalException());
            apiStateService.deploy(GraviteeContext.getExecutionContext(), API_ID, "some-user", new ApiDeploymentEntity());
        });
    }

    @Test
    public void should_throw_not_found_exception_during_get_by_id() {
        assertThrows(ApiNotFoundException.class, () -> {
            when(apiSearchService.findRepositoryApiById(GraviteeContext.getExecutionContext(), API_ID)).thenThrow(
                new ApiNotFoundException(API_ID)
            );
            apiStateService.deploy(GraviteeContext.getExecutionContext(), API_ID, "some-user", new ApiDeploymentEntity());
        });
    }

    @Test
    public void shouldAddDeploymentLabelAndIncrementDeploymentNumber() throws Exception {
        ExecutionContext executionContext = mock(ExecutionContext.class);
        EventLatestRepository eventLatestRepository = mock(EventLatestRepository.class);
        ApiDeploymentEntity deploymentEntity = new ApiDeploymentEntity();
        deploymentEntity.setDeploymentLabel("Release v1.0");
        Event mockEvent = new Event();
        mockEvent.setProperties(Map.of(Event.EventProperties.DEPLOYMENT_NUMBER.getValue(), "5"));
        when(eventLatestRepository.search(any(EventCriteria.class), eq(Event.EventProperties.API_ID), eq(0L), eq(1L))).thenReturn(
            List.of(mockEvent)
        );
        Map<String, String> props = new HashMap<>();

        ApiStateServiceImpl impl = new ApiStateServiceImpl(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            eventLatestRepository,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
        Method method = ApiStateServiceImpl.class.getDeclaredMethod(
            "addDeploymentLabelToProperties",
            ExecutionContext.class,
            String.class,
            Map.class,
            ApiDeploymentEntity.class
        );
        method.setAccessible(true);
        method.invoke(impl, executionContext, "api-id", props, deploymentEntity);
        assertEquals("6", props.get(Event.EventProperties.DEPLOYMENT_NUMBER.getValue()));
        assertEquals("Release v1.0", props.get(Event.EventProperties.DEPLOYMENT_LABEL.getValue()));
    }

    private void given_deployable_api(ApiType type, String definition) throws TechnicalException {
        api.setType(type);
        api.setDefinition(definition);
        when(apiValidationService.canDeploy(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(true);
        when(apiSearchService.findRepositoryApiById(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(api);
        when(apiRepository.update(api)).thenReturn(api);
    }

    private void verify_encrypted_properties_access_audited() {
        verify_encrypted_properties_audited(API_ENCRYPTED_PROPERTIES_ACCESSED);
    }

    private void verify_encrypted_properties_audited(Api.AuditEvent event) {
        verify(auditService).createApiAuditLog(
            eq(GraviteeContext.getExecutionContext()),
            argThat(
                auditLogData ->
                    auditLogData.getEvent() == event &&
                    USER_NAME.equals(auditLogData.getUser()) &&
                    auditLogData.getProperties().equals(Map.of(ENCRYPTED, "true")) &&
                    auditLogData.getCreatedAt().equals(api.getDeployedAt()) &&
                    auditLogData.getOldValue() == null &&
                    auditLogData.getNewValue() == null &&
                    ACCESS_PATCH.equals(auditLogData.getPatch())
            ),
            eq(API_ID)
        );
    }
}
