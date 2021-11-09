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
import io.gravitee.repository.elasticsearch.configuration.RepositoryConfiguration;
import io.gravitee.repository.elasticsearch.utils.ClusterUtils;
import io.gravitee.repository.healthcheck.query.Bucket;
import io.gravitee.repository.healthcheck.query.FieldBucket;
import io.gravitee.repository.healthcheck.query.Query;
import io.gravitee.repository.healthcheck.query.availability.AvailabilityQuery;
import io.gravitee.repository.healthcheck.query.availability.AvailabilityResponse;
import io.reactivex.Single;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Command used to handle AverageResponseTime.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AverageAvailabilityCommand extends AbstractElasticsearchQueryCommand<AvailabilityResponse> {

	/**
	 * Logger.
	 */
	private final Logger logger = LoggerFactory.getLogger(AverageAvailabilityCommand.class);

	private final static String TEMPLATE = "healthcheck/avg-availability.ftl";

	@Autowired
	protected RepositoryConfiguration configuration;

	@Override
	public Class<? extends Query<AvailabilityResponse>> getSupportedQuery() {
		return AvailabilityQuery.class;
	}

	@Override
	public AvailabilityResponse executeQuery(Query<AvailabilityResponse> query) throws AnalyticsException {
		final AvailabilityQuery availabilityQuery = (AvailabilityQuery) query;

		final String sQuery = this.createQuery(TEMPLATE, availabilityQuery);
		String[] clusters = ClusterUtils.extractClusterIndexPrefixes(availabilityQuery, configuration);

		try {
			final long now = System.currentTimeMillis();
			final long from = ZonedDateTime
					.ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault())
					.minus(1, ChronoUnit.MONTHS)
					.toInstant()
					.toEpochMilli();

			final Single<SearchResponse> result = this.client.search(
					this.indexNameGenerator.getIndexName(Type.HEALTH_CHECK, from, now, clusters),
					!info.getVersion().canUseTypeRequests() ? Type.DOC.getType() : Type.HEALTH_CHECK.getType(),
					sQuery);

			return this.toAvailabilityResponseResponse(result.blockingGet());
		} catch (Exception eex) {
			logger.error("Impossible to perform AverageResponseTimeQuery", eex);
			throw new AnalyticsException("Impossible to perform AverageResponseTimeQuery", eex);
		}
	}

	private AvailabilityResponse toAvailabilityResponseResponse(final SearchResponse response) {
		final AvailabilityResponse availabilityResponse = new AvailabilityResponse();

		if (response.getAggregations() == null) {
			availabilityResponse.setEndpointAvailabilities(Collections.emptyList());
			return availabilityResponse;
		}

		Aggregation termsAgg = response.getAggregations().get("terms");

		// Store buckets to avoid multiple unmodifiableList to be created
		List<JsonNode> endpointsBucket = termsAgg.getBuckets();
		List<FieldBucket<Double>> endpointAvailabilities = new ArrayList<>(endpointsBucket.size());
		for (JsonNode endpointBucket : endpointsBucket) {
			String endpointKey = endpointBucket.get("key").asText();
			FieldBucket<Double> endpoint = new FieldBucket<>(endpointKey);

			JsonNode dateRanges = endpointBucket.get("ranges");
			JsonNode dateRangesBucketsNode = dateRanges.get("buckets");

			List<Bucket<Double>> availabilities = new ArrayList<>(dateRangesBucketsNode.size());
			for (JsonNode dateRange : dateRangesBucketsNode) {
				String range = dateRange.get("key").asText();
				long from = dateRange.get("from").asLong();

				JsonNode results = dateRange.get("results");

				long successCount = 0;
				long failureCount = 0;

				JsonNode resultsBucketNode = results.get("buckets");
				for (JsonNode resultBucket : resultsBucketNode) {
					long docCount = resultBucket.get("doc_count").asLong();
					if (resultBucket.get("key_as_string").asBoolean()) {
						successCount = docCount;
					} else {
						failureCount = docCount;
					}
				}

				double total = successCount + failureCount;
				double percent = (total == 0) ? -1 : (successCount / total) * 100;

				Bucket<Double> availability = new Bucket<>();
				availability.setFrom(from);
				availability.setKey(range);
				availability.setValue(percent);

				availabilities.add(availability);
			}

			endpoint.setValues(availabilities);
			endpointAvailabilities.add(endpoint);
		}

		availabilityResponse.setEndpointAvailabilities(endpointAvailabilities);
		return availabilityResponse;
	}
}
