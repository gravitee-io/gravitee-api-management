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
import io.gravitee.apim.core.observability.model.FilterType;
import io.gravitee.apim.core.observability.model.NumberRange;
import io.gravitee.apim.core.observability.model.Signal;
import java.util.List;
import java.util.Set;

public record FilterSpec(
    Name name,
    String label,
    FilterType type,
    List<String> enumValues,
    NumberRange range,
    List<FilterOperator> operators,
    List<ApiSpec.Name> apis,
    Set<Signal> signals
) {
    /**
     * Normalises the optional {@code signals} entry of the definition file — see {@link Signal#DEFAULT}.
     *
     * <p>Done here rather than in the accessor so the record stays consistent with itself: {@code equals} and
     * {@code hashCode} read the backing field, so an accessor-only default would make two specs that behave
     * identically compare unequal.
     */
    public FilterSpec {
        signals = signals == null || signals.isEmpty() ? Signal.DEFAULT : Set.copyOf(signals);
    }

    /**
     * Copy carrying a different {@code apis} list. Callers used to rebuild the whole record positionally,
     * which made every new component a change in each of them.
     */
    public FilterSpec withApis(List<ApiSpec.Name> apis) {
        return new FilterSpec(name, label, type, enumValues, range, operators, apis, signals);
    }

    /** {@code true} when this filter is advertised for, and translatable by, the given signal. */
    public boolean appliesTo(Signal signal) {
        return signals.contains(signal);
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
