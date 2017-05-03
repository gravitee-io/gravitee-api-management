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
package io.gravitee.management.model.log;

import io.gravitee.common.http.HttpMethod;

import java.util.List;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApiRequest {

    private String id;

    private long timestamp;

    private String transactionId;

    private String uri;

    private String path;

    private HttpMethod method;

    private int status;

    private long responseTime;

    private long apiResponseTime;

    private long requestContentLength;

    private long responseContentLength;

    private String apiKey;

    private String user;

    private String plan;

    private String application;

    private String localAddress;

    private String remoteAddress;

    private String endpoint;

    private String tenant;

    private Map<String, List<String>> clientRequestHeaders;

    private Map<String, List<String>> clientResponseHeaders;

    private Map<String, List<String>> proxyRequestHeaders;

    private Map<String, List<String>> proxyResponseHeaders;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(long responseTime) {
        this.responseTime = responseTime;
    }

    public long getApiResponseTime() {
        return apiResponseTime;
    }

    public void setApiResponseTime(long apiResponseTime) {
        this.apiResponseTime = apiResponseTime;
    }

    public long getRequestContentLength() {
        return requestContentLength;
    }

    public void setRequestContentLength(long requestContentLength) {
        this.requestContentLength = requestContentLength;
    }

    public long getResponseContentLength() {
        return responseContentLength;
    }

    public void setResponseContentLength(long responseContentLength) {
        this.responseContentLength = responseContentLength;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }

    public String getLocalAddress() {
        return localAddress;
    }

    public void setLocalAddress(String localAddress) {
        this.localAddress = localAddress;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public void setRemoteAddress(String remoteAddress) {
        this.remoteAddress = remoteAddress;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Map<String, List<String>> getClientRequestHeaders() {
        return clientRequestHeaders;
    }

    public void setClientRequestHeaders(Map<String, List<String>> clientRequestHeaders) {
        this.clientRequestHeaders = clientRequestHeaders;
    }

    public Map<String, List<String>> getClientResponseHeaders() {
        return clientResponseHeaders;
    }

    public void setClientResponseHeaders(Map<String, List<String>> clientResponseHeaders) {
        this.clientResponseHeaders = clientResponseHeaders;
    }

    public Map<String, List<String>> getProxyRequestHeaders() {
        return proxyRequestHeaders;
    }

    public void setProxyRequestHeaders(Map<String, List<String>> proxyRequestHeaders) {
        this.proxyRequestHeaders = proxyRequestHeaders;
    }

    public Map<String, List<String>> getProxyResponseHeaders() {
        return proxyResponseHeaders;
    }

    public void setProxyResponseHeaders(Map<String, List<String>> proxyResponseHeaders) {
        this.proxyResponseHeaders = proxyResponseHeaders;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ApiRequest request = (ApiRequest) o;

        return id.equals(request.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
