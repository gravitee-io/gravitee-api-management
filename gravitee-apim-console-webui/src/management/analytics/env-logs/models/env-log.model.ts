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

export interface ApiLogRequestContent {
  method: string;
  uri: string;
  headers: Record<string, string[]>;
  body: string;
}

export interface ApiLogResponseContent {
  status: number;
  headers: Record<string, string[]>;
  body: string;
}

export interface EnvLog {
  id: string;
  timestamp: string;
  api: string;
  type: string;
  application: string;
  method: string;
  path: string;
  status: number;
  responseTime: string;
  gateway: string;
  // Details
  host: string;
  requestId: string;
  transactionId: string;
  remoteAddress: string;
  gatewayResponseTime: string;
  endpointResponseTime: string;
  gatewayLatency: string;
  responseContentLength: string;
  plan: { name: string };
  endpoint: string;
  clientIdentifier: string;
  requestEnded: boolean;
  entrypointRequest: ApiLogRequestContent;
  endpointRequest: ApiLogRequestContent;
  entrypointResponse: ApiLogResponseContent;
  endpointResponse: ApiLogResponseContent;
}
