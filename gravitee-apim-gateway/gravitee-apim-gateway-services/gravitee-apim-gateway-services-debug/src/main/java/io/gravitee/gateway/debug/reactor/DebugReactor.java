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
import io.gravitee.gateway.debug.definition.DebugApi;
import io.gravitee.gateway.debug.vertx.VertxDebugHttpClientConfiguration;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;
import io.gravitee.gateway.reactor.impl.DefaultReactor;
import io.gravitee.gateway.reactor.impl.ReactableWrapper;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.ApiDebugStatus;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.net.*;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class DebugReactor extends DefaultReactor {

    private final Logger logger = LoggerFactory.getLogger(DebugReactor.class);

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Vertx vertx;

    @Autowired
    private VertxDebugHttpClientConfiguration debugHttpClientConfiguration;

    @Autowired
    @Qualifier("debugReactorHandlerRegistry")
    private ReactorHandlerRegistry reactorHandlerRegistry;

    @Override
    public void onEvent(Event<ReactorEvent, Reactable> reactorEvent) {
        if (reactorEvent.type() == ReactorEvent.DEBUG) {
            logger.debug("Try to deploy api for debug...");
            ReactableWrapper<io.gravitee.repository.management.model.Event> reactableWrapper = (ReactableWrapper<io.gravitee.repository.management.model.Event>) reactorEvent.content();
            io.gravitee.repository.management.model.Event debugEvent = reactableWrapper.getContent();
            DebugApi reactableDebugApi = toReactableDebugApi(reactableWrapper.getContent());
            if (reactableDebugApi != null) {
                if (reactorHandlerRegistry.contains(reactableDebugApi)) {
                    logger.debug("Reactable already deployed. No need to do it again.");
                    return;
                }
                logger.info("Deploy api for debug...");

                logger.debug("Creating ReactorHandler");
                reactorHandlerRegistry.create(reactableDebugApi);

                try {
                    HttpRequest req = reactableDebugApi.getRequest();
                    HttpResponse response = new HttpResponse();

                    updateEvent(debugEvent, ApiDebugStatus.DEBUGGING);

                    logger.info("Sending request to debug...");
                    HttpClient httpClient = vertx.createHttpClient(buildClientOptions());

                    Future<HttpClientRequest> requestFuture = prepareRequest(reactableDebugApi, req, httpClient);

                    requestFuture
                        .flatMap(reqEvent -> req.getBody() == null ? reqEvent.send() : reqEvent.send(req.getBody()))
                        .flatMap(
                            result -> {
                                logger.debug("Response status: {}", result.statusCode());
                                return result.body();
                            }
                        )
                        .onSuccess(
                            bodyEvent -> {
                                logger.debug("Response body: {}", bodyEvent);
                                logger.info("Debugging successful, removing the handler.");
                                reactorHandlerRegistry.remove(reactableDebugApi);
                                logger.info("The debug handler has been removed");
                            }
                        )
                        .onFailure(
                            throwable -> {
                                logger.error("Debugging API has failed, removing the handler.", throwable);
                                reactorHandlerRegistry.remove(reactableDebugApi);
                                failEvent(debugEvent);
                            }
                        );
                } catch (TechnicalException e) {
                    logger.error(
                        "An error occurred when debugging api for event {}, removing the handler.",
                        reactableDebugApi.getEventId(),
                        e
                    );
                    reactorHandlerRegistry.remove(reactableDebugApi);
                    failEvent(debugEvent);
                }
            }
        }
    }

    private DebugApi toReactableDebugApi(io.gravitee.repository.management.model.Event event) {
        try {
            // Read API definition from event
            io.gravitee.definition.model.debug.DebugApi eventPayload = objectMapper.readValue(
                event.getPayload(),
                io.gravitee.definition.model.debug.DebugApi.class
            );

            DebugApi debugApi = new DebugApi(event.getId(), eventPayload);
            debugApi.setEnabled(true);
            debugApi.setDeployedAt(new Date());

            return debugApi;
        } catch (Exception e) {
            // Log the error and ignore this event.
            logger.error("Unable to extract api definition from event [{}].", event.getId());
            failEvent(event);
            return null;
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

    private HttpClientOptions buildClientOptions() {
        HttpClientOptions options = new HttpClientOptions();
        options.setDefaultHost(debugHttpClientConfiguration.getHost());
        options.setDefaultPort(debugHttpClientConfiguration.getPort());
        options.setConnectTimeout(debugHttpClientConfiguration.getConnectTimeout());
        options.setTryUseCompression(debugHttpClientConfiguration.isCompressionSupported());
        options.setUseAlpn(debugHttpClientConfiguration.isAlpn());
        if (debugHttpClientConfiguration.isSecured()) {
            options.setSsl(debugHttpClientConfiguration.isSecured());
            options.setTrustAll(true);
            if (debugHttpClientConfiguration.isOpenssl()) {
                options.setSslEngineOptions(new OpenSSLEngineOptions());
            }
        }
        return options;
    }

    private Future<HttpClientRequest> prepareRequest(
        DebugApi debugApi,
        io.gravitee.definition.model.HttpRequest req,
        io.vertx.core.http.HttpClient httpClient
    ) {
        Future<HttpClientRequest> future = httpClient
            .request(
                new RequestOptions()
                    .setMethod(HttpMethod.valueOf(req.getMethod()))
                    .setHeaders(buildHeaders(debugApi, req))
                    // TODO: Need to manage entrypoints in future release: https://github.com/gravitee-io/issues/issues/6143
                    .setURI(debugApi.getProxy().getVirtualHosts().get(0).getPath() + req.getPath())
                    .setTimeout(debugHttpClientConfiguration.getRequestTimeout())
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
                // TODO: Need to manage entrypoints in future release: https://github.com/gravitee-io/issues/issues/6143
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

    MultiMap convertHeaders(Map<String, List<String>> headers) {
        final HeadersMultiMap headersMultiMap = new HeadersMultiMap();
        if (headers != null) {
            headers.forEach((key, value) -> headersMultiMap.add(key, String.join(", ", value)));
        }
        return headersMultiMap;
    }
}
