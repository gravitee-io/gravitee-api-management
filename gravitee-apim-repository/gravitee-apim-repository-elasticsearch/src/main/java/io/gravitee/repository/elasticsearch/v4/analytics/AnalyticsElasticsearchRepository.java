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
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepository;
import io.gravitee.repository.elasticsearch.configuration.RepositoryConfiguration;
import io.gravitee.repository.elasticsearch.utils.ClusterUtils;
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchRequestsCountQueryAdapter;
import io.gravitee.repository.elasticsearch.v4.analytics.adapter.SearchRequestsCountResponseAdapter;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import io.gravitee.repository.log.v4.model.analytics.CountAggregate;
import io.gravitee.repository.log.v4.model.analytics.RequestsCountQuery;
import java.util.Optional;

public class AnalyticsElasticsearchRepository extends AbstractElasticsearchRepository implements AnalyticsRepository {

    private final RepositoryConfiguration configuration;

    public AnalyticsElasticsearchRepository(RepositoryConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Optional<CountAggregate> searchRequestsCount(RequestsCountQuery query) {
        var clusters = ClusterUtils.extractClusterIndexPrefixes(configuration);
        var index = this.indexNameGenerator.getWildcardIndexName(Type.V4_METRICS, clusters);

        return this.client.search(index, null, SearchRequestsCountQueryAdapter.adapt(query))
            .map(SearchRequestsCountResponseAdapter::adapt)
            .blockingGet();
    }
}
