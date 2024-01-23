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

import { ProxyGroupServiceDiscoveryConfiguration } from './service-discovery/api-proxy-group-service-discovery.model';

import { EndpointGroupV2, LoadBalancerTypeEnum } from '../../../../../entities/management-api-v2';
import { EndpointHttpConfigValue } from '../../components/endpoint-http-config/endpoint-http-config.component';

export const toProxyGroup = (
  group: EndpointGroupV2,
  generalData: { name: string; loadBalancerType: LoadBalancerTypeEnum },
  configuration: EndpointHttpConfigValue,
  serviceDiscoveryConfiguration: ProxyGroupServiceDiscoveryConfiguration,
): EndpointGroupV2 => {
  let updatedGroup: EndpointGroupV2 = {
    ...group,
    name: generalData.name,
    loadBalancer: {
      type: generalData.loadBalancerType,
    },
  };

  if (configuration) {
    updatedGroup = {
      ...updatedGroup,
      httpClientOptions: configuration.httpClientOptions,
      httpClientSslOptions: configuration.httpClientSslOptions,
      headers: configuration.headers,
      // Not send empty proxy if not configured
      httpProxy: configuration.httpProxy?.enabled ? configuration.httpProxy : undefined,
    };
  }

  if (serviceDiscoveryConfiguration) {
    updatedGroup = {
      ...updatedGroup,
      services: {
        discovery: {
          ...serviceDiscoveryConfiguration.discovery,
        },
      },
    };
  } else {
    updatedGroup = {
      ...updatedGroup,
      services: {
        discovery: {
          enabled: false,
        },
      },
    };
  }

  return updatedGroup;
};
