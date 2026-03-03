/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
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

export type ApiLogRequestContent = {
  method: string;
  uri: string;
  headers: Record<string, string[]>;
  body: string;
};

export type ApiLogResponseContent = {
  status: number;
  headers: Record<string, string[]>;
  body: string;
};

export type EnvLogWarning = {
  key: string;
};

/**
 * Display model for environment logs.
 *
 * Required fields are used by the table, optional detail fields
 * are populated when viewing a single log's detail page.
 */
export type EnvLog = {
  /** Internal log UUID */
  id: string;
  /** Formatted timestamp string for display */
  timestamp: string;
  /** Resolved API name (enriched post-fetch) */
  api: string;
  /** API ID from backend (used for detail navigation) */
  apiId: string;
  /** Application name */
  application: string;
  /** HTTP method (GET, POST, etc.) */
  method: string;
  /** Request URI path */
  path: string;
  /** HTTP status code */
  status: number;
  /** Formatted response time (e.g. "42 ms") */
  responseTime: string;
  /** Gateway instance name */
  gateway?: string;
  /** Plan name */
  plan?: { name: string };
  /** Whether the request completed */
  requestEnded: boolean;
  /** Error key for diagnostics */
  errorKey?: string;
  /** Warning diagnostics */
  warnings?: EnvLogWarning[];

  // --- Detail fields (populated for the log detail page) ---
  /** API type label (e.g. "HTTP Proxy") */
  type?: string;
  /** Request host */
  host?: string;
  /** Request ID */
  requestId?: string;
  /** Transaction ID */
  transactionId?: string;
  /** Client remote address */
  remoteAddress?: string;
  /** Full gateway response time string */
  gatewayResponseTime?: string;
  /** Full endpoint response time string */
  endpointResponseTime?: string;
  /** Gateway latency string */
  gatewayLatency?: string;
  /** Response content length */
  responseContentLength?: string;
  /** Backend endpoint URL */
  endpoint?: string;
  /** Client identifier hash */
  clientIdentifier?: string;
  /** Consumer (entrypoint) request */
  entrypointRequest?: ApiLogRequestContent;
  /** Gateway (endpoint) request */
  endpointRequest?: ApiLogRequestContent;
  /** Consumer (entrypoint) response */
  entrypointResponse?: ApiLogResponseContent;
  /** Gateway (endpoint) response */
  endpointResponse?: ApiLogResponseContent;
};
