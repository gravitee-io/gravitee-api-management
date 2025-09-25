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

import static io.gravitee.rest.api.model.EventType.PUBLISH_API;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.event.EventManager;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.rest.api.model.EventEntity;
import io.gravitee.rest.api.model.EventQuery;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.v4.api.GenericApiEntity;
import io.gravitee.rest.api.service.*;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.converter.ApiConverter;
import io.gravitee.rest.api.service.converter.CategoryMapper;
import io.gravitee.rest.api.service.event.ApiEvent;
import io.gravitee.rest.api.service.exceptions.ApiNotDeployableException;
import io.gravitee.rest.api.service.exceptions.ApiNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import io.gravitee.rest.api.service.processor.SynchronizationService;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.v4.ApiNotificationService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.ApiStateService;
import io.gravitee.rest.api.service.v4.FlowService;
import io.gravitee.rest.api.service.v4.PlanSearchService;
import io.gravitee.rest.api.service.v4.PlanService;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import io.gravitee.rest.api.service.v4.mapper.ApiMapper;
import io.gravitee.rest.api.service.v4.mapper.GenericApiMapper;
import io.gravitee.rest.api.service.v4.validation.ApiValidationService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
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
public class ApiStateServiceImplTest {

    private static final String API_ID = "id-api";
    private static final String API_NAME = "myAPI";
    private static final String USER_NAME = "myUser";
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
    private PlanService planService;

    @Mock
    private EventService eventService;

    @Mock
    private EventLatestRepository eventLatestRepository;

    @Mock
    private FlowService flowService;

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
    private ApiConverter apiConverter;

    @Mock
    private PlanSearchService planSearchService;

    @InjectMocks
    private SynchronizationService synchronizationService = Mockito.spy(new SynchronizationService(this.objectMapper));

    private ExecutionContext executionContext = GraviteeContext.getExecutionContext();

    @Mock
    private EventManager eventManager;

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
        ApiMapper apiMapper = new ApiMapper(
            new ObjectMapper(),
            planService,
            flowService,
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
            eventManager
        );
        reset(searchEngineService);
        UserEntity admin = new UserEntity();
        admin.setId(USER_NAME);

        api = new Api();
        api.setId(API_ID);
        api.setName(API_NAME);
        api.setEnvironmentId(executionContext.getEnvironmentId());
        api.setDefinitionVersion(DefinitionVersion.V4);
        api.setType(ApiType.PROXY);

        updatedApi = new Api(api);

        when(apiMetadataService.fetchMetadataForApi(any(ExecutionContext.class), any(GenericApiEntity.class))).then(invocation ->
            invocation.getArgument(1)
        );
    }

    @Test
    public void shouldThrowExceptionWhenNoPlanPublished() throws TechnicalException {
        when(apiValidationService.canDeploy(executionContext, API_ID)).thenReturn(false);

        assertThrows(ApiNotDeployableException.class, () -> apiStateService.start(executionContext, API_ID, USER_NAME));
    }

    @Test
    public void shouldStartApiForTheFirstTime() throws TechnicalException {
        when(apiValidationService.canDeploy(executionContext, API_ID)).thenReturn(true);

        api.setApiLifecycleState(ApiLifecycleState.CREATED);
        when(apiValidationService.canDeploy(executionContext, API_ID)).thenReturn(true);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(apiSearchService.findRepositoryApiById(executionContext, API_ID)).thenReturn(api);

        final EventQuery query = new EventQuery();
        query.setApi(API_ID);
        query.setTypes(singleton(PUBLISH_API));
        query.setEnvironmentIds(Set.of(executionContext.getEnvironmentId()));
        query.setOrganizationIds(Set.of(executionContext.getOrganizationId()));
        when(eventService.search(executionContext, query)).thenReturn(emptyList());

        updatedApi.setLifecycleState(LifecycleState.STARTED);
        when(apiRepository.update(any())).thenReturn(updatedApi);

        apiStateService.start(executionContext, API_ID, USER_NAME);

        verify(apiRepository, times(2)).update(argThat(api -> api.getLifecycleState().equals(LifecycleState.STARTED)));

        Map<String, String> properties = new HashMap<>();
        properties.put(Event.EventProperties.USER.getValue(), USER_NAME);
        properties.put(Event.EventProperties.DEPLOYMENT_NUMBER.getValue(), "1");

        verify(eventService).createApiEvent(
            eq(executionContext),
            eq(singleton(GraviteeContext.getCurrentEnvironment())),
            eq(GraviteeContext.getCurrentOrganization()),
            eq(EventType.PUBLISH_API),
            argThat((ArgumentMatcher<Api>) argApi -> argApi.getId().equals(API_ID)),
            eq(properties)
        );

        verify(apiNotificationService, times(1)).triggerDeployNotification(
            eq(executionContext),
            argThat(argApi -> argApi.getId().equals(API_ID))
        );
        verify(apiNotificationService, times(1)).triggerStartNotification(
            eq(executionContext),
            argThat(argApi -> argApi.getId().equals(API_ID))
        );
    }

    @Test
    public void shouldReStartApi() throws TechnicalException {
        when(apiValidationService.canDeploy(executionContext, API_ID)).thenReturn(true);

        api.setApiLifecycleState(ApiLifecycleState.CREATED);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        final EventEntity event = new EventEntity();
        event.setType(PUBLISH_API);
        event.setPayload("{ \"id\": \"" + API_ID + "\"}");
        final EventQuery query = new EventQuery();
        query.setApi(API_ID);
        query.setTypes(singleton(PUBLISH_API));
        query.setEnvironmentIds(Set.of(executionContext.getEnvironmentId()));
        query.setOrganizationIds(Set.of(executionContext.getOrganizationId()));
        when(eventService.search(executionContext, query)).thenReturn(singleton(event));

        updatedApi.setLifecycleState(LifecycleState.STARTED);
        when(apiRepository.update(any())).thenReturn(updatedApi);

        apiStateService.start(executionContext, API_ID, USER_NAME);

        verify(apiRepository, times(1)).update(argThat(api -> api.getLifecycleState().equals(LifecycleState.STARTED)));

        Map<String, String> properties = new HashMap<>();
        properties.put(Event.EventProperties.USER.getValue(), USER_NAME);
        properties.put(Event.EventProperties.DEPLOYMENT_NUMBER.getValue(), "0");

        verify(eventService).createApiEvent(
            eq(executionContext),
            eq(singleton(GraviteeContext.getCurrentEnvironment())),
            eq(GraviteeContext.getCurrentOrganization()),
            eq(EventType.START_API),
            argThat((ArgumentMatcher<Api>) argApi -> argApi.getId().equals(API_ID)),
            eq(properties)
        );

        verify(apiNotificationService, times(1)).triggerStartNotification(
            eq(executionContext),
            argThat(argApi -> argApi.getId().equals(API_ID))
        );
    }

    @Test
    public void shouldStartApiWithKubernetesOrigin() throws TechnicalException {
        when(apiValidationService.canDeploy(executionContext, API_ID)).thenReturn(true);

        api.setApiLifecycleState(ApiLifecycleState.CREATED);
        api.setOrigin(Api.ORIGIN_KUBERNETES);
        when(apiValidationService.canDeploy(executionContext, API_ID)).thenReturn(true);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(apiSearchService.findRepositoryApiById(executionContext, API_ID)).thenReturn(api);

        final EventQuery query = new EventQuery();
        query.setApi(API_ID);
        query.setTypes(singleton(PUBLISH_API));
        query.setEnvironmentIds(Set.of(executionContext.getEnvironmentId()));
        query.setOrganizationIds(Set.of(executionContext.getOrganizationId()));
        when(eventService.search(executionContext, query)).thenReturn(emptyList());

        updatedApi.setLifecycleState(LifecycleState.STARTED);
        when(apiRepository.update(any())).thenReturn(updatedApi);

        apiStateService.start(executionContext, API_ID, USER_NAME);

        verify(apiRepository, times(2)).update(argThat(api -> api.getLifecycleState().equals(LifecycleState.STARTED)));

        Map<String, String> properties = new HashMap<>();
        properties.put(Event.EventProperties.USER.getValue(), USER_NAME);
        properties.put(Event.EventProperties.DEPLOYMENT_NUMBER.getValue(), "1");

        verify(eventService).createApiEvent(
            eq(executionContext),
            eq(singleton(GraviteeContext.getCurrentEnvironment())),
            eq(GraviteeContext.getCurrentOrganization()),
            eq(EventType.PUBLISH_API),
            argThat((ArgumentMatcher<Api>) argApi -> argApi.getId().equals(API_ID)),
            eq(properties)
        );

        verify(apiNotificationService, times(1)).triggerDeployNotification(
            eq(executionContext),
            argThat(argApi -> argApi.getId().equals(API_ID))
        );
        verify(apiNotificationService, times(1)).triggerStartNotification(
            eq(executionContext),
            argThat(argApi -> argApi.getId().equals(API_ID))
        );
    }

    @Test
    public void shouldStopApi() throws TechnicalException {
        api.setApiLifecycleState(ApiLifecycleState.CREATED);
        api.setLifecycleState(LifecycleState.STARTED);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        final EventEntity event = new EventEntity();
        event.setType(PUBLISH_API);
        event.setPayload("{ \"id\": \"" + API_ID + "\"}");
        final EventQuery query = new EventQuery();
        query.setApi(API_ID);
        query.setTypes(singleton(PUBLISH_API));
        query.setEnvironmentIds(Set.of(executionContext.getEnvironmentId()));
        query.setOrganizationIds(Set.of(executionContext.getOrganizationId()));
        when(eventService.search(executionContext, query)).thenReturn(singleton(event));

        updatedApi.setLifecycleState(LifecycleState.STOPPED);
        when(apiRepository.update(any())).thenReturn(updatedApi);

        apiStateService.stop(executionContext, API_ID, USER_NAME);

        verify(apiRepository, times(1)).update(argThat(api -> api.getLifecycleState().equals(LifecycleState.STOPPED)));

        Map<String, String> properties = new HashMap<>();
        properties.put(Event.EventProperties.USER.getValue(), USER_NAME);
        properties.put(Event.EventProperties.DEPLOYMENT_NUMBER.getValue(), "0");

        verify(eventService).createApiEvent(
            eq(executionContext),
            eq(singleton(GraviteeContext.getCurrentEnvironment())),
            eq(GraviteeContext.getCurrentOrganization()),
            eq(EventType.STOP_API),
            argThat((ArgumentMatcher<Api>) argApi -> argApi.getId().equals(API_ID)),
            eq(properties)
        );

        verify(apiNotificationService, times(1)).triggerStopNotification(
            eq(executionContext),
            argThat(argApi -> argApi.getId().equals(API_ID))
        );
    }

    @Test
    public void shouldStopApiWithKubernetesOrigin() throws TechnicalException {
        api.setApiLifecycleState(ApiLifecycleState.CREATED);
        api.setLifecycleState(LifecycleState.STARTED);
        api.setOrigin(Api.ORIGIN_KUBERNETES);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        final EventEntity event = new EventEntity();
        event.setType(PUBLISH_API);
        event.setPayload("{ \"id\": \"" + API_ID + "\"}");
        final EventQuery query = new EventQuery();
        query.setApi(API_ID);
        query.setTypes(singleton(PUBLISH_API));
        query.setEnvironmentIds(Set.of(executionContext.getEnvironmentId()));
        query.setOrganizationIds(Set.of(executionContext.getOrganizationId()));
        when(eventService.search(executionContext, query)).thenReturn(singleton(event));

        updatedApi.setLifecycleState(LifecycleState.STOPPED);
        when(apiRepository.update(any())).thenReturn(updatedApi);

        apiStateService.stop(executionContext, API_ID, USER_NAME);

        verify(apiRepository, times(1)).update(argThat(api -> api.getLifecycleState().equals(LifecycleState.STOPPED)));

        Map<String, String> properties = new HashMap<>();
        properties.put(Event.EventProperties.USER.getValue(), USER_NAME);
        properties.put(Event.EventProperties.DEPLOYMENT_NUMBER.getValue(), "0");

        verify(eventService).createApiEvent(
            eq(executionContext),
            eq(singleton(GraviteeContext.getCurrentEnvironment())),
            eq(GraviteeContext.getCurrentOrganization()),
            eq(EventType.STOP_API),
            argThat((ArgumentMatcher<Api>) argApi -> argApi.getId().equals(API_ID)),
            eq(properties)
        );

        verify(apiNotificationService, times(1)).triggerStopNotification(
            eq(executionContext),
            argThat(argApi -> argApi.getId().equals(API_ID))
        );
    }

    @Test
    public void shouldStartV4DynamicProperties() throws TechnicalException {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        updatedApi.setLifecycleState(LifecycleState.STARTED);
        boolean result = apiStateService.startV4DynamicProperties(API_ID);
        assertThat(result).isTrue();

        verify(apiRepository, times(1)).findById(API_ID);
        verify(eventManager, times(1)).publishEvent(ApiEvent.START_DYNAMIC_PROPERTY_V4, api);
    }

    @Test(expected = ApiNotFoundException.class)
    public void shouldThrowApiNotFoundException() throws Exception {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.empty());

        apiStateService.startV4DynamicProperties(API_ID);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldThrowTechnicalManagementExceptionOnTechnicalException() throws Exception {
        when(apiRepository.findById(API_ID)).thenThrow(new TechnicalException("db error"));

        apiStateService.startV4DynamicProperties(API_ID);
    }

    @Test
    public void shouldStopV4DynamicPropertiesSuccessfully() throws Exception {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        boolean result = apiStateService.stopV4DynamicProperties(API_ID);

        assertThat(result).isTrue();
        verify(apiRepository, times(1)).findById(API_ID);
        verify(eventManager, times(1)).publishEvent(ApiEvent.STOP_DYNAMIC_PROPERTY_V4, api);
    }

    @Test(expected = ApiNotFoundException.class)
    public void shouldThrowApiNotFoundExceptionWhenApiDoesNotExist() throws Exception {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.empty());

        apiStateService.stopV4DynamicProperties(API_ID);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldThrowTechnicalManagementExceptionOnRepositoryFailure() throws Exception {
        when(apiRepository.findById(API_ID)).thenThrow(new TechnicalException("db failure"));

        apiStateService.stopV4DynamicProperties(API_ID);
    }

    @Test
    public void should_start_v2_dynamic_properties_successfully() throws Exception {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        boolean result = apiStateService.startV2DynamicProperties(API_ID);

        assertThat(result).isTrue();
        verify(apiRepository, times(1)).findById(API_ID);
        verify(eventManager, times(1)).publishEvent(ApiEvent.START_DYNAMIC_PROPERTY_V2, api);
    }

    @Test(expected = ApiNotFoundException.class)
    public void should_throw_ApiNotFoundException_when_api_not_found() throws Exception {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.empty());

        apiStateService.startV2DynamicProperties(API_ID);
    }

    @Test(expected = TechnicalManagementException.class)
    public void should_throw_TechnicalManagementException_on_repository_failure() throws Exception {
        when(apiRepository.findById(API_ID)).thenThrow(new TechnicalException("db error"));

        apiStateService.startV2DynamicProperties(API_ID);
    }

    @Test
    public void should_stop_v2_dynamic_properties_successfully() throws Exception {
        // Arrange
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        // Act
        boolean result = apiStateService.stopV2DynamicProperties(API_ID);

        // Assert
        assertThat(result).isTrue();
        verify(apiRepository, times(1)).findById(API_ID);
        verify(eventManager, times(1)).publishEvent(ApiEvent.STOP_DYNAMIC_PROPERTY_V2, api);
    }

    @Test(expected = ApiNotFoundException.class)
    public void should_throw_ApiNotFoundException_when_api_not_found_v2() throws Exception {
        // Arrange
        when(apiRepository.findById(API_ID)).thenReturn(Optional.empty());

        // Act
        apiStateService.stopV2DynamicProperties(API_ID);
    }

    @Test(expected = TechnicalManagementException.class)
    public void should_throw_TechnicalManagementException_on_repository_failure_v2() throws Exception {
        // Arrange
        when(apiRepository.findById(API_ID)).thenThrow(new TechnicalException("db error"));

        // Act
        apiStateService.stopV2DynamicProperties(API_ID);
    }
}
