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
package io.gravitee.repository.elasticsearch.analytics.query;

import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.index.IndexNameGenerator;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.elasticsearch.templating.freemarker.FreeMarkerComponent;
import io.gravitee.elasticsearch.utils.Type;
import io.gravitee.repository.analytics.query.AbstractQuery;
import io.gravitee.repository.analytics.query.Query;
import io.gravitee.repository.analytics.query.response.Response;
import io.gravitee.repository.elasticsearch.analytics.ElasticsearchQueryCommand;
import io.gravitee.repository.elasticsearch.configuration.RepositoryConfiguration;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Abstract class used to execute an analytic Elasticsearch query.
 * 
 * Based on Command Design Pattern.
 * 
 * @author Guillaume Waignier (Zenika)
 * @author Sebastien Devaux (Zenika)
 *
 */
public abstract class AbstractElasticsearchQueryCommand<T extends Response> implements ElasticsearchQueryCommand<T> {

	/**
	 * Logger.
	 */
	protected final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	/**
	 * Elasticsearch client to perform search request.
	 */
	@Autowired
	protected Client client;

	/**
	 * Templating component
	 */
	@Autowired
	private FreeMarkerComponent freeMarkerComponent;

	/**
	 * Util component used to compute index name.
	 */
	@Autowired
	protected IndexNameGenerator indexNameGenerator;

	@Autowired
	protected RepositoryConfiguration configuration;

	private final static String TENANT_FIELD = "tenant";

	/**
	 * Create the elasticsearch query
	 * @param templateName Freemarker template name
	 * @param query query parameter
	 * @return the elasticsearch json query
	 */
	String createQuery(final String templateName, final Query<T> query) {
		final Map<String, Object> data = new HashMap<>();
		data.put("query", query);
		final String request = this.freeMarkerComponent.generateFromTemplate(templateName, data);
		
		logger.debug("ES request {}", request);
		
		return request;
	}

	Single<SearchResponse> execute(AbstractQuery<T> query, Type type, String sQuery) {
		final Single<SearchResponse> result;

		String [] clusters = null;

		if (configuration.hasCrossClusterMapping()) {
			clusters = extractCluster(query);
		}

		if (query.timeRange() != null) {
			final Long from = query.timeRange().range().from();
			final Long to = query.timeRange().range().to();

			result = this.client.search(
					this.indexNameGenerator.getIndexName(type, from, to, clusters),
					type.getType(),
					sQuery);
		} else {
			result = this.client.search(
					this.indexNameGenerator.getTodayIndexName(type, clusters),
					type.getType(),
					sQuery);
		}

		return result;
	}

	String [] extractCluster(AbstractQuery<T> query) {
		// Extract tenant(s) filtering
		if (query != null && query.query() != null && query.query().filter() != null) {
			String filter = query.query().filter();
			int idx = filter.indexOf(TENANT_FIELD);
			if (idx != -1) {
				idx += TENANT_FIELD.length() + 1;
				String tenantQuery = filter.substring(idx, filter.indexOf(')', idx));
				return Stream
						.of(tenantQuery.split(" OR "))
						.map(fieldValue ->
								configuration.getCrossClusterMapping().get(fieldValue.substring(2, fieldValue.length() - 2)))
						.toArray(String[]::new);

			}

		}

		return null;
	}
}
