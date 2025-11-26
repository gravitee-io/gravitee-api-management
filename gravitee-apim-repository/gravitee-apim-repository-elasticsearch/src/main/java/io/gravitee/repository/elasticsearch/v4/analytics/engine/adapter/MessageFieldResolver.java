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

import io.gravitee.repository.analytics.engine.api.metric.Metric;
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.api.FieldResolver;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MessageFieldResolver implements FieldResolver {

    private final HTTPFieldResolver httpFieldResolver = new HTTPFieldResolver();

    public String fromMetric(Metric metric) {
        return switch (metric) {
            case MESSAGE_PAYLOAD_SIZE -> "content-length";
            case MESSAGES -> "count-increment";
            case MESSAGE_ERRORS -> "error-count-increment";
            case MESSAGE_GATEWAY_LATENCY -> "gateway-latency-ms";
            default -> throw new UnsupportedOperationException("not a message metric");
        };
    }

    @Override
    public String fromFacet(Facet facet) {
        return switch (facet) {
            case MESSAGE_CONNECTOR_ID -> "connector-id";
            case MESSAGE_CONNECTOR_TYPE -> "connector-type";
            case MESSAGE_OPERATION_TYPE -> "operation";
            default -> httpFieldResolver.fromFacet(facet);
        };
    }

    @Override
    public String fromFilter(Filter filter) {
        return switch (filter.name()) {
            case Filter.Name.MESSAGE_OPERATION_TYPE -> "operation";
            case Filter.Name.MESSAGE_CONNECTOR_ID -> "connector-id";
            case Filter.Name.MESSAGE_CONNECTOR_TYPE -> "connector-type";
            default -> httpFieldResolver.fromFilter(filter);
        };
    }
}
