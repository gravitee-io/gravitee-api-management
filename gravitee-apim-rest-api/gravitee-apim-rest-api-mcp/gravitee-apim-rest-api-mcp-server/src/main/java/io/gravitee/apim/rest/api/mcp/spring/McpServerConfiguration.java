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
import io.gravitee.apim.core.audit.use_case.SearchApiAuditUseCase;
import io.gravitee.apim.core.event.query_service.EventQueryService;
import io.gravitee.apim.core.logs_engine.use_case.SearchEnvironmentLogsUseCase;
import io.gravitee.apim.core.user.domain_service.UserContextLoader;
import io.gravitee.apim.rest.api.mcp.core.port.ApiDirectory;
import io.gravitee.apim.rest.api.mcp.core.port.ChangeLog;
import io.gravitee.apim.rest.api.mcp.core.port.RequestLogs;
import io.gravitee.apim.rest.api.mcp.core.port.TrafficAnalytics;
import io.gravitee.apim.rest.api.mcp.core.port.TriageAccess;
import io.gravitee.apim.rest.api.mcp.core.use_case.BreakDownTrafficUseCase;
import io.gravitee.apim.rest.api.mcp.core.use_case.GetHealthOverviewUseCase;
import io.gravitee.apim.rest.api.mcp.core.use_case.GetTrafficTimelineUseCase;
import io.gravitee.apim.rest.api.mcp.core.use_case.ListRecentChangesUseCase;
import io.gravitee.apim.rest.api.mcp.core.use_case.SearchFailedRequestsUseCase;
import io.gravitee.apim.rest.api.mcp.infra.ComputeTrafficAnalytics;
import io.gravitee.apim.rest.api.mcp.infra.EventAndAuditChangeLog;
import io.gravitee.apim.rest.api.mcp.infra.LogsEngineRequestLogs;
import io.gravitee.apim.rest.api.mcp.infra.PermissionTriageAccess;
import io.gravitee.apim.rest.api.mcp.infra.UserContextApiDirectory;
import io.gravitee.apim.rest.api.mcp.sdk.McpEndpointServlet;
import io.gravitee.apim.rest.api.mcp.sdk.McpServerFactory;
import io.gravitee.apim.rest.api.mcp.tools.BreakDownTrafficTool;
import io.gravitee.apim.rest.api.mcp.tools.GetHealthOverviewTool;
import io.gravitee.apim.rest.api.mcp.tools.GetTrafficTimelineTool;
import io.gravitee.apim.rest.api.mcp.tools.IncidentTriageGuide;
import io.gravitee.apim.rest.api.mcp.tools.ListRecentChangesTool;
import io.gravitee.apim.rest.api.mcp.tools.SearchFailedRequestsTool;
import io.gravitee.common.util.Version;
import io.gravitee.rest.api.service.PermissionService;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
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

    @Bean
    public TriageAccess mcpTriageAccess(PermissionService permissionService) {
        return new PermissionTriageAccess(permissionService);
    }

    @Bean
    public ApiDirectory mcpApiDirectory(UserContextLoader userContextLoader) {
        return new UserContextApiDirectory(userContextLoader);
    }

    @Bean
    public TrafficAnalytics mcpTrafficAnalytics(
        ComputeMeasuresUseCase computeMeasuresUseCase,
        ComputeFacetsUseCase computeFacetsUseCase,
        ComputeTimeSeriesUseCase computeTimeSeriesUseCase
    ) {
        return new ComputeTrafficAnalytics(computeMeasuresUseCase, computeFacetsUseCase, computeTimeSeriesUseCase);
    }

    @Bean
    public RequestLogs mcpRequestLogs(SearchEnvironmentLogsUseCase searchEnvironmentLogsUseCase) {
        return new LogsEngineRequestLogs(searchEnvironmentLogsUseCase);
    }

    @Bean
    public ChangeLog mcpChangeLog(EventQueryService eventQueryService, SearchApiAuditUseCase searchApiAuditUseCase) {
        return new EventAndAuditChangeLog(eventQueryService, searchApiAuditUseCase);
    }

    @Bean
    public McpEndpointServlet mcpEndpointServlet(
        GetHealthOverviewUseCase getHealthOverviewUseCase,
        BreakDownTrafficUseCase breakDownTrafficUseCase,
        GetTrafficTimelineUseCase getTrafficTimelineUseCase,
        SearchFailedRequestsUseCase searchFailedRequestsUseCase,
        ListRecentChangesUseCase listRecentChangesUseCase
    ) {
        return McpServerFactory.create(
            Version.RUNTIME_VERSION.MAJOR_VERSION,
            instructions(),
            List.of(
                new GetHealthOverviewTool(getHealthOverviewUseCase),
                new BreakDownTrafficTool(breakDownTrafficUseCase),
                new GetTrafficTimelineTool(getTrafficTimelineUseCase),
                new SearchFailedRequestsTool(searchFailedRequestsUseCase),
                new ListRecentChangesTool(listRecentChangesUseCase)
            ),
            List.of(new IncidentTriageGuide())
        );
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
