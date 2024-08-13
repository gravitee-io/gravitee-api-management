/*
 * Copyright (C) 2024 The Gravitee team (http://gravitee.io)
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

export interface LogListItem {
  id: string;
  api: string;
  metadata?: unknown;
  method: string;
  path: string;
  plan: string;
  responseTime: number;
  status: number;
  timestamp: number;
  transactionId?: string;
}

export interface LogsResponse {
  data: LogListItem[];
  links?: unknown;
  metadata: LogsResponseMetadata;
}

export interface LogsResponseMetadata {
  [applicationIdOrPlanId: string]: LogsResponseMetadataPlan | LogsResponseMetadataApi | LogsResponseMetadataTotalData;
}

export interface LogsResponseMetadataPlan {
  name: string;
}
export interface LogsResponseMetadataApi {
  name: string;
  version: string;
}

export interface LogsResponseMetadataTotalData {
  total: number;
}

export interface Log extends LogListItem {
  uri?: string;
  requestContentLength?: number;
  responseContentLength?: number;
  request?: Request;
  response?: Response;
  metadata?: LogMetadata;
  host?: string;
  user?: string;
  securityType?: string;
  securityToken?: string;
}

export interface Request {
  method?: string;
  headers?: HttpHeaders;
  uri?: string;
  body?: string;
}

export interface Response {
  status?: number;
  headers?: HttpHeaders;
  body?: string;
}

interface HttpHeaders {
  [headerKey: string]: string;
}

export interface LogMetadata {
  [headerKey: string]: LogMetadataApi | LogMetadataPlan;
}

export interface LogMetadataApi {
  name: string;
  version: string;
}
export interface LogMetadataPlan {
  name: string;
}
