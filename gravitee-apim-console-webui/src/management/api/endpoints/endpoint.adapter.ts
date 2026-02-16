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

import { ApiV2, HttpEndpointV2 } from '../../../entities/management-api-v2';

export type EndpointGroup = {
  name: string;
  endpoints: Endpoint[];
};

export type Endpoint = {
  name: string;
  type: string;
  target: string;
  weight: number;
  isBackup: boolean;
  healthcheck: 'inherit-enable' | 'inherit-disable' | 'enable' | 'disable' | 'none';
};

export const toEndpoints = (api: ApiV2): EndpointGroup[] => {
  return toEndpointsFromApiV2(api);
};

const toEndpointsFromApiV2 = (api: ApiV2): EndpointGroup[] => {
  if (!api.proxy.groups) {
    return [];
  }

  const hasApiHealthCheckService = api.services && api.services.healthCheck && api.services.healthCheck.enabled;

  return api.proxy.groups.flatMap(endpointGroup => {
    return {
      name: endpointGroup.name,
      endpoints:
        endpointGroup.endpoints && endpointGroup.endpoints.length > 0
          ? endpointGroup.endpoints.map(endpoint => ({
              name: endpoint.name,
              type: endpoint.type,
              target: endpoint.target,
              weight: endpoint.weight,
              isBackup: endpoint.backup,
              healthcheck: hasApiV2HealthCheck(hasApiHealthCheckService, endpoint),
              inherit: endpoint.inherit,
            }))
          : [],
    };
  });
};

const hasApiV2HealthCheck = (hasApiHealthCheckService: boolean, endpoint: HttpEndpointV2): Endpoint['healthcheck'] => {
  if (endpoint.backup || (endpoint.type.toLowerCase() !== 'http' && endpoint.type.toLowerCase() !== 'grpc')) {
    return 'none';
  }

  if (!endpoint.healthCheck || (endpoint.healthCheck.inherit !== false && endpoint.healthCheck.enabled !== false)) {
    return hasApiHealthCheckService ? 'inherit-enable' : 'inherit-disable';
  }

  return endpoint.healthCheck.enabled || endpoint.healthCheck.enabled === undefined ? 'enable' : 'disable';
};
