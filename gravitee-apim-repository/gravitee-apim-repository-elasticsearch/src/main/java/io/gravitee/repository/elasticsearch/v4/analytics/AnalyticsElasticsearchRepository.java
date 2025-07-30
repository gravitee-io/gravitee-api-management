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
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepository;
import io.gravitee.repository.elasticsearch.configuration.RepositoryConfiguration;
import io.gravitee.repository.elasticsearch.utils.ClusterUtils;
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.AggregateValueCountByFieldAdapter;
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.GroupByQueryAdapter;
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.ResponseTimeRangeQueryAdapter;
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchAverageConnectionDurationQueryAdapter;
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchAverageConnectionDurationResponseAdapter;
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchAverageMessagesPerRequestQueryAdapter;
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchAverageMessagesPerRequestResponseAdapter;
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchHistogramQueryAdapter;
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchRequestResponseTimeAdapter;
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchRequestsCountByEventQueryAdapter;
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchRequestsCountQueryAdapter;
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchRequestsCountResponseAdapter;
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchResponseStatusOverTimeAdapter;
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchResponseStatusRangesAdapter;
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchTopFailedApisAdapter;
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.StatsQueryAdapter;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import io.gravitee.repository.log.v4.model.analytics.AverageAggregate;
import io.gravitee.repository.log.v4.model.analytics.AverageConnectionDurationQuery;
import io.gravitee.repository.log.v4.model.analytics.AverageMessagesPerRequestQuery;
import io.gravitee.repository.log.v4.model.analytics.CountAggregate;
import io.gravitee.repository.log.v4.model.analytics.CountByAggregate;
import io.gravitee.repository.log.v4.model.analytics.GroupByAggregate;
import io.gravitee.repository.log.v4.model.analytics.GroupByQuery;
import io.gravitee.repository.log.v4.model.analytics.HistogramAggregate;
import io.gravitee.repository.log.v4.model.analytics.HistogramQuery;
import io.gravitee.repository.log.v4.model.analytics.RequestResponseTimeAggregate;
import io.gravitee.repository.log.v4.model.analytics.RequestResponseTimeQueryCriteria;
import io.gravitee.repository.log.v4.model.analytics.RequestsCountByEventQuery;
import io.gravitee.repository.log.v4.model.analytics.RequestsCountQuery;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusOverTimeAggregate;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusOverTimeQuery;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusQueryCriteria;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusRangesAggregate;
import io.gravitee.repository.log.v4.model.analytics.ResponseTimeRangeQuery;
import io.gravitee.repository.log.v4.model.analytics.StatsAggregate;
import io.gravitee.repository.log.v4.model.analytics.StatsQuery;
import io.gravitee.repository.log.v4.model.analytics.TopFailedAggregate;
import io.gravitee.repository.log.v4.model.analytics.TopFailedQueryCriteria;
import io.gravitee.repository.log.v4.model.analytics.TopHitsAggregate;
import io.gravitee.repository.log.v4.model.analytics.TopHitsQueryCriteria;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AnalyticsElasticsearchRepository extends AbstractElasticsearchRepository implements AnalyticsRepository {

    public static final String ENTRYPOINT_ID_FIELD = "entrypoint-id";
    private final String[] clusters;
    private static final String KEYWORD = "keyword";

    private static final SearchResponseStatusOverTimeAdapter searchResponseStatusOverTimeAdapter =
        new SearchResponseStatusOverTimeAdapter();

    public AnalyticsElasticsearchRepository(RepositoryConfiguration configuration) {
        clusters = ClusterUtils.extractClusterIndexPrefixes(configuration);
    }

    @Override
    public Optional<CountAggregate> searchRequestsCount(QueryContext queryContext, RequestsCountQuery query) {
        var index = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_METRICS, clusters);

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
        var indexV1V2Request = indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.REQUEST, clusters);
        var indexV4Metrics = indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_METRICS, clusters);
        var indexes = String.join(",", List.of(indexV1V2Request, indexV4Metrics));

        var apiIdFields = List.of("api-id", "api");
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
        return client.search(indices, null, esQuery).map(response -> adapter.adaptResponse(response, queryCriteria)).blockingGet();
    }

    @Override
    public Optional<TopHitsAggregate> searchTopApps(QueryContext queryContext, TopHitsQueryCriteria criteria) {
        var indexV1V2Request = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.REQUEST, clusters);
        var indexV4Metrics = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_METRICS, clusters);
        var indexes = String.join(",", List.of(indexV1V2Request, indexV4Metrics));

        var applicationIdFields = List.of("application-id", "application");
        var esQuery = AggregateValueCountByFieldAdapter.adaptQueryForFields(applicationIdFields, criteria);

        log.debug("Search response top apps query: {}", esQuery);
        return this.client.search(indexes, null, esQuery).map(AggregateValueCountByFieldAdapter::adaptResponse).blockingGet();
    }

    @Override
    public Optional<TopFailedAggregate> searchTopFailedApis(QueryContext queryContext, TopFailedQueryCriteria criteria) {
        var indexV1V2Request = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.REQUEST, clusters);
        var indexV4Metrics = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_METRICS, clusters);
        var indexes = String.join(",", List.of(indexV1V2Request, indexV4Metrics));

        var esQuery = SearchTopFailedApisAdapter.adaptQuery(criteria);

        log.debug("Search top failed apis query: {}", esQuery);
        return this.client.search(indexes, null, esQuery).map(SearchTopFailedApisAdapter::adaptResponse).blockingGet();
    }

    @Override
    public List<HistogramAggregate> searchHistogram(QueryContext queryContext, HistogramQuery query) {
        var index = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_METRICS, clusters);
        var adapter = new SearchHistogramQueryAdapter();
        var esQuery = adapter.adapt(query);

        log.debug("Search histogram query: {}", esQuery);
        return client.search(index, null, esQuery).map(adapter::adaptResponse).blockingGet();
    }

    @Override
    public Optional<GroupByAggregate> searchGroupBy(QueryContext queryContext, GroupByQuery query) {
        var index = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_METRICS, clusters);
        var adapter = new GroupByQueryAdapter();
        var esQuery = adapter.adapt(query);

        log.debug("Search group by query: {}", esQuery);
        return client.search(index, null, esQuery).map(adapter::adaptResponse).blockingGet();
    }

    @Override
    public Optional<StatsAggregate> searchStats(QueryContext queryContext, StatsQuery query) {
        var index = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_METRICS, clusters);
        var adapter = new StatsQueryAdapter();
        var esQuery = adapter.adapt(query);

        log.debug("Search stats query: {}", esQuery);
        return client.search(index, null, esQuery).map(adapter::adaptResponse).blockingGet();
    }

    @Override
    public Optional<CountByAggregate> searchRequestsCountByEvent(QueryContext queryContext, RequestsCountByEventQuery query) {
        var index = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_METRICS, clusters);
        var esQuery = SearchRequestsCountByEventQueryAdapter.adapt(query);
        log.debug("Search Request total counts query: {}", esQuery);

        return client.search(index, null, esQuery).map(SearchRequestsCountByEventQueryAdapter::adaptResponse).blockingGet();
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
