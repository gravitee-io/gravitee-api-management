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
package io.gravitee.elasticsearch.client.http;

import io.gravitee.elasticsearch.config.Endpoint;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HttpClientConfiguration {

    private List<Endpoint> endpoints;

    /**
     * Elasticsearch basic authentication password.
     */
    private String username;
    private String password;

    private ClientSslConfiguration sslConfig;

    private long requestTimeout = 10000;

    private String proxyType;

    private String proxyHttpHost;
    private int proxyHttpPort;
    private String proxyHttpUsername;
    private String proxyHttpPassword;

    private String proxyHttpsHost;
    private int proxyHttpsPort;
    private String proxyHttpsUsername;
    private String proxyHttpsPassword;

    private boolean isProxyConfigured;

    public List<Endpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(List<Endpoint> endpoints) {
        this.endpoints = endpoints;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public long getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(long requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public ClientSslConfiguration getSslConfig() {
        return sslConfig;
    }

    public void setSslConfig(ClientSslConfiguration sslConfig) {
        this.sslConfig = sslConfig;
    }

    public String getProxyType() {
        return proxyType;
    }

    public void setProxyType(String proxyType) {
        this.proxyType = proxyType;
    }

    public String getProxyHttpHost() {
        return proxyHttpHost;
    }

    public void setProxyHttpHost(String proxyHttpHost) {
        this.proxyHttpHost = proxyHttpHost;
    }

    public int getProxyHttpPort() {
        return proxyHttpPort;
    }

    public void setProxyHttpPort(int proxyHttpPort) {
        this.proxyHttpPort = proxyHttpPort;
    }

    public String getProxyHttpUsername() {
        return proxyHttpUsername;
    }

    public void setProxyHttpUsername(String proxyHttpUsername) {
        this.proxyHttpUsername = proxyHttpUsername;
    }

    public String getProxyHttpPassword() {
        return proxyHttpPassword;
    }

    public void setProxyHttpPassword(String proxyHttpPassword) {
        this.proxyHttpPassword = proxyHttpPassword;
    }

    public String getProxyHttpsHost() {
        return proxyHttpsHost;
    }

    public void setProxyHttpsHost(String proxyHttpsHost) {
        this.proxyHttpsHost = proxyHttpsHost;
    }

    public int getProxyHttpsPort() {
        return proxyHttpsPort;
    }

    public void setProxyHttpsPort(int proxyHttpsPort) {
        this.proxyHttpsPort = proxyHttpsPort;
    }

    public String getProxyHttpsUsername() {
        return proxyHttpsUsername;
    }

    public void setProxyHttpsUsername(String proxyHttpsUsername) {
        this.proxyHttpsUsername = proxyHttpsUsername;
    }

    public String getProxyHttpsPassword() {
        return proxyHttpsPassword;
    }

    public void setProxyHttpsPassword(String proxyHttpsPassword) {
        this.proxyHttpsPassword = proxyHttpsPassword;
    }

    public boolean isProxyConfigured() {
        return isProxyConfigured;
    }

    public void setProxyConfigured(boolean proxyConfigured) {
        isProxyConfigured = proxyConfigured;
    }
}
