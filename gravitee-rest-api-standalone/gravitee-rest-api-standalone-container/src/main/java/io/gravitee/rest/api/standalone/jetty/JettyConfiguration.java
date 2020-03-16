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
package io.gravitee.rest.api.standalone.jetty;

import org.springframework.beans.factory.annotation.Value;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class JettyConfiguration {

    @Value("${jetty.host:0.0.0.0}")
    private String httpHost;

    @Value("${jetty.port:8083}")
    private int httpPort;

    @Value("${jetty.idleTimeout:30000}")
    private int idleTimeout;

    @Value("${jetty.acceptors:-1}")
    private int acceptors;

    @Value("${jetty.selectors:-1}")
    private int selectors;

    @Value("${jetty.pool.minThreads:10}")
    private int poolMinThreads;

    @Value("${jetty.pool.maxThreads:200}")
    private int poolMaxThreads;

    @Value("${jetty.pool.idleTimeout:60000}")
    private int poolIdleTimeout;

    @Value("${jetty.pool.queueSize:6000}")
    private int poolQueueSize;

    @Value("${jetty.jmx:false}")
    private boolean jmxEnabled;

    @Value("${jetty.statistics:false}")
    private boolean statisticsEnabled;

    @Value("${jetty.accesslog.enabled:true}")
    private boolean accessLogEnabled;

    @Value("${jetty.accesslog.path:${gravitee.home}/logs/gravitee_accesslog_yyyy_mm_dd.log}")
    private String accessLogPath;

    @Value("${jetty.secured:false}")
    private boolean secured;

    @Value("${jetty.ssl.keystore.path:#{null}}")
    private String keyStorePath;

    @Value("${jetty.ssl.keystore.password:#{null}}")
    private String keyStorePassword;

    @Value("${jetty.ssl.truststore.path:#{null}}")
    private String trustStorePath;

    @Value("${jetty.ssl.truststore.password:#{null}}")
    private String trustStorePassword;

    public String getHttpHost() {
      return httpHost;
    }

    public void setHttpHost(String httpHost) {
      this.httpHost = httpHost;
    }

    public int getHttpPort() {
        return httpPort;
    }

    public void setHttpPort(int httpPort) {
        this.httpPort = httpPort;
    }

    public int getAcceptors() {
        return acceptors;
    }

    public void setAcceptors(int acceptors) {
        this.acceptors = acceptors;
    }

    public int getSelectors() {
        return selectors;
    }

    public void setSelectors(int selectors) {
        this.selectors = selectors;
    }

    public int getPoolMinThreads() {
        return poolMinThreads;
    }

    public void setPoolMinThreads(int poolMinThreads) {
        this.poolMinThreads = poolMinThreads;
    }

    public int getPoolMaxThreads() {
        return poolMaxThreads;
    }

    public void setPoolMaxThreads(int poolMaxThreads) {
        this.poolMaxThreads = poolMaxThreads;
    }

    public boolean isJmxEnabled() {
        return jmxEnabled;
    }

    public void setJmxEnabled(boolean jmxEnabled) {
        this.jmxEnabled = jmxEnabled;
    }

    public int getIdleTimeout() {
        return idleTimeout;
    }

    public void setIdleTimeout(int idleTimeout) {
        this.idleTimeout = idleTimeout;
    }

    public boolean isStatisticsEnabled() {
        return statisticsEnabled;
    }

    public void setStatisticsEnabled(boolean statisticsEnabled) {
        this.statisticsEnabled = statisticsEnabled;
    }

    public int getPoolIdleTimeout() {
        return poolIdleTimeout;
    }

    public void setPoolIdleTimeout(int poolIdleTimeout) {
        this.poolIdleTimeout = poolIdleTimeout;
    }

    public int getPoolQueueSize() {
        return poolQueueSize;
    }

    public void setPoolQueueSize(int poolQueueSize) {
        this.poolQueueSize = poolQueueSize;
    }

    public boolean isAccessLogEnabled() {
        return accessLogEnabled;
    }

    public void setAccessLogEnabled(boolean accessLogEnabled) {
        this.accessLogEnabled = accessLogEnabled;
    }

    public String getAccessLogPath() {
        return accessLogPath;
    }

    public void setAccessLogPath(String accessLogPath) {
        this.accessLogPath = accessLogPath;
    }

    public boolean isSecured() {
        return secured;
    }

    public void setSecured(boolean secured) {
        this.secured = secured;
    }

    public String getKeyStorePath() {
        return keyStorePath;
    }

    public void setKeyStorePath(String keyStorePath) {
        this.keyStorePath = keyStorePath;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getTrustStorePath() {
        return trustStorePath;
    }

    public void setTrustStorePath(String trustStorePath) {
        this.trustStorePath = trustStorePath;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }
}
