/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.repository.elasticsearch.log;

import static io.gravitee.repository.analytics.query.QueryBuilders.tabular;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.springframework.util.StringUtils.isEmpty;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.elasticsearch.model.SearchHit;
import io.gravitee.elasticsearch.model.SearchHits;
import io.gravitee.elasticsearch.model.SearchResponse;
import io.gravitee.elasticsearch.utils.Type;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.analytics.query.QueryFilter;
import io.gravitee.repository.analytics.query.tabular.TabularQuery;
import io.gravitee.repository.analytics.query.tabular.TabularQueryBuilder;
import io.gravitee.repository.analytics.query.tabular.TabularResponse;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepository;
import io.gravitee.repository.elasticsearch.configuration.RepositoryConfiguration;
import io.gravitee.repository.elasticsearch.utils.ClusterUtils;
import io.gravitee.repository.log.api.LogRepository;
import io.gravitee.repository.log.model.ExtendedLog;
import io.gravitee.repository.log.model.Log;
import io.reactivex.Single;
import java.time.Instant;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author David BRASSELY (david.brassely at graviteesource.com)
 * @author Sebastien Devaux (Zenika)
 * @author Guillaume Waignier (Zenika)
 * @author Azize ELAMRANI (azize.elamrani at graviteesource.com)
 * @author Nicolas GERAUD (nicolas.geraud at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ElasticLogRepository extends AbstractElasticsearchRepository implements LogRepository {

    /**
     * Logger.
     */
    private final Logger logger = LoggerFactory.getLogger(ElasticLogRepository.class);

    /**
     * Freemarker template name.
     */
    private static final String LOG_TEMPLATE = "log/log.ftl";

    /**
     * Freemarker template name for finding log by id.
     */
    private static final String LOG_BY_ID_TEMPLATE = "log/logById.ftl";

    @Autowired
    protected RepositoryConfiguration configuration;

    @Override
    public TabularResponse query(final TabularQuery query) throws AnalyticsException {
        final Long from = query.timeRange().range().from();
        final Long to = query.timeRange().range().to();
        String[] clusters = ClusterUtils.extractClusterIndexPrefixes(query, configuration);
        try {
            final TabularQueryBuilder tabularQueryBuilder = tabular()
                .timeRange(query.timeRange().range(), query.timeRange().interval())
                .page(query.page())
                .size(query.size())
                .sort(query.sort());
            final String logQueryString = getQuery(query.query(), true);
            if (isEmpty(logQueryString)) {
                final Single<SearchResponse> result =
                    this.client.search(
                            this.indexNameGenerator.getIndexName(Type.REQUEST, from, to, clusters),
                            !info.getVersion().canUseTypeRequests() ? Type.DOC.getType() : Type.REQUEST.getType(),
                            this.createElasticsearchJsonQuery(query)
                        );

                return this.toTabularResponse(result.blockingGet());
            } else {
                final String sQuery = this.createElasticsearchJsonQuery(tabularQueryBuilder.query(logQueryString).build());
                Single<SearchResponse> result =
                    this.client.search(
                            this.indexNameGenerator.getIndexName(Type.LOG, from, to, clusters),
                            !info.getVersion().canUseTypeRequests() ? Type.DOC.getType() : Type.LOG.getType(),
                            sQuery
                        );

                final SearchResponse searchResponse = result.blockingGet();
                final String logIdsQuery = searchResponse
                    .getSearchHits()
                    .getHits()
                    .stream()
                    .map(searchHit -> "_id:" + searchHit.getId())
                    .collect(joining(" OR "));

                if (!logIdsQuery.isEmpty()) {
                    final String queryString = getQuery(query.query(), false);
                    final String requestQuery = isEmpty(queryString) ? logIdsQuery : format("(%s) AND (%s)", queryString, logIdsQuery);
                    final TabularQueryBuilder tqb = tabularQueryBuilder.query(requestQuery);
                    if (query.root() != null) {
                        tqb.root(query.root().field(), query.root().id());
                    }
                    result =
                        this.client.search(
                                this.indexNameGenerator.getIndexName(Type.REQUEST, from, to, clusters),
                                !info.getVersion().canUseTypeRequests() ? Type.DOC.getType() : Type.REQUEST.getType(),
                                this.createElasticsearchJsonQuery(tqb.build())
                            );
                }

                return this.toTabularResponse(result.blockingGet());
            }
        } catch (final Exception eex) {
            logger.error("Impossible to perform log request", eex);
            throw new AnalyticsException("Impossible to perform log request", eex);
        }
    }

    private String getQuery(final QueryFilter query, final boolean log) {
        if (query == null) {
            return null;
        }
        final String filterSeparator = " AND ";
        final String[] filters = query.filter().split(filterSeparator);
        return stream(filters)
            .map(f -> f.split(":"))
            .filter(
                filter -> {
                    final String filterKey = filter[0];
                    return (log && filterKey.contains("body")) || (!log && !filterKey.contains("body"));
                }
            )
            .map(
                filter -> {
                    final String filterKey = filter[0];
                    if ("body".equals(filterKey)) {
                        return "\\\\*.body" + ":" + filter[1];
                    } else {
                        return filterKey + ":" + filter[1];
                    }
                }
            )
            .collect(joining(filterSeparator));
    }

    /**
     * Create JSON Elasticsearch query for the log
     * @param query user query
     * @return JSON Elasticsearch query
     */
    private String createElasticsearchJsonQuery(final TabularQuery query) {
        final Map<String, Object> data = new HashMap<>();
        data.put("query", query);

        return this.freeMarkerComponent.generateFromTemplate(LOG_TEMPLATE, data);
    }

    @Override
    public ExtendedLog findById(final String requestId, final Long timestamp) throws AnalyticsException {
        final Map<String, Object> data = new HashMap<>(1);
        data.put("requestId", requestId);

        String sQuery = this.freeMarkerComponent.generateFromTemplate(LOG_BY_ID_TEMPLATE, data);
        String[] clusters = ClusterUtils.extractClusterIndexPrefixes(configuration);

        try {
            Single<SearchResponse> result =
                this.client.search(
                        (timestamp == null)
                            ? this.indexNameGenerator.getWildcardIndexName(Type.REQUEST, clusters)
                            : this.indexNameGenerator.getIndexName(Type.REQUEST, Instant.ofEpochMilli(timestamp), clusters),
                        !info.getVersion().canUseTypeRequests() ? Type.DOC.getType() : Type.REQUEST.getType(),
                        sQuery
                    );

            SearchResponse searchResponse = result.blockingGet();
            if (searchResponse.getSearchHits().getTotal().getValue() == 0) {
                throw new AnalyticsException("Request [" + requestId + "] does not exist");
            }

            SearchHit searchHit = searchResponse.getSearchHits().getHits().get(0);

            String searchHitIndex = searchHit.getIndex();
            sQuery = this.freeMarkerComponent.generateFromTemplate(LOG_BY_ID_TEMPLATE, data);

            // Search index must be updated in case of per-type index
            // WARNING: in the case of ILM, the index could not be the same
            if (!configuration.isILMIndex()) {
                searchHitIndex = searchHitIndex.replaceAll(Type.REQUEST.getType(), Type.LOG.getType());
            } else {
                searchHitIndex = this.indexNameGenerator.getIndexName(Type.LOG, Instant.ofEpochMilli(timestamp), clusters);
            }

            result =
                this.client.search(
                        searchHitIndex,
                        !info.getVersion().canUseTypeRequests() ? Type.DOC.getType() : Type.LOG.getType(),
                        sQuery
                    );
            searchResponse = result.blockingGet();

            JsonNode log = null;
            if (searchResponse.getSearchHits().getTotal().getValue() != 0) {
                log = searchResponse.getSearchHits().getHits().get(0).getSource();
            }

            return LogBuilder.createExtendedLog(searchHit, log);
        } catch (Exception e) {
            logger.error("Request [{}] does not exist", requestId, e);
            throw new AnalyticsException("Request [" + requestId + "] does not exist");
        }
    }

    private TabularResponse toTabularResponse(final SearchResponse response) {
        final SearchHits hits = response.getSearchHits();
        final TabularResponse tabularResponse = new TabularResponse(hits.getTotal().getValue());
        final List<Log> logs = new ArrayList<>(hits.getHits().size());
        for (int i = 0; i < hits.getHits().size(); i++) {
            logs.add(LogBuilder.createLog(hits.getHits().get(i)));
        }
        tabularResponse.setLogs(logs);

        return tabularResponse;
    }
}
