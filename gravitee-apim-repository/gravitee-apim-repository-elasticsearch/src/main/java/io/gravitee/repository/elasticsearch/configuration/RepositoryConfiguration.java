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
package io.gravitee.repository.elasticsearch.configuration;

import io.gravitee.common.util.EnvironmentUtils;
import io.gravitee.elasticsearch.config.Endpoint;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

/**
 * Elasticsearch repository configuration.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RepositoryConfiguration {

    private static final String DEFAULT_ELASTICSEARCH_ENDPOINT = "http://localhost:9200";

    @Autowired
    private Environment environment;

    /**
     * Prefix index name.
     */
    @Value("${repositories.analytics.elasticsearch.index:${analytics.elasticsearch.index:gravitee}}")
    private String indexName;

    /**
     * Single index or index per type?
     */
    @Value("${repositories.analytics.elasticsearch.index_per_type:${analytics.elasticsearch.index_per_type:false}}")
    private boolean perTypeIndex;

    /**
     * Index mode normal (daily index) vs ILM (managed by ILM)
     */
    @Value("${repositories.analytics.elasticsearch.index_mode:${analytics.elasticsearch.index_mode:daily}}")
    private String indexMode;

    /**
     * Elasticsearch basic oauth login.
     */
    @Value("${repositories.analytics.elasticsearch.security.username:${analytics.elasticsearch.security.username:#{null}}}")
    private String username;

    /**
     * Elasticsearch basic oauth password.
     */
    @Value("${repositories.analytics.elasticsearch.security.password:${analytics.elasticsearch.security.password:#{null}}}")
    private String password;

    /**
     * Elasticsearch ssl keystore type. (jks, pkcs12,)
     */
    @Value("${repositories.analytics.elasticsearch.ssl.keystore.type:${analytics.elasticsearch.ssl.keystore.type:#{null}}}")
    private String sslKeystoreType;

    /**
     * Elasticsearch ssl keystore path.
     */
    @Value("${repositories.analytics.elasticsearch.ssl.keystore.path:${analytics.elasticsearch.ssl.keystore.path:#{null}}}")
    private String sslKeystore;

    /**
     * Elasticsearch ssl keystore password.
     */
    @Value("${repositories.analytics.elasticsearch.ssl.keystore.password:${analytics.elasticsearch.ssl.keystore.password:#{null}}}")
    private String sslKeystorePassword;

    /**
     * Elasticsearch ssl pem certs paths
     */
    private List<String> sslPemCerts;

    /**
     * Elasticsearch ssl pem keys paths
     */
    private List<String> sslPemKeys;

    /**
     * Configurable request timeout for http requests to elasticsearch
     */
    @Value("${repositories.analytics.elasticsearch.http.timeout:${analytics.elasticsearch.http.timeout:10000}}")
    private Long requestTimeout;

    /**
     * Proxy settings
     */
    @Value("${repositories.analytics.elasticsearch.http.proxy.type:${analytics.elasticsearch.http.proxy.type:HTTP}}")
    private String proxyType;

    @Value(
        "${repositories.analytics.elasticsearch.http.proxy.http.host:${analytics.elasticsearch.http.proxy.http.host:#{systemProperties['http.proxyHost'] ?: 'localhost'}}}"
    )
    private String proxyHttpHost;

    @Value(
        "${repositories.analytics.elasticsearch.http.proxy.http.port:${analytics.elasticsearch.http.proxy.http.port:#{systemProperties['http.proxyPort'] ?: 3128}}}"
    )
    private int proxyHttpPort;

    @Value("${repositories.analytics.elasticsearch.http.proxy.http.username:${analytics.elasticsearch.http.proxy.http.username:#{null}}}")
    private String proxyHttpUsername;

    @Value("${repositories.analytics.elasticsearch.http.proxy.http.password:${analytics.elasticsearch.http.proxy.http.password:#{null}}}")
    private String proxyHttpPassword;

    @Value(
        "${repositories.analytics.elasticsearch.http.proxy.https.host:${analytics.elasticsearch.http.proxy.https.host:#{systemProperties['https.proxyHost'] ?: 'localhost'}}}"
    )
    private String proxyHttpsHost;

    @Value(
        "${repositories.analytics.elasticsearch.http.proxy.https.port:${analytics.elasticsearch.http.proxy.https.port:#{systemProperties['https.proxyPort'] ?: 3128}}}"
    )
    private int proxyHttpsPort;

    @Value("${repositories.analytics.elasticsearch.http.proxy.https.username:${analytics.elasticsearch.http.proxy.https.username:#{null}}}")
    private String proxyHttpsUsername;

    @Value("${repositories.analytics.elasticsearch.http.proxy.https.password:${analytics.elasticsearch.http.proxy.https.password:#{null}}}")
    private String proxyHttpsPassword;

    /**
     * Elasticsearch endpoints
     */
    private List<Endpoint> endpoints;

    private Map<String, String> crossClusterMapping;

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

    public Long getRequestTimeout() {
        return requestTimeout;
    }

    public List<Endpoint> getEndpoints() {
        if (endpoints == null) {
            endpoints = initializeEndpoints();
        }

        return endpoints;
    }

    public void setEndpoints(List<Endpoint> endpoints) {
        this.endpoints = endpoints;
    }

    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public boolean isPerTypeIndex() {
        return perTypeIndex;
    }

    public void setPerTypeIndex(boolean perTypeIndex) {
        this.perTypeIndex = perTypeIndex;
    }

    public String getSslKeystoreType() {
        return sslKeystoreType;
    }

    public void setSslKeystoreType(String sslKeystoreType) {
        this.sslKeystoreType = sslKeystoreType;
    }

    public String getSslKeystore() {
        return sslKeystore;
    }

    public void setSslKeystore(String sslKeystore) {
        this.sslKeystore = sslKeystore;
    }

    public String getSslKeystorePassword() {
        return sslKeystorePassword;
    }

    public void setSslKeystorePassword(String sslKeystorePassword) {
        this.sslKeystorePassword = sslKeystorePassword;
    }

    public List<String> getSslPemCerts() {
        if (sslPemCerts == null) {
            sslPemCerts = readPropertyAsList("analytics.elasticsearch.ssl.keystore.certs");
        }

        return sslPemCerts;
    }

    public void setSslPemCerts(List<String> sslPemCerts) {
        this.sslPemCerts = sslPemCerts;
    }

    public List<String> getSslPemKeys() {
        if (sslPemKeys == null) {
            sslPemKeys = readPropertyAsList("analytics.elasticsearch.ssl.keystore.keys");
        }

        return sslPemKeys;
    }

    public void setSslPemKeys(List<String> sslPemKeys) {
        this.sslPemKeys = sslPemKeys;
    }

    public String getIndexMode() {
        return indexMode;
    }

    public void setIndexMode(String indexMode) {
        this.indexMode = indexMode;
    }

    public boolean isILMIndex() {
        return indexMode != null && indexMode.equalsIgnoreCase("ilm");
    }

    private boolean crossClusterInitialized;

    public Map<String, String> getCrossClusterMapping() {
        if (!crossClusterInitialized) {
            crossClusterMapping = initializeCrossClusterMapping();
        }

        return crossClusterMapping;
    }

    public boolean hasCrossClusterMapping() {
        Map<String, String> mapping = getCrossClusterMapping();
        return mapping != null && !mapping.isEmpty();
    }

    private Map<String, String> initializeCrossClusterMapping() {
        crossClusterInitialized = true;

        String newPrefix = "repositories.analytics.elasticsearch.cross_cluster.mapping.";
        String oldPrefix = "analytics.elasticsearch.cross_cluster.mapping.";

        Map<String, Object> tenantMapping = EnvironmentUtils.getPropertiesStartingWith((ConfigurableEnvironment) environment, newPrefix);

        if (tenantMapping.isEmpty()) {
            tenantMapping = EnvironmentUtils.getPropertiesStartingWith((ConfigurableEnvironment) environment, oldPrefix);
        }

        if (tenantMapping != null && !tenantMapping.isEmpty()) {
            Map<String, String> mapping = new HashMap<>(tenantMapping.size());
            tenantMapping.forEach((s, o) -> mapping.put(s.substring(s.lastIndexOf('.') + 1), (String) o));

            return mapping;
        }

        return null;
    }

    public void setCrossClusterMapping(Map<String, String> crossClusterMapping) {
        this.crossClusterMapping = crossClusterMapping;
    }

    private List<String> readPropertyAsList(String property) {
        String newPrefix = "repositories.analytics.elasticsearch." + property;
        String oldPrefix = property;

        List<String> properties = readPropertyAsListFromKey(newPrefix);

        if (properties.isEmpty()) {
            properties = readPropertyAsListFromKey(oldPrefix);
        }

        // fallback to single value style for backward compatibility
        if (properties.isEmpty()) {
            String newValue = environment.getProperty(newPrefix);
            String oldValue = environment.getProperty(oldPrefix);
            if (newValue != null) {
                properties.add(newValue);
            } else if (oldValue != null) {
                properties.add(oldValue);
            }
        }

        return properties;
    }

    private List<String> readPropertyAsListFromKey(String property) {
        String key = String.format("%s[%s]", property, 0);
        List<String> properties = new ArrayList<>();

        while (environment.containsProperty(key)) {
            String p = environment.getProperty(key);
            properties.add(p);

            key = String.format("%s[%s]", property, properties.size());
        }
        return properties;
    }

    private List<Endpoint> initializeEndpoints() {
        String newPrefix = "repositories.analytics.elasticsearch.endpoints";
        String oldPrefix = "analytics.elasticsearch.endpoints";

        // Try to read from new path first
        List<Endpoint> endpoints = readEndpointsFromKey(newPrefix);

        // Fallback to legacy path
        if (endpoints.isEmpty()) {
            endpoints = readEndpointsFromKey(oldPrefix);
        }

        // Use default host if still empty
        if (endpoints.isEmpty()) {
            endpoints.add(new Endpoint(DEFAULT_ELASTICSEARCH_ENDPOINT));
        }

        return endpoints;
    }

    private List<Endpoint> readEndpointsFromKey(String baseKey) {
        List<Endpoint> endpoints = new ArrayList<>();
        int i = 0;
        String key = String.format("%s[%s]", baseKey, i);

        while (environment.containsProperty(key)) {
            endpoints.add(new Endpoint(environment.getProperty(key)));
            key = String.format("%s[%s]", baseKey, ++i);
        }
        return endpoints;
    }

    public void setRequestTimeout(Long requestTimeout) {
        this.requestTimeout = requestTimeout;
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
        String newPrefix = "repositories.analytics.elasticsearch.http.proxy";
        String oldPrefix = "analytics.elasticsearch.http.proxy";

        return (
            !EnvironmentUtils.getPropertiesStartingWith((ConfigurableEnvironment) environment, newPrefix).isEmpty() ||
            !EnvironmentUtils.getPropertiesStartingWith((ConfigurableEnvironment) environment, oldPrefix).isEmpty()
        );
    }

    public boolean isCrossClusterInitialized() {
        return crossClusterInitialized;
    }

    public void setCrossClusterInitialized(boolean crossClusterInitialized) {
        this.crossClusterInitialized = crossClusterInitialized;
    }
}
