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
package io.gravitee.gateway.api.reporter.metrics;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.gateway.api.reporter.Reportable;

import java.time.Instant;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class Metrics implements Reportable {

    private long proxyResponseTimeMs = -1;

    private long apiResponseTimeMs = -1;

    private String apiName;

    private String apiKey;

    private String requestId;

    private Instant requestTimestamp;

    private HttpMethod requestHttpMethod;

    private String requestPath;

    private String requestLocalAddress;

    private String requestRemoteAddress;

    private String requestContentType;

    private long requestContentLength = -1;

    private int responseHttpStatus;

    private String responseContentType;

    private long responseContentLength = -1;

    private String endpoint;

    public Instant getRequestTimestamp() {
        return requestTimestamp;
    }

    public void setRequestTimestamp(Instant requestTimestamp) {
        this.requestTimestamp = requestTimestamp;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public long getApiResponseTimeMs() {
        return apiResponseTimeMs;
    }

    public void setApiResponseTimeMs(long apiResponseTimeMs) {
        this.apiResponseTimeMs = apiResponseTimeMs;
    }

    public HttpMethod getRequestHttpMethod() {
        return requestHttpMethod;
    }

    public void setRequestHttpMethod(HttpMethod requestHttpMethod) {
        this.requestHttpMethod = requestHttpMethod;
    }

    public int getResponseHttpStatus() {
        return responseHttpStatus;
    }

    public void setResponseHttpStatus(int responseHttpStatus) {
        this.responseHttpStatus = responseHttpStatus;
    }

    public String getRequestLocalAddress() {
        return requestLocalAddress;
    }

    public void setRequestLocalAddress(String requestLocalAddress) {
        this.requestLocalAddress = requestLocalAddress;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    public long getProxyResponseTimeMs() {
        return proxyResponseTimeMs;
    }

    public void setProxyResponseTimeMs(long proxyResponseTimeMs) {
        this.proxyResponseTimeMs = proxyResponseTimeMs;
    }

    public String getRequestRemoteAddress() {
        return requestRemoteAddress;
    }

    public void setRequestRemoteAddress(String requestRemoteAddress) {
        this.requestRemoteAddress = requestRemoteAddress;
    }

    public long getResponseContentLength() {
        return responseContentLength;
    }

    public void setResponseContentLength(long responseContentLength) {
        this.responseContentLength = responseContentLength;
    }

    public String getResponseContentType() {
        return responseContentType;
    }

    public void setResponseContentType(String responseContentType) {
        this.responseContentType = responseContentType;
    }

    public long getRequestContentLength() {
        return requestContentLength;
    }

    public void setRequestContentLength(long requestContentLength) {
        this.requestContentLength = requestContentLength;
    }

    public String getRequestContentType() {
        return requestContentType;
    }

    public void setRequestContentType(String requestContentType) {
        this.requestContentType = requestContentType;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public Instant timestamp() {
        return this.requestTimestamp;
    }
}
