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

export interface Log {
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
  data: Log[];
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
