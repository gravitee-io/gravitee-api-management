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
package io.gravitee.apim.core.analytics_engine.service_provider;

import io.gravitee.apim.core.DomainService;
import io.gravitee.apim.core.analytics_engine.exception.UnsupportedMetricException;
import io.gravitee.apim.core.analytics_engine.model.MeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.query_service.DataPlaneAnalyticsQueryService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
@DomainService
public class AnalyticsQueryContextProvider {

    private final Map<MetricSpec.Name, DataPlaneAnalyticsQueryService> services = new HashMap<>();

    public AnalyticsQueryContextProvider(List<DataPlaneAnalyticsQueryService> impl) {
        for (var service : impl) {
            for (var metric : service.metrics()) {
                services.put(metric, service);
            }
        }
    }

    public Map<DataPlaneAnalyticsQueryService, MeasuresRequest> resolve(MeasuresRequest measureRequest) {
        var context = new HashMap<DataPlaneAnalyticsQueryService, MeasuresRequest>();
        for (var metric : measureRequest.metrics()) {
            var service = resolve(metric.name());
            var request = context.computeIfAbsent(service, s -> measureRequest.emptyMetrics());
            request.metrics().add(metric);
        }
        return context;
    }

    public DataPlaneAnalyticsQueryService resolve(MetricSpec.Name metric) {
        return Optional.ofNullable(services.get(metric)).orElseThrow(() -> new UnsupportedMetricException(metric.name()));
    }
}
