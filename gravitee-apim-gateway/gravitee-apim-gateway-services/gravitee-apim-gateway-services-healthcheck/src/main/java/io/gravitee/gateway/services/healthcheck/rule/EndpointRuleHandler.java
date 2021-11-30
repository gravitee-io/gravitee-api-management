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
package io.gravitee.gateway.services.healthcheck.rule;

import io.gravitee.alert.api.event.Event;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.common.utils.UUID;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.gateway.core.endpoint.EndpointException;
import io.gravitee.gateway.services.healthcheck.EndpointRule;
import io.gravitee.gateway.services.healthcheck.EndpointStatusDecorator;
import io.gravitee.gateway.services.healthcheck.eval.EvaluationException;
import io.gravitee.gateway.services.healthcheck.eval.assertion.AssertionEvaluation;
import io.gravitee.gateway.services.healthcheck.http.el.EvaluableHttpResponse;
import io.gravitee.node.api.Node;
import io.gravitee.node.api.utils.NodeUtils;
import io.gravitee.plugin.alert.AlertEventProducer;
import io.gravitee.reporter.api.common.Request;
import io.gravitee.reporter.api.common.Response;
import io.gravitee.reporter.api.health.EndpointStatus;
import io.gravitee.reporter.api.health.Step;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.TimeoutException;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Iterator;
import java.util.regex.Pattern;

import static java.lang.System.currentTimeMillis;

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
    private Handler<EndpointStatus> statusHandler;

    private AlertEventProducer alertEventProducer;
    private Node node;

    private final HttpClient httpClient;

    public EndpointRuleHandler(Vertx vertx, EndpointRule<T> rule, Environment environment) throws Exception{
        this.rule = rule;
        this.environment = environment;

        endpointStatus = new EndpointStatusDecorator(rule.endpoint());

        if (! rule.steps().isEmpty()) {
            // For now, we only allow one step per rule.
            URI uri = createRequest(rule.endpoint(), rule.steps().get(0));

            HttpClientOptions clientOptions = createHttpClientOptions(rule.endpoint(), uri);
            httpClient = vertx.createHttpClient(clientOptions);

        } else {
            httpClient = null;
        }
    }

    @Override
    public void handle(Long timer) {
        T endpoint = rule.endpoint();
        logger.debug("Running health-check for endpoint: {} [{}]", endpoint.getName(), endpoint.getTarget());

        // Run request for each step
        for (io.gravitee.definition.model.services.healthcheck.Step step : rule.steps()) {
            runStep(endpoint, step);
        }
    }

    protected abstract HttpClientOptions createHttpClientOptions(final T endpoint, final URI requestUri) throws Exception;

    protected HttpClientRequest createHttpClientRequest(final HttpClient httpClient, URI request, io.gravitee.definition.model.services.healthcheck.Step step) throws Exception {
        final int port = request.getPort() != -1 ? request.getPort() :
                (HTTPS_SCHEME.equals(request.getScheme()) ? 443 : 80);

        String relativeUri = (request.getRawQuery() == null) ? request.getRawPath() :
                request.getRawPath() + '?' + request.getRawQuery();

        // Run health-check
        HttpClientRequest healthRequest = httpClient.request(
                HttpMethod.valueOf(step.getRequest().getMethod().name().toUpperCase()),
                port, request.getHost(), relativeUri);

        // Prepare request
        if (step.getRequest().getHeaders() != null) {
            step.getRequest().getHeaders().forEach(
                    httpHeader -> healthRequest.headers().set(httpHeader.getName(), httpHeader.getValue()));
        }

        // add custom headers
        healthRequest.putHeader(HttpHeaders.USER_AGENT, NodeUtils.userAgent(node));
        healthRequest.putHeader("X-Gravitee-Request-Id", UUID.toString(UUID.random()));

        return healthRequest;
    }

    protected URI createRequest(T endpoint, io.gravitee.definition.model.services.healthcheck.Step step) {
        URI targetURI = URI.create(endpoint.getTarget());

        if (step.getRequest().isFromRoot()) {
            try {
                targetURI = new URI(targetURI.getScheme(), targetURI.getAuthority(), null, null, null);
            } catch (URISyntaxException ex) {
                logger.error("Unexpected error while creating healthcheck request from target[{}]", endpoint.getTarget(), ex);
            }
        }

        final String path = step.getRequest().getPath();

        if (path == null || path.trim().isEmpty()) {
            return targetURI;
        }

        final String uri;
        if (path.startsWith("/") || path.startsWith("?")) {
            uri = targetURI + path;
        } else {
            uri = targetURI + "/" + path;
        }

        return URI.create(DUPLICATE_SLASH_REMOVER.matcher(uri).replaceAll("/"));

    }

    protected void runStep(T endpoint, io.gravitee.definition.model.services.healthcheck.Step step) {
            try {
                URI hcRequestUri = createRequest(endpoint, step);

                HttpClientRequest healthRequest = createHttpClientRequest(httpClient, hcRequestUri, step);

                final EndpointStatus.Builder healthBuilder = EndpointStatus
                        .forEndpoint(rule.api(), endpoint.getName())
                        .on(currentTimeMillis());

                long startTime = currentTimeMillis();

                Request request = new Request();
                request.setMethod(step.getRequest().getMethod());
                request.setUri(hcRequestUri.toString());

                healthRequest.handler(response -> {
                    response.bodyHandler(buffer -> {
                        long endTime = currentTimeMillis();
                        logger.debug("Health-check endpoint returns a response with a {} status code", response.statusCode());

                        String body = buffer.toString();

                        EndpointStatus.StepBuilder stepBuilder = validateAssertions(step, new EvaluableHttpResponse(response, body));
                        stepBuilder.request(request);
                        stepBuilder.responseTime(endTime - startTime);

                        Response healthResponse = new Response();
                        healthResponse.setStatus(response.statusCode());

                        // If validation fail, store request and response data
                        if (!stepBuilder.isSuccess()) {
                            request.setBody(step.getRequest().getBody());

                            if (step.getRequest().getHeaders() != null) {
                                HttpHeaders reqHeaders = new HttpHeaders();
                                step.getRequest().getHeaders().forEach(httpHeader -> reqHeaders.put(httpHeader.getName(), Collections.singletonList(httpHeader.getValue())));
                                request.setHeaders(reqHeaders);
                            }

                            // Extract headers
                            HttpHeaders headers = new HttpHeaders();
                            response.headers().names().forEach(headerName ->
                                    headers.put(headerName, response.headers().getAll(headerName)));
                            healthResponse.setHeaders(headers);

                            // Store body
                            healthResponse.setBody(body);
                        }

                        stepBuilder.response(healthResponse);

                        // Append step stepBuilder
                        healthBuilder.step(stepBuilder.build());

                        report(healthBuilder.build());
                    });
                    response.exceptionHandler(throwable -> {
                        logger.error("An error has occurred during Health check response handler", throwable);
                    });
                });

                healthRequest.exceptionHandler(event -> {
                    long endTime = currentTimeMillis();

                    EndpointStatus.StepBuilder stepBuilder = EndpointStatus.forStep(step.getName());
                    stepBuilder.fail(event.getMessage());

                    Response healthResponse = new Response();

                    // Extract request information
                    request.setBody(step.getRequest().getBody());
                    if (step.getRequest().getHeaders() != null) {
                        HttpHeaders reqHeaders = new HttpHeaders();
                        step.getRequest().getHeaders().forEach(httpHeader -> reqHeaders.put(httpHeader.getName(), Collections.singletonList(httpHeader.getValue())));
                        request.setHeaders(reqHeaders);
                    }

                    if (event instanceof ConnectTimeoutException || event instanceof TimeoutException) {
                        stepBuilder.fail(event.getMessage());
                        healthResponse.setStatus(HttpStatusCode.GATEWAY_TIMEOUT_504);
                    } else {
                        stepBuilder.fail(event.getMessage());
                        healthResponse.setStatus(HttpStatusCode.BAD_GATEWAY_502);
                    }

                    Step result = stepBuilder.build();
                    result.setResponse(healthResponse);
                    result.setRequest(request);

                    result.setResponseTime(endTime - startTime);

                    // Append step result
                    healthBuilder.step(result);

                    report(healthBuilder.build());
                });

                // Send request
                logger.debug("Execute health-check request: {}", healthRequest);
                if (step.getRequest().getBody() != null && !step.getRequest().getBody().isEmpty()) {
                    healthRequest.end(step.getRequest().getBody());
                } else {
                    healthRequest.end();
                }
            } catch (EndpointException ee) {
                logger.error("An error occurs while configuring the endpoint " + endpoint.getName() +
                        ". Healthcheck is skipped for this endpoint.", ee);
            } catch (Exception ex) {
                logger.error("An unexpected error has occurred while configuring Healthcheck for API : {}", rule.api(), ex);
            }
    }

    private EndpointStatus.StepBuilder validateAssertions(final io.gravitee.definition.model.services.healthcheck.Step step, final EvaluableHttpResponse response) {
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

        if (transition && alertEventProducer != null && ! alertEventProducer.isEmpty()) {
            final Event event = Event
                    .at(currentTimeMillis())
                    .context(CONTEXT_NODE_ID, node.id())
                    .context(CONTEXT_NODE_HOSTNAME, node.hostname())
                    .context(CONTEXT_NODE_APPLICATION, node.application())
                    .type(EVENT_TYPE)
                    .property(PROP_API, rule.api())
                    .property(PROP_ENDPOINT_NAME, rule.endpoint().getName())
                    .property(PROP_STATUS_OLD, previousStatusName)
                    .property(PROP_STATUS_NEW, rule.endpoint().getStatus().name())
                    .property(PROP_SUCCESS, endpointStatus.isSuccess())
                    .property(PROP_TENANT, () -> node.metadata().get("tenant"))
                    .property(PROP_RESPONSE_TIME, responseTime)
                    .property(PROP_MESSAGE, endpointStatus.getSteps().get(0).getMessage())
                    .build();
            alertEventProducer.send(event);
        }

        statusHandler.handle(endpointStatus);
    }

    public void setStatusHandler(Handler<EndpointStatus> statusHandler) {
        this.statusHandler = statusHandler;
    }

    public void setAlertEventProducer(AlertEventProducer alertEventProducer) {
        this.alertEventProducer = alertEventProducer;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    public void close() {
        if (httpClient != null) {
            httpClient.close();
        }
    }
}
