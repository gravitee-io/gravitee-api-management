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

import { ProxyGroupServiceDiscoveryConfiguration } from './edit/service-discovery/api-proxy-group-service-discovery.model';

import { ProxyConfiguration, ProxyGroup, ProxyGroupLoadBalancerType } from '../../../../../entities/proxy';

export const toProxyGroup = (
  group: ProxyGroup,
  generalData: { name: string; loadBalancerType: ProxyGroupLoadBalancerType },
  configuration: ProxyConfiguration,
  serviceDiscoveryConfiguration: ProxyGroupServiceDiscoveryConfiguration,
): ProxyGroup => {
  let updatedGroup: ProxyGroup = {
    ...group,
    name: generalData.name,
    load_balancing: {
      type: generalData.loadBalancerType,
    },
  };

  if (configuration) {
    updatedGroup = {
      ...updatedGroup,
      http: configuration.http,
      ssl: configuration.ssl,
      headers: configuration.headers,
      proxy: configuration.proxy,
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
