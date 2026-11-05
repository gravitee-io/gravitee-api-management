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
package io.gravitee.apim.core.generic_event.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.event.crud_service.EventLatestCrudService;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.core.event.query_service.EventLatestQueryService;
import io.gravitee.common.utils.TimeProvider;
import io.gravitee.rest.api.model.EventType;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Set;
import lombok.RequiredArgsConstructor;

/**
 * Deletes the generic events that expired, then the unpublished ones the gateways no longer need.
 * <p>
 * An expired event is deleted without an unpublish: the gateway subscribers already stop applying it at its expiry.
 * A gateway that misses a purged unpublish keeps the event until it restarts, so a delete is kept for a long retention.
 */
@UseCase
@RequiredArgsConstructor
public class PurgeGenericEventsUseCase {

    private final EventLatestQueryService eventLatestQueryService;
    private final EventLatestCrudService eventLatestCrudService;

    public record Input(Duration deletedRetention) {}

    public record Output(int expired, int purged) {}

    public Output execute(Input input) {
        var now = TimeProvider.instantNow();
        return new Output(
            delete(eventLatestQueryService.findAll(Set.of(EventType.PUBLISH_GENERIC_EVENT), now, null).iterator()),
            delete(
                eventLatestQueryService
                    .findAll(Set.of(EventType.UNPUBLISH_GENERIC_EVENT), null, now.minus(input.deletedRetention()))
                    .iterator()
            )
        );
    }

    private int delete(Iterator<Event> events) {
        var deleted = 0;
        while (events.hasNext()) {
            eventLatestCrudService.delete(events.next().getId());
            deleted++;
        }
        return deleted;
    }
}
