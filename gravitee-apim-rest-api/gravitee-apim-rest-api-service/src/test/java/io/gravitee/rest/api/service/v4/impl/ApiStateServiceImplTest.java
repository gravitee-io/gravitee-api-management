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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.deployment.service_provider.ApiDeploymentGate;
import io.gravitee.common.event.EventManager;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.ApiLifecycleState;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.LifecycleState;
import io.gravitee.rest.api.model.EventEntity;
import io.gravitee.rest.api.model.EventQuery;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.model.UserEntity;
import io.gravitee.rest.api.model.api.ApiDeploymentEntity;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
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
    private ApiDeploymentGate deploymentValidator;

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
            eventManager,
            searchEngineService,
            deploymentValidator
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

        lenient()
            .when(apiMetadataService.fetchMetadataForApi(any(ExecutionContext.class), any(GenericApiEntity.class)))
            .then(invocation -> invocation.getArgument(1));
    }

    @Test
    public void shouldThrowExceptionWhenNoPlanPublished() {
        when(apiValidationService.canDeploy(executionContext, API_ID)).thenReturn(false);

        assertThatExceptionOfType(ApiNotDeployableException.class).isThrownBy(() ->
            apiStateService.start(executionContext, API_ID, USER_NAME)
        );
    }

    @Test
    public void shouldThrowWhenAValidatorRefusesTheDefinitionOnStart() throws TechnicalException {
        api = new Api();
        when(apiValidationService.canDeploy(executionContext, API_ID)).thenReturn(true);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        doThrow(new ApiNotDeployableException("Guardrail 'guardrail-1' is not deployed"))
            .when(deploymentValidator)
            .validateDeployable(any());

        assertThatExceptionOfType(ApiNotDeployableException.class).isThrownBy(() ->
            apiStateService.start(executionContext, API_ID, USER_NAME)
        );

        // The guard runs before the lifecycle is persisted.
        verify(apiRepository, never()).update(any());
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
        verify(searchEngineService, times(1)).index(eq(executionContext), argThat(argApi -> argApi.getId().equals(API_ID)), eq(false));
    }

    /**
     * A Guardrail change republishes the deployment the gateways run, published or started, under a number of its own
     * written to the latest event of the API ({@code DeployApiInternalFlowDomainService}): the deployment that follows
     * takes the number after it, whatever the type of that event.
     */
    @Test
    public void should_deploy_under_the_number_after_the_one_of_the_latest_event() throws TechnicalException {
        when(apiSearchService.findRepositoryApiById(executionContext, API_ID)).thenReturn(api);
        when(apiValidationService.canDeploy(executionContext, API_ID)).thenReturn(true);
        var criteria = givenTheLatestEvent(io.gravitee.repository.management.model.EventType.START_API, "8");

        apiStateService.deploy(executionContext, API_ID, USER_NAME, new ApiDeploymentEntity());

        assertSoftly(softly -> {
            softly.assertThat(deploymentNumberWritten(EventType.PUBLISH_API)).as("deployment number").isEqualTo("9");
            softly
                .assertThat(criteria.getValue().getTypes())
                .as("types of the latest event read")
                .contains(
                    io.gravitee.repository.management.model.EventType.PUBLISH_API,
                    io.gravitee.repository.management.model.EventType.START_API
                );
            softly
                .assertThat(criteria.getValue().getProperties())
                .as("properties of the latest event read")
                .containsEntry(Event.EventProperties.API_ID.getValue(), API_ID);
        });
    }

    /**
     * A gateway that missed the stop, the two falling within one synchronization, sees the start alone: it replaces
     * the API it runs only under another number, so a start keeping the number would leave a Guardrail changed while
     * the API was stopped unseen.
     */
    @Test
    public void should_start_under_the_number_after_the_one_of_the_latest_event() throws Exception {
        givenTheStoredDefinition(guardrailDefinition("guardrail-2"));
        givenTheLastPublishedDefinition(guardrailDefinition("guardrail-1"), "7");
        givenTheLatestEvent(io.gravitee.repository.management.model.EventType.STOP_API, "8");

        apiStateService.start(executionContext, API_ID, USER_NAME);

        assertThat(deploymentNumberWritten(EventType.START_API)).as("deployment number").isEqualTo("9");
    }

    /**
     * The latest event may be a Guardrail change republished on a started API, which is no published event: a stop
     * taking the number of the last published one would hand the deployment that follows a number already used.
     */
    @Test
    public void should_stop_under_the_number_of_the_latest_event() throws Exception {
        api.setApiLifecycleState(ApiLifecycleState.CREATED);
        api.setLifecycleState(LifecycleState.STARTED);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        updatedApi.setLifecycleState(LifecycleState.STOPPED);
        when(apiRepository.update(any())).thenReturn(updatedApi);
        givenTheLastPublishedDefinition(guardrailDefinition("guardrail-1"), "7");
        givenTheLatestEvent(io.gravitee.repository.management.model.EventType.START_API, "8");

        apiStateService.stop(executionContext, API_ID, USER_NAME);

        assertThat(deploymentNumberWritten(EventType.STOP_API)).as("deployment number").isEqualTo("8");
    }

    private ArgumentCaptor<EventCriteria> givenTheLatestEvent(
        io.gravitee.repository.management.model.EventType type,
        String deploymentNumber
    ) {
        var criteria = ArgumentCaptor.forClass(EventCriteria.class);
        when(eventLatestRepository.search(criteria.capture(), eq(Event.EventProperties.API_ID), eq(0L), eq(1L))).thenReturn(
            List.of(
                Event.builder()
                    .type(type)
                    .properties(
                        Map.of(
                            Event.EventProperties.API_ID.getValue(),
                            API_ID,
                            Event.EventProperties.DEPLOYMENT_NUMBER.getValue(),
                            deploymentNumber
                        )
                    )
                    .build()
            )
        );
        return criteria;
    }

    private String deploymentNumberWritten(EventType type) {
        var properties = ArgumentCaptor.<Map<String, String>>captor();
        verify(eventService).createApiEvent(eq(executionContext), any(), any(), eq(type), any(Api.class), properties.capture());
        return properties.getValue().get(Event.EventProperties.DEPLOYMENT_NUMBER.getValue());
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
        properties.put(Event.EventProperties.DEPLOYMENT_NUMBER.getValue(), "1");

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
        verify(searchEngineService, times(1)).index(eq(executionContext), argThat(argApi -> argApi.getId().equals(API_ID)), eq(false));
    }

    /**
     * A Guardrail change reaches the gateways as soon as it is written, without a deployment, so the last published
     * definition may predate it: restarting the API from that definition alone would silently drop the Guardrail.
     */
    @Test
    public void should_restart_an_api_with_the_internal_flow_it_stores_rather_than_the_published_one() throws Exception {
        givenTheStoredDefinition(guardrailDefinition("guardrail-2"));
        givenTheLastPublishedDefinition(guardrailDefinition("guardrail-1"));

        apiStateService.start(executionContext, API_ID, USER_NAME);

        var started = objectMapper.readTree(definitionStartedWith());
        assertSoftly(softly -> {
            softly
                .assertThat(started.at("/internalFlow/request/0/configuration/guardrailId").asText())
                .as("guardrail started")
                .isEqualTo("guardrail-2");
            softly.assertThat(started.at("/plans/0/id").asText()).as("plan of the published definition").isEqualTo("plan-published");
        });
    }

    /** The reverse: a Guardrail turned off since the last deployment must not come back on a restart. */
    @Test
    public void should_restart_an_api_without_the_internal_flow_it_no_longer_stores() throws Exception {
        givenTheStoredDefinition(guardrailDefinition(null));
        givenTheLastPublishedDefinition(guardrailDefinition("guardrail-1"));

        apiStateService.start(executionContext, API_ID, USER_NAME);

        assertThat(objectMapper.readTree(definitionStartedWith()).has("internalFlow")).as("internal flow started").isFalse();
    }

    private void givenTheStoredDefinition(String definition) throws TechnicalException {
        when(apiValidationService.canDeploy(executionContext, API_ID)).thenReturn(true);
        api.setApiLifecycleState(ApiLifecycleState.CREATED);
        api.setDefinition(definition);
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        updatedApi.setLifecycleState(LifecycleState.STARTED);
        when(apiRepository.update(any())).thenReturn(updatedApi);
    }

    private void givenTheLastPublishedDefinition(String definition) throws JsonProcessingException {
        givenTheLastPublishedDefinition(definition, null);
    }

    private void givenTheLastPublishedDefinition(String definition, String deploymentNumber) throws JsonProcessingException {
        var published = new Api(api);
        published.setDefinition(definition);
        final EventEntity event = new EventEntity();
        event.setType(PUBLISH_API);
        if (deploymentNumber != null) {
            event.setProperties(Map.of(Event.EventProperties.DEPLOYMENT_NUMBER.getValue(), deploymentNumber));
        }
        event.setPayload(objectMapper.writeValueAsString(published));
        final EventQuery query = new EventQuery();
        query.setApi(API_ID);
        query.setTypes(singleton(PUBLISH_API));
        query.setEnvironmentIds(Set.of(executionContext.getEnvironmentId()));
        query.setOrganizationIds(Set.of(executionContext.getOrganizationId()));
        when(eventService.search(executionContext, query)).thenReturn(singleton(event));
    }

    private String definitionStartedWith() {
        var started = ArgumentCaptor.forClass(Api.class);
        verify(eventService).createApiEvent(
            eq(executionContext),
            eq(singleton(GraviteeContext.getCurrentEnvironment())),
            eq(GraviteeContext.getCurrentOrganization()),
            eq(EventType.START_API),
            started.capture(),
            any()
        );
        return started.getValue().getDefinition();
    }

    private static String guardrailDefinition(String guardrailId) {
        var internalFlow = guardrailId == null
            ? ""
            : """
            "internalFlow": { "request": [ { "policy": "guardrail-run", "configuration": { "guardrailId": "%s" } } ] },
            """.formatted(guardrailId);
        return """
        { "id": "%s", "definitionVersion": "4.0.0", "type": "proxy", %s "plans": [ { "id": "plan-published" } ] }
        """.formatted(API_ID, internalFlow);
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
        verify(searchEngineService, times(1)).index(eq(executionContext), argThat(argApi -> argApi.getId().equals(API_ID)), eq(false));
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    public void shouldStopApi(boolean sendNotification) throws TechnicalException {
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

        if (sendNotification) {
            apiStateService.stop(executionContext, API_ID, USER_NAME);
        } else {
            apiStateService.stopWithoutNotification(executionContext, API_ID, USER_NAME);
        }

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

        if (sendNotification) {
            verify(apiNotificationService, times(1)).triggerStopNotification(
                eq(executionContext),
                argThat(argApi -> argApi.getId().equals(API_ID))
            );
        } else {
            verify(apiNotificationService, never()).triggerStopNotification(any(), any());
        }
        verify(searchEngineService, times(1)).index(eq(executionContext), argThat(argApi -> argApi.getId().equals(API_ID)), eq(false));
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
        verify(searchEngineService, times(1)).index(eq(executionContext), argThat(argApi -> argApi.getId().equals(API_ID)), eq(false));
    }

    @Test
    public void should_not_mark_the_audit_when_starting_an_api_holding_an_encrypted_property() throws Exception {
        when(apiValidationService.canDeploy(executionContext, API_ID)).thenReturn(true);
        api.setApiLifecycleState(ApiLifecycleState.CREATED);
        givenDefinitionHoldingAnEncryptedProperty();
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(eventService.search(eq(executionContext), any(EventQuery.class))).thenReturn(singleton(aPublishApiEvent()));
        updatedApi.setLifecycleState(LifecycleState.STARTED);
        when(apiRepository.update(any())).thenReturn(updatedApi);

        apiStateService.start(executionContext, API_ID, USER_NAME);

        verify(auditService).createApiAuditLog(
            eq(executionContext),
            argThat(data -> data.getEvent().equals(Api.AuditEvent.API_UPDATED) && data.getProperties().isEmpty()),
            eq(API_ID)
        );
    }

    @Test
    public void should_not_mark_the_audit_when_stopping_an_api_holding_an_encrypted_property() throws Exception {
        api.setApiLifecycleState(ApiLifecycleState.CREATED);
        api.setLifecycleState(LifecycleState.STARTED);
        givenDefinitionHoldingAnEncryptedProperty();
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));
        when(eventService.search(eq(executionContext), any(EventQuery.class))).thenReturn(singleton(aPublishApiEvent()));
        updatedApi.setLifecycleState(LifecycleState.STOPPED);
        when(apiRepository.update(any())).thenReturn(updatedApi);

        apiStateService.stop(executionContext, API_ID, USER_NAME);

        verify(auditService).createApiAuditLog(
            eq(executionContext),
            argThat(data -> data.getEvent().equals(Api.AuditEvent.API_UPDATED) && data.getProperties().isEmpty()),
            eq(API_ID)
        );
    }

    private void givenDefinitionHoldingAnEncryptedProperty() throws JsonProcessingException {
        var definition = new io.gravitee.definition.model.v4.Api();
        definition.setId(API_ID);
        definition.setProperties(List.of(Property.builder().key("secret").value("ciphertext").encrypted(true).build()));
        api.setDefinition(objectMapper.writeValueAsString(definition));
        updatedApi.setDefinition(api.getDefinition());
    }

    private static EventEntity aPublishApiEvent() {
        final EventEntity event = new EventEntity();
        event.setType(PUBLISH_API);
        event.setPayload("{ \"id\": \"" + API_ID + "\"}");
        return event;
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

    @Test
    public void shouldThrowApiNotFoundException() throws Exception {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ApiNotFoundException.class).isThrownBy(() -> apiStateService.startV4DynamicProperties(API_ID));
    }

    @Test
    public void shouldThrowTechnicalManagementExceptionOnTechnicalException() throws Exception {
        when(apiRepository.findById(API_ID)).thenThrow(new TechnicalException("db error"));

        assertThatExceptionOfType(TechnicalManagementException.class).isThrownBy(() -> apiStateService.startV4DynamicProperties(API_ID));
    }

    @Test
    public void shouldStopV4DynamicPropertiesSuccessfully() throws Exception {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        boolean result = apiStateService.stopV4DynamicProperties(API_ID);

        assertThat(result).isTrue();
        verify(apiRepository, times(1)).findById(API_ID);
        verify(eventManager, times(1)).publishEvent(ApiEvent.STOP_DYNAMIC_PROPERTY_V4, api);
    }

    @Test
    public void shouldThrowApiNotFoundExceptionWhenApiDoesNotExist() throws Exception {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ApiNotFoundException.class).isThrownBy(() -> apiStateService.stopV4DynamicProperties(API_ID));
    }

    @Test
    public void shouldThrowTechnicalManagementExceptionOnRepositoryFailure() throws Exception {
        when(apiRepository.findById(API_ID)).thenThrow(new TechnicalException("db failure"));

        assertThatExceptionOfType(TechnicalManagementException.class).isThrownBy(() -> apiStateService.stopV4DynamicProperties(API_ID));
    }

    @Test
    public void should_start_v2_dynamic_properties_successfully() throws Exception {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.of(api));

        boolean result = apiStateService.startV2DynamicProperties(API_ID);

        assertThat(result).isTrue();
        verify(apiRepository, times(1)).findById(API_ID);
        verify(eventManager, times(1)).publishEvent(ApiEvent.START_DYNAMIC_PROPERTY_V2, api);
    }

    @Test
    public void should_throw_ApiNotFoundException_when_api_not_found() throws Exception {
        when(apiRepository.findById(API_ID)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ApiNotFoundException.class).isThrownBy(() -> apiStateService.startV2DynamicProperties(API_ID));
    }

    @Test
    public void should_throw_TechnicalManagementException_on_repository_failure() throws Exception {
        when(apiRepository.findById(API_ID)).thenThrow(new TechnicalException("db error"));

        assertThatExceptionOfType(TechnicalManagementException.class).isThrownBy(() -> apiStateService.startV2DynamicProperties(API_ID));
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

    @Test
    public void should_throw_ApiNotFoundException_when_api_not_found_v2() throws Exception {
        // Arrange
        when(apiRepository.findById(API_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatExceptionOfType(ApiNotFoundException.class).isThrownBy(() -> apiStateService.stopV2DynamicProperties(API_ID));
    }

    @Test
    public void should_throw_TechnicalManagementException_on_repository_failure_v2() throws Exception {
        // Arrange
        when(apiRepository.findById(API_ID)).thenThrow(new TechnicalException("db error"));

        // Act & Assert
        assertThatExceptionOfType(TechnicalManagementException.class).isThrownBy(() -> apiStateService.stopV2DynamicProperties(API_ID));
    }
}
