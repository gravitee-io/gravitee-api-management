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
export interface EnvLog {
  timestamp: string;
  api: string;
  type: string;
  application: string;
  method: string;
  path: string;
  status: number;
  responseTime: string;
  gateway: string;
}

/**
 * Used for generating custom log objects for tests
 */
export const fakeEnvLog = (attributes?: Partial<EnvLog>): EnvLog => {
  const defaultLog: EnvLog = {
    timestamp: '15/06/2025 12:00:00',
    api: '/api/v1/event-types',
    type: 'HTTP Proxy',
    application: 'My mobile app',
    method: 'GET',
    path: '/api/v1/event-types',
    status: 200,
    responseTime: '44ms',
    gateway: 'API Gateway 1',
  };

  return {
    ...defaultLog,
    ...attributes,
  };
};

export const fakeEnvLogs = (): EnvLog[] => {
  return [
    {
      timestamp: '15/06/2025 12:00:00',
      api: '/api/v1/event-types',
      type: 'HTTP Proxy',
      application: 'My mobile app',
      method: 'PATCH',
      path: '/api/v1/event-types',
      status: 200,
      responseTime: '44ms',
      gateway: 'API Gateway 1',
    },
    {
      timestamp: '16/06/2025 13:15:00',
      api: '/api/v1/event-types',
      type: 'HTTP Proxy',
      application: 'My mobile app',
      method: 'DELETE',
      path: '/api/v1/event-types/{id}',
      status: 400,
      responseTime: '0ms',
      gateway: 'API Gateway 2',
    },
    {
      timestamp: '17/06/2025 14:30:00',
      api: '/api/v1/event-types',
      type: 'HTTP Proxy',
      application: 'My mobile app',
      method: 'POST',
      path: '/api/v1/event-types',
      status: 201,
      responseTime: '1ms',
      gateway: 'API Gateway 3',
    },
    {
      timestamp: '18/06/2025 15:45:00',
      api: '/api/v1/event-types',
      type: 'HTTP Proxy',
      application: 'My mobile app',
      method: 'PUT',
      path: '/api/v1/event-types/{id}',
      status: 301,
      responseTime: '8ms',
      gateway: 'API Gateway 4',
    },
    {
      timestamp: '19/06/2025 17:00:00',
      api: '/api/v1/event-types',
      type: 'HTTP Proxy',
      application: 'My mobile app',
      method: 'GET',
      path: '/api/v1/event-types/{id}',
      status: 500,
      responseTime: '500ms',
      gateway: 'API Gateway 5',
    },
  ];
};
