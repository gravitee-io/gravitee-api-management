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

import { Proxy } from './proxy';
import { ServicesV2 } from './servicesV2';
import { FlowV2 } from './flowV2';
import { ExecutionMode } from './executionMode';

import { BaseApi } from '../baseApi';
import { ApiEntrypoint } from '../apiEntrypoint';
import { FlowMode } from '../flowMode';

export interface ApiV2 extends BaseApi {
  definitionVersion: 'V2';
  /**
   * The environment's uuid.
   */
  environmentId?: string;
  executionMode?: ExecutionMode;
  /**
   * The context path of the API.
   */
  contextPath?: string;
  proxy?: Proxy;
  flowMode?: FlowMode;
  /**
   * The list of flows associated with this API.
   */
  flows?: FlowV2[];
  services?: ServicesV2;
  /**
   * The list of path mappings associated with this API.
   */
  pathMappings?: string[];
  /**
   * The list of entrypoints associated with this API.
   */
  entrypoints?: ApiEntrypoint[];
}
