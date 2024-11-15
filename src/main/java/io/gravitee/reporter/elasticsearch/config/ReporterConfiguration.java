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
package io.gravitee.reporter.elasticsearch.config;

import static java.lang.String.format;

import io.gravitee.common.util.EnvironmentUtils;
import io.gravitee.elasticsearch.config.Endpoint;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Elasticsearch client reporter configuration.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 * @author Guillaume Waignier (Zenika)
 * @author Sebastien Devaux (Zenika)
 * @author Guillaume Gillon
 */
public class ReporterConfiguration {

    private static final String DEFAULT_ELASTICSEARCH_ENDPOINT = "http://localhost:9200";

    @Autowired
    private ConfigurableEnvironment environment;

    /**
     * Prefix index name.
     */
    @Value("${reporters.elasticsearch.index:gravitee}")
    private String indexName;

    /**
     * Single index or index per type?
     */
    @Value("${reporters.elasticsearch.index_per_type:false}")
    private boolean perTypeIndex;

    /**
     * Index mode normal (daily index) vs ILM (managed by ILM)
     */
    @Value("${reporters.elasticsearch.index_mode:daily}")
    private String indexMode;

    /**
     * Request actions max by bulk
     */
    @Value("${reporters.elasticsearch.bulk.actions:1000}")
    private Integer bulkActions;

    /**
     * Bulk flush interval in seconds
     */
    @Value("${reporters.elasticsearch.bulk.flush_interval:5}")
    private Long flushInterval;

    /**
     * Elasticsearch basic oauth login.
     */
    @Value("${reporters.elasticsearch.security.username:#{null}}")
    private String username;

    /**
     * Elasticsearch basic oauth password.
     */
    @Value("${reporters.elasticsearch.security.password:#{null}}")
    private String password;

    /**
     * Elasticsearch ssl keystore path.
     */
    @Value("${reporters.elasticsearch.ssl.keystore.type:#{null}}")
    private String sslKeystoreType;

    /**
     * Elasticsearch ssl keystore path.
     */
    @Value("${reporters.elasticsearch.ssl.keystore.path:#{null}}")
    private String sslKeystore;

    /**
     * Elasticsearch ssl keystore password.
     */
    @Value("${reporters.elasticsearch.ssl.keystore.password:#{null}}")
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
     * Elasticsearch HTTP request timeout.
     */
    @Value("${reporters.elasticsearch.http.timeout:30000}")
    private long requestTimeout;

    @Value("${reporters.elasticsearch.http.proxy.type:HTTP}")
    private String proxyType;

    @Value("${reporters.elasticsearch.http.proxy.http.host:#{systemProperties['http.proxyHost'] ?: 'localhost'}}")
    private String proxyHttpHost;

    @Value("${reporters.elasticsearch.http.proxy.http.port:#{systemProperties['http.proxyPort'] ?: 3128}}")
    private int proxyHttpPort;

    @Value("${reporters.elasticsearch.http.proxy.http.username:#{null}}")
    private String proxyHttpUsername;

    @Value("${reporters.elasticsearch.http.proxy.http.password:#{null}}")
    private String proxyHttpPassword;

    @Value("${reporters.elasticsearch.http.proxy.https.host:#{systemProperties['https.proxyHost'] ?: 'localhost'}}")
    private String proxyHttpsHost;

    @Value("${reporters.elasticsearch.http.proxy.https.port:#{systemProperties['https.proxyPort'] ?: 3128}}")
    private int proxyHttpsPort;

    @Value("${reporters.elasticsearch.http.proxy.https.username:#{null}}")
    private String proxyHttpsUsername;

    @Value("${reporters.elasticsearch.http.proxy.https.password:#{null}}")
    private String proxyHttpsPassword;

    /**
     * Settings: number of shards
     */
    @Value("${reporters.elasticsearch.settings.number_of_shards:1}")
    private int numberOfShards;

    /**
     * Settings: number of replicas
     */
    @Value("${reporters.elasticsearch.settings.number_of_replicas:1}")
    private int numberOfReplicas;

    /**
     * Settings: refresh interval
     */
    @Value("${reporters.elasticsearch.settings.refresh_interval:5s}")
    private String refreshInterval;

    @Value("${reporters.elasticsearch.enabled:true}")
    private boolean enabled;

    /**
     * Elasticsearch endpoints
     */
    private List<Endpoint> endpoints;

    /**
     * Extended request mapping template
     */
    @Value("${reporters.elasticsearch.template_mapping.extended_request_mapping:#{null}}")
    private String extendedRequestMappingTemplate;

    /**
     * Index indexLifecyclePolicy Policy: monitor
     */
    @Value("${reporters.elasticsearch.lifecycle.policies.monitor:#{null}}")
    private String indexLifecyclePolicyMonitor;

    /**
     * Index indexLifecyclePolicy Policy: health
     */
    @Value("${reporters.elasticsearch.lifecycle.policies.health:#{null}}")
    private String indexLifecyclePolicyHealth;

    /**
     * Index indexLifecyclePolicy Policy: request
     */
    @Value("${reporters.elasticsearch.lifecycle.policies.request:#{null}}")
    private String indexLifecyclePolicyRequest;

    /**
     * Index indexLifecyclePolicy Policy: log
     */
    @Value("${reporters.elasticsearch.lifecycle.policies.log:#{null}}")
    private String indexLifecyclePolicyLog;

    /**
     * Policy name Property name
     */
    @Value("${reporters.elasticsearch.lifecycle.policy_property_name:index.lifecycle.name}")
    private String indexLifecyclePolicyPropertyName;

    /**
     * Rollover name Property name
     */
    @Value("${reporters.elasticsearch.lifecycle.rollover_alias_property_name:index.lifecycle.rollover_alias}")
    private String indexLifecycleRolloverAliasPropertyName;

    /**
     * Extended settings template
     */
    @Value("${reporters.elasticsearch.template_mapping.extended_settings:#{null}}")
    private String extendedSettingsTemplate;

    public List<Endpoint> getEndpoints() {
        if (endpoints == null) {
            endpoints = initializeEndpoints();
        }

        return endpoints;
    }

    public void setEndpoints(List<Endpoint> endpoints) {
        this.endpoints = endpoints;
    }

    public Integer getBulkActions() {
        return bulkActions;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public void setBulkActions(Integer bulkActions) {
        this.bulkActions = bulkActions;
    }

    public void setFlushInterval(Long flushInterval) {
        this.flushInterval = flushInterval;
    }

    public Long getFlushInterval() {
        return flushInterval;
    }

    public String getIndexName() {
        return indexName;
    }

    private List<Endpoint> initializeEndpoints() {
        String key = format("reporters.elasticsearch.endpoints[%s]", 0);
        List<Endpoint> endpoints = new ArrayList<>();

        while (environment.containsProperty(key)) {
            String url = environment.getProperty(key);
            endpoints.add(new Endpoint(url));

            key = format("reporters.elasticsearch.endpoints[%s]", endpoints.size());
        }

        // Use default host if required
        if (endpoints.isEmpty()) {
            endpoints.add(new Endpoint(DEFAULT_ELASTICSEARCH_ENDPOINT));
        }

        return endpoints;
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
            sslPemCerts = readPropertyAsList("reporters.elasticsearch.ssl.keystore.certs");
        }

        return sslPemCerts;
    }

    public void setSslPemCerts(List<String> sslPemCerts) {
        this.sslPemCerts = sslPemCerts;
    }

    public List<String> getSslPemKeys() {
        if (sslPemKeys == null) {
            sslPemKeys = readPropertyAsList("reporters.elasticsearch.ssl.keystore.keys");
        }

        return sslPemKeys;
    }

    public void setSslPemKeys(List<String> sslPemKeys) {
        this.sslPemKeys = sslPemKeys;
    }

    public int getNumberOfShards() {
        return numberOfShards;
    }

    public void setNumberOfShards(int numberOfShards) {
        this.numberOfShards = numberOfShards;
    }

    public int getNumberOfReplicas() {
        return numberOfReplicas;
    }

    public void setNumberOfReplicas(int numberOfReplicas) {
        this.numberOfReplicas = numberOfReplicas;
    }

    public String getRefreshInterval() {
        return refreshInterval;
    }

    public void setRefreshInterval(String refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isPerTypeIndex() {
        return perTypeIndex;
    }

    public void setPerTypeIndex(boolean perTypeIndex) {
        this.perTypeIndex = perTypeIndex;
    }

    public long getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(long requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public String getExtendedRequestMappingTemplate() {
        return extendedRequestMappingTemplate;
    }

    public void setExtendedRequestMappingTemplate(String extendedRequestMappingTemplate) {
        this.extendedRequestMappingTemplate = extendedRequestMappingTemplate;
    }

    public String getIndexLifecyclePolicyMonitor() {
        return indexLifecyclePolicyMonitor;
    }

    public void setIndexLifecyclePolicyMonitor(String indexLifecyclePolicyMonitor) {
        this.indexLifecyclePolicyMonitor = indexLifecyclePolicyMonitor;
    }

    public String getIndexLifecyclePolicyHealth() {
        return indexLifecyclePolicyHealth;
    }

    public void setIndexLifecyclePolicyHealth(String indexLifecyclePolicyHealth) {
        this.indexLifecyclePolicyHealth = indexLifecyclePolicyHealth;
    }

    public String getIndexLifecyclePolicyRequest() {
        return indexLifecyclePolicyRequest;
    }

    public void setIndexLifecyclePolicyRequest(String indexLifecyclePolicyRequest) {
        this.indexLifecyclePolicyRequest = indexLifecyclePolicyRequest;
    }

    public String getIndexLifecyclePolicyLog() {
        return indexLifecyclePolicyLog;
    }

    public void setIndexLifecyclePolicyLog(String indexLifecyclePolicyLog) {
        this.indexLifecyclePolicyLog = indexLifecyclePolicyLog;
    }

    public String getIndexLifecyclePolicyPropertyName() {
        return indexLifecyclePolicyPropertyName;
    }

    public void setIndexLifecyclePolicyPropertyName(String indexLifecyclePolicyPropertyName) {
        this.indexLifecyclePolicyPropertyName = indexLifecyclePolicyPropertyName;
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
        return !EnvironmentUtils.getPropertiesStartingWith(environment, "reporters.elasticsearch.http.proxy").isEmpty();
    }

    public String getExtendedSettingsTemplate() {
        return extendedSettingsTemplate;
    }

    public void setExtendedSettingsTemplate(String extendedSettingsTemplate) {
        this.extendedSettingsTemplate = extendedSettingsTemplate;
    }

    public void setIndexMode(String indexMode) {
        this.indexMode = indexMode;
    }

    public boolean isIlmManagedIndex() {
        return "ilm".equalsIgnoreCase(indexMode);
    }

    private List<String> readPropertyAsList(String property) {
        String key = String.format("%s[%s]", property, 0);
        List<String> properties = new ArrayList<>();

        while (environment.containsProperty(key)) {
            String p = environment.getProperty(key);
            properties.add(p);

            key = String.format("%s[%s]", property, properties.size());
        }

        // fallback to single value style for backward compatibility
        if (properties.isEmpty()) {
            properties.add(environment.getProperty(property));
        }

        return properties;
    }

    public String getIndexLifecycleRolloverAliasPropertyName() {
        return indexLifecycleRolloverAliasPropertyName;
    }

    public void setIndexLifecycleRolloverAliasPropertyName(String indexLifecycleRolloverAliasPropertyName) {
        this.indexLifecycleRolloverAliasPropertyName = indexLifecycleRolloverAliasPropertyName;
    }
}
