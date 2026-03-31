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
package io.gravitee.repository.elasticsearch.log;

import static io.gravitee.repository.analytics.query.QueryBuilders.tabular;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.springframework.util.StringUtils.isEmpty;

import com.fasterxml.jackson.databind.JsonNode;
import io.gravitee.elasticsearch.model.*;
import io.gravitee.elasticsearch.utils.Type;
import io.gravitee.repository.analytics.AnalyticsException;
import io.gravitee.repository.analytics.query.QueryFilter;
import io.gravitee.repository.analytics.query.tabular.TabularQuery;
import io.gravitee.repository.analytics.query.tabular.TabularQueryBuilder;
import io.gravitee.repository.analytics.query.tabular.TabularResponse;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.elasticsearch.AbstractElasticsearchRepository;
import io.gravitee.repository.elasticsearch.configuration.RepositoryConfiguration;
import io.gravitee.repository.elasticsearch.utils.ClusterUtils;
import io.gravitee.repository.log.api.LogRepository;
import io.gravitee.repository.log.model.ExtendedLog;
import io.gravitee.repository.log.model.Log;
import io.reactivex.rxjava3.core.Single;
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

    private static final int MAX_RESULT_WINDOW = 10000;

    @Override
    public TabularResponse query(final QueryContext queryContext, final TabularQuery query) throws AnalyticsException {
        final Long from = query.timeRange().range().from();
        final Long to = query.timeRange().range().to();
        String[] clusters = ClusterUtils.extractClusterIndexPrefixes(query, configuration);
        try {
            final String logQueryString = getQuery(query.query(), true);
            if (isEmpty(logQueryString)) {
                String safeQuery = this.createSafeElasticsearchJsonQuery(query);
                final Single<SearchResponse> result = this.client.search(
                    this.indexNameGenerator.getIndexName(queryContext.placeholder(), Type.REQUEST, from, to, clusters),
                    !info.getVersion().canUseTypeRequests() ? null : Type.REQUEST.getType(),
                    safeQuery
                );

                return this.toTabularResponse(result.blockingGet());
            } else {
                final TabularQuery logQuery = tabular()
                    .timeRange(query.timeRange().range(), query.timeRange().interval())
                    .page(1)
                    .size(MAX_RESULT_WINDOW)
                    .query(logQueryString)
                    .build();
                final String sQuery = this.createElasticsearchJsonQuery(logQuery);

                Single<SearchResponse> result = this.client.search(
                    this.indexNameGenerator.getIndexName(queryContext.placeholder(), Type.LOG, from, to, clusters),
                    !info.getVersion().canUseTypeRequests() ? null : Type.LOG.getType(),
                    sQuery
                );

                final SearchResponse searchResponseLog = result.blockingGet();
                final String logIdsQuery = searchResponseLog
                    .getSearchHits()
                    .getHits()
                    .stream()
                    .map(searchHit -> "_id:" + searchHit.getId())
                    .collect(joining(" OR "));

                if (!logIdsQuery.isEmpty()) {
                    final String queryString = getQuery(query.query(), false);
                    final String requestQuery = isEmpty(queryString) ? logIdsQuery : format("(%s) AND (%s)", queryString, logIdsQuery);
                    final TabularQueryBuilder requestQueryBuilder = tabular()
                        .timeRange(query.timeRange().range(), query.timeRange().interval())
                        .page(query.page())
                        .size(query.size())
                        .sort(query.sort())
                        .query(requestQuery);
                    if (query.root() != null) {
                        requestQueryBuilder.root(query.root().field(), query.root().id());
                    }
                    result = this.client.search(
                        this.indexNameGenerator.getIndexName(queryContext.placeholder(), Type.REQUEST, from, to, clusters),
                        !info.getVersion().canUseTypeRequests() ? null : Type.REQUEST.getType(),
                        this.createElasticsearchJsonQuery(requestQueryBuilder.build())
                    );
                }

                SearchResponse searchResponseRequest = result.blockingGet();
                return this.toTabularResponse(searchResponseRequest, searchResponseRequest.getSearchHits().getTotal().getValue());
            }
        } catch (final Exception eex) {
            logger.error("Impossible to perform log request", eex);
            throw new AnalyticsException("Impossible to perform log request", eex);
        }
    }

    String getQuery(final QueryFilter query, final boolean log) {
        if (query == null) {
            return null;
        }
        final String filterSeparator = " AND ";
        final String[] filters = query.filter().split(filterSeparator);
        return stream(filters)
            .filter(f -> {
                int colonIdx = f.indexOf(':');
                String key = colonIdx >= 0 ? f.substring(0, colonIdx) : f;
                return (log && key.contains("body")) || (!log && !key.contains("body"));
            })
            .map(f -> {
                int colonIdx = f.indexOf(':');
                if (colonIdx < 0) {
                    logger.warn("Log query filter segment has no field:value separator: {}", f);
                    return escapeValueForLuceneJson(f);
                }
                String field = f.substring(0, colonIdx);
                String value = f.substring(colonIdx + 1);
                String escapedValue = escapeValueForLuceneJson(value);
                if ("body".equals(field)) {
                    return "\\\\*.body:" + escapedValue;
                }
                return field + ":" + escapedValue;
            })
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
    public ExtendedLog findById(final QueryContext queryContext, final String requestId, final Long timestamp) throws AnalyticsException {
        final Map<String, Object> data = new HashMap<>(1);
        data.put("requestId", requestId);

        String sQuery = this.freeMarkerComponent.generateFromTemplate(LOG_BY_ID_TEMPLATE, data);
        String[] clusters = ClusterUtils.extractClusterIndexPrefixes(configuration);

        try {
            Single<SearchResponse> result = this.client.search(
                (timestamp == null)
                    ? this.indexNameGenerator.getWildcardIndexName(queryContext.placeholder(), Type.REQUEST, clusters)
                    : this.indexNameGenerator.getIndexName(
                        queryContext.placeholder(),
                        Type.REQUEST,
                        Instant.ofEpochMilli(timestamp),
                        clusters
                    ),
                !info.getVersion().canUseTypeRequests() ? null : Type.REQUEST.getType(),
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
                searchHitIndex = this.indexNameGenerator.getIndexName(
                    queryContext.placeholder(),
                    Type.LOG,
                    Instant.ofEpochMilli(timestamp),
                    clusters
                );
            }

            result = this.client.search(searchHitIndex, !info.getVersion().canUseTypeRequests() ? null : Type.LOG.getType(), sQuery);
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
        return toTabularResponse(response, response.getSearchHits().getTotal().getValue());
    }

    private TabularResponse toTabularResponse(final SearchResponse response, final long total) {
        final SearchHits hits = response.getSearchHits();
        final TabularResponse tabularResponse = new TabularResponse(total);
        final List<Log> logs = new ArrayList<>(hits.getHits().size());
        for (int i = 0; i < hits.getHits().size(); i++) {
            logs.add(LogBuilder.createLog(hits.getHits().get(i)));
        }
        tabularResponse.setLogs(logs);

        return tabularResponse;
    }

    /**
     * Fix APIM-12955: Escapes Lucene special characters in query filter values.
     * Only escapes the value portion of each field:value clause, preserving field
     * patterns like {@code \\*.body} which use backslashes intentionally.
     * Quadruple backslashes survive both Java and JSON parsing so Lucene receives
     * the single backslash escape (e.g., \( ).
     */
    private String createSafeElasticsearchJsonQuery(final TabularQuery query) {
        String json = this.createElasticsearchJsonQuery(query);

        if (query != null && query.query() != null && query.query().filter() != null) {
            String filter = query.query().filter();

            if (!filter.isEmpty()) {
                String escaped = escapeFilterValues(filter);

                // Targeted replacement within quotes to protect JSON structure
                json = json.replace("\"" + filter + "\"", "\"" + escaped + "\"");
            }
        }
        return json;
    }

    /**
     * Escapes Lucene special characters only in the value portions of field:value
     * clauses, preserving field names and patterns (e.g. \\*.body wildcard fields).
     */
    private String escapeFilterValues(String filter) {
        final String andSeparator = " AND ";
        return stream(filter.split(andSeparator)).map(ElasticLogRepository::escapeClauseValues).collect(joining(andSeparator));
    }

    private static String escapeClauseValues(String clause) {
        // Handle OR-separated sub-clauses (e.g. "_id:x OR _id:y")
        if (clause.contains(" OR ")) {
            return stream(clause.split(" OR ")).map(ElasticLogRepository::escapeSingleClauseValue).collect(joining(" OR "));
        }
        return escapeSingleClauseValue(clause);
    }

    private static String escapeSingleClauseValue(String clause) {
        int colonIdx = clause.indexOf(':');
        if (colonIdx < 0) {
            return escapeValueForLuceneJson(clause);
        }
        String field = clause.substring(0, colonIdx + 1);
        String value = clause.substring(colonIdx + 1);
        return field + escapeValueForLuceneJson(value);
    }

    // Lucene special characters that must be escaped in query values (backslash handled separately).
    // * and ? are intentionally absent — they serve as wildcards in body searches.
    private static final String LUCENE_SPECIAL = "+-!(){}[]^~:/&|=\"";

    /**
     * Idempotent Lucene escaping that handles two pre-escape conventions:
     *
     * <ul>
     *   <li>The console uses a 2-backslash convention: each special char X is sent as {@code \\X}
     *       (two backslashes + X). Detected by looking 3 chars ahead for the pattern
     *       {@code \} {@code \} {@code X} — passed through as-is.</li>
     *   <li>Raw API clients may use a 1-backslash convention ({@code \X}) or no escaping at all.
     *       Both are normalised to {@code \\X}.</li>
     * </ul>
     *
     * In all cases the output is {@code \\X} in the Java string, which FreeMarker places verbatim
     * into the JSON query body. After JSON parsing Elasticsearch/Lucene receives {@code \X},
     * the standard Lucene escape for a literal {@code X}.
     */
    private static String escapeValueForLuceneJson(String value) {
        StringBuilder sb = new StringBuilder(value.length() * 2);
        int i = 0;
        final int n = value.length();
        while (i < n) {
            char c = value.charAt(i);
            if (c == '\\') {
                if (i + 2 < n && value.charAt(i + 1) == '\\' && LUCENE_SPECIAL.indexOf(value.charAt(i + 2)) >= 0) {
                    // \\X — console pre-escaped special char (2-backslash convention). Pass through.
                    sb.append("\\\\").append(value.charAt(i + 2));
                    i += 3;
                } else if (i + 1 < n && value.charAt(i + 1) == '\\') {
                    // \\ — escaped backslash (console encodes literal \ as \\). Output \\\\.
                    sb.append("\\\\\\\\");
                    i += 2;
                } else if (i + 1 < n && LUCENE_SPECIAL.indexOf(value.charAt(i + 1)) >= 0) {
                    // \X — single-backslash pre-escape (raw client). Normalise to \\X.
                    sb.append("\\\\").append(value.charAt(i + 1));
                    i += 2;
                } else {
                    // Lone \ — literal backslash. Output \\\\.
                    sb.append("\\\\\\\\");
                    i++;
                }
            } else if (LUCENE_SPECIAL.indexOf(c) >= 0) {
                sb.append("\\\\").append(c); // unescaped special char → \\X
                i++;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }
}
