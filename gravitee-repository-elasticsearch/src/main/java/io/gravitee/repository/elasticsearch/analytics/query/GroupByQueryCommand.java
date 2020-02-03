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

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.elasticsearch.utils.Type;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.analytics.query.Query;
import io.gravitee.repository.analytics.query.groupby.GroupByQuery;
import io.gravitee.repository.analytics.query.groupby.GroupByResponse;

import java.util.Iterator;

/**
 * Command used to handle GroupByQuery.
 * 
 * @author Guillaume Waignier
 * @author Sebastien Devaux
 * @author Guillaume Gillon
 *
 */
public class GroupByQueryCommand extends AbstractElasticsearchQueryCommand<GroupByResponse> {

	private final static String TEMPLATE = "groupBy.ftl";

	@Override
	public Class<? extends Query<GroupByResponse>> getSupportedQuery() {
		return GroupByQuery.class;
	}

	@Override
	public GroupByResponse executeQuery(Query<GroupByResponse> query) throws AnalyticsException {
		final GroupByQuery groupByQuery = (GroupByQuery) query;

		final String sQuery = this.createQuery(TEMPLATE, query);

		try {
			SearchResponse searchResponse = execute(groupByQuery, Type.REQUEST, sQuery).blockingGet();
			return this.toGroupByResponse(searchResponse);
		} catch (Exception eex) {
			logger.error("Impossible to perform GroupByQuery", eex);
			throw new AnalyticsException("Impossible to perform GroupByQuery", eex);
		}
	}

	private GroupByResponse toGroupByResponse(final SearchResponse response) {
		final GroupByResponse groupByresponse = new GroupByResponse();

		if (response.getAggregations() == null) {
			return groupByresponse;
		}

		final String aggregationName = response.getAggregations().keySet().iterator().next();
		final Aggregation aggregation = response.getAggregations().get(aggregationName);

		final String fieldName = aggregationName.substring(aggregationName.indexOf('_') + 1);

		groupByresponse.setField(fieldName);

		if (aggregationName.endsWith("_range")) {

			for (final JsonNode bucket : aggregation.getBuckets()) {

				final String keyAsString = bucket.get("key").asText();
				final long docCount = bucket.get("doc_count").asLong();
				GroupByResponse.Bucket value = new GroupByResponse.Bucket(keyAsString, docCount);
				groupByresponse.values().add(value);
			}

		} else if (aggregationName.startsWith("by_")) {

			for (final JsonNode bucket : aggregation.getBuckets()) {
				final JsonNode subAggregation = this.getFirstSubAggregation(bucket);
				if (subAggregation == null) {
					final String keyAsString = bucket.get("key").asText();
					final long docCount = bucket.get("doc_count").asLong();
					final GroupByResponse.Bucket value = new GroupByResponse.Bucket(keyAsString, docCount);
					groupByresponse.values().add(value);
				} else {
					final JsonNode aggValue = subAggregation.get("value");
					if (aggValue.isNumber()) {
						final String keyAsString = bucket.get("key").asText();
						GroupByResponse.Bucket value = new GroupByResponse.Bucket(keyAsString, aggValue.asLong());
						groupByresponse.values().add(value);
					}
				}
			}
		}
		return groupByresponse;

	}

	private JsonNode getFirstSubAggregation(JsonNode bucket) {
		for (final Iterator<String> it = bucket.fieldNames(); it.hasNext(); ) {
			final String fieldName = it.next();
			final JsonNode subAggregation = bucket.get(fieldName);
			if (subAggregation != null && subAggregation.get("value") != null && !subAggregation.get("value").asText().equals("null"))
				return subAggregation;
		}
		return null;
	}
}
