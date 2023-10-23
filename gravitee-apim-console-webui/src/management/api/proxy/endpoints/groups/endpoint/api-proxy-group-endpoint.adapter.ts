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

import { EndpointGeneralData } from './edit/general/api-proxy-group-endpoint-edit-general.component';
import { EndpointConfigurationData } from './edit/configuration/api-proxy-group-endpoint-configuration.component';

import { EndpointHealthCheckService, EndpointV2 } from '../../../../../../entities/management-api-v2';

export const toProxyGroupEndpoint = (
  endpoint: EndpointV2,
  generalData: EndpointGeneralData,
  configurationData: EndpointConfigurationData,
  healthCheck: EndpointHealthCheckService,
): EndpointV2 => {
  let updatedEndpoint: EndpointV2 = {
    ...endpoint,
    ...generalData,
    healthCheck: healthCheck,
  };

  if (!configurationData.inherit) {
    updatedEndpoint = {
      ...updatedEndpoint,
      inherit: false,
      httpProxy: configurationData.configuration.httpProxy,
      httpClientOptions: configurationData.configuration.httpClientOptions,
      httpClientSslOptions: configurationData.configuration.httpClientSslOptions,
      headers: configurationData.configuration.headers,
    };
  } else {
    updatedEndpoint = {
      ...updatedEndpoint,
      inherit: true,
    };
    delete updatedEndpoint.httpClientOptions;
    delete updatedEndpoint.httpClientSslOptions;
    delete updatedEndpoint.headers;
    delete updatedEndpoint.httpProxy;
  }

  return updatedEndpoint;
};
