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
package io.gravitee.repository.management.api;

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EventGroupKeyHelperTest {

    @Test
    void should_group_api_events_by_api_id() {
        // Given
        Map<String, String> properties = new HashMap<>();
        properties.put(Event.EventProperties.API_ID.getValue(), "api-123");

        // When
        EventRepository.EventToCleanGroup result = EventRepository.EventGroupKeyHelper.determineGroup(EventType.PUBLISH_API, properties);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.type()).isEqualTo("PUBLISH_API");
        assertThat(result.referenceId()).isEqualTo("api-123");
    }

    @Test
    void should_group_dictionary_events_by_dictionary_id() {
        // Given
        EventType[] dictionaryEventTypes = {
            EventType.PUBLISH_DICTIONARY,
            EventType.UNPUBLISH_DICTIONARY,
            EventType.START_DICTIONARY,
            EventType.STOP_DICTIONARY,
        };

        Map<String, String> properties = new HashMap<>();
        properties.put(Event.EventProperties.DICTIONARY_ID.getValue(), "dict-789");

        // When & Then
        for (EventType eventType : dictionaryEventTypes) {
            EventRepository.EventToCleanGroup result = EventRepository.EventGroupKeyHelper.determineGroup(eventType, properties);
            assertThat(result).isNotNull();
            assertThat(result.type()).isEqualTo(eventType.name());
            assertThat(result.referenceId()).isEqualTo("dict-789");
        }
    }

    @Test
    void should_group_gateway_events_by_gateway_id() {
        // Given
        EventType[] gatewayEventTypes = { EventType.GATEWAY_STARTED, EventType.GATEWAY_STOPPED };

        Map<String, String> properties = new HashMap<>();
        properties.put(Event.EventProperties.ID.getValue(), "gw-303");

        // When & Then
        for (EventType eventType : gatewayEventTypes) {
            EventRepository.EventToCleanGroup result = EventRepository.EventGroupKeyHelper.determineGroup(eventType, properties);
            assertThat(result).isNotNull();
            assertThat(result.type()).isEqualTo(eventType.name());
            assertThat(result.referenceId()).isEqualTo("gw-303");
        }
    }

    @Test
    void should_return_null_when_event_type_is_null() {
        // Given
        Map<String, String> properties = new HashMap<>();
        properties.put(Event.EventProperties.API_ID.getValue(), "api-123");

        // When
        EventRepository.EventToCleanGroup result = EventRepository.EventGroupKeyHelper.determineGroup(null, properties);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void should_return_null_when_properties_is_null() {
        // Given
        EventType eventType = EventType.PUBLISH_API;

        // When
        EventRepository.EventToCleanGroup result = EventRepository.EventGroupKeyHelper.determineGroup(eventType, null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void should_return_null_when_required_property_is_missing() {
        // Given
        EventType eventType = EventType.PUBLISH_API;
        Map<String, String> properties = new HashMap<>();
        // Missing API_ID property

        // When
        EventRepository.EventToCleanGroup result = EventRepository.EventGroupKeyHelper.determineGroup(eventType, properties);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void should_return_null_when_required_property_is_empty() {
        // Given
        EventType eventType = EventType.PUBLISH_API;
        Map<String, String> properties = new HashMap<>();
        properties.put(Event.EventProperties.API_ID.getValue(), "");

        // When
        EventRepository.EventToCleanGroup result = EventRepository.EventGroupKeyHelper.determineGroup(eventType, properties);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void should_return_null_when_required_property_is_blank() {
        // Given
        EventType eventType = EventType.PUBLISH_API;
        Map<String, String> properties = new HashMap<>();
        properties.put(Event.EventProperties.API_ID.getValue(), "   ");

        // When
        EventRepository.EventToCleanGroup result = EventRepository.EventGroupKeyHelper.determineGroup(eventType, properties);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void should_handle_events_with_multiple_properties() {
        // Given
        Map<String, String> properties = new HashMap<>();
        properties.put(Event.EventProperties.API_ID.getValue(), "api-123");
        properties.put(Event.EventProperties.DICTIONARY_ID.getValue(), "dict-456");
        properties.put(Event.EventProperties.ORGANIZATION_ID.getValue(), "org-789");

        // When
        EventRepository.EventToCleanGroup result = EventRepository.EventGroupKeyHelper.determineGroup(EventType.PUBLISH_API, properties);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.type()).isEqualTo("PUBLISH_API");
        assertThat(result.referenceId()).isEqualTo("api-123");
    }

    @Test
    void should_handle_events_with_extra_properties() {
        // Given
        Map<String, String> properties = new HashMap<>();
        properties.put(Event.EventProperties.API_ID.getValue(), "api-123");
        properties.put("extra_property", "extra_value");
        properties.put("another_property", "another_value");

        // When
        EventRepository.EventToCleanGroup result = EventRepository.EventGroupKeyHelper.determineGroup(EventType.PUBLISH_API, properties);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.type()).isEqualTo("PUBLISH_API");
        assertThat(result.referenceId()).isEqualTo("api-123");
    }
}
