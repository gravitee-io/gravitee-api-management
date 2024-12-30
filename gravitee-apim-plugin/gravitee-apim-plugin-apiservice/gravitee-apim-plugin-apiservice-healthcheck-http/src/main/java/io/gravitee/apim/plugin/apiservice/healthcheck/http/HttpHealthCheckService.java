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
package io.gravitee.apim.plugin.apiservice.healthcheck.http;

import static io.reactivex.rxjava3.core.Completable.defer;
import static java.util.Optional.ofNullable;

import com.google.common.base.Strings;
import io.gravitee.apim.plugin.apiservice.healthcheck.http.context.HttpHealthCheckExecutionContext;
import io.gravitee.apim.plugin.apiservice.healthcheck.http.helper.HttpHealthCheckHelper;
import io.gravitee.common.cron.CronTrigger;
import io.gravitee.common.util.URIUtils;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointGroupServices;
import io.gravitee.definition.model.v4.endpointgroup.service.EndpointServices;
import io.gravitee.definition.model.v4.service.Service;
import io.gravitee.gateway.api.buffer.Buffer;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.env.GatewayConfiguration;
import io.gravitee.gateway.reactive.api.apiservice.ApiService;
import io.gravitee.gateway.reactive.api.connector.endpoint.BaseEndpointConnector;
import io.gravitee.gateway.reactive.api.context.DeploymentContext;
import io.gravitee.gateway.reactive.api.context.ExecutionContext;
import io.gravitee.gateway.reactive.api.context.Request;
import io.gravitee.gateway.reactive.api.context.Response;
import io.gravitee.gateway.reactive.api.exception.PluginConfigurationException;
import io.gravitee.gateway.reactive.api.helper.PluginConfigurationHelper;
import io.gravitee.gateway.reactive.core.v4.endpoint.EndpointManager;
import io.gravitee.gateway.reactive.core.v4.endpoint.ManagedEndpoint;
import io.gravitee.gateway.reactive.handlers.api.v4.Api;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.configuration.Configuration;
import io.gravitee.node.vertx.client.http.VertxHttpClientFactory;
import io.gravitee.plugin.alert.AlertEventProducer;
import io.gravitee.plugin.apiservice.healthcheck.common.HealthCheckManagedEndpoint;
import io.gravitee.reporter.api.health.EndpointStatus;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableSource;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.http.impl.headers.HeadersMultiMap;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.core.http.HttpClient;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Slf4j
@Getter
@RequiredArgsConstructor
public class HttpHealthCheckService implements ApiService {

    public static final String HTTP_HEALTH_CHECK_TYPE = "http-health-check";
    public static final String DEFAULT_STEP = "default-step";
    public static final int LOG_ERROR_COUNT = 10;
    public static final int UNREACHABLE_SERVICE = -1;

    private final Api api;
    private final DeploymentContext deploymentContext;
    private final GatewayConfiguration gatewayConfiguration;
    private final AtomicBoolean httpClientCreated = new AtomicBoolean(false);

    private String listenerId;
    private HttpClient httpClient;
    private EndpointManager endpointManager;
    private PluginConfigurationHelper pluginConfigurationHelper;
    private final Map<ManagedEndpoint, Disposable> jobs = new ConcurrentHashMap<>(1);

    @Override
    public String id() {
        return "http-health-check";
    }

    @Override
    public String kind() {
        return "health-check";
    }

    @Override
    public Completable start() {
        endpointManager = deploymentContext.getComponent(EndpointManager.class);
        pluginConfigurationHelper = deploymentContext.getComponent(PluginConfigurationHelper.class);

        // Make sure to listen to any endpoint event (add, remove, disable, ...), so we don't miss endpoint that could be added during the service startup.
        listenerId = endpointManager.addListener(this::processEvent);

        final List<ManagedEndpoint> endpoints = endpointManager.all();
        endpoints.forEach(this::startHealthCheck);

        return Completable.complete();
    }

    @Override
    public Completable stop() {
        log.info("Stopping health check service for api {}.", api.getName());

        // Stop listening to endpoint events.
        endpointManager.removeListener(listenerId);

        jobs.keySet().forEach(this::stopHealthCheck);

        if (httpClientCreated.get()) {
            return httpClient.close();
        }

        return Completable.complete();
    }

    private void processEvent(EndpointManager.Event event, ManagedEndpoint endpoint) {
        if (event.equals(EndpointManager.Event.ADD)) {
            startHealthCheck(endpoint);
        } else if (event.equals(EndpointManager.Event.REMOVE)) {
            stopHealthCheck(endpoint);
        }
    }

    private void startHealthCheck(ManagedEndpoint endpoint) {
        try {
            if (HttpHealthCheckHelper.isServiceEnabled(endpoint, gatewayConfiguration.tenant().orElse(null))) {
                final Service groupHC = ofNullable(endpoint.getGroup().getDefinition().getServices())
                    .map(EndpointGroupServices::getHealthCheck)
                    .orElse(null);
                final Service endpointHC = ofNullable(endpoint.getDefinition().getServices())
                    .map(EndpointServices::getHealthCheck)
                    .orElse(null);
                final HttpHealthCheckServiceConfiguration hcConfiguration = buildConfig(groupHC, endpointHC);

                jobs.computeIfAbsent(endpoint, managedEndpoint -> scheduleInBackground(endpoint, hcConfiguration));
            }
        } catch (PluginConfigurationException e) {
            log.warn("Unable to start healthcheck for api [{}] and endpoint [{}].", api.getName(), endpoint.getDefinition().getName());
        }
    }

    private HttpHealthCheckServiceConfiguration buildConfig(Service groupHC, Service endpointHC) throws PluginConfigurationException {
        final String configuration;
        // Get the configuration from the endpoint if overridden or from the group otherwise.
        if (endpointHC != null && endpointHC.isOverrideConfiguration()) {
            configuration = endpointHC.getConfiguration();
        } else {
            configuration = groupHC.getConfiguration();
        }

        return pluginConfigurationHelper.readConfiguration(HttpHealthCheckServiceConfiguration.class, configuration);
    }

    private Disposable scheduleInBackground(ManagedEndpoint endpoint, HttpHealthCheckServiceConfiguration hcConfiguration) {
        final HealthCheckManagedEndpoint hcManagedEndpoint = new HealthCheckManagedEndpoint(
            api,
            deploymentContext.getComponent(Node.class),
            endpoint,
            endpointManager,
            deploymentContext.getComponent(ReporterService.class),
            deploymentContext.getComponent(AlertEventProducer.class),
            hcConfiguration.getSuccessThreshold(),
            hcConfiguration.getFailureThreshold()
        );
        final CronTrigger cron = new CronTrigger(hcConfiguration.getSchedule());
        final AtomicLong errorCount = new AtomicLong(0);

        return Observable
            .defer(() -> Observable.timer(cron.nextExecutionIn(), TimeUnit.MILLISECONDS))
            .switchMapCompletable(aLong -> {
                final HttpHealthCheckExecutionContext ctx = new HttpHealthCheckExecutionContext(hcConfiguration, deploymentContext);

                if (endpoint.getDefinition().getType().startsWith("http")) {
                    return checkUsingEndpointConnector(hcConfiguration, hcManagedEndpoint, ctx);
                } else {
                    return checkUsingHttpClient(hcConfiguration, hcManagedEndpoint, ctx);
                }
            })
            .onErrorResumeNext(throwable -> continueOnError(endpoint, errorCount, throwable))
            .repeat()
            .subscribe(
                () -> {},
                throwable -> {
                    log.error("Unable to run health check", throwable);
                    jobs.remove(endpoint);
                }
            );
    }

    private Completable checkUsingEndpointConnector(
        HttpHealthCheckServiceConfiguration hcConfiguration,
        HealthCheckManagedEndpoint hcManagedEndpoint,
        HttpHealthCheckExecutionContext ctx
    ) {
        // The endpoint is a http one. Reuse it for efficiency.
        return hcManagedEndpoint
            .<BaseEndpointConnector>getConnector()
            .connect(ctx)
            .onErrorResumeNext(error -> this.ignoreConnectionError(ctx, error))
            .andThen(evaluateAndReport(hcManagedEndpoint, ctx, hcConfiguration));
    }

    private Completable checkUsingHttpClient(
        HttpHealthCheckServiceConfiguration hcConfiguration,
        HealthCheckManagedEndpoint hcManagedEndpoint,
        HttpHealthCheckExecutionContext ctx
    ) {
        if (!URIUtils.isAbsolute(hcConfiguration.getTarget())) {
            return Completable.error(
                new IllegalArgumentException(
                    "Target url [" + hcConfiguration.getTarget() + "] must be absolute to perform health check against non http endpoint."
                )
            );
        }

        final Response response = ctx.response();
        final RequestOptions requestOptions = buildRequestOptions(ctx, hcConfiguration);

        return getOrBuildHttpClient(hcConfiguration)
            .rxRequest(requestOptions)
            .flatMap(httpClientRequest ->
                hcConfiguration.getBody() != null ? httpClientRequest.rxSend(hcConfiguration.getBody()) : httpClientRequest.rxSend()
            )
            .doOnSuccess(endpointResponse -> {
                response.status(endpointResponse.statusCode());
                response.chunks(endpointResponse.toFlowable().map(Buffer::buffer));
                endpointResponse.headers().forEach(header -> response.headers().add(header.getKey(), header.getValue()));
            })
            .ignoreElement()
            .onErrorResumeNext(error -> this.ignoreConnectionError(ctx, error))
            .andThen(evaluateAndReport(hcManagedEndpoint, ctx, hcConfiguration));
    }

    private CompletableSource ignoreConnectionError(HttpHealthCheckExecutionContext ctx, Throwable err) {
        if (err instanceof UnknownHostException || err instanceof SocketException) {
            log.debug("HealthCheck failed, unable to connect to the Service", err);
            final Response response = ctx.response();
            response.status(UNREACHABLE_SERVICE);
            if (!Strings.isNullOrEmpty(err.getMessage())) {
                response.body(Buffer.buffer(err.getMessage()));
            }
            return Completable.complete();
        }
        return Completable.error(err);
    }

    private Completable continueOnError(ManagedEndpoint endpoint, AtomicLong errorCount, Throwable throwable) {
        if (errorCount.incrementAndGet() == 1) {
            log.warn(
                "Unable to run health check for api [{}] and endpoint [{}].",
                api.getId(),
                endpoint.getDefinition().getName(),
                throwable
            );
        } else if ((errorCount.get() % LOG_ERROR_COUNT == 0)) {
            log.warn(
                "Unable to run health check for api [{}] and endpoint [{}] (times: {}, see previous log report for details).",
                api.getId(),
                endpoint.getDefinition().getName(),
                errorCount.get()
            );
        }
        return Completable.complete();
    }

    private void stopHealthCheck(ManagedEndpoint endpoint) {
        final Disposable disposable = jobs.remove(endpoint);

        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    private HttpClient getOrBuildHttpClient(HttpHealthCheckServiceConfiguration hcConfiguration) {
        if (httpClient == null) {
            synchronized (this) {
                // Double-checked locking.
                if (httpClientCreated.compareAndSet(false, true)) {
                    httpClient =
                        VertxHttpClientFactory
                            .builder()
                            .vertx(deploymentContext.getComponent(Vertx.class))
                            .nodeConfiguration(deploymentContext.getComponent(Configuration.class))
                            .defaultTarget(hcConfiguration.getTarget())
                            .build()
                            .createHttpClient();
                }
            }
        }

        return httpClient;
    }

    private RequestOptions buildRequestOptions(final ExecutionContext ctx, final HttpHealthCheckServiceConfiguration hcConfiguration) {
        final RequestOptions requestOptions = new RequestOptions();
        final Request request = ctx.request();

        // Override any request headers that are configured at endpoint level.
        final HttpHeaders configHeaders = ctx.request().headers();
        if (configHeaders != null && !configHeaders.isEmpty()) {
            final MultiMap headers = new HeadersMultiMap();
            configHeaders.forEach(header -> {
                headers.add(header.getKey(), header.getValue());
            });
            requestOptions.setHeaders(headers);
        }

        final URL target = VertxHttpClientFactory.buildUrl(hcConfiguration.getTarget());

        final boolean isSsl = VertxHttpClientFactory.isSecureProtocol(target.getProtocol());
        requestOptions
            .setMethod(HttpMethod.valueOf(request.method().name()))
            .setURI(target.getQuery() == null ? target.getPath() : target.getPath() + "?" + target.getQuery())
            .setPort(VertxHttpClientFactory.getPort(target, isSsl))
            .setSsl(isSsl)
            .setHost(target.getHost());

        ctx.metrics().setEndpoint(VertxHttpClientFactory.toAbsoluteUri(requestOptions, target.getHost(), target.getDefaultPort()));

        return requestOptions;
    }

    private Completable evaluateAndReport(
        final HealthCheckManagedEndpoint hcEndpoint,
        final ExecutionContext ctx,
        final HttpHealthCheckServiceConfiguration hcConfiguration
    ) {
        return defer(() -> {
            final long currentTimestamp = System.currentTimeMillis();
            final Request request = ctx.request();
            final Response response = ctx.response();
            final io.gravitee.reporter.api.common.Request reportRequest = new io.gravitee.reporter.api.common.Request();
            final io.gravitee.reporter.api.common.Response reportResponse = new io.gravitee.reporter.api.common.Response();

            return ctx
                .getTemplateEngine()
                .eval(hcConfiguration.getAssertion(), Boolean.class)
                .defaultIfEmpty(false)
                .onErrorReturnItem(false)
                .flatMapCompletable(success -> {
                    reportRequest.setMethod(request.method());
                    reportRequest.setUri(ctx.metrics().getEndpoint());
                    reportResponse.setStatus(response.status());

                    final EndpointStatus.Builder statusBuilder = EndpointStatus
                        .forEndpoint(api.getId(), api.getName(), hcEndpoint.getDefinition().getName())
                        .on(request.timestamp());

                    final EndpointStatus.StepBuilder stepBuilder = EndpointStatus
                        .forStep(DEFAULT_STEP)
                        .request(reportRequest)
                        .response(reportResponse)
                        .responseTime(currentTimestamp - request.timestamp());

                    if (success) {
                        statusBuilder.step(stepBuilder.success().build());
                        return Completable.fromRunnable(() -> hcEndpoint.reportStatus(true, statusBuilder.build()));
                    } else {
                        reportRequest.setHeaders(request.headers());
                        reportRequest.setBody(hcConfiguration.getBody());
                        reportResponse.setHeaders(response.headers());

                        return response
                            .bodyOrEmpty()
                            .doOnSuccess(body -> {
                                reportResponse.setBody(body.toString());
                                statusBuilder.step(stepBuilder.fail("Assertion not validated: " + hcConfiguration.getAssertion()).build());
                                hcEndpoint.reportStatus(false, statusBuilder.build());
                            })
                            .ignoreElement();
                    }
                });
        });
    }
}
