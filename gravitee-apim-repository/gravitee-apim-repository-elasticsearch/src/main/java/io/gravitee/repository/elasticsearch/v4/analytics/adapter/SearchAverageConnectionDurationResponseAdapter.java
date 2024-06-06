/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.elasticsearch.v4.analytics.adapter;

import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchAverageConnectionDurationQueryAdapter.AVG_ENDED_REQUEST_DURATION_MS;
import static io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchAverageMessagesPerRequestQueryAdapter.ENTRYPOINTS_AGG;

import com.google.common.util.concurrent.AtomicDouble;
import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.log.v4.model.analytics.AverageAggregate;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.util.CollectionUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchAverageConnectionDurationResponseAdapter {

    public static Optional<AverageAggregate> adapt(SearchResponse response) {
        final Map<String, Aggregation> aggregations = response.getAggregations();
        if (aggregations == null || aggregations.isEmpty()) {
            return Optional.empty();
        }
        final var entrypointsAggregation = aggregations.get(ENTRYPOINTS_AGG);
        if (entrypointsAggregation == null || CollectionUtils.isEmpty(entrypointsAggregation.getBuckets())) {
            return Optional.empty();
        }

        final var globalAverage = new AtomicDouble(0);
        final var averageConnectionDuration = entrypointsAggregation
            .getBuckets()
            .stream()
            .collect(
                Collectors.toMap(
                    jsonNode -> jsonNode.get("key").asText(),
                    jsonNode -> {
                        final double docCount = jsonNode.get(AVG_ENDED_REQUEST_DURATION_MS).get("value").asDouble();
                        globalAverage.addAndGet(docCount);
                        return docCount;
                    }
                )
            );

        return Optional.of(buildFromSource(averageConnectionDuration, globalAverage.get() / averageConnectionDuration.size()));
    }

    private static AverageAggregate buildFromSource(Map<String, Double> averageConnectionDuration, double globalAverage) {
        return AverageAggregate.builder().average(globalAverage).averageBy(averageConnectionDuration).build();
    }
}
