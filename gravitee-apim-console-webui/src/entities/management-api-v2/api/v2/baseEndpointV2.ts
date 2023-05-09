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

import { EndpointHealthCheckService } from './endpointHealthCheckService';
import { EndpointStatus } from './endpointStatus';

export interface BaseEndpointV2 {
  /**
   * The name of the endpoint
   */
  name?: string;
  /**
   * The target of the endpoint
   */
  target?: string;
  /**
   * The weight of the endpoint
   */
  weight?: number;
  /**
   * Is the endpoint a backup or not.
   */
  backup?: boolean;
  status?: EndpointStatus;
  /**
   * The list of tenants associated with this endpoint.
   */
  tenants?: string[];
  /**
   * The type of the endpoint.
   */
  type: string;
  /**
   * Inherit the configuration of the parent endpoint group.
   */
  inherit?: boolean;
  healthCheck?: EndpointHealthCheckService;
}
