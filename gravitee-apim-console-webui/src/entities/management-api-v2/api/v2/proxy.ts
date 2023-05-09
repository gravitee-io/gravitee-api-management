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

import { EndpointGroupV2 } from './endpointGroupV2';
import { Failover } from './failover';
import { LoggingV2 } from './loggingV2';
import { VirtualHost } from './virtualHost';

import { Cors } from '../cors';

export interface Proxy {
  virtualHosts?: VirtualHost[];
  groups?: EndpointGroupV2[];
  failover?: Failover;
  cors?: Cors;
  logging?: LoggingV2;
  stripContextPath?: boolean;
  preserveHost?: boolean;
  servers?: string[];
}
