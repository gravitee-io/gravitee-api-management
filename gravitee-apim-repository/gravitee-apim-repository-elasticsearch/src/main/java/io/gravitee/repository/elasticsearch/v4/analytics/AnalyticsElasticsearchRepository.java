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
package io.gravitee.repository.elasticsearch.v4.analytics;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.elasticsearch.utils.Type;
import io.gravitee.repository.analytics.engine.api.query.Facet;
import io.gravitee.repository.analytics.engine.api.query.FacetsQuery;
import io.gravitee.repository.analytics.engine.api.query.Filter;
import io.gravitee.repository.analytics.engine.api.query.MeasuresQuery;
import io.gravitee.repository.analytics.engine.api.query.Query;
import io.gravitee.repository.analytics.engine.api.query.TimeSeriesQuery;
import io.gravitee.repository.analytics.engine.api.result.FacetsResult;
import io.gravitee.repository.analytics.engine.api.result.MeasuresResult;
import io.gravitee.repository.analytics.engine.api.result.TimeSeriesResult;
import io.gravitee.repository.analytics.query.events.EventAnalyticsAggregate;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepository;
import io.gravitee.repository.elasticsearch.configuration.RepositoryConfiguration;
import io.gravitee.repository.elasticsearch.utils.ClusterUtils;
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.*;
import io.gravitee.repository.elasticsearch.v4.analytics.engine.adapter.*;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import io.gravitee.repository.log.v4.model.analytics.*;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Maybe;
import io.vertx.core.json.JsonObject;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.CustomLog;

@CustomLog
public class AnalyticsElasticsearchRepository extends AbstractElasticsearchRepository implements AnalyticsRepository {

    public static final String ENTRYPOINT_ID_FIELD = "entrypoint-id";
    private final String[] clusters;
    private static final String KEYWORD = "keyword";

    private static final SearchResponseStatusOverTimeAdapter searchResponseStatusOverTimeAdapter =
        new SearchResponseStatusOverTimeAdapter();

    private final HTTPMeasuresQueryAdapter httpMeasuresQueryAdapter = new HTTPMeasuresQueryAdapter();
    private final HTTPFacetsQueryAdapter httpFacetsQueryAdapter = new HTTPFacetsQueryAdapter();
    private final NativeFacetsQueryAdapter nativeFacetsQueryAdapter = new NativeFacetsQueryAdapter();
    private final HTTPTimeSeriesQueryAdapter httpTimeSeriesQueryAdapter = new HTTPTimeSeriesQueryAdapter();
    private final NativeTimeSeriesQueryAdapter nativeTimeSeriesQueryAdapter = new NativeTimeSeriesQueryAdapter();
    private final FilterAdapter messageFilterAdapter = new FilterAdapter(new MessageFieldResolver());
    private final MeasuresResponseAdapter measuresResponseAdapter = new MeasuresResponseAdapter();
    private final FacetsResponseAdapter facetsResponseAdapter = new FacetsResponseAdapter();
    private final TimeSeriesResponseAdapter timeSeriesResponseAdapter = new TimeSeriesResponseAdapter();
    private final MessageMeasuresQueryAdapter messageMeasuresQueryAdapter = new MessageMeasuresQueryAdapter();
    private final MessageFacetsQueryAdapter messageFacetsQueryAdapter = new MessageFacetsQueryAdapter();
    private final MessageTimeSeriesQueryAdapter messageTimeSeriesQueryAdapter = new MessageTimeSeriesQueryAdapter();
    private final EventMetricsMeasuresQueryAdapter eventMetricsMeasuresQueryAdapter = new EventMetricsMeasuresQueryAdapter();
    private final EventMetricsFacetsQueryAdapter eventMetricsFacetsQueryAdapter = new EventMetricsFacetsQueryAdapter();
    private final EventMetricsTimeSeriesQueryAdapter eventMetricsTimeSeriesQueryAdapter = new EventMetricsTimeSeriesQueryAdapter();
    private final AuthzMeasuresQueryAdapter authzMeasuresQueryAdapter = new AuthzMeasuresQueryAdapter();
    private final AuthzFacetsQueryAdapter authzFacetsQueryAdapter = new AuthzFacetsQueryAdapter();
    private final AuthzTimeSeriesQueryAdapter authzTimeSeriesQueryAdapter = new AuthzTimeSeriesQueryAdapter();
    private final FilterValuesQueryAdapter filterValuesQueryAdapter = new FilterValuesQueryAdapter();
    private final FilterValuesResponseAdapter filterValuesResponseAdapter = new FilterValuesResponseAdapter();

    /** Field whose presence marks a message document as carrying the connection dimensions. */
    private static final String ENRICHMENT_MARKER_FIELD = "schema-version";

    /** Aggregation naming the newest message document written before the dimensions existed. */
    private static final String WATERMARK_AGG_NAME = "newest_unenriched";

    /** The stamped dimensions, as Elasticsearch field names. Each must be a keyword to be filterable. */
    private static final List<String> CONNECTION_DIMENSION_FIELDS = List.of("plan-id", "application-id", "entrypoint-id");

    /**
     * How long the direct-path readiness of an index is reused, and the safety margin applied to its
     * watermark.
     *
     * <p>Both, deliberately: a cached watermark describes the data as it stood when it was computed,
     * so while a fleet still holds gateways that do not stamp the dimensions, the true watermark keeps
     * advancing and the cached one falls behind. Requiring a window to start later than
     * {@code watermark + TTL} rather than later than the watermark itself covers that drift for a
     * gateway reporting at least once per interval — which is the fleet-wide upgrade case this is
     * written for.
     *
     * <p>Two things it does <strong>not</strong> cover, both of which undercount rather than fail. A
     * gateway that stays silent longer than the interval and then resumes: a low-traffic API still
     * served by an old one can leave the watermark stale by its own reporting gap. And ingest lag —
     * the watermark only sees what is already indexed and searchable, so an unstamped document
     * flushed late, after a reporter buffered through an Elasticsearch outage, can land behind a
     * watermark already computed without it.
     *
     * <p>Raising this value widens the margin in the same motion, which is the lever for both: set it
     * above the reporting and buffering horizon of the slowest gateway in the fleet.
     */
    private static final Duration ENRICHMENT_WATERMARK_TTL = Duration.ofMinutes(1);

    private final Map<String, DirectPathReadiness> directPathReadiness = new ConcurrentHashMap<>();

    /**
     * @param newestUnenriched {@code null} when every message document carries the marker.
     * @param dimensionsAreKeyword false when any index behind the wildcard maps one of them otherwise,
     *     and also when the probe itself failed — either way the join is the answer.
     */
    private record DirectPathReadiness(Instant computedAt, Instant newestUnenriched, boolean dimensionsAreKeyword) {}

    public AnalyticsElasticsearchRepository(RepositoryConfiguration configuration) {
        clusters = ClusterUtils.extractClusterIndexPrefixes(configuration);
    }

    @Override
    public Optional<CountAggregate> searchRequestsCount(QueryContext queryContext, RequestsCountQuery query) {
        var indexV4 = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_METRICS, clusters);
        var indexV2 = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.REQUEST, clusters);
        var index = String.join(",", indexV4, indexV2);

        return this.client.getFieldTypes(index, ENTRYPOINT_ID_FIELD)
            .map(types -> types.stream().allMatch(KEYWORD::equals))
            .flatMap(isEntrypointIdKeyword ->
                this.client.search(index, null, SearchRequestsCountQueryAdapter.adapt(query, isEntrypointIdKeyword))
            )
            .map(SearchRequestsCountResponseAdapter::adapt)
            .blockingGet();
    }

    @Override
    public Optional<AverageAggregate> searchAverageMessagesPerRequest(QueryContext queryContext, AverageMessagesPerRequestQuery query) {
        var index = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_MESSAGE_METRICS, clusters);
        return this.client.search(index, null, SearchAverageMessagesPerRequestQueryAdapter.adapt(query))
            .map(SearchAverageMessagesPerRequestResponseAdapter::adapt)
            .blockingGet();
    }

    @Override
    public Optional<AverageAggregate> searchAverageConnectionDuration(QueryContext queryContext, AverageConnectionDurationQuery query) {
        var index = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_METRICS, clusters);
        return this.client.getFieldTypes(index, ENTRYPOINT_ID_FIELD)
            .map(types -> types.stream().allMatch(KEYWORD::equals))
            .flatMap(isEntrypointIdKeyword ->
                this.client.search(index, null, SearchAverageConnectionDurationQueryAdapter.adapt(query, isEntrypointIdKeyword))
            )
            .map(SearchAverageConnectionDurationResponseAdapter::adapt)
            .blockingGet();
    }

    @Override
    public @NonNull Optional<ResponseStatusRangesAggregate> searchResponseStatusRanges(
        QueryContext queryContext,
        ResponseStatusQueryCriteria query
    ) {
        String indices = getIndices(queryContext, query.definitionVersions());

        var adapter = new SearchResponseStatusRangesAdapter();

        return client
            .getFieldTypes(indices, ENTRYPOINT_ID_FIELD)
            .map(types -> types.stream().allMatch(KEYWORD::equals))
            .flatMap(isEntrypointIdKeyword -> client.search(indices, null, adapter.adaptQuery(query, isEntrypointIdKeyword)))
            .map(adapter::adaptResponse)
            .blockingGet();
    }

    @Override
    public @NonNull Maybe<AverageAggregate> searchResponseTimeOverTime(QueryContext queryContext, ResponseTimeRangeQuery query) {
        var adapter = new ResponseTimeRangeQueryAdapter();
        String indices = getIndices(queryContext, query.versions());
        return client.search(indices, null, adapter.queryAdapt(info, query)).flatMapMaybe(adapter::responseAdapt);
    }

    @Override
    public ResponseStatusOverTimeAggregate searchResponseStatusOvertime(QueryContext queryContext, ResponseStatusOverTimeQuery query) {
        String indices = getIndices(queryContext, query.versions());
        var esQuery = searchResponseStatusOverTimeAdapter.adaptQuery(query, info);

        log.debug("Search response status over time: {}", esQuery);
        return client.search(indices, null, esQuery).map(searchResponseStatusOverTimeAdapter::adaptResponse).blockingGet();
    }

    @Override
    public Optional<TopHitsAggregate> searchTopHitsApi(QueryContext queryContext, TopHitsQueryCriteria criteria) {
        var indexV2Request = indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.REQUEST, clusters);
        var indexV4Metrics = indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_METRICS, clusters);
        var indexes = String.join(",", List.of(indexV2Request, indexV4Metrics));

        var apiIdFields = List.of("api-id");
        var esQuery = AggregateValueCountByFieldAdapter.adaptQueryForFields(apiIdFields, criteria);

        log.debug("Search response top hit query: {}", esQuery);
        return client.search(indexes, null, esQuery).map(AggregateValueCountByFieldAdapter::adaptResponse).blockingGet();
    }

    @Override
    public RequestResponseTimeAggregate searchRequestResponseTimes(
        QueryContext queryContext,
        RequestResponseTimeQueryCriteria queryCriteria
    ) {
        String indices = getIndices(queryContext, queryCriteria.definitionVersions());
        var adapter = new SearchRequestResponseTimeAdapter();
        var esQuery = adapter.adaptQuery(queryCriteria);

        log.debug("Search request response time query: {}", esQuery);
        return client
            .search(indices, null, esQuery)
            .map(response -> adapter.adaptResponse(response, queryCriteria))
            .blockingGet();
    }

    @Override
    public Optional<TopHitsAggregate> searchTopApps(QueryContext queryContext, TopHitsQueryCriteria criteria) {
        var indexV2Request = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.REQUEST, clusters);
        var indexV4Metrics = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_METRICS, clusters);
        var indexes = String.join(",", List.of(indexV2Request, indexV4Metrics));

        var applicationIdFields = List.of("application-id");
        var esQuery = AggregateValueCountByFieldAdapter.adaptQueryForFields(applicationIdFields, criteria);

        log.debug("Search response top apps query: {}", esQuery);
        return this.client.search(indexes, null, esQuery).map(AggregateValueCountByFieldAdapter::adaptResponse).blockingGet();
    }

    @Override
    public Optional<TopFailedAggregate> searchTopFailedApis(QueryContext queryContext, TopFailedQueryCriteria criteria) {
        var indexV2Request = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.REQUEST, clusters);
        var indexV4Metrics = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_METRICS, clusters);
        var indexes = String.join(",", List.of(indexV2Request, indexV4Metrics));

        var esQuery = SearchTopFailedApisAdapter.adaptQuery(criteria);

        log.debug("Search top failed apis query: {}", esQuery);
        return this.client.search(indexes, null, esQuery).map(SearchTopFailedApisAdapter::adaptResponse).blockingGet();
    }

    @Override
    public List<HistogramAggregate> searchHistogram(QueryContext queryContext, HistogramQuery query) {
        var indexV4 = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_METRICS, clusters);
        var indexV2 = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.REQUEST, clusters);
        var index = String.join(",", indexV4, indexV2);
        var adapter = new SearchHistogramQueryAdapter();
        var esQuery = adapter.adapt(query);

        log.debug("Search histogram query: {}", esQuery);
        return client.search(index, null, esQuery).map(adapter::adaptResponse).blockingGet();
    }

    @Override
    public Optional<GroupByAggregate> searchGroupBy(QueryContext queryContext, GroupByQuery query) {
        var indexV4 = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_METRICS, clusters);
        var indexV2 = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.REQUEST, clusters);
        var index = String.join(",", indexV4, indexV2);
        var adapter = new GroupByQueryAdapter();
        var esQuery = adapter.adapt(query);

        log.debug("Search group by query: {}", esQuery);
        return client.search(index, null, esQuery).map(adapter::adaptResponse).blockingGet();
    }

    @Override
    public Optional<StatsAggregate> searchStats(QueryContext queryContext, StatsQuery query) {
        var indexV4 = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_METRICS, clusters);
        var indexV2 = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.REQUEST, clusters);
        var index = String.join(",", indexV4, indexV2);
        var adapter = new StatsQueryAdapter();
        var esQuery = adapter.adapt(query);

        log.debug("Search stats query: {}", esQuery);
        return client.search(index, null, esQuery).map(adapter::adaptResponse).blockingGet();
    }

    @Override
    public Optional<CountByAggregate> searchRequestsCountByEvent(QueryContext queryContext, RequestsCountByEventQuery query) {
        var indexV4 = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_METRICS, clusters);
        var indexV2 = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.REQUEST, clusters);
        var index = String.join(",", indexV4, indexV2);
        var esQuery = SearchRequestsCountByEventQueryAdapter.adapt(query);
        log.debug("Search Request total counts query: {}", esQuery);

        return client.search(index, null, esQuery).map(SearchRequestsCountByEventQueryAdapter::adaptResponse).blockingGet();
    }

    @Override
    public Optional<ApiMetricsDetail> findApiMetricsDetail(QueryContext queryContext, ApiMetricsDetailQuery query) {
        var indexV4Metrics = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_METRICS, clusters);
        var indexV2Request = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.REQUEST, clusters);
        var index = String.join(",", indexV4Metrics, indexV2Request);

        return this.client.search(index, null, FindApiMetricsDetailQueryAdapter.adapt(query))
            .map(FindApiMetricsDetailResponseAdapter::adaptFirst)
            .blockingGet();
    }

    @Override
    public Optional<EventAnalyticsAggregate> searchEventAnalytics(QueryContext queryContext, HistogramQuery query) {
        var index = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.EVENT_METRICS, clusters);
        var esQuery = EventMetricsQueryAdapter.toESQuery(query);

        log.debug("Search native stats query: {}", esQuery);

        return client
            .search(index, null, esQuery)
            .map(response -> EventMetricsResponseAdapter.adapt(response, query))
            .blockingGet();
    }

    @Override
    public MeasuresResult searchHTTPMeasures(QueryContext queryContext, MeasuresQuery query) {
        var index = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_METRICS, clusters);
        var esQuery = httpMeasuresQueryAdapter.adapt(query);

        log.debug("HTTP measures query: {}", esQuery);

        return client
            .search(index, null, esQuery)
            .map(response -> measuresResponseAdapter.adapt(response, query))
            .blockingGet();
    }

    @Override
    public FacetsResult searchHTTPFacets(QueryContext queryContext, FacetsQuery query) {
        var index = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_METRICS, clusters);
        var esQuery = httpFacetsQueryAdapter.adapt(query);

        log.debug("HTTP facets query: {}", esQuery);

        return client
            .search(index, null, esQuery)
            .map(response -> facetsResponseAdapter.adapt(response, query))
            .blockingGet();
    }

    @Override
    public FacetsResult searchEdgeFacets(QueryContext queryContext, FacetsQuery query) {
        var index = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_METRICS, clusters);
        var esQuery = httpFacetsQueryAdapter.adaptEdge(query);

        log.debug("Edge facets query: {}", esQuery);

        return client
            .search(index, null, esQuery)
            .map(response -> facetsResponseAdapter.adapt(response, query))
            .blockingGet();
    }

    @Override
    public FacetsResult searchNativeApiFacets(QueryContext queryContext, FacetsQuery query) {
        var index = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_METRICS, clusters);
        var esQuery = nativeFacetsQueryAdapter.adapt(query);

        log.debug("Native facets query: {}", esQuery);

        return client
            .search(index, null, esQuery)
            .map(response -> facetsResponseAdapter.adapt(response, query))
            .blockingGet();
    }

    @Override
    public TimeSeriesResult searchHTTPTimeSeries(QueryContext queryContext, TimeSeriesQuery query) {
        var index = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_METRICS, clusters);
        var esQuery = httpTimeSeriesQueryAdapter.adapt(query);

        log.debug("HTTP time series query: {}", esQuery);

        return client
            .search(index, null, esQuery)
            .map(response -> timeSeriesResponseAdapter.adapt(response, query))
            .blockingGet();
    }

    @Override
    public TimeSeriesResult searchNativeApiTimeSeries(QueryContext queryContext, TimeSeriesQuery query) {
        var index = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_METRICS, clusters);
        var esQuery = nativeTimeSeriesQueryAdapter.adapt(query);

        log.debug("Native time series query: {}", esQuery);

        return client
            .search(index, null, esQuery)
            .map(response -> timeSeriesResponseAdapter.adapt(response, query))
            .blockingGet();
    }

    @Override
    public MeasuresResult searchEventMetricsMeasures(QueryContext queryContext, MeasuresQuery query) {
        var index = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.EVENT_METRICS, clusters);
        var esQuery = eventMetricsMeasuresQueryAdapter.adapt(query);

        log.debug("Event metrics measures query: {}", esQuery);

        return client
            .search(index, null, esQuery)
            .map(response -> measuresResponseAdapter.adapt(response, query))
            .blockingGet();
    }

    @Override
    public FacetsResult searchEventMetricsFacets(QueryContext queryContext, FacetsQuery query) {
        var index = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.EVENT_METRICS, clusters);
        var esQuery = eventMetricsFacetsQueryAdapter.adapt(query);

        log.debug("Event metrics facets query: {}", esQuery);

        return client
            .search(index, null, esQuery)
            .map(response -> facetsResponseAdapter.adapt(response, query))
            .blockingGet();
    }

    @Override
    public TimeSeriesResult searchEventMetricsTimeSeries(QueryContext queryContext, TimeSeriesQuery query) {
        var index = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.EVENT_METRICS, clusters);
        var esQuery = eventMetricsTimeSeriesQueryAdapter.adapt(query);

        log.debug("Event metrics time series query: {}", esQuery);

        return client
            .search(index, null, esQuery)
            .map(response -> timeSeriesResponseAdapter.adapt(response, query))
            .blockingGet();
    }

    @Override
    public MeasuresResult searchAuthzMeasures(QueryContext queryContext, MeasuresQuery query) {
        var index = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.DECISIONS, clusters);
        var esQuery = authzMeasuresQueryAdapter.adapt(query);

        log.debug("Authz measures query: {}", esQuery);

        return client
            .search(index, null, esQuery)
            .map(response -> measuresResponseAdapter.adapt(response, query))
            .blockingGet();
    }

    @Override
    public FacetsResult searchAuthzFacets(QueryContext queryContext, FacetsQuery query) {
        var index = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.DECISIONS, clusters);
        var esQuery = authzFacetsQueryAdapter.adapt(query);

        log.debug("Authz facets query: {}", esQuery);

        return client
            .search(index, null, esQuery)
            .map(response -> facetsResponseAdapter.adapt(AuthzScopedFacetAggregation.unwrap(response, query), query))
            .blockingGet();
    }

    @Override
    public TimeSeriesResult searchAuthzTimeSeries(QueryContext queryContext, TimeSeriesQuery query) {
        var index = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.DECISIONS, clusters);
        var esQuery = authzTimeSeriesQueryAdapter.adapt(query);

        log.debug("Authz time series query: {}", esQuery);

        return client
            .search(index, null, esQuery)
            .map(response -> timeSeriesResponseAdapter.adapt(AuthzScopedFacetAggregation.unwrap(response, query), query))
            .blockingGet();
    }

    @Override
    public MeasuresResult searchMessageMeasures(QueryContext queryContext, MeasuresQuery query) {
        var httpIndex = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_METRICS, clusters);

        var messageIndex = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_MESSAGE_METRICS, clusters);

        // See FilterAdapter#isFullyAppliedOnMessages for why the join is skipped here, and for what
        // skipping it changes in the counted set.
        if (canReadMessagesDirectly(query, messageIndex, List.of())) {
            var unjoined = messageMeasuresQueryAdapter.adapt(query);
            log.debug("Message - unjoined Measures query: {}", unjoined);
            return client
                .search(messageIndex, null, unjoined)
                .map(response -> measuresResponseAdapter.adapt(response, query))
                .blockingGet();
        }

        var httpConnectionRequestIDs = searchMessageConnectionRequestIDs(query, httpIndex);

        if (httpConnectionRequestIDs.isEmpty()) {
            return measuresResponseAdapter.empty(query);
        }

        var messageQuery = messageMeasuresQueryAdapter.adapt(query, httpConnectionRequestIDs);

        log.debug("Message - Measures query: {}", messageQuery);

        return client
            .search(messageIndex, null, messageQuery)
            .map(response -> measuresResponseAdapter.adapt(response, query))
            .blockingGet();
    }

    @Override
    public FacetsResult searchMessageFacets(QueryContext queryContext, FacetsQuery query) {
        var httpIndex = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_METRICS, clusters);

        var messageIndex = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_MESSAGE_METRICS, clusters);

        // See FilterAdapter#isFullyAppliedOnMessages for why the join is skipped here, and for what
        // skipping it changes in the counted set.
        if (canReadMessagesDirectly(query, messageIndex, query.facets())) {
            var unjoined = messageFacetsQueryAdapter.adapt(query);
            log.debug("Message - unjoined Facets query: {}", unjoined);
            return client
                .search(messageIndex, null, unjoined)
                .map(response -> facetsResponseAdapter.adapt(response, query))
                .blockingGet();
        }

        var httpConnectionRequestIDs = searchMessageConnectionRequestIDs(query, httpIndex);

        if (httpConnectionRequestIDs.isEmpty()) {
            return facetsResponseAdapter.empty(query);
        }

        var messageQuery = messageFacetsQueryAdapter.adapt(query, httpConnectionRequestIDs);

        log.debug("Message - Facets query: {}", messageQuery);

        return client
            .search(messageIndex, null, messageQuery)
            .map(response -> facetsResponseAdapter.adapt(response, query))
            .blockingGet();
    }

    @Override
    public TimeSeriesResult searchMessageTimeSeries(QueryContext queryContext, TimeSeriesQuery query) {
        var httpIndex = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_METRICS, clusters);

        var messageIndex = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_MESSAGE_METRICS, clusters);

        // See FilterAdapter#isFullyAppliedOnMessages for why the join is skipped here, and for what
        // skipping it changes in the counted set.
        if (canReadMessagesDirectly(query, messageIndex, query.facets())) {
            var unjoined = messageTimeSeriesQueryAdapter.adapt(query);
            log.debug("Message - unjoined Time series query: {}", unjoined);
            return client
                .search(messageIndex, null, unjoined)
                .map(response -> timeSeriesResponseAdapter.adapt(response, query))
                .blockingGet();
        }

        var httpConnectionRequestIDs = searchMessageConnectionRequestIDs(query, httpIndex);

        if (httpConnectionRequestIDs.isEmpty()) {
            return timeSeriesResponseAdapter.empty(query);
        }

        var messageQuery = messageTimeSeriesQueryAdapter.adapt(query, httpConnectionRequestIDs);

        log.debug("Message - Time series query: {}", messageQuery);

        return client
            .search(messageIndex, null, messageQuery)
            .map(response -> timeSeriesResponseAdapter.adapt(response, query))
            .blockingGet();
    }

    /**
     * Whether this query can read the message documents on their own, without resolving the
     * connections first.
     *
     * <p>Three conditions, and the last two are transitional. Every filter has to name a field a
     * message document carries. When one of them is a dimension of the connection, the index must
     * also map that dimension as a keyword, and the window must hold no document written before the
     * gateway started stamping it. A query naming only the dimensions a message always carried —
     * operation, connector, api — needs neither check.
     *
     * <p>Anything unknown answers false and keeps the join, which works on every document whatever
     * wrote it. That is the safe direction: the join is slower, never wrong.
     */
    private boolean canReadMessagesDirectly(Query query, String messageIndex, Collection<Facet> facets) {
        if (!messageFilterAdapter.isFullyAppliedOnMessages(query)) {
            return false;
        }
        if (!readsConnectionDimension(query, facets)) {
            return true;
        }

        var readiness = directPathReadiness(messageIndex);
        if (!readiness.dimensionsAreKeyword()) {
            return false;
        }
        var watermark = readiness.newestUnenriched();
        return watermark == null || query.timeRange().from().isAfter(watermark.plus(ENRICHMENT_WATERMARK_TTL));
    }

    /** Whether this query filters or breaks down on a dimension the message documents carry only once stamped. */
    private static boolean readsConnectionDimension(Query query, Collection<Facet> facets) {
        var filtered = query
            .filters()
            .stream()
            .anyMatch(filter -> FilterAdapter.isConnectionDimension(filter.name()));
        if (filtered) {
            return true;
        }
        return facets != null && facets.stream().anyMatch(FilterAdapter::isConnectionDimension);
    }

    /**
     * What the direct path needs to be safe on this index, computed and cached as one answer.
     *
     * <p>Two questions, and both have to hold. <em>Is the dimension usable</em> — an index template
     * only applies when an index is created, so the period current at upgrade time keeps its old
     * mapping and maps the new fields dynamically, as {@code text}. A term filter on an analysed UUID
     * matches nothing, and the documents landing there do carry the marker, so the watermark alone
     * would happily open the direct path over an index that cannot answer. The same guard is applied
     * to {@code entrypoint-id} elsewhere in this class, for the same reason.
     *
     * <p><em>Is the data new enough</em> — the newest message carrying no marker, after which the
     * documents can be read on their own. Cached rather than probed per query: a probe asking "is
     * there still an unstamped document in this window" is cheap only while the answer is yes, and
     * the steady state, where nothing matches and Elasticsearch has to prove absence, would become
     * the expensive one on every query.
     *
     * <p>A failing probe answers "unusable" and is cached as such for the interval, rather than
     * propagating. A watermark is an optimisation gate: its failure must mean "take the join", never
     * "fail the request the join would have served" — and an uncached failure would be re-run by
     * every request, each paying the client timeout before falling back anyway.
     */
    private DirectPathReadiness directPathReadiness(String messageIndex) {
        var cached = directPathReadiness.get(messageIndex);
        if (cached != null && cached.computedAt().isAfter(Instant.now().minus(ENRICHMENT_WATERMARK_TTL))) {
            return cached;
        }

        // Probed outside the map on purpose, and the map only written afterwards. These are blocking
        // searches with no timeout of their own, and ConcurrentHashMap#compute holds its bin lock for
        // the whole mapping function — running them inside it would queue every message query on this
        // index behind one slow Elasticsearch call, serving them one at a time. The trade is real but
        // the right way round: every request arriving while a probe is in flight probes too, so
        // against a slow Elasticsearch that is all of them within the timeout window — but they run
        // in parallel instead of queueing, and each still falls back to the join on failure.
        DirectPathReadiness probed;
        try {
            probed = new DirectPathReadiness(Instant.now(), newestUnenrichedMessage(messageIndex), dimensionsAreKeyword(messageIndex));
        } catch (RuntimeException e) {
            // Cached as unusable rather than left absent: a failing probe that is not remembered is
            // re-run by every request, each paying the client timeout before falling back anyway.
            log.warn("Cannot establish whether {} can be read without the connection join; keeping the join", messageIndex, e);
            probed = new DirectPathReadiness(Instant.now(), null, false);
        }

        directPathReadiness.put(messageIndex, probed);
        return probed;
    }

    /** Every stamped dimension must be a keyword on every index behind the wildcard, or none of them is usable. */
    private boolean dimensionsAreKeyword(String messageIndex) {
        for (var field : CONNECTION_DIMENSION_FIELDS) {
            // Non-empty as well as all-keyword: `allMatch` is vacuously true on an empty set, which
            // would read "every index maps it correctly" as "no index maps it at all" — the one case
            // where the direct path is guaranteed to return nothing.
            var allKeyword = this.client.getFieldTypes(messageIndex, field)
                .map(types -> !types.isEmpty() && types.stream().allMatch(KEYWORD::equals))
                .blockingGet();
            if (allKeyword == null || !allKeyword) {
                log.debug("Message - {} is not a keyword on every {} index; keeping the join", field, messageIndex);
                return false;
            }
        }
        return true;
    }

    /** Newest message document carrying no marker, or {@code null} when every one of them does. */
    private Instant newestUnenrichedMessage(String messageIndex) {
        var watermarkQuery = new JsonObject()
            .put("size", 0)
            .put(
                "query",
                JsonObject.of("bool", JsonObject.of("must_not", JsonObject.of("exists", JsonObject.of("field", ENRICHMENT_MARKER_FIELD))))
            )
            .put("aggs", JsonObject.of(WATERMARK_AGG_NAME, JsonObject.of("max", JsonObject.of("field", "@timestamp"))));

        log.debug("Message - enrichment watermark query: {}", watermarkQuery);

        var response = client.search(messageIndex, null, watermarkQuery.toString()).blockingGet();

        if (response.getAggregations() == null) {
            return null;
        }
        var aggregation = response.getAggregations().get(WATERMARK_AGG_NAME);
        return aggregation == null || aggregation.getValue() == null ? null : Instant.ofEpochMilli(aggregation.getValue().longValue());
    }

    private Set<String> searchMessageConnectionRequestIDs(Query query, String httpIndex) {
        return searchMessageConnectionRequestIDs(query, httpIndex, null, new HashSet<>(), 0);
    }

    private Set<String> searchMessageConnectionRequestIDs(
        Query query,
        String httpIndex,
        JsonObject afterKey,
        Set<String> accumulatedRequestIDs,
        int iteration
    ) {
        var maxIterations = 1000;

        if (iteration >= maxIterations) {
            log.warn(
                "The limit of {} requests has been reached for message connection request IDs. This will lead to partial result.",
                maxIterations
            );
            return accumulatedRequestIDs;
        }

        var httpQuery = httpFacetsQueryAdapter.adaptRequestIDsQuery(query, afterKey);

        log.debug("Message - HTTP connexions requests query {}", httpQuery);

        var requestIDsResponse = client.search(httpIndex, null, httpQuery).blockingGet();

        var messageFacetJoin = MessageFacetExtractor.extractFromComposite(requestIDsResponse, List.of());

        if (messageFacetJoin.isEmpty()) {
            log.debug("No request IDs found in iteration {}", iteration);
            log.debug("Collected {} total message request IDs in {} iterations", accumulatedRequestIDs.size(), iteration);
            return accumulatedRequestIDs;
        }

        var requestIDs = messageFacetJoin.getRequestIDs();
        accumulatedRequestIDs.addAll(requestIDs);

        var nextAfterKey = messageFacetJoin.getAfterKey();

        if (nextAfterKey == null || nextAfterKey.isEmpty()) {
            log.debug("Collected {} total message request IDs in {} iterations", accumulatedRequestIDs.size(), iteration + 1);
            return accumulatedRequestIDs;
        }

        return searchMessageConnectionRequestIDs(query, httpIndex, nextAfterKey, accumulatedRequestIDs, iteration + 1);
    }

    @Override
    public FilterValuesResult searchFilterValues(QueryContext queryContext, FilterValuesQuery query) {
        var index = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_METRICS, clusters);
        var esQuery = filterValuesQueryAdapter.adapt(query);

        log.debug("Filter values query: {}", esQuery);

        return client.search(index, null, esQuery).map(filterValuesResponseAdapter::adapt).blockingGet();
    }

    private String getIndices(QueryContext queryContext, Collection<DefinitionVersion> definitionVersions) {
        var indexByVersion = Map.of(DefinitionVersion.V4, Type.V4_METRICS, DefinitionVersion.V2, Type.REQUEST);
        return definitionVersions
            .stream()
            .flatMap(v -> Stream.ofNullable(indexByVersion.get(v)))
            .map(v -> indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), v, clusters))
            .collect(Collectors.joining(","));
    }
}
