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
package io.gravitee.repository.analytics.engine.api.query;

/**
 * @author Antoine CORDIER (antoine.cordier at graviteesource.com)
 * @author GraviteeSource Team
 */
public record Filter(Filter.Name name, Operator operator, Object value) {
    public enum Name {
        API,
        APPLICATION,
        PLAN,
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
        KAFKA_TOPIC,
        API_STATE,
        API_LIFECYCLE_STATE,
        API_VISIBILITY,
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
    }

    public enum Operator {
        EQ,
        IN,
        LTE,
        GTE,
    }
}
