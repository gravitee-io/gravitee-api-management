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
package io.gravitee.apim.rest.api.mcp.infra;

import io.gravitee.apim.core.analytics_engine.model.ApiSpec;
import io.gravitee.apim.core.analytics_engine.model.FacetSpec;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.use_case.GetApiMetricSpecUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.GetApiSpecUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.GetMetricFacetSpecUseCase;
import io.gravitee.apim.core.analytics_engine.use_case.GetMetricFilterSpecUseCase;
import io.gravitee.apim.rest.api.mcp.core.port.AnalyticsCatalog;
import java.util.List;

/**
 * The shipped analytics definition, through the use cases the Console's dashboard builder reads it with.
 */
public class DefinitionAnalyticsCatalog implements AnalyticsCatalog {

    private final GetApiSpecUseCase apiSpecs;
    private final GetApiMetricSpecUseCase metricSpecs;
    private final GetMetricFacetSpecUseCase facetSpecs;
    private final GetMetricFilterSpecUseCase filterSpecs;

    public DefinitionAnalyticsCatalog(
        GetApiSpecUseCase apiSpecs,
        GetApiMetricSpecUseCase metricSpecs,
        GetMetricFacetSpecUseCase facetSpecs,
        GetMetricFilterSpecUseCase filterSpecs
    ) {
        this.apiSpecs = apiSpecs;
        this.metricSpecs = metricSpecs;
        this.facetSpecs = facetSpecs;
        this.filterSpecs = filterSpecs;
    }

    @Override
    public List<ApiSpec> apiTypes() {
        return apiSpecs.execute().specs();
    }

    @Override
    public List<MetricSpec> metrics(ApiSpec.Name apiType) {
        return metricSpecs.execute(new GetApiMetricSpecUseCase.Input(apiType.name())).specs();
    }

    @Override
    public List<FacetSpec> facets(MetricSpec.Name metric) {
        return facetSpecs.execute(new GetMetricFacetSpecUseCase.Input(metric.name())).specs();
    }

    @Override
    public List<FilterSpec> filters(MetricSpec.Name metric) {
        return filterSpecs.execute(new GetMetricFilterSpecUseCase.Input(metric.name())).specs();
    }
}
