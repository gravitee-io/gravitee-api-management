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
import { get, isEmpty, remove } from 'lodash';

import { ApiV4, EndpointGroupV4, EndpointV4 } from '../../../../../entities/management-api-v2';
import { Tenant } from '../../../../../entities/tenant/tenant';

export type EndpointGroup = {
  name: string;
  type: string;
  loadBalancerType: string;
  generalColumnName?: string;
  endpoints: Endpoint[];
  endpointsDisplayedColumns: string[];
};

export type Endpoint = {
  name: string;
  nameBadge?: {
    title: string;
    tooltip?: string;
  };
  general?: string;
  options: {
    class: string;
    tooltip: string;
    textContent: string;
  }[];
  tenants?: string[];
  weight: number;
  configuration?: any;
};

export const toEndpoints = (api: ApiV4, tenants: Tenant[]): EndpointGroup[] => {
  return toEndpointsFromApiV4(api, tenants);
};

const toEndpointsFromApiV4 = (api: ApiV4, tenants: Tenant[]): EndpointGroup[] => {
  if (!api.endpointGroups) {
    return [];
  }

  const isNativeKafkaApi = api.type === 'NATIVE' && api.listeners.some(listener => listener.type === 'KAFKA');

  return api.endpointGroups.flatMap((endpointGroup, endpointGroupIndex) => {
    const loadBalancerType = endpointGroup?.loadBalancer?.type?.replace(/_/g, ' ');
    const endpointsDisplayedColumns = ['drag-icon', 'name', 'general', 'options', 'tenants', 'weight', 'actions'];
    if (api.type === 'NATIVE') {
      remove(endpointsDisplayedColumns, o => o === 'weight');
    }
    const partialEndpointGroup = {
      name: endpointGroup.name,
      type: endpointGroup.type,
      loadBalancerType: loadBalancerType ? loadBalancerType.charAt(0).toUpperCase() + loadBalancerType.slice(1).toLowerCase() : '',
      generalColumnName: toGeneralColumnName(api, endpointGroup),
      endpoints:
        endpointGroup.endpoints && endpointGroup.endpoints.length > 0
          ? endpointGroup.endpoints.map((endpoint, endpointIndex) => {
              let nameBadge: Endpoint['nameBadge'] = undefined;
              if (isNativeKafkaApi && endpointGroupIndex === 0 && endpointIndex === 0) {
                nameBadge = {
                  title: 'Default',
                  tooltip: 'The default endpoint used by the API is the first one',
                };
              }
              const options = toEndpointOptions(api, endpointGroup, endpoint);

              return {
                name: endpoint.name,
                general: toGeneralInfo(api, endpoint),
                nameBadge,
                options,
                tenants: endpoint.tenants?.map(tenantId => tenants.find(tenant => tenant.id === tenantId)?.name ?? tenantId),
                weight: endpoint.weight,
                configuration: endpoint.configuration,
              } satisfies Endpoint;
            })
          : [],
      endpointsDisplayedColumns,
    };

    if (!partialEndpointGroup.endpoints.some(endpoint => endpoint.options.length > 0)) {
      remove(endpointsDisplayedColumns, o => o === 'options');
    }

    if (!partialEndpointGroup.endpoints.some(endpoint => endpoint.tenants?.length > 0)) {
      remove(endpointsDisplayedColumns, o => o === 'tenants');
    }

    if (isEmpty(partialEndpointGroup.generalColumnName)) {
      remove(endpointsDisplayedColumns, o => o === 'general');
    }

    return {
      ...partialEndpointGroup,
      endpointsDisplayedColumns,
    } satisfies EndpointGroup;
  });
};

const toGeneralColumnName = (api: ApiV4, endpointGroup: EndpointGroupV4): string | undefined => {
  switch (api.type + '.' + endpointGroup.type) {
    case 'PROXY.http-proxy':
      return 'Target URL';
    case 'NATIVE.native-kafka':
      return 'Bootstrap Servers';
    case 'MESSAGE.kafka':
      return 'Bootstrap Servers';
  }
};

const toGeneralInfo = (api: ApiV4, endpoint: EndpointV4): string | undefined => {
  switch (api.type + '.' + endpoint.type) {
    case 'PROXY.http-proxy':
      return get(endpoint.configuration, 'target', '');
    case 'NATIVE.native-kafka':
      return get(endpoint.configuration, 'bootstrapServers', '');
    case 'MESSAGE.kafka':
      return get(endpoint.configuration, 'bootstrapServers', '');
  }
};

const toEndpointOptions = (api: ApiV4, endpointGroup: EndpointGroupV4, endpoint: EndpointV4): Endpoint['options'] => {
  switch (api.type + '.' + endpoint.type) {
    case 'PROXY.http-proxy': {
      const groupHealthCheckEnabled = get(endpointGroup, 'services.healthCheck.enabled', false);
      const endpointHealthCheckEnabled = get(endpoint, 'services.healthCheck.enabled', false);
      if (groupHealthCheckEnabled || endpointHealthCheckEnabled) {
        return [
          {
            class: 'gio-badge-neutral',
            tooltip: endpointHealthCheckEnabled
              ? 'Health check enabled by endpoint configuration'
              : 'Health check enabled via inherited group configuration',
            textContent: 'Health Check',
          },
        ];
      }
      return [];
    }
    case 'NATIVE.native-kafka': {
      const groupSecurityProtocol = get(endpointGroup.sharedConfiguration, 'security.protocol');
      const endpointSecurityProtocol = endpoint.inheritConfiguration
        ? undefined
        : get(endpoint.sharedConfigurationOverride, 'security.protocol');
      if (groupSecurityProtocol || endpointSecurityProtocol) {
        return [
          {
            class: 'gio-badge-neutral',
            tooltip: endpointSecurityProtocol
              ? `Security protocol override by endpoint configuration`
              : 'Security protocol inherited from group configuration',
            textContent: endpointSecurityProtocol ?? groupSecurityProtocol,
          },
        ];
      }
    }
  }
  return [];
};
