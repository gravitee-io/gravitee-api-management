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
package inmemory;

import static io.gravitee.apim.core.analytics_engine.model.MetricSpec.Name.HTTP_ENDPOINT_RESPONSE_TIME;
import static io.gravitee.apim.core.analytics_engine.model.MetricSpec.Name.HTTP_ERRORS;
import static io.gravitee.apim.core.analytics_engine.model.MetricSpec.Name.HTTP_GATEWAY_LATENCY;
import static io.gravitee.apim.core.analytics_engine.model.MetricSpec.Name.HTTP_GATEWAY_RESPONSE_TIME;
import static io.gravitee.apim.core.analytics_engine.model.MetricSpec.Name.HTTP_REQUESTS;
import static io.gravitee.apim.core.analytics_engine.model.MetricSpec.Name.HTTP_REQUEST_CONTENT_LENGTH;
import static io.gravitee.apim.core.analytics_engine.model.MetricSpec.Name.HTTP_RESPONSE_CONTENT_LENGTH;
import static io.gravitee.apim.core.analytics_engine.model.MetricSpec.Name.LLM_PROMPT_TOKEN_RECEIVED;
import static io.gravitee.apim.core.analytics_engine.model.MetricSpec.Name.LLM_PROMPT_TOKEN_RECEIVED_COST;
import static io.gravitee.apim.core.analytics_engine.model.MetricSpec.Name.LLM_PROMPT_TOKEN_SENT;
import static io.gravitee.apim.core.analytics_engine.model.MetricSpec.Name.LLM_PROMPT_TOKEN_SENT_COST;

import io.gravitee.apim.core.analytics_engine.model.MeasuresRequest;
import io.gravitee.apim.core.analytics_engine.model.MeasuresResponse;
import io.gravitee.apim.core.analytics_engine.model.MetricSpec;
import io.gravitee.apim.core.analytics_engine.query_service.DataPlaneAnalyticsQueryService;
import io.gravitee.rest.api.service.common.ExecutionContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public class HTTPProxyDataPlaneQueryServiceInMemory implements DataPlaneAnalyticsQueryService, InMemoryAlternative<MeasuresResponse> {

    private final List<MeasuresResponse> storage;

    public HTTPProxyDataPlaneQueryServiceInMemory() {
        storage = new ArrayList<>();
    }

    @Override
    public Set<MetricSpec.Name> metrics() {
        return Set.of(
            HTTP_REQUESTS,
            HTTP_ERRORS,
            HTTP_REQUEST_CONTENT_LENGTH,
            HTTP_RESPONSE_CONTENT_LENGTH,
            HTTP_ENDPOINT_RESPONSE_TIME,
            HTTP_GATEWAY_RESPONSE_TIME,
            HTTP_GATEWAY_LATENCY,
            LLM_PROMPT_TOKEN_SENT,
            LLM_PROMPT_TOKEN_RECEIVED,
            LLM_PROMPT_TOKEN_SENT_COST,
            LLM_PROMPT_TOKEN_RECEIVED_COST
        );
    }

    @Override
    public MeasuresResponse searchMeasures(ExecutionContext context, MeasuresRequest request) {
        for (var metricRequest : request.metrics()) {
            for (var response : storage) {
                for (var metricResponse : response.metrics()) {
                    if (metricRequest.name().equals(metricResponse.name())) {
                        return response;
                    }
                }
            }
        }
        return MeasuresResponse.merge(List.of());
    }

    @Override
    public void initWith(List<MeasuresResponse> items) {
        storage.addAll(items);
    }

    @Override
    public void reset() {
        storage.clear();
    }

    @Override
    public List<MeasuresResponse> storage() {
        return storage;
    }
}
