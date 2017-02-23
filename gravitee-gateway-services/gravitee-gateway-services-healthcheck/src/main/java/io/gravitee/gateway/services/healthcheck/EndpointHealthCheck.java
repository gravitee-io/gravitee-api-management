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
import io.gravitee.definition.model.services.healthcheck.HealthCheck;
import io.gravitee.gateway.el.function.JsonPathFunction;
import io.gravitee.gateway.report.ReporterService;
import io.gravitee.gateway.services.healthcheck.el.HealthCheckResponse;
import io.gravitee.reporter.api.health.HealthStatus;
import org.asynchttpclient.*;
import org.asynchttpclient.uri.Uri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.net.SocketTimeoutException;
import java.util.Iterator;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
class EndpointHealthCheck implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointHealthCheck.class);

    private static final int GLOBAL_TIMEOUT = 2000;

    private ReporterService reporterService;

    private final AsyncHttpClient asyncHttpClient;
    private final Api api;
    private final HealthCheck healthCheck;

    private final EndpointStatusManager statusManager = new EndpointStatusManager();

    EndpointHealthCheck(Api api) {
        this.api = api;
        this.healthCheck = api.getServices().get(HealthCheck.class);
        asyncHttpClient = createAsyncHttpClient();
    }

    @Override
    public void run() {
        LOGGER.debug("Runnning health-check for {}", api);

        // Prepare request
        RequestBuilder requestBuilder = new org.asynchttpclient.RequestBuilder(
                healthCheck.getRequest().getMethod().name());

        if (healthCheck.getRequest().getHeaders() != null) {
            healthCheck.getRequest().getHeaders().forEach(
                    httpHeader -> requestBuilder.setHeader(httpHeader.getName(), httpHeader.getValue()));
        }

        if (healthCheck.getRequest().getBody() != null && !healthCheck.getRequest().getBody().isEmpty()) {
            requestBuilder.setBody(healthCheck.getRequest().getBody());
        }

        // Run request for each endpoint
        if (api.getProxy().getEndpoints() != null) {
            for (Endpoint endpoint : api.getProxy().getEndpoints()) {
                if (endpoint.isHealthcheck()) {
                    String requestUri = endpoint.getTarget() + healthCheck.getRequest().getUri();
                    requestBuilder.setUri(Uri.create(requestUri));

                    Request request = requestBuilder.build();
                    LOGGER.debug("Execute health-check request: {}", request);

                    final HealthStatus.Builder healthBuilder = HealthStatus
                            .forApi(api.getId())
                            .on(System.currentTimeMillis())
                            .method(healthCheck.getRequest().getMethod())
                            .url(requestUri);

                    asyncHttpClient.prepareRequest(requestBuilder.build()).execute(new AsyncCompletionHandler<Void>() {
                        @Override
                        public Void onCompleted(Response response) throws Exception {
                            validateAssertions(healthBuilder, new HealthCheckResponse(response));
                            report(endpoint, healthBuilder);
                            return null;
                        }

                        @Override
                        public void onThrowable(Throwable t) {
                            LOGGER.debug("An error occurs while executing request {}: {}", request, t.getMessage());
                            healthBuilder.fail().message(t.getMessage());

                            if (t instanceof SpelEvaluationException) {
                                healthBuilder.message(t.getMessage());
                                healthBuilder.status(HttpStatusCode.INTERNAL_SERVER_ERROR_500);
                            } else if (t instanceof SocketTimeoutException) {
                                healthBuilder.message(t.getMessage());
                                healthBuilder.status(HttpStatusCode.REQUEST_TIMEOUT_408);
                            } else {
                                healthBuilder.status(HttpStatusCode.SERVICE_UNAVAILABLE_503);
                            }

                            report(endpoint, healthBuilder);
                        }
                    });
                }
            }
        }
    }

    private void report(final Endpoint endpoint, final HealthStatus.Builder healthBuilder) {
        LOGGER.debug("Report health results for {}", api);
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
            }

            healthBuilder.success(success);
        }
    }

    private AsyncHttpClient createAsyncHttpClient() {
        AsyncHttpClientConfig cf = new DefaultAsyncHttpClientConfig.Builder()
                .setConnectTimeout(GLOBAL_TIMEOUT)
                .setReadTimeout(GLOBAL_TIMEOUT)
                .setRequestTimeout(GLOBAL_TIMEOUT)
                .setMaxConnections(10)
                .setMaxConnectionsPerHost(5)
                .setAcceptAnyCertificate(true)
                .setThreadPoolName("healthcheck-poller")
                .build();

        return new DefaultAsyncHttpClient(cf);
    }

    void setReporterService(ReporterService reporterService) {
        this.reporterService = reporterService;
    }
}
