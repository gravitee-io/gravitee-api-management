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
import java.util.Optional;
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
        final JsonObject searchPayload = ctx.getBodyAsJson();
        final EventCriteria eventCriteria = readCriteria(searchPayload);

        bridgeWorkerExecutor.executeBlocking(
            (Handler<Promise<List<Event>>>) promise -> {
                Long pageSize = getPageSize(ctx, null);
                Long pageNumber = getPageNumber(ctx, null);
                Event.EventProperties group = getGroup(ctx);

                try {
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
        return Event.EventProperties.valueOf(ctx.request().getParam("group"));
    }

    private Long getPageSize(RoutingContext ctx, Long defaultValue) {
        final String sPageSize = ctx.request().getParam("size");
        try {
            return Long.parseLong(sPageSize);
        } catch (NumberFormatException nfe) {
            return defaultValue;
        }
    }

    private Long getPageNumber(RoutingContext ctx, Long defaultValue) {
        final String sPageNumber = ctx.request().getParam("page");

        try {
            return Long.parseLong(sPageNumber);
        } catch (NumberFormatException nfe) {
            return defaultValue;
        }
    }

    public void createOrPatch(RoutingContext ctx) {
        bridgeWorkerExecutor.executeBlocking(
            promise -> {
                try {
                    Event event = ctx.getBodyAsJson().mapTo(Event.class);
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

    private EventCriteria readCriteria(JsonObject payload) {
        EventCriteria.Builder builder = new EventCriteria.Builder();

        Long fromVal = payload.getLong("from");
        if (fromVal != null && fromVal > 0) {
            builder.from(fromVal);
        }

        Long toVal = payload.getLong("to");
        if (toVal != null && toVal > 0) {
            builder.to(toVal);
        }

        JsonArray typesArr = payload.getJsonArray("types");
        if (typesArr != null) {
            Set<EventType> types = typesArr.stream().map(obj -> EventType.valueOf((String) obj)).collect(Collectors.toSet());

            builder.types(types.toArray(new EventType[types.size()]));
        }

        JsonObject propertiesObj = payload.getJsonObject("properties");
        if (propertiesObj != null) {
            propertiesObj.getMap().forEach(builder::property);
        }

        JsonArray environmentsArr = payload.getJsonArray("environments");
        if (environmentsArr != null) {
            final List<String> environments = environmentsArr.stream().map(obj -> (String) obj).collect(Collectors.toList());

            builder.environments(environments);
        }

        Boolean strictMode = payload.getBoolean("strictMode");
        if (strictMode != null) {
            builder.strictMode(strictMode);
        }

        return builder.build();
    }
}
