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
import { ApiEntityFlowModeEnum } from '@models/v3/ApiEntity';
import { Flow } from '@models/v3/Flow';

export interface NewApiEntity {
  /**
   * API's context path.
   * @type {string}
   * @memberof NewApiEntity
   */
  contextPath: string;
  /**
   * API's description. A short description of your API.
   * @type {string}
   * @memberof NewApiEntity
   */
  description: string;
  /**
   * API's first endpoint (target url).
   * @type {string}
   * @memberof NewApiEntity
   */
  endpoint: string;
  /**
   * API's flow mode.
   * @type {string}
   * @memberof NewApiEntity
   */
  flow_mode?: ApiEntityFlowModeEnum;
  /**
   * API's paths. A json representation of the design of each flow.
   * @type {Array<Flow>}
   * @memberof NewApiEntity
   */
  flows?: Array<Flow>;
  /**
   * API's gravitee definition version
   * @type {string}
   * @memberof NewApiEntity
   */
  gravitee?: string;
  /**
   * API's groups. Used to add team in your API.
   * @type {Array<string>}
   * @memberof NewApiEntity
   */
  groups?: Array<string>;
  /**
   * Api's name. Duplicate names can exists.
   * @type {string}
   * @memberof NewApiEntity
   */
  name: string;
  /**
   * API's paths. A json representation of the design of each path.
   * @type {Array<string>}
   * @memberof NewApiEntity
   */
  paths?: Array<string>;
  /**
   * Api's version. It's a simple string only used in the portal.
   * @type {string}
   * @memberof NewApiEntity
   */
  version: string;
}
