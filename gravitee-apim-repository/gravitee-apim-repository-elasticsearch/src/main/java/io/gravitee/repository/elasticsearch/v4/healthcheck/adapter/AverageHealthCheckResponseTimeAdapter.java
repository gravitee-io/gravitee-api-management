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
package io.gravitee.repository.elasticsearch.v4.healthcheck.adapter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.healthcheck.v4.model.AverageHealthCheckResponseTime;
import io.gravitee.repository.healthcheck.v4.model.AverageHealthCheckResponseTimeQuery;
import java.util.HashMap;
import java.util.Optional;

public class AverageHealthCheckResponseTimeAdapter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String TIME_FIELD = "@timestamp";
    private static final String PERIOD = "period";

    public String adaptQuery(AverageHealthCheckResponseTimeQuery query) {
        return json().put("size", 0).<ObjectNode>set("query", query(query)).set("aggregations", aggregations(query)).toString();
    }

    public Optional<AverageHealthCheckResponseTime> adaptResponse(SearchResponse response) {
        var group = new HashMap<String, Long>();

        var groupedBuckets = response.getAggregations().get("terms").getBuckets();
        for (var groupBucket : groupedBuckets) {
            var key = groupBucket.get("key").asText();

            var responseTimeBucketIterator = groupBucket.get("ranges").get("buckets").iterator();
            if (responseTimeBucketIterator.hasNext()) {
                var responseTimeBucket = responseTimeBucketIterator.next();
                var responseTime = responseTimeBucket.get("results").get("value");
                if (!responseTime.isNull()) {
                    group.put(key, Math.round(responseTime.asDouble()));
                }
            }
        }

        if (group.isEmpty()) {
            return Optional.empty();
        }

        var globalResponseTime = group.values().stream().mapToDouble(Long::doubleValue).average();
        if (globalResponseTime.isPresent()) {
            var average = globalResponseTime.getAsDouble();
            return Optional.of(new AverageHealthCheckResponseTime(Math.round(average), group));
        }

        return Optional.of(new AverageHealthCheckResponseTime(-1L, group));
    }

    private ObjectNode query(AverageHealthCheckResponseTimeQuery query) {
        JsonNode termFilter = json().set("term", json().put("api", query.apiId()));
        var bool = json().set("filter", array().add(termFilter));
        return json().set("bool", bool);
    }

    private ObjectNode aggregations(AverageHealthCheckResponseTimeQuery query) {
        var aggregations = json()
            .set(
                "ranges",
                json()
                    .<ObjectNode>set("aggregations", json().set("results", json().set("avg", json().put("field", "response-time"))))
                    .<ObjectNode>set(
                        "date_range",
                        json()
                            .put("field", TIME_FIELD)
                            .put("keyed", false)
                            .set(
                                "ranges",
                                array()
                                    .add(
                                        json()
                                            .put("from", query.from().toEpochMilli())
                                            .put("to", query.to().toEpochMilli())
                                            .put("key", PERIOD)
                                    )
                            )
                    )
            );
        var terms = json()
            .put("field", query.field())
            .put("size", 100)
            .set("order", array().add(json().put("_count", "desc")).add(json().put("_key", "asc")));

        return json().set("terms", json().<ObjectNode>set("aggregations", aggregations).set("terms", terms));
    }

    private ObjectNode json() {
        return MAPPER.createObjectNode();
    }

    private ArrayNode array() {
        return MAPPER.createArrayNode();
    }
}
