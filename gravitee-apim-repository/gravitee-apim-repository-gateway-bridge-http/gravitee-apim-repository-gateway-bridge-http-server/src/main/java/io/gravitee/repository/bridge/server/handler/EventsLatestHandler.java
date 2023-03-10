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
package io.gravitee.repository.bridge.server.handler;

import static io.gravitee.repository.bridge.server.handler.EventsHandler.readCriteria;
import static io.gravitee.repository.bridge.server.utils.ParamUtils.getPageNumber;
import static io.gravitee.repository.bridge.server.utils.ParamUtils.getPageSize;

import io.gravitee.repository.management.api.EventLatestRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EventsLatestHandler extends AbstractHandler {

    @Autowired
    private EventLatestRepository eventLatestRepository;

    public EventsLatestHandler(WorkerExecutor bridgeWorkerExecutor) {
        super(bridgeWorkerExecutor);
    }

    public void search(RoutingContext ctx) {
        bridgeWorkerExecutor.executeBlocking(
            (Handler<Promise<List<Event>>>) promise -> {
                try {
                    EventCriteria eventCriteria = readCriteria(ctx.body().asJsonObject());

                    Long pageSize = getPageSize(ctx, null);
                    Long pageNumber = getPageNumber(ctx, null);
                    Event.EventProperties group = getGroup(ctx);

                    promise.complete(eventLatestRepository.search(eventCriteria, group, pageNumber, pageSize));
                } catch (Exception ex) {
                    LOGGER.error("Unable to search for events", ex);
                    promise.fail(ex);
                }
            },
            false,
            event -> handleResponse(ctx, event)
        );
    }

    private Event.EventProperties getGroup(RoutingContext ctx) {
        try {
            return Event.EventProperties.valueOf(ctx.request().getParam("group"));
        } catch (Exception e) {
            return null;
        }
    }

    public void createOrPatch(RoutingContext ctx) {
        bridgeWorkerExecutor.executeBlocking(
            promise -> {
                try {
                    Event event = ctx.body().asJsonObject().mapTo(Event.class);
                    promise.complete(eventLatestRepository.createOrPatch(event));
                } catch (Exception ex) {
                    LOGGER.error("Unable to create or update an event", ex);
                    promise.fail(ex);
                }
            },
            false,
            (Handler<AsyncResult<Event>>) event -> handleResponse(ctx, event)
        );
    }
}
