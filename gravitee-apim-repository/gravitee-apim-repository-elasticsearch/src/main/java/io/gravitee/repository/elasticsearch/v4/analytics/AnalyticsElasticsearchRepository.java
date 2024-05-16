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
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchAverageConnectionDurationQueryAdapter;
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchAverageConnectionDurationResponseAdapter;
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchAverageMessagesPerRequestQueryAdapter;
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchAverageMessagesPerRequestResponseAdapter;
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchRequestsCountQueryAdapter;
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchRequestsCountResponseAdapter;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import io.gravitee.repository.log.v4.model.analytics.AverageAggregate;
import io.gravitee.repository.log.v4.model.analytics.AverageConnectionDurationQuery;
import io.gravitee.repository.log.v4.model.analytics.AverageMessagesPerRequestQuery;
import io.gravitee.repository.log.v4.model.analytics.CountAggregate;
import io.gravitee.repository.log.v4.model.analytics.RequestsCountQuery;
import java.util.Optional;

public class AnalyticsElasticsearchRepository extends AbstractElasticsearchRepository implements AnalyticsRepository {

    private final String[] clusters;

    public AnalyticsElasticsearchRepository(RepositoryConfiguration configuration) {
        clusters = ClusterUtils.extractClusterIndexPrefixes(configuration);
    }

    @Override
    public Optional<CountAggregate> searchRequestsCount(QueryContext queryContext, RequestsCountQuery query) {
        var index = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_METRICS, clusters);

        return this.client.search(index, null, SearchRequestsCountQueryAdapter.adapt(query))
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
        return this.client.search(index, null, SearchAverageConnectionDurationQueryAdapter.adapt(query))
            .map(SearchAverageConnectionDurationResponseAdapter::adapt)
            .blockingGet();
    }
}
