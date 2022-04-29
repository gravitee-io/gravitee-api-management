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
import io.gravitee.repository.healthcheck.query.responsetime.AverageResponseTimeQuery;
import io.gravitee.repository.healthcheck.query.responsetime.AverageResponseTimeResponse;
import io.reactivex.Single;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Command used to handle AverageResponseTime.
 *
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class AverageResponseTimeCommand extends AbstractElasticsearchQueryCommand<AverageResponseTimeResponse> {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(AverageResponseTimeCommand.class);

    private static final String TEMPLATE = "healthcheck/avg-response-time.ftl";

    @Autowired
    protected RepositoryConfiguration configuration;

    @Override
    public Class<? extends Query<AverageResponseTimeResponse>> getSupportedQuery() {
        return AverageResponseTimeQuery.class;
    }

    @Override
    public AverageResponseTimeResponse executeQuery(Query<AverageResponseTimeResponse> query) throws AnalyticsException {
        final AverageResponseTimeQuery averageResponseTimeQuery = (AverageResponseTimeQuery) query;

        final String sQuery = this.createQuery(TEMPLATE, averageResponseTimeQuery);
        String[] clusters = ClusterUtils.extractClusterIndexPrefixes(averageResponseTimeQuery, configuration);

        try {
            final long now = System.currentTimeMillis();
            final long from = ZonedDateTime
                .ofInstant(Instant.ofEpochMilli(now), ZoneId.systemDefault())
                .minus(1, ChronoUnit.MONTHS)
                .toInstant()
                .toEpochMilli();

            final Single<SearchResponse> result =
                this.client.search(
                        this.indexNameGenerator.getIndexName(Type.HEALTH_CHECK, from, now, clusters),
                        !info.getVersion().canUseTypeRequests() ? Type.DOC.getType() : Type.HEALTH_CHECK.getType(),
                        sQuery
                    );
            return this.toAverageResponseTimeResponse(result.blockingGet());
        } catch (Exception eex) {
            logger.error("Impossible to perform AverageResponseTimeQuery", eex);
            throw new AnalyticsException("Impossible to perform AverageResponseTimeQuery", eex);
        }
    }

    private AverageResponseTimeResponse toAverageResponseTimeResponse(final SearchResponse response) {
        final AverageResponseTimeResponse averageResponseTime = new AverageResponseTimeResponse();

        if (response.getAggregations() == null) {
            averageResponseTime.setEndpointResponseTimes(Collections.emptyList());
            return averageResponseTime;
        }

        Aggregation termsAgg = response.getAggregations().get("terms");

        // Store buckets to avoid multiple unmodifiableList to be created
        List<JsonNode> endpointsBucket = termsAgg.getBuckets();
        List<FieldBucket<Long>> endpointsResponseTimes = new ArrayList<>(endpointsBucket.size());
        for (JsonNode endpointBucket : endpointsBucket) {
            String endpointKey = endpointBucket.get("key").asText();
            FieldBucket<Long> endpoint = new FieldBucket<>(endpointKey);

            JsonNode dateRanges = endpointBucket.get("ranges");
            JsonNode dateRangesBucketsNode = dateRanges.get("buckets");
            List<Bucket<Long>> responseTimes = new ArrayList<>(dateRangesBucketsNode.size());

            for (JsonNode dateRange : dateRangesBucketsNode) {
                Bucket<Long> responseTime = new Bucket<>();
                responseTime.setFrom(dateRange.get("from").asLong());
                responseTime.setKey(dateRange.get("key").asText());
                final JsonNode value = dateRange.get("results").get("value");
                if (value.isNull()) {
                    responseTime.setValue(-1L);
                } else {
                    responseTime.setValue(value.asLong());
                }

                responseTimes.add(responseTime);
            }

            // If all response times are equals to 0, do not include this bucket
            boolean include = responseTimes.stream().anyMatch(longBucket -> longBucket.getValue() != 0L);

            if (include) {
                endpoint.setValues(responseTimes);
                endpointsResponseTimes.add(endpoint);
            }
        }

        averageResponseTime.setEndpointResponseTimes(endpointsResponseTimes);
        return averageResponseTime;
    }
}
