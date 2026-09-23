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

export const HEALTH_CHECK_FILTER_QUERY = 'has_health_check:true';

/**
 * Classic's health check search scopes by definition version
 * (`home-api-health-check.component.ts` sends `definitionVersions: ['V2', 'V4']`); Gamma sends V4 only.
 */
export const HEALTH_CHECK_DEFINITION_VERSIONS = ['V4'] as const;

/**
 * The page lists V4 HTTP proxies. Definition version alone is not enough: the indexer emits nine V4 types
 * (V4_HTTP_PROXY, V4_TCP_PROXY, V4_KAFKA, V4_MESSAGE, V4_MCP_PROXY, V4_LLM_PROXY, V4_A2A_PROXY, V4_AUTHZ,
 * V4_EDGE), and the row action links to the V4 HTTP proxy health dashboard, which does not fit the others.
 */
export const HEALTH_CHECK_API_TYPES = ['V4_HTTP_PROXY'] as const;
