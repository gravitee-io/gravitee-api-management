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
import io.gravitee.common.data.domain.Page;
import io.gravitee.elasticsearch.model.SearchHit;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.healthcheck.v4.model.HealthCheckLog;
import io.gravitee.repository.healthcheck.v4.model.HealthCheckLogQuery;
import io.gravitee.repository.management.api.search.Pageable;
import io.reactivex.rxjava3.core.Maybe;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class HealthCheckLogAdapter implements QueryResponseAdapter<HealthCheckLogQuery, Page<HealthCheckLog>> {

    private static final String TIME_FIELD = "@timestamp";
    private final Pageable pageable;

    public JsonNode adaptQuery(HealthCheckLogQuery query) {
        int size = query.pageable().pageSize();
        int from = (query.pageable().pageNumber() - 1) * query.pageable().pageSize();

        return json()
            .put("size", size)
            .put("from", from)
            .<ObjectNode>set("query", query(query))
            .set("sort", array().add(json().set(TIME_FIELD, json().put("order", "asc"))));
    }

    @Override
    public Maybe<Page<HealthCheckLog>> adaptResponse(SearchResponse response) {
        var total = response.getSearchHits().getTotal().getValue();
        var pageSize = response.getSearchHits().getHits().size();
        var content = response.getSearchHits().getHits().stream().map(this::toHealthCheckLog).toList();

        return Maybe.just(new Page<>(content, pageable.pageNumber(), pageSize, total));
    }

    private ObjectNode query(HealthCheckLogQuery query) {
        var termFilter = json().set("term", json().put("api", query.apiId()));
        var rangeFilter = json()
            .set(
                "range",
                json()
                    .set(
                        TIME_FIELD,
                        json()
                            .put("from", query.from().toEpochMilli())
                            .put("to", query.to().toEpochMilli())
                            .put("include_lower", true)
                            .put("include_upper", true)
                    )
            );

        var mustTerm = json();
        query.success().ifPresent(success -> mustTerm.put("success", success));

        var bool = json().<ObjectNode>set("filter", array().add(termFilter).add(rangeFilter));
        if (!mustTerm.isEmpty()) {
            bool.set("must", json().set("term", mustTerm));
        }

        return json().set("bool", bool);
    }

    private HealthCheckLog toHealthCheckLog(SearchHit hit) {
        var steps = StreamSupport
            .stream(((Iterable<JsonNode>) () -> hit.getSource().get("steps").elements()).spliterator(), false)
            .map(this::toStep)
            .toList();

        return HealthCheckLog
            .builder()
            .id(hit.getId())
            .timestamp(Instant.parse(hit.getSource().get(TIME_FIELD).asText()).truncatedTo(ChronoUnit.SECONDS))
            .apiId(toText(hit.getSource().get("api")))
            .endpointName(toText(hit.getSource().get("endpoint")))
            .gatewayId(toText(hit.getSource().get("gateway")))
            .responseTime(hit.getSource().get("response-time").asLong())
            .success(hit.getSource().get("success").asBoolean())
            .steps(steps)
            .build();
    }

    private HealthCheckLog.Step toStep(JsonNode node) {
        return new HealthCheckLog.Step(
            toText(node.get("name")),
            node.get("success").asBoolean(),
            toText(node.get("message")),
            new HealthCheckLog.Request(
                toText(node.get("request").get("uri")),
                toText(node.get("request").get("method")),
                toMap(node.get("request").get("headers"))
            ),
            new HealthCheckLog.Response(
                node.get("response").get("status").asInt(),
                toText(node.get("response").get("body")),
                toMap(node.get("response").get("headers"))
            )
        );
    }

    private String toText(JsonNode node) {
        return node != null ? node.asText() : null;
    }

    private Map<String, String> toMap(JsonNode node) {
        var map = new HashMap<String, String>();
        if (node != null) {
            node.fields().forEachRemaining(entry -> map.put(entry.getKey(), entry.getValue().asText()));
        }
        return map;
    }
}
