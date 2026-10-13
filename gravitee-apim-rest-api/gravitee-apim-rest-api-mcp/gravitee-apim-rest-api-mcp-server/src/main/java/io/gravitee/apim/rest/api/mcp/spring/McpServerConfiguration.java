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
package io.gravitee.apim.rest.api.mcp.spring;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeFacetsUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeMeasuresUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeTimeSeriesUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.GetApiMetricSpecUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.GetApiSpecUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.GetFilterValuesUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.GetMetricFacetSpecUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.GetMetricFilterSpecUseCase;
import io.gravitee.apim.core.audit.use_case.SearchApiAuditUseCase;
import io.gravitee.apim.core.event.query_service.EventQueryService;
import io.gravitee.apim.core.logs_engine.use_case.SearchEnvironmentLogsUseCase;
import io.gravitee.apim.core.user.domain_service.UserContextLoader;
import io.gravitee.apim.rest.api.mcp.core.port.AnalyticsCatalog;
import io.gravitee.apim.rest.api.mcp.core.port.AnalyticsQueries;
import io.gravitee.apim.rest.api.mcp.core.port.ApiDirectory;
import io.gravitee.apim.rest.api.mcp.core.port.ChangeLog;
import io.gravitee.apim.rest.api.mcp.core.port.EnvironmentDirectory;
import io.gravitee.apim.rest.api.mcp.core.port.LogSearch;
import io.gravitee.apim.rest.api.mcp.core.port.Messaging;
import io.gravitee.apim.rest.api.mcp.core.port.TriageAccess;
import io.gravitee.apim.rest.api.mcp.core.use_case.DescribeMetricsUseCase;
import io.gravitee.apim.rest.api.mcp.core.use_case.ListApisUseCase;
import io.gravitee.apim.rest.api.mcp.core.use_case.ListChangesUseCase;
import io.gravitee.apim.rest.api.mcp.core.use_case.ListEnvironmentsUseCase;
import io.gravitee.apim.rest.api.mcp.core.use_case.ListFilterValuesUseCase;
import io.gravitee.apim.rest.api.mcp.core.use_case.NotifyUseCase;
import io.gravitee.apim.rest.api.mcp.core.use_case.QueryAnalyticsUseCase;
import io.gravitee.apim.rest.api.mcp.core.use_case.SearchLogsUseCase;
import io.gravitee.apim.rest.api.mcp.infra.ComputeAnalyticsQueries;
import io.gravitee.apim.rest.api.mcp.infra.DefinitionAnalyticsCatalog;
import io.gravitee.apim.rest.api.mcp.infra.EnvironmentServiceEnvironmentDirectory;
import io.gravitee.apim.rest.api.mcp.infra.EventAndAuditChangeLog;
import io.gravitee.apim.rest.api.mcp.infra.LogsEngineLogSearch;
import io.gravitee.apim.rest.api.mcp.infra.MessageServiceMessaging;
import io.gravitee.apim.rest.api.mcp.infra.PermissionTriageAccess;
import io.gravitee.apim.rest.api.mcp.infra.UserContextApiDirectory;
import io.gravitee.apim.rest.api.mcp.sdk.McpEndpointServlet;
import io.gravitee.apim.rest.api.mcp.sdk.McpServerFactory;
import io.gravitee.apim.rest.api.mcp.spi.McpTool;
import io.gravitee.apim.rest.api.mcp.tools.DescribeMetricsTool;
import io.gravitee.apim.rest.api.mcp.tools.IncidentTriageGuide;
import io.gravitee.apim.rest.api.mcp.tools.ListApisTool;
import io.gravitee.apim.rest.api.mcp.tools.ListChangesTool;
import io.gravitee.apim.rest.api.mcp.tools.ListEnvironmentsTool;
import io.gravitee.apim.rest.api.mcp.tools.ListFilterValuesTool;
import io.gravitee.apim.rest.api.mcp.tools.NotifyTool;
import io.gravitee.apim.rest.api.mcp.tools.QueryAnalyticsTool;
import io.gravitee.apim.rest.api.mcp.tools.SearchLogsTool;
import io.gravitee.apim.rest.api.mcp.tools.TriagePrompt;
import io.gravitee.common.util.Version;
import io.gravitee.rest.api.service.EnvironmentService;
import io.gravitee.rest.api.service.MessageService;
import io.gravitee.rest.api.service.PermissionService;
import io.gravitee.rest.api.service.UserService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

/**
 * Spring wiring of the MCP server: its use cases, their adapters onto the APIM use cases of the parent context, and
 * the servlet the standalone container mounts.
 *
 * @author GraviteeSource Team
 */
@Configuration
@ComponentScan(
    basePackages = "io.gravitee.apim.rest.api.mcp.core",
    includeFilters = { @ComponentScan.Filter(type = FilterType.ANNOTATION, value = UseCase.class) }
)
public class McpServerConfiguration {

    private static final String INSTRUCTIONS = "/mcp/instructions.md";
    static final String WRITE_SCOPE = "write";

    /** What the operator exposes on this mount: {@code read} alone, or {@code read,write} to add the tools that act. */
    @Value("${http.api.mcp.scopes:read}")
    private String scopes;

    @Bean
    public TriageAccess mcpTriageAccess(PermissionService permissionService) {
        return new PermissionTriageAccess(permissionService);
    }

    @Bean
    public EnvironmentDirectory mcpEnvironmentDirectory(EnvironmentService environmentService, TriageAccess access) {
        return new EnvironmentServiceEnvironmentDirectory(environmentService, access);
    }

    @Bean
    public ApiDirectory mcpApiDirectory(UserContextLoader userContextLoader) {
        return new UserContextApiDirectory(userContextLoader);
    }

    @Bean
    public AnalyticsCatalog mcpAnalyticsCatalog(
        GetApiSpecUseCase apiSpecs,
        GetApiMetricSpecUseCase metricSpecs,
        GetMetricFacetSpecUseCase facetSpecs,
        GetMetricFilterSpecUseCase filterSpecs
    ) {
        return new DefinitionAnalyticsCatalog(apiSpecs, metricSpecs, facetSpecs, filterSpecs);
    }

    @Bean
    public AnalyticsQueries mcpAnalyticsQueries(
        ComputeMeasuresUseCase measures,
        ComputeFacetsUseCase facets,
        ComputeTimeSeriesUseCase timeSeries,
        GetFilterValuesUseCase filterValues
    ) {
        return new ComputeAnalyticsQueries(measures, facets, timeSeries, filterValues);
    }

    @Bean
    public LogSearch mcpLogSearch(SearchEnvironmentLogsUseCase searchEnvironmentLogsUseCase) {
        return new LogsEngineLogSearch(searchEnvironmentLogsUseCase);
    }

    @Bean
    public Messaging mcpMessaging(MessageService messageService, UserService userService) {
        return new MessageServiceMessaging(messageService, userService);
    }

    @Bean
    public ChangeLog mcpChangeLog(EventQueryService eventQueryService, SearchApiAuditUseCase searchApiAuditUseCase) {
        return new EventAndAuditChangeLog(eventQueryService, searchApiAuditUseCase);
    }

    @Bean
    public McpEndpointServlet mcpEndpointServlet(
        ListEnvironmentsUseCase listEnvironments,
        ListApisUseCase listApis,
        DescribeMetricsUseCase describeMetrics,
        QueryAnalyticsUseCase queryAnalytics,
        ListFilterValuesUseCase listFilterValues,
        SearchLogsUseCase searchLogs,
        ListChangesUseCase listChanges,
        NotifyUseCase notify
    ) {
        var guide = new IncidentTriageGuide();
        var tools = new ArrayList<McpTool<?>>(
            List.of(
                new ListEnvironmentsTool(listEnvironments),
                new ListApisTool(listApis),
                new DescribeMetricsTool(describeMetrics),
                new QueryAnalyticsTool(queryAnalytics),
                new ListFilterValuesTool(listFilterValues),
                new SearchLogsTool(searchLogs),
                new ListChangesTool(listChanges)
            )
        );
        if (scopes(scopes).contains(WRITE_SCOPE)) {
            tools.add(new NotifyTool(notify));
        }
        return McpServerFactory.create(
            Version.RUNTIME_VERSION.MAJOR_VERSION,
            instructions(),
            tools,
            List.of(guide),
            List.of(new TriagePrompt(guide))
        );
    }

    static Set<String> scopes(String configured) {
        return Arrays.stream(configured == null ? new String[0] : configured.split("[,\\s]+"))
            .map(scope -> scope.trim().toLowerCase(Locale.ROOT))
            .filter(scope -> !scope.isEmpty())
            .collect(Collectors.toSet());
    }

    private static String instructions() {
        try (var stream = McpServerConfiguration.class.getResourceAsStream(INSTRUCTIONS)) {
            if (stream == null) {
                throw new IllegalStateException("No server instructions at " + INSTRUCTIONS);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read the server instructions at " + INSTRUCTIONS, e);
        }
    }
}
