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
import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryContextLoader;
import io.gravitee.apim.core.analytics_engine.domain_service.AnalyticsQueryValidator;
import io.gravitee.apim.core.analytics_engine.domain_service.QueryFilterTransformer;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsQueryContext;
import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.MeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MeasuresResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricMeasuresRequest;
import io.gravitee.apim.core.analytics_engine.query_service.AnalyticsEngineQueryService;
import io.gravitee.apim.core.analytics_engine.service_provider.AnalyticsQueryContextProvider;
import io.gravitee.apim.core.audit.model.AuditInfo;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@UseCase
public class ComputeMeasuresUseCase {

    private final AnalyticsQueryContextProvider queryContextProvider;

    private final AnalyticsQueryValidator validator;

    private final List<QueryFilterTransformer> filterTransformers;

    private final AnalyticsQueryContextLoader contextLoader;

    public ComputeMeasuresUseCase(
        AnalyticsQueryContextProvider queryContextResolver,
        AnalyticsQueryValidator validator,
        List<QueryFilterTransformer> filterTransformers,
        AnalyticsQueryContextLoader contextLoader
    ) {
        this.queryContextProvider = queryContextResolver;
        this.validator = validator;
        this.filterTransformers = filterTransformers;
        this.contextLoader = contextLoader;
    }

    public record Input(AuditInfo auditInfo, MeasuresRequest request) {}

    public record Output(MeasuresResponse response) {}

    public Output execute(Input input) {
        validator.validateMeasuresRequest(input.request);

        var analyticsContext = contextLoader.load(input.auditInfo);

        var queryContext = queryContextProvider.resolve(input.request);

        var responses = executeQueries(analyticsContext.executionContext(), analyticsContext, queryContext);

        return new Output(MeasuresResponse.merge(responses));
    }

    private List<MeasuresResponse> executeQueries(
        ExecutionContext executionContext,
        AnalyticsQueryContext analyticsContext,
        Map<AnalyticsEngineQueryService, MeasuresRequest> queryExecutions
    ) {
        var responses = new ArrayList<MeasuresResponse>();

        queryExecutions.forEach((queryService, request) -> {
            var filters = transformFilters(analyticsContext, request.filters());
            var metrics = request
                .metrics()
                .stream()
                .map(m -> transformMetricFilters(analyticsContext, m))
                .toList();

            responses.add(queryService.searchMeasures(executionContext, new MeasuresRequest(request.timeRange(), filters, metrics)));
        });

        return responses;
    }

    private List<Filter> transformFilters(AnalyticsQueryContext context, List<Filter> filters) {
        List<Filter> result = new ArrayList<>(filters);
        for (var transformer : filterTransformers) {
            result = transformer.transform(context, result);
        }
        return result;
    }

    private MetricMeasuresRequest transformMetricFilters(AnalyticsQueryContext context, MetricMeasuresRequest metric) {
        if (metric.filters() == null || metric.filters().isEmpty()) {
            return metric;
        }
        return metric.withFilters(transformFilters(context, metric.filters()));
    }
}
