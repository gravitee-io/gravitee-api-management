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
package io.gravitee.repository.elasticsearch.log;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.elasticsearch.model.SearchHit;
import io.gravitee.elasticsearch.model.SearchHits;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.elasticsearch.utils.Type;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.analytics.query.tabular.TabularQuery;
import io.gravitee.repository.analytics.query.tabular.TabularResponse;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepository;
import io.gravitee.repository.log.api.LogRepository;
import io.gravitee.repository.log.model.ExtendedLog;
import io.gravitee.repository.log.model.Log;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author GraviteeSource Team
 * @author Sebastien Devaux (Zenika)
 * @author Guillaume Waignier (Zenika)
 */
public class ElasticLogRepository extends AbstractElasticsearchRepository implements LogRepository {

	/**
	 * Logger.
	 */
	private final Logger logger = LoggerFactory.getLogger(ElasticLogRepository.class);

	/**
	 * Freemarker template name.
	 */
	private static final String LOG_TEMPLATE = "log/log.ftl";

	/**
	 * Freemarker template name for finding log by id.
	 */
	private static final String LOG_BY_ID_TEMPLATE = "log/logById.ftl";

	@Override
	public TabularResponse query(final TabularQuery query) throws AnalyticsException {
		final String sQuery = this.createElasticsearchJsonQuery(query);
		
		final Long from = query.timeRange().range().from();
		final Long to = query.timeRange().range().to();

		try {
			final Single<SearchResponse> result = this.client.search(
					this.indexNameGenerator.getIndexName(Type.REQUEST, from, to),
					(info.getVersion().getMajorVersion() > 6) ? Type.DOC.getType() : Type.REQUEST.getType(),
					sQuery);

			return this.toTabularResponse(result.blockingGet());
		} catch (final Exception eex) {
			logger.error("Impossible to perform log request", eex);
			throw new AnalyticsException("Impossible to perform log request", eex);
		}
	}

	/**
	 * Create JSON Elasticsearch query for the log
	 * @param query user query
	 * @return JSON Elasticsearch query
	 */
	private String createElasticsearchJsonQuery(final TabularQuery query) {
		final Map<String, Object> data = new HashMap<>();
		data.put("query", query);

		return this.freeMarkerComponent.generateFromTemplate(LOG_TEMPLATE, data);
	}

	@Override
	public ExtendedLog findById(final String requestId, final Long timestamp) throws AnalyticsException {
		final Map<String, Object> data = new HashMap<>();
		data.put("requestId", requestId);

		String sQuery = this.freeMarkerComponent.generateFromTemplate(LOG_BY_ID_TEMPLATE, data);

		try {
			Single<SearchResponse> result = this.client.search(
					(timestamp == null) ? this.indexNameGenerator.getWildcardIndexName(Type.REQUEST) : this.indexNameGenerator.getIndexName(Type.REQUEST, Instant.ofEpochMilli(timestamp)),
					(info.getVersion().getMajorVersion() > 6) ? Type.DOC.getType() : Type.REQUEST.getType(),
					sQuery);

			SearchResponse searchResponse = result.blockingGet();
			if (searchResponse.getSearchHits().getTotal().getValue() == 0) {
				throw new AnalyticsException("Request [" + requestId + "] does not exist");
			}

			SearchHit searchHit = searchResponse.getSearchHits().getHits().get(0);

			String searchHitIndex = searchHit.getIndex();
			sQuery = this.freeMarkerComponent.generateFromTemplate(LOG_BY_ID_TEMPLATE, data);

			// Search index must be updated in case of per-type index
			searchHitIndex = searchHitIndex.replaceAll(Type.REQUEST.getType(), Type.LOG.getType());

			result = this.client.search(searchHitIndex, (info.getVersion().getMajorVersion() > 6) ? Type.DOC.getType() : Type.LOG.getType(), sQuery);
			searchResponse = result.blockingGet();

			JsonNode log = null;
			if (searchResponse.getSearchHits().getTotal().getValue() != 0) {
				log = searchResponse.getSearchHits().getHits().get(0).getSource();
			}

			return LogBuilder.createExtendedLog(searchHit, log);
		} catch (Exception e) {
			logger.error("Request [{}] does not exist", requestId, e);
			throw new AnalyticsException("Request [" + requestId + "] does not exist");
		}
	}

	private TabularResponse toTabularResponse(final SearchResponse response) {
		final SearchHits hits = response.getSearchHits();
		final TabularResponse tabularResponse = new TabularResponse(hits.getTotal().getValue());
		final List<Log> logs = new ArrayList<>(hits.getHits().size());
		for (int i = 0; i < hits.getHits().size(); i++) {
			logs.add(LogBuilder.createLog(hits.getHits().get(i)));
		}
		tabularResponse.setLogs(logs);

		return tabularResponse;

	}
}
