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
package io.gravitee.rest.api.service.impl.upgrade.upgrader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.gravitee.common.data.domain.Page;
import io.gravitee.definition.jackson.datatype.GraviteeMapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.ApiRepository;
import io.gravitee.repository.management.api.DictionaryRepository;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.OrganizationRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Dictionary;
import io.gravitee.repository.management.model.DictionaryType;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.repository.management.model.Organization;
import io.gravitee.rest.api.service.EventService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class EventsLatestUpgraderTest {

    @Mock
    private ApiRepository apiRepository;

    @Mock
    private DictionaryRepository dictionaryRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventLatestRepository eventLatestRepository;

    @Mock
    private EventsLatestUpgrader cut;

    @Captor
    private ArgumentCaptor<Event> eventCaptor;

    @Before
    public void before() {
        cut =
            new EventsLatestUpgrader(
                apiRepository,
                dictionaryRepository,
                organizationRepository,
                eventRepository,
                eventLatestRepository,
                new GraviteeMapper(false)
            );
    }

    @Test
    public void upgrade_should_failed_because_of_exception() throws TechnicalException {
        when(apiRepository.searchIds(any(), any(), any())).thenThrow(new RuntimeException());

        assertFalse(cut.upgrade());

        verify(apiRepository, times(1)).searchIds(any(), any(), any());
        verifyNoMoreInteractions(apiRepository);
    }

    @Test
    public void should_order_equals_600() {
        assertThat(cut.getOrder()).isEqualTo(600);
    }

    @Test
    public void should_do_nothing_when_nothing_to_migrate() throws TechnicalException {
        cut.upgrade();
        verifyNoInteractions(eventLatestRepository);
    }

    @Test
    public void should_create_latest_events_from_existing_apis() throws TechnicalException {
        when(apiRepository.searchIds(eq(List.of()), any(), eq(null))).thenReturn(new Page<>(List.of("api1", "api2"), 0, 2, 2));
        Event event1 = new Event();
        when(
            eventRepository.search(
                EventCriteria.builder().property(Event.EventProperties.API_ID.getValue(), "api1").build(),
                new PageableBuilder().pageNumber(0).pageSize(1).build()
            )
        )
            .thenReturn(new Page<>(List.of(event1), 0, 1, 1));
        when(eventLatestRepository.createOrUpdate(event1)).thenReturn(event1);
        Event event2 = new Event();
        when(
            eventRepository.search(
                EventCriteria.builder().property(Event.EventProperties.API_ID.getValue(), "api2").build(),
                new PageableBuilder().pageNumber(0).pageSize(1).build()
            )
        )
            .thenReturn(new Page<>(List.of(event2), 0, 1, 1));
        when(eventLatestRepository.createOrUpdate(event2)).thenReturn(event2);

        cut.upgrade();

        verify(eventLatestRepository).createOrUpdate(event1);
        verify(eventLatestRepository).createOrUpdate(event2);
        verifyNoMoreInteractions(eventLatestRepository);
    }

    @Test
    public void should_create_latest_events_from_existing_apis_with_valid_and_invalid_payload() throws TechnicalException {
        when(apiRepository.searchIds(eq(List.of()), any(), eq(null))).thenReturn(new Page<>(List.of("api1", "api2"), 0, 2, 2));
        Event event1 = new Event();
        event1.setId("id1");
        event1.setPayload("{\n \"test\": \"value\"\n}");
        when(
            eventRepository.search(
                EventCriteria.builder().property(Event.EventProperties.API_ID.getValue(), "api1").build(),
                new PageableBuilder().pageNumber(0).pageSize(1).build()
            )
        )
            .thenReturn(new Page<>(List.of(event1), 0, 1, 1));
        when(eventLatestRepository.createOrUpdate(event1)).thenReturn(event1);
        Event event2 = new Event();
        event2.setId("id2");
        event2.setPayload("{\\n");
        when(
            eventRepository.search(
                EventCriteria.builder().property(Event.EventProperties.API_ID.getValue(), "api2").build(),
                new PageableBuilder().pageNumber(0).pageSize(1).build()
            )
        )
            .thenReturn(new Page<>(List.of(event2), 0, 1, 1));
        when(eventLatestRepository.createOrUpdate(event2)).thenReturn(event2);

        cut.upgrade();

        verify(eventLatestRepository, times(2))
            .createOrUpdate(
                argThat(argument -> {
                    if (argument.getId().equals("api1")) {
                        assertThat(argument.getPayload()).isEqualTo("{\"test\":\"value\"}");
                    } else if (argument.getId().equals("api2")) {
                        assertThat(argument.getPayload()).isEqualTo("{\\n");
                    } else {
                        return false;
                    }
                    return true;
                })
            );
        verifyNoMoreInteractions(eventLatestRepository);
    }

    @Test
    public void should_create_latest_events_from_existing_dictionaries() throws TechnicalException {
        Dictionary dictionary1 = new Dictionary();
        dictionary1.setId("dictionary1");
        dictionary1.setType(DictionaryType.DYNAMIC);
        Dictionary dictionary2 = new Dictionary();
        dictionary2.setId("dictionary2");
        dictionary2.setType(DictionaryType.MANUAL);
        when(dictionaryRepository.findAll()).thenReturn(Set.of(dictionary1, dictionary2));
        Event event1 = new Event();
        event1.setType(EventType.PUBLISH_DICTIONARY);
        when(
            eventRepository.search(
                EventCriteria
                    .builder()
                    .property(Event.EventProperties.DICTIONARY_ID.getValue(), dictionary1.getId())
                    .types(Set.of(EventType.PUBLISH_DICTIONARY, EventType.UNPUBLISH_DICTIONARY))
                    .build(),
                new PageableBuilder().pageNumber(0).pageSize(1).build()
            )
        )
            .thenReturn(new Page<>(List.of(event1), 0, 1, 1));
        when(eventLatestRepository.createOrUpdate(event1)).thenReturn(event1);
        Event event2 = new Event();
        event2.setType(EventType.STOP_DICTIONARY);
        when(
            eventRepository.search(
                EventCriteria
                    .builder()
                    .property(Event.EventProperties.DICTIONARY_ID.getValue(), dictionary1.getId())
                    .types(Set.of(EventType.START_DICTIONARY, EventType.STOP_DICTIONARY))
                    .build(),
                new PageableBuilder().pageNumber(0).pageSize(1).build()
            )
        )
            .thenReturn(new Page<>(List.of(event2), 0, 1, 1));
        when(eventLatestRepository.createOrUpdate(event2)).thenReturn(event2);
        Event event3 = new Event();
        event3.setType(EventType.UNPUBLISH_DICTIONARY);
        when(
            eventRepository.search(
                EventCriteria
                    .builder()
                    .property(Event.EventProperties.DICTIONARY_ID.getValue(), dictionary2.getId())
                    .types(Set.of(EventType.PUBLISH_DICTIONARY, EventType.UNPUBLISH_DICTIONARY))
                    .build(),
                new PageableBuilder().pageNumber(0).pageSize(1).build()
            )
        )
            .thenReturn(new Page<>(List.of(event3), 0, 1, 1));
        when(eventLatestRepository.createOrUpdate(event3)).thenReturn(event3);

        cut.upgrade();

        verify(eventLatestRepository, times(3)).createOrUpdate(eventCaptor.capture());
        var eventsSaved = eventCaptor.getAllValues();
        assertThat(eventsSaved)
            .extracting(Event::getId, Event::getType)
            .containsExactlyInAnyOrder(
                tuple("dictionary1" + EventService.EVENT_LATEST_DYNAMIC_SUFFIX, EventType.PUBLISH_DICTIONARY),
                tuple("dictionary1", EventType.STOP_DICTIONARY),
                tuple("dictionary2", EventType.UNPUBLISH_DICTIONARY)
            );

        verifyNoMoreInteractions(eventLatestRepository);
    }

    @Test
    public void should_create_latest_events_from_existing_organizations() throws TechnicalException {
        Organization organization1 = new Organization();
        organization1.setId("organization1");
        Organization organization2 = new Organization();
        organization2.setId("organization2");
        when(organizationRepository.findAll()).thenReturn(Set.of(organization1, organization2));
        Event event1 = new Event();
        when(
            eventRepository.search(
                EventCriteria.builder().property(Event.EventProperties.ORGANIZATION_ID.getValue(), organization1.getId()).build(),
                new PageableBuilder().pageNumber(0).pageSize(1).build()
            )
        )
            .thenReturn(new Page<>(List.of(event1), 0, 1, 1));
        when(eventLatestRepository.createOrUpdate(event1)).thenReturn(event1);
        Event event2 = new Event();
        when(
            eventRepository.search(
                EventCriteria.builder().property(Event.EventProperties.ORGANIZATION_ID.getValue(), organization2.getId()).build(),
                new PageableBuilder().pageNumber(0).pageSize(1).build()
            )
        )
            .thenReturn(new Page<>(List.of(event2), 0, 1, 1));
        when(eventLatestRepository.createOrUpdate(event2)).thenReturn(event2);

        cut.upgrade();

        verify(eventLatestRepository).createOrUpdate(event1);
        verify(eventLatestRepository).createOrUpdate(event2);
        verifyNoMoreInteractions(eventLatestRepository);
    }

    @Test
    public void should_create_latest_events_from_existing_apis_with_multiple_pages() throws TechnicalException {
        // Prepare pages
        Map<Integer, List<String>> pageMapping = new HashMap<>();
        int index = 0;
        for (int i = 1; i <= 5; i++) {
            List<String> apis = new ArrayList<>();
            for (int j = 0; j < 100; j++) {
                apis.add("api" + index);
                index++;
            }
            pageMapping.put(i, apis);
        }

        when(apiRepository.searchIds(eq(List.of()), any(), eq(null)))
            .thenReturn(new Page<>(pageMapping.get(1), 0, 100, 500))
            .thenReturn(new Page<>(pageMapping.get(2), 1, 100, 500))
            .thenReturn(new Page<>(pageMapping.get(3), 2, 100, 500))
            .thenReturn(new Page<>(pageMapping.get(4), 3, 100, 500))
            .thenReturn(new Page<>(pageMapping.get(5), 4, 100, 500));

        for (int i = 0; i < 500; i++) {
            Event event = new Event();
            when(
                eventRepository.search(
                    EventCriteria.builder().property(Event.EventProperties.API_ID.getValue(), "api" + i).build(),
                    new PageableBuilder().pageNumber(0).pageSize(1).build()
                )
            )
                .thenReturn(new Page<>(List.of(event), 0, 1, 1));
            when(eventLatestRepository.createOrUpdate(event)).thenReturn(event);
        }
        cut.upgrade();

        verify(eventLatestRepository, times(500)).createOrUpdate(any());
    }

    @Test
    public void test_order() {
        Assert.assertEquals(UpgraderOrder.EVENTS_LATEST_UPGRADER, cut.getOrder());
    }
}
