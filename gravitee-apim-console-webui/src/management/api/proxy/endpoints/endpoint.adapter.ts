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

import { ProxyGroup, ProxyGroupEndpoint } from '../../../../entities/proxy';

export type EndpointGroup = {
  name: string;
  endpoints: Endpoint[];
};

export type Endpoint = {
  name: string;
  type: string;
  target: string;
  weight: number;
};

export const toEndpoints = (endpointGroups: ProxyGroup[]): EndpointGroup[] => {
  if (!endpointGroups) {
    return [];
  }

  return endpointGroups.flatMap((endpointGroup) => {
    return {
      name: endpointGroup.name,
      endpoints:
        endpointGroup.endpoints && endpointGroup.endpoints.length > 0
          ? endpointGroup.endpoints.map((endpoint: ProxyGroupEndpoint) => ({
              name: endpoint.name,
              inherit: endpoint.inherit,
              type: endpoint.type,
              target: endpoint.target,
              weight: endpoint.weight,
              isBackup: endpoint.backup,
            }))
          : [],
    };
  });
};
