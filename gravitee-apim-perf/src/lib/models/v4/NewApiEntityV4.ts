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
import { EndpointGroupV4 } from '@models/v4/EndpointGroupV4';
import { FlowV4 } from '@models/v4/FlowV4';
import { ListenerV4 } from '@models/v4/ListenerV4';

export interface NewApiEntityV4 {
  /**
   * Api's version. It's a simple string only used in the portal.
   * @type {string}
   * @memberof NewApiEntityV4
   */
  apiVersion: string;
  /**
   * API's gravitee definition version
   * @type {string}
   * @memberof NewApiEntityV4
   */
  definitionVersion: NewApiEntityV4DefinitionVersionEnum;
  /**
   * API's description. A short description of your API.
   * @type {string}
   * @memberof NewApiEntityV4
   */
  description: string;
  /**
   * A list of endpoint describing the endpoints to contact.
   * @type {Array<EndpointGroupV4>}
   * @memberof NewApiEntityV4
   */
  endpointGroups: Array<EndpointGroupV4>;
  /**
   * API's flow mode.
   * @type {string}
   * @memberof NewApiEntityV4
   */
  flowMode?: NewApiEntityV4FlowModeEnum;
  /**
   * A list of flows containing the policies configuration.
   * @type {Array<FlowV4>}
   * @memberof NewApiEntityV4
   */
  flows?: Array<FlowV4>;
  /**
   * API's groups. Used to add team in your API.
   * @type {Array<string>}
   * @memberof NewApiEntityV4
   */
  groups?: Array<string>;
  /**
   * A list of listeners used to describe our you api could be reached.
   * @type {Array<ListenerV4>}
   * @memberof NewApiEntityV4
   */
  listeners: Array<ListenerV4>;
  /**
   * API's name. Duplicate names can exists.
   * @type {string}
   * @memberof NewApiEntityV4
   */
  name: string;
  /**
   * The list of sharding tags associated with this API.
   * @type {Array<string>}
   * @memberof NewApiEntityV4
   */
  tags?: Array<string>;
  /**
   * API's type
   * @type {string}
   * @memberof NewApiEntityV4
   */
  type: NewApiEntityV4TypeEnum;
}

export const enum NewApiEntityV4DefinitionVersionEnum {
  _V2 = 'V2',
  _V4 = 'V4',
}

export const enum NewApiEntityV4FlowModeEnum {
  DEFAULT = 'DEFAULT',
  BEST_MATCH = 'BEST_MATCH',
}

export const enum NewApiEntityV4TypeEnum {
  PROXY = 'PROXY',
  MESSAGE = 'MESSAGE',
}
