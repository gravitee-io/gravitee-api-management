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
package io.gravitee.apim.infra.query_service.analytics_engine;

import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.FilterValue;
import io.gravitee.apim.core.analytics_engine.model.FilterValuesPage;
import io.gravitee.apim.core.analytics_engine.query_service.FilterValuesQueryService;
import io.gravitee.repository.common.query.QueryContext;
import io.gravitee.repository.log.v4.api.AnalyticsRepository;
import io.gravitee.repository.log.v4.model.analytics.FilterValuesQuery;
import io.gravitee.repository.log.v4.model.analytics.FilterValuesResult;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

@Service
public class FilterValuesQueryServiceImpl implements FilterValuesQueryService {

    private final AnalyticsRepository analyticsRepository;

    public FilterValuesQueryServiceImpl(@Lazy AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    @Override
    public FilterValuesPage searchFilterValues(
        String organizationId,
        String environmentId,
        FilterSpec.Name filterName,
        Instant from,
        Instant to,
        int page,
        int size,
        Map<String, Object> afterKey,
        String searchPattern,
        Set<String> authorizedApiIds
    ) {
        var esFieldName = resolveEsFieldName(filterName);
        var queryContext = new QueryContext(organizationId, environmentId);

        Map<String, Object> currentAfterKey = null;
        FilterValuesResult result = null;

        for (int i = 1; i <= page; i++) {
            var query = FilterValuesQuery.builder()
                .esFieldName(esFieldName)
                .from(from != null ? from.toEpochMilli() : null)
                .to(to != null ? to.toEpochMilli() : null)
                .size(size)
                .afterKey(currentAfterKey)
                .searchPattern(searchPattern)
                .apiIds(authorizedApiIds)
                .build();

            result = analyticsRepository.searchFilterValues(queryContext, query);

            if (i < page && (result.afterKey() == null || result.afterKey().isEmpty())) {
                return new FilterValuesPage(Collections.emptyList(), null, result.totalCount());
            }
            currentAfterKey = result.afterKey();
        }

        var values = result.values().stream().map(FilterValue::new).toList();
        return new FilterValuesPage(values, result.afterKey(), result.totalCount());
    }

    /**
     * Maps domain filter names to Elasticsearch field names.
     * Mirrors the mapping in HTTPFieldResolver (ES module) to keep the repository API simple (String field name).
     */
    private static String resolveEsFieldName(FilterSpec.Name filterName) {
        return switch (filterName) {
            case API -> "api-id";
            case APPLICATION -> "application-id";
            case PLAN -> "plan-id";
            case GATEWAY -> "gateway";
            case HOST -> "host";
            case TENANT -> "tenant";
            case ZONE -> "zone";
            case HTTP_METHOD -> "http-method";
            case HTTP_STATUS_CODE_GROUP, HTTP_STATUS -> "status";
            case HTTP_PATH -> "path";
            case HTTP_PATH_MAPPING -> "mapped-path";
            case GEO_IP_COUNTRY -> "geoip.country_iso_code";
            case GEO_IP_REGION -> "geoip.region_name";
            case GEO_IP_CITY -> "geoip.city_name";
            case GEO_IP_CONTINENT -> "geoip.continent_name";
            case CONSUMER_IP -> "remote-address";
            case HTTP_USER_AGENT_OS_NAME -> "user_agent.os_name";
            case HTTP_USER_AGENT_DEVICE -> "user_agent.device.name";
            case LLM_PROXY_MODEL -> "additional-metrics.keyword_llm-proxy_model";
            case LLM_PROXY_PROVIDER -> "additional-metrics.keyword_llm-proxy_provider";
            case MCP_PROXY_METHOD -> "additional-metrics.keyword_mcp-proxy_method";
            case MCP_PROXY_TOOL -> "additional-metrics.keyword_mcp-proxy_tools/call";
            case MCP_PROXY_RESOURCE -> "additional-metrics.keyword_mcp-proxy_resources/read";
            case MCP_PROXY_PROMPT -> "additional-metrics.keyword_mcp-proxy_prompts/get";
            default -> throw new UnsupportedOperationException("No ES field mapping for filter: " + filterName);
        };
    }
}
