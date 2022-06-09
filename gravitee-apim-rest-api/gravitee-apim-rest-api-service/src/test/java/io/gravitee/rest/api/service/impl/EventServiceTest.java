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
package io.gravitee.rest.api.service.impl;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.model.debug.DebugApi;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Api;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.repository.management.model.flow.FlowReferenceType;
import io.gravitee.rest.api.model.*;
import io.gravitee.rest.api.service.PlanService;
import io.gravitee.rest.api.service.UserService;
import io.gravitee.rest.api.service.common.GraviteeContext;
import io.gravitee.rest.api.service.configuration.flow.FlowService;
import io.gravitee.rest.api.service.converter.PlanConverter;
import io.gravitee.rest.api.service.exceptions.EventNotFoundException;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
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

    private static final String EVENT_ID = "id-event";
    private static final String EVENT_PAYLOAD = "{}";
    private static final String EVENT_USERNAME = "admin";
    private static final String EVENT_ORIGIN = "localhost";
    private static final String API_ID = "id-api";
    private static final String ENVIRONMENT_ID = "DEFAULT";
    private static final Map<String, String> EVENT_PROPERTIES = new HashMap<String, String>() {
        {
            put(Event.EventProperties.API_ID.getValue(), API_ID);
            put(Event.EventProperties.USER.getValue(), EVENT_USERNAME);
            put(Event.EventProperties.ORIGIN.getValue(), EVENT_ORIGIN);
        }
    };

    @Before
    public void setup() {
        GraviteeContext.setCurrentEnvironment(ENVIRONMENT_ID);
    }

    @After
    public void tearDown() {
        GraviteeContext.cleanContext();
    }

    @InjectMocks
    private EventServiceImpl eventService = new EventServiceImpl();

    @Mock
    private EventRepository eventRepository;

    @Mock
    private NewEventEntity newEvent;

    @Mock
    private Event event;

    @Mock
    private Event event2;

    @Mock
    private Page<Event> eventPage;

    @Mock
    private UserService userService;

    @Mock
    private PlanService planService;

    @Mock
    private FlowService flowService;

    @Mock
    private ObjectMapper objectMapper;

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
            Collections.singleton(GraviteeContext.getCurrentEnvironment()),
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
    public void shouldDelete() throws TechnicalException {
        eventService.delete(EVENT_ID);

        verify(eventRepository).delete(EVENT_ID);
    }

    @Test
    public void shouldSearchNoResults() {
        when(eventPage.getTotalElements()).thenReturn(0L);
        when(eventPage.getContent()).thenReturn(Collections.emptyList());

        when(
            eventRepository.search(
                new EventCriteria.Builder()
                    .from(1420070400000L)
                    .to(1422748800000L)
                    .types(EventType.START_API)
                    .environments(Collections.singletonList(ENVIRONMENT_ID))
                    .build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build()
            )
        )
            .thenReturn(eventPage);

        Page<EventEntity> eventPageEntity = eventService.search(
            GraviteeContext.getExecutionContext(),
            Collections.singletonList(io.gravitee.rest.api.model.EventType.START_API),
            null,
            1420070400000L,
            1422748800000L,
            0,
            10,
            Collections.singletonList(GraviteeContext.getCurrentEnvironment())
        );
        assertTrue(0L == eventPageEntity.getTotalElements());
    }

    @Test
    public void shouldSearchBySingleEventType() throws Exception {
        Map<String, Object> values = new HashMap<>();
        values.put("type", EventType.START_API.toString());

        when(event.getId()).thenReturn("event1");
        when(event.getType()).thenReturn(EventType.START_API);
        when(event.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event.getProperties()).thenReturn(EVENT_PROPERTIES);

        when(event2.getId()).thenReturn("event2");
        when(event2.getType()).thenReturn(EventType.START_API);
        when(event2.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event2.getProperties()).thenReturn(EVENT_PROPERTIES);

        when(eventPage.getTotalElements()).thenReturn(2L);
        when(eventPage.getContent()).thenReturn(Arrays.asList(event, event2));

        when(
            eventRepository.search(
                new EventCriteria.Builder()
                    .from(1420070400000L)
                    .to(1422748800000L)
                    .types(EventType.START_API)
                    .environments(Collections.singletonList(ENVIRONMENT_ID))
                    .build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build()
            )
        )
            .thenReturn(eventPage);

        Page<EventEntity> eventPageEntity = eventService.search(
            GraviteeContext.getExecutionContext(),
            Collections.singletonList(io.gravitee.rest.api.model.EventType.START_API),
            null,
            1420070400000L,
            1422748800000L,
            0,
            10,
            Collections.singletonList(GraviteeContext.getCurrentEnvironment())
        );

        assertTrue(2L == eventPageEntity.getTotalElements());
        assertTrue("event1".equals(eventPageEntity.getContent().get(0).getId()));
    }

    @Test
    public void shouldSearchByMultipleEventType() throws Exception {
        when(event.getId()).thenReturn("event1");
        when(event.getType()).thenReturn(EventType.START_API);
        when(event.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event.getProperties()).thenReturn(EVENT_PROPERTIES);

        when(event2.getId()).thenReturn("event2");
        when(event2.getType()).thenReturn(EventType.STOP_API);
        when(event2.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event2.getProperties()).thenReturn(EVENT_PROPERTIES);

        when(eventPage.getTotalElements()).thenReturn(2l);
        when(eventPage.getContent()).thenReturn(Arrays.asList(event, event2));

        when(
            eventRepository.search(
                new EventCriteria.Builder()
                    .from(1420070400000L)
                    .to(1422748800000L)
                    .types(EventType.START_API, EventType.STOP_API)
                    .environments(Collections.singletonList(ENVIRONMENT_ID))
                    .build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build()
            )
        )
            .thenReturn(eventPage);

        Page<EventEntity> eventPageEntity = eventService.search(
            GraviteeContext.getExecutionContext(),
            Arrays.asList(io.gravitee.rest.api.model.EventType.START_API, io.gravitee.rest.api.model.EventType.STOP_API),
            null,
            1420070400000L,
            1422748800000L,
            0,
            10,
            Collections.singletonList(GraviteeContext.getCurrentEnvironment())
        );

        assertTrue(2L == eventPageEntity.getTotalElements());
        assertTrue("event1".equals(eventPageEntity.getContent().get(0).getId()));
    }

    @Test
    public void shouldSearchByAPIId() throws Exception {
        Map<String, Object> values = new HashMap<>();
        values.put(Event.EventProperties.API_ID.getValue(), "id-api");

        when(event.getId()).thenReturn("event1");
        when(event.getType()).thenReturn(EventType.START_API);
        when(event.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event.getProperties()).thenReturn(EVENT_PROPERTIES);

        when(event2.getId()).thenReturn("event2");
        when(event2.getType()).thenReturn(EventType.STOP_API);
        when(event2.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event2.getProperties()).thenReturn(EVENT_PROPERTIES);

        when(eventPage.getTotalElements()).thenReturn(2L);
        when(eventPage.getContent()).thenReturn(Arrays.asList(event, event2));

        when(
            eventRepository.search(
                new EventCriteria.Builder()
                    .from(1420070400000L)
                    .to(1422748800000L)
                    .property(Event.EventProperties.API_ID.getValue(), "id-api")
                    .environments(Collections.singletonList(ENVIRONMENT_ID))
                    .build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build()
            )
        )
            .thenReturn(eventPage);

        Page<EventEntity> eventPageEntity = eventService.search(
            GraviteeContext.getExecutionContext(),
            null,
            values,
            1420070400000L,
            1422748800000L,
            0,
            10,
            Collections.singletonList(GraviteeContext.getCurrentEnvironment())
        );

        assertTrue(2L == eventPageEntity.getTotalElements());
        assertTrue("event1".equals(eventPageEntity.getContent().get(0).getId()));
    }

    @Test
    public void shouldSearchByMixProperties() throws Exception {
        Map<String, Object> values = new HashMap<>();
        values.put(Event.EventProperties.API_ID.getValue(), "id-api");

        when(event.getId()).thenReturn("event1");
        when(event.getType()).thenReturn(EventType.START_API);
        when(event.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event.getProperties()).thenReturn(EVENT_PROPERTIES);

        when(event2.getId()).thenReturn("event2");
        when(event2.getType()).thenReturn(EventType.STOP_API);
        when(event2.getPayload()).thenReturn(EVENT_PAYLOAD);
        when(event2.getProperties()).thenReturn(EVENT_PROPERTIES);

        when(eventPage.getTotalElements()).thenReturn(2L);
        when(eventPage.getContent()).thenReturn(Arrays.asList(event, event2));

        when(
            eventRepository.search(
                new EventCriteria.Builder()
                    .from(1420070400000L)
                    .to(1422748800000L)
                    .property(Event.EventProperties.API_ID.getValue(), "id-api")
                    .types(EventType.START_API, EventType.STOP_API)
                    .environments(Collections.singletonList(ENVIRONMENT_ID))
                    .build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build()
            )
        )
            .thenReturn(eventPage);

        Page<EventEntity> eventPageEntity = eventService.search(
            GraviteeContext.getExecutionContext(),
            Arrays.asList(io.gravitee.rest.api.model.EventType.START_API, io.gravitee.rest.api.model.EventType.STOP_API),
            values,
            1420070400000L,
            1422748800000L,
            0,
            10,
            Collections.singletonList(GraviteeContext.getCurrentEnvironment())
        );

        assertTrue(2L == eventPageEntity.getTotalElements());
        assertTrue("event1".equals(eventPageEntity.getContent().get(0).getId()));
    }

    @Test
    public void shouldFilterEvent() throws TechnicalException {
        when(eventRepository.search(any(), any()))
            .thenReturn(
                new Page<>(
                    Arrays.asList(
                        generateInstanceEvent("evt1", false),
                        generateInstanceEvent("evt2", true),
                        generateInstanceEvent("evt3", true),
                        generateInstanceEvent("evt4", false),
                        generateInstanceEvent("evt5", true)
                    ),
                    1,
                    5,
                    5
                )
            );

        // test without predicate
        Page<Map<String, String>> page = eventService.search(
            GraviteeContext.getExecutionContext(),
            Arrays.asList(io.gravitee.rest.api.model.EventType.GATEWAY_STARTED),
            Collections.EMPTY_MAP,
            0,
            0,
            1,
            10,
            evt -> {
                Map<String, String> map = new HashMap<>();
                map.put("id", evt.getId());
                map.put("state", evt.getType().name());
                return map;
            },
            Collections.singletonList(GraviteeContext.getCurrentEnvironment())
        );
        assertNotNull(page);
        assertNotNull(page.getContent());
        assertEquals(5, page.getContent().size());

        // test with predicate
        page =
            eventService.search(
                GraviteeContext.getExecutionContext(),
                Arrays.asList(io.gravitee.rest.api.model.EventType.GATEWAY_STARTED),
                Collections.EMPTY_MAP,
                0,
                0,
                1,
                10,
                evt -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("id", evt.getId());
                    map.put("state", evt.getType().name());
                    return map;
                },
                map -> !map.get("state").equals(io.gravitee.rest.api.model.EventType.GATEWAY_STOPPED.name()),
                Collections.singletonList(GraviteeContext.getCurrentEnvironment())
            );
        assertNotNull(page);
        assertNotNull(page.getContent());
        assertEquals(3, page.getContent().size());
    }

    @Test
    public void createDebugApiEvent_shouldCreateEvent_withoutPayload() throws TechnicalException {
        when(eventRepository.create(any())).thenAnswer(i -> i.getArguments()[0]);

        eventService.createDebugApiEvent(
            GraviteeContext.getExecutionContext(),
            Set.of(),
            io.gravitee.rest.api.model.EventType.DEBUG_API,
            null,
            Map.of()
        );

        verify(eventRepository, times(1)).create(argThat(e -> e.getPayload() == null));
        verifyNoMoreInteractions(eventRepository);
    }

    @Test
    public void createDebugApiEvent_shouldCreateEvent_withPayload() throws TechnicalException, JsonProcessingException {
        String jsonValue = "serialized json value";
        DebugApi debugApi = mock(DebugApi.class);
        when(objectMapper.writeValueAsString(debugApi)).thenReturn(jsonValue);
        when(eventRepository.create(any())).thenAnswer(i -> i.getArguments()[0]);

        eventService.createDebugApiEvent(
            GraviteeContext.getExecutionContext(),
            Set.of(),
            io.gravitee.rest.api.model.EventType.DEBUG_API,
            debugApi,
            Map.of()
        );

        verify(eventRepository, times(1)).create(argThat(e -> jsonValue.equals(e.getPayload())));
        verifyNoMoreInteractions(eventRepository);
    }

    @Test
    public void createDictionaryEvent_shouldCreateEvent_withoutPayload() throws TechnicalException {
        when(eventRepository.create(any())).thenAnswer(i -> i.getArguments()[0]);

        eventService.createDictionaryEvent(
            GraviteeContext.getExecutionContext(),
            Set.of(),
            io.gravitee.rest.api.model.EventType.DEBUG_API,
            null,
            Map.of()
        );

        verify(eventRepository, times(1)).create(argThat(e -> e.getPayload() == null));
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
            Set.of(),
            io.gravitee.rest.api.model.EventType.DEBUG_API,
            dictionary,
            Map.of()
        );

        verify(eventRepository, times(1)).create(argThat(e -> jsonValue.equals(e.getPayload())));
        verifyNoMoreInteractions(eventRepository);
    }

    @Test
    public void createOrganizationEvent_shouldCreateEvent_withoutPayload() throws TechnicalException {
        when(eventRepository.create(any())).thenAnswer(i -> i.getArguments()[0]);

        eventService.createOrganizationEvent(
            GraviteeContext.getExecutionContext(),
            Set.of(),
            io.gravitee.rest.api.model.EventType.DEBUG_API,
            null,
            Map.of()
        );

        verify(eventRepository, times(1)).create(argThat(e -> e.getPayload() == null));
        verifyNoMoreInteractions(eventRepository);
    }

    @Test
    public void createOrganizationApiEvent_shouldCreateEvent_withPayload() throws TechnicalException, JsonProcessingException {
        String jsonValue = "serialized json value";
        OrganizationEntity organization = mock(OrganizationEntity.class);
        when(objectMapper.writeValueAsString(organization)).thenReturn(jsonValue);
        when(eventRepository.create(any())).thenAnswer(i -> i.getArguments()[0]);

        eventService.createOrganizationEvent(
            GraviteeContext.getExecutionContext(),
            Set.of(),
            io.gravitee.rest.api.model.EventType.DEBUG_API,
            organization,
            Map.of()
        );

        verify(eventRepository, times(1)).create(argThat(e -> jsonValue.equals(e.getPayload())));
        verifyNoMoreInteractions(eventRepository);
    }

    @Test
    public void createApiEvent_shouldCreateEvent_withoutPayload() throws TechnicalException {
        when(eventRepository.create(any())).thenAnswer(i -> i.getArguments()[0]);

        eventService.createApiEvent(
            GraviteeContext.getExecutionContext(),
            Set.of(),
            io.gravitee.rest.api.model.EventType.DEBUG_API,
            null,
            Map.of()
        );

        verify(eventRepository, times(1)).create(argThat(e -> e.getPayload() == null));
        verifyNoMoreInteractions(eventRepository);
    }

    @Test
    public void createApiEvent_shouldReadDatabasePlans_thenCreateEvent_withPayloadContainingPlans()
        throws TechnicalException, JsonProcessingException {
        ObjectMapper realObjectMapper = new ObjectMapper();
        ReflectionTestUtils.setField(eventService, "objectMapper", realObjectMapper);
        ReflectionTestUtils.setField(eventService, "planConverter", new PlanConverter());
        when(eventRepository.create(any())).thenAnswer(i -> i.getArguments()[0]);

        Api api = new Api();
        api.setId(API_ID);
        api.setDefinition("{}");

        when(planService.findByApi(any(), eq(API_ID)))
            .thenReturn(
                Set.of(
                    buildPlanEntity("plan1", PlanStatus.STAGING),
                    buildPlanEntity("plan2", PlanStatus.CLOSED),
                    buildPlanEntity("plan3", PlanStatus.PUBLISHED)
                )
            );

        eventService.createApiEvent(
            GraviteeContext.getExecutionContext(),
            Set.of(),
            io.gravitee.rest.api.model.EventType.DEBUG_API,
            api,
            Map.of()
        );

        // check event has been created and capture his payload
        ArgumentCaptor<Event> createdEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository, times(1)).create(createdEvent.capture());
        verifyNoMoreInteractions(eventRepository);

        // deserialize payload event and check it contains plans from database, except closed ones
        Api payloadApi = realObjectMapper.readValue(createdEvent.getValue().getPayload(), Api.class);
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
        ReflectionTestUtils.setField(eventService, "planConverter", new PlanConverter());
        when(eventRepository.create(any())).thenAnswer(i -> i.getArguments()[0]);

        Api api = new Api();
        api.setId(API_ID);
        api.setDefinition("{}");

        when(flowService.findByReference(FlowReferenceType.API, API_ID)).thenReturn(List.of(buildFlow("flow1"), buildFlow("flow2")));

        eventService.createApiEvent(
            GraviteeContext.getExecutionContext(),
            Set.of(),
            io.gravitee.rest.api.model.EventType.PUBLISH_API,
            api,
            Map.of()
        );

        // check event has been created and capture his payload
        ArgumentCaptor<Event> createdEvent = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository, times(1)).create(createdEvent.capture());
        verifyNoMoreInteractions(eventRepository);

        // deserialize payload event and check it contains api flows from database
        Api payloadApi = realObjectMapper.readValue(createdEvent.getValue().getPayload(), Api.class);
        var payloadApiDefinition = realObjectMapper.readValue(payloadApi.getDefinition(), io.gravitee.definition.model.Api.class);
        assertEquals(2, payloadApiDefinition.getFlows().size());
        assertEquals("flow1", payloadApiDefinition.getFlows().get(0).getName());
        assertEquals("flow2", payloadApiDefinition.getFlows().get(1).getName());
    }

    private PlanEntity buildPlanEntity(String id, PlanStatus status) {
        PlanEntity plan = new PlanEntity();
        plan.setId(id);
        plan.setStatus(status);
        plan.setSecurity(PlanSecurityType.KEY_LESS);
        return plan;
    }

    private Flow buildFlow(String name) {
        Flow flow = new Flow();
        flow.setName(name);
        return flow;
    }

    private Event generateInstanceEvent(String name, boolean isUnknown) {
        Event event = new Event();
        event.setId("evt1");
        event.setCreatedAt(new Date(Instant.now().minus(1, ChronoUnit.HOURS).toEpochMilli()));
        event.setUpdatedAt(new Date(Instant.now().minus(50, ChronoUnit.MINUTES).toEpochMilli()));
        Map<String, String> properties = new HashMap<>();
        properties.put("started_at", "" + Instant.now().minus(1, ChronoUnit.HOURS).toEpochMilli());
        if (isUnknown) {
            event.setType(EventType.GATEWAY_STARTED);
            properties.put("last_heartbeat_at", "" + Instant.now().minus(50, ChronoUnit.MINUTES).toEpochMilli());
        } else {
            event.setType(EventType.GATEWAY_STOPPED);
            properties.put("last_heartbeat_at", "" + Instant.now().minus(50, ChronoUnit.MINUTES).toEpochMilli());
            properties.put("stopped_at", "" + Instant.now().minus(50, ChronoUnit.MINUTES).toEpochMilli());
        }
        event.setProperties(properties);
        return event;
    }
}
