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
package io.gravitee.repository.elasticsearch.healthcheck.query;

import io.gravitee.elasticsearch.client.Client;
import io.gravitee.elasticsearch.index.IndexNameGenerator;
import io.gravitee.elasticsearch.templating.freemarker.FreeMarkerComponent;
import io.gravitee.elasticsearch.version.ElasticsearchInfo;
import io.gravitee.repository.elasticsearch.healthcheck.ElasticsearchQueryCommand;
import io.gravitee.repository.healthcheck.query.Query;
import io.gravitee.repository.healthcheck.query.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract class used to execute an analytic Elasticsearch query.
 * 
 * Based on Command Design Pattern.
 *
 * @author Guillaume Waignier (Zenika)
 * @author Sebastien Devaux (Zenika)
 */
public abstract class AbstractElasticsearchQueryCommand<T extends Response> implements ElasticsearchQueryCommand<T> {

	/**
	 * Logger.
	 */
	private final Logger logger = LoggerFactory.getLogger(AbstractElasticsearchQueryCommand.class);
	
	/**
	 * Elasticsearch component to perform search request.
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
	protected ElasticsearchInfo info;

	/**
	 * Create the elasticsearch query
	 * @param templateName Freemarker template name
	 * @param query query parameter
	 * @return the elasticsearch json query
	 */
	protected String createQuery(final String templateName, final Query<T> query) {
		return this.createQuery(templateName, query, null, null);
	}

	/**
	 * Create the elasticsearch query
	 * @param templateName Freemarker template name
	 * @param query query parameter
	 * @param roundedFrom from parameter
	 * @param roundedTo to parameter
	 * @return the elasticsearch json query
	 */
	String createQuery(final String templateName, final Query<T> query, Long roundedFrom, Long roundedTo ) {
		final Map<String, Object> data = new HashMap<>();
		data.put("query", query);
		if (roundedFrom !=null) {data.put("roundedFrom", roundedFrom);}
		if (roundedTo !=null) {data.put("roundedTo", roundedTo);}

		final String request = this.freeMarkerComponent.generateFromTemplate(templateName, data);

		logger.debug("ES request {}", request);

		return request;
	}
}
