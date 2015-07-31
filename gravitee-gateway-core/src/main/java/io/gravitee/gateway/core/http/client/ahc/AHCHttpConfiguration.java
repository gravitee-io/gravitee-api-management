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
package io.gravitee.gateway.core.http.client.ahc;

import org.springframework.beans.factory.annotation.Value;

/**
 * @author David BRASSELY (brasseld at gmail.com)
 */
public class AHCHttpConfiguration {

    @Value("${http.client.allowPoolingConnections:true}")
    private boolean allowPoolingConnections;

    @Value("${http.client.requestTimeout:30000}")
    private int requestTimeout;

    @Value("${http.client.readTimeout:30000}")
    private int readTimeout;

    @Value("${http.client.connectTimeout:2000}")
    private int connectTimeout;

    @Value("${http.client.maxConnectionsPerHost:1000}")
    private int maxConnectionsPerHost;

    @Value("${http.client.maxConnections:1000}")
    private int maxConnections;

    public boolean isAllowPoolingConnections() {
        return allowPoolingConnections;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public int getMaxConnectionsPerHost() {
        return maxConnectionsPerHost;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public int getRequestTimeout() {
        return requestTimeout;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AHCHttpConfiguration{");
        sb.append("allowPoolingConnections=").append(allowPoolingConnections);
        sb.append(", requestTimeout=").append(requestTimeout);
        sb.append(", readTimeout=").append(readTimeout);
        sb.append(", connectTimeout=").append(connectTimeout);
        sb.append(", maxConnectionsPerHost=").append(maxConnectionsPerHost);
        sb.append(", maxConnections=").append(maxConnections);
        sb.append('}');
        return sb.toString();
    }
}
