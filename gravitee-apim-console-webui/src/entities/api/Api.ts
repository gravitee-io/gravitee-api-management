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
import { Json } from '../../util/json';
import { Flow } from '../flow/flow';
import { HttpMethod } from '../HttpMethod';
import { Proxy } from '../proxy';
import { Services } from '../services';

export interface Api {
  id: string;
  name: string;
  version?: string;
  description?: string;

  groups?: string[];
  visibility?: ApiVisibility;
  state?: ApiState;
  tags?: string[];
  picture?: string;
  categories?: string[];
  labels?: string[];
  entrypoints?: ApiEntrypoint[];
  background?: string;
  context_path?: string;
  proxy?: Proxy;
  flow_mode?: ApiFlowMode;
  paths?: Record<string, ApiRule>;
  flows?: Flow[];
  plans?: ApiPlan[];
  gravitee?: string;
  execution_mode?: 'v3' | 'jupiter';

  deployed_at?: number;
  created_at: number;
  updated_at?: number;

  owner?: ApiPrimaryOwner;
  properties?: ApiProperty[];
  services?: Services;
  picture_url?: string;
  resources?: ApiResource[];
  path_mappings?: string[];
  response_templates?: Record<string, unknown>;
  lifecycle_state?: ApiLifecycleState;
  workflow_state?: ApiWorkflowState;
  disable_membership_notifications?: boolean;
  background_url?: string;

  etag?: string;
}

export type ApiVisibility = 'PUBLIC' | 'PRIVATE';
export type ApiState = 'INITIALIZED' | 'STOPPED' | 'STARTED' | 'CLOSED';

export interface ApiEntrypoint {
  tags?: string[];
  target?: string;
  host?: string;
}

export type ApiFlowMode = 'DEFAULT' | 'BEST_MATCH';

export interface ApiRule {
  methods?: HttpMethod[];
  policy?: ApiRulePolicy;
  description?: string;
  enabled?: boolean;
}

export interface ApiRulePolicy {
  name?: string;
  configuration?: string;
}

export interface ApiPlan {
  id: string;
  name?: string;
  security?: string;
  securityDefinition?: string;
  paths?: Record<string, unknown[]>;
  api?: string;
  selectionRule?: string;
  flows?: Flow[];
  tags?: string[];
  status?: string;
}

export interface ApiPrimaryOwner {
  id: string;
  email?: string;
  displayName?: string;
  type?: string;
}

export interface ApiProperty {
  key?: string;
  value?: string;
  dynamic?: boolean;
}

export interface ApiResource {
  name: string;
  type: string;
  configuration: Json;
  enabled?: boolean;
}

export type ApiLifecycleState = 'CREATED' | 'PUBLISHED' | 'UNPUBLISHED' | 'DEPRECATED' | 'ARCHIVED';

export type ApiWorkflowState = 'DRAFT' | 'IN_REVIEW' | 'REQUEST_FOR_CHANGES' | 'REVIEW_OK';
