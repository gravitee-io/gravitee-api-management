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

import { DefinitionVersion } from './definitionVersion';
import { ApiLifecycleState } from './apiLifecycleState';
import { ApiVisibility } from './apiVisibility';
import { ApiWorkflowState } from './apiWorkflowState';
import { DefinitionContext } from './definitionContext';
import { Property } from './property';
import { Resource } from './resource';
import { ResponseTemplate } from './responseTemplate';
import { PrimaryOwner } from './primaryOwner';
import { OriginContext } from './originContext';

export interface BaseApi {
  /**
   * API's uuid.
   */
  id: string;
  /**
   * API's name. Duplicate names can exists.
   */
  name: string;
  /**
   * API's description. A short description of your API.
   */
  description?: string;
}

export interface GenericApi extends BaseApi {
  /**
   * API's crossId. Identifies API across environments.
   */
  crossId?: string;
  /**
   * API's version. It's a simple string only used in the portal.
   */
  apiVersion: string;
  definitionVersion?: DefinitionVersion;
  /**
   * The last date (as timestamp) when the API was deployed.
   */
  deployedAt?: Date;
  /**
   * The date (as timestamp) when the API was created.
   */
  createdAt?: Date;
  /**
   * The last date (as timestamp) when the API was updated.
   */
  updatedAt?: Date;
  /**
   * Disable membership notifications.
   */
  disableMembershipNotifications?: boolean;
  /**
   * When true, allows an application to subscribe to more than one JWT/OAuth2 plan (V4 only).
   */
  allowMultiJwtOauth2Subscriptions?: boolean;
  /**
   * API's groups. Used to add team in your API.
   */
  groups?: string[];
  /**
   * The status of the API regarding the gateway(s).
   */
  state?: ApiState;

  /**
   * The deployment state of the API regarding the gateway(s).
   */
  deploymentState?: ApiDeploymentState;

  /**
   * The visibility of the resource regarding the portal.
   */
  visibility?: ApiVisibility;
  /**
   * The free list of labels associated with this API.
   */
  labels?: string[];
  lifecycleState?: ApiLifecycleState;
  /**
   * The list of sharding tags associated with this API.
   */
  tags?: string[];
  /**
   * The list of category ids associated with this API.
   */
  categories?: string[];
  definitionContext?: DefinitionContext;
  originContext: OriginContext;
  /**
   * The status of the API regarding the review feature.
   */
  workflowState?: ApiWorkflowState;
  responseTemplates?: { [key: string]: { [key: string]: ResponseTemplate } };
  resources?: Resource[];
  properties?: Property[];
  primaryOwner?: PrimaryOwner;
  /**
   * Indicates whether this API is allowed to be used in API Products. Only applicable for V4 HTTP Proxy APIs.
   */
  allowedInApiProducts?: boolean;
  _links?: { [key: string]: string };
}

export type ApiState = 'CLOSED' | 'INITIALIZED' | 'STARTED' | 'STOPPED' | 'STOPPING';
export type ApiDeploymentState = 'NEED_REDEPLOY' | 'DEPLOYED';
