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
package io.gravitee.gateway.services.sync.synchronizer;

import static io.gravitee.repository.management.model.Event.EventProperties.API_DEBUG_ID;

import io.gravitee.common.event.EventManager;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.gateway.reactor.impl.ReactableWrapper;
import io.gravitee.node.api.Node;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.model.ApiDebugStatus;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.reactivex.Flowable;
import io.reactivex.annotations.NonNull;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DebugApiSynchronizer extends AbstractSynchronizer {

    private final Logger logger = LoggerFactory.getLogger(DebugApiSynchronizer.class);

    @Autowired
    private EventManager eventManager;

    @Autowired
    private Node node;

    public void synchronize(long lastRefreshAt, long nextLastRefreshAt, List<String> environments) {
        final long start = System.currentTimeMillis();
        if (lastRefreshAt != -1) {
            Long count =
                this.searchLatestEvents(lastRefreshAt, nextLastRefreshAt, API_DEBUG_ID, environments, EventType.DEBUG_API)
                    .compose(this::processApiEvents)
                    .count()
                    .blockingGet();
            logger.debug("{} debug apis synchronized in {}ms", count, (System.currentTimeMillis() - start));
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
        return upstream
            .doOnNext(
                debugEvent -> {
                    eventManager.publishEvent(ReactorEvent.DEBUG, new ReactableWrapper(debugEvent));
                }
            )
            .map(io.gravitee.repository.management.model.Event::getId);
    }
}
