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

import io.gravitee.elasticsearch.utils.Type;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepository;
import io.gravitee.repository.elasticsearch.configuration.RepositoryConfiguration;
import io.gravitee.repository.elasticsearch.utils.ClusterUtils;
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.ResponseTimeRangeQueryAdapter;
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchAverageConnectionDurationQueryAdapter;
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchAverageConnectionDurationResponseAdapter;
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchAverageMessagesPerRequestQueryAdapter;
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchAverageMessagesPerRequestResponseAdapter;
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchRequestResponseTimeAdapter;
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchRequestsCountQueryAdapter;
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchRequestsCountResponseAdapter;
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchResponseStatusOverTimeAdapter;
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchResponseStatusRangesQueryAdapter;
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchResponseStatusRangesResponseAdapter;
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchTopHitsAdapter;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import io.gravitee.repository.log.v4.model.analytics.AverageAggregate;
import io.gravitee.repository.log.v4.model.analytics.AverageConnectionDurationQuery;
import io.gravitee.repository.log.v4.model.analytics.AverageMessagesPerRequestQuery;
import io.gravitee.repository.log.v4.model.analytics.CountAggregate;
import io.gravitee.repository.log.v4.model.analytics.RequestResponseTimeAggregate;
import io.gravitee.repository.log.v4.model.analytics.RequestResponseTimeQueryCriteria;
import io.gravitee.repository.log.v4.model.analytics.RequestsCountQuery;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusOverTimeAggregate;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusOverTimeQuery;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusQueryCriteria;
import io.gravitee.repository.log.v4.model.analytics.ResponseStatusRangesAggregate;
import io.gravitee.repository.log.v4.model.analytics.ResponseTimeRangeQuery;
import io.gravitee.repository.log.v4.model.analytics.TopHitsAggregate;
import io.gravitee.repository.log.v4.model.analytics.TopHitsQueryCriteria;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Maybe;
import java.util.Optional;
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
        var index = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_METRICS, clusters);
        return this.client.getFieldTypes(index, ENTRYPOINT_ID_FIELD)
            .map(types -> types.stream().allMatch(KEYWORD::equals))
            .flatMap(isEntrypointIdKeyword ->
                this.client.search(index, null, SearchResponseStatusRangesQueryAdapter.adapt(query, isEntrypointIdKeyword))
            )
            .map(SearchResponseStatusRangesResponseAdapter::adapt)
            .blockingGet();
    }

    @Override
    public @NonNull Maybe<AverageAggregate> searchResponseTimeOverTime(QueryContext queryContext, ResponseTimeRangeQuery query) {
        var adapter = new ResponseTimeRangeQueryAdapter();
        var index = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_METRICS, clusters);
        return client.search(index, null, adapter.queryAdapt(info, query)).flatMapMaybe(adapter::responseAdapt);
    }

    @Override
    public ResponseStatusOverTimeAggregate searchResponseStatusOvertime(QueryContext queryContext, ResponseStatusOverTimeQuery query) {
        var index = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_METRICS, clusters);
        var esQuery = searchResponseStatusOverTimeAdapter.adaptQuery(query, info);

        log.debug("Search response status over time: {}", esQuery);
        return this.client.search(index, null, esQuery).map(searchResponseStatusOverTimeAdapter::adaptResponse).blockingGet();
    }

    @Override
    public Optional<TopHitsAggregate> searchTopHitsApi(QueryContext queryContext, TopHitsQueryCriteria criteria) {
        var index = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_METRICS, clusters);
        var esQuery = SearchTopHitsAdapter.adaptQuery(criteria);

        log.debug("Search response top hit query: {}", esQuery);
        return this.client.search(index, null, esQuery).map(SearchTopHitsAdapter::adaptResponse).blockingGet();
    }

    @Override
    public RequestResponseTimeAggregate searchRequestResponseTimes(
        QueryContext queryContext,
        RequestResponseTimeQueryCriteria queryCriteria
    ) {
        var index = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_METRICS, clusters);
        var esQuery = SearchRequestResponseTimeAdapter.adaptQuery(queryCriteria);

        log.debug("Search request response time query: {}", esQuery);
        return this.client.search(index, null, esQuery)
            .map(response -> SearchRequestResponseTimeAdapter.adaptResponse(response, queryCriteria))
            .blockingGet();
    }
}
