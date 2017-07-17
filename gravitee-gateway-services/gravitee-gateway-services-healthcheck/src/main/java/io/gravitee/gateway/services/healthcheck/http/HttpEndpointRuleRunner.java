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
package io.gravitee.gateway.services.healthcheck.http;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.HttpClientSslOptions;
import io.gravitee.definition.model.HttpProxy;
import io.gravitee.definition.model.services.healthcheck.Step;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.gateway.services.healthcheck.EndpointRule;
import io.gravitee.gateway.services.healthcheck.EndpointStatusDecorator;
import io.gravitee.gateway.services.healthcheck.eval.EvaluationException;
import io.gravitee.gateway.services.healthcheck.eval.assertion.AssertionEvaluation;
import io.gravitee.gateway.services.healthcheck.http.el.EvaluableHttpResponse;
import io.gravitee.reporter.api.health.EndpointHealthStatus;
import io.gravitee.reporter.api.health.StepResult;
import io.netty.channel.ConnectTimeoutException;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.ProxyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Iterator;
import java.util.regex.Pattern;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpEndpointRuleRunner implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(HttpEndpointRuleRunner.class);

    // Pattern reuse for duplicate slash removal
    private static final Pattern DUPLICATE_SLASH_REMOVER = Pattern.compile("(?<!(http:|https:))[//]+");

    private static final String HTTPS_SCHEME = "https";

    private ReporterService reporterService;

    private final EndpointRule rule;

    private final Vertx vertx;

    private final EndpointStatusDecorator endpointStatus;

    public HttpEndpointRuleRunner(Vertx vertx, EndpointRule rule) {
        this.vertx = vertx;
        this.rule = rule;

        endpointStatus = new EndpointStatusDecorator(rule.endpoint());
    }

    @Override
    public void run() {
        this.run0();
    }

    public Future<EndpointHealthStatus> run0() {
        Future<EndpointHealthStatus> future = Future.future();
        Endpoint endpoint = rule.endpoint();

        logger.debug("Running health-check for endpoint: {} [{}]", endpoint.getName(), endpoint.getTarget());

        // Run request for each step
        for (Step step : rule.steps()) {
            String requestUri = create(endpoint.getTarget(), step.getRequest().getPath());
            URI hcRequestUri = URI.create(requestUri);

            // Prepare HTTP client
            HttpClientOptions httpClientOptions = new HttpClientOptions()
                    .setMaxPoolSize(1)
                    .setKeepAlive(false)
                    .setTcpKeepAlive(false);

            if (endpoint.getHttpClientOptions() != null) {
                httpClientOptions
                        .setIdleTimeout((int) (endpoint.getHttpClientOptions().getIdleTimeout() / 1000))
                        .setConnectTimeout((int) endpoint.getHttpClientOptions().getConnectTimeout())
                        .setTryUseCompression(endpoint.getHttpClientOptions().isUseCompression());
            }

            // Configure HTTP proxy
            HttpProxy proxy = endpoint.getHttpProxy();
            if (proxy != null && proxy.isEnabled()) {
                ProxyOptions proxyOptions = new ProxyOptions()
                        .setHost(proxy.getHost())
                        .setPort(proxy.getPort())
                        .setUsername(proxy.getUsername())
                        .setPassword(proxy.getPassword())
                        .setType(ProxyType.valueOf(proxy.getType().name()));

                httpClientOptions.setProxyOptions(proxyOptions);
            }

            // Configure TLS if required
            HttpClientSslOptions sslOptions = endpoint.getHttpClientSslOptions();
            if (sslOptions != null && sslOptions.isEnabled()) {
                httpClientOptions
                        .setSsl(sslOptions.isEnabled())
                        .setVerifyHost(sslOptions.isHostnameVerifier())
                        .setTrustAll(sslOptions.isTrustAll());

                if (sslOptions.getPem() != null && !sslOptions.getPem().isEmpty()) {
                    httpClientOptions.setPemTrustOptions(
                            new PemTrustOptions().addCertValue(
                                    io.vertx.core.buffer.Buffer.buffer(sslOptions.getPem())));
                }
            } else if (HTTPS_SCHEME.equalsIgnoreCase(hcRequestUri.getScheme())) {
                // SSL is not configured but the endpoint scheme is HTTPS so let's enable the SSL on Vert.x HTTP client
                // automatically
                httpClientOptions.setSsl(true).setTrustAll(true);
            }

            HttpClient httpClient = vertx.createHttpClient(httpClientOptions);

            final int port = hcRequestUri.getPort() != -1 ? hcRequestUri.getPort() :
                    (HTTPS_SCHEME.equals(hcRequestUri.getScheme()) ? 443 : 80);

            // Run health-check
            HttpClientRequest healthRequest = httpClient.request(
                    HttpMethod.valueOf(step.getRequest().getMethod().name().toUpperCase()),
                    port,
                    hcRequestUri.getHost(),
                    hcRequestUri.toString()
            );

            // Set timeout on request
            if (endpoint.getHttpClientOptions() != null) {
                healthRequest.setTimeout(endpoint.getHttpClientOptions().getReadTimeout());
            }

            // Prepare request
            if (step.getRequest().getHeaders() != null) {
                step.getRequest().getHeaders().forEach(
                        httpHeader -> healthRequest.headers().set(httpHeader.getName(), httpHeader.getValue()));
            }

            final EndpointHealthStatus.Builder healthBuilder = EndpointHealthStatus
                    .forEndpoint(rule.api(), endpoint.getName())
                    .on(System.currentTimeMillis());

            long startTime = System.currentTimeMillis();

            healthRequest.handler(response -> response.bodyHandler(buffer -> {
                long endTime = System.currentTimeMillis();
                logger.debug("Health-check endpoint returns a response with a {} status code", response.statusCode());

                StepResult result = validateAssertions(step, new EvaluableHttpResponse(response, buffer.toString()));
                result.setMethod(step.getRequest().getMethod());
                result.setUrl(hcRequestUri.toString());
                result.setStatus(response.statusCode());
                result.setResponseTime(endTime - startTime);

                // Append step result
                healthBuilder.step(result);

                updateStatus(healthBuilder, future);

                // Close client
                httpClient.close();
            }));

            healthRequest.exceptionHandler(event -> {
                long endTime = System.currentTimeMillis();
                logger.info("An error occurs while running health-check request {}: {}", hcRequestUri, event.getMessage());

                EndpointHealthStatus.StepBuilder stepBuilder = EndpointHealthStatus.forStep(step.getName());
                stepBuilder.fail(event.getMessage());

                if (event instanceof ConnectTimeoutException) {
                    stepBuilder.fail(event.getMessage());
                    stepBuilder.status(HttpStatusCode.REQUEST_TIMEOUT_408);
                } else {
                    stepBuilder.status(HttpStatusCode.SERVICE_UNAVAILABLE_503);
                }

                StepResult result = stepBuilder.build();
                result.setMethod(step.getRequest().getMethod());
                result.setUrl(hcRequestUri.toString());
                result.setResponseTime(endTime - startTime);

                // Append step result
                healthBuilder.step(result);

                updateStatus(healthBuilder, future);

                try {
                    // Close client
                    httpClient.close();
                } catch (IllegalStateException ise) {
                    // Do not take care about exception when closing client
                }
            });

            // Send request
            logger.debug("Execute health-check request: {}", healthRequest);
            if (step.getRequest().getBody() != null && !step.getRequest().getBody().isEmpty()) {
                healthRequest.end(step.getRequest().getBody());
            } else {
                healthRequest.end();
            }
        }

        return future;
    }

    private String create(String target, String requestPath) {
        if (requestPath == null || requestPath.trim().isEmpty()) {
            return target;
        }

        String uri = target + '/' + requestPath;
        return DUPLICATE_SLASH_REMOVER.matcher(uri).replaceAll("/");
    }

    private void updateStatus(final EndpointHealthStatus.Builder endpointHealthBuilder, final Future<EndpointHealthStatus> future) {
        logger.debug("Report health results for {}", rule.api());

        EndpointHealthStatus healthStatus = endpointHealthBuilder.build();
        endpointStatus.updateStatus(healthStatus.isSuccess());
        healthStatus.setState(rule.endpoint().getStatus().code());
        healthStatus.setAvailable(!rule.endpoint().getStatus().isDown());
        healthStatus.setResponseTime((long) healthStatus.getSteps().stream().mapToLong(StepResult::getResponseTime).average().getAsDouble());
        reporterService.report(healthStatus);
        future.complete(healthStatus);
    }

    private StepResult validateAssertions(final Step step, final EvaluableHttpResponse response) {
        EndpointHealthStatus.StepBuilder stepBuilder = EndpointHealthStatus.forStep(step.getName());

        // HTTP response status
        stepBuilder.status(response.getStatus());

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

        return stepBuilder.build();
    }

    public void setReporterService(ReporterService reporterService) {
        this.reporterService = reporterService;
    }
}
