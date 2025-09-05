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
package io.gravitee.apim.infra.crud_service.event;

import com.google.common.util.concurrent.AtomicLongMap;
import io.gravitee.apim.core.event.crud_service.EventCrudService;
import io.gravitee.apim.core.event.model.Event;
import io.gravitee.apim.infra.adapter.EventAdapter;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.rest.api.model.EventType;
import io.gravitee.rest.api.service.EventService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import io.reactivex.rxjava3.core.Flowable;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class EventCrudServiceLegacyWrapper implements EventCrudService {

    private static final Logger logger = LoggerFactory.getLogger(EventCrudServiceLegacyWrapper.class);

    private final EventService eventService;
    private final EventRepository eventRepository;

    public EventCrudServiceLegacyWrapper(EventService eventService, @Lazy EventRepository eventRepository) {
        this.eventService = eventService;
        this.eventRepository = eventRepository;
    }

    @Override
    public Event createEvent(
        String organizationId,
        String environmentId,
        Set<String> environmentIds,
        EventType eventType,
        Object content,
        Map<Event.EventProperties, String> properties
    ) {
        return EventAdapter.INSTANCE.fromEntity(
            eventService.createEvent(
                new ExecutionContext(organizationId, environmentId),
                environmentIds,
                organizationId,
                eventType,
                content,
                EventAdapter.INSTANCE.toStringEventPropertiesMap(properties)
            )
        );
    }

    @Override
    public Event get(String organizationId, String environmentId, String eventId) {
        return EventAdapter.INSTANCE.fromEntity(eventService.findById(new ExecutionContext(organizationId, environmentId), eventId));
    }

    @Override
    public void cleanupEvents(String environmentId, int nbEventsToKeep, Duration timeToLive) {
        var counters = AtomicLongMap.<EventRepository.EventToCleanGroup>create();

        logger.info(
            "Starting cleanup for environment: {} (keep {} events per type, max duration: {}s)",
            environmentId,
            nbEventsToKeep,
            timeToLive.toSeconds()
        );

        long startTime = System.currentTimeMillis();
        AtomicLong processedCount = new AtomicLong(0);
        AtomicLong deletedCount = new AtomicLong(0);
        AtomicLong skippedCount = new AtomicLong(0);

        try {
            Flowable
                .fromStream(eventRepository.findEventsToClean(environmentId))
                .filter(eventToClean -> {
                    long currentProcessed = processedCount.incrementAndGet();

                    if (currentProcessed % 1000 == 0) {
                        logger.info(
                            "Processed {} events, deleted {} events, skipped {} events",
                            currentProcessed,
                            deletedCount.get(),
                            skippedCount.get()
                        );
                    }

                    // Use the structured group directly for safe counting
                    EventRepository.EventToCleanGroup group = eventToClean.group();
                    if (group == null) {
                        skippedCount.incrementAndGet();
                        return false;
                    }

                    // Check if we've already seen enough events for this group
                    long currentGroupCount = counters.get(group);

                    if (currentGroupCount >= nbEventsToKeep) {
                        // We've already seen enough events for this group, this one should be deleted
                        deletedCount.incrementAndGet();
                        return true;
                    } else {
                        // We haven't seen enough events yet, keep this one
                        counters.incrementAndGet(group);
                        skippedCount.incrementAndGet();
                        return false;
                    }
                })
                .map(EventRepository.EventToClean::id)
                .buffer(20)
                .takeUntil(Flowable.timer(timeToLive.toSeconds(), TimeUnit.SECONDS))
                .blockingForEach(eventRepository::delete);

            long duration = System.currentTimeMillis() - startTime;
            logger.info(
                "Cleanup completed for environment: {}. Processed: {} events, Deleted: {} events, Skipped: {} events, Duration: {}ms",
                environmentId,
                processedCount.get(),
                deletedCount.get(),
                skippedCount.get(),
                duration
            );
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error(
                "Cleanup failed for environment: {} after {}ms. Processed: {} events, Deleted: {} events, Skipped: {} events",
                environmentId,
                duration,
                processedCount.get(),
                deletedCount.get(),
                skippedCount.get(),
                e
            );
            throw e;
        }
    }
}
