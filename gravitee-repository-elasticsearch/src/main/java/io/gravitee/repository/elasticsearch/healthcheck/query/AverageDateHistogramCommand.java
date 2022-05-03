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

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.elasticsearch.utils.Type;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.analytics.query.AggregationType;
import io.gravitee.repository.analytics.query.response.histogram.Bucket;
import io.gravitee.repository.analytics.query.response.histogram.Data;
import io.gravitee.repository.elasticsearch.configuration.RepositoryConfiguration;
import io.gravitee.repository.elasticsearch.utils.ClusterUtils;
import io.gravitee.repository.healthcheck.query.DateHistogramQuery;
import io.gravitee.repository.healthcheck.query.Query;
import io.gravitee.repository.healthcheck.query.response.histogram.DateHistogramResponse;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Command used to handle DateHistogramResponse.
 *
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AverageDateHistogramCommand extends AbstractElasticsearchQueryCommand<DateHistogramResponse> {

	/**
	 * Logger.
	 */
	private final Logger logger = LoggerFactory.getLogger(AverageDateHistogramCommand.class);

	private final static String TEMPLATE = "healthcheck/avg-date-histogram.ftl";

	@Autowired
	protected RepositoryConfiguration configuration;

	@Override
	public Class<? extends Query<DateHistogramResponse>> getSupportedQuery() {
		return DateHistogramQuery.class;
	}

	@Override
	public DateHistogramResponse executeQuery(Query<DateHistogramResponse> query) throws AnalyticsException {
		final DateHistogramQuery dateHistogramQuery = (DateHistogramQuery) query;

		try {
			final long to;
			final long from;
			if (dateHistogramQuery.timeRange().range().to() != null) {
				to = dateHistogramQuery.timeRange().range().to();
				from = dateHistogramQuery.timeRange().range().from();
			} else {
				to = System.currentTimeMillis();
				from = ZonedDateTime
						.ofInstant(Instant.ofEpochMilli(to), ZoneId.systemDefault())
						.minus(1, ChronoUnit.MONTHS)
						.toInstant()
						.toEpochMilli();
			}

			String[] clusters = ClusterUtils.extractClusterIndexPrefixes(dateHistogramQuery, configuration);

			//"from" and "to" are rounded according to the internal. It allows to exec the same request during the "interval" and make use of ES cache
			final long interval = dateHistogramQuery.timeRange().interval().toMillis();
			final long roundedFrom = (from / interval) * interval;
			final long roundedTo = ((to / interval) * interval) + interval;

			final String sQuery = this.createQuery(TEMPLATE, dateHistogramQuery, roundedFrom, roundedTo);
			final Single<SearchResponse> result = this.client.search(
					this.indexNameGenerator.getIndexName(Type.HEALTH_CHECK, from, to, clusters),
					!info.getVersion().canUseTypeRequests() ? Type.DOC.getType() : Type.HEALTH_CHECK.getType(),
					sQuery);
			return this.toAvailabilityResponseResponse(result.blockingGet(), dateHistogramQuery);
		} catch (Exception eex) {
			logger.error("Impossible to perform AverageResponseTimeQuery", eex);
			throw new AnalyticsException("Impossible to perform AverageResponseTimeQuery", eex);
		}
	}

	private DateHistogramResponse toAvailabilityResponseResponse(final SearchResponse response,
																 final DateHistogramQuery query) {
		final DateHistogramResponse dateHistogramResponse = new DateHistogramResponse();

		if (response.getAggregations() == null) {
			return dateHistogramResponse;
		}

		// Prepare data
		final Map<String, Bucket> fieldBuckets = new HashMap<>();

		final Aggregation dateHistogram = response.getAggregations().get("by_date");
		for (JsonNode dateBucket : dateHistogram.getBuckets()) {
			final long keyAsDate = dateBucket.get("key").asLong();
			dateHistogramResponse.timestamps().add(keyAsDate);

			final Iterator<String> fieldNamesInDateBucket = dateBucket.fieldNames();
			while (fieldNamesInDateBucket.hasNext()) {
				final String fieldNameInDateBucket = fieldNamesInDateBucket.next();
				this.handleSubAggregation(fieldBuckets, fieldNameInDateBucket, dateBucket, keyAsDate);
			}
		}

		if (!query.aggregations().isEmpty()) {
			query.aggregations().forEach(aggregation -> {
				String key = aggregation.type().name().toLowerCase() + '_' + aggregation.field();
				if (aggregation.type() == AggregationType.FIELD) {
					key = "by_" + aggregation.field();
				}

				dateHistogramResponse.values().add(fieldBuckets.get(key));
			});
		}
		return dateHistogramResponse;
	}

	private void handleSubAggregation(final Map<String, Bucket> fieldBuckets, final String fieldNameInDateBucket,
									  final JsonNode dateBucket, final long keyAsDate) {
		if (!fieldNameInDateBucket.startsWith("by_") && !fieldNameInDateBucket.startsWith("avg_")
				&& !fieldNameInDateBucket.startsWith("min_") && !fieldNameInDateBucket.startsWith("max_")) {
			return;
		}

		final String kindAggregation = fieldNameInDateBucket.split("_")[0];
		final String fieldName = fieldNameInDateBucket.split("_")[1];

		Bucket fieldBucket = fieldBuckets.get(fieldNameInDateBucket);
		if (fieldBucket == null) {
			fieldBucket = new Bucket(fieldNameInDateBucket, fieldName);
			fieldBuckets.put(fieldNameInDateBucket, fieldBucket);
		}

		final Map<String, List<Data>> bucketData = fieldBucket.data();
		List<Data> data;

		switch (kindAggregation) {
			case "by":
				long successCount = 0;
				long failureCount = 0;
				for (final JsonNode termBucket : dateBucket.get(fieldNameInDateBucket).get("buckets")) {
					if (termBucket.get("key").asText().equals("1")) {
						successCount = termBucket.get("doc_count").asLong();
					} else {
						failureCount = termBucket.get("doc_count").asLong();
					}
					double total = successCount + failureCount;
					double percent = (total == 0) ? 100 : (successCount / total) * 100;

					data = bucketData.computeIfAbsent(fieldNameInDateBucket, k -> new ArrayList<>());
					data.add(new Data(keyAsDate, percent));
				}
				break;
			case "min":
			case "max":
			case "avg":
				final JsonNode numericBucket = dateBucket.get(fieldNameInDateBucket);
				if (numericBucket.get("value") != null && numericBucket.get("value").isNumber()) {
					final double value = numericBucket.get("value").asDouble();
					data = bucketData.get(fieldNameInDateBucket);
					if (data == null) {
						data = new ArrayList<>();
						bucketData.put(fieldNameInDateBucket, data);
					}
					data.add(new Data(keyAsDate, (long) value));
				}
				break;
		}
	}
}
