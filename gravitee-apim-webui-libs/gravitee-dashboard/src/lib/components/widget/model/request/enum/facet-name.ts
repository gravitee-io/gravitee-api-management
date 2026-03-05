/*
 * Copyright (C) 2025 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
export type GlobalFacetName = 'API' | 'APPLICATION' | 'PLAN' | 'GATEWAY' | 'TENANT' | 'ZONE' | 'HOST' | 'CONSUMER_IP';

export type HttpFacetName =
  | 'HTTP_METHOD'
  | 'HTTP_STATUS_CODE_GROUP'
  | 'HTTP_STATUS'
  | 'HTTP_PATH'
  | 'HTTP_PATH_MAPPING'
  | 'HTTP_USER_AGENT_OS_NAME'
  | 'HTTP_USER_AGENT_DEVICE';

export type GeoIpFacetName = 'GEO_IP_COUNTRY' | 'GEO_IP_REGION' | 'GEO_IP_CITY' | 'GEO_IP_CONTINENT';

export type MessageFacetName = 'MESSAGE_CONNECTOR_TYPE' | 'MESSAGE_CONNECTOR_ID' | 'MESSAGE_OPERATION_TYPE';

export type KafkaFacetName = 'KAFKA_TOPIC';

export type ApiFacetName = 'API_STATE' | 'API_LIFECYCLE_STATE' | 'API_VISIBILITY';

export type LlmFacetName = 'LLM_PROXY_MODEL' | 'LLM_PROXY_PROVIDER';

export type McpFacetName = 'MCP_PROXY_METHOD' | 'MCP_PROXY_TOOL' | 'MCP_PROXY_RESOURCE' | 'MCP_PROXY_PROMPT';

export type FacetName =
  | GlobalFacetName
  | HttpFacetName
  | GeoIpFacetName
  | MessageFacetName
  | KafkaFacetName
  | ApiFacetName
  | LlmFacetName
  | McpFacetName;
