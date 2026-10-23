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
package io.gravitee.apim.infra.domain_service.analytics_engine.processors;

import io.gravitee.apim.core.analytics_engine.domain_service.QueryFilterTransformer;
import io.gravitee.apim.core.analytics_engine.model.AnalyticsQueryContext;
import io.gravitee.apim.core.analytics_engine.model.Filter;
import io.gravitee.apim.core.analytics_engine.model.FilterSpec;
import io.gravitee.apim.core.observability.model.FilterOperator;
import java.util.ArrayList;
import java.util.List;

/**
 * Narrows a query to the applications of {@link AnalyticsQueryContext#restrictedToApplicationIds()} with an
 * {@code APPLICATION IN [...]} filter, which also leaves out keyless calls. An application condition of the request
 * is kept next to it, so the query reads their intersection.
 *
 * @author GraviteeSource Team
 */
public class ApplicationScopeFilterTransformer implements QueryFilterTransformer {

    @Override
    public List<Filter> transform(AnalyticsQueryContext context, List<Filter> filters) {
        var applicationIds = context.restrictedToApplicationIds();
        if (applicationIds.isEmpty()) {
            return filters;
        }
        var transformed = new ArrayList<>(filters);
        transformed.add(new Filter(FilterSpec.Name.APPLICATION, FilterOperator.IN, applicationIds.get()));
        return transformed;
    }
}
