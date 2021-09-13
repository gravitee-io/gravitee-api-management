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
package io.gravitee.gateway.debug.reactor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.event.Event;
import io.gravitee.definition.model.HttpRequest;
import io.gravitee.definition.model.HttpResponse;
import io.gravitee.gateway.debug.handler.definition.DebugApi;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;
import io.gravitee.gateway.reactor.impl.DefaultReactor;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.ApiDebugStatus;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

public class DebugReactor extends DefaultReactor {

    private static final String HOST = "localhost";

    private final Logger logger = LoggerFactory.getLogger(DebugReactor.class);

    @Autowired
    private EventRepository eventRepository;

    @Value("${debug.http.port:8482}")
    private int port;

    @Value("${debug.http.secured:false}")
    private boolean secured;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Vertx vertx;

    @Autowired
    @Qualifier("debugReactorHandlerRegistry")
    private ReactorHandlerRegistry reactorHandlerRegistry;

    @Override
    public void onEvent(Event<ReactorEvent, Reactable> reactorEvent) {
        if (reactorEvent.type() == ReactorEvent.DEBUG) {
            logger.info("Deploy api for debug...");

            if (reactorHandlerRegistry.contains(reactorEvent.content())) {
                logger.debug("Reactable already deployed. No need to do it again.");
                return;
            }

            logger.debug("Creating ReactorHandler");
            reactorHandlerRegistry.create(reactorEvent.content());

            DebugApi debugApi = (DebugApi) reactorEvent.content();
            Optional<io.gravitee.repository.management.model.Event> eventOptional;
            try {
                eventOptional = eventRepository.findById(debugApi.getEventId());
            } catch (TechnicalException e) {
                logger.error("An error occurred when searching event {}, removing the handler.", debugApi.getEventId(), e);
                reactorHandlerRegistry.remove(reactorEvent.content());
                return;
            }

            if (eventOptional.isEmpty()) {
                logger.error("No event found for id {}, removing the handler.", debugApi.getEventId());
                reactorHandlerRegistry.remove(reactorEvent.content());
                return;
            }

            io.gravitee.repository.management.model.Event debugEvent = eventOptional.get();
            try {
                HttpRequest req = debugApi.getRequest();
                HttpResponse response = new HttpResponse();

                updateEvent(debugEvent, ApiDebugStatus.DEBUGGING);

                logger.info("Sending request to debug...");
                HttpClient httpClient = vertx.createHttpClient();
                Future<HttpClientRequest> requestFuture = prepareRequest(debugApi, req, httpClient);

                requestFuture
                    .flatMap(reqEvent -> req.getBody() == null ? reqEvent.send() : reqEvent.send(req.getBody()))
                    .flatMap(
                        result -> {
                            Map<String, List<String>> headers = convertHeaders(result.headers());

                            response.setHeaders(headers);
                            response.statusCode(result.statusCode());

                            logger.debug("Response status: {}", result.statusCode());

                            return result.body();
                        }
                    )
                    .onSuccess(
                        bodyEvent -> {
                            try {
                                response.setBody(bodyEvent.toString());
                                logger.debug("Response body: {}", bodyEvent);
                                debugApi.setResponse(response);
                                debugEvent.setPayload(objectMapper.writeValueAsString(convert(debugApi)));
                                updateEvent(debugEvent, ApiDebugStatus.SUCCESS);
                            } catch (TechnicalException | JsonProcessingException e) {
                                logger.error("Error when saving debug response...");
                                failEvent(debugEvent);
                            } finally {
                                logger.info("Debugging successful, removing the handler.");
                                reactorHandlerRegistry.remove(reactorEvent.content());
                                logger.info("The debug handler has been removed");
                            }
                        }
                    )
                    .onFailure(
                        throwable -> {
                            logger.error("Debugging API has failed, removing the handler.", throwable);
                            reactorHandlerRegistry.remove(reactorEvent.content());
                            failEvent(debugEvent);
                        }
                    );
            } catch (TechnicalException e) {
                logger.error("An error occurred when debugging api for event {}, removing the handler.", debugApi.getEventId(), e);
                reactorHandlerRegistry.remove(reactorEvent.content());
                failEvent(debugEvent);
            }
        }
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        eventManager.subscribeForEvents(this, ReactorEvent.class);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        reactorHandlerRegistry.clear();
    }

    private Future<HttpClientRequest> prepareRequest(
        DebugApi debugApi,
        io.gravitee.definition.model.HttpRequest req,
        io.vertx.core.http.HttpClient httpClient
    ) {
        Future<HttpClientRequest> future = httpClient
            .request(
                new RequestOptions()
                    .setHost(HOST)
                    .setMethod(HttpMethod.valueOf(req.getMethod()))
                    .setPort(port)
                    .setHeaders(buildHeaders(debugApi, req))
                    // FIXME: Need to manage entrypoints in future release: https://github.com/gravitee-io/issues/issues/6143
                    .setURI(debugApi.getProxy().getVirtualHosts().get(0).getPath() + req.getPath())
                    .setTimeout(5000)
            )
            .map(
                httpClientRequest -> {
                    // Always set chunked mode for gRPC transport
                    return httpClientRequest.setChunked(true);
                }
            );
        return future;
    }

    private MultiMap buildHeaders(DebugApi debugApi, HttpRequest req) {
        final HeadersMultiMap headers = new HeadersMultiMap();
        // If API is configured in virtual hosts mode, we force the Host header
        if (debugApi.getProxy().getVirtualHosts().size() > 1) {
            String host = debugApi.getProxy().getVirtualHosts().get(0).getHost();
            if (host != null) {
                // FIXME: Need to manage entrypoints in future release: https://github.com/gravitee-io/issues/issues/6143
                headers.add(io.gravitee.common.http.HttpHeaders.HOST, host);
            }
        }
        return headers.addAll(convertHeaders(req.getHeaders()));
    }

    private void failEvent(io.gravitee.repository.management.model.Event debugEvent) {
        try {
            if (debugEvent != null) {
                updateEvent(debugEvent, ApiDebugStatus.ERROR);
            }
        } catch (TechnicalException e) {
            logger.error("Error when updating event {} with ERROR status", debugEvent.getId());
        }
    }

    private void updateEvent(io.gravitee.repository.management.model.Event debugEvent, ApiDebugStatus apiDebugStatus)
        throws TechnicalException {
        debugEvent
            .getProperties()
            .put(io.gravitee.repository.management.model.Event.EventProperties.API_DEBUG_STATUS.getValue(), apiDebugStatus.name());
        eventRepository.update(debugEvent);
    }

    private io.gravitee.definition.model.DebugApi convert(DebugApi content) {
        io.gravitee.definition.model.DebugApi debugAPI = new io.gravitee.definition.model.DebugApi();
        debugAPI.setName(content.getName());
        debugAPI.setId(content.getId());
        debugAPI.setDefinitionVersion(content.getDefinitionVersion());
        debugAPI.setResponse(content.getResponse());
        debugAPI.setRequest(content.getRequest());
        debugAPI.setFlowMode(content.getFlowMode());
        debugAPI.setFlows(content.getFlows());
        debugAPI.setPathMappings(content.getPathMappings());
        debugAPI.setPlans(content.getPlans());
        debugAPI.setPaths(content.getPaths());
        debugAPI.setServices(content.getServices());
        debugAPI.setProxy(content.getProxy());
        debugAPI.setProperties(content.getProperties());
        debugAPI.setResources(content.getResources());
        debugAPI.setServices(content.getServices());
        debugAPI.setResponseTemplates(content.getResponseTemplates());
        return debugAPI;
    }

    Map<String, List<String>> convertHeaders(MultiMap headersMultimap) {
        Map<String, List<String>> headers = new HashMap<>();
        if (headersMultimap != null) {
            headersMultimap.forEach(e -> headers.put(e.getKey(), headersMultimap.getAll(e.getKey())));
        }
        return headers;
    }

    MultiMap convertHeaders(Map<String, List<String>> headers) {
        final HeadersMultiMap headersMultiMap = new HeadersMultiMap();
        if (headers != null) {
            headers.forEach((key, value) -> headersMultiMap.add(key, String.join(", ", value)));
        }
        return headersMultiMap;
    }
}
