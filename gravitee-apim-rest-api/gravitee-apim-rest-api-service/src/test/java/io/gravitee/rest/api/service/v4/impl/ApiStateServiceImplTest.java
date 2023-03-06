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

import static io.gravitee.rest.api.model.EventType.PUBLISH_API;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.rest.api.model.EventEntity;
import io.gravitee.rest.api.model.EventQuery;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.service.AuditService;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.WorkflowService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.search.SearchEngineService;
import io.gravitee.rest.api.service.v4.ApiNotificationService;
import io.gravitee.rest.api.service.v4.ApiSearchService;
import io.gravitee.rest.api.service.v4.ApiStateService;
import io.gravitee.rest.api.service.v4.FlowService;
import io.gravitee.rest.api.service.v4.PlanService;
import io.gravitee.rest.api.service.v4.PrimaryOwnerService;
import io.gravitee.rest.api.service.v4.mapper.ApiMapper;
import io.gravitee.rest.api.service.v4.mapper.CategoryMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
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
    private FlowService flowService;

    @Mock
    private WorkflowService workflowService;

    @Mock
    private PrimaryOwnerService primaryOwnerService;

    @Mock
    private ApiNotificationService apiNotificationService;

    @Mock
    private ApiSearchService apiSearchService;

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
        apiStateService =
            new ApiStateServiceImpl(
                apiSearchService,
                apiRepository,
                apiMapper,
                apiNotificationService,
                primaryOwnerService,
                auditService,
                eventService,
                objectMapper
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
    }

    @Test
    public void shouldStartApiForTheFirstTime() throws TechnicalException {
        api.setApiLifecycleState(ApiLifecycleState.CREATED);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(apiSearchService.findRepositoryApiById(GraviteeContext.getExecutionContext(), API_ID)).thenReturn(api);

        final EventQuery query = new EventQuery();
        query.setApi(API_ID);
        query.setTypes(singleton(PUBLISH_API));
        when(eventService.search(GraviteeContext.getExecutionContext(), query)).thenReturn(emptyList());

        updatedApi.setLifecycleState(LifecycleState.STARTED);
        when(apiRepository.update(any())).thenReturn(updatedApi);

        apiStateService.start(GraviteeContext.getExecutionContext(), API_ID, USER_NAME);

        verify(apiRepository, times(2)).update(argThat(api -> api.getLifecycleState().equals(LifecycleState.STARTED)));

        Map<String, String> properties = new HashMap<>();
        properties.put(Event.EventProperties.API_ID.getValue(), api.getId());
        properties.put(Event.EventProperties.USER.getValue(), USER_NAME);
        properties.put(Event.EventProperties.DEPLOYMENT_NUMBER.getValue(), "1");

        verify(eventService)
            .createApiEvent(
                eq(GraviteeContext.getExecutionContext()),
                eq(singleton(GraviteeContext.getCurrentEnvironment())),
                eq(EventType.PUBLISH_API),
                argThat((ArgumentMatcher<Api>) argApi -> argApi.getId().equals(API_ID)),
                eq(properties)
            );

        verify(apiNotificationService, times(1))
            .triggerDeployNotification(eq(GraviteeContext.getExecutionContext()), argThat(argApi -> argApi.getId().equals(API_ID)));
        verify(apiNotificationService, times(1))
            .triggerStartNotification(eq(GraviteeContext.getExecutionContext()), argThat(argApi -> argApi.getId().equals(API_ID)));
    }

    @Test
    public void shouldReStartApi() throws TechnicalException {
        api.setApiLifecycleState(ApiLifecycleState.CREATED);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        final EventEntity event = new EventEntity();
        event.setType(PUBLISH_API);
        event.setPayload("{ \"id\": \"" + API_ID + "\"}");
        final EventQuery query = new EventQuery();
        query.setApi(API_ID);
        query.setTypes(singleton(PUBLISH_API));
        when(eventService.search(GraviteeContext.getExecutionContext(), query)).thenReturn(singleton(event));

        updatedApi.setLifecycleState(LifecycleState.STARTED);
        when(apiRepository.update(any())).thenReturn(updatedApi);

        apiStateService.start(GraviteeContext.getExecutionContext(), API_ID, USER_NAME);

        verify(apiRepository, times(1)).update(argThat(api -> api.getLifecycleState().equals(LifecycleState.STARTED)));

        Map<String, String> properties = new HashMap<>();
        properties.put(Event.EventProperties.API_ID.getValue(), API_ID);
        properties.put(Event.EventProperties.USER.getValue(), USER_NAME);

        verify(eventService)
            .createApiEvent(
                eq(GraviteeContext.getExecutionContext()),
                eq(singleton(GraviteeContext.getCurrentEnvironment())),
                eq(EventType.START_API),
                argThat((ArgumentMatcher<Api>) argApi -> argApi.getId().equals(API_ID)),
                eq(properties)
            );

        verify(apiNotificationService, times(1))
            .triggerStartNotification(eq(GraviteeContext.getExecutionContext()), argThat(argApi -> argApi.getId().equals(API_ID)));
    }

    @Test
    public void shouldStartApiWithKubernetesOrigin() throws TechnicalException {
        api.setApiLifecycleState(ApiLifecycleState.CREATED);
        api.setOrigin(Api.ORIGIN_KUBERNETES);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        updatedApi.setOrigin(Api.ORIGIN_KUBERNETES);
        when(apiRepository.update(any())).thenReturn(updatedApi);

        apiStateService.start(GraviteeContext.getExecutionContext(), API_ID, USER_NAME);

        verify(apiRepository, times(1)).update(argThat(api -> api.getLifecycleState().equals(LifecycleState.STARTED)));

        Map<String, String> properties = new HashMap<>();
        properties.put(Event.EventProperties.API_ID.getValue(), API_ID);
        properties.put(Event.EventProperties.USER.getValue(), USER_NAME);

        verifyNoInteractions(eventService);

        verify(apiNotificationService, times(1))
            .triggerStartNotification(eq(GraviteeContext.getExecutionContext()), argThat(argApi -> argApi.getId().equals(API_ID)));
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
        when(eventService.search(GraviteeContext.getExecutionContext(), query)).thenReturn(singleton(event));

        updatedApi.setLifecycleState(LifecycleState.STOPPED);
        when(apiRepository.update(any())).thenReturn(updatedApi);

        apiStateService.stop(GraviteeContext.getExecutionContext(), API_ID, USER_NAME);

        verify(apiRepository, times(1)).update(argThat(api -> api.getLifecycleState().equals(LifecycleState.STOPPED)));

        Map<String, String> properties = new HashMap<>();
        properties.put(Event.EventProperties.API_ID.getValue(), API_ID);
        properties.put(Event.EventProperties.USER.getValue(), USER_NAME);

        verify(eventService)
            .createApiEvent(
                eq(GraviteeContext.getExecutionContext()),
                eq(singleton(GraviteeContext.getCurrentEnvironment())),
                eq(EventType.STOP_API),
                argThat((ArgumentMatcher<Api>) argApi -> argApi.getId().equals(API_ID)),
                eq(properties)
            );

        verify(apiNotificationService, times(1))
            .triggerStopNotification(eq(GraviteeContext.getExecutionContext()), argThat(argApi -> argApi.getId().equals(API_ID)));
    }

    @Test
    public void shouldStopApiWithKubernetesOrigin() throws TechnicalException {
        api.setApiLifecycleState(ApiLifecycleState.CREATED);
        api.setLifecycleState(LifecycleState.STARTED);
        api.setOrigin(Api.ORIGIN_KUBERNETES);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        updatedApi.setLifecycleState(LifecycleState.STOPPED);
        updatedApi.setOrigin(Api.ORIGIN_KUBERNETES);
        when(apiRepository.update(any())).thenReturn(updatedApi);

        apiStateService.stop(GraviteeContext.getExecutionContext(), API_ID, USER_NAME);

        verify(apiRepository, times(1)).update(argThat(api -> api.getLifecycleState().equals(LifecycleState.STOPPED)));

        Map<String, String> properties = new HashMap<>();
        properties.put(Event.EventProperties.API_ID.getValue(), API_ID);
        properties.put(Event.EventProperties.USER.getValue(), USER_NAME);

        verifyNoInteractions(eventService);

        verify(apiNotificationService, times(1))
            .triggerStopNotification(eq(GraviteeContext.getExecutionContext()), argThat(argApi -> argApi.getId().equals(API_ID)));
    }
}
