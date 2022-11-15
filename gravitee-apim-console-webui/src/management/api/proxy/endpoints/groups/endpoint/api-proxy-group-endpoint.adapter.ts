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

import { EndpointGeneralData } from './edit/general/api-proxy-group-endpoint-edit-general.model';
import { EndpointConfigurationData } from './edit/configuration/api-proxy-group-endpoint-edit-configuration.model';

import { ProxyGroupEndpoint } from '../../../../../../entities/proxy';
import { HealthCheck } from '../../../../../../entities/health-check';

export const toProxyGroupEndpoint = (
  endpoint: ProxyGroupEndpoint,
  generalData: EndpointGeneralData,
  configurationData: EndpointConfigurationData,
  healthCheck: HealthCheck,
): ProxyGroupEndpoint => {
  let updatedEndpoint: ProxyGroupEndpoint = {
    ...endpoint,
    ...generalData,
    healthcheck: healthCheck,
  };

  if (!configurationData.inherit) {
    updatedEndpoint = {
      ...updatedEndpoint,
      inherit: false,
      http: configurationData.http,
      ssl: configurationData.ssl,
      headers: configurationData.headers,
      proxy: configurationData.proxy,
    };
  } else {
    updatedEndpoint = {
      ...updatedEndpoint,
      inherit: true,
    };
    delete updatedEndpoint.http;
    delete updatedEndpoint.ssl;
    delete updatedEndpoint.headers;
    delete updatedEndpoint.proxy;
  }

  return updatedEndpoint;
};
