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
package io.gravitee.apim.infra.query_service.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.gravitee.apim.core.event.model.Event;
import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.EventLatestCursor;
import io.gravitee.repository.management.api.search.EventLatestPage;
import io.gravitee.repository.management.model.EventType;
import io.gravitee.rest.api.service.exceptions.TechnicalManagementException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventLatestQueryServiceImplTest {

    @Mock
    private EventLatestRepository eventLatestRepository;

    private EventLatestQueryServiceImpl cut;

    @BeforeEach
    void setUp() {
        cut = new EventLatestQueryServiceImpl(eventLatestRepository);
    }

    @Test
    void should_find_all_by_type_and_environments() {
        var repoEvent = new io.gravitee.repository.management.model.Event();
        repoEvent.setId("event-1");
        repoEvent.setType(EventType.DEPLOY_CLUSTER);
        repoEvent.setPayload("payload");
        repoEvent.setProperties(Map.of());

        when(
            eventLatestRepository.search(
                any(EventCriteria.class),
                eq(io.gravitee.repository.management.model.Event.EventProperties.CLUSTER_ID),
                isNull(),
                isNull()
            )
        ).thenReturn(List.of(repoEvent));

        List<Event> result = cut.findAllByTypeAndEnvironments(
            Set.of(io.gravitee.rest.api.model.EventType.DEPLOY_CLUSTER),
            Set.of("env-1"),
            Event.EventProperties.CLUSTER_ID
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPayload()).isEqualTo("payload");
    }

    @Test
    void should_return_empty_list_when_no_events() {
        when(eventLatestRepository.search(any(EventCriteria.class), any(), isNull(), isNull())).thenReturn(List.of());

        List<Event> result = cut.findAllByTypeAndEnvironments(
            Set.of(io.gravitee.rest.api.model.EventType.DEPLOY_CLUSTER),
            Set.of("env-1"),
            Event.EventProperties.CLUSTER_ID
        );

        assertThat(result).isEmpty();
    }

    @Test
    void should_pass_correct_criteria_to_repository() {
        when(eventLatestRepository.search(any(EventCriteria.class), any(), isNull(), isNull())).thenReturn(List.of());

        var criteriaCaptor = ArgumentCaptor.forClass(EventCriteria.class);

        cut.findAllByTypeAndEnvironments(
            Set.of(io.gravitee.rest.api.model.EventType.DEPLOY_CLUSTER, io.gravitee.rest.api.model.EventType.UNDEPLOY_CLUSTER),
            Set.of("env-1", "env-2"),
            Event.EventProperties.CLUSTER_ID
        );

        org.mockito.Mockito.verify(eventLatestRepository).search(
            criteriaCaptor.capture(),
            eq(io.gravitee.repository.management.model.Event.EventProperties.CLUSTER_ID),
            isNull(),
            isNull()
        );

        EventCriteria captured = criteriaCaptor.getValue();
        assertThat(captured.getTypes()).containsExactlyInAnyOrder(EventType.DEPLOY_CLUSTER, EventType.UNDEPLOY_CLUSTER);
        assertThat(captured.getEnvironments()).containsExactlyInAnyOrder("env-1", "env-2");
    }

    @Test
    void should_find_all_by_types_expiring_and_updated_before_instants() {
        when(eventLatestRepository.searchAfter(any(EventCriteria.class), isNull(), isNull(), anyInt())).thenReturn(
            new EventLatestPage(List.of(), null)
        );
        var criteriaCaptor = ArgumentCaptor.forClass(EventCriteria.class);

        cut
            .findAll(
                Set.of(io.gravitee.rest.api.model.EventType.UNPUBLISH_GENERIC_EVENT),
                Instant.ofEpochMilli(2000),
                Instant.ofEpochMilli(1000)
            )
            .toList();

        org.mockito.Mockito.verify(eventLatestRepository).searchAfter(criteriaCaptor.capture(), isNull(), isNull(), anyInt());
        EventCriteria captured = criteriaCaptor.getValue();
        assertThat(captured.getTypes()).containsExactly(EventType.UNPUBLISH_GENERIC_EVENT);
        assertThat(captured.getProperties()).isEmpty();
        assertThat(captured.getExpiresBefore()).isEqualTo(2000);
        assertThat(captured.getTo()).isEqualTo(1000);
        assertThat(captured.getEnvironments()).isEmpty();
    }

    @Test
    void should_find_all_across_pages() {
        var full = IntStream.range(0, EventLatestQueryServiceImpl.FIND_ALL_PAGE_SIZE)
            .mapToObj(i -> io.gravitee.repository.management.model.Event.builder().id("event-" + i).properties(Map.of()).build())
            .toList();
        var cursor = new EventLatestCursor(null, "event-99");
        when(eventLatestRepository.searchAfter(any(EventCriteria.class), isNull(), isNull(), anyInt())).thenReturn(
            new EventLatestPage(full, cursor)
        );
        when(eventLatestRepository.searchAfter(any(EventCriteria.class), isNull(), eq(cursor), anyInt())).thenReturn(
            new EventLatestPage(
                List.of(io.gravitee.repository.management.model.Event.builder().id("last").properties(Map.of()).build()),
                null
            )
        );

        var events = cut.findAll(Set.of(io.gravitee.rest.api.model.EventType.UNPUBLISH_GENERIC_EVENT), null, null).iterator();

        for (var i = 0; i < EventLatestQueryServiceImpl.FIND_ALL_PAGE_SIZE; i++) {
            events.next();
        }
        verify(eventLatestRepository, never()).searchAfter(any(EventCriteria.class), isNull(), eq(cursor), anyInt());
        assertThat(events.next().getId()).isEqualTo("last");
        assertThat(events.hasNext()).isFalse();
    }

    @Test
    void should_throw_technical_exception_on_error() {
        when(eventLatestRepository.search(any(EventCriteria.class), any(), isNull(), isNull())).thenThrow(new RuntimeException("DB error"));

        assertThatThrownBy(() ->
            cut.findAllByTypeAndEnvironments(
                Set.of(io.gravitee.rest.api.model.EventType.DEPLOY_CLUSTER),
                Set.of("env-1"),
                Event.EventProperties.CLUSTER_ID
            )
        )
            .isInstanceOf(TechnicalManagementException.class)
            .hasMessageContaining("DB error");
    }
}
