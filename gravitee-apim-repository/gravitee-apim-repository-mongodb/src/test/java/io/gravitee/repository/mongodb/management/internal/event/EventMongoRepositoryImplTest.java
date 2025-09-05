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
package io.gravitee.repository.mongodb.management.internal.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.mongodb.management.internal.model.EventMongo;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@ExtendWith(MockitoExtension.class)
class EventMongoRepositoryImplTest {

    @Mock
    private MongoTemplate mongoTemplate;

    private EventMongoRepositoryImpl eventMongoRepository;

    @BeforeEach
    void setUp() {
        eventMongoRepository = new EventMongoRepositoryImpl();
        // Use reflection to set the mongoTemplate field
        try {
            java.lang.reflect.Field field = EventMongoRepositoryImpl.class.getDeclaredField("mongoTemplate");
            field.setAccessible(true);
            field.set(eventMongoRepository, mongoTemplate);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set mongoTemplate field", e);
        }
    }

    @Test
    void should_find_events_to_clean_with_grouping() {
        // Given
        String environmentId = "DEFAULT";

        EventMongo event1 = createEventMongo("event1", "PUBLISH_API", Map.of("api_id", "api-123"));
        EventMongo event2 = createEventMongo("event2", "PUBLISH_API", Map.of("api_id", "api-123"));
        EventMongo event3 = createEventMongo("event3", "PUBLISH_DICTIONARY", Map.of("dictionary_id", "dict-456"));
        EventMongo event4 = createEventMongo("event4", "PUBLISH_ORGANIZATION", Map.of("organization_id", "org-789"));

        Stream<EventMongo> eventStream = Stream.of(event1, event2, event3, event4);

        when(mongoTemplate.stream(any(Query.class), eq(EventMongo.class))).thenReturn(eventStream);

        // When
        Stream<EventRepository.EventToClean> result = eventMongoRepository.findEventsToClean(environmentId);

        // Then
        List<EventRepository.EventToClean> eventsToClean = result.toList();
        assertThat(eventsToClean).hasSize(4);

        // Check API events are grouped correctly
        List<EventRepository.EventToClean> apiEvents = eventsToClean
            .stream()
            .filter(event -> event.group().type().equals("PUBLISH_API"))
            .toList();
        assertThat(apiEvents).hasSize(2);
        assertThat(apiEvents).allSatisfy(event -> assertThat(event.group().referenceId()).isEqualTo("api-123"));

        // Check dictionary events are grouped correctly
        List<EventRepository.EventToClean> dictEvents = eventsToClean
            .stream()
            .filter(event -> event.group().type().equals("PUBLISH_DICTIONARY"))
            .toList();
        assertThat(dictEvents).hasSize(1);
        assertThat(dictEvents.get(0).group().referenceId()).isEqualTo("dict-456");

        // Check organization events are grouped correctly
        List<EventRepository.EventToClean> orgEvents = eventsToClean
            .stream()
            .filter(event -> event.group().type().equals("PUBLISH_ORGANIZATION"))
            .toList();
        assertThat(orgEvents).hasSize(1);
        assertThat(orgEvents.get(0).group().referenceId()).isEqualTo("org-789");
    }

    @Test
    void should_filter_out_events_without_required_properties() {
        // Given
        String environmentId = "DEFAULT";

        EventMongo eventWithProps = createEventMongo("event-with-props", "PUBLISH_API", Map.of("api_id", "api-123"));
        EventMongo eventWithoutProps = createEventMongo("event-without-props", "PUBLISH_API", Map.of()); // No api_id
        EventMongo eventWithNullProps = createEventMongo("event-null-props", "PUBLISH_API", null);

        Stream<EventMongo> eventStream = Stream.of(eventWithProps, eventWithoutProps, eventWithNullProps);

        when(mongoTemplate.stream(any(Query.class), eq(EventMongo.class))).thenReturn(eventStream);

        // When
        Stream<EventRepository.EventToClean> result = eventMongoRepository.findEventsToClean(environmentId);

        // Then
        List<EventRepository.EventToClean> eventsToClean = result.toList();
        assertThat(eventsToClean).hasSize(1);
        assertThat(eventsToClean.get(0).id()).isEqualTo("event-with-props");
    }

    @Test
    void should_filter_out_events_with_empty_properties() {
        // Given
        String environmentId = "DEFAULT";

        EventMongo eventWithEmptyProps = createEventMongo("event-empty-props", "PUBLISH_API", Map.of("api_id", ""));
        EventMongo eventWithBlankProps = createEventMongo("event-blank-props", "PUBLISH_API", Map.of("api_id", "   "));

        Stream<EventMongo> eventStream = Stream.of(eventWithEmptyProps, eventWithBlankProps);

        when(mongoTemplate.stream(any(Query.class), eq(EventMongo.class))).thenReturn(eventStream);

        // When
        Stream<EventRepository.EventToClean> result = eventMongoRepository.findEventsToClean(environmentId);

        // Then
        List<EventRepository.EventToClean> eventsToClean = result.toList();
        assertThat(eventsToClean).isEmpty();
    }

    @Test
    void should_handle_mixed_event_types_correctly() {
        // Given
        String environmentId = "DEFAULT";

        EventMongo apiEvent = createEventMongo("api-event", "PUBLISH_API", Map.of("api_id", "api-1"));
        EventMongo dictEvent = createEventMongo("dict-event", "PUBLISH_DICTIONARY", Map.of("dictionary_id", "dict-1"));
        EventMongo orgEvent = createEventMongo("org-event", "PUBLISH_ORGANIZATION", Map.of("organization_id", "org-1"));
        EventMongo spgEvent = createEventMongo("spg-event", "DEPLOY_SHARED_POLICY_GROUP", Map.of("shared_policy_group_id", "spg-1"));
        EventMongo gwEvent = createEventMongo("gw-event", "GATEWAY_STARTED", Map.of("id", "gw-1"));

        Stream<EventMongo> eventStream = Stream.of(apiEvent, dictEvent, orgEvent, spgEvent, gwEvent);

        when(mongoTemplate.stream(any(Query.class), eq(EventMongo.class))).thenReturn(eventStream);

        // When
        Stream<EventRepository.EventToClean> result = eventMongoRepository.findEventsToClean(environmentId);

        // Then
        List<EventRepository.EventToClean> eventsToClean = result.toList();
        assertThat(eventsToClean).hasSize(5);

        // Verify each event type is grouped correctly
        assertThat(eventsToClean)
            .anySatisfy(event -> {
                assertThat(event.group().type()).isEqualTo("PUBLISH_API");
                assertThat(event.group().referenceId()).isEqualTo("api-1");
            });

        assertThat(eventsToClean)
            .anySatisfy(event -> {
                assertThat(event.group().type()).isEqualTo("PUBLISH_DICTIONARY");
                assertThat(event.group().referenceId()).isEqualTo("dict-1");
            });

        assertThat(eventsToClean)
            .anySatisfy(event -> {
                assertThat(event.group().type()).isEqualTo("PUBLISH_ORGANIZATION");
                assertThat(event.group().referenceId()).isEqualTo("org-1");
            });

        assertThat(eventsToClean)
            .anySatisfy(event -> {
                assertThat(event.group().type()).isEqualTo("DEPLOY_SHARED_POLICY_GROUP");
                assertThat(event.group().referenceId()).isEqualTo("spg-1");
            });

        assertThat(eventsToClean)
            .anySatisfy(event -> {
                assertThat(event.group().type()).isEqualTo("GATEWAY_STARTED");
                assertThat(event.group().referenceId()).isEqualTo("gw-1");
            });
    }

    @Test
    void should_handle_unknown_event_types_with_generic_id() {
        // Given
        String environmentId = "DEFAULT";

        EventMongo unknownEvent = createEventMongo("unknown-event", "UNKNOWN_EVENT_TYPE", Map.of("id", "unknown-123"));

        Stream<EventMongo> eventStream = Stream.of(unknownEvent);

        when(mongoTemplate.stream(any(Query.class), eq(EventMongo.class))).thenReturn(eventStream);

        // When
        Stream<EventRepository.EventToClean> result = eventMongoRepository.findEventsToClean(environmentId);

        // Then
        List<EventRepository.EventToClean> eventsToClean = result.toList();
        assertThat(eventsToClean).isEmpty();
    }

    @Test
    void should_handle_events_with_multiple_properties() {
        // Given
        String environmentId = "DEFAULT";

        EventMongo eventWithMultipleProps = createEventMongo(
            "multi-props-event",
            "PUBLISH_API",
            Map.of("api_id", "api-123", "dictionary_id", "dict-456", "organization_id", "org-789", "extra_property", "extra_value")
        );

        Stream<EventMongo> eventStream = Stream.of(eventWithMultipleProps);

        when(mongoTemplate.stream(any(Query.class), eq(EventMongo.class))).thenReturn(eventStream);

        // When
        Stream<EventRepository.EventToClean> result = eventMongoRepository.findEventsToClean(environmentId);

        // Then
        List<EventRepository.EventToClean> eventsToClean = result.toList();
        assertThat(eventsToClean).hasSize(1);
        assertThat(eventsToClean.get(0).group().type()).isEqualTo("PUBLISH_API");
        assertThat(eventsToClean.get(0).group().referenceId()).isEqualTo("api-123");
    }

    private EventMongo createEventMongo(String id, String type, Map<String, String> properties) {
        EventMongo event = new EventMongo();
        event.setId(id);
        event.setType(type);
        event.setProperties(properties);
        event.setCreatedAt(new Date());
        return event;
    }
}
