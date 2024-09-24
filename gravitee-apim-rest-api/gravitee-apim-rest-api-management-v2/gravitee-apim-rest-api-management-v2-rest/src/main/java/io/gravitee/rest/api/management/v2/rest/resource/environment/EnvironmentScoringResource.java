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
package io.gravitee.rest.api.management.v2.rest.resource.environment;

import io.gravitee.apim.core.scoring.use_case.GetEnvironmentReportsUseCase;
import io.gravitee.apim.core.scoring.use_case.GetEnvironmentScoringOverviewUseCase;
import io.gravitee.apim.core.scoring.use_case.ImportEnvironmentRulesetUseCase;
import io.gravitee.common.http.MediaType;
import io.gravitee.rest.api.management.v2.rest.mapper.ScoringReportMapper;
import io.gravitee.rest.api.management.v2.rest.model.EnvironmentApisScoringResponse;
import io.gravitee.rest.api.management.v2.rest.model.EnvironmentScoringOverview;
import io.gravitee.rest.api.management.v2.rest.pagination.PaginationInfo;
import io.gravitee.rest.api.management.v2.rest.resource.AbstractResource;
import io.gravitee.rest.api.management.v2.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.model.common.PageableImpl;
import io.gravitee.rest.api.service.common.GraviteeContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EnvironmentScoringResource extends AbstractResource {

    @Context
    protected UriInfo uriInfo;

    @Context
    private ResourceContext resourceContext;

    @Inject
    private GetEnvironmentReportsUseCase getEnvironmentReportsUseCase;

    @Inject
    private GetEnvironmentScoringOverviewUseCase getEnvironmentScoringOverviewUseCase;

    @Inject
    private ImportEnvironmentRulesetUseCase importEnvironmentRulesetUseCase;

    @Path("apis")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public EnvironmentApisScoringResponse getApisScoring(@BeanParam @Valid PaginationParam paginationParam) {
        var executionContext = GraviteeContext.getExecutionContext();
        var result = getEnvironmentReportsUseCase.execute(
            new GetEnvironmentReportsUseCase.Input(
                executionContext.getEnvironmentId(),
                new PageableImpl(paginationParam.getPage(), paginationParam.getPerPage())
            )
        );

        var page = result.reports();
        var totalElements = page.getTotalElements();
        return EnvironmentApisScoringResponse
            .builder()
            .data(page.getContent().stream().map(r -> ScoringReportMapper.INSTANCE.map(r, uriInfo)).toList())
            .pagination(PaginationInfo.computePaginationInfo(totalElements, Math.toIntExact(page.getPageElements()), paginationParam))
            .links(computePaginationLinks(totalElements, paginationParam))
            .build();
    }

    @Path("overview")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public EnvironmentScoringOverview getScoringEnvironmentOverview() {
        var executionContext = GraviteeContext.getExecutionContext();
        var overview = getEnvironmentScoringOverviewUseCase
            .execute(new GetEnvironmentScoringOverviewUseCase.Input(executionContext.getEnvironmentId()))
            .overview();
        return ScoringReportMapper.INSTANCE.map(overview);
    }

    @Path("rulesets")
    public EnvironmentScoringRulesetsResource getEnvironmentScoringRulesetsResource() {
        return resourceContext.getResource(EnvironmentScoringRulesetsResource.class);
    }
}
