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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.gravitee.common.data.domain.Page;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.http.MediaType;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.api.search.EventCriteria;
import io.gravitee.repository.management.api.search.builder.PageableBuilder;
import io.gravitee.repository.management.model.Event;
import io.gravitee.repository.management.model.EventType;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class EventsHandler {

    private final Logger LOGGER = LoggerFactory.getLogger(EventsHandler.class);

    @Autowired
    private EventRepository eventRepository;

    public void search(RoutingContext ctx) {
        HttpServerResponse response = ctx.response();

        try {
            JsonObject searchPayload = ctx.getBodyAsJson();
            EventCriteria eventCriteria = readCriteria(searchPayload);

            String pageNumber = ctx.request().getParam("page");
            if (pageNumber != null) {
                String pageSize = ctx.request().getParam("size");
                if (pageSize == null || pageSize.isEmpty()) {
                    pageSize = "10";
                }

                try {
                    int page = Integer.parseInt(pageNumber);
                    int size = Integer.parseInt(pageSize);

                    Page<Event> events = eventRepository.search(eventCriteria,
                            new PageableBuilder().pageNumber(page).pageSize(size).build());

                    response.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                    response.setStatusCode(HttpStatusCode.OK_200);
                    response.setChunked(true);

                    Json.prettyMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                    response.write(Json.prettyMapper.writeValueAsString(events));
                } catch (NumberFormatException nfe) {
                    response.setStatusCode(HttpStatusCode.BAD_REQUEST_400);
                }
            } else {
                List<Event> events = eventRepository.search(eventCriteria);

                response.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                response.setStatusCode(HttpStatusCode.OK_200);
                response.setChunked(true);

                Json.prettyMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                response.write(Json.prettyMapper.writeValueAsString(events));
            }
        } catch (JsonProcessingException jpe) {
            response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
            LOGGER.error("Unable to transform data object to JSON", jpe);
        }

        response.end();
    }

    public void create(RoutingContext ctx) {
        HttpServerResponse response = ctx.response();

        try {
            Event event = ctx.getBodyAsJson().mapTo(Event.class);
            Event createdEvent = eventRepository.create(event);

            response.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            response.setStatusCode(HttpStatusCode.OK_200);
            response.setChunked(true);

            Json.prettyMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            response.write(Json.prettyMapper.writeValueAsString(createdEvent));
        } catch (JsonProcessingException jpe) {
            response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
            LOGGER.error("Unable to transform data object to JSON", jpe);
        } catch (TechnicalException te) {
            response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
            LOGGER.error("Unable to update an event", te);
        }

        response.end();
    }

    public void update(RoutingContext ctx) {
        HttpServerResponse response = ctx.response();

        try {
            Event event = ctx.getBodyAsJson().mapTo(Event.class);
            Event updatedEvent = eventRepository.update(event);

            response.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            response.setStatusCode(HttpStatusCode.OK_200);
            response.setChunked(true);

            Json.prettyMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            response.write(Json.prettyMapper.writeValueAsString(updatedEvent));
        } catch (JsonProcessingException jpe) {
            response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
            LOGGER.error("Unable to transform data object to JSON", jpe);
        } catch (TechnicalException te) {
            response.setStatusCode(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
            LOGGER.error("Unable to update an event", te);
        }

        response.end();
    }

    private EventCriteria readCriteria(JsonObject payload) {
        EventCriteria.Builder builder = new EventCriteria.Builder();

        Long fromVal = payload.getLong("from");
        if (fromVal != null && fromVal > 0) {
            builder.from(fromVal);
        }

        Long toVal = payload.getLong("to");
        if (toVal != null && toVal > 0) {
            builder.from(toVal);
        }

        JsonArray typesArr = payload.getJsonArray("types");
        if (typesArr != null) {
            Set<EventType> types = typesArr.stream()
                    .map(obj -> EventType.valueOf((String) obj))
                    .collect(Collectors.toSet());

            builder.types(types.toArray(new EventType[types.size()]));
        }

        JsonObject propertiesObj = payload.getJsonObject("properties");
        if (propertiesObj != null) {
            propertiesObj.getMap().forEach(builder::property);
        }

        return builder.build();
    }
}
