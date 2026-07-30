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
package io.gravitee.apim.core.analytics_engine.model;

import io.gravitee.apim.core.observability.model.FilterOperator;
import io.gravitee.apim.core.observability.model.FilterSignal;
import io.gravitee.apim.core.observability.model.FilterType;
import io.gravitee.apim.core.observability.model.NumberRange;
import java.util.List;

/**
 * A filter of the shared observability catalog. {@code signals} lists the surfaces (query engines)
 * supporting the filter: {@code null} means signals were not derived and no surface restriction
 * applies; an empty list means no surface supports it and clients must not offer it anywhere.
 */
public record FilterSpec(
    Name name,
    String label,
    FilterType type,
    List<String> enumValues,
    NumberRange range,
    List<FilterOperator> operators,
    List<ApiSpec.Name> apis,
    List<FilterSignal> signals
) {
    /**
     * Definition-time constructor — signals are derived per filter from each surface's engine, not
     * declared (see {@code GetAnalyticsFilterDefinitionsUseCase}).
     */
    public FilterSpec(
        Name name,
        String label,
        FilterType type,
        List<String> enumValues,
        NumberRange range,
        List<FilterOperator> operators,
        List<ApiSpec.Name> apis
    ) {
        this(name, label, type, enumValues, range, operators, apis, null);
    }

    public FilterSpec withSignals(List<FilterSignal> signals) {
        return new FilterSpec(name, label, type, enumValues, range, operators, apis, signals);
    }

    public enum Name {
        API,
        APPLICATION,
        PLAN,
        API_PRODUCT,
        GATEWAY,
        TENANT,
        ZONE,
        HTTP_METHOD,
        HTTP_STATUS_CODE_GROUP,
        HTTP_STATUS,
        HTTP_PATH,
        HTTP_PATH_MAPPING,
        HOST,
        GEO_IP_COUNTRY,
        GEO_IP_REGION,
        GEO_IP_CITY,
        GEO_IP_CONTINENT,
        CONSUMER_IP,
        HTTP_USER_AGENT_OS_NAME,
        HTTP_USER_AGENT_DEVICE,
        MESSAGE_CONNECTOR_TYPE,
        MESSAGE_CONNECTOR_ID,
        MESSAGE_OPERATION_TYPE,
        MESSAGE_SIZE,
        MESSAGE_COUNT,
        MESSAGE_ERROR_COUNT,
        HTTP_ENDPOINT_RESPONSE_TIME,
        HTTP_GATEWAY_LATENCY,
        HTTP_GATEWAY_RESPONSE_TIME,
        HTTP_REQUEST_CONTENT_LENGTH,
        HTTP_RESPONSE_CONTENT_LENGTH,
        LLM_PROXY_MODEL,
        LLM_PROXY_PROVIDER,
        MCP_PROXY_METHOD,
        MCP_PROXY_TOOL,
        MCP_PROXY_RESOURCE,
        MCP_PROXY_PROMPT,
        EDGE_PROVIDER,
        EDGE_PROCESS,
        EDGE_CLIENT,
        EDGE_TYPE,
        EDGE_VERSION,
        EDGE_MODEL,
        EDGE_TOOL,
        API_TYPE,
        NATIVE_CONNECTION_STATUS,
        URI,
        ENTRYPOINT,
        ERROR_KEY,
        REQUEST_ID,
        TRANSACTION_ID,
        PAYLOAD,
    }
}
