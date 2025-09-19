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
import io.gravitee.repository.elasticsearch.v4.log.adapter.connection.SearchConnectionLogDetailQueryAdapter;
import io.gravitee.repository.elasticsearch.v4.log.adapter.connection.SearchConnectionLogDetailResponseAdapter;
import io.gravitee.repository.elasticsearch.v4.log.adapter.connection.SearchConnectionLogQueryAdapter;
import io.gravitee.repository.elasticsearch.v4.log.adapter.connection.SearchConnectionLogResponseAdapter;
import io.gravitee.repository.elasticsearch.v4.log.adapter.message.SearchMessageLogQueryAdapter;
import io.gravitee.repository.elasticsearch.v4.log.adapter.message.SearchMessageLogResponseAdapter;
import io.gravitee.repository.log.v4.api.LogRepository;
import io.gravitee.repository.log.v4.model.LogResponse;
import io.gravitee.repository.log.v4.model.connection.ConnectionLog;
import io.gravitee.repository.log.v4.model.connection.ConnectionLogDetail;
import io.gravitee.repository.log.v4.model.connection.ConnectionLogDetailQuery;
import io.gravitee.repository.log.v4.model.connection.ConnectionLogQuery;
import io.gravitee.repository.log.v4.model.message.AggregatedMessageLog;
import io.gravitee.repository.log.v4.model.message.MessageLogQuery;
import io.reactivex.rxjava3.core.Single;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class LogElasticsearchRepository extends AbstractElasticsearchRepository implements LogRepository {

    private final RepositoryConfiguration configuration;

    public LogElasticsearchRepository(RepositoryConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public LogResponse<ConnectionLog> searchConnectionLogs(
        QueryContext queryContext,
        ConnectionLogQuery query,
        List<DefinitionVersion> definitionVersions
    ) {
        var indexes = getQueryIndexesFromDefinitionVersions(Type.REQUEST, Type.V4_METRICS, queryContext, definitionVersions);

        return this.client.search(indexes, null, SearchConnectionLogQueryAdapter.adapt(query))
            .map(SearchConnectionLogResponseAdapter::adapt)
            .blockingGet();
    }

    @Override
    public Optional<ConnectionLogDetail> searchConnectionLogDetail(QueryContext queryContext, ConnectionLogDetailQuery query) {
        var indexes = getQueryIndexesFromDefinitionVersions(
            Type.LOG,
            Type.V4_LOG,
            queryContext,
            List.of(DefinitionVersion.V2, DefinitionVersion.V4)
        );

        return this.client.search(indexes, null, SearchConnectionLogDetailQueryAdapter.adapt(query))
            .map(SearchConnectionLogDetailResponseAdapter::adaptFirst)
            .blockingGet();
    }

    @Override
    public LogResponse<ConnectionLogDetail> searchConnectionLogDetails(QueryContext queryContext, ConnectionLogDetailQuery query) {
        var indexes = getQueryIndexesFromDefinitionVersions(
            Type.LOG,
            Type.V4_LOG,
            queryContext,
            List.of(DefinitionVersion.V2, DefinitionVersion.V4)
        );

        return this.client.search(indexes, null, SearchConnectionLogDetailQueryAdapter.adapt(query))
            .map(SearchConnectionLogDetailResponseAdapter::adapt)
            .blockingGet();
    }

    @Override
    public LogResponse<AggregatedMessageLog> searchAggregatedMessageLog(QueryContext queryContext, MessageLogQuery query) {
        var clusters = ClusterUtils.extractClusterIndexPrefixes(configuration);
        var index = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.V4_MESSAGE_LOG, clusters);

        var entrypointMessages = this.client.search(
            index,
            null,
            SearchMessageLogQueryAdapter.adapt(
                query.toBuilder().filter(query.getFilter().toBuilder().connectorType("entrypoint").build()).build()
            )
        ).map(response -> {
            var result = response.getSearchHits();
            if (result == null) {
                SearchHits searchHits = new SearchHits();
                searchHits.setTotal(new TotalHits(0));
                return searchHits;
            }
            return result;
        });

        var endpointMessages = this.client.search(
            index,
            null,
            SearchMessageLogQueryAdapter.adapt(
                query.toBuilder().filter(query.getFilter().toBuilder().connectorType("endpoint").build()).build()
            )
        ).map(response -> {
            var result = response.getSearchHits();
            if (result == null) {
                SearchHits searchHits = new SearchHits();
                searchHits.setTotal(new TotalHits(0));
                return searchHits;
            }
            return result;
        });

        return Single.zip(entrypointMessages, endpointMessages, (entrypointResponse, endpointResponse) -> {
            var totalEntrypointMessages = entrypointResponse.getTotal().getValue();
            var totalEndpointMessages = endpointResponse.getTotal().getValue();

            if (totalEndpointMessages == 0) {
                return new LogResponse<>((int) totalEntrypointMessages, SearchMessageLogResponseAdapter.adapt(entrypointResponse));
            }

            if (totalEntrypointMessages == 0) {
                return new LogResponse<>((int) totalEndpointMessages, SearchMessageLogResponseAdapter.adapt(endpointResponse));
            }

            return new LogResponse<>(
                (int) totalEntrypointMessages,
                SearchMessageLogResponseAdapter.adapt(entrypointResponse, endpointResponse)
            );
        }).blockingGet();
    }

    private String getQueryIndexesFromDefinitionVersions(
        Type v2Index,
        Type v4Index,
        QueryContext queryContext,
        List<DefinitionVersion> definitionVersions
    ) {
        var isDefinitionVersionsNullOrEmpty = definitionVersions == null || definitionVersions.isEmpty();

        var clusters = ClusterUtils.extractClusterIndexPrefixes(configuration);
        var indexV2Request = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), v2Index, clusters);
        var indexV4Metrics = this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), v4Index, clusters);

        var indexes = new ArrayList<String>();

        if (isDefinitionVersionsNullOrEmpty || definitionVersions.contains(DefinitionVersion.V4)) {
            indexes.add(indexV4Metrics);
        }
        if (
            isDefinitionVersionsNullOrEmpty ||
            definitionVersions.contains(DefinitionVersion.V2) ||
            definitionVersions.contains(DefinitionVersion.V1)
        ) {
            indexes.add(indexV2Request);
        }

        return String.join(",", indexes);
    }
}
