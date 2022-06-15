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
package io.gravitee.repository.mock.management;

import static io.gravitee.repository.utils.DateUtils.parse;
import static java.util.Arrays.asList;
import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.*;

import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.repository.mock.AbstractRepositoryMock;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.mockito.internal.util.collections.Sets;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EventRepositoryMock extends AbstractRepositoryMock<EventRepository> {

    public EventRepositoryMock() {
        super(EventRepository.class);
    }

    @Override
    protected void prepare(EventRepository eventRepository) throws Exception {
        final Event event1 = mock(Event.class);
        final Event event2 = mock(Event.class);
        final Event event3 = mock(Event.class);
        final Event event4 = mock(Event.class);
        final Event event5 = mock(Event.class);
        final Event event6 = mock(Event.class);
        final Event event7 = mock(Event.class);
        final Event event8 = mock(Event.class);
        final Event event9 = mock(Event.class);
        final Event event10 = mock(Event.class);
        final Event event11 = mock(Event.class);
        final Event event12 = mock(Event.class);
        final Event event13 = mock(Event.class);
        final io.gravitee.common.data.domain.Page<Event> pageEvent = mock(io.gravitee.common.data.domain.Page.class);
        final io.gravitee.common.data.domain.Page<Event> pageEvent2 = mock(io.gravitee.common.data.domain.Page.class);
        final io.gravitee.common.data.domain.Page<Event> pageEvent3 = mock(io.gravitee.common.data.domain.Page.class);
        final io.gravitee.common.data.domain.Page<Event> pageEvent4 = mock(io.gravitee.common.data.domain.Page.class);
        final io.gravitee.common.data.domain.Page<Event> pageEvent5 = mock(io.gravitee.common.data.domain.Page.class);
        final io.gravitee.common.data.domain.Page<Event> pageEvent6 = mock(io.gravitee.common.data.domain.Page.class);
        final io.gravitee.common.data.domain.Page<Event> pageEvent7 = mock(io.gravitee.common.data.domain.Page.class);
        final io.gravitee.common.data.domain.Page<Event> pageEvent8 = mock(io.gravitee.common.data.domain.Page.class);
        final io.gravitee.common.data.domain.Page<Event> pageEvent9 = mock(io.gravitee.common.data.domain.Page.class);
        final io.gravitee.common.data.domain.Page<Event> pageEvent10 = mock(io.gravitee.common.data.domain.Page.class);

        Map<String, String> eventProperties = new HashMap<>();
        eventProperties.put("api_id", "api-1");

        Map<String, String> eventProperties2 = new HashMap<>();
        eventProperties.put("api_id", "api-4");

        when(event1.getId()).thenReturn("event01");
        when(event1.getEnvironments()).thenReturn(singleton("DEFAULT"));
        when(event1.getCreatedAt()).thenReturn(parse("11/02/2016"));
        when(event1.getUpdatedAt()).thenReturn(parse("11/02/2016"));
        when(event1.getType()).thenReturn(EventType.PUBLISH_API);
        when(event1.getPayload()).thenReturn("{}");
        when(event1.getProperties()).thenReturn(eventProperties);

        when(event2.getId()).thenReturn("event02");
        when(event2.getEnvironments()).thenReturn(singleton("DEFAULT"));
        when(event2.getType()).thenReturn(EventType.UNPUBLISH_API);
        when(event2.getCreatedAt()).thenReturn(parse("01/02/2016"));
        when(event2.getUpdatedAt()).thenReturn(parse("01/02/2016"));
        when(event2.getProperties()).thenReturn(eventProperties);

        when(event3.getId()).thenReturn("event03");
        when(event3.getEnvironments()).thenReturn(singleton("DEFAULT"));
        when(event3.getType()).thenReturn(EventType.PUBLISH_API);
        when(event3.getCreatedAt()).thenReturn(parse("01/03/2016"));
        when(event3.getUpdatedAt()).thenReturn(parse("01/03/2016"));

        when(event4.getId()).thenReturn("event04");
        when(event4.getEnvironments()).thenReturn(singleton("DEFAULT"));
        when(event4.getType()).thenReturn(EventType.STOP_API);
        when(event4.getCreatedAt()).thenReturn(parse("01/04/2016"));
        when(event4.getUpdatedAt()).thenReturn(parse("01/04/2016"));
        when(event4.getProperties()).thenReturn(eventProperties2);

        when(event5.getId()).thenReturn("event05");
        when(event5.getEnvironments()).thenReturn(singleton("DEFAULT"));
        when(event5.getType()).thenReturn(EventType.START_API);
        when(event5.getCreatedAt()).thenReturn(parse("01/05/2016"));
        when(event5.getUpdatedAt()).thenReturn(parse("01/05/2016"));

        when(event6.getId()).thenReturn("event06");
        when(event6.getEnvironments()).thenReturn(singleton("DEFAULT"));
        when(event6.getType()).thenReturn(EventType.START_API);
        when(event6.getCreatedAt()).thenReturn(parse("01/06/2016"));
        when(event6.getUpdatedAt()).thenReturn(parse("01/06/2016"));

        when(event7.getId()).thenReturn("event07");
        when(event7.getEnvironments()).thenReturn(Sets.newSet("OTHER_ENV", "OTHER_ENV_2"));
        when(event7.getType()).thenReturn(EventType.GATEWAY_STOPPED);
        when(event7.getCreatedAt()).thenReturn(parse("01/07/2016"));
        when(event7.getUpdatedAt()).thenReturn(parse("01/07/2016"));

        when(event8.getId()).thenReturn("event08");
        when(event8.getEnvironments()).thenReturn(singleton("DEFAULT"));
        when(event8.getType()).thenReturn(EventType.STOP_DICTIONARY);
        when(event8.getCreatedAt()).thenReturn(parse("01/08/2016"));
        when(event8.getUpdatedAt()).thenReturn(parse("01/08/2016"));

        when(event9.getId()).thenReturn("event09");
        when(event9.getEnvironments()).thenReturn(singleton("DEFAULT"));
        when(event9.getType()).thenReturn(EventType.START_DICTIONARY);
        when(event9.getCreatedAt()).thenReturn(parse("01/09/2016"));
        when(event9.getUpdatedAt()).thenReturn(parse("01/09/2016"));

        when(event10.getId()).thenReturn("event10");
        when(event10.getEnvironments()).thenReturn(singleton("OTHER"));
        when(event10.getType()).thenReturn(EventType.PUBLISH_DICTIONARY);
        when(event10.getCreatedAt()).thenReturn(parse("01/10/2016"));
        when(event10.getUpdatedAt()).thenReturn(parse("01/10/2016"));

        when(event11.getId()).thenReturn("event11");
        when(event11.getEnvironments()).thenReturn(singleton("DEFAULT"));
        when(event11.getType()).thenReturn(EventType.START_API);
        when(event11.getCreatedAt()).thenReturn(parse("16/02/2016"));
        when(event11.getUpdatedAt()).thenReturn(parse("16/02/2016"));

        when(event12.getId()).thenReturn("event12");
        when(event12.getEnvironments()).thenReturn(singleton("DEFAULT"));
        when(event12.getType()).thenReturn(EventType.START_API);
        when(event12.getCreatedAt()).thenReturn(parse("16/02/2016"));
        when(event12.getUpdatedAt()).thenReturn(parse("16/02/2016"));

        when(event13.getId()).thenReturn("event13");
        when(event13.getEnvironments()).thenReturn(singleton("DEFAULT"));
        when(event13.getType()).thenReturn(EventType.STOP_DICTIONARY);
        when(event13.getCreatedAt()).thenReturn(parse("20/02/2016"));
        when(event13.getUpdatedAt()).thenReturn(parse("20/02/2016"));

        when(eventRepository.findById("event01")).thenReturn(of(event1));

        when(eventRepository.findById("event05")).thenReturn(of(event5), empty());

        when(eventRepository.create(any(Event.class))).thenReturn(event1);

        when(pageEvent.getTotalElements()).thenReturn(4L);
        when(pageEvent.getContent()).thenReturn(asList(event12, event11, event6, event5));
        when(
            eventRepository.search(
                new EventCriteria.Builder().from(1451606400000L).to(1470157767000L).types(EventType.START_API).build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build()
            )
        )
            .thenReturn(pageEvent);

        when(pageEvent2.getTotalElements()).thenReturn(5L);
        when(pageEvent2.getContent()).thenReturn(asList(event12, event11, event6, event5, event4));
        when(
            eventRepository.search(
                new EventCriteria.Builder().from(1451606400000L).to(1470157767000L).types(EventType.START_API, EventType.STOP_API).build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build()
            )
        )
            .thenReturn(pageEvent2);

        when(pageEvent3.getTotalElements()).thenReturn(2L);
        when(pageEvent3.getContent()).thenReturn(asList(event2, event1));
        when(
            eventRepository.search(
                new EventCriteria.Builder()
                    .from(1451606400000L)
                    .to(1470157767000L)
                    .property(Event.EventProperties.API_ID.getValue(), "api-1")
                    .build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build()
            )
        )
            .thenReturn(pageEvent3);

        when(pageEvent4.getTotalElements()).thenReturn(0L);
        when(pageEvent4.getContent()).thenReturn(Collections.emptyList());
        when(
            eventRepository.search(
                new EventCriteria.Builder().from(1420070400000L).to(1422748800000L).types(EventType.START_API).build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build()
            )
        )
            .thenReturn(pageEvent4);

        when(pageEvent5.getTotalElements()).thenReturn(1L);
        when(pageEvent5.getContent()).thenReturn(singletonList(event4));
        when(
            eventRepository.search(
                new EventCriteria.Builder()
                    .from(1451606400000L)
                    .to(1470157767000L)
                    .property(Event.EventProperties.API_ID.getValue(), "api-3")
                    .types(EventType.START_API, EventType.STOP_API)
                    .build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build()
            )
        )
            .thenReturn(pageEvent5);

        when(pageEvent6.getTotalElements()).thenReturn(2L);
        when(pageEvent6.getContent()).thenReturn(asList(event2, event1));
        when(
            eventRepository.search(
                new EventCriteria.Builder()
                    .from(1451606400000L)
                    .to(1470157767000L)
                    .property(Event.EventProperties.API_ID.getValue(), "api-1")
                    .build(),
                null
            )
        )
            .thenReturn(pageEvent6);

        when(pageEvent7.getTotalElements()).thenReturn(3L);
        when(pageEvent7.getContent()).thenReturn(asList(event4, event2, event1));
        when(
            eventRepository.search(
                new EventCriteria.Builder()
                    .from(1451606400000L)
                    .to(1470157767000L)
                    .property(Event.EventProperties.API_ID.getValue(), asList("api-1", "api-3"))
                    .build(),
                null
            )
        )
            .thenReturn(pageEvent7);

        when(pageEvent8.getTotalElements()).thenReturn(5L);
        when(pageEvent8.getPageElements()).thenReturn(2L);
        when(pageEvent8.getContent()).thenReturn(asList(event12, event11));
        when(
            eventRepository.search(
                new EventCriteria.Builder().from(1451606400000L).to(1470157767000L).types(EventType.START_API, EventType.STOP_API).build(),
                new PageableBuilder().pageNumber(0).pageSize(2).build()
            )
        )
            .thenReturn(pageEvent8);

        when(pageEvent9.getTotalElements()).thenReturn(5L);
        when(pageEvent9.getPageElements()).thenReturn(1L);
        when(pageEvent9.getContent()).thenReturn(singletonList(event4));
        when(
            eventRepository.search(
                new EventCriteria.Builder().from(1451606400000L).to(1470157767000L).types(EventType.START_API, EventType.STOP_API).build(),
                new PageableBuilder().pageNumber(2).pageSize(2).build()
            )
        )
            .thenReturn(pageEvent9);

        when(
            eventRepository.search(
                new EventCriteria.Builder()
                    .from(1451606400000L)
                    .to(1470157767000L)
                    .property(Event.EventProperties.API_ID.getValue(), asList("api-1", "api-3"))
                    .build()
            )
        )
            .thenReturn(asList(event4, event2, event1));

        when(
            eventRepository.search(
                new EventCriteria.Builder().property(Event.EventProperties.API_ID.getValue(), asList("api-1", "api-3")).build()
            )
        )
            .thenReturn(asList(event4, event2, event1));

        when(eventRepository.search(new EventCriteria.Builder().environments(singletonList("DEFAULT")).build()))
            .thenReturn(asList(event9, event8, event12, event11, event6, event5, event4, event3, event2, event1));

        when(eventRepository.search(new EventCriteria.Builder().environments(Arrays.asList("DEFAULT", "OTHER_ENV")).build()))
            .thenReturn(asList(event9, event8, event7, event12, event11, event6, event5, event4, event3, event2, event1));

        when(
            eventRepository.search(
                new EventCriteria.Builder().types(EventType.GATEWAY_STARTED).build(),
                new PageableBuilder().pageNumber(0).pageSize(10).build()
            )
        )
            .thenReturn(new io.gravitee.common.data.domain.Page<>(Collections.emptyList(), 0, 2, 0));

        when(eventRepository.update(argThat(o -> o == null || o.getId().equals("unknown")))).thenThrow(new IllegalStateException());

        when(eventRepository.searchLatest(new EventCriteria.Builder().build(), Event.EventProperties.API_ID, null, null))
            .thenReturn(Arrays.asList(event12, event11, event6, event4, event2));

        when(eventRepository.searchLatest(new EventCriteria.Builder().build(), Event.EventProperties.API_ID, 3L, 1L))
            .thenReturn(singletonList(event4));

        when(eventRepository.searchLatest(new EventCriteria.Builder().build(), Event.EventProperties.API_ID, 0L, 1L))
            .thenReturn(singletonList(event12));
        when(eventRepository.searchLatest(new EventCriteria.Builder().build(), Event.EventProperties.API_ID, 1L, 1L))
            .thenReturn(singletonList(event11));
        when(eventRepository.searchLatest(new EventCriteria.Builder().build(), Event.EventProperties.API_ID, 2L, 1L))
            .thenReturn(singletonList(event6));
        when(eventRepository.searchLatest(new EventCriteria.Builder().build(), Event.EventProperties.API_ID, 3L, 1L))
            .thenReturn(singletonList(event4));
        when(eventRepository.searchLatest(new EventCriteria.Builder().build(), Event.EventProperties.API_ID, 4L, 1L))
            .thenReturn(singletonList(event2));

        when(eventRepository.searchLatest(new EventCriteria.Builder().build(), Event.EventProperties.API_ID, 0L, 2L))
            .thenReturn(Arrays.asList(event12, event11));
        when(eventRepository.searchLatest(new EventCriteria.Builder().build(), Event.EventProperties.API_ID, 1L, 2L))
            .thenReturn(Arrays.asList(event6, event4));
        when(eventRepository.searchLatest(new EventCriteria.Builder().build(), Event.EventProperties.API_ID, 2L, 2L))
            .thenReturn(singletonList(event2));

        when(eventRepository.searchLatest(new EventCriteria.Builder().build(), Event.EventProperties.API_ID, 0L, 3L))
            .thenReturn(Arrays.asList(event12, event11, event6));
        when(eventRepository.searchLatest(new EventCriteria.Builder().build(), Event.EventProperties.API_ID, 1L, 3L))
            .thenReturn(Arrays.asList(event4, event2));

        when(eventRepository.searchLatest(new EventCriteria.Builder().build(), Event.EventProperties.API_ID, 0L, 4L))
            .thenReturn(Arrays.asList(event12, event11, event6, event4));
        when(eventRepository.searchLatest(new EventCriteria.Builder().build(), Event.EventProperties.API_ID, 1L, 4L))
            .thenReturn(singletonList(event2));

        when(eventRepository.searchLatest(new EventCriteria.Builder().build(), Event.EventProperties.API_ID, 0L, 5L))
            .thenReturn(Arrays.asList(event12, event11, event6, event4, event2));

        when(
            eventRepository.searchLatest(
                new EventCriteria.Builder()
                    .from(1451606400000L)
                    .to(1470157767000L)
                    .property(Event.EventProperties.API_ID.getValue(), "api-3")
                    .types(EventType.START_API, EventType.STOP_API)
                    .build(),
                Event.EventProperties.API_ID,
                0L,
                10L
            )
        )
            .thenReturn(singletonList(event4));

        when(
            eventRepository.searchLatest(
                new EventCriteria.Builder().from(1475200000000L).to(1475381000000L).types(EventType.PUBLISH_DICTIONARY).build(),
                Event.EventProperties.DICTIONARY_ID,
                0L,
                10L
            )
        )
            .thenReturn(singletonList(event10));

        when(eventRepository.searchLatest(new EventCriteria.Builder().build(), Event.EventProperties.DICTIONARY_ID, null, null))
            .thenReturn(Arrays.asList(event13, event8));

        when(
            eventRepository.searchLatest(
                new EventCriteria.Builder()
                    .property(Event.EventProperties.API_ID.getValue(), "api-1")
                    .types(EventType.START_API, EventType.PUBLISH_API)
                    .strictMode(true)
                    .build(),
                Event.EventProperties.API_ID,
                0L,
                10L
            )
        )
            .thenReturn(EMPTY_LIST);

        when(
            eventRepository.searchLatest(
                new EventCriteria.Builder()
                    .property(Event.EventProperties.API_ID.getValue(), "api-1")
                    .types(EventType.START_API, EventType.PUBLISH_API)
                    .strictMode(false)
                    .build(),
                Event.EventProperties.API_ID,
                0L,
                10L
            )
        )
            .thenReturn(singletonList(event1));
    }
}
