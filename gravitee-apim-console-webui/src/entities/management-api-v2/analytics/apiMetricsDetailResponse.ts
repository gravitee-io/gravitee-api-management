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
import { BasePlan } from '../plan';
import { BaseApplication } from '../application';
import { HttpMethod } from '../api';
import { ConnectionLogDiagnostic } from '../log';

export interface BaseInstance {
  id: string;
  hostname: string;
  ip: string;
}

export interface ApiMetricsDetailResponse {
  timestamp: string;
  apiId: string;
  requestId: string;
  transactionId: string;
  host: string;
  plan: BasePlan;
  application: BaseApplication;
  gateway: string;
  uri: string;
  status: number;
  requestContentLength: number;
  responseContentLength: number;
  remoteAddress: string;
  gatewayLatency: number;
  gatewayResponseTime: number;
  endpointResponseTime: number;
  method: HttpMethod;
  endpoint: string;
  message?: string;
  errorKey?: string;
  errorComponentName?: string;
  errorComponentType?: string;
  warnings?: ConnectionLogDiagnostic[];
}
