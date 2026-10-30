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
package io.gravitee.gateway.services.sync.process.repository.fetcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.EventLatestCursor;
import io.gravitee.repository.management.api.search.EventLatestPage;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LatestEventFetcherTest {

    private static final EventLatestCursor POSITION = new EventLatestCursor(LocalDateTime.of(2026, 3, 29, 2, 30, 0, 123_456_000), "e2");

    @Mock
    private EventLatestRepository eventLatestRepository;

    private LatestEventFetcher cut;

    @BeforeEach
    public void beforeEach() {
        cut = new LatestEventFetcher(eventLatestRepository, 1);
    }

    @Test
    void should_fetch_latest_event() {
        Event event = event("e1", 1000);
        when(eventLatestRepository.searchAfter(any(), any(), any(), anyInt())).thenReturn(page(event)).thenReturn(page());
        cut
            .fetchLatest(null, null, Event.EventProperties.API_ID, Set.of(), Set.of())
            .test()
            .assertValueCount(1)
            .assertValue(events -> events.contains(event));
    }

    @Test
    void should_fetch_latest_event_and_complete_if_page_size_is_higher_than_results() {
        cut = new LatestEventFetcher(eventLatestRepository, 10);
        Event event = event("e1", 1000);
        when(eventLatestRepository.searchAfter(any(), any(), any(), anyInt())).thenReturn(page(event)).thenReturn(page());
        cut
            .fetchLatest(null, null, Event.EventProperties.API_ID, Set.of(), Set.of())
            .test()
            .assertValueCount(1)
            .assertValue(events -> events.contains(event))
            .assertComplete();
    }

    @Test
    void should_fetch_event_with_criteria() {
        Instant to = Instant.now();
        Instant from = to.minus(1000, ChronoUnit.MILLIS);
        Event event = event("e1", 1000);
        when(
            eventLatestRepository.searchAfter(
                argThat(
                    argument ->
                        argument.getTypes().size() == 1 &&
                        argument.getTypes().contains(EventType.PUBLISH_API) &&
                        argument.getEnvironments().contains("env") &&
                        argument.getFrom() < from.toEpochMilli() &&
                        argument.getTo() > to.toEpochMilli()
                ),
                eq(Event.EventProperties.API_ID),
                isNull(),
                eq(1)
            )
        ).thenReturn(page(event));
        cut
            .fetchLatest(from.toEpochMilli(), to.toEpochMilli(), Event.EventProperties.API_ID, Set.of("env"), Set.of(EventType.PUBLISH_API))
            .test()
            .assertValueCount(1)
            .assertValue(apiKeys -> apiKeys.contains(event));
    }

    @Test
    void should_fetch_new_latest_events_on_each_downstream_request() {
        Event event1 = event("e1", 1000);
        Event event2 = event("e2", 2000);
        Event event3 = event("e3", 3000);
        when(eventLatestRepository.searchAfter(any(), any(), any(), anyInt()))
            .thenReturn(page(event1))
            .thenReturn(page(event2))
            .thenReturn(page(event3))
            .thenReturn(page());
        cut
            .fetchLatest(null, null, Event.EventProperties.API_ID, Set.of(), Set.of())
            .test(0)
            .requestMore(1)
            .assertValueAt(0, List.of(event1))
            .requestMore(1)
            .assertValueAt(1, List.of(event2))
            .requestMore(1)
            .assertValueAt(2, List.of(event3))
            .requestMore(1)
            .assertComplete()
            .assertValueCount(3);
        verify(eventLatestRepository, times(4)).searchAfter(any(), any(), any(), anyInt());
    }

    @Test
    void should_seek_each_page_after_the_last_event_of_the_previous_one() {
        cut = new LatestEventFetcher(eventLatestRepository, 2);
        when(eventLatestRepository.searchAfter(any(), any(), isNull(), eq(2))).thenReturn(
            new EventLatestPage(List.of(event("e1", 1000), event("e2", 2000)), POSITION)
        );
        when(eventLatestRepository.searchAfter(any(), any(), eq(POSITION), eq(2))).thenReturn(page(event("e3", 2000)));

        cut.fetchLatest(null, null, Event.EventProperties.AUTHZ_ENTITY_ID, Set.of(), Set.of()).test().assertComplete().assertValueCount(2);

        verify(eventLatestRepository, never()).search(any(), any(), any(), any());
    }

    @Test
    void should_page_by_offset_when_repository_has_no_keyset_support() {
        cut = new LatestEventFetcher(eventLatestRepository, 2);
        when(eventLatestRepository.searchAfter(any(), any(), any(), anyInt())).thenThrow(new UnsupportedOperationException());
        when(eventLatestRepository.search(any(), any(), eq(0L), eq(2L))).thenReturn(List.of(event("e1", 1000), event("e2", 2000)));
        when(eventLatestRepository.search(any(), any(), eq(1L), eq(2L))).thenReturn(List.of(event("e3", 3000)));

        cut.fetchLatest(null, null, Event.EventProperties.API_ID, Set.of(), Set.of()).test().assertComplete().assertValueCount(2);

        verify(eventLatestRepository, times(1)).searchAfter(any(), any(), any(), anyInt());
    }

    @Test
    void should_fail_when_keyset_paging_stops_being_supported_after_the_first_page() {
        cut = new LatestEventFetcher(eventLatestRepository, 2);
        when(eventLatestRepository.searchAfter(any(), any(), isNull(), eq(2))).thenReturn(
            new EventLatestPage(List.of(event("e1", 1000), event("e2", 2000)), POSITION)
        );
        when(eventLatestRepository.searchAfter(any(), any(), eq(POSITION), eq(2))).thenThrow(new UnsupportedOperationException());

        cut
            .fetchLatest(null, null, Event.EventProperties.API_ID, Set.of(), Set.of())
            .test()
            .assertError(UnsupportedOperationException.class);

        verify(eventLatestRepository, never()).search(any(), any(), any(), any());
    }

    @Test
    void should_seek_after_an_event_without_update_date_by_its_id() {
        cut = new LatestEventFetcher(eventLatestRepository, 2);
        when(eventLatestRepository.searchAfter(any(), any(), isNull(), eq(2))).thenReturn(page(event("u1", null), event("u2", null)));
        when(eventLatestRepository.searchAfter(any(), any(), eq(new EventLatestCursor(null, "u2")), eq(2))).thenReturn(
            page(event("e1", 1000))
        );

        cut.fetchLatest(null, null, Event.EventProperties.API_ID, Set.of(), Set.of()).test().assertComplete().assertValueCount(2);

        verify(eventLatestRepository, never()).search(any(), any(), any(), any());
    }

    @Test
    void should_not_fetch_new_latest_event_without_downstream_request() {
        cut.fetchLatest(null, null, Event.EventProperties.API_ID, Set.of(), Set.of()).test(0).assertNotComplete();
        verifyNoInteractions(eventLatestRepository);
    }

    @Test
    void should_emit_on_error_when_repository_thrown_exception() {
        when(eventLatestRepository.searchAfter(any(), any(), any(), anyInt())).thenThrow(new RuntimeException());
        cut.fetchLatest(null, null, Event.EventProperties.API_ID, Set.of(), Set.of()).test().assertError(RuntimeException.class);
    }

    @Test
    void fetchLatest_with_property_adds_property_to_criteria() {
        LatestEventFetcher fetcher = new LatestEventFetcher(eventLatestRepository, 10);
        ArgumentCaptor<EventCriteria> captor = ArgumentCaptor.forClass(EventCriteria.class);
        when(eventLatestRepository.searchAfter(captor.capture(), any(), any(), anyInt())).thenReturn(page());

        fetcher
            .fetchLatest(
                -1L,
                null,
                Event.EventProperties.AUTHZ_PDP_ID,
                Event.EventProperties.GATEWAY_ID,
                "gw-1",
                Set.of("env-1"),
                Set.of(EventType.PUBLISH_AUTHZ_PDP)
            )
            .test()
            .awaitDone(5, TimeUnit.SECONDS);

        assertThat(captor.getValue().getProperties()).containsEntry(Event.EventProperties.GATEWAY_ID.getValue(), "gw-1");
    }

    @Test
    void should_emit_nothing_when_fetching_events_for_empty_api_ids() {
        cut.fetchLatestForApiIds(Set.of(), Set.of("env"), Set.of(EventType.PUBLISH_API)).test().assertComplete().assertNoValues();
        cut.fetchLatestForApiIds(null, Set.of("env"), Set.of(EventType.PUBLISH_API)).test().assertComplete().assertNoValues();
        verifyNoInteractions(eventLatestRepository);
    }

    @Test
    void should_fetch_latest_events_for_specific_api_ids() {
        LatestEventFetcher fetcher = new LatestEventFetcher(eventLatestRepository, 2);
        Event event = new Event();
        when(eventLatestRepository.search(any(), eq(Event.EventProperties.API_ID), eq(0L), eq(2L))).thenReturn(List.of(event));

        fetcher
            .fetchLatestForApiIds(Set.of("api-1", "api-2"), Set.of("env"), Set.of(EventType.START_API))
            .test()
            .assertComplete()
            .assertValue(List.of(event));
    }

    @Test
    void should_page_through_events_for_specific_api_ids() {
        // A full page means there may be more: the fetcher must ask for the next one instead of
        // loading every member API's event in one unpaginated search.
        LatestEventFetcher fetcher = new LatestEventFetcher(eventLatestRepository, 2);
        List<Event> fullPage = List.of(new Event(), new Event());
        List<Event> lastPage = List.of(new Event());
        when(eventLatestRepository.search(any(), eq(Event.EventProperties.API_ID), eq(0L), eq(2L))).thenReturn(fullPage);
        when(eventLatestRepository.search(any(), eq(Event.EventProperties.API_ID), eq(1L), eq(2L))).thenReturn(lastPage);

        fetcher
            .fetchLatestForApiIds(Set.of("api-1"), Set.of("env"), Set.of(EventType.PUBLISH_API))
            .test()
            .assertComplete()
            .assertValues(fullPage, lastPage);
    }

    @Test
    void should_end_the_stream_when_repository_search_fails_for_api_ids() {
        when(eventLatestRepository.search(any(), any(), any(), any())).thenThrow(new RuntimeException("db down"));

        cut.fetchLatestForApiIds(Set.of("api-1"), Set.of("env"), Set.of(EventType.PUBLISH_API)).test().assertComplete().assertNoValues();
    }

    private static EventLatestPage page(Event... events) {
        EventLatestCursor next = events.length == 0 ? null : new EventLatestCursor(null, events[events.length - 1].getId());
        return new EventLatestPage(List.of(events), next);
    }

    private static Event event(String id, Integer updatedAt) {
        Event event = new Event();
        event.setId(id);
        event.setUpdatedAt(updatedAt == null ? null : new Date(updatedAt));
        return event;
    }
}
