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
import io.gravitee.repository.elasticsearch.v4.log.adapter.connection.RequestV2MetricsV4Fields;
import io.gravitee.repository.log.v4.model.connection.NativeApiMetricKeys;

public class NativeApiFieldResolver implements FieldResolver {

    private static final String CONNECTION_STATUS_FIELD =
        RequestV2MetricsV4Fields.ADDITIONAL_METRICS + "." + NativeApiMetricKeys.CONNECTION_STATUS;

    /**
     * Side of the proxying path the failure sits on, as written by the gateway
     * ({@code DOWNSTREAM} / {@code UPSTREAM} / {@code INTERNAL}).
     *
     * <p>Not to be confused with the logs' {@code FAILURE_ORIGIN}, which is <em>derived</em> from the
     * error key and the connection status and speaks a user-facing vocabulary
     * ({@code CLIENT_TO_GATEWAY}…). Analytics cannot reproduce that derivation — it would need the
     * error-key classification rules inside an aggregation — so it exposes the raw stored side. On
     * native Kafka connections the error key is currently always the catch-all
     * {@code UNKNOWN_SERVER_ERROR}, which makes this field the only usable failure discriminator.
     */
    private static final String FAILURE_SIDE_FIELD = RequestV2MetricsV4Fields.ADDITIONAL_METRICS + "." + NativeApiMetricKeys.FAILURE_SIDE;

    /**
     * Kafka {@code client.id}. The only attribution axis that survives a failed connection: a
     * connection that breaks before authenticating has no application and no plan, so grouping
     * errors by application yields nothing — this is what tells you <em>who</em> is failing.
     */
    private static final String CLIENT_ID_FIELD = RequestV2MetricsV4Fields.ADDITIONAL_METRICS + "." + NativeApiMetricKeys.CLIENT_ID;

    /**
     * KIP-511 client library name. Where {@code client.id} is caller-chosen and near-unique per pod, this is a
     * handful of values across a fleet — the cardinality a breakdown actually wants.
     */
    private static final String CLIENT_SOFTWARE_NAME_FIELD =
        RequestV2MetricsV4Fields.ADDITIONAL_METRICS + "." + NativeApiMetricKeys.CLIENT_SOFTWARE_NAME;

    @Override
    public String fromMetric(Metric metric) {
        return switch (metric) {
            case NATIVE_CONNECTIONS_SUMMARY -> RequestV2MetricsV4Fields.TIMESTAMP;
            default -> throw new UnsupportedOperationException(
                "NativeApiFieldResolver supports only Metric.NATIVE_CONNECTIONS_SUMMARY but got " + metric
            );
        };
    }

    @Override
    public String fromFilter(Filter filter) {
        return switch (filter.name()) {
            case API -> RequestV2MetricsV4Fields.API_ID.v4Metrics();
            case APPLICATION -> RequestV2MetricsV4Fields.APPLICATION_ID.v4Metrics();
            case PLAN -> RequestV2MetricsV4Fields.PLAN_ID.v4Metrics();
            case NATIVE_CONNECTION_STATUS -> CONNECTION_STATUS_FIELD;
            case NATIVE_FAILURE_SIDE -> FAILURE_SIDE_FIELD;
            case NATIVE_CLIENT_ID -> CLIENT_ID_FIELD;
            case NATIVE_CLIENT_SOFTWARE_NAME -> CLIENT_SOFTWARE_NAME_FIELD;
            default -> throw new UnsupportedOperationException(
                "NativeApiFieldResolver does not support filter '" +
                    filter.name() +
                    "' — supported names: API, APPLICATION, PLAN, NATIVE_CONNECTION_STATUS, NATIVE_FAILURE_SIDE, NATIVE_CLIENT_ID, NATIVE_CLIENT_SOFTWARE_NAME"
            );
        };
    }

    /**
     * Facets must stay aligned with what {@code NATIVE_CONNECTIONS_SUMMARY} declares in
     * {@code analytics-definition.yaml}: the catalog is what {@code AnalyticsQueryValidator}
     * validates a query against, so anything it declares must resolve here or the query passes
     * validation and then blows up in this adapter. All of them target {@code keyword} fields, so a
     * {@code terms} aggregation applies directly.
     *
     * <p>Note that APPLICATION and PLAN are empty on <em>failed</em> connections — the connection
     * breaks before it authenticates — so an error breakdown must group by API, failure side or
     * client id instead.
     */
    @Override
    public String fromFacet(Facet facet) {
        return switch (facet) {
            case API -> RequestV2MetricsV4Fields.API_ID.v4Metrics();
            case APPLICATION -> RequestV2MetricsV4Fields.APPLICATION_ID.v4Metrics();
            case PLAN -> RequestV2MetricsV4Fields.PLAN_ID.v4Metrics();
            case NATIVE_CONNECTION_STATUS -> CONNECTION_STATUS_FIELD;
            case NATIVE_FAILURE_SIDE -> FAILURE_SIDE_FIELD;
            case NATIVE_CLIENT_ID -> CLIENT_ID_FIELD;
            case NATIVE_CLIENT_SOFTWARE_NAME -> CLIENT_SOFTWARE_NAME_FIELD;
            default -> throw new UnsupportedOperationException(
                "NativeApiFieldResolver does not support facet '" +
                    facet +
                    "' — supported facets: API, APPLICATION, PLAN, NATIVE_CONNECTION_STATUS, NATIVE_FAILURE_SIDE, NATIVE_CLIENT_ID, NATIVE_CLIENT_SOFTWARE_NAME"
            );
        };
    }
}
