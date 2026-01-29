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
package io.gravitee.apim.core.analytics_engine.use_case;

import io.gravitee.apim.core.UseCase;
import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryValidator;
import io.gravitee.apim.core.analytics_engine.domain_service.BucketNamesPostProcessor;
import io.gravitee.apim.core.analytics_engine.domain_service.FilterPreProcessor;
import io.gravitee.apim.core.analytics_engine.model.FacetsRequest;
import io.gravitee.apim.core.analytics_engine.model.FacetsResponse;
import io.gravitee.apim.core.analytics_engine.query_service.AnalyticsEngineQueryService;
import io.gravitee.apim.core.analytics_engine.service_provider.AnalyticsQueryContextProvider;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.apim.core.user.domain_service.UserContextLoader;
import io.gravitee.apim.core.user.model.UserContext;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@UseCase
public class ComputeFacetsUseCase {

    private final AnalyticsQueryContextProvider queryContextProvider;

    private final AnalyticsQueryValidator validator;

    private final FilterPreProcessor filterPreprocessor;

    private final BucketNamesPostProcessor bucketNamesPostprocessor;

    private final UserContextLoader userContextLoader;

    public ComputeFacetsUseCase(
        AnalyticsQueryContextProvider queryContextResolver,
        AnalyticsQueryValidator validator,
        FilterPreProcessor filterPreprocessor,
        BucketNamesPostProcessor bucketNamesPostprocessor,
        UserContextLoader userContextLoader
    ) {
        this.queryContextProvider = queryContextResolver;
        this.validator = validator;
        this.filterPreprocessor = filterPreprocessor;
        this.bucketNamesPostprocessor = bucketNamesPostprocessor;
        this.userContextLoader = userContextLoader;
    }

    public record Input(AuditInfo auditInfo, FacetsRequest request) {}

    public record Output(FacetsResponse response) {}

    public ComputeFacetsUseCase.Output execute(Input input) {
        validator.validateFacetsRequest(input.request);

        var executionContext = new ExecutionContext(input.auditInfo.organizationId(), input.auditInfo.environmentId());

        var userContext = userContextLoader.loadApis(new UserContext(input.auditInfo));

        var queryContext = queryContextProvider.resolve(input.request);

        var responses = executeQueries(executionContext, userContext, queryContext);

        var response = FacetsResponse.merge(responses);

        var mappedResponse = bucketNamesPostprocessor.mapBucketNames(userContext, input.request.facets(), response);

        return new ComputeFacetsUseCase.Output(mappedResponse);
    }

    private List<FacetsResponse> executeQueries(
        ExecutionContext executionContext,
        UserContext userContext,
        Map<AnalyticsEngineQueryService, FacetsRequest> queryContext
    ) {
        var responses = new ArrayList<FacetsResponse>();
        queryContext.forEach((queryService, request) -> {
            var filters = new ArrayList<>(request.filters());
            filters.addAll(filterPreprocessor.buildFilters(userContext));

            responses.add(queryService.searchFacets(executionContext, request.withFilters(filters)));
        });
        return responses;
    }
}
