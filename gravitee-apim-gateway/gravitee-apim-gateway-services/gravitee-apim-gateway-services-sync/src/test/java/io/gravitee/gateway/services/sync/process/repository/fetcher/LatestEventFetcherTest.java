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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class LatestEventFetcherTest {

    @Mock
    private EventLatestRepository eventLatestRepository;

    private LatestEventFetcher cut;

    @BeforeEach
    public void beforeEach() {
        cut = new LatestEventFetcher(eventLatestRepository, 1);
    }

    @Test
    void should_fetch_latest_event() {
        Event event = new Event();
        when(eventLatestRepository.search(any(), any(), any(), any())).thenReturn(List.of(event)).thenReturn(null);
        cut
            .fetchLatest(null, null, Event.EventProperties.API_ID, Set.of(), Set.of())
            .test()
            .assertValueCount(1)
            .assertValue(events -> events.contains(event));
    }

    @Test
    void should_fetch_latest_event_and_complete_if_page_size_is_higher_than_results() {
        cut = new LatestEventFetcher(eventLatestRepository, 10);
        Event event = new Event();
        when(eventLatestRepository.search(any(), any(), any(), any())).thenReturn(List.of(event)).thenReturn(null);
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
        Event event = new Event();
        when(
            eventLatestRepository.search(
                argThat(
                    argument ->
                        argument.getTypes().size() == 1 &&
                        argument.getTypes().contains(EventType.PUBLISH_API) &&
                        argument.getEnvironments().contains("env") &&
                        argument.getFrom() < from.toEpochMilli() &&
                        argument.getTo() > to.toEpochMilli()
                ),
                eq(Event.EventProperties.API_ID),
                eq(0L),
                eq(1L)
            )
        ).thenReturn(List.of(event));
        cut
            .fetchLatest(from.toEpochMilli(), to.toEpochMilli(), Event.EventProperties.API_ID, Set.of("env"), Set.of(EventType.PUBLISH_API))
            .test()
            .assertValueCount(1)
            .assertValue(apiKeys -> apiKeys.contains(event));
    }

    @Test
    void should_fetch_new_latest_events_on_each_downstream_request() {
        Event event1 = new Event();
        Event event2 = new Event();
        Event event3 = new Event();
        when(eventLatestRepository.search(any(), any(), any(), any()))
            .thenReturn(List.of(event1))
            .thenReturn(List.of(event2))
            .thenReturn(List.of(event3))
            .thenReturn(null);
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
        verify(eventLatestRepository, times(4)).search(any(), any(), any(), any());
    }

    @Test
    void should_not_fetch_new_latest_event_without_downstream_request() {
        cut.fetchLatest(null, null, Event.EventProperties.API_ID, Set.of(), Set.of()).test(0).assertNotComplete();
        verifyNoInteractions(eventLatestRepository);
    }

    @Test
    void should_emit_on_error_when_repository_thrown_exception() {
        when(eventLatestRepository.search(any(), any(), any(), any())).thenThrow(new RuntimeException());
        cut.fetchLatest(null, null, Event.EventProperties.API_ID, Set.of(), Set.of()).test().assertError(RuntimeException.class);
    }
}
