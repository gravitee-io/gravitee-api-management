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
package io.gravitee.gateway.reactive.policy.adapter.context;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.reporter.api.http.Metrics;
import io.gravitee.reporter.api.http.SecurityType;
import io.gravitee.reporter.api.log.Log;
import java.time.Instant;
import java.util.Map;

/**
 * @author Guillaume LAMIRAND (guillaume.lamirand at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MetricsAdapter extends Metrics {

    private io.gravitee.reporter.api.v4.metric.Metrics metrics;

    public MetricsAdapter(final io.gravitee.reporter.api.v4.metric.Metrics metrics) {
        super(metrics.getTimestamp());
        this.metrics = metrics;
    }

    @Override
    public long getTimestamp() {
        return metrics.getTimestamp();
    }

    @Override
    public Instant timestamp() {
        return metrics.timestamp();
    }

    @Override
    public long getProxyResponseTimeMs() {
        return metrics.getGatewayResponseTimeMs();
    }

    @Override
    public long getProxyLatencyMs() {
        return metrics.getGatewayLatencyMs();
    }

    @Override
    public long getApiResponseTimeMs() {
        return metrics.getEndpointResponseTimeMs();
    }

    @Override
    public String getRequestId() {
        return metrics.getRequestId();
    }

    @Override
    public String getApi() {
        return metrics.getApiId();
    }

    @Override
    public String getApiName() {
        return metrics.getApiName();
    }

    @Override
    public String getApplication() {
        return metrics.getApplicationId();
    }

    @Override
    public String getTransactionId() {
        return metrics.getTransactionId();
    }

    @Override
    public String getClientIdentifier() {
        return metrics.getClientIdentifier();
    }

    @Override
    public String getTenant() {
        return metrics.getTenant();
    }

    @Override
    public String getMessage() {
        return metrics.getErrorMessage();
    }

    @Override
    public String getPlan() {
        return metrics.getPlanId();
    }

    @Override
    public String getLocalAddress() {
        return metrics.getLocalAddress();
    }

    @Override
    public String getRemoteAddress() {
        return metrics.getRemoteAddress();
    }

    @Override
    public HttpMethod getHttpMethod() {
        return metrics.getHttpMethod();
    }

    @Override
    public String getHost() {
        return metrics.getHost();
    }

    @Override
    public String getUri() {
        return metrics.getUri();
    }

    @Override
    public long getRequestContentLength() {
        return metrics.getRequestContentLength();
    }

    @Override
    public long getResponseContentLength() {
        return metrics.getResponseContentLength();
    }

    @Override
    public int getStatus() {
        return metrics.getStatus();
    }

    @Override
    public String getEndpoint() {
        return metrics.getEndpoint();
    }

    @Override
    public Log getLog() {
        if (metrics.getLog() != null) {
            return new LogAdapter(metrics.getLog());
        }
        return null;
    }

    @Override
    public String getPath() {
        return metrics.getPathInfo();
    }

    @Override
    public String getMappedPath() {
        return metrics.getMappedPath();
    }

    @Override
    public String getUserAgent() {
        return metrics.getUserAgent();
    }

    @Override
    public String getUser() {
        return metrics.getUser();
    }

    @Override
    public SecurityType getSecurityType() {
        return metrics.getSecurityType();
    }

    @Override
    public String getSecurityToken() {
        return metrics.getSecurityToken();
    }

    @Override
    public String getErrorKey() {
        return metrics.getErrorKey();
    }

    @Override
    public String getSubscription() {
        return metrics.getSubscriptionId();
    }

    @Override
    public String getZone() {
        return metrics.getZone();
    }

    @Override
    public Map<String, String> getCustomMetrics() {
        return metrics.getCustomMetrics();
    }

    @Override
    public void setProxyResponseTimeMs(final long proxyResponseTimeMs) {
        metrics.setGatewayResponseTimeMs(proxyResponseTimeMs);
    }

    @Override
    public void setProxyLatencyMs(final long proxyLatencyMs) {
        metrics.setGatewayLatencyMs(proxyLatencyMs);
    }

    @Override
    public void setApiResponseTimeMs(final long apiResponseTimeMs) {
        metrics.setEndpointResponseTimeMs(apiResponseTimeMs);
    }

    @Override
    public void setRequestId(final String requestId) {
        metrics.setRequestId(requestId);
    }

    @Override
    public void setApi(final String api) {
        metrics.setApiId(api);
    }

    @Override
    public void setApiName(final String apiName) {
        metrics.setApiName(apiName);
    }

    @Override
    public void setApplication(final String application) {
        metrics.setApplicationId(application);
    }

    @Override
    public void setTransactionId(final String transactionId) {
        metrics.setTransactionId(transactionId);
    }

    @Override
    public void setClientIdentifier(final String clientIdentifier) {
        metrics.setClientIdentifier(clientIdentifier);
    }

    @Override
    public void setTenant(final String tenant) {
        metrics.setTenant(tenant);
    }

    @Override
    public void setMessage(final String message) {
        metrics.setErrorMessage(message);
    }

    @Override
    public void setPlan(final String plan) {
        metrics.setPlanId(plan);
    }

    @Override
    public void setLocalAddress(final String localAddress) {
        metrics.setLocalAddress(localAddress);
    }

    @Override
    public void setRemoteAddress(final String remoteAddress) {
        metrics.setRemoteAddress(remoteAddress);
    }

    @Override
    public void setHttpMethod(final HttpMethod httpMethod) {
        metrics.setHttpMethod(httpMethod);
    }

    @Override
    public void setHost(final String host) {
        metrics.setHost(host);
    }

    @Override
    public void setUri(final String uri) {
        metrics.setUri(uri);
    }

    @Override
    public void setRequestContentLength(final long requestContentLength) {
        metrics.setRequestContentLength(requestContentLength);
    }

    @Override
    public void setResponseContentLength(final long responseContentLength) {
        metrics.setResponseContentLength(responseContentLength);
    }

    @Override
    public void setStatus(final int status) {
        metrics.setStatus(status);
    }

    @Override
    public void setEndpoint(final String endpoint) {
        metrics.setEndpoint(endpoint);
    }

    @Override
    public void setLog(final Log log) {
        if (log != null) {
            metrics.setLog(
                io.gravitee.reporter.api.v4.log.Log
                    .builder()
                    .timestamp(log.getTimestamp())
                    .entrypointRequest(log.getClientRequest())
                    .entrypointResponse(log.getClientResponse())
                    .endpointRequest(log.getProxyRequest())
                    .endpointResponse(log.getProxyResponse())
                    .build()
            );
        }
    }

    @Override
    public void setPath(final String path) {
        metrics.setPathInfo(path);
    }

    @Override
    public void setMappedPath(final String mappedPath) {
        metrics.setMappedPath(mappedPath);
    }

    @Override
    public void setUserAgent(final String userAgent) {
        metrics.setUserAgent(userAgent);
    }

    @Override
    public void setUser(final String user) {
        metrics.setUser(user);
    }

    @Override
    public void setSecurityType(final SecurityType securityType) {
        metrics.setSecurityType(securityType);
    }

    @Override
    public void setSecurityToken(final String securityToken) {
        metrics.setSecurityToken(securityToken);
    }

    @Override
    public void setErrorKey(final String errorKey) {
        metrics.setErrorKey(errorKey);
    }

    @Override
    public void setSubscription(final String subscription) {
        metrics.setSubscriptionId(subscription);
    }

    @Override
    public void setZone(final String zone) {
        metrics.setZone(zone);
    }

    @Override
    public void setCustomMetrics(final Map<String, String> customMetrics) {
        metrics.setCustomMetrics(customMetrics);
    }

    @Override
    public void addCustomMetric(final String key, final String value) {
        metrics.addCustomMetric(key, value);
    }
}
