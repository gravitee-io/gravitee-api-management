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
package io.gravitee.repository.elasticsearch.configuration;

import io.gravitee.common.util.EnvironmentUtils;
import io.gravitee.elasticsearch.config.Endpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch repository configuration.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 */
public class RepositoryConfiguration {

	private final static String DEFAULT_ELASTICSEARCH_ENDPOINT = "http://localhost:9200";

	@Autowired
	private Environment environment;
	
	/**
	 * Prefix index name. 
	 */
	@Value("${analytics.elasticsearch.index:gravitee}")
	private String indexName;

	/**
	 * Single index or index per type?
	 */
	@Value("${analytics.elasticsearch.index_per_type:false}")
	private boolean perTypeIndex;

	/**
	 * Elasticsearch basic oauth login. 
	 */
	@Value("${analytics.elasticsearch.security.username:#{null}}")
	private String username;
	
	/**
	 * Elasticsearch basic oauth password. 
	 */
	@Value("${analytics.elasticsearch.security.password:#{null}}")
	private String password;

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

		Map<String, Object> tenantMapping = EnvironmentUtils.getPropertiesStartingWith(
				(ConfigurableEnvironment) environment, "analytics.elasticsearch.cross_cluster.mapping.");

		if (tenantMapping != null) {
			Map<String, String> mapping = new HashMap<>(tenantMapping.size());
			tenantMapping.forEach((s, o) -> mapping.put(s.substring(s.lastIndexOf('.') + 1), (String) o));

			return mapping;
		}

		return null;
	}

	public void setCrossClusterMapping(Map<String, String> crossClusterMapping) {
		this.crossClusterMapping = crossClusterMapping;
	}

	private List<Endpoint> initializeEndpoints() {
		String key = String.format("analytics.elasticsearch.endpoints[%s]", 0);
		List<Endpoint> endpoints = new ArrayList<>();

		while (environment.containsProperty(key)) {
			String url = environment.getProperty(key);
			endpoints.add(new Endpoint(url));

			key = String.format("analytics.elasticsearch.endpoints[%s]", endpoints.size());
		}

		// Use default host if required
		if(endpoints.isEmpty()) {
			endpoints.add(new Endpoint(DEFAULT_ELASTICSEARCH_ENDPOINT));
		}

		return endpoints;
	}
}
