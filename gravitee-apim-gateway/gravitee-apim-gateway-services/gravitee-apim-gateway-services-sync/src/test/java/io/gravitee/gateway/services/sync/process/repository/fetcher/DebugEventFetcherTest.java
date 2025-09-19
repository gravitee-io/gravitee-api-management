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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import io.gravitee.node.api.Node;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventRepository;
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
class DebugEventFetcherTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private Node node;

    private DebugEventFetcher cut;

    @BeforeEach
    public void beforeEach() {
        cut = new DebugEventFetcher(eventRepository, node);
    }

    @Test
    void should_fetch_debug_event() {
        Event event = new Event();
        when(eventRepository.search(any())).thenReturn(List.of(event));
        cut
            .fetchLatest(null, null, Set.of())
            .test()
            .assertValueCount(1)
            .assertValue(apiKeys -> apiKeys.contains(event));
    }

    @Test
    void should_fetch_event_with_criteria() {
        Instant to = Instant.now();
        Instant from = to.minus(1000, ChronoUnit.MILLIS);
        Event event = new Event();
        when(
            eventRepository.search(
                argThat(
                    argument ->
                        argument.getTypes().size() == 1 &&
                        argument.getTypes().contains(EventType.DEBUG_API) &&
                        argument.getProperties().containsKey(Event.EventProperties.API_DEBUG_STATUS.getValue()) &&
                        argument.getProperties().containsKey(Event.EventProperties.GATEWAY_ID.getValue()) &&
                        argument.getEnvironments().contains("env") &&
                        argument.getFrom() < from.toEpochMilli() &&
                        argument.getTo() > to.toEpochMilli()
                )
            )
        ).thenReturn(List.of(event));
        cut
            .fetchLatest(from.toEpochMilli(), to.toEpochMilli(), Set.of("env"))
            .test()
            .assertValueCount(1)
            .assertValue(apiKeys -> apiKeys.contains(event));
    }

    @Test
    void should_emit_on_error_when_repository_thrown_exception() {
        when(eventRepository.search(any())).thenThrow(new RuntimeException());
        cut.fetchLatest(null, null, Set.of()).test().assertError(RuntimeException.class);
    }
}
