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

export interface DiagnosticItem {
  componentType: string;
  componentName: string;
  key: string;
  message: string;
}

export interface PlatformLogsQueryParams {
  from: number;
  to: number;
  query?: string;
  page: number;
  size: number;
  field: string;
  order: boolean;
}

export interface PlatformLog {
  id: string;
  timestamp: number;
  transactionId: string;
  path: string;
  method: string;
  status: number;
  responseTime: number;
  api: string;
  plan: string;
  application: string;
  endpoint: boolean;
  message?: string;
  errorKey?: string;
  errorComponentName?: string;
  errorComponentType?: string;
  warnings?: DiagnosticItem[];
}

export interface PlatformLogsResponse {
  logs: PlatformLog[];
  total: number;
}
