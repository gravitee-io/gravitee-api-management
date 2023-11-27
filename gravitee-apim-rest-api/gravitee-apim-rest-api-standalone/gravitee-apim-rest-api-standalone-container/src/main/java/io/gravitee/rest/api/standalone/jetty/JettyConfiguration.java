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
package io.gravitee.rest.api.standalone.jetty;

import io.gravitee.node.api.configuration.Configuration;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class JettyConfiguration {

    private final Configuration configuration;

    public JettyConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public String getHttpHost() {
        return configuration.getProperty("jetty.host", "0.0.0.0");
    }

    public int getHttpPort() {
        return configuration.getProperty("jetty.port", Integer.class, 8083);
    }

    public int getAcceptors() {
        return configuration.getProperty("jetty.acceptors", Integer.class, -1);
    }

    public int getSelectors() {
        return configuration.getProperty("jetty.selectors", Integer.class, -1);
    }

    public int getPoolMinThreads() {
        return configuration.getProperty("jetty.pool.minThreads", Integer.class, 10);
    }

    public int getPoolMaxThreads() {
        return configuration.getProperty("jetty.pool.maxThreads", Integer.class, 200);
    }

    public boolean isJmxEnabled() {
        return configuration.getProperty("jetty.jmx", Boolean.class, false);
    }

    public int getIdleTimeout() {
        return configuration.getProperty("jetty.idleTimeout", Integer.class, 30000);
    }

    public boolean isStatisticsEnabled() {
        return configuration.getProperty("jetty.statistics", Boolean.class, false);
    }

    public int getPoolIdleTimeout() {
        return configuration.getProperty("jetty.pool.idleTimeout", Integer.class, 60000);
    }

    public int getPoolQueueSize() {
        return configuration.getProperty("jetty.pool.queueSize", Integer.class, 6000);
    }

    public boolean isAccessLogEnabled() {
        return configuration.getProperty("jetty.accesslog.enabled", Boolean.class, true);
    }

    public String getAccessLogPath() {
        return configuration.getProperty(
            "jetty.accesslog.path",
            configuration.getProperty("gravitee.home") + "/logs/gravitee_accesslog_yyyy_mm_dd.log"
        );
    }

    public boolean isSecured() {
        return configuration.getProperty("jetty.secured", Boolean.class, false);
    }

    public String getKeyStorePath() {
        return configuration.getProperty("jetty.ssl.keystore.path");
    }

    public String getKeyStorePassword() {
        return configuration.getProperty("jetty.ssl.keystore.password");
    }

    public String getTrustStorePath() {
        return configuration.getProperty("jetty.ssl.truststore.path");
    }

    public String getTrustStorePassword() {
        return configuration.getProperty("jetty.ssl.truststore.password");
    }

    public String getKeyStoreType() {
        return configuration.getProperty("jetty.ssl.keystore.type");
    }

    public String getTrustStoreType() {
        return configuration.getProperty("jetty.ssl.truststore.type");
    }
}
