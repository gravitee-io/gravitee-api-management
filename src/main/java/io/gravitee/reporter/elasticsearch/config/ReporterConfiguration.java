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
package io.gravitee.reporter.elasticsearch.config;

import io.gravitee.elasticsearch.config.Endpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;

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

	private final static String DEFAULT_ELASTICSEARCH_ENDPOINT = "http://localhost:9200";

	@Autowired
	private Environment environment;
	
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
	 * Request actions max by bulk 
	 */
	@Value("${reporters.elasticsearch.bulk.actions:50}")
	private Integer bulkActions;
	
	/**
	 * Bulk flush interval in seconds
	 */
	@Value("${reporters.elasticsearch.bulk.flush_interval:1}")
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
	 * Settings: number of shards
	 */
	@Value("${reporters.elasticsearch.settings.number_of_shards:5}")
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

	public List<Endpoint> getEndpoints() {
		if(endpoints == null){
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
		String key = String.format("reporters.elasticsearch.endpoints[%s]", 0);
		List<Endpoint> endpoints = new ArrayList<>();
		
		while (environment.containsProperty(key)) {
			String url = environment.getProperty(key);
			endpoints.add(new Endpoint(url));
			
			key = String.format("reporters.elasticsearch.endpoints[%s]", endpoints.size());
		}
		
		// Use default host if required
		if(endpoints.isEmpty()) {
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
}
