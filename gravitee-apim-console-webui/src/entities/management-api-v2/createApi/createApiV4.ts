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

import { CreateBaseApi } from './createBaseApi';

import { Analytics, ApiType, EndpointGroupV4, FlowExecution, FlowV4, Listener } from '../api';

export interface CreateApiV4 extends CreateBaseApi {
  type?: ApiType;
  /**
   * The list of sharding tags associated with this API.
   */
  tags?: string[];
  /**
   * The list of listeners associated with this API.
   */
  listeners: Listener[];
  endpointGroups: EndpointGroupV4[];
  analytics?: Analytics;
  flowExecution?: FlowExecution;
  flows?: FlowV4[];
}
