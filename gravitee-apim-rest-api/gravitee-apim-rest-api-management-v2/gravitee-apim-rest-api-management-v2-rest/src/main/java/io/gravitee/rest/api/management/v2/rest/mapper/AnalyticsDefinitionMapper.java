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
package io.gravitee.rest.api.management.v2.rest.mapper;

import io.gravitee.apim.core.analytics_engine.model.NumberRange;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.ApiSpec;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.ApiSpecsResponse;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FacetSpec;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FacetSpecsResponse;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FilterSpec;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FilterSpecRange;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.FilterSpecsResponse;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MetricSpec;
import io.gravitee.rest.api.management.v2.rest.model.analytics.engine.MetricSpecsResponse;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface AnalyticsDefinitionMapper {
    AnalyticsDefinitionMapper INSTANCE = Mappers.getMapper(AnalyticsDefinitionMapper.class);

    ApiSpec mapApiSpec(io.gravitee.apim.core.analytics_engine.model.ApiSpec apiSpec);

    List<ApiSpec> mapApiSpecs(List<io.gravitee.apim.core.analytics_engine.model.ApiSpec> apiSpecs);

    default ApiSpecsResponse toApiSpecsResponse(List<io.gravitee.apim.core.analytics_engine.model.ApiSpec> apiSpecs) {
        return new ApiSpecsResponse().data(mapApiSpecs(apiSpecs));
    }

    MetricSpec mapMetricSpec(io.gravitee.apim.core.analytics_engine.model.MetricSpec metricSpec);

    List<MetricSpec> mapMetricSpecs(List<io.gravitee.apim.core.analytics_engine.model.MetricSpec> metricSpecs);

    default MetricSpecsResponse toMetricSpecsResponse(List<io.gravitee.apim.core.analytics_engine.model.MetricSpec> metricSpecs) {
        return new MetricSpecsResponse().data(mapMetricSpecs(metricSpecs));
    }

    FacetSpec mapFacetSpec(io.gravitee.apim.core.analytics_engine.model.FacetSpec facetSpec);

    List<FacetSpec> mapFacetSpecs(List<io.gravitee.apim.core.analytics_engine.model.FacetSpec> facetSpecs);

    default FacetSpecsResponse toFacetSpecsResponse(List<io.gravitee.apim.core.analytics_engine.model.FacetSpec> facetSpecs) {
        return new FacetSpecsResponse().data(mapFacetSpecs(facetSpecs));
    }

    @Mapping(source = "from", target = "min")
    @Mapping(source = "to", target = "max")
    FilterSpecRange mapRange(NumberRange range);

    FilterSpec mapFilterSpec(io.gravitee.apim.core.analytics_engine.model.FilterSpec filterSpec);

    List<FilterSpec> mapFilterSpecs(List<io.gravitee.apim.core.analytics_engine.model.FilterSpec> filterSpecs);

    default FilterSpecsResponse toFilterSpecsResponse(List<io.gravitee.apim.core.analytics_engine.model.FilterSpec> filterSpecs) {
        return new FilterSpecsResponse().data(mapFilterSpecs(filterSpecs));
    }
}
