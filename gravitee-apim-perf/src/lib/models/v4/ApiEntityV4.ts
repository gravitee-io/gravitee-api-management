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
import { ApiLifecycleState, DefinitionContext, PrimaryOwnerEntity, Visibility, WorkflowState } from '@models/v3/ApiEntity';
import { EndpointGroupV4, ServiceV4 } from '@models/v4/EndpointGroupV4';
import { ListenerV4 } from '@models/v4/ListenerV4';
import { FlowV4 } from '@models/v4/FlowV4';
import { PlanEntityV4 } from '@models/v4/PlanEntityV4';
import { ResponseTemplate } from '@models/v3/ResponseTemplate';

export interface ApiEntityV4 {
  /**
   * Api's version. It's a simple string only used in the portal.
   * @type {string}
   * @memberof ApiEntityV4
   */
  apiVersion?: string;
  /**
   * the API background encoded in base64
   * @type {string}
   * @memberof ApiEntityV4
   */
  background?: string;
  /**
   * the API background url.
   * @type {string}
   * @memberof ApiEntityV4
   */
  backgroundUrl?: string;
  /**
   * the list of categories associated with this API
   * @type {Array<string>}
   * @memberof ApiEntityV4
   */
  categories?: Array<string>;
  /**
   * The date (as a timestamp) when the API was created.
   * @type {Date}
   * @memberof ApiEntityV4
   */
  createdAt?: Date;
  /**
   * API's crossId. Identifies API across environments.
   * @type {string}
   * @memberof ApiEntityV4
   */
  crossId?: string;
  /**
   *
   * @type {DefinitionContext}
   * @memberof ApiEntityV4
   */
  definitionContext?: DefinitionContext;
  /**
   * API's gravitee definition version
   * @type {string}
   * @memberof ApiEntityV4
   */
  definitionVersion?: ApiEntityV4DefinitionVersionEnum;
  /**
   * The last date (as timestamp) when the API was deployed.
   * @type {Date}
   * @memberof ApiEntityV4
   */
  deployedAt?: Date;
  /**
   * API's description. A short description of your API.
   * @type {string}
   * @memberof ApiEntityV4
   */
  description?: string;
  /**
   *
   * @type {boolean}
   * @memberof ApiEntityV4
   */
  disableMembershipNotifications?: boolean;
  /**
   * A list of endpoint describing the endpoints to contact.
   * @type {Array<EndpointGroupV4>}
   * @memberof ApiEntityV4
   */
  endpointGroups?: Array<EndpointGroupV4>;
  /**
   * API's flow mode.
   * @type {string}
   * @memberof ApiEntityV4
   */
  flowMode?: ApiEntityV4FlowModeEnum;
  /**
   * A list of flows containing the policies configuration.
   * @type {Array<FlowV4>}
   * @memberof ApiEntityV4
   */
  flows?: Array<FlowV4>;
  /**
   * API's groups. Used to add team in your API.
   * @type {Array<string>}
   * @memberof ApiEntityV4
   */
  groups?: Array<string>;
  /**
   * API's uuid.
   * @type {string}
   * @memberof ApiEntityV4
   */
  id?: string;
  /**
   * the free list of labels associated with this API
   * @type {Array<string>}
   * @memberof ApiEntityV4
   */
  labels?: Array<string>;
  /**
   *
   * @type {ApiLifecycleState}
   * @memberof ApiEntityV4
   */
  lifecycleState?: ApiLifecycleState;
  /**
   * A list of listeners used to describe our you api could be reached.
   * @type {Array<ListenerV4>}
   * @memberof ApiEntityV4
   */
  listeners?: Array<ListenerV4>;
  /**
   * API's name. Duplicate names can exists.
   * @type {string}
   * @memberof ApiEntityV4
   */
  name?: string;
  /**
   * the API logo encoded in base64
   * @type {string}
   * @memberof ApiEntityV4
   */
  picture?: string;
  /**
   * the API logo URL.
   * @type {string}
   * @memberof ApiEntityV4
   */
  pictureUrl?: string;
  /**
   * A list of plans to apply on the API
   * @type {Array<PlanEntityV4>}
   * @memberof ApiEntityV4
   */
  plans?: Array<PlanEntityV4>;
  /**
   *
   * @type {PrimaryOwnerEntity}
   * @memberof ApiEntityV4
   */
  primaryOwner?: PrimaryOwnerEntity;
  /**
   * A dictionary (could be dynamic) of properties available in the API context.
   * @type {Array<PropertyV4>}
   * @memberof ApiEntityV4
   */
  properties?: Array<PropertyV4>;
  /**
   * The list of API resources used by policies like cache resources or oauth2
   * @type {Array<ResourceV4>}
   * @memberof ApiEntityV4
   */
  resources?: Array<ResourceV4>;
  /**
   * A map that allows you to configure the output of a request based on the event throws by the gateway. Example : Quota exceeded, api-key is missing, ...
   * @type {{ [key: string]: { [key: string]: ResponseTemplate; }; }}
   * @memberof ApiEntityV4
   */
  responseTemplates?: { [key: string]: { [key: string]: ResponseTemplate } };
  /**
   *
   * @type {ApiServicesV4}
   * @memberof ApiEntityV4
   */
  services?: ApiServicesV4;
  /**
   * The status of the API regarding the gateway.
   * @type {string}
   * @memberof ApiEntityV4
   */
  state?: ApiEntityV4StateEnum;
  /**
   * The list of sharding tags associated with this API.
   * @type {Array<string>}
   * @memberof ApiEntityV4
   */
  tags?: Array<string>;
  /**
   * API's type
   * @type {string}
   * @memberof ApiEntityV4
   */
  type?: ApiEntityV4TypeEnum;
  /**
   * The last date (as a timestamp) when the API was updated.
   * @type {Date}
   * @memberof ApiEntityV4
   */
  updatedAt?: Date;
  /**
   *
   * @type {Visibility}
   * @memberof ApiEntityV4
   */
  visibility?: Visibility;
  /**
   *
   * @type {WorkflowState}
   * @memberof ApiEntityV4
   */
  workflowState?: WorkflowState;
}

export const enum ApiEntityV4DefinitionVersionEnum {
  _1_0_0 = '1.0.0',
  _2_0_0 = '2.0.0',
  _4_0_0 = '4.0.0',
}

export const enum ApiEntityV4FlowModeEnum {
  DEFAULT = 'DEFAULT',
  BEST_MATCH = 'BEST_MATCH',
}

export interface PropertyV4 {
  /**
   *
   * @type {boolean}
   * @memberof PropertyV4
   */
  dynamic?: boolean;
  /**
   *
   * @type {boolean}
   * @memberof PropertyV4
   */
  encrypted?: boolean;
  /**
   *
   * @type {string}
   * @memberof PropertyV4
   */
  key: string;
  /**
   *
   * @type {string}
   * @memberof PropertyV4
   */
  value: string;
}

export interface ResourceV4 {
  /**
   *
   * @type {any}
   * @memberof ResourceV4
   */
  configuration: any;
  /**
   *
   * @type {boolean}
   * @memberof ResourceV4
   */
  enabled?: boolean;
  /**
   *
   * @type {string}
   * @memberof ResourceV4
   */
  name: string;
  /**
   *
   * @type {string}
   * @memberof ResourceV4
   */
  type: string;
}

export interface ApiServicesV4 {
  /**
   *
   * @type {ServiceV4}
   * @memberof ApiServicesV4
   */
  dynamicProperty?: ServiceV4;
}

export const enum ApiEntityV4TypeEnum {
  SYNC = 'SYNC',
  ASYNC = 'ASYNC',
}

export const enum ApiEntityV4StateEnum {
  INITIALIZED = 'INITIALIZED',
  STOPPED = 'STOPPED',
  STOPPING = 'STOPPING',
  STARTED = 'STARTED',
  CLOSED = 'CLOSED',
}
