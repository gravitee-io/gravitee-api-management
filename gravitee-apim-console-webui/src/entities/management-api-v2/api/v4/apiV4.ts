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

import { Analytics } from './analytics';
import { ApiServices } from './apiServices';
import { ApiType } from './apiType';
import { Listener } from './listener';
import { FlowV4 } from './flowV4';
import { EndpointGroupV4 } from './endpointGroupV4';
import { FlowExecution } from './flowExecution';
import { Failover } from './failover';

import { GenericApi } from '../baseApi';
import {MCP} from "./mcp";

export interface ApiV4 extends GenericApi {
  definitionVersion: 'V4';
  type: ApiType;
  /**
   * The list of listeners associated with this API.
   */
  listeners?: Listener[];
  endpointGroups?: EndpointGroupV4[];
  analytics?: Analytics;
  flowExecution?: FlowExecution;
  flows?: FlowV4[];
  services?: ApiServices;
  failover?: Failover;
  mcp?: MCP;
}
