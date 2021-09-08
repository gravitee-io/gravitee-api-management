/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.gateway.debug.sync;

import static io.gravitee.gateway.services.sync.spring.SyncConfiguration.PARALLELISM;
import static io.gravitee.repository.management.model.Event.EventProperties.API_DEBUG_ID;

import io.gravitee.common.event.EventManager;
import io.gravitee.definition.model.DebugApi;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.gateway.services.sync.synchronizer.AbstractSynchronizer;
import io.gravitee.gateway.services.sync.synchronizer.PlanFetcher;
import io.gravitee.node.api.Node;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.model.ApiDebugStatus;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.annotations.NonNull;
import io.reactivex.schedulers.Schedulers;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DebugApiSynchronizer extends AbstractSynchronizer {

    public static final int DEFAULT_BULK_SIZE = 10;

    private final Logger logger = LoggerFactory.getLogger(DebugApiSynchronizer.class);

    @Autowired
    private EventManager eventManager;

    @Autowired
    private Node node;

    @Autowired
    private PlanFetcher planFetcher;

    @Value("${services.debug.bulk_items:10}")
    protected int bulkItems;

    public void synchronize(long lastRefreshAt, long nextLastRefreshAt, List<String> environments) {
        final long start = System.currentTimeMillis();
        if (lastRefreshAt != -1) {
            Long count =
                this.searchLatestEvents(
                        bulkItems > 0 ? bulkItems : DEFAULT_BULK_SIZE,
                        lastRefreshAt,
                        nextLastRefreshAt,
                        API_DEBUG_ID,
                        environments,
                        EventType.DEBUG_API
                    )
                    .compose(this::processApiEvents)
                    .count()
                    .blockingGet();
            logger.debug("{} apis synchronized in {}ms", count, (System.currentTimeMillis() - start));
        }
    }

    @Override
    protected EventCriteria appendCriteria(EventCriteria.Builder builder) {
        return builder
            .property(Event.EventProperties.API_DEBUG_STATUS.getValue(), ApiDebugStatus.TO_DEBUG.name())
            .property(Event.EventProperties.GATEWAY_ID.getValue(), node.id())
            .build();
    }

    @NonNull
    private Flowable<String> processApiEvents(Flowable<Event> upstream) {
        return upstream.groupBy(Event::getType).flatMap(eventsByType -> eventsByType.compose(this::processApiDebugEvents));
    }

    private Maybe<io.gravitee.gateway.debug.handler.definition.DebugApi> toApiDefinition(Event apiEvent) {
        try {
            // Read API definition from event
            DebugApi eventPayload = objectMapper.readValue(apiEvent.getPayload(), DebugApi.class);

            io.gravitee.gateway.debug.handler.definition.DebugApi debugApi = new io.gravitee.gateway.debug.handler.definition.DebugApi(
                apiEvent.getId(),
                eventPayload
            );
            debugApi.setEnabled(true);
            debugApi.setDeployedAt(new Date());

            return Maybe.just(debugApi);
        } catch (Exception e) {
            // Log the error and ignore this event.
            logger.error("Unable to extract api definition from event [{}].", apiEvent.getId());
            return Maybe.empty();
        }
    }

    /**
     * Process events related to api debug in an optimized way.
     * This process is divided into following steps:
     *  - Map each event payload to api definition
     *  - fetch api plans (bulk mode, for definition v1 only).
     *  - invoke ApiManager to register each api
     *
     * @param upstream the flow of events to process.
     * @return the flow of api ids registered.
     */
    @NonNull
    private Flowable<String> processApiDebugEvents(Flowable<Event> upstream) {
        return upstream.flatMapMaybe(this::toApiDefinition).compose(planFetcher::fetchApiPlans).compose(this::registerDebugApi);
    }

    @NonNull
    private Flowable<String> registerDebugApi(Flowable<io.gravitee.gateway.handlers.api.definition.Api> upstream) {
        return upstream
            .parallel(PARALLELISM)
            .runOn(Schedulers.from(executor))
            .doOnNext(
                api -> {
                    if (!api.getPlans().isEmpty()) {
                        eventManager.publishEvent(ReactorEvent.DEBUG, api);
                    } else {
                        logger.info("No plan for API, skipping debug");
                    }
                }
            )
            .sequential()
            .map(io.gravitee.definition.model.Api::getId);
    }
}
