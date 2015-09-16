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
package io.gravitee.gateway.core.definition;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class HttpClientDefinition {

    @JsonProperty("read_timeout")
    private int readTimeout = 60000;

    @JsonProperty("request_timeout")
    private int requestTimeout = 60000;

    @JsonProperty("connect_timeout")
    private int connectTimeout = 5000;

    @JsonProperty("max_connections")
    private int maxConnections = -1;

    @JsonProperty("request_retry")
    private int requestRetry = 0;

    @JsonProperty("max_connections_per_host")
    private int maxConnectionsPerHost = -1;

    @JsonProperty("use_proxy")
    private boolean useProxy = false;

    @JsonProperty("http_proxy")
    private HttpProxyDefinition httpProxy = new HttpProxyDefinition();

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public int getMaxConnectionsPerHost() {
        return maxConnectionsPerHost;
    }

    public void setMaxConnectionsPerHost(int maxConnectionsPerHost) {
        this.maxConnectionsPerHost = maxConnectionsPerHost;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(int requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public int getRequestRetry() {
        return requestRetry;
    }

    public void setRequestRetry(int requestRetry) {
        this.requestRetry = requestRetry;
    }

    public HttpProxyDefinition getHttpProxy() {
        return httpProxy;
    }

    public void setHttpProxy(HttpProxyDefinition httpProxy) {
        this.httpProxy = httpProxy;
    }

    public boolean isUseProxy() {
        return useProxy;
    }

    public void setUseProxy(boolean useProxy) {
        this.useProxy = useProxy;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("HttpClient{");
        sb.append("connectTimeout=").append(connectTimeout);
        sb.append(", readTimeout=").append(readTimeout);
        sb.append(", requestTimeout=").append(requestTimeout);
        sb.append(", maxConnections=").append(maxConnections);
        sb.append(", maxConnectionsPerHost=").append(maxConnectionsPerHost);
        sb.append(", useProxy=").append(useProxy);
        sb.append(", httpProxy=").append(httpProxy);
        sb.append('}');
        return sb.toString();
    }
}
