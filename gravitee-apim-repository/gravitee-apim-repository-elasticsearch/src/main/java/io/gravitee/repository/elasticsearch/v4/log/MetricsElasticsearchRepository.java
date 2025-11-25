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
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepository;
import io.gravitee.repository.elasticsearch.configuration.RepositoryConfiguration;
import io.gravitee.repository.elasticsearch.utils.ClusterUtils;
import io.gravitee.repository.elasticsearch.v4.log.adapter.connection.SearchMetricsQueryAdapter;
import io.gravitee.repository.elasticsearch.v4.log.adapter.connection.SearchMetricsResponseAdapter;
import io.gravitee.repository.elasticsearch.v4.log.adapter.message.SearchMessageMetricsQueryAdapter;
import io.gravitee.repository.elasticsearch.v4.log.adapter.message.SearchMessageMetricsResponseAdapter;
import io.gravitee.repository.log.v4.api.MetricsRepository;
import io.gravitee.repository.log.v4.model.LogResponse;
import io.gravitee.repository.log.v4.model.connection.Metrics;
import io.gravitee.repository.log.v4.model.connection.MetricsQuery;
import io.gravitee.repository.log.v4.model.message.MessageMetrics;
import io.gravitee.repository.log.v4.model.message.MessageMetricsQuery;
import java.util.List;

/**
 * @author Benoit BORDIGONI (benoit.bordigoni at graviteesource.com)
 * @author GraviteeSource Team
 */
public class MetricsElasticsearchRepository extends AbstractElasticsearchRepository implements MetricsRepository {

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
        var indexes = getQueryIndexesFromDefinitionVersions(Type.REQUEST, Type.V4_METRICS, configuration, queryContext, definitionVersions);

        return this.client.search(indexes, null, SearchMetricsQueryAdapter.adapt(query))
            .map(SearchMetricsResponseAdapter::adapt)
            .blockingGet();
    }
}
