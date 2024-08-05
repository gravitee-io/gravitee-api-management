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
package io.gravitee.gateway.services.healthcheck.rule;

import static java.lang.System.currentTimeMillis;

import io.gravitee.alert.api.event.Event;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.utils.UUID;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.services.healthcheck.HealthCheckStep;
import io.gravitee.el.TemplateEngine;
import io.gravitee.el.exceptions.ExpressionEvaluationException;
import io.gravitee.gateway.api.http.HttpHeaders;
import io.gravitee.gateway.http.vertx.VertxHttpHeaders;
import io.gravitee.gateway.services.healthcheck.EndpointRule;
import io.gravitee.gateway.services.healthcheck.EndpointStatusDecorator;
import io.gravitee.gateway.services.healthcheck.eval.EvaluationException;
import io.gravitee.gateway.services.healthcheck.eval.assertion.AssertionEvaluation;
import io.gravitee.gateway.services.healthcheck.http.el.EvaluableHttpResponse;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.utils.NodeUtils;
import io.gravitee.plugin.alert.AlertEventProducer;
import io.gravitee.reporter.api.Reportable;
import io.gravitee.reporter.api.common.Request;
import io.gravitee.reporter.api.common.Response;
import io.gravitee.reporter.api.health.EndpointStatus;
import io.gravitee.reporter.api.health.Step;
import io.netty.channel.ConnectTimeoutException;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.core.net.ProxyOptions;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author GraviteeSource Team
 */
public abstract class EndpointRuleHandler<T extends Endpoint> implements Handler<Long> {

    private final Logger logger = LoggerFactory.getLogger(EndpointRuleHandler.class);

    // Pattern reuse for duplicate slash removal
    private static final Pattern DUPLICATE_SLASH_REMOVER = Pattern.compile("(?<!(grpc:|grpcs:|http:|https:|wss:|ws:))[//]+");

    private static final String HTTPS_SCHEME = "https";

    private static final String EVENT_TYPE = "ENDPOINT_HEALTH_CHECK";
    private static final String CONTEXT_NODE_ID = "node.id";
    private static final String CONTEXT_NODE_HOSTNAME = "node.hostname";
    private static final String CONTEXT_NODE_APPLICATION = "node.application";

    private static final String PROP_RESPONSE_TIME = "response_time";
    private static final String PROP_TENANT = "tenant";
    private static final String PROP_API = "api";
    private static final String PROP_ENDPOINT_NAME = "endpoint.name";
    private static final String PROP_STATUS_OLD = "status.old";
    private static final String PROP_STATUS_NEW = "status.new";
    private static final String PROP_SUCCESS = "success";
    private static final String PROP_MESSAGE = "message";

    protected final EndpointRule<T> rule;
    protected final Environment environment;
    private final EndpointStatusDecorator endpointStatus;
    private TemplateEngine templateEngine;
    private Handler<EndpointStatus> statusHandler;
    private Handler<Void> rescheduleHandler;

    private AlertEventProducer alertEventProducer;
    private Node node;

    private final HttpClient httpClient;
    protected final ProxyOptions systemProxyOptions;

    public EndpointRuleHandler(Vertx vertx, EndpointRule<T> rule, TemplateEngine templateEngine, Environment environment) throws Exception {
        this.rule = rule;
        this.environment = environment;
        this.systemProxyOptions = rule.getSystemProxyOptions();

        endpointStatus = new EndpointStatusDecorator(rule.endpoint());
        this.templateEngine = templateEngine;

        if (!rule.steps().isEmpty()) {
            // For now, we only allow one step per rule.
            URL url = createRequest(rule.endpoint(), rule.steps().get(0));

            HttpClientOptions clientOptions = createHttpClientOptions(rule.endpoint(), url);
            httpClient = vertx.createHttpClient(clientOptions);
        } else {
            httpClient = null;
        }
    }

    @Override
    public void handle(Long timer) {
        try {
            MDC.put("api", rule.api().getId());
            T endpoint = rule.endpoint();
            logger.debug("Running health-check for endpoint: {} [{}]", endpoint.getName(), endpoint.getTarget());

            // We only allow one step per rule. To support more than one step implement healthCheckResponseHandler accordingly
            runStep(endpoint, rule.steps().get(0));
        } finally {
            MDC.remove("api");
        }
    }

    protected abstract HttpClientOptions createHttpClientOptions(final T endpoint, final URL requestUrl) throws Exception;

    protected Future<HttpClientRequest> createHttpClientRequest(final HttpClient httpClient, URL request, HealthCheckStep step) {
        logger.debug("Health-check Request host {}", request.getHost());
        RequestOptions options = prepareHttpClientRequest(request, step);
        logger.debug("Health-check create HttpClient Request with options {}", options.toJson().encodePrettily());
        return httpClient.request(options);
    }

    protected RequestOptions prepareHttpClientRequest(URL request, HealthCheckStep step) {
        final int port = request.getPort() != -1 ? request.getPort() : (HTTPS_SCHEME.equals(request.getProtocol()) ? 443 : 80);

        String relativeUrl = (request.getQuery() == null) ? request.getPath() : request.getPath() + '?' + request.getQuery();

        // Prepare request
        RequestOptions options = new RequestOptions()
            .setURI(relativeUrl)
            .setPort(port)
            .setHost(request.getHost())
            .setMethod(HttpMethod.valueOf(step.getRequest().getMethod().name().toUpperCase()))
            .putHeader(io.vertx.rxjava3.core.http.HttpHeaders.USER_AGENT, NodeUtils.userAgent(node))
            .putHeader("X-Gravitee-Request-Id", UUID.toString(UUID.random()));

        if (step.getRequest().getHeaders() != null) {
            step
                .getRequest()
                .getHeaders()
                .forEach(httpHeader -> {
                    String resolvedHeader = null;
                    try {
                        resolvedHeader = templateEngine.getValue(httpHeader.getValue(), String.class);
                    } catch (ExpressionEvaluationException e) {
                        logger.warn("Expression {} cannot be evaluated", httpHeader.getValue());
                    }

                    options.putHeader(httpHeader.getName(), resolvedHeader == null ? "" : resolvedHeader);
                });
        }

        return options;
    }

    protected URL createRequest(T endpoint, HealthCheckStep step) throws MalformedURLException {
        URL targetURL = new URL(null, templateEngine.getValue(endpoint.getTarget(), String.class));
        logger.debug("Health-check step request{}", step.getRequest());
        if (step.getRequest().isFromRoot()) {
            targetURL = new URL(targetURL.getProtocol(), targetURL.getHost(), targetURL.getPort(), "/");
        }

        final String path = step.getRequest().getPath();

        if (path == null || path.trim().isEmpty()) {
            return targetURL;
        }

        final String url;
        if (path.startsWith("/") || path.startsWith("?")) {
            url = targetURL + path;
        } else {
            url = targetURL + "/" + path;
        }

        URL resultURL = new URL(null, DUPLICATE_SLASH_REMOVER.matcher(url).replaceAll("/"));
        logger.debug("Health-check URL {}", resultURL);
        return resultURL;
    }

    protected void runStep(T endpoint, HealthCheckStep step) {
        try {
            URL hcRequestUrl = createRequest(endpoint, step);
            Future<HttpClientRequest> healthRequestPromise = createHttpClientRequest(httpClient, hcRequestUrl, step);

            healthRequestPromise.onComplete(requestPreparationEvent -> {
                HttpClientRequest healthRequest = requestPreparationEvent.result();
                final EndpointStatus.Builder healthBuilder = EndpointStatus
                    .forEndpoint(rule.api().getId(), rule.api().getName(), endpoint.getName())
                    .on(currentTimeMillis());

                long startTime = currentTimeMillis();

                Request request = new Request();
                request.setMethod(step.getRequest().getMethod());
                request.setUri(hcRequestUrl.toString());

                if (requestPreparationEvent.failed()) {
                    rescheduleHandler.handle(null);
                    reportThrowable(requestPreparationEvent.cause(), step, healthBuilder, startTime, request);
                } else {
                    healthRequest.response(healthRequestEvent -> {
                        if (healthRequestEvent.succeeded()) {
                            HttpClientResponse response = healthRequestEvent.result();
                            response.bodyHandler(buffer -> {
                                long endTime = currentTimeMillis();
                                logger.debug("Health-check endpoint returns a response with a {} status code", response.statusCode());

                                String body = buffer.toString();

                                Step healthCheckStep = buildStep(step, startTime, endTime, request, response, body);

                                // Append step stepBuilder
                                healthBuilder.step(healthCheckStep);

                                rescheduleHandler.handle(null);
                                report(healthBuilder.build());
                            });
                            response.exceptionHandler(throwable -> {
                                logger.error("An error has occurred during Health check response handler", throwable);
                                rescheduleHandler.handle(null);
                            });
                        } else {
                            logger.error("An error has occurred during Health check request", healthRequestEvent.cause());
                            rescheduleHandler.handle(null);
                            reportThrowable(healthRequestEvent.cause(), step, healthBuilder, startTime, request);
                        }
                    });

                    healthRequest.exceptionHandler(throwable -> {
                        rescheduleHandler.handle(null);
                        reportThrowable(throwable, step, healthBuilder, startTime, request);
                    });

                    // Send request
                    logger.debug("Execute health-check request: {}", healthRequest);
                    if (step.getRequest().getBody() != null && !step.getRequest().getBody().isEmpty()) {
                        healthRequest.end(step.getRequest().getBody());
                    } else {
                        healthRequest.end();
                    }
                }
            });
        } catch (Exception ex) {
            logger.error("An unexpected error has occurred while configuring Healthcheck for API : {}", rule.api().getId(), ex);
        }
    }

    private void reportThrowable(
        Throwable throwable,
        HealthCheckStep step,
        EndpointStatus.Builder healthBuilder,
        long startTime,
        Request request
    ) {
        long endTime = currentTimeMillis();
        Step failingStep = buildFailingStep(step, startTime, endTime, request, throwable);

        // Append step result
        healthBuilder.step(failingStep);

        report(healthBuilder.build());
    }

    private Step buildStep(HealthCheckStep step, long startTime, long endTime, Request request, HttpClientResponse response, String body) {
        long responseTime = endTime - startTime;

        EndpointStatus.StepBuilder stepBuilder = validateAssertions(step, new EvaluableHttpResponse(response, body), responseTime);
        stepBuilder.request(request);
        stepBuilder.responseTime(responseTime);

        Response healthResponse = new Response();
        healthResponse.setStatus(response.statusCode());

        // If validation fail, store request and response data
        if (!stepBuilder.isSuccess()) {
            request.setBody(step.getRequest().getBody());

            if (step.getRequest().getHeaders() != null) {
                request.setHeaders(getHttpHeaders(step));
            }

            // Extract headers
            HttpHeaders headers = new VertxHttpHeaders(response.headers());
            healthResponse.setHeaders(headers);

            // Store body
            healthResponse.setBody(body);
        }

        stepBuilder.response(healthResponse);
        return stepBuilder.build();
    }

    private Step buildFailingStep(HealthCheckStep step, long startTime, long endTime, Request request, Throwable throwable) {
        EndpointStatus.StepBuilder stepBuilder = EndpointStatus.forStep(step.getName());
        stepBuilder.fail(throwable.getMessage());
        stepBuilder.request(request);
        stepBuilder.responseTime(endTime - startTime);

        Response healthResponse = new Response();

        // Extract request information
        request.setBody(step.getRequest().getBody());
        if (step.getRequest().getHeaders() != null) {
            request.setHeaders(getHttpHeaders(step));
        }

        if (throwable instanceof ConnectTimeoutException || throwable instanceof TimeoutException) {
            stepBuilder.fail(throwable.getMessage());
            healthResponse.setStatus(HttpStatusCode.GATEWAY_TIMEOUT_504);
        } else {
            stepBuilder.fail(throwable.getMessage());
            healthResponse.setStatus(HttpStatusCode.BAD_GATEWAY_502);
        }

        logger.debug("Health-check failing step because", throwable);

        stepBuilder.response(healthResponse);
        return stepBuilder.build();
    }

    private io.gravitee.gateway.api.http.HttpHeaders getHttpHeaders(HealthCheckStep step) {
        io.gravitee.gateway.api.http.HttpHeaders reqHeaders = io.gravitee.gateway.api.http.HttpHeaders.create();
        step.getRequest().getHeaders().forEach(httpHeader -> reqHeaders.add(httpHeader.getName(), httpHeader.getValue()));
        return reqHeaders;
    }

    private EndpointStatus.StepBuilder validateAssertions(
        final HealthCheckStep step,
        final EvaluableHttpResponse response,
        final long responseTime
    ) {
        EndpointStatus.StepBuilder stepBuilder = EndpointStatus.forStep(step.getName());

        // Run assertions
        if (step.getResponse().getAssertions() != null) {
            Iterator<String> assertionIterator = step.getResponse().getAssertions().iterator();
            boolean success = true;
            while (success && assertionIterator.hasNext()) {
                try {
                    // TODO: assertion must be compiled only one time to preserve CPU
                    AssertionEvaluation evaluation = new AssertionEvaluation(assertionIterator.next());
                    evaluation.setVariable("response", response);
                    evaluation.setVariable("responseTime", responseTime);

                    // Run validation
                    success = evaluation.validate();

                    if (success) {
                        stepBuilder.success();
                    } else {
                        stepBuilder.fail("Assertion not validated: " + evaluation.getAssertion());
                    }
                } catch (EvaluationException eex) {
                    success = false;
                    stepBuilder.fail(eex.getMessage());
                }
            }
        }

        return stepBuilder;
    }

    private void report(final EndpointStatus endpointStatus) {
        final int previousStatusCode = rule.endpoint().getStatus().code();
        final String previousStatusName = rule.endpoint().getStatus().name();
        this.endpointStatus.updateStatus(endpointStatus.isSuccess());
        endpointStatus.setState(rule.endpoint().getStatus().code());
        endpointStatus.setAvailable(!rule.endpoint().getStatus().isDown());

        final long responseTime = endpointStatus.getSteps().stream().mapToLong(Step::getResponseTime).sum();
        endpointStatus.setResponseTime(responseTime);
        final boolean transition = previousStatusCode != rule.endpoint().getStatus().code();
        endpointStatus.setTransition(transition);

        if (transition && alertEventProducer != null && !alertEventProducer.isEmpty()) {
            final Event event = Event
                .at(currentTimeMillis())
                .context(CONTEXT_NODE_ID, node.id())
                .context(CONTEXT_NODE_HOSTNAME, node.hostname())
                .context(CONTEXT_NODE_APPLICATION, node.application())
                .type(EVENT_TYPE)
                .property(PROP_API, rule.api().getId())
                .property(PROP_ENDPOINT_NAME, rule.endpoint().getName())
                .property(PROP_STATUS_OLD, previousStatusName)
                .property(PROP_STATUS_NEW, rule.endpoint().getStatus().name())
                .property(PROP_SUCCESS, endpointStatus.isSuccess())
                .property(PROP_TENANT, () -> node.metadata().get("tenant"))
                .property(PROP_RESPONSE_TIME, responseTime)
                .property(PROP_MESSAGE, endpointStatus.getSteps().get(0).getMessage())
                .organization(rule.api().getOrganizationId())
                .environment(rule.api().getEnvironmentId())
                .build();
            alertEventProducer.send(event);
        }

        statusHandler.handle(endpointStatus);
    }

    public void setStatusHandler(Handler<EndpointStatus> statusHandler) {
        this.statusHandler = statusHandler;
    }

    public void setRescheduleHandler(Handler<Void> rescheduleHandler) {
        this.rescheduleHandler = rescheduleHandler;
    }

    public void setAlertEventProducer(AlertEventProducer alertEventProducer) {
        this.alertEventProducer = alertEventProducer;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public long getDelayMillis() {
        CronTrigger expression = new CronTrigger(rule.schedule());
        Date nextExecutionDate = expression.nextExecutionTime(new SimpleTriggerContext());
        if (nextExecutionDate == null) { // NOSONAR nextExecutionDate is null if the trigger won't fire anymore
            return -1;
        }

        return nextExecutionDate.getTime() - new Date().getTime();
    }

    public void close() {
        if (httpClient != null) {
            httpClient.close();
        }
    }
}
