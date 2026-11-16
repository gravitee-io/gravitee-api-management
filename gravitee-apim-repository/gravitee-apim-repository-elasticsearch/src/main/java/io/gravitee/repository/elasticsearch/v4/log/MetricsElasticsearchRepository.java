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
package io.gravitee.repository.elasticsearch.v4.log;

import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.elasticsearch.model.SearchHits;
import io.gravitee.elasticsearch.model.TotalHits;
import io.gravitee.elasticsearch.utils.Type;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepository;
import io.gravitee.repository.elasticsearch.configuration.RepositoryConfiguration;
import io.gravitee.repository.elasticsearch.utils.ClusterUtils;
import io.gravitee.repository.elasticsearch.v4.log.adapter.authz.FindAuthzDecisionLogQueryAdapter;
import io.gravitee.repository.elasticsearch.v4.log.adapter.authz.SearchAuthzDecisionLogsQueryAdapter;
import io.gravitee.repository.elasticsearch.v4.log.adapter.authz.SearchAuthzDecisionLogsResponseAdapter;
import io.gravitee.repository.elasticsearch.v4.log.adapter.connection.SearchConnectionLogErrorKeysQueryAdapter;
import io.gravitee.repository.elasticsearch.v4.log.adapter.connection.SearchConnectionLogErrorKeysResponseAdapter;
import io.gravitee.repository.elasticsearch.v4.log.adapter.connection.SearchMetricsQueryAdapter;
import io.gravitee.repository.elasticsearch.v4.log.adapter.connection.SearchMetricsResponseAdapter;
import io.gravitee.repository.elasticsearch.v4.log.adapter.message.SearchMessageMetricsQueryAdapter;
import io.gravitee.repository.elasticsearch.v4.log.adapter.message.SearchMessageMetricsResponseAdapter;
import io.gravitee.repository.elasticsearch.v4.log.adapter.nativeapi.NativeApiMetricsFindQueryAdapter;
import io.gravitee.repository.elasticsearch.v4.log.adapter.nativeapi.NativeApiMetricsFindResponseAdapter;
import io.gravitee.repository.elasticsearch.v4.log.adapter.nativeapi.NativeApiMetricsSearchQueryAdapter;
import io.gravitee.repository.elasticsearch.v4.log.adapter.nativeapi.NativeApiMetricsSearchResponseAdapter;
import io.gravitee.repository.log.v4.api.MetricsRepository;
import io.gravitee.repository.log.v4.model.LogResponse;
import io.gravitee.repository.log.v4.model.authz.AuthzDecisionLog;
import io.gravitee.repository.log.v4.model.authz.AuthzDecisionLogQuery;
import io.gravitee.repository.log.v4.model.connection.*;
import io.gravitee.repository.log.v4.model.message.MessageMetrics;
import io.gravitee.repository.log.v4.model.message.MessageMetricsQuery;
import io.reactivex.rxjava3.core.Single;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.CustomLog;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
@CustomLog
public class MetricsElasticsearchRepository extends AbstractElasticsearchRepository implements MetricsRepository {

    private static final Comparator<Metrics> NEWEST_FIRST = Comparator.comparing((Metrics metrics) ->
        metrics.getTimestamp() == null ? Instant.MIN : OffsetDateTime.parse(metrics.getTimestamp()).toInstant()
    ).reversed();

    private final RepositoryConfiguration configuration;

    public MetricsElasticsearchRepository(RepositoryConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public LogResponse<MessageMetrics> searchMessageMetrics(QueryContext queryContext, MessageMetricsQuery query) {
        var clusters = ClusterUtils.extractClusterIndexPrefixes(configuration);
        var index = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_MESSAGE_METRICS, clusters);

        return this.client.search(index, null, SearchMessageMetricsQueryAdapter.adapt(query))
            .map(response -> {
                var result = response.getSearchHits();
                if (result == null) {
                    SearchHits searchHits = new SearchHits();
                    searchHits.setTotal(new TotalHits(0));
                    return searchHits;
                }
                return result;
            })
            .map(hits -> new LogResponse<>((int) hits.getTotal().getValue(), SearchMessageMetricsResponseAdapter.adapt(hits)))
            .blockingGet();
    }

    @Override
    public LogResponse<Metrics> searchMetrics(QueryContext queryContext, MetricsQuery query, List<DefinitionVersion> definitionVersions) {
        var readsV2 = reads(definitionVersions, DefinitionVersion.V2);
        var readsV4 = reads(definitionVersions, DefinitionVersion.V4);
        var clusters = ClusterUtils.extractClusterIndexPrefixes(configuration);
        var v4Index = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_METRICS, clusters);
        var v2Index = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.REQUEST, clusters);

        if (readsV2 && readsV4) {
            return withinResultWindow(searchV2AndV4(v2Index, v4Index, query));
        }
        if (readsV4) {
            return withinResultWindow(search(v4Index, SearchMetricsQueryAdapter.adaptOnePerConnection(query)));
        }
        return withinResultWindow(search(v2Index, SearchMetricsQueryAdapter.adapt(query)));
    }

    /**
     * The paginator offers every page of the total, and Elasticsearch refuses a page past {@code max_result_window}.
     * The count of connections is not capped as {@code hits.total} is, so it is capped here.
     */
    private LogResponse<Metrics> withinResultWindow(LogResponse<Metrics> response) {
        return new LogResponse<>(Math.min(response.total(), configuration.getMaxResultWindow()), response.data());
    }

    /**
     * A message API connection, written twice, is shown once — the end, or the opening while it is still open. That
     * takes a collapse, which fails on an index that does not map {@code request-id}, the v2 one among them. So v4
     * and v2 are searched apart, each for every row up to the requested page, and merged newest first.
     */
    private LogResponse<Metrics> searchV2AndV4(String v2Index, String v4Index, MetricsQuery query) {
        var upToPage = MetricsQuery.builder().filter(query.getFilter()).page(1).size(query.getPage() * query.getSize()).build();

        return Single.zip(
            this.client.search(v4Index, null, SearchMetricsQueryAdapter.adaptOnePerConnection(upToPage)).map(
                SearchMetricsResponseAdapter::adapt
            ),
            this.client.search(v2Index, null, SearchMetricsQueryAdapter.adapt(upToPage)).map(SearchMetricsResponseAdapter::adapt),
            (v4, v2) ->
                new LogResponse<>(
                    v4.total() + v2.total(),
                    Stream.concat(v4.data().stream(), v2.data().stream())
                        .sorted(NEWEST_FIRST)
                        .skip((long) (query.getPage() - 1) * query.getSize())
                        .limit(query.getSize())
                        .toList()
                )
        ).blockingGet();
    }

    private LogResponse<Metrics> search(String index, String search) {
        return this.client.search(index, null, search).map(SearchMetricsResponseAdapter::adapt).blockingGet();
    }

    /** No definition version given reads them all. */
    private static boolean reads(List<DefinitionVersion> definitionVersions, DefinitionVersion version) {
        return definitionVersions == null || definitionVersions.isEmpty() || definitionVersions.contains(version);
    }

    @Override
    public List<String> searchConnectionLogErrorKeys(QueryContext queryContext, Collection<String> apiIds, Long from, Long to) {
        var indexes = getQueryIndexesFromDefinitionVersions(
            Type.REQUEST,
            Type.V4_METRICS,
            configuration,
            queryContext,
            List.of(DefinitionVersion.V2, DefinitionVersion.V4)
        );
        var query = SearchConnectionLogErrorKeysQuery.of(apiIds, from, to);
        var searchRequest = SearchConnectionLogErrorKeysQueryAdapter.adapt(query);
        var searchResponse = this.client.search(indexes, null, searchRequest);
        return searchResponse.map(SearchConnectionLogErrorKeysResponseAdapter::adapt).blockingGet();
    }

    @Override
    public Optional<NativeApiMetrics> findNativeApiMetrics(QueryContext queryContext, String apiId, String requestId, Long from, Long to)
        throws AnalyticsException {
        var indexes = getQueryIndexesFromDefinitionVersions(
            Type.REQUEST,
            Type.V4_METRICS,
            configuration,
            queryContext,
            List.of(DefinitionVersion.V4)
        );
        try {
            return this.client.search(indexes, null, NativeApiMetricsFindQueryAdapter.adapt(apiId, requestId, from, to))
                .map(NativeApiMetricsFindResponseAdapter::adapt)
                .blockingGet();
        } catch (RuntimeException e) {
            log.error("Failed to find native metric [apiId={},requestId={}]", apiId, requestId, e);
            throw new AnalyticsException("Failed to find native metric for api " + apiId + " and requestId " + requestId, e);
        }
    }

    @Override
    public LogResponse<NativeApiMetrics> searchNativeApiMetrics(QueryContext queryContext, NativeApiMetricsQuery query)
        throws AnalyticsException {
        query.validate();
        var indexes = getQueryIndexesFromDefinitionVersions(
            Type.REQUEST,
            Type.V4_METRICS,
            configuration,
            queryContext,
            List.of(DefinitionVersion.V4)
        );
        // Built before the try on purpose. The adapter refuses a page past the cluster's result window with an
        // IllegalArgumentException, and that is the caller's mistake, not a search failure: wrapping it as an
        // AnalyticsException turns a 400 into a 500 and buries the message that says which page was too deep.
        var searchQuery = NativeApiMetricsSearchQueryAdapter.adapt(query, configuration.getMaxResultWindow());
        try {
            var response = this.client.search(indexes, null, searchQuery).map(NativeApiMetricsSearchResponseAdapter::adapt).blockingGet();
            // The count is truthful; how far it can be paged is not the same number. Handed up rather than
            // used to clamp the total, so the caller can say "10 000 of 50 000" instead of just "10 000".
            return new LogResponse<>(response.total(), response.data(), (long) configuration.getMaxResultWindow());
        } catch (RuntimeException e) {
            log.error("Failed to search native metrics [apiId={}]", query.getApiId(), e);
            throw new AnalyticsException("Failed to search native metrics for api " + query.getApiId(), e);
        }
    }

    @Override
    public LogResponse<AuthzDecisionLog> searchAuthzDecisionLogs(QueryContext queryContext, AuthzDecisionLogQuery query)
        throws AnalyticsException {
        query.validate();
        var clusters = ClusterUtils.extractClusterIndexPrefixes(configuration);
        var index = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.DECISIONS, clusters);

        try {
            return this.client.search(index, null, SearchAuthzDecisionLogsQueryAdapter.adapt(queryContext, query))
                .map(SearchAuthzDecisionLogsResponseAdapter::adapt)
                .blockingGet();
        } catch (RuntimeException e) {
            throw new AnalyticsException("Failed to search authz decision logs for apis " + query.getApiIds(), e);
        }
    }

    @Override
    public Optional<AuthzDecisionLog> findAuthzDecisionLog(QueryContext queryContext, String apiId, String eventId)
        throws AnalyticsException {
        var clusters = ClusterUtils.extractClusterIndexPrefixes(configuration);
        var index = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.DECISIONS, clusters);

        try {
            return this.client.search(index, null, FindAuthzDecisionLogQueryAdapter.adapt(queryContext, apiId, eventId))
                .map(SearchAuthzDecisionLogsResponseAdapter::adaptFirst)
                .blockingGet();
        } catch (RuntimeException e) {
            throw new AnalyticsException("Failed to find authz decision " + eventId + " for api " + apiId, e);
        }
    }
}
