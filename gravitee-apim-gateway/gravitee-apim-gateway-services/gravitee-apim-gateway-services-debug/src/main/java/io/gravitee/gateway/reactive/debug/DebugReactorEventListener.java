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
package io.gravitee.gateway.reactive.debug;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.gravitee.common.event.Event;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.util.DataEncryptor;
import io.gravitee.definition.model.HttpRequest;
import io.gravitee.definition.model.Properties;
import io.gravitee.definition.model.Property;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.debug.definition.DebugApi;
import io.gravitee.gateway.debug.vertx.VertxDebugHttpClientConfiguration;
import io.gravitee.gateway.handlers.accesspoint.manager.AccessPointManager;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.gateway.reactor.accesspoint.ReactableAccessPoint;
import io.gravitee.gateway.reactor.handler.ReactorEventListener;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;
import io.gravitee.gateway.reactor.impl.ReactableEvent;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.ApiDebugStatus;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.core.net.OpenSSLEngineOptions;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpClient;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class DebugReactorEventListener extends ReactorEventListener {

    private final Logger logger = LoggerFactory.getLogger(DebugReactorEventListener.class);
    private final Vertx vertx;
    private final EventRepository eventRepository;
    private final ObjectMapper objectMapper;
    private final VertxDebugHttpClientConfiguration debugHttpClientConfiguration;
    private final AccessPointManager accessPointManager;
    private final DataEncryptor dataEncryptor;

    public DebugReactorEventListener(
        final Vertx vertx,
        final EventManager eventManager,
        final EventRepository eventRepository,
        final ObjectMapper objectMapper,
        final VertxDebugHttpClientConfiguration debugHttpClientConfiguration,
        final ReactorHandlerRegistry reactorHandlerRegistry,
        final AccessPointManager accessPointManager,
        DataEncryptor dataEncryptor
    ) {
        super(eventManager, reactorHandlerRegistry);
        this.vertx = vertx;
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
        this.debugHttpClientConfiguration = debugHttpClientConfiguration;
        this.accessPointManager = accessPointManager;
        this.dataEncryptor = dataEncryptor;
    }

    @Override
    public void onEvent(final Event<ReactorEvent, Reactable> reactorEvent) {
        if (reactorEvent.type() == ReactorEvent.DEBUG) {
            logger.info("Deploying api for debug");
            ReactableEvent<io.gravitee.repository.management.model.Event> reactableEvent =
                (ReactableEvent<io.gravitee.repository.management.model.Event>) reactorEvent.content();
            io.gravitee.repository.management.model.Event debugEvent = reactableEvent.getContent();
            DebugApi debugApi = toDebugApi(reactableEvent);
            if (debugApi != null) {
                if (reactorHandlerRegistry.contains(debugApi)) {
                    logger.info("Api for debug already deployed. No need to do it again.");
                    return;
                }

                try {
                    reactorHandlerRegistry.create(debugApi);

                    HttpRequest debugApiRequest = debugApi.getRequest();
                    updateEvent(debugEvent, ApiDebugStatus.DEBUGGING);

                    logger.info("Sending request to debug");
                    HttpClient httpClient = vertx.createHttpClient(buildClientOptions());

                    httpClient
                        .rxRequest(
                            new RequestOptions()
                                .setMethod(HttpMethod.valueOf(debugApiRequest.getMethod()))
                                .setHeaders(buildHeaders(debugApi, debugApiRequest))
                                // TODO: Need to manage entrypoints in future release: https://github.com/gravitee-io/issues/issues/6143
                                .setURI(debugApi.getDefinition().getProxy().getVirtualHosts().get(0).getPath() + debugApiRequest.getPath())
                                .setTimeout(debugHttpClientConfiguration.getRequestTimeout())
                        )
                        .map(httpClientRequest ->
                            // Always set chunked mode for gRPC transport
                            httpClientRequest.setChunked(true)
                        )
                        .flatMap(httpClientRequest ->
                            debugApiRequest.getBody() == null
                                ? httpClientRequest.rxSend()
                                : httpClientRequest.rxSend(debugApiRequest.getBody())
                        )
                        .doOnSuccess(httpClientResponse -> logger.debug("Response status: {}", httpClientResponse.statusCode()))
                        .flatMap(io.vertx.rxjava3.core.http.HttpClientResponse::rxBody)
                        .doFinally(httpClient::close)
                        .subscribeOn(Schedulers.io())
                        .subscribe(
                            body -> {
                                logger.info("Debugging successful, removing the handler.");
                                reactorHandlerRegistry.remove(debugApi);
                            },
                            throwable -> {
                                logger.error("Debugging API has failed, removing the handler.", throwable);
                                reactorHandlerRegistry.remove(debugApi);
                                failEvent(debugEvent);
                            }
                        );
                } catch (TechnicalException e) {
                    logger.error("An error occurred when debugging api for event {}, removing the handler.", debugApi.getEventId(), e);
                    reactorHandlerRegistry.remove(debugApi);
                    failEvent(debugEvent);
                }
            }
        }
    }

    private DebugApi toDebugApi(final ReactableEvent<io.gravitee.repository.management.model.Event> reactableEvent) {
        io.gravitee.repository.management.model.Event event = reactableEvent.getContent();
        try {
            // Read API definition from event
            io.gravitee.definition.model.debug.DebugApi eventDebugApi = objectMapper.readValue(
                event.getPayload(),
                io.gravitee.definition.model.debug.DebugApi.class
            );

            if (null != eventDebugApi.getProperties()) {
                decryptProperties(eventDebugApi.getProperties());
            }

            eventDebugApi.setPlans(
                eventDebugApi.getPlans().stream().filter(plan -> !PlanStatus.CLOSED.name().equalsIgnoreCase(plan.getStatus())).toList()
            );

            DebugApi debugApi = new DebugApi(reactableEvent.getId(), eventDebugApi);
            debugApi.setDeployedAt(reactableEvent.getDeployedAt());
            debugApi.setEnvironmentHrid(reactableEvent.getEnvironmentHrid());
            debugApi.setEnvironmentId(reactableEvent.getEnvironmentId());
            debugApi.setOrganizationHrid(reactableEvent.getOrganizationHrid());
            debugApi.setOrganizationId(reactableEvent.getOrganizationId());

            return debugApi;
        } catch (Exception e) {
            // Log the error and ignore this event.
            logger.error("Unable to extract api definition from event [{}].", reactableEvent.getId(), e);
            failEvent(event);
            return null;
        }
    }

    private HttpClientOptions buildClientOptions() {
        HttpClientOptions options = new HttpClientOptions();
        options.setDefaultHost(debugHttpClientConfiguration.getHost());
        options.setDefaultPort(debugHttpClientConfiguration.getPort());
        options.setConnectTimeout(debugHttpClientConfiguration.getConnectTimeout());
        options.setTryUseCompression(debugHttpClientConfiguration.isCompressionSupported());
        options.setUseAlpn(debugHttpClientConfiguration.isAlpn());
        options.setVerifyHost(false);
        if (debugHttpClientConfiguration.isSecured()) {
            options.setSsl(debugHttpClientConfiguration.isSecured());
            options.setTrustAll(true);
            options.setVerifyHost(false);
            if (debugHttpClientConfiguration.isOpenssl()) {
                options.setSslEngineOptions(new OpenSSLEngineOptions());
            }
        }
        return options;
    }

    MultiMap buildHeaders(DebugApi debugApi, HttpRequest req) {
        final HeadersMultiMap headers = new HeadersMultiMap();
        headers.addAll(convertHeaders(req.getHeaders()));

        String host = null;
        // If API is configured in virtual hosts mode, we force the Host header
        if (!debugApi.getDefinition().getProxy().getVirtualHosts().isEmpty()) {
            host = debugApi.getDefinition().getProxy().getVirtualHosts().get(0).getHost();
        }
        // If gateway is multi tenant, we need to apply access point host
        if (host == null) {
            List<ReactableAccessPoint> accessPoints = accessPointManager.getByEnvironmentId(debugApi.getEnvironmentId());
            if (accessPoints != null && !accessPoints.isEmpty()) {
                host = accessPoints.get(0).getHost();
            }
        }
        if (host != null) {
            // TODO: Need to manage entrypoints in future release: https://github.com/gravitee-io/issues/issues/6143
            headers.add(HttpHeaderNames.HOST, host);
        }
        return headers;
    }

    MultiMap convertHeaders(Map<String, List<String>> headers) {
        final HeadersMultiMap headersMultiMap = new HeadersMultiMap();
        if (headers != null) {
            headers.forEach(headersMultiMap::set);
        }
        return headersMultiMap;
    }

    private void failEvent(io.gravitee.repository.management.model.Event debugEvent) {
        try {
            if (debugEvent != null) {
                updateEvent(debugEvent, ApiDebugStatus.ERROR);
            }
        } catch (TechnicalException e) {
            logger.error("Error when updating event {}", debugEvent.getId(), e);
        }
    }

    private void updateEvent(io.gravitee.repository.management.model.Event debugEvent, ApiDebugStatus apiDebugStatus)
        throws TechnicalException {
        debugEvent
            .getProperties()
            .put(io.gravitee.repository.management.model.Event.EventProperties.API_DEBUG_STATUS.getValue(), apiDebugStatus.name());
        eventRepository.update(debugEvent);
    }

    private void decryptProperties(final Properties properties) {
        for (Property property : properties.getProperties()) {
            if (property.isEncrypted()) {
                try {
                    property.setValue(dataEncryptor.decrypt(property.getValue()));
                    property.setEncrypted(false);
                    properties.getValues().put(property.getKey(), property.getValue());
                } catch (GeneralSecurityException e) {
                    logger.error("Error decrypting API property value for key {}", property.getKey(), e);
                }
            }
        }
    }
}
