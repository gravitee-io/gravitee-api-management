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

import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.ObservabilityEntrypoints;
import io.gravitee.repository.analytics.engine.api.query.Query;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.api.FieldResolver;
import io.gravitee.repository.elasticsearch.v4.shared.AuthzEntityRefClauses;
import io.gravitee.repository.elasticsearch.v4.shared.EntrypointScopeClause;
import io.gravitee.repository.elasticsearch.v4.shared.StatusCodeGroups;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.*;
import java.util.stream.Stream;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class FilterAdapter {

    static final String ENTRYPOINT_FIELD = EntrypointScopeClause.FIELD;

    static final List<Filter.Name> HTTP_FILTER_NAMES = List.of(
        Filter.Name.API,
        Filter.Name.APPLICATION,
        Filter.Name.PLAN,
        Filter.Name.API_PRODUCT,
        Filter.Name.GATEWAY,
        Filter.Name.HOST,
        Filter.Name.TENANT,
        Filter.Name.ZONE,
        Filter.Name.HTTP_METHOD,
        Filter.Name.HTTP_STATUS_CODE_GROUP,
        Filter.Name.HTTP_STATUS,
        Filter.Name.HTTP_PATH,
        Filter.Name.HTTP_PATH_MAPPING,
        Filter.Name.GEO_IP_CITY,
        Filter.Name.GEO_IP_REGION,
        Filter.Name.GEO_IP_COUNTRY,
        Filter.Name.GEO_IP_CONTINENT,
        Filter.Name.CONSUMER_IP,
        Filter.Name.HTTP_USER_AGENT_OS_NAME,
        Filter.Name.HTTP_USER_AGENT_DEVICE,
        Filter.Name.HTTP_ENDPOINT_RESPONSE_TIME,
        Filter.Name.HTTP_GATEWAY_LATENCY,
        Filter.Name.HTTP_GATEWAY_RESPONSE_TIME,
        Filter.Name.HTTP_REQUEST_CONTENT_LENGTH,
        Filter.Name.HTTP_RESPONSE_CONTENT_LENGTH,
        Filter.Name.LLM_PROXY_MODEL,
        Filter.Name.LLM_PROXY_PROVIDER,
        Filter.Name.MCP_PROXY_METHOD,
        Filter.Name.MCP_PROXY_TOOL,
        Filter.Name.MCP_PROXY_RESOURCE,
        Filter.Name.MCP_PROXY_PROMPT,
        Filter.Name.URI,
        Filter.Name.ENTRYPOINT
    );

    /**
     * What a message document has carried since it existed, whatever wrote it, and therefore what the
     * second phase of the join may filter on.
     *
     * <p>{@code API} belongs here even though the first phase already restricts by it: message
     * documents carry {@code api-id} of their own, so scoping the second phase to the API stops it
     * depending on the resolved request-id set being complete.
     *
     * <p>The connection dimensions are deliberately absent. They are stamped on the documents now,
     * but the join exists precisely for those written before they were, and filtering on a field such
     * a document lacks returns no hits rather than an error. They live in
     * {@link #ENRICHED_MESSAGE_FILTER_NAMES}, which only the direct path uses.
     */
    static final List<Filter.Name> MESSAGE_FILTER_NAMES = List.of(
        Filter.Name.API,
        Filter.Name.MESSAGE_CONNECTOR_TYPE,
        Filter.Name.MESSAGE_CONNECTOR_ID,
        Filter.Name.MESSAGE_OPERATION_TYPE,
        Filter.Name.MESSAGE_COUNT,
        Filter.Name.MESSAGE_SIZE,
        Filter.Name.MESSAGE_ERROR_COUNT
    );

    /**
     * Dimensions of the connection, carried on a message document only since the gateway began
     * stamping them.
     *
     * <p>Separate from {@link #MESSAGE_FILTER_NAMES} because the two are needed at different moments.
     * A query naming one of these <em>can</em> be answered from the message documents alone — that is
     * the point of stamping them — but only for documents a gateway wrote after it started doing so.
     * While the join still runs for older data, its message phase must not filter on them: those
     * documents do not carry the field, and Elasticsearch answers a term query on a missing field
     * with no hits rather than an error, which would silently return nothing.
     */
    public static final List<Filter.Name> CONNECTION_DIMENSION_FILTER_NAMES = List.of(
        Filter.Name.PLAN,
        Filter.Name.APPLICATION,
        Filter.Name.ENTRYPOINT
    );

    /**
     * Whether this dimension is one the message documents carry only since the gateway started
     * stamping them.
     *
     * <p>Exposed so the repository asks this list rather than keeping its own copy: it is the
     * repository that decides, per query window, whether the data is new enough to be read without
     * the join, and a second list drifting from this one would open that path for a dimension the
     * documents do not carry — a term filter on a missing field, which Elasticsearch answers with no
     * hits rather than an error.
     */
    public static boolean isConnectionDimension(Filter.Name name) {
        return CONNECTION_DIMENSION_FILTER_NAMES.contains(name);
    }

    /** The facet form of {@link #isConnectionDimension(Filter.Name)}; not every dimension has one. */
    public static boolean isConnectionDimension(Facet facet) {
        return CONNECTION_DIMENSION_FILTER_NAMES.stream().anyMatch(name -> name.name().equals(facet.name()));
    }

    /** Everything a message document carries once the gateway stamps the connection dimensions. */
    static final List<Filter.Name> ENRICHED_MESSAGE_FILTER_NAMES = Stream.concat(
        MESSAGE_FILTER_NAMES.stream(),
        CONNECTION_DIMENSION_FILTER_NAMES.stream()
    ).toList();

    static final List<Filter.Name> NATIVE_FILTER_NAMES = List.of(
        Filter.Name.API,
        Filter.Name.APPLICATION,
        Filter.Name.PLAN,
        Filter.Name.NATIVE_CONNECTION_STATUS,
        Filter.Name.NATIVE_FAILURE_SIDE,
        Filter.Name.NATIVE_CLIENT_ID,
        Filter.Name.NATIVE_CLIENT_SOFTWARE_NAME
    );

    /**
     * Native Kafka event metrics. Deliberately narrow: the `event-metrics` documents only carry the
     * routing dimensions plus `topic` / `operation`. Anything else reaching this family — an explicit
     * `ENTRYPOINT` condition, for instance — is dropped by the allow-list rather than failing the
     * query, matching how the other families behave.
     */
    static final List<Filter.Name> EVENT_METRICS_FILTER_NAMES = List.of(
        Filter.Name.API,
        Filter.Name.APPLICATION,
        Filter.Name.PLAN,
        Filter.Name.NATIVE_TOPIC,
        Filter.Name.NATIVE_OPERATION
    );

    static final List<Filter.Name> EDGE_FILTER_NAMES = List.of(
        Filter.Name.API,
        Filter.Name.GATEWAY,
        Filter.Name.TENANT,
        Filter.Name.ZONE,
        Filter.Name.EDGE_TYPE,
        Filter.Name.EDGE_PROVIDER,
        Filter.Name.EDGE_PROCESS,
        Filter.Name.EDGE_CLIENT,
        Filter.Name.EDGE_VERSION,
        Filter.Name.EDGE_MODEL,
        Filter.Name.EDGE_TOOL
    );

    static final List<Filter.Name> AUTHZ_FILTER_NAMES = List.of(
        Filter.Name.API,
        Filter.Name.GATEWAY,
        Filter.Name.AUTHZ_DECISION,
        Filter.Name.AUTHZ_OPERATION,
        Filter.Name.AUTHZ_STATUS,
        Filter.Name.AUTHZ_CALLER,
        Filter.Name.AUTHZ_SUBJECT_ID,
        Filter.Name.AUTHZ_ACTION,
        Filter.Name.AUTHZ_RESOURCE_ID,
        Filter.Name.AUTHZ_REASON,
        Filter.Name.AUTHZ_PDP
    );

    private final FieldResolver fieldResolver;

    public FilterAdapter(FieldResolver fieldResolver) {
        this.fieldResolver = fieldResolver;
    }

    /**
     * The message-side filter for the direct path, where the connection dimensions are readable off
     * the documents themselves. {@link #adaptForMessage(Query)} stays the join's message phase.
     */
    public JsonArray adaptForEnrichedMessage(Query query) {
        var jsonFilters = JsonArray.of(TimeRangeAdapter.adapt(query));
        for (var filter : query.filters()) {
            if (shouldAdaptForEnrichedMessage(filter)) {
                jsonFilters.add(filter(filter));
            }
        }
        return jsonFilters;
    }

    public JsonArray adaptForMessage(Query query) {
        var jsonFilters = JsonArray.of(TimeRangeAdapter.adapt(query));
        for (var filter : query.filters()) {
            if (shouldAdaptForMessage(filter)) {
                jsonFilters.add(filter(filter));
            }
        }
        return jsonFilters;
    }

    public JsonArray adaptForHTTP(Query query) {
        var jsonFilters = JsonArray.of(TimeRangeAdapter.adapt(query));
        boolean hasEntrypointFilter = false;
        for (var filter : query.filters()) {
            if (shouldAdaptForHTTP(filter)) {
                jsonFilters.add(filter(filter));
                if (filter.name() == Filter.Name.ENTRYPOINT) {
                    hasEntrypointFilter = true;
                }
            }
        }
        if (!hasEntrypointFilter) {
            jsonFilters.add(defaultHttpEntrypointScope());
        }
        return jsonFilters;
    }

    /**
     * Selects the connection documents whose messages a message query aggregates over.
     *
     * <p>Adds no entrypoint predicate of its own. A condition the caller wrote is still applied — it reaches
     * this family through {@link #HTTP_FILTER_NAMES} — but the default scope never does. Which API a connection
     * belongs to is already
     * expressed by the {@code API} filter every query carries — {@code ApiTypeFilterTransformer}
     * appends one unconditionally — and the second phase keeps only the request ids that have
     * message documents, an index no other api type writes to. An entrypoint predicate would
     * restate that guess in terms the entrypoint ids cannot support: they name plugins, and
     * {@code http-get} / {@code http-post} serve Message APIs and LLM/MCP proxies alike.
     *
     * <p>This used to be the negation of the default HTTP entrypoint scope, which read as "not a plain HTTP
     * proxy" when that scope held a single id. Every later widening of that list silently narrowed
     * this one, and GMA-513 — adding http-get and http-post to fix LLM/MCP dashboards — made every
     * Message API exposed over them invisible to message analytics.
     */
    public JsonArray adaptForMessageConnexion(Query query) {
        var jsonFilters = JsonArray.of(TimeRangeAdapter.adapt(query));
        for (var filter : query.filters()) {
            if (shouldAdaptForMessageConnexion(filter)) {
                jsonFilters.add(filter(filter));
            }
        }
        return jsonFilters;
    }

    public JsonArray adaptForNative(Query query) {
        var jsonFilters = JsonArray.of(TimeRangeAdapter.adapt(query));
        for (var filter : query.filters()) {
            if (shouldAdaptForNative(filter)) {
                jsonFilters.add(filter(filter));
            }
        }
        return jsonFilters;
    }

    public JsonArray adaptForEventMetrics(Query query) {
        var jsonFilters = JsonArray.of(TimeRangeAdapter.adapt(query));
        for (var filter : query.filters()) {
            if (shouldAdaptForEventMetrics(filter)) {
                jsonFilters.add(filter(filter));
            }
        }
        return jsonFilters;
    }

    public JsonArray adaptForEdge(Query query) {
        var jsonFilters = JsonArray.of(TimeRangeAdapter.adapt(query));
        for (var filter : query.filters()) {
            if (shouldAdaptForEdge(filter)) {
                jsonFilters.add(filter(filter));
            }
        }
        return jsonFilters.add(edgeFilter());
    }

    public JsonArray adaptForAuthz(Query query) {
        var jsonFilters = JsonArray.of(TimeRangeAdapter.adapt(query));
        var entityRefs = new AuthzEntityRefClauses();
        for (var filter : query.filters()) {
            if (shouldAdaptForAuthz(filter)) {
                jsonFilters.add(filter(filter, entityRefs));
            }
        }
        return jsonFilters
            .add(term(AuthzFieldResolver.DECISION_POINT_TYPE_FIELD, AuthzFieldResolver.DECISION_POINT_TYPE_AUTHZ))
            .add(term(AuthzFieldResolver.PHASE_FIELD, AuthzFieldResolver.PHASE_RESOLVED));
    }

    private static JsonObject term(String field, String value) {
        return JsonObject.of("term", JsonObject.of(field, value));
    }

    public boolean shouldAdaptForAuthz(Filter filter) {
        return AUTHZ_FILTER_NAMES.contains(filter.name());
    }

    public boolean shouldAdaptForHTTP(Filter filter) {
        return HTTP_FILTER_NAMES.contains(filter.name());
    }

    public boolean shouldAdaptForMessage(Filter filter) {
        return MESSAGE_FILTER_NAMES.contains(filter.name());
    }

    public boolean shouldAdaptForEnrichedMessage(Filter filter) {
        return ENRICHED_MESSAGE_FILTER_NAMES.contains(filter.name());
    }

    /**
     * Whether every filter of this query reads a field the message documents carry themselves.
     *
     * <p>When it does, the connection phase of the message join has nothing left to contribute: it
     * would resolve request ids only to re-express a restriction the message query already applies.
     * Skipping it also removes the ceiling that phase carries — the ids are collected 10k at a time,
     * up to a thousand pages, into a single {@code terms} clause, and Elasticsearch refuses the whole
     * search past {@code index.max_terms_count} (65,536 by default).
     *
     * <p>This answers for the enriched shape alone. Plan, application and entrypoint are stamped on the
     * message documents now, so a query naming one qualifies here — but only the repository knows
     * whether the data in a given window actually carries them, and it keeps the join when it does
     * not.
     *
     * <p><strong>The connection phase was never purely a filter restatement, and skipping it changes
     * what is counted.</strong> {@link #adaptForMessageConnexion(Query)} opens with the query's time
     * range, so the join also required the connection document to exist <em>and</em> to fall inside
     * the window, while the message phase applies its own window to the message timestamp. Two
     * classes of message become visible once the join is skipped:
     *
     * <ul>
     *   <li><em>Straddling connections</em> — a stream opened before {@code from} and still running.
     *       For SSE, WebSocket or {@code http-get} against a short dashboard window that is the
     *       normal case, not an edge case.</li>
     *   <li><em>Orphaned messages</em> — no connection document at all: sampling, retention, index
     *       rollover.</li>
     * </ul>
     *
     * <p>Both were silently dropped before and are counted now. This is held to be a correction
     * rather than a regression — a message that flowed inside the window did flow inside the window,
     * whatever became of the document describing its connection — but it does move the numbers
     * operators read, and it makes a board internally inconsistent in one direction: adding a plan
     * filter restores the join, so the count drops for a reason unrelated to the plan. Pinned by
     * {@code AnalyticsElasticsearchRepositoryTest}'s straddling-connection case.
     */
    public boolean isFullyAppliedOnMessages(Query query) {
        return query.filters().stream().allMatch(this::shouldAdaptForEnrichedMessage);
    }

    public boolean shouldAdaptForMessageConnexion(Filter filter) {
        return HTTP_FILTER_NAMES.contains(filter.name());
    }

    public boolean shouldAdaptForNative(Filter filter) {
        return NATIVE_FILTER_NAMES.contains(filter.name());
    }

    public boolean shouldAdaptForEventMetrics(Filter filter) {
        return EVENT_METRICS_FILTER_NAMES.contains(filter.name());
    }

    public boolean shouldAdaptForEdge(Filter filter) {
        return EDGE_FILTER_NAMES.contains(filter.name());
    }

    /**
     * The scope an HTTP query gets when it carries no {@code ENTRYPOINT} condition: every entrypoint the
     * registry does not declare outside the HTTP scope, documents without an entrypoint id included (requests
     * refused before an entrypoint was selected, on gateways predating report-time attribution).
     */
    JsonObject defaultHttpEntrypointScope() {
        return EntrypointScopeClause.analyticsDefault();
    }

    /*
     * Add a bool step to the aggregation pipeline so that filters defined
     * at the metric level are applied to the aggregation results.
     */
    public JsonObject adaptMetricFilters(List<Filter> filters) {
        var must = new JsonArray();
        for (var f : filters) {
            must.add(filter(f));
        }
        return JsonObject.of("bool", JsonObject.of("must", must));
    }

    public JsonObject edgeFilter() {
        return JsonObject.of("term", JsonObject.of(ENTRYPOINT_FIELD, ObservabilityEntrypoints.EDGE.id()));
    }

    private JsonObject filter(Filter filter) {
        return filter(filter, AuthzEntityRefClauses.bareIdsOnly());
    }

    private JsonObject filter(Filter filter, AuthzEntityRefClauses entityRefs) {
        if (filter.name() == Filter.Name.HTTP_STATUS_CODE_GROUP) {
            return statusCodeGroupFilter(filter);
        }
        if (
            filter.name() == Filter.Name.ENTRYPOINT && (filter.operator() == Filter.Operator.EQ || filter.operator() == Filter.Operator.IN)
        ) {
            return entrypointFilter(filter);
        }
        if (
            (filter.name() == Filter.Name.AUTHZ_SUBJECT_ID || filter.name() == Filter.Name.AUTHZ_RESOURCE_ID) &&
            (filter.operator() == Filter.Operator.EQ || filter.operator() == Filter.Operator.IN)
        ) {
            return entityRefFilter(filter, entityRefs);
        }
        if (filter.operator() == Filter.Operator.GTE || filter.operator() == Filter.Operator.LTE) {
            return rangeFilter(filter);
        }
        return JsonObject.of(filterName(filter), filterValue(filter));
    }

    /**
     * An explicit entrypoint condition is exact: it selects the given values and nothing else. The value is read
     * as tolerantly as the generic path it replaces — a caller may send a single id under {@code IN} — so a
     * shape the query builders used to accept still yields a query rather than an error.
     */
    private static JsonObject entrypointFilter(Filter filter) {
        return EntrypointScopeClause.exactly(stringValues(filter.value()));
    }

    private JsonObject entityRefFilter(Filter filter, AuthzEntityRefClauses entityRefs) {
        return entityRefs.matching(
            fieldResolver.entityTypeFromFilter(filter),
            fieldResolver.fromFilter(filter),
            stringValues(filter.value())
        );
    }

    private static List<String> stringValues(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Collection<?> values) {
            return values.stream().filter(Objects::nonNull).map(String::valueOf).toList();
        }
        return List.of(String.valueOf(value));
    }

    private JsonObject rangeFilter(Filter filter) {
        var field = fieldResolver.fromFilter(filter);
        var bound = filter.operator() == Filter.Operator.GTE ? "gte" : "lte";
        return JsonObject.of("range", JsonObject.of(field, JsonObject.of(bound, filter.value())));
    }

    @SuppressWarnings("unchecked")
    private JsonObject statusCodeGroupFilter(Filter filter) {
        var field = fieldResolver.fromFilter(filter);
        return switch (filter.operator()) {
            case EQ -> StatusCodeGroups.rangeForGroup(field, (String) filter.value());
            case IN -> StatusCodeGroups.shouldForGroups(field, (Collection<String>) filter.value());
            default -> throw new IllegalArgumentException("Unsupported operator for HTTP_STATUS_CODE_GROUP filter: " + filter.operator());
        };
    }

    private String filterName(Filter filter) {
        return switch (filter.operator()) {
            case EQ -> "term";
            case Filter.Operator.IN -> "terms";
            case Filter.Operator.GTE, Filter.Operator.LTE -> throw new IllegalStateException("GTE/LTE should be handled by rangeFilter()");
        };
    }

    private JsonObject filterValue(Filter filter) {
        return switch (filter.operator()) {
            case EQ -> JsonObject.of(fieldResolver.fromFilter(filter), filter.value());
            case Filter.Operator.IN -> JsonObject.of(fieldResolver.fromFilter(filter), listValue(filter.value()));
            case Filter.Operator.GTE, Filter.Operator.LTE -> throw new IllegalStateException("GTE/LTE should be handled by rangeFilter()");
        };
    }

    private JsonArray listValue(Object value) {
        if (Objects.requireNonNull(value) instanceof Collection<?> l) {
            return new JsonArray(new ArrayList<>(l));
        }
        return JsonArray.of(value);
    }
}
