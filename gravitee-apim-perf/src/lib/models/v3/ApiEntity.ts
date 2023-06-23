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
import { Flow } from '@lib/models/v3/Flow';
import { PlanEntity } from '@lib/models/v3/PlanEntity';
import { Property } from '@lib/models/v3/Property';
import { Proxy } from '@lib/models/v3/Proxy';
import { Resource } from '@lib/models/v3/Resource';
import { ResponseTemplate } from '@lib/models/v3/ResponseTemplate';
import { Services } from '@lib/models/v3/Services';
import { Rule } from '@lib/models/v3/Rule';

export interface ApiEntity {
  /**
   * the API background encoded in base64
   * @type {string}
   * @memberof ApiEntity
   */
  background?: string;
  /**
   * the API background url.
   * @type {string}
   * @memberof ApiEntity
   */
  background_url?: string;
  /**
   * the list of categories associated with this API
   * @type {Array<string>}
   * @memberof ApiEntity
   */
  categories?: Array<string>;
  /**
   * API's context path.
   * @type {string}
   * @memberof ApiEntity
   */
  context_path?: string;
  /**
   * The date (as a timestamp) when the API was created.
   * @type {Date}
   * @memberof ApiEntity
   */
  created_at?: Date;
  /**
   * API's crossId. Identifies API across environments.
   * @type {string}
   * @memberof ApiEntity
   */
  crossId?: string;
  /**
   *
   * @type {DefinitionContext}
   * @memberof ApiEntity
   */
  definition_context?: DefinitionContext;
  /**
   * The last date (as timestamp) when the API was deployed.
   * @type {Date}
   * @memberof ApiEntity
   */
  deployed_at?: Date;
  /**
   * API's description. A short description of your API.
   * @type {string}
   * @memberof ApiEntity
   */
  description?: string;
  /**
   *
   * @type {boolean}
   * @memberof ApiEntity
   */
  disable_membership_notifications?: boolean;
  /**
   *
   * @type {Array<ApiEntrypointEntity>}
   * @memberof ApiEntity
   */
  entrypoints?: Array<ApiEntrypointEntity>;
  /**
   *
   * @type {ExecutionMode}
   * @memberof ApiEntity
   */
  execution_mode?: 'v3' | 'v4-emulation-engine';
  /**
   * API's flow mode.
   * @type {string}
   * @memberof ApiEntity
   */
  flow_mode?: ApiEntityFlowModeEnum;
  /**
   * a list of flows (the policies configuration)
   * @type {Array<Flow>}
   * @memberof ApiEntity
   */
  flows?: Array<Flow>;
  /**
   * API's gravitee definition version
   * @type {string}
   * @memberof ApiEntity
   */
  gravitee?: string;
  /**
   * API's groups. Used to add team in your API.
   * @type {Array<string>}
   * @memberof ApiEntity
   */
  groups?: Array<string>;
  /**
   * API's uuid.
   * @type {string}
   * @memberof ApiEntity
   */
  id?: string;
  /**
   * the free list of labels associated with this API
   * @type {Array<string>}
   * @memberof ApiEntity
   */
  labels?: Array<string>;
  /**
   *
   * @type {ApiLifecycleState}
   * @memberof ApiEntity
   */
  lifecycle_state?: ApiLifecycleState;
  /**
   * API's name. Duplicate names can exists.
   * @type {string}
   * @memberof ApiEntity
   */
  name?: string;
  /**
   *
   * @type {PrimaryOwnerEntity}
   * @memberof ApiEntity
   */
  owner?: PrimaryOwnerEntity;
  /**
   * A list of paths used to aggregate data in analytics
   * @type {Array<string>}
   * @memberof ApiEntity
   */
  path_mappings?: Array<string>;
  /**
   * a map where you can associate a path to a configuration (the policies configuration)
   * @type {{ [key: string]: Array<Rule>; }}
   * @memberof ApiEntity
   */
  paths?: { [key: string]: Array<Rule> };
  /**
   * the API logo encoded in base64
   * @type {string}
   * @memberof ApiEntity
   */
  picture?: string;
  /**
   * the API logo url.
   * @type {string}
   * @memberof ApiEntity
   */
  picture_url?: string;
  /**
   * a list of plans with flows (the policies configuration)
   * @type {Array<PlanEntity>}
   * @memberof ApiEntity
   */
  plans?: Array<PlanEntity>;
  /**
   * A dictionary (could be dynamic) of properties available in the API context.
   * @type {Array<Property>}
   * @memberof ApiEntity
   */
  properties?: Array<Property>;
  /**
   *
   * @type {Proxy}
   * @memberof ApiEntity
   */
  proxy: Proxy;
  /**
   * The list of API resources used by policies like cache resources or oauth2
   * @type {Array<Resource>}
   * @memberof ApiEntity
   */
  resources?: Array<Resource>;
  /**
   * A map that allows you to configure the output of a request based on the event throws by the gateway. Example : Quota exceeded, api-ky is missing, ...
   * @type {{ [key: string]: { [key: string]: ResponseTemplate; }; }}
   * @memberof ApiEntity
   */
  response_templates?: { [key: string]: { [key: string]: ResponseTemplate } };
  /**
   *
   * @type {Services}
   * @memberof ApiEntity
   */
  services?: Services;
  /**
   * The status of the API regarding the gateway.
   * @type {string}
   * @memberof ApiEntity
   */
  state?: ApiEntityStateEnum;
  /**
   * the list of sharding tags associated with this API.
   * @type {Array<string>}
   * @memberof ApiEntity
   */
  tags?: Array<string>;
  /**
   * The last date (as a timestamp) when the API was updated.
   * @type {Date}
   * @memberof ApiEntity
   */
  updated_at?: Date;
  /**
   * Api's version. It's a simple string only used in the portal.
   * @type {string}
   * @memberof ApiEntity
   */
  version?: string;
  /**
   *
   * @type {Visibility}
   * @memberof ApiEntity
   */
  visibility?: Visibility;
  /**
   *
   * @type {WorkflowState}
   * @memberof ApiEntity
   */
  workflow_state?: WorkflowState;
}

export interface DefinitionContext {
  /**
   *
   * @type {string}
   * @memberof DefinitionContext
   */
  mode?: string;
  /**
   *
   * @type {string}
   * @memberof DefinitionContext
   */
  origin?: string;
}

export interface ApiEntrypointEntity {
  /**
   *
   * @type {string}
   * @memberof ApiEntrypointEntity
   */
  host?: string;
  /**
   *
   * @type {Array<string>}
   * @memberof ApiEntrypointEntity
   */
  tags?: Array<string>;
  /**
   *
   * @type {string}
   * @memberof ApiEntrypointEntity
   */
  target?: string;
}

export interface PrimaryOwnerEntity {
  /**
   * The user or group display name.
   * @type {string}
   * @memberof PrimaryOwnerEntity
   */
  displayName?: string;
  /**
   * The user or group email.
   * @type {string}
   * @memberof PrimaryOwnerEntity
   */
  email?: string;
  /**
   * The user or group id.
   * @type {string}
   * @memberof PrimaryOwnerEntity
   */
  id?: string;
  /**
   * The primary owner type
   * @type {string}
   * @memberof PrimaryOwnerEntity
   */
  type?: string;
}

export const enum ApiEntityFlowModeEnum {
  DEFAULT = 'DEFAULT',
  BEST_MATCH = 'BEST_MATCH',
}

export const enum ApiLifecycleState {
  CREATED = 'CREATED',
  PUBLISHED = 'PUBLISHED',
  UNPUBLISHED = 'UNPUBLISHED',
  DEPRECATED = 'DEPRECATED',
  ARCHIVED = 'ARCHIVED',
}

export const enum ApiEntityStateEnum {
  INITIALIZED = 'INITIALIZED',
  STOPPED = 'STOPPED',
  STOPPING = 'STOPPING',
  STARTED = 'STARTED',
  CLOSED = 'CLOSED',
}

export const enum Visibility {
  PUBLIC = 'PUBLIC',
  PRIVATE = 'PRIVATE',
}

export const enum WorkflowState {
  DRAFT = 'DRAFT',
  IN_REVIEW = 'IN_REVIEW',
  REQUEST_FOR_CHANGES = 'REQUEST_FOR_CHANGES',
  REVIEW_OK = 'REVIEW_OK',
}

export const enum LifecycleAction {
  START = 'START',
  STOP = 'STOP',
}
