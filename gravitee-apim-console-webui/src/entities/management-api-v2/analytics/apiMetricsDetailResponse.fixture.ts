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
import { ApiMetricsDetailResponse } from './apiMetricsDetailResponse';

export const fakeApiMetricResponse = (modifier?: Partial<ApiMetricsDetailResponse>): ApiMetricsDetailResponse => {
  const base: ApiMetricsDetailResponse = {
    timestamp: '2025-08-01T17:29:20.385+02:00',
    apiId: '2ebe3deb-1859-4d5b-be3d-eb1859dd5b16',
    requestId: '39107cc9-b8bf-4f16-907c-c9b8bf8f16fb',
    transactionId: '39107cc9-b8bf-4f16-907c-c9b8bf8f16fb',
    host: 'localhost:8082',
    plan: {
      id: 'ccefeab8-2f7c-45dc-afea-b82f7c75dc1a',
      name: 'Default Keyless (UNSECURED)',
      description: 'Default unsecured plan',
      apiId: '2ebe3deb-1859-4d5b-be3d-eb1859dd5b16',
      security: {
        type: 'KEY_LESS',
        configuration: {},
      },
      mode: 'STANDARD',
    },
    application: {
      id: '1',
      name: 'Unknown',
      apiKeyMode: 'UNSPECIFIED',
    },
    gateway: {
      id: 'b504bb7b-8b6e-426f-84bb-7b8b6e626f3f',
      hostname: 'Mac.lan',
      ip: '192.168.1.139',
    },
    uri: '/v4/echo',
    status: 202,
    requestContentLength: 0,
    responseContentLength: 276,
    remoteAddress: '0:0:0:0:0:0:0:1',
    gatewayLatency: 3,
    gatewayResponseTime: 276,
    endpointResponseTime: 150,
    method: 'GET',
    endpoint: 'https://example.endpoint.com',
  };

  return {
    ...base,
    ...modifier,
  };
};
