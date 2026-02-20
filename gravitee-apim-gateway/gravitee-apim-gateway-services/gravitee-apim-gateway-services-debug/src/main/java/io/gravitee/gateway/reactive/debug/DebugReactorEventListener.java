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
import io.gravitee.definition.model.debug.DebugApiV4;
import io.gravitee.definition.model.v4.plan.PlanStatus;
import io.gravitee.gateway.api.http.HttpHeaderNames;
import io.gravitee.gateway.debug.definition.ReactableDebugApi;
import io.gravitee.gateway.debug.vertx.VertxDebugHttpClientConfiguration;
import io.gravitee.gateway.handlers.accesspoint.manager.AccessPointManager;
import io.gravitee.gateway.reactor.Reactable;
import io.gravitee.gateway.reactor.ReactorEvent;
import io.gravitee.gateway.reactor.accesspoint.ReactableAccessPoint;
import io.gravitee.gateway.reactor.handler.ReactorEventListener;
import io.gravitee.gateway.reactor.handler.ReactorHandlerRegistry;
import io.gravitee.gateway.reactor.impl.ReactableEvent;
import io.gravitee.repository.management.api.EventRepository;
import io.gravitee.repository.management.model.ApiDebugStatus;
import io.gravitee.secrets.api.discovery.DefinitionMetadata;
import io.gravitee.secrets.api.event.SecretDiscoveryEvent;
import io.gravitee.secrets.api.event.SecretDiscoveryEventType;
import io.reactivex.rxjava3.core.Completable;
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
import java.util.List;
import java.util.Map;
import lombok.CustomLog;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class DebugReactorEventListener extends ReactorEventListener {

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
            log.debug("Deploying api for debug");
            ReactableEvent<io.gravitee.repository.management.model.Event> reactableEvent = (ReactableEvent<
                io.gravitee.repository.management.model.Event
            >) reactorEvent.content();
            io.gravitee.repository.management.model.Event debugEvent = reactableEvent.getContent();
            ReactableDebugApi<?> debugApi = toDebugApi(reactableEvent);
            if (debugApi != null) {
                if (reactorHandlerRegistry.contains(debugApi)) {
                    log.debug("Api [{}] for debug already deployed. No need to do it again.", debugApi.getId());
                    return;
                }

                reactorHandlerRegistry.create(debugApi);

                var secretDiscoveryEvent = new SecretDiscoveryEvent(
                    debugApi.getEnvironmentId(),
                    debugApi.getDefinition(),
                    new DefinitionMetadata(debugApi.getRevision())
                );
                eventManager.publishEvent(SecretDiscoveryEventType.DISCOVER, secretDiscoveryEvent);

                HttpRequest debugApiRequest = debugApi.getRequest();

                updateEvent(debugEvent, ApiDebugStatus.DEBUGGING)
                    .andThen(
                        Completable.defer(() -> {
                            log.debug("Sending request to debug to API [{}]", debugApi.getId());
                            HttpClient httpClient = vertx.createHttpClient(buildClientOptions());
                            return httpClient
                                .rxRequest(
                                    new RequestOptions()
                                        .setMethod(HttpMethod.valueOf(debugApiRequest.getMethod()))
                                        .setHeaders(buildHeaders(debugApi, debugApiRequest))
                                        .setURI(debugApi.extractUri())
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
                                .doOnSuccess(httpClientResponse ->
                                    log.debug("Response status: {} for API [{}]", httpClientResponse.statusCode(), debugApi.getId())
                                )
                                .flatMap(io.vertx.rxjava3.core.http.HttpClientResponse::rxBody)
                                .doFinally(httpClient::close)
                                .ignoreElement();
                        })
                    )
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                        () -> {
                            log.debug("Debugging successful for API [{}], removing the handler.", debugApi.getId());
                            eventManager.publishEvent(SecretDiscoveryEventType.REVOKE, secretDiscoveryEvent);
                            reactorHandlerRegistry.remove(debugApi);
                        },
                        throwable -> {
                            log.error("Debugging API has failed for API [{}], removing the handler.", debugApi.getId(), throwable);
                            eventManager.publishEvent(SecretDiscoveryEventType.REVOKE, secretDiscoveryEvent);
                            reactorHandlerRegistry.remove(debugApi);
                            failEvent(debugEvent);
                        }
                    );
            }
        }
    }

    private ReactableDebugApi<?> toDebugApi(final ReactableEvent<io.gravitee.repository.management.model.Event> reactableEvent) {
        io.gravitee.repository.management.model.Event event = reactableEvent.getContent();
        try {
            if (event.getProperties().getOrDefault("definition_version", "V2").equals("V4")) {
                return toDebugApiV4(reactableEvent);
            }
            // Read API definition from event
            io.gravitee.definition.model.debug.DebugApiV2 eventDebugApi = objectMapper.readValue(
                event.getPayload(),
                io.gravitee.definition.model.debug.DebugApiV2.class
            );

            if (null != eventDebugApi.getProperties()) {
                decryptProperties(eventDebugApi.getId(), eventDebugApi.getProperties());
            }

            eventDebugApi.setPlans(
                eventDebugApi
                    .getPlans()
                    .stream()
                    .filter(plan -> !PlanStatus.CLOSED.name().equalsIgnoreCase(plan.getStatus()))
                    .toList()
            );

            var debugApi = new io.gravitee.gateway.debug.definition.DebugApiV2(reactableEvent.getId(), eventDebugApi);
            debugApi.setDeployedAt(reactableEvent.getDeployedAt());
            debugApi.setEnvironmentHrid(reactableEvent.getEnvironmentHrid());
            debugApi.setEnvironmentId(reactableEvent.getEnvironmentId());
            debugApi.setOrganizationHrid(reactableEvent.getOrganizationHrid());
            debugApi.setOrganizationId(reactableEvent.getOrganizationId());

            return debugApi;
        } catch (Exception e) {
            // Log the error and ignore this event.
            log.error("Unable to extract api definition from event [{}].", reactableEvent.getId(), e);
            failEvent(event);
            return null;
        }
    }

    private ReactableDebugApi<?> toDebugApiV4(final ReactableEvent<io.gravitee.repository.management.model.Event> reactableEvent) {
        io.gravitee.repository.management.model.Event event = reactableEvent.getContent();
        try {
            // Read API definition from event
            DebugApiV4 eventDebugApi = objectMapper.readValue(event.getPayload(), DebugApiV4.class);

            if (null != eventDebugApi.getApiDefinition().getProperties()) {
                decryptProperties(eventDebugApi.getApiDefinition().getId(), eventDebugApi.getApiDefinition().getProperties());
            }

            eventDebugApi
                .getApiDefinition()
                .setPlans(
                    eventDebugApi
                        .getApiDefinition()
                        .getPlans()
                        .stream()
                        .filter(plan -> !PlanStatus.CLOSED.equals(plan.getStatus()))
                        .toList()
                );

            var debugApi = new io.gravitee.gateway.debug.definition.DebugApiV4(reactableEvent.getId(), eventDebugApi);
            debugApi.setDeployedAt(reactableEvent.getDeployedAt());
            debugApi.setEnvironmentHrid(reactableEvent.getEnvironmentHrid());
            debugApi.setEnvironmentId(reactableEvent.getEnvironmentId());
            debugApi.setOrganizationHrid(reactableEvent.getOrganizationHrid());
            debugApi.setOrganizationId(reactableEvent.getOrganizationId());

            return debugApi;
        } catch (Exception e) {
            // Log the error and ignore this event.
            log.error("Unable to extract api definition from event [{}].", reactableEvent.getId(), e);
            failEvent(event);
            return null;
        }
    }

    private HttpClientOptions buildClientOptions() {
        HttpClientOptions options = new HttpClientOptions();
        options.setDefaultHost(debugHttpClientConfiguration.getHost());
        options.setDefaultPort(debugHttpClientConfiguration.getPort());
        options.setConnectTimeout(debugHttpClientConfiguration.getConnectTimeout());
        options.setDecompressionSupported(debugHttpClientConfiguration.isCompressionSupported());
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

    MultiMap buildHeaders(ReactableDebugApi<?> debugApi, HttpRequest req) {
        final HeadersMultiMap headers = new HeadersMultiMap();
        headers.addAll(convertHeaders(req.getHeaders()));

        // If API is configured in virtual hosts mode, we force the Host header
        String host = debugApi.extractHost();

        // If gateway is multi tenant, we need to apply access point host
        if (host == null) {
            List<ReactableAccessPoint> accessPoints = accessPointManager.getByEnvironmentId(debugApi.getEnvironmentId());
            if (accessPoints != null && !accessPoints.isEmpty()) {
                host = accessPoints.getFirst().getHost();
            }
        }
        if (host != null) {
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
        if (debugEvent != null) {
            updateEvent(debugEvent, ApiDebugStatus.ERROR)
                .subscribeOn(Schedulers.io())
                .subscribe(() -> {}, throwable -> log.error("Failed to update event {} to ERROR status", debugEvent.getId(), throwable));
        }
    }

    private Completable updateEvent(io.gravitee.repository.management.model.Event debugEvent, ApiDebugStatus apiDebugStatus) {
        return Completable.fromAction(() -> {
            eventRepository.update(
                debugEvent.updateProperties(
                    io.gravitee.repository.management.model.Event.EventProperties.API_DEBUG_STATUS.getValue(),
                    apiDebugStatus.name()
                )
            );
        });
    }

    private void decryptProperties(String apiId, final Properties properties) {
        for (Property property : properties.getProperties()) {
            if (property.isEncrypted()) {
                try {
                    property.setValue(dataEncryptor.decrypt(property.getValue()));
                    property.setEncrypted(false);
                    properties.getValues().put(property.getKey(), property.getValue());
                } catch (GeneralSecurityException e) {
                    log.error("Error decrypting API [{}] property value for key {}", apiId, property.getKey(), e);
                }
            }
        }
    }

    private void decryptProperties(String apiId, List<io.gravitee.definition.model.v4.property.Property> properties) {
        properties.forEach(property -> {
            if (property.isEncrypted()) {
                try {
                    property.setValue(dataEncryptor.decrypt(property.getValue()));
                    property.setEncrypted(false);
                } catch (GeneralSecurityException e) {
                    log.error("Error decrypting API [{}] property value for key {}", apiId, property.getKey(), e);
                }
            }
        });
    }
}
