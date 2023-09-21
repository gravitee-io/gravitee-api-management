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

import { EndpointV2 } from './endpointV2';
import { HttpClientSslOptions } from './httpClientSslOptions';
import { HttpProxy } from './httpProxy';
import { HttpClientOptions } from './httpClientOptions';
import { EndpointDiscoveryService } from './endpointDiscoveryService';
import { HttpHeader } from './httpHeader';

import { LoadBalancer } from '../loadBalancer';

export type EndpointGroupServicesV2 = {
  discovery?: EndpointDiscoveryService;
};

export interface EndpointGroupV2 {
  /**
   * Endpoint group's name.
   */
  name?: string;
  /**
   * The list of endpoints associated with this endpoint group.
   */
  endpoints?: EndpointV2[];
  loadBalancer?: LoadBalancer;
  services?: EndpointGroupServicesV2;
  httpProxy?: HttpProxy;
  httpClientOptions?: HttpClientOptions;
  httpClientSslOptions?: HttpClientSslOptions;
  headers?: HttpHeader[];
}
