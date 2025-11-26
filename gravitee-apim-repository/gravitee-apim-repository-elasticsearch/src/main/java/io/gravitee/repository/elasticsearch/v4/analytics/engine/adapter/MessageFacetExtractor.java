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
package io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter;

import io.gravitee.elasticsearch.model.Aggregation;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.vertx.core.json.JsonObject;
import java.util.HashSet;
import java.util.List;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MessageFacetExtractor {

    static final String REQUEST_ID_AGG_NAME = "_REQUEST_ID";

    public static MessageFacetJoin extractFromComposite(SearchResponse response, List<Facet> joinFacets) {
        if (response == null || response.getAggregations() == null || response.getAggregations().isEmpty()) {
            return MessageFacetJoin.empty();
        }

        var aggregations = response.getAggregations();
        var requestIdAgg = aggregations.get(REQUEST_ID_AGG_NAME);

        if (requestIdAgg == null) {
            return MessageFacetJoin.empty();
        }

        var requestIDs = new HashSet<String>();

        var buckets = requestIdAgg.getBuckets();
        if (buckets != null && !buckets.isEmpty()) {
            for (var bucket : buckets) {
                var keyNode = bucket.get("key");
                if (keyNode != null && keyNode.has("request-id")) {
                    var requestId = keyNode.get("request-id").asText();
                    if (requestId != null && !requestId.isEmpty()) {
                        requestIDs.add(requestId);
                    }
                }
            }
        }

        return MessageFacetJoin.fromComposite(requestIDs, extractAfterKey(requestIdAgg));
    }

    private static JsonObject extractAfterKey(Aggregation requestIdAgg) {
        var afterKeyNode = requestIdAgg.getAfterKey();
        if (afterKeyNode != null && !afterKeyNode.isNull() && afterKeyNode.isObject()) {
            return new JsonObject(afterKeyNode.toString());
        }
        return null;
    }
}
