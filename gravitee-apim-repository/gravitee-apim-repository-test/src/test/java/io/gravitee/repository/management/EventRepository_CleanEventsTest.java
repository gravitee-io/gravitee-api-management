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
package io.gravitee.repository.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.not;

import io.gravitee.repository.management.api.EventRepository;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.assertj.core.api.Condition;
import org.junit.Test;

public class EventRepository_CleanEventsTest extends AbstractManagementRepositoryTest {

    // for the test the event from DEFAULT env don’t start with _ and others does
    static final Condition<EventRepository.EventToClean> DEFAULT_ENV = new Condition<>(
        e -> !e.id().startsWith("_"),
        "event from \"NOT-DEFAULT\" environment"
    );

    @Override
    protected String getTestCasesPath() {
        return "/data/event-cleaning/";
    }

    @Test
    public void findEventsToClean() {
        // when
        var events = eventRepository.findEventsToClean("DEFAULT").toList();

        // then
        assertThat(events)
            .doNotHave(not(DEFAULT_ENV))
            .filteredOn(DEFAULT_ENV)
            .hasSize(15)
            // to help the test, the dataset is created with order of creation date same as order of ids
            .isSortedAccordingTo(Comparator.comparing(EventRepository.EventToClean::id).reversed());
    }

    @Test
    public void should_group_events_by_type_and_reference_id() {
        // when
        var events = eventRepository.findEventsToClean("DEFAULT").toList();

        // then
        assertThat(events)
            .allSatisfy(event -> {
                assertThat(event.group()).isNotNull();
                assertThat(event.group().type()).isNotNull();
                assertThat(event.group().referenceId()).isNotNull();
            });
    }

    @Test
    public void should_group_api_events_correctly() {
        // when
        var events = eventRepository.findEventsToClean("DEFAULT").toList();

        // then
        List<EventRepository.EventToClean> apiEvents = events
            .stream()
            .filter(event ->
                event.group().type().startsWith("PUBLISH_API") ||
                event.group().type().startsWith("UNPUBLISH_API") ||
                event.group().type().startsWith("START_API") ||
                event.group().type().startsWith("STOP_API")
            )
            .collect(Collectors.toList());

        assertThat(apiEvents).isNotEmpty();
        assertThat(apiEvents)
            .allSatisfy(event -> {
                assertThat(event.group().referenceId()).isEqualTo("api-1");
            });
    }

    @Test
    public void should_filter_out_events_from_other_environments() {
        // when
        var events = eventRepository.findEventsToClean("NOT-DEFAULT").toList();

        // then
        assertThat(events).hasSize(3); // Should have 3 events from NOT-DEFAULT environment
        assertThat(events)
            .anySatisfy(event -> {
                assertThat(event.group().type()).isEqualTo("START_API");
                assertThat(event.group().referenceId()).isEqualTo("api-2");
            })
            .anySatisfy(event -> {
                assertThat(event.group().type()).isEqualTo("PUBLISH_DICTIONARY");
                assertThat(event.group().referenceId()).isEqualTo("dict-3");
            })
            .anySatisfy(event -> {
                assertThat(event.group().type()).isEqualTo("GATEWAY_STARTED");
                assertThat(event.group().referenceId()).isEqualTo("gateway-2");
            });
    }

    @Test
    public void should_group_dictionary_events_correctly() {
        // when
        var events = eventRepository.findEventsToClean("DEFAULT").toList();

        // then
        List<EventRepository.EventToClean> dictionaryEvents = events
            .stream()
            .filter(event ->
                event.group().type().startsWith("PUBLISH_DICTIONARY") ||
                event.group().type().startsWith("UNPUBLISH_DICTIONARY") ||
                event.group().type().startsWith("START_DICTIONARY") ||
                event.group().type().startsWith("STOP_DICTIONARY")
            )
            .collect(Collectors.toList());

        assertThat(dictionaryEvents).hasSize(4);
        assertThat(dictionaryEvents)
            .anySatisfy(event -> {
                assertThat(event.group().type()).isEqualTo("PUBLISH_DICTIONARY");
                assertThat(event.group().referenceId()).isEqualTo("dict-1");
            })
            .anySatisfy(event -> {
                assertThat(event.group().type()).isEqualTo("UNPUBLISH_DICTIONARY");
                assertThat(event.group().referenceId()).isEqualTo("dict-1");
            })
            .anySatisfy(event -> {
                assertThat(event.group().type()).isEqualTo("START_DICTIONARY");
                assertThat(event.group().referenceId()).isEqualTo("dict-2");
            })
            .anySatisfy(event -> {
                assertThat(event.group().type()).isEqualTo("STOP_DICTIONARY");
                assertThat(event.group().referenceId()).isEqualTo("dict-2");
            });
    }

    @Test
    public void should_group_gateway_events_correctly() {
        // when
        var events = eventRepository.findEventsToClean("DEFAULT").toList();

        // then
        List<EventRepository.EventToClean> gatewayEvents = events
            .stream()
            .filter(event -> event.group().type().startsWith("GATEWAY_STARTED") || event.group().type().startsWith("GATEWAY_STOPPED"))
            .collect(Collectors.toList());

        assertThat(gatewayEvents).hasSize(2);
        assertThat(gatewayEvents)
            .allSatisfy(event -> {
                assertThat(event.group().referenceId()).isEqualTo("gateway-1");
            })
            .anySatisfy(event -> {
                assertThat(event.group().type()).isEqualTo("GATEWAY_STARTED");
            })
            .anySatisfy(event -> {
                assertThat(event.group().type()).isEqualTo("GATEWAY_STOPPED");
            });
    }

    @Test
    public void should_group_organization_events_correctly() {
        // when
        var events = eventRepository.findEventsToClean("DEFAULT").toList();

        // then
        List<EventRepository.EventToClean> orgEvents = events
            .stream()
            .filter(event -> event.group().type().startsWith("PUBLISH_ORGANIZATION"))
            .collect(Collectors.toList());

        assertThat(orgEvents).hasSize(1);
        assertThat(orgEvents)
            .allSatisfy(event -> {
                assertThat(event.group().type()).isEqualTo("PUBLISH_ORGANIZATION");
                assertThat(event.group().referenceId()).isEqualTo("org-1");
            });
    }

    @Test
    public void should_handle_debug_api_events() {
        // when
        var events = eventRepository.findEventsToClean("DEFAULT").toList();

        // then
        List<EventRepository.EventToClean> debugEvents = events
            .stream()
            .filter(event -> event.group().type().equals("DEBUG_API"))
            .collect(Collectors.toList());

        assertThat(debugEvents).hasSize(2);
        assertThat(debugEvents)
            .allSatisfy(event -> {
                assertThat(event.group().type()).isEqualTo("DEBUG_API");
            })
            .anySatisfy(event -> {
                assertThat(event.group().referenceId()).isEqualTo("api-2");
            })
            .anySatisfy(event -> {
                assertThat(event.group().referenceId()).isEqualTo("api-3");
            });
    }

    @Test
    public void should_handle_policy_group_events() {
        // when
        var events = eventRepository.findEventsToClean("DEFAULT").toList();

        // then
        List<EventRepository.EventToClean> policyEvents = events
            .stream()
            .filter(event -> event.group().type().equals("DEPLOY_SHARED_POLICY_GROUP"))
            .collect(Collectors.toList());

        assertThat(policyEvents).hasSize(1);
        assertThat(policyEvents)
            .allSatisfy(event -> {
                assertThat(event.group().type()).isEqualTo("DEPLOY_SHARED_POLICY_GROUP");
                assertThat(event.group().referenceId()).isEqualTo("policy-group-1");
            });
    }

    @Test
    public void should_handle_mixed_event_types_correctly() {
        // when
        var events = eventRepository.findEventsToClean("DEFAULT").toList();

        // then
        assertThat(events)
            .extracting(event -> event.group().type())
            .containsExactlyInAnyOrder(
                "PUBLISH_API",
                "UNPUBLISH_API",
                "PUBLISH_API",
                "STOP_API",
                "START_API",
                "PUBLISH_DICTIONARY",
                "UNPUBLISH_DICTIONARY",
                "START_DICTIONARY",
                "STOP_DICTIONARY",
                "GATEWAY_STARTED",
                "GATEWAY_STOPPED",
                "PUBLISH_ORGANIZATION",
                "DEBUG_API",
                "DEBUG_API",
                "DEPLOY_SHARED_POLICY_GROUP"
            );
    }
}
