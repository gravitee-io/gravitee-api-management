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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Event;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiDeploymentEntity;
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
import java.util.List;
import java.util.Map;
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

@RunWith(MockitoJUnitRunner.class)
public class ApiStateServiceImpl_DeployTest {

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
            planServiceV4,
            flowServiceV4,
            parameterService,
            workflowService,
            new CategoryMapper(categoryService)
        );
        GenericApiMapper genericApiMapper = new GenericApiMapper(apiMapper, apiConverter);
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
        reset(searchEngineService);
        UserEntity admin = new UserEntity();
        admin.setId(USER_NAME);

        api = new Api();
        api.setId(API_ID);
        api.setName(API_NAME);
        api.setEnvironmentId(GraviteeContext.getExecutionContext().getEnvironmentId());
        api.setDefinitionVersion(DefinitionVersion.V4);

        updatedApi = new Api(api);

        when(apiMetadataService.fetchMetadataForApi(any(ExecutionContext.class), any(GenericApiEntity.class)))
            .then(invocation -> invocation.getArgument(1));
    }

    @Test
    public void should_deploy_api_if_managed_by_kubernetes() throws TechnicalException {
        final Event previousPublishedEvent = new Event();
        previousPublishedEvent.setProperties(Map.of(Event.EventProperties.DEPLOYMENT_NUMBER.getValue(), "3"));

        when(apiValidationService.canDeploy(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(true);
        when(apiSearchService.findRepositoryApiById(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(api);
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
        final GenericApiEntity result = apiStateService.deploy(
            GraviteeContext.getExecutionContext(),
            API_ID,
            USER_NAME,
            apiDeploymentEntity
        );

        verify(eventService)
            .createApiEvent(
                any(ExecutionContext.class),
                anySet(),
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

    @Test(expected = ApiNotDeployableException.class)
    public void should_not_deploy_when_no_active_plan_for_api() {
        when(apiValidationService.canDeploy(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(false);
        apiStateService.deploy(GraviteeContext.getExecutionContext(), API_ID, "some-user", new ApiDeploymentEntity());
    }

    @Test
    public void should_deploy_api() throws TechnicalException {
        final Event previousPublishedEvent = new Event();
        previousPublishedEvent.setProperties(Map.of(Event.EventProperties.DEPLOYMENT_NUMBER.getValue(), "3"));

        when(apiValidationService.canDeploy(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(true);
        when(apiSearchService.findRepositoryApiById(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(api);
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
        final GenericApiEntity result = apiStateService.deploy(
            GraviteeContext.getExecutionContext(),
            API_ID,
            USER_NAME,
            apiDeploymentEntity
        );

        verify(eventService)
            .createApiEvent(
                any(ExecutionContext.class),
                anySet(),
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

    @Test(expected = TechnicalManagementException.class)
    public void should_throw_technical_exception_during_update() throws TechnicalException {
        when(apiValidationService.canDeploy(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(true);
        when(apiSearchService.findRepositoryApiById(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(api);
        when(apiRepository.update(api)).thenThrow(new TechnicalException());
        apiStateService.deploy(GraviteeContext.getExecutionContext(), API_ID, "some-user", new ApiDeploymentEntity());
    }

    @Test(expected = ApiNotFoundException.class)
    public void should_throw_not_found_exception_during_get_by_id() {
        when(apiSearchService.findRepositoryApiById(GraviteeContext.getExecutionContext(), API_ID))
            .thenThrow(new ApiNotFoundException(API_ID));
        apiStateService.deploy(GraviteeContext.getExecutionContext(), API_ID, "some-user", new ApiDeploymentEntity());
    }
}
