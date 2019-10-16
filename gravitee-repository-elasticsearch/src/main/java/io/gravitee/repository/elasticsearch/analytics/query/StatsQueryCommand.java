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

import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.elasticsearch.utils.Type;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.analytics.query.Query;
import io.gravitee.repository.analytics.query.stats.StatsQuery;
import io.gravitee.repository.analytics.query.stats.StatsResponse;

/**
 * Commmand used to handle StatsQuery
 *
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 *
 */
public class StatsQueryCommand extends AbstractElasticsearchQueryCommand<StatsResponse> {

	private final static String TEMPLATE = "stats.ftl";

	@Override
	public Class<? extends Query<StatsResponse>> getSupportedQuery() {
		return StatsQuery.class;
	}

	@Override
	public StatsResponse executeQuery(Query<StatsResponse> query) throws AnalyticsException {
		final StatsQuery statsQuery = (StatsQuery) query;
		final String sQuery = this.createQuery(TEMPLATE, query);
		
		try {
			SearchResponse searchResponse = execute(statsQuery, Type.REQUEST, sQuery).blockingGet();
			return this.toStatsResponse(searchResponse);
		} catch (final Exception eex) {
			logger.error("Impossible to perform StatsQuery", eex);
			throw new AnalyticsException("Impossible to perform StatsQuery", eex);
		}
	}

	private StatsResponse toStatsResponse(final SearchResponse response) {
		final StatsResponse statsResponse = new StatsResponse();
		if (response.getAggregations() != null && !response.getAggregations().isEmpty()) {
			final Aggregation aggregation = response.getAggregations().entrySet().iterator().next().getValue();
			statsResponse.setAvg(aggregation.getAvg());
			statsResponse.setCount(aggregation.getCount());
			statsResponse.setMax(aggregation.getMax());
			statsResponse.setMin(aggregation.getMin());
			statsResponse.setSum(aggregation.getSum());
		}
		return statsResponse;
	}
}
