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

    /**
     * Only the dimensions a message document actually carries.
     *
     * <p>Deliberately not falling through to the HTTP resolver for the rest. It would answer with a
     * field of the connection document, absent from this index, and Elasticsearch does not fail a
     * terms aggregation on a field it cannot find: it returns no buckets. A breakdown that cannot
     * work would render as an empty chart rather than an error, which is the harder failure to
     * diagnose. Failing here keeps a drift between the catalog and this resolver visible.
     *
     * <p>{@code API} and {@code GATEWAY} do fall through, and are meant to: {@code api-id} and
     * {@code gateway} are stamped on every message document.
     *
     * <p>Plan and application stay refused here even though the message documents now carry them, and
     * the catalog is why it costs nothing: it declares neither as a facet of the message metrics, so
     * the validator rejects such a query before it reaches this resolver. Answering them would only
     * take effect on the path that must not have them — the join, which exists precisely for the
     * documents written before the fields did, and which builds its buckets through this same
     * resolver. A breakdown there would aggregate on a field those documents lack and render an empty
     * chart. They belong here once the join is deleted, not before.
     *
     * <p>Entrypoint is absent here on purpose: the catalog declares it as a filter, not a facet, so
     * a query can restrict by it but never break down on it. Filtering resolves through
     * {@link #fromFilter}.
     */
    @Override
    public String fromFacet(Facet facet) {
        return switch (facet) {
            case MESSAGE_CONNECTOR_ID -> "connector-id";
            case MESSAGE_CONNECTOR_TYPE -> "connector-type";
            case MESSAGE_OPERATION_TYPE -> "operation";
            case API, GATEWAY -> httpFieldResolver.fromFacet(facet);
            default -> throw new UnsupportedOperationException(
                "Message documents carry no %s dimension; it lives on the connection document".formatted(facet)
            );
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
