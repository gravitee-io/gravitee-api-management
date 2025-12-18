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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
                final Single<SearchResponse> result = this.client.search(
                    this.indexNameGenerator.getIndexName(queryContext.placeholder(), Type.REQUEST, from, to, clusters),
                    !info.getVersion().canUseTypeRequests() ? null : Type.REQUEST.getType(),
                    this.createElasticsearchJsonQuery(query)
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

    private String getQuery(final QueryFilter query, final boolean log) {
        if (query == null) {
            return null;
        }
        final String filterSeparator = " AND ";
        final String[] filters = query.filter().split(filterSeparator);
        return stream(filters)
            .map(f -> f.split(":"))
            .filter(filter -> {
                final String filterKey = filter[0];
                return (log && filterKey.contains("body")) || (!log && !filterKey.contains("body"));
            })
            .map(filter -> {
                final String filterKey = filter[0];
                if ("body".equals(filterKey)) {
                    return "\\\\*.body" + ":" + filter[1];
                } else {
                    return filterKey + ":" + filter[1];
                }
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

        // Always initialize these variables so template can access them
        List<Map<String, Object>> responseTimeRanges = new ArrayList<>();
        String cleanedQueryFilter = null;

        // Extract response-time range queries and remove them from the query string
        if (query.query() != null && query.query().filter() != null) {
            final String originalFilter = query.query().filter();
            logger.debug("Processing query filter: [{}]", originalFilter);

            responseTimeRanges = extractResponseTimeRanges(originalFilter);
            cleanedQueryFilter = removeResponseTimeRanges(originalFilter);

            if (!responseTimeRanges.isEmpty()) {
                logger.debug("Extracted {} response-time range(s), cleaned filter: [{}]", responseTimeRanges.size(), cleanedQueryFilter);
            }
        }

        // Always put these in the data map, even if empty
        data.put("responseTimeRanges", responseTimeRanges);
        data.put("cleanedQueryFilter", cleanedQueryFilter);

        final String esQuery = this.freeMarkerComponent.generateFromTemplate(LOG_TEMPLATE, data);
        logger.debug("Generated Elasticsearch query: {}", esQuery);
        return esQuery;
    }

    /**
     * Extract response-time range queries from the filter string
     * Pattern: response-time:"[X TO Y]" or OR "[X TO Y]" (in response-time context)
     */
    /**
     * Extract response-time range queries from the filter string
     * Pattern: response-time:"[X TO Y]" or OR "[X TO Y]" (in response-time context)
     */
    private List<Map<String, Object>> extractResponseTimeRanges(final String filter) {
        final List<Map<String, Object>> ranges = new ArrayList<>();
        if (isEmpty(filter)) {
            return ranges;
        }

        logger.debug("Extracting ranges from filter: [{}]", filter);

        // Use a Set to track unique ranges and avoid duplicates
        final Set<String> seenRanges = new HashSet<>();

        // Pattern 1: response-time:"[X TO Y]" (regular quotes)
        final Pattern pattern1a = Pattern.compile("response-time:\"\\[(\\d+)\\s+TO\\s+(\\d+)\\]\"");
        final Matcher matcher1a = pattern1a.matcher(filter);

        while (matcher1a.find()) {
            final int from = Integer.parseInt(matcher1a.group(1));
            final int to = Integer.parseInt(matcher1a.group(2));
            final String rangeKey = from + "-" + to;
            if (seenRanges.add(rangeKey)) {
                final Map<String, Object> range = new HashMap<>();
                range.put("gte", from);
                range.put("lte", to);
                ranges.add(range);
                logger.debug("Extracted response-time range: {} TO {}", from, to);
            }
        }

        // Pattern 1b: response-time:\"[X TO Y]\" (escaped quotes - fallback if pattern1a found nothing)
        if (ranges.isEmpty()) {
            final Pattern pattern1b = Pattern.compile("response-time:\\\\\"\\[(\\d+)\\s+TO\\s+(\\d+)\\]\\\\\"");
            final Matcher matcher1b = pattern1b.matcher(filter);

            while (matcher1b.find()) {
                final int from = Integer.parseInt(matcher1b.group(1));
                final int to = Integer.parseInt(matcher1b.group(2));
                final String rangeKey = from + "-" + to;
                if (seenRanges.add(rangeKey)) {
                    final Map<String, Object> range = new HashMap<>();
                    range.put("gte", from);
                    range.put("lte", to);
                    ranges.add(range);
                    logger.debug("Extracted response-time range (escaped): {} TO {}", from, to);
                }
            }
        }

        // Pattern 2: OR \"[X TO Y]\" (for OR conditions with escaped quotes)
        final Pattern pattern2 = Pattern.compile("\\s+OR\\s+\\\\\"\\[(\\d+)\\s+TO\\s+(\\d+)\\]\\\\\"");
        final Matcher matcher2 = pattern2.matcher(filter);

        while (matcher2.find()) {
            final int from = Integer.parseInt(matcher2.group(1));
            final int to = Integer.parseInt(matcher2.group(2));
            final String rangeKey = from + "-" + to;
            if (seenRanges.add(rangeKey)) {
                final Map<String, Object> range = new HashMap<>();
                range.put("gte", from);
                range.put("lte", to);
                ranges.add(range);
                logger.debug("Extracted response-time range (OR): {} TO {}", from, to);
            }
        }

        if (ranges.isEmpty()) {
            logger.debug("No ranges extracted from filter: [{}]", filter);
        } else {
            logger.debug("Extracted {} unique response-time range(s)", ranges.size());
        }

        return ranges;
    }

    /**
     * Remove response-time range patterns from the query filter
     */
    private String removeResponseTimeRanges(final String filter) {
        if (isEmpty(filter)) {
            return filter;
        }

        String cleaned = filter;
        logger.debug("Removing ranges from filter: [{}]", cleaned);

        // Try both patterns - regular quotes first (most likely), then escaped quotes
        // Pattern set 1: Regular quotes (response-time:"[X TO Y]")
        // Remove entire parenthesized groups
        cleaned = cleaned.replaceAll(
            "\\(\\s*response-time:\"\\[\\d+\\s+TO\\s+\\d+\\]\"(?:\\s+OR\\s+\"\\[\\d+\\s+TO\\s+\\d+\\]\")*\\s*\\)",
            ""
        );
        cleaned = cleaned.replaceAll("\\(\\s*response-time:\"\\[\\d+\\s+TO\\s+\\d+\\]\"\\s*\\)", "");
        cleaned = cleaned.replaceAll("\\(response-time:\"\\[\\d+\\s+TO\\s+\\d+\\]\"(?:\\s+OR\\s+\"\\[\\d+\\s+TO\\s+\\d+\\]\")*\\)", "");
        // Remove individual patterns
        cleaned = cleaned.replaceAll("response-time:\"\\[\\d+\\s+TO\\s+\\d+\\]\"", "");
        cleaned = cleaned.replaceAll("\\s+OR\\s+\"\\[\\d+\\s+TO\\s+\\d+\\]\"", "");

        // Pattern set 2: Escaped quotes (response-time:\"[X TO Y]\") - fallback
        if (cleaned.equals(filter)) {
            logger.debug("Regular quote patterns didn't match, trying escaped quotes");
            cleaned = cleaned.replaceAll(
                "\\(\\s*response-time:\\\\\"\\[\\d+\\s+TO\\s+\\d+\\]\\\\\"(?:\\s+OR\\s+\\\\\"\\[\\d+\\s+TO\\s+\\d+\\]\\\\\")*\\s*\\)",
                ""
            );
            cleaned = cleaned.replaceAll("\\(\\s*response-time:\\\\\"\\[\\d+\\s+TO\\s+\\d+\\]\\\\\"\\s*\\)", "");
            cleaned = cleaned.replaceAll(
                "\\(response-time:\\\\\"\\[\\d+\\s+TO\\s+\\d+\\]\\\\\"(?:\\s+OR\\s+\\\\\"\\[\\d+\\s+TO\\s+\\d+\\]\\\\\")*\\)",
                ""
            );
            cleaned = cleaned.replaceAll("response-time:\\\\\"\\[\\d+\\s+TO\\s+\\d+\\]\\\\\"", "");
            cleaned = cleaned.replaceAll("\\s+OR\\s+\\\\\"\\[\\d+\\s+TO\\s+\\d+\\]\\\\\"", "");
        }

        // Clean up empty parentheses (multiple patterns to catch various spacing)
        cleaned = cleaned.replaceAll("\\(\\s*\\)", "");
        cleaned = cleaned.replaceAll("\\(\\s+\\)", "");

        // Clean up extra spaces and operators
        cleaned = cleaned.replaceAll("\\s+AND\\s+AND", " AND");
        cleaned = cleaned.replaceAll("\\s+OR\\s+OR", " OR");
        cleaned = cleaned.replaceAll("\\(\\s+AND", "(");
        cleaned = cleaned.replaceAll("AND\\s+\\)", ")");
        cleaned = cleaned.replaceAll("\\(\\s+OR", "(");
        cleaned = cleaned.replaceAll("OR\\s+\\)", ")");
        cleaned = cleaned.trim();

        // Remove leading/trailing AND/OR operators
        cleaned = cleaned.replaceAll("^\\s*(AND|OR)\\s+", "");
        cleaned = cleaned.replaceAll("\\s+(AND|OR)\\s*$", "");

        // Final cleanup of empty parentheses and whitespace
        cleaned = cleaned.replaceAll("\\(\\s*\\)", "");
        cleaned = cleaned.trim();

        logger.debug("Removed response-time ranges. Original: [{}], Cleaned: [{}]", filter, cleaned);

        return cleaned;
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
}
