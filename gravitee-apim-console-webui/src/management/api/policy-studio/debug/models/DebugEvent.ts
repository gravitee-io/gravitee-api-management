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
export interface DebugEvent {
  id: string;
  payload: DebugEventPayload;
  status: 'SUCCESS' | 'FAILED';
}

interface DebugEventPayload {
  execution_mode: 'v3' | 'jupiter';
  response?: {
    statusCode?: number;
    method?: string;
    path?: string;
    headers?: Record<string, string[]>;
    body?: string;
  };
  request?: {
    method?: string;
    path?: string;
    headers?: Record<string, string[]>;
    body?: string;
  };
  debugSteps?: DebugEventDebugStep[];
  preprocessorStep: {
    attributes?: Record<string, boolean | number | string>;
    headers?: Record<string, string[]>;
  };
  backendResponse: {
    statusCode?: number;
    method?: string;
    path?: string;
    headers?: Record<string, string[]>;
    body?: string;
  };
  metrics?: DebugEventMetrics;
}

interface DebugEventDebugStep {
  policyInstanceId: string;
  policyId: string;
  scope: 'ON_REQUEST' | 'ON_REQUEST_CONTENT' | 'ON_RESPONSE' | 'ON_RESPONSE_CONTENT';
  status: 'COMPLETED' | 'ERROR' | 'SKIPPED' | 'NO_TRANSFORMATION';
  condition?: string;
  error?: {
    contentType?: string;
    key?: string;
    message?: string;
    status?: number;
  };
  duration: number;
  result: Record<string, unknown>;
  stage: 'SECURITY' | 'PLATFORM' | 'PLAN' | 'API';
}

export interface DebugEventMetrics {
  apiResponseTimeMs?: number;
  proxyLatencyMs?: number;
  proxyResponseTimeMs?: number;
}
