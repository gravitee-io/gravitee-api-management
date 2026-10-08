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
package io.gravitee.rest.api.portal.rest.resource;

import static io.gravitee.rest.api.service.common.GraviteeContext.getExecutionContext;

import io.gravitee.apim.core.ai_workspace.model.AiWorkspaceConsumption;
import io.gravitee.apim.core.ai_workspace.use_case.GetMyAiWorkspaceConsumptionUseCase;
import io.gravitee.apim.core.ai_workspace.use_case.GetMyAiWorkspaceUseCase;
import io.gravitee.apim.core.ai_workspace.use_case.ListMyAiWorkspacesUseCase;
import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.Measure;
import io.gravitee.apim.core.analytics_engine.model.MeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MeasuresResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MetricMeasuresResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.model.TimeRange;
import io.gravitee.apim.core.analytics_engine.use_case.ComputeMeasuresUseCase;
import io.gravitee.apim.core.observability.model.FilterOperator;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.model.PrimaryOwnerEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.parameters.Key;
import io.gravitee.rest.api.model.parameters.ParameterReferenceType;
import io.gravitee.rest.api.portal.rest.mapper.AiWorkspaceMapper;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.ParameterService;
import io.gravitee.rest.api.service.exceptions.UnauthorizedAccessException;
import jakarta.inject.Inject;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class AiWorkspacesResource extends AbstractResource {

    private static final int CONSUMPTION_WINDOW_DAYS = 30;

    @Inject
    private ListMyAiWorkspacesUseCase listMyAiWorkspacesUseCase;

    @Inject
    private GetMyAiWorkspaceUseCase getMyAiWorkspaceUseCase;

    @Inject
    private GetMyAiWorkspaceConsumptionUseCase getMyAiWorkspaceConsumptionUseCase;

    @Inject
    private ComputeMeasuresUseCase computeMeasuresUseCase;

    @Inject
    private ApplicationService applicationService;

    @Inject
    private ParameterService parameterService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(@BeanParam PaginationParam pagination, @QueryParam("name") String name) {
        if (!isAuthenticated()) {
            throw new UnauthorizedAccessException();
        }
        requireEnabled();
        var executionContext = getExecutionContext();
        var workspaces = listMyAiWorkspacesUseCase
            .execute(new ListMyAiWorkspacesUseCase.Input(executionContext, applicationIds(), name))
            .workspaces()
            .stream()
            .map(AiWorkspaceMapper.INSTANCE::toSummary)
            .toList();
        return createListResponse(executionContext, workspaces, pagination);
    }

    @GET
    @Path("{aiWorkspaceId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@PathParam("aiWorkspaceId") String aiWorkspaceId) {
        if (!isAuthenticated()) {
            throw new UnauthorizedAccessException();
        }
        requireEnabled();
        var details = getMyAiWorkspaceUseCase
            .execute(new GetMyAiWorkspaceUseCase.Input(getExecutionContext(), applicationIds(), aiWorkspaceId))
            .details();
        return Response.ok(AiWorkspaceMapper.INSTANCE.toDetails(details)).build();
    }

    @GET
    @Path("{aiWorkspaceId}/consumption")
    @Produces(MediaType.APPLICATION_JSON)
    public Response consumption(@PathParam("aiWorkspaceId") String aiWorkspaceId) {
        if (!isAuthenticated()) {
            throw new UnauthorizedAccessException();
        }
        requireEnabled();
        var access = getMyAiWorkspaceConsumptionUseCase.execute(
            new GetMyAiWorkspaceConsumptionUseCase.Input(getExecutionContext(), applicationIds(), aiWorkspaceId)
        );
        return Response.ok(AiWorkspaceMapper.INSTANCE.toConsumption(consumption(aiWorkspaceId, access))).build();
    }

    /**
     * 404, not 403. The analytics capability returns 403 when it is off. AI workspaces return 404
     * so a caller cannot tell the feature exists while it is disabled.
     */
    private void requireEnabled() {
        if (
            !parameterService.findAsBoolean(
                getExecutionContext(),
                Key.PORTAL_NEXT_AI_WORKSPACES_ENABLED,
                ParameterReferenceType.ENVIRONMENT
            )
        ) {
            throw new NotFoundException();
        }
    }

    private AiWorkspaceConsumption consumption(String workspaceId, GetMyAiWorkspaceConsumptionUseCase.Output access) {
        Instant to = Instant.now();
        Instant from = to.minus(CONSUMPTION_WINDOW_DAYS, ChronoUnit.DAYS);
        OffsetDateTime windowFrom = from.atOffset(ZoneOffset.UTC);
        OffsetDateTime windowTo = to.atOffset(ZoneOffset.UTC);
        if (access.applicationId() == null || access.applicationId().isBlank() || access.apiIds() == null || access.apiIds().isEmpty()) {
            return AiWorkspaceConsumption.empty(windowFrom, windowTo);
        }
        try {
            var output = computeMeasuresUseCase.executeForApis(
                getAuditInfo(),
                request(workspaceId, access.applicationId(), from, to),
                access.apiIds()
            );
            return fromResponse(output == null ? null : output.response(), windowFrom, windowTo);
        } catch (RuntimeException exception) {
            log.warn("Could not read AI workspace consumption for workspace {}", workspaceId, exception);
            return AiWorkspaceConsumption.empty(windowFrom, windowTo);
        }
    }

    private static MeasuresRequest request(String workspaceId, String applicationId, Instant from, Instant to) {
        // COUNT on the token and cost metrics is the analytics engine's sum of those stored totals.
        return new MeasuresRequest(
            new TimeRange(from, to),
            List.of(
                new Filter(FilterSpec.Name.API_PRODUCT, FilterOperator.EQ, workspaceId),
                new Filter(FilterSpec.Name.APPLICATION, FilterOperator.EQ, applicationId)
            ),
            List.of(
                new MetricMeasuresRequest(MetricSpec.Name.LLM_PROMPT_TOTAL_TOKEN, List.of(MetricSpec.Measure.COUNT)),
                new MetricMeasuresRequest(MetricSpec.Name.HTTP_REQUESTS, List.of(MetricSpec.Measure.COUNT)),
                new MetricMeasuresRequest(MetricSpec.Name.LLM_PROMPT_TOKEN_TOTAL_COST, List.of(MetricSpec.Measure.COUNT))
            )
        );
    }

    private static AiWorkspaceConsumption fromResponse(MeasuresResponse response, OffsetDateTime from, OffsetDateTime to) {
        return new AiWorkspaceConsumption(
            value(response, MetricSpec.Name.LLM_PROMPT_TOTAL_TOKEN),
            value(response, MetricSpec.Name.HTTP_REQUESTS),
            cost(response),
            from,
            to
        );
    }

    private static double cost(MeasuresResponse response) {
        if (response == null || response.metrics() == null) {
            return 0;
        }
        return response
            .metrics()
            .stream()
            .filter(metric -> metric != null && metric.name() == MetricSpec.Name.LLM_PROMPT_TOKEN_TOTAL_COST)
            .map(MetricMeasuresResponse::measures)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .map(Measure::value)
            .filter(Objects::nonNull)
            .mapToDouble(Number::doubleValue)
            .findFirst()
            .orElse(0);
    }

    private static long value(MeasuresResponse response, MetricSpec.Name name) {
        if (response == null || response.metrics() == null) {
            return 0;
        }
        return response
            .metrics()
            .stream()
            .filter(metric -> metric != null && metric.name() == name)
            .map(MetricMeasuresResponse::measures)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .map(Measure::value)
            .filter(Objects::nonNull)
            .mapToLong(value -> Math.round(value.doubleValue()))
            .findFirst()
            .orElse(0);
    }

    private Set<String> applicationIds() {
        var applications = applicationService.findByUser(getExecutionContext(), getAuthenticatedUser());
        if (applications == null || applications.isEmpty()) {
            return Set.of();
        }
        String callerId = getAuthenticatedUser();
        return applications
            .stream()
            .filter(application -> ownedByCaller(application, callerId))
            .map(ApplicationListItem::getId)
            .filter(id -> id != null && !id.isBlank())
            .collect(Collectors.toSet());
    }

    private static boolean ownedByCaller(ApplicationListItem application, String callerId) {
        PrimaryOwnerEntity owner = application.getPrimaryOwner();
        return owner != null && "USER".equals(owner.getType()) && callerId.equals(owner.getId());
    }
}
