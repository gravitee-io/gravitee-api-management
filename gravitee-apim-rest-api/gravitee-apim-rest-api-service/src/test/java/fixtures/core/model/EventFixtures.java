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
package fixtures.core.model;

import io.gravitee.apim.core.event.model.Event;
import java.time.Instant;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class EventFixtures {

    private EventFixtures() {}

    private static final Supplier<Event.EventBuilder> BASE = () ->
        Event.builder()
            .id("event-id")
            .payload("event-payload")
            .environments(Set.of("environment-id"))
            .type(io.gravitee.rest.api.model.EventType.PUBLISH_API)
            .properties(
                new EnumMap<>(
                    Map.ofEntries(Map.entry(Event.EventProperties.API_ID, "api-id"), Map.entry(Event.EventProperties.USER, "user-id"))
                )
            )
            .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()))
            .createdAt(Instant.parse("2020-02-01T20:22:02.00Z").atZone(ZoneId.systemDefault()));

    public static Event anEvent() {
        return BASE.get().build();
    }

    public static Event anApiEvent(String apiId) {
        var event = BASE.get().build();
        event.getProperties().put(Event.EventProperties.API_ID, apiId);
        return event;
    }
}
