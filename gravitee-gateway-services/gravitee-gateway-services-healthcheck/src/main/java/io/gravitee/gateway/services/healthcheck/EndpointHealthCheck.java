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
package io.gravitee.gateway.services.healthcheck;

import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.Api;
import io.gravitee.definition.model.Endpoint;
import io.gravitee.definition.model.HttpClientSslOptions;
import io.gravitee.definition.model.HttpProxy;
import io.gravitee.definition.model.services.healthcheck.HealthCheck;
import io.gravitee.gateway.el.function.JsonPathFunction;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.gateway.services.healthcheck.el.HealthCheckResponse;
import io.gravitee.reporter.api.health.HealthStatus;
import io.netty.channel.ConnectTimeoutException;
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
import org.springframework.beans.BeanUtils;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.net.URI;
import java.util.Iterator;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
class EndpointHealthCheck implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(EndpointHealthCheck.class);

    private static final String HTTPS_SCHEME = "https";

    private ReporterService reporterService;

    private final Api api;
    private final HealthCheck healthCheck;

    private final EndpointStatusManager statusManager = new EndpointStatusManager();
    private final Vertx vertx;

    EndpointHealthCheck(Vertx vertx, Api api) {
        this.vertx = vertx;
        this.api = api;
        this.healthCheck = api.getServices().get(HealthCheck.class);
    }

    @Override
    public void run() {
        logger.debug("Running health-check for {}", api);

        // Run request for each endpoint
        if (api.getProxy().getEndpoints() != null) {
            for (Endpoint endpoint : api.getProxy().getEndpoints()) {
                if (endpoint.isHealthcheck()) {
                    // Prepare HTTP client
                    HttpClientOptions httpClientOptions = new HttpClientOptions()
                            .setMaxPoolSize(1)
                            .setKeepAlive(false)
                            .setTcpKeepAlive(false)
                            .setIdleTimeout((int) (endpoint.getHttpClientOptions().getIdleTimeout() / 1000))
                            .setConnectTimeout((int) endpoint.getHttpClientOptions().getConnectTimeout())
                            .setTryUseCompression(endpoint.getHttpClientOptions().isUseCompression());

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

                        if (sslOptions.getPem() != null && ! sslOptions.getPem().isEmpty()) {
                            httpClientOptions.setPemTrustOptions(
                                    new PemTrustOptions().addCertValue(
                                            io.vertx.core.buffer.Buffer.buffer(sslOptions.getPem())));
                        }
                    }

                    HttpClient httpClient = vertx.createHttpClient(httpClientOptions);

                    // Run health-check
                    URI requestUri = URI.create(endpoint.getTarget() + healthCheck.getRequest().getUri());

                    final int port = requestUri.getPort() != -1 ? requestUri.getPort() :
                            (HTTPS_SCHEME.equals(requestUri.getScheme()) ? 443 : 80);

                    HttpClientRequest healthRequest = httpClient.request(
                            HttpMethod.valueOf(healthCheck.getRequest().getMethod().name().toUpperCase()),
                            port,
                            requestUri.getHost(),
                            requestUri.toString()
                    );

                    // Prepare request
                    if (healthCheck.getRequest().getHeaders() != null) {
                        healthCheck.getRequest().getHeaders().forEach(
                                httpHeader -> healthRequest.headers().set(httpHeader.getName(), httpHeader.getValue()));
                    }

                    final HealthStatus.Builder healthBuilder = HealthStatus
                            .forApi(api.getId())
                            .on(System.currentTimeMillis())
                            .method(healthCheck.getRequest().getMethod())
                            .url(requestUri.toString());

                    healthRequest.handler(response -> response.bodyHandler(buffer -> {
                        logger.debug("Health-check endpoint returns a response with a {} status code", response.statusCode());

                        validateAssertions(healthBuilder, new HealthCheckResponse(response, buffer.toString()));
                        report(endpoint, healthBuilder);

                        // Close client
                        httpClient.close();
                    }));

                    healthRequest.exceptionHandler(event -> {
                        logger.info("An error occurs while running health-check request {}: {}", requestUri, event.getMessage());
                        healthBuilder.fail().message(event.getMessage());

                        if (event instanceof ConnectTimeoutException) {
                            healthBuilder.message(event.getMessage());
                            healthBuilder.status(HttpStatusCode.REQUEST_TIMEOUT_408);
                        } else {
                            healthBuilder.status(HttpStatusCode.SERVICE_UNAVAILABLE_503);
                        }

                        report(endpoint, healthBuilder);

                        // Close client
                        httpClient.close();
                    });

                    // Send request
                    logger.debug("Execute health-check request: {}", healthRequest);
                    if (healthCheck.getRequest().getBody() != null && !healthCheck.getRequest().getBody().isEmpty()) {
                        healthRequest.end(healthCheck.getRequest().getBody());
                    } else {
                        healthRequest.end();
                    }
                }
            }
        }
    }

    private void report(final Endpoint endpoint, final HealthStatus.Builder healthBuilder) {
        logger.debug("Report health results for {}", api);
        statusManager.update(endpoint, healthBuilder.isSuccess());
        reporterService.report(healthBuilder.build());
    }

    private void validateAssertions(final HealthStatus.Builder healthBuilder, final HealthCheckResponse response) {
        healthBuilder.success().status(response.getStatus());

        // Run assertions
        if (healthCheck.getExpectation().getAssertions() != null) {
            Iterator<String> assertionIterator = healthCheck.getExpectation().getAssertions().iterator();
            boolean success = true;
            while (success && assertionIterator.hasNext()) {
                try {
                    String assertion = assertionIterator.next();
                    ExpressionParser parser = new SpelExpressionParser();
                    Expression expr = parser.parseExpression(assertion);

                    StandardEvaluationContext context = new StandardEvaluationContext();
                    context.registerFunction("jsonPath",
                            BeanUtils.resolveSignature("evaluate", JsonPathFunction.class));

                    context.setVariable("response", response);

                    success = expr.getValue(context, boolean.class);

                    if (!success) {
                        healthBuilder.message("Assertion can not be verified : " + assertion);
                    }
                } catch (SpelEvaluationException spelex) {
                    success = false;
                    healthBuilder.message("Assertion can not be verified : " + spelex.getMessage());
                }
            }

            healthBuilder.success(success);
        }
    }

    void setReporterService(ReporterService reporterService) {
        this.reporterService = reporterService;
    }
}
