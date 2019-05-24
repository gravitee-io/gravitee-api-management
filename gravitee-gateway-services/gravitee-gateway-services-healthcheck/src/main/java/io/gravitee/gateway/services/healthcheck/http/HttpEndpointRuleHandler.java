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

import io.gravitee.alert.api.event.Event;
import io.gravitee.common.http.HttpHeaders;
import io.gravitee.common.http.HttpStatusCode;
import io.gravitee.definition.model.HttpClientSslOptions;
import io.gravitee.definition.model.HttpProxy;
import io.gravitee.definition.model.endpoint.HttpEndpoint;
import io.gravitee.definition.model.ssl.jks.JKSKeyStore;
import io.gravitee.definition.model.ssl.jks.JKSTrustStore;
import io.gravitee.definition.model.ssl.pem.PEMKeyStore;
import io.gravitee.definition.model.ssl.pem.PEMTrustStore;
import io.gravitee.definition.model.ssl.pkcs12.PKCS12KeyStore;
import io.gravitee.definition.model.ssl.pkcs12.PKCS12TrustStore;
import io.gravitee.gateway.core.endpoint.EndpointException;
import io.gravitee.gateway.services.healthcheck.EndpointRule;
import io.gravitee.gateway.services.healthcheck.EndpointStatusDecorator;
import io.gravitee.gateway.services.healthcheck.eval.EvaluationException;
import io.gravitee.gateway.services.healthcheck.eval.assertion.AssertionEvaluation;
import io.gravitee.gateway.services.healthcheck.http.el.EvaluableHttpResponse;
import io.gravitee.node.api.Node;
import io.gravitee.plugin.alert.AlertEventProducer;
import io.gravitee.reporter.api.common.Request;
import io.gravitee.reporter.api.common.Response;
import io.gravitee.reporter.api.health.EndpointStatus;
import io.gravitee.reporter.api.health.Step;
import io.netty.channel.ConnectTimeoutException;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.net.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class HttpEndpointRuleHandler implements Handler<Long> {

    private final Logger logger = LoggerFactory.getLogger(HttpEndpointRuleHandler.class);

    // Pattern reuse for duplicate slash removal
    private static final Pattern DUPLICATE_SLASH_REMOVER = Pattern.compile("(?<!(http:|https:|wss:|ws:))[//]+");
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

    private final EndpointRule rule;
    private final Vertx vertx;
    private final EndpointStatusDecorator endpointStatus;
    private Handler<EndpointStatus> statusHandler;

    private AlertEventProducer alertEventProducer;
    private Node node;
    private String port;

    public HttpEndpointRuleHandler(Vertx vertx, EndpointRule rule) {
        this.vertx = vertx;
        this.rule = rule;

        endpointStatus = new EndpointStatusDecorator(rule.endpoint());
    }

    private URI create(String target, io.gravitee.definition.model.services.healthcheck.Request request) {
        URI targetURI = URI.create(target);

        if (request.isFromRoot()) {
            try {
                targetURI = new URI(targetURI.getScheme(), targetURI.getAuthority(), null, null, null);
            } catch (URISyntaxException ex) {
                logger.error("Unexpected error while creating healthcheck request from target[{}]", target, ex);
            }
        }

        if (request.getPath() == null || request.getPath().trim().isEmpty()) {
            return targetURI;
        }

        String uri = targetURI.toString() + '/' + request.getPath();
        return URI.create(DUPLICATE_SLASH_REMOVER.matcher(uri).replaceAll("/"));
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

    @Override
    public void handle(Long timer) {
        HttpEndpoint endpoint = (HttpEndpoint) rule.endpoint();

        logger.debug("Running health-check for endpoint: {} [{}]", endpoint.getName(), endpoint.getTarget());

        // Run request for each step
        for (io.gravitee.definition.model.services.healthcheck.Step step : rule.steps()) {
            try {
                URI hcRequestUri = create(endpoint.getTarget(), step.getRequest());

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

                HttpClientSslOptions sslOptions = endpoint.getHttpClientSslOptions();

                if (HTTPS_SCHEME.equalsIgnoreCase(hcRequestUri.getScheme())) {
                    // Configure SSL
                    httpClientOptions.setSsl(true);

                    if (sslOptions != null) {
                        httpClientOptions
                                .setVerifyHost(sslOptions.isHostnameVerifier())
                                .setTrustAll(sslOptions.isTrustAll());

                        // Client trust configuration
                        if (!sslOptions.isTrustAll() && sslOptions.getTrustStore() != null) {
                            switch (sslOptions.getTrustStore().getType()) {
                                case PEM:
                                    PEMTrustStore pemTrustStore = (PEMTrustStore) sslOptions.getTrustStore();
                                    PemTrustOptions pemTrustOptions = new PemTrustOptions();
                                    if (pemTrustStore.getPath() != null && !pemTrustStore.getPath().isEmpty()) {
                                        pemTrustOptions.addCertPath(pemTrustStore.getPath());
                                    } else if (pemTrustStore.getContent() != null && !pemTrustStore.getContent().isEmpty()) {
                                        pemTrustOptions.addCertValue(io.vertx.core.buffer.Buffer.buffer(pemTrustStore.getContent()));
                                    } else {
                                        throw new EndpointException("Missing PEM certificate value for endpoint " + endpoint.getName());
                                    }
                                    httpClientOptions.setPemTrustOptions(pemTrustOptions);
                                    break;
                                case PKCS12:
                                    PKCS12TrustStore pkcs12TrustStore = (PKCS12TrustStore) sslOptions.getTrustStore();
                                    PfxOptions pfxOptions = new PfxOptions();
                                    pfxOptions.setPassword(pkcs12TrustStore.getPassword());
                                    if (pkcs12TrustStore.getPath() != null && !pkcs12TrustStore.getPath().isEmpty()) {
                                        pfxOptions.setPath(pkcs12TrustStore.getPath());
                                    } else if (pkcs12TrustStore.getContent() != null && !pkcs12TrustStore.getContent().isEmpty()) {
                                        pfxOptions.setValue(io.vertx.core.buffer.Buffer.buffer(pkcs12TrustStore.getContent()));
                                    } else {
                                        throw new EndpointException("Missing PKCS12 value for endpoint " + endpoint.getName());
                                    }
                                    httpClientOptions.setPfxTrustOptions(pfxOptions);
                                    break;
                                case JKS:
                                    JKSTrustStore jksTrustStore = (JKSTrustStore) sslOptions.getTrustStore();
                                    JksOptions jksOptions = new JksOptions();
                                    jksOptions.setPassword(jksTrustStore.getPassword());
                                    if (jksTrustStore.getPath() != null && !jksTrustStore.getPath().isEmpty()) {
                                        jksOptions.setPath(jksTrustStore.getPath());
                                    } else if (jksTrustStore.getContent() != null && !jksTrustStore.getContent().isEmpty()) {
                                        jksOptions.setValue(io.vertx.core.buffer.Buffer.buffer(jksTrustStore.getContent()));
                                    } else {
                                        throw new EndpointException("Missing JKS value for endpoint " + endpoint.getName());
                                    }
                                    httpClientOptions.setTrustStoreOptions(jksOptions);
                                    break;
                            }
                        }

                        // Client authentication configuration
                        if (sslOptions.getKeyStore() != null) {
                            switch (sslOptions.getKeyStore().getType()) {
                                case PEM:
                                    PEMKeyStore pemKeyStore = (PEMKeyStore) sslOptions.getKeyStore();
                                    PemKeyCertOptions pemKeyCertOptions = new PemKeyCertOptions();
                                    if (pemKeyStore.getCertPath() != null && !pemKeyStore.getCertPath().isEmpty()) {
                                        pemKeyCertOptions.setCertPath(pemKeyStore.getCertPath());
                                    } else if (pemKeyStore.getCertContent() != null && !pemKeyStore.getCertContent().isEmpty()) {
                                        pemKeyCertOptions.setCertValue(io.vertx.core.buffer.Buffer.buffer(pemKeyStore.getCertContent()));
                                    }
                                    if (pemKeyStore.getKeyPath() != null && !pemKeyStore.getKeyPath().isEmpty()) {
                                        pemKeyCertOptions.setKeyPath(pemKeyStore.getKeyPath());
                                    } else if (pemKeyStore.getKeyContent() != null && !pemKeyStore.getKeyContent().isEmpty()) {
                                        pemKeyCertOptions.setKeyValue(io.vertx.core.buffer.Buffer.buffer(pemKeyStore.getKeyContent()));
                                    }
                                    httpClientOptions.setPemKeyCertOptions(pemKeyCertOptions);
                                    break;
                                case PKCS12:
                                    PKCS12KeyStore pkcs12KeyStore = (PKCS12KeyStore) sslOptions.getKeyStore();
                                    PfxOptions pfxOptions = new PfxOptions();
                                    pfxOptions.setPassword(pkcs12KeyStore.getPassword());
                                    if (pkcs12KeyStore.getPath() != null && !pkcs12KeyStore.getPath().isEmpty()) {
                                        pfxOptions.setPath(pkcs12KeyStore.getPath());
                                    } else if (pkcs12KeyStore.getContent() != null && !pkcs12KeyStore.getContent().isEmpty()) {
                                        pfxOptions.setValue(io.vertx.core.buffer.Buffer.buffer(pkcs12KeyStore.getContent()));
                                    }
                                    httpClientOptions.setPfxKeyCertOptions(pfxOptions);
                                    break;
                                case JKS:
                                    JKSKeyStore jksKeyStore = (JKSKeyStore) sslOptions.getKeyStore();
                                    JksOptions jksOptions = new JksOptions();
                                    jksOptions.setPassword(jksKeyStore.getPassword());
                                    if (jksKeyStore.getPath() != null && !jksKeyStore.getPath().isEmpty()) {
                                        jksOptions.setPath(jksKeyStore.getPath());
                                    } else if (jksKeyStore.getContent() != null && !jksKeyStore.getContent().isEmpty()) {
                                        jksOptions.setValue(io.vertx.core.buffer.Buffer.buffer(jksKeyStore.getContent()));
                                    }
                                    httpClientOptions.setKeyStoreOptions(jksOptions);
                                    break;
                            }
                        }
                    }
                }

                HttpClient httpClient = vertx.createHttpClient(httpClientOptions);

                final int port = hcRequestUri.getPort() != -1 ? hcRequestUri.getPort() :
                        (HTTPS_SCHEME.equals(hcRequestUri.getScheme()) ? 443 : 80);

                String relativeUri = (hcRequestUri.getRawQuery() == null) ? hcRequestUri.getRawPath() :
                        hcRequestUri.getRawPath() + '?' + hcRequestUri.getRawQuery();

                // Run health-check
                HttpClientRequest healthRequest = httpClient.request(
                        HttpMethod.valueOf(step.getRequest().getMethod().name().toUpperCase()),
                        port, hcRequestUri.getHost(), relativeUri);

                // Set timeout on request
                if (endpoint.getHttpClientOptions() != null) {
                    healthRequest.setTimeout(endpoint.getHttpClientOptions().getReadTimeout());
                }

                // Prepare request
                if (step.getRequest().getHeaders() != null) {
                    step.getRequest().getHeaders().forEach(
                            httpHeader -> healthRequest.headers().set(httpHeader.getName(), httpHeader.getValue()));
                }

                final EndpointStatus.Builder healthBuilder = EndpointStatus
                        .forEndpoint(rule.api(), endpoint.getName())
                        .on(currentTimeMillis());

                long startTime = currentTimeMillis();

                Request request = new Request();
                request.setMethod(step.getRequest().getMethod());
                request.setUri(hcRequestUri.toString());

                healthRequest.handler(response -> response.bodyHandler(buffer -> {
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

                    // Close client
                    httpClient.close();
                }));

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

                    if (event instanceof ConnectTimeoutException) {
                        stepBuilder.fail(event.getMessage());
                        healthResponse.setStatus(HttpStatusCode.REQUEST_TIMEOUT_408);
                    } else {
                        healthResponse.setStatus(HttpStatusCode.SERVICE_UNAVAILABLE_503);
                    }

                    Step result = stepBuilder.build();
                    result.setResponse(healthResponse);
                    result.setRequest(request);

                    result.setResponseTime(endTime - startTime);

                    // Append step result
                    healthBuilder.step(result);

                    report(healthBuilder.build());

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
            } catch (EndpointException ee) {
                logger.error("An error occurs while configuring the endpoint " + endpoint.getName() +
                        ". Healthcheck is skipped for this endpoint.", ee);
            } catch (Exception ex) {
                logger.error("An unexpected error occurs", ex);
            }
        }
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

    public void setPort(String port) {
        this.port = port;
    }
}
