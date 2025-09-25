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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.apim.core.flow.crud_service.FlowCrudService;
import io.gravitee.apim.core.plan.model.Plan;
import io.gravitee.apim.core.plan.query_service.PlanQueryService;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.v4.ApiType;
import io.gravitee.definition.model.v4.nativeapi.NativeFlow;
import io.gravitee.definition.model.v4.nativeapi.NativePlan;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.model.EventEntity;
import io.gravitee.rest.api.model.NewEventEntity;
import io.gravitee.rest.api.model.OrganizationEntity;
import io.gravitee.rest.api.model.PlanEntity;
import io.gravitee.rest.api.model.PlanSecurityType;
import io.gravitee.rest.api.model.PlanStatus;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.gravitee.rest.api.service.converter.PlanConverter;
import io.gravitee.rest.api.service.exceptions.EventNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author Titouan COMPIEGNE (titouan.compiegne at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class EventServiceTest {

    private static final String ORGANIZATION_ID = GraviteeContext.getCurrentOrganization();

    private static final String EVENT_ID = "id-event";
    private static final String EVENT_PAYLOAD = "{}";
    private static final String EVENT_USERNAME = "admin";
    private static final String EVENT_ORIGIN = "localhost";
    private static final String API_ID = "id-api";
    private static final String ENVIRONMENT_ID = "DEFAULT";
    private static final Map<String, String> EVENT_PROPERTIES = Map.of(
        Event.EventProperties.API_ID.getValue(),
        API_ID,
        Event.EventProperties.USER.getValue(),
        EVENT_USERNAME,
        Event.EventProperties.ORIGIN.getValue(),
        EVENT_ORIGIN
    );

    @Before
    public void setup() {
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);
    }

    @After
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @InjectMocks
    private EventServiceImpl eventService;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventLatestRepository eventLatestRepository;

    @Mock
    private NewEventEntity newEvent;

    @Mock
    private Event event;

    @Mock
    private Event event2;

    @Mock
    private UserService userService;

    @Mock
    private PlanService planService;

    @Mock
    private FlowService flowService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PlanQueryService planQueryService;

    @Mock
    private FlowCrudService flowCrudService;

    @Test
    public void shouldCreateEventWithPublishApiEventType() throws TechnicalException {
        when(event.getType()).thenReturn(EventType.PUBLISH_API);
        when(event.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event.getProperties()).thenReturn(EVENT_PROPERTIES);
        when(eventRepository.create(any())).thenReturn(event);

        when(newEvent.getType()).thenReturn(io.gravitee.rest.api.model.EventType.PUBLISH_API);
        when(newEvent.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(newEvent.getProperties()).thenReturn(EVENT_PROPERTIES);

        final EventEntity eventEntity = eventService.createNewEventEntity(
            GraviteeContext.getExecutionContext(),
            Set.of(GraviteeContext.getCurrentEnvironment()),
            GraviteeContext.getCurrentOrganization(),
            newEvent
        );

        assertNotNull(eventEntity);
        assertEquals(EventType.PUBLISH_API.toString(), eventEntity.getType().toString());
        assertEquals(EVENT_PAYLOAD, eventEntity.getPayload());
        assertEquals(EVENT_USERNAME, eventEntity.getProperties().get(Event.EventProperties.USER.getValue()));
    }

    @Test
    public void shouldFindById() throws TechnicalException {
        when(event.getType()).thenReturn(EventType.PUBLISH_API);
        when(event.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event.getProperties()).thenReturn(EVENT_PROPERTIES);

        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event));

        final EventEntity eventEntity = eventService.findById(GraviteeContext.getExecutionContext(), EVENT_ID);

        assertNotNull(eventEntity);
    }

    @Test(expected = TechnicalManagementException.class)
    public void shouldNotFindByIdBecauseTechnicalException() throws TechnicalException {
        when(eventRepository.findById(any(String.class))).thenThrow(TechnicalException.class);

        eventService.findById(GraviteeContext.getExecutionContext(), EVENT_ID);
    }

    @Test(expected = EventNotFoundException.class)
    public void shouldNotFindByNameBecauseNotExists() throws TechnicalException {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.empty());

        eventService.findById(GraviteeContext.getExecutionContext(), EVENT_ID);
    }

    @Test
    public void shouldDeleteApiEvents() throws TechnicalException {
        eventService.deleteApiEvents(API_ID);

        verify(eventLatestRepository).delete(API_ID);
    }

    @Test
    public void shouldSearchNoResults() {
        when(
            eventRepository.search(
                EventCriteria.builder()
                    .from(1420070400000L)
                    .to(1422748800000L)
                    .type(EventType.START_API)
                    .environment(ENVIRONMENT_ID)
                    .build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build()
            )
        ).thenReturn(new Page<>(List.of(), 0, 0, 0));

        Page<EventEntity> eventPageEntity = eventService.search(
            GraviteeContext.getExecutionContext(),
            List.of(io.gravitee.rest.api.model.EventType.START_API),
            null,
            1420070400000L,
            1422748800000L,
            0,
            10,
            List.of(GraviteeContext.getCurrentEnvironment())
        );
        assertEquals(0L, eventPageEntity.getTotalElements());
    }

    @Test
    public void shouldSearchBySingleEventType() {
        when(event.getId()).thenReturn("event1");
        when(event.getType()).thenReturn(EventType.START_API);
        when(event.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event.getProperties()).thenReturn(EVENT_PROPERTIES);

        when(event2.getId()).thenReturn("event2");
        when(event2.getType()).thenReturn(EventType.START_API);
        when(event2.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event2.getProperties()).thenReturn(EVENT_PROPERTIES);

        when(
            eventRepository.search(
                EventCriteria.builder()
                    .from(1420070400000L)
                    .to(1422748800000L)
                    .type(EventType.START_API)
                    .environment(ENVIRONMENT_ID)
                    .build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build()
            )
        ).thenReturn(new Page<>(List.of(event, event2), 0, 2, 2));

        Page<EventEntity> eventPageEntity = eventService.search(
            GraviteeContext.getExecutionContext(),
            List.of(io.gravitee.rest.api.model.EventType.START_API),
            null,
            1420070400000L,
            1422748800000L,
            0,
            10,
            List.of(GraviteeContext.getCurrentEnvironment())
        );

        assertEquals(2L, eventPageEntity.getTotalElements());
        assertEquals("event1", eventPageEntity.getContent().get(0).getId());
    }

    @Test
    public void shouldSearchByMultipleEventType() {
        when(event.getId()).thenReturn("event1");
        when(event.getType()).thenReturn(EventType.START_API);
        when(event.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event.getProperties()).thenReturn(EVENT_PROPERTIES);

        when(event2.getId()).thenReturn("event2");
        when(event2.getType()).thenReturn(EventType.STOP_API);
        when(event2.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event2.getProperties()).thenReturn(EVENT_PROPERTIES);

        when(
            eventRepository.search(
                EventCriteria.builder()
                    .from(1420070400000L)
                    .to(1422748800000L)
                    .types(Set.of(EventType.START_API, EventType.STOP_API))
                    .environment(ENVIRONMENT_ID)
                    .build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build()
            )
        ).thenReturn(new Page<>(List.of(event, event2), 0, 2, 2));

        Page<EventEntity> eventPageEntity = eventService.search(
            GraviteeContext.getExecutionContext(),
            List.of(io.gravitee.rest.api.model.EventType.START_API, io.gravitee.rest.api.model.EventType.STOP_API),
            null,
            1420070400000L,
            1422748800000L,
            0,
            10,
            List.of(GraviteeContext.getCurrentEnvironment())
        );

        assertEquals(2L, eventPageEntity.getTotalElements());
        assertEquals("event1", eventPageEntity.getContent().get(0).getId());
    }

    @Test
    public void shouldSearchByAPIId() {
        Map<String, Object> values = Map.of(Event.EventProperties.API_ID.getValue(), "id-api");

        when(event.getId()).thenReturn("event1");
        when(event.getType()).thenReturn(EventType.START_API);
        when(event.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event.getProperties()).thenReturn(EVENT_PROPERTIES);

        when(event2.getId()).thenReturn("event2");
        when(event2.getType()).thenReturn(EventType.STOP_API);
        when(event2.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event2.getProperties()).thenReturn(EVENT_PROPERTIES);

        when(
            eventRepository.search(
                EventCriteria.builder()
                    .from(1420070400000L)
                    .to(1422748800000L)
                    .property(Event.EventProperties.API_ID.getValue(), "id-api")
                    .environments(List.of(ENVIRONMENT_ID))
                    .build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build()
            )
        ).thenReturn(new Page<>(List.of(event, event2), 0, 2, 2));

        Page<EventEntity> eventPageEntity = eventService.search(
            GraviteeContext.getExecutionContext(),
            null,
            values,
            1420070400000L,
            1422748800000L,
            0,
            10,
            List.of(GraviteeContext.getCurrentEnvironment())
        );

        assertEquals(2L, eventPageEntity.getTotalElements());
        assertEquals("event1", eventPageEntity.getContent().get(0).getId());
    }

    @Test
    public void shouldSearchByMixProperties() {
        Map<String, Object> values = Map.of(Event.EventProperties.API_ID.getValue(), "id-api");

        when(event.getId()).thenReturn("event1");
        when(event.getType()).thenReturn(EventType.START_API);
        when(event.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event.getProperties()).thenReturn(EVENT_PROPERTIES);

        when(event2.getId()).thenReturn("event2");
        when(event2.getType()).thenReturn(EventType.STOP_API);
        when(event2.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event2.getProperties()).thenReturn(EVENT_PROPERTIES);

        when(
            eventRepository.search(
                EventCriteria.builder()
                    .from(1420070400000L)
                    .to(1422748800000L)
                    .property(Event.EventProperties.API_ID.getValue(), "id-api")
                    .types(Set.of(EventType.START_API, EventType.STOP_API))
                    .environment(ENVIRONMENT_ID)
                    .build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build()
            )
        ).thenReturn(new Page<>(List.of(event, event2), 0, 2, 2));

        Page<EventEntity> eventPageEntity = eventService.search(
            GraviteeContext.getExecutionContext(),
            List.of(io.gravitee.rest.api.model.EventType.START_API, io.gravitee.rest.api.model.EventType.STOP_API),
            values,
            1420070400000L,
            1422748800000L,
            0,
            10,
            List.of(GraviteeContext.getCurrentEnvironment())
        );

        assertEquals(2L, eventPageEntity.getTotalElements());
        assertEquals("event1", eventPageEntity.getContent().get(0).getId());
    }

    @Test
    public void createDynamicDictionaryEvent_shouldCreateEvent_withId() throws TechnicalException {
        when(eventRepository.create(any())).thenAnswer(i -> i.getArguments()[0]);

        eventService.createDynamicDictionaryEvent(
            GraviteeContext.getExecutionContext(),
            Set.of(ENVIRONMENT_ID),
            ORGANIZATION_ID,
            io.gravitee.rest.api.model.EventType.START_DICTIONARY,
            "dictionaryId"
        );

        verify(eventRepository).create(
            argThat(e -> {
                assertEquals(Set.of(ENVIRONMENT_ID), e.getEnvironments());
                assertEquals(Set.of(ORGANIZATION_ID), e.getOrganizations());
                assertTrue(e.getPayload() == null && e.getProperties().containsKey(Event.EventProperties.DICTIONARY_ID.getValue()));
                return true;
            })
        );
        verify(eventLatestRepository).createOrUpdate(
            argThat(e -> {
                assertEquals(Set.of(ENVIRONMENT_ID), e.getEnvironments());
                assertEquals(Set.of(ORGANIZATION_ID), e.getOrganizations());
                assertTrue(e.getPayload() == null && e.getProperties().containsKey(Event.EventProperties.DICTIONARY_ID.getValue()));
                return true;
            })
        );
        verifyNoMoreInteractions(eventRepository);
    }

    @Test
    public void createDictionaryApiEvent_shouldCreateEvent_withPayload() throws TechnicalException, JsonProcessingException {
        String jsonValue = "serialized json value";
        var dictionary = mock(io.gravitee.repository.management.model.Dictionary.class);
        when(objectMapper.writeValueAsString(dictionary)).thenReturn(jsonValue);
        when(eventRepository.create(any())).thenAnswer(i -> i.getArguments()[0]);

        eventService.createDictionaryEvent(
            GraviteeContext.getExecutionContext(),
            Set.of(ENVIRONMENT_ID),
            ORGANIZATION_ID,
            io.gravitee.rest.api.model.EventType.DEBUG_API,
            dictionary
        );

        verify(eventRepository).create(
            argThat(e -> {
                assertEquals(Set.of(ENVIRONMENT_ID), e.getEnvironments());
                assertEquals(Set.of(ORGANIZATION_ID), e.getOrganizations());
                assertTrue(
                    jsonValue.equals(e.getPayload()) && e.getProperties().containsKey(Event.EventProperties.DICTIONARY_ID.getValue())
                );
                return true;
            })
        );
        verify(eventLatestRepository).createOrUpdate(
            argThat(e -> {
                assertEquals(Set.of(ENVIRONMENT_ID), e.getEnvironments());
                assertEquals(Set.of(ORGANIZATION_ID), e.getOrganizations());
                assertTrue(
                    jsonValue.equals(e.getPayload()) && e.getProperties().containsKey(Event.EventProperties.DICTIONARY_ID.getValue())
                );
                return true;
            })
        );

        verifyNoMoreInteractions(eventRepository);
    }

    @Test
    public void createOrganizationEvent_shouldCreateEvent_withId() throws TechnicalException {
        when(eventRepository.create(any())).thenAnswer(i -> i.getArguments()[0]);

        eventService.createOrganizationEvent(
            GraviteeContext.getExecutionContext(),
            Set.of(ENVIRONMENT_ID),
            ORGANIZATION_ID,
            io.gravitee.rest.api.model.EventType.PUBLISH_ORGANIZATION,
            null
        );

        verify(eventRepository).create(
            argThat(e -> {
                assertEquals(Set.of(ENVIRONMENT_ID), e.getEnvironments());
                assertEquals(Set.of(ORGANIZATION_ID), e.getOrganizations());
                assertNull(e.getPayload());
                return true;
            })
        );
        verifyNoMoreInteractions(eventRepository);
        verifyNoInteractions(eventLatestRepository);
    }

    @Test
    public void createOrganizationEvent_shouldCreateEvent_withPayload() throws TechnicalException, JsonProcessingException {
        String jsonValue = "serialized json value";
        OrganizationEntity organization = mock(OrganizationEntity.class);
        when(objectMapper.writeValueAsString(organization)).thenReturn(jsonValue);
        when(eventRepository.create(any())).thenAnswer(i -> i.getArguments()[0]);

        eventService.createOrganizationEvent(
            GraviteeContext.getExecutionContext(),
            Set.of(ENVIRONMENT_ID),
            ORGANIZATION_ID,
            io.gravitee.rest.api.model.EventType.PUBLISH_ORGANIZATION,
            organization
        );

        verify(eventRepository).create(
            argThat(e -> {
                assertEquals(Set.of(ENVIRONMENT_ID), e.getEnvironments());
                assertEquals(Set.of(ORGANIZATION_ID), e.getOrganizations());
                assertTrue(
                    jsonValue.equals(e.getPayload()) && e.getProperties().containsKey(Event.EventProperties.ORGANIZATION_ID.getValue())
                );
                return true;
            })
        );

        verifyNoMoreInteractions(eventRepository);

        verify(eventLatestRepository).createOrUpdate(
            argThat(e -> {
                assertEquals(Set.of(ENVIRONMENT_ID), e.getEnvironments());
                assertEquals(Set.of(ORGANIZATION_ID), e.getOrganizations());
                assertTrue(
                    jsonValue.equals(e.getPayload()) && e.getProperties().containsKey(Event.EventProperties.ORGANIZATION_ID.getValue())
                );
                return true;
            })
        );
    }

    @Test
    public void createApiEvent_shouldCreateEvent_withoutPayload() throws TechnicalException {
        when(eventRepository.create(any())).thenAnswer(i -> i.getArguments()[0]);

        eventService.createApiEvent(
            GraviteeContext.getExecutionContext(),
            Set.of(ENVIRONMENT_ID),
            ORGANIZATION_ID,
            io.gravitee.rest.api.model.EventType.PUBLISH_API,
            (Api) null,
            Map.of()
        );

        verify(eventRepository).create(
            argThat(e -> {
                assertEquals(Set.of(ENVIRONMENT_ID), e.getEnvironments());
                assertEquals(Set.of(ORGANIZATION_ID), e.getOrganizations());
                assertNull(e.getPayload());
                return true;
            })
        );
        verifyNoMoreInteractions(eventRepository);
        verifyNoInteractions(eventLatestRepository);
    }

    @Test
    public void createApiEvent_shouldCreateEventFromId_withoutPayload() throws TechnicalException {
        when(eventRepository.create(any())).thenAnswer(i -> i.getArguments()[0]);

        eventService.createApiEvent(
            GraviteeContext.getExecutionContext(),
            Set.of(ENVIRONMENT_ID),
            ORGANIZATION_ID,
            io.gravitee.rest.api.model.EventType.PUBLISH_API,
            "apiId",
            Map.of()
        );

        verify(eventRepository).create(
            argThat(e -> {
                assertEquals(Set.of(ENVIRONMENT_ID), e.getEnvironments());
                assertEquals(Set.of(ORGANIZATION_ID), e.getOrganizations());
                assertNull(e.getPayload());
                return true;
            })
        );

        verify(eventLatestRepository).createOrUpdate(
            argThat(e -> {
                assertEquals(Set.of(ENVIRONMENT_ID), e.getEnvironments());
                assertEquals(Set.of(ORGANIZATION_ID), e.getOrganizations());
                assertNull(e.getPayload());
                return true;
            })
        );
        verifyNoMoreInteractions(eventRepository);
    }

    @Test
    public void createApiEvent_shouldReadDatabasePlans_thenCreateEvent_withPayloadContainingPlans()
        throws TechnicalException, JsonProcessingException {
        ObjectMapper realObjectMapper = new ObjectMapper();
        ReflectionTestUtils.setField(eventService, "objectMapper", realObjectMapper);
        ReflectionTestUtils.setField(eventService, "planConverter", new PlanConverter(objectMapper));
        when(eventRepository.create(any())).thenAnswer(i -> i.getArguments()[0]);

        Api api = new Api();
        api.setId(API_ID);
        api.setDefinition("{}");

        when(planService.findByApi(any(), eq(API_ID))).thenReturn(
            Set.of(
                buildPlanEntity("plan1", PlanStatus.STAGING),
                buildPlanEntity("plan2", PlanStatus.CLOSED),
                buildPlanEntity("plan3", PlanStatus.PUBLISHED)
            )
        );

        eventService.createApiEvent(
            GraviteeContext.getExecutionContext(),
            Set.of(ENVIRONMENT_ID),
            ORGANIZATION_ID,
            io.gravitee.rest.api.model.EventType.START_API,
            api,
            Map.of()
        );

        // check event has been created and capture his payload
        ArgumentCaptor<Event> createdEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).create(createdEvent.capture());
        verifyNoMoreInteractions(eventRepository);

        Event eventCaptured = createdEvent.getValue();
        assertTrue(eventCaptured.getProperties().containsKey(Event.EventProperties.API_ID.getValue()));

        // deserialize payload event and check it contains plans from database, except closed ones
        Api payloadApi = realObjectMapper.readValue(eventCaptured.getPayload(), Api.class);
        var payloadApiDefinition = realObjectMapper.readValue(payloadApi.getDefinition(), io.gravitee.definition.model.Api.class);
        assertEquals(2, payloadApiDefinition.getPlans().size());
        assertEquals("plan1", payloadApiDefinition.getPlans().get(0).getId());
        assertEquals("plan3", payloadApiDefinition.getPlans().get(1).getId());
    }

    @Test
    public void createApiEvent_shouldReadDatabaseApiFlows_thenCreateEvent_withPayloadContainingFlows()
        throws TechnicalException, JsonProcessingException {
        ObjectMapper realObjectMapper = new ObjectMapper();
        ReflectionTestUtils.setField(eventService, "objectMapper", realObjectMapper);
        ReflectionTestUtils.setField(eventService, "planConverter", new PlanConverter(objectMapper));
        when(eventRepository.create(any())).thenAnswer(i -> i.getArguments()[0]);

        Api api = new Api();
        api.setId(API_ID);
        api.setDefinition("{}");

        when(flowService.findByReference(FlowReferenceType.API, API_ID)).thenReturn(List.of(buildFlow("flow1"), buildFlow("flow2")));

        eventService.createApiEvent(
            GraviteeContext.getExecutionContext(),
            Set.of(ENVIRONMENT_ID),
            ORGANIZATION_ID,
            io.gravitee.rest.api.model.EventType.PUBLISH_API,
            api,
            Map.of()
        );

        // check event has been created and capture his payload
        ArgumentCaptor<Event> createdEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).create(createdEvent.capture());
        verifyNoMoreInteractions(eventRepository);

        // deserialize payload event and check it contains api flows from database
        Event eventCaptured = createdEvent.getValue();
        assertTrue(eventCaptured.getProperties().containsKey(Event.EventProperties.API_ID.getValue()));
        Api payloadApi = realObjectMapper.readValue(eventCaptured.getPayload(), Api.class);
        var payloadApiDefinition = realObjectMapper.readValue(payloadApi.getDefinition(), io.gravitee.definition.model.Api.class);
        assertEquals(2, payloadApiDefinition.getFlows().size());
        assertEquals("flow1", payloadApiDefinition.getFlows().get(0).getName());
        assertEquals("flow2", payloadApiDefinition.getFlows().get(1).getName());

        assertEquals(Set.of(ENVIRONMENT_ID), eventCaptured.getEnvironments());
        assertEquals(Set.of(ORGANIZATION_ID), eventCaptured.getOrganizations());
    }

    @Test
    public void create_native_api_event_with_api_and_plan_flows_in_payload() throws TechnicalException, JsonProcessingException {
        ObjectMapper realObjectMapper = new ObjectMapper();
        ReflectionTestUtils.setField(eventService, "objectMapper", realObjectMapper);
        ReflectionTestUtils.setField(eventService, "planConverter", new PlanConverter(objectMapper));
        when(eventRepository.create(any())).thenAnswer(i -> i.getArguments()[0]);

        Api api = new Api();
        api.setId(API_ID);
        api.setDefinitionVersion(DefinitionVersion.V4);
        api.setType(ApiType.NATIVE);
        api.setDefinition("{ \"type\": \"native\" }");

        when(planQueryService.findAllByApiId(API_ID)).thenReturn(
            List.of(
                buildCorePlan("plan-id", ApiType.NATIVE, io.gravitee.definition.model.v4.plan.PlanStatus.PUBLISHED),
                buildCorePlan("plan-id", ApiType.MESSAGE, io.gravitee.definition.model.v4.plan.PlanStatus.PUBLISHED)
            )
        );

        when(flowCrudService.getNativePlanFlows("plan-id")).thenReturn(
            List.of(buildNativeFlow("flow1-plan"), buildNativeFlow("flow2-plan"))
        );
        when(flowCrudService.getNativeApiFlows(API_ID)).thenReturn(List.of(buildNativeFlow("flow1"), buildNativeFlow("flow2")));

        eventService.createApiEvent(
            GraviteeContext.getExecutionContext(),
            Set.of(ENVIRONMENT_ID),
            ORGANIZATION_ID,
            io.gravitee.rest.api.model.EventType.PUBLISH_API,
            api,
            Map.of()
        );

        // check event has been created and capture his payload
        ArgumentCaptor<Event> createdEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).create(createdEvent.capture());
        verifyNoMoreInteractions(eventRepository);

        // deserialize payload event and check it contains api flows from database
        Event eventCaptured = createdEvent.getValue();
        assertTrue(eventCaptured.getProperties().containsKey(Event.EventProperties.API_ID.getValue()));
        Api payloadApi = realObjectMapper.readValue(eventCaptured.getPayload(), Api.class);
        var payloadApiDefinition = realObjectMapper.readValue(
            payloadApi.getDefinition(),
            io.gravitee.definition.model.v4.nativeapi.NativeApi.class
        );
        assertEquals(2, payloadApiDefinition.getFlows().size());
        assertEquals("flow1", payloadApiDefinition.getFlows().get(0).getName());
        assertEquals("flow2", payloadApiDefinition.getFlows().get(1).getName());
        assertEquals(1, payloadApiDefinition.getPlans().size());
        assertEquals(2, payloadApiDefinition.getPlans().get(0).getFlows().size());
        assertEquals("flow1-plan", payloadApiDefinition.getPlans().get(0).getFlows().get(0).getName());
        assertEquals("flow2-plan", payloadApiDefinition.getPlans().get(0).getFlows().get(1).getName());

        assertEquals(Set.of(ENVIRONMENT_ID), eventCaptured.getEnvironments());
        assertEquals(Set.of(ORGANIZATION_ID), eventCaptured.getOrganizations());
    }

    private PlanEntity buildPlanEntity(String id, PlanStatus status) {
        PlanEntity plan = new PlanEntity();
        plan.setId(id);
        plan.setStatus(status);
        plan.setSecurity(PlanSecurityType.KEY_LESS);
        return plan;
    }

    private Plan buildCorePlan(String id, ApiType apiType, io.gravitee.definition.model.v4.plan.PlanStatus status) {
        var plan = Plan.builder().id(id).definitionVersion(DefinitionVersion.V4).apiType(apiType).apiId(API_ID);

        if (apiType == ApiType.NATIVE) {
            plan.planDefinitionNativeV4(NativePlan.builder().status(status).build());
        } else {
            plan.planDefinitionHttpV4(io.gravitee.definition.model.v4.plan.Plan.builder().status(status).build());
        }

        return plan.build();
    }

    private Flow buildFlow(String name) {
        Flow flow = new Flow();
        flow.setName(name);
        return flow;
    }

    private NativeFlow buildNativeFlow(String name) {
        return NativeFlow.builder().name(name).build();
    }
}
