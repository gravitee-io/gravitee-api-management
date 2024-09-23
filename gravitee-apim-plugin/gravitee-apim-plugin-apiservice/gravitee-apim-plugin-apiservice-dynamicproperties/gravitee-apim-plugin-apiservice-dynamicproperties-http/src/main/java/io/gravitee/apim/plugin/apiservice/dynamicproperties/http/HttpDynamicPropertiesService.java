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
package io.gravitee.apim.plugin.apiservice.dynamicproperties.http;

import com.google.common.annotations.VisibleForTesting;
import io.gravitee.apim.plugin.apiservice.dynamicproperties.http.http.HttpClientFactory;
import io.gravitee.apim.plugin.apiservice.dynamicproperties.http.jolt.JoltMapper;
import io.gravitee.apim.rest.api.common.apiservices.ManagementApiService;
import io.gravitee.apim.rest.api.common.apiservices.ManagementDeploymentContext;
import io.gravitee.apim.rest.api.common.apiservices.events.DynamicPropertiesEvent;
import io.gravitee.apim.rest.api.common.apiservices.events.ManagementApiServiceEvent;
import io.gravitee.common.cron.CronTrigger;
import io.gravitee.common.event.EventManager;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.util.URIUtils;
import io.gravitee.definition.model.v4.Api;
import io.gravitee.definition.model.v4.property.Property;
import io.gravitee.definition.model.v4.service.Service;
import io.gravitee.gateway.reactive.api.exception.PluginConfigurationException;
import io.gravitee.gateway.reactive.api.helper.PluginConfigurationHelper;
import io.gravitee.node.api.cluster.ClusterManager;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.rxjava3.core.buffer.Buffer;
import io.vertx.rxjava3.core.http.HttpClient;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Yann TAVERNIER (yann.tavernier at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Getter
public class HttpDynamicPropertiesService implements ManagementApiService {

    public static final String HTTP_DYNAMIC_PROPERTIES_TYPE = "http-dynamic-properties";

    @SuppressWarnings("java:S3008")
    private static int LOG_ERROR_COUNT = 10;

    private final ManagementDeploymentContext deploymentContext;
    private final PluginConfigurationHelper pluginConfigurationHelper;
    private final ClusterManager clusterManager;
    private final EventManager eventManager;
    private HttpDynamicPropertiesServiceConfiguration configuration;
    private Api api;
    private HttpClient httpClient;
    private JoltMapper joltMapper;

    @VisibleForTesting
    final AtomicReference<Disposable> scheduledJob = new AtomicReference<>();

    @VisibleForTesting
    CronTrigger cronTrigger;

    public HttpDynamicPropertiesService(ManagementDeploymentContext deploymentContext) {
        this.deploymentContext = deploymentContext;
        this.api = deploymentContext.getComponent(Api.class);
        this.clusterManager = deploymentContext.getComponent(ClusterManager.class);
        this.eventManager = deploymentContext.getComponent(EventManager.class);
        this.pluginConfigurationHelper = deploymentContext.getComponent(PluginConfigurationHelper.class);
    }

    @Override
    public String id() {
        return HTTP_DYNAMIC_PROPERTIES_TYPE;
    }

    @Override
    public String kind() {
        return "dynamic-properties";
    }

    @Override
    public Completable start() {
        log.debug("Starting dynamic properties service for Api: {}", api.getId());
        disposeExistingJob();

        try {
            this.configuration =
                pluginConfigurationHelper.readConfiguration(
                    HttpDynamicPropertiesServiceConfiguration.class,
                    api.getServices().getDynamicProperty().getConfiguration()
                );
        } catch (PluginConfigurationException e) {
            return Completable.error(
                new IllegalArgumentException("Unable to start http-dynamic-properties service for api: [" + api.getId() + "]")
            );
        }
        this.httpClient = HttpClientFactory.createClient(deploymentContext, configuration);
        this.joltMapper = new JoltMapper(configuration.getTransformation());

        scheduledJob.set(scheduleInBackground());
        return Completable.complete();
    }

    @Override
    public Completable stop() {
        log.debug("Stopping dynamic properties service for api {}", api.getId());

        disposeExistingJob();

        return httpClient.close();
    }

    @Override
    public Completable update(Api updatedApi) {
        log.debug("Restarting dynamic properties service for api: {}", updatedApi.getId());

        final Disposable currentJob = scheduledJob.get();
        final Service currentDynamicPropertiesService = api.getServices().getDynamicProperty().toBuilder().build();
        final Service updatedDynamicPropertiesService = updatedApi.getServices().getDynamicProperty().toBuilder().build();
        this.api = updatedApi;
        if ((currentJob == null || currentJob.isDisposed()) && updatedDynamicPropertiesService.isEnabled()) {
            // Start dynamic properties for the updated api
            return start();
        }

        if (currentDynamicPropertiesService != null) {
            if (!updatedDynamicPropertiesService.isEnabled()) {
                return stop();
            } else if (!Objects.equals(currentDynamicPropertiesService, updatedDynamicPropertiesService)) {
                return stop().andThen(start());
            }
        }
        return Completable.complete();
    }

    private Disposable scheduleInBackground() {
        cronTrigger = new CronTrigger(configuration.getSchedule());
        final AtomicLong errorCount = new AtomicLong(0);

        return Observable
            .defer(() -> Observable.timer(cronTrigger.nextExecutionIn(), TimeUnit.MILLISECONDS))
            .observeOn(Schedulers.computation())
            .filter(aLong -> clusterManager.self().primary())
            .switchMapCompletable(aLong -> fetchProperties())
            .onErrorResumeNext(throwable -> logOnError(errorCount, throwable))
            .repeat()
            .subscribe(() -> {}, throwable -> log.error("Unable to run Dynamic Properties for Api: {}", api.getId()));
    }

    private Completable fetchProperties() {
        if (!URIUtils.isAbsolute(configuration.getUrl())) {
            return Completable.error(
                new IllegalArgumentException(
                    "Target url [" + configuration.getUrl() + "] must be absolute to perform dynamic properties fetching against."
                )
            );
        }
        final RequestOptions requestOptions = new RequestOptions();
        final MultiMap headers = new HeadersMultiMap();
        configuration.getHeaders().forEach(header -> headers.add(header.getName(), header.getValue()));
        requestOptions.setHeaders(headers);
        final URL target = HttpClientFactory.buildUrl(configuration.getUrl());
        final boolean isSsl = HttpClientFactory.isSecureProtocol(target.getProtocol());

        requestOptions
            .setMethod(HttpMethod.valueOf(configuration.getMethod().name()))
            .setURI(target.getQuery() == null ? target.getPath() : target.getPath() + "?" + target.getQuery())
            .setPort(HttpClientFactory.getPort(target, isSsl))
            .setSsl(isSsl)
            .setHeaders(headers)
            .setHost(target.getHost());

        return httpClient
            .rxRequest(requestOptions)
            .observeOn(Schedulers.io())
            .flatMap(request -> configuration.getBody() != null ? request.rxSend(configuration.getBody()) : request.rxSend())
            .flatMapCompletable(response -> {
                if (response.statusCode() != HttpStatusCode.OK_200) {
                    // Properly end the response
                    return response.toFlowable().ignoreElements();
                }
                return response.body().flatMapCompletable(this::evaluateAndDispatchProperties);
            });
    }

    private Completable logOnError(AtomicLong errorCount, Throwable throwable) {
        if (errorCount.incrementAndGet() == 1) {
            log.warn("Unable to run dynamic properties for api [{}] on URL [{}].", api.getId(), configuration.getUrl(), throwable);
        } else if ((errorCount.get() % LOG_ERROR_COUNT == 0)) {
            log.warn(
                "Unable to run dynamic properties for api [{}] on URL [{}] (times: {}, see previous log report for details).",
                api.getId(),
                configuration.getUrl(),
                errorCount.get()
            );
        }
        return Completable.complete();
    }

    private Completable evaluateAndDispatchProperties(Buffer bodyBuffer) {
        return Completable
            .fromRunnable(() -> {
                final List<Property> properties = joltMapper.map(bodyBuffer.toString());
                eventManager.publishEvent(
                    ManagementApiServiceEvent.DYNAMIC_PROPERTY_UPDATE,
                    new DynamicPropertiesEvent(api.getId(), this.id(), properties)
                );
            })
            .subscribeOn(Schedulers.io());
    }

    /**
     * If a job is running, dispose it
     */
    private void disposeExistingJob() {
        Optional.ofNullable(scheduledJob.get()).ifPresent(Disposable::dispose);
    }

    @VisibleForTesting
    static void setLogErrorCount(int logErrorCount) {
        LOG_ERROR_COUNT = logErrorCount;
    }
}
