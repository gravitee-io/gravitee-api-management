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
import { ApiV4 } from '../../../../entities/management-api-v2';

export type EndpointGroup = {
  name: string;
  type: string;
  loadBalancerType: string;
  endpoints: Endpoint[];
};

export type Endpoint = {
  name: string;
  options: {
    healthCheck: boolean;
  };
  weight: number;
};

export const toEndpoints = (api: ApiV4): EndpointGroup[] => {
  return toEndpointsFromApiV4(api);
};

const toEndpointsFromApiV4 = (api: ApiV4): EndpointGroup[] => {
  if (!api.endpointGroups) {
    return [];
  }

  return api.endpointGroups.flatMap((endpointGroup) => {
    const loadBalancerType = endpointGroup?.loadBalancer?.type?.replace(/_/g, ' ');
    return {
      name: endpointGroup.name,
      type: endpointGroup.type,
      loadBalancerType: loadBalancerType[0].toUpperCase() + loadBalancerType.substring(1).toLowerCase(),
      endpoints:
        endpointGroup.endpoints && endpointGroup.endpoints.length > 0
          ? endpointGroup.endpoints.map((endpoint) => ({
              name: endpoint.name,
              options: {
                healthCheck: false, // TODO: check it somewhere in the group configuration
              },
              weight: endpoint.weight,
            }))
          : [],
    };
  });
};
