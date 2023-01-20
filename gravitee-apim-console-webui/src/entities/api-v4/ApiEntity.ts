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
import { ApiType } from './ApiType';
import { EndpointGroup } from './EndpointGroup';
import { Flow } from './Flow';
import { FlowMode } from './FlowMode';
import { Listener } from './Listener';

export interface ApiEntity {
  id: string;
  name: string;
  apiVersion: string;
  type: ApiType;
  createdAt: string;
  updatedAt: string;
  definitionVersion: '4.0.0';

  deployedAt?: string;
  crossId?: string;
  description?: string;
  tags?: string[];
  listeners?: Listener[];
  endpointGroups?: EndpointGroup[];
  properties?: unknown[]; // Property[] To Complete when needed
  resources?: unknown[]; // Resource[] To Complete when needed
  plans?: unknown[]; // PlanEntity[] To Complete when needed
  flowMode?: FlowMode;
  flows?: Flow[];
  responseTemplates?: Record<string, Record<string, unknown>>; // ResponseTemplate To Complete when needed
  services?: unknown; // To Complete when needed
  groups?: string[];
  visibility?: ApiVisibility;
  state?: LifecycleState;
  primaryOwner?: unknown; // PrimaryOwnerEntity To Complete when needed
  picture?: string;
  pictureUrl?: string;
  background?: string;
  backgroundUrl?: string;
  categories?: string[];
  labels?: string[];
  definitionContext?: ApiDefinitionContext;
  metadata?: Record<string, unknown>;
  lifecycleState?: ApiLifecycleState;
  workflowState?: unknown; // WorkflowState To Complete when needed
  disableMembershipNotifications?: boolean;
}

export type ApiVisibility = 'PUBLIC' | 'PRIVATE';

export type LifecycleState = 'INITIALIZED' | 'STOPPED' | 'STOPPING' | 'STARTED' | 'CLOSED';

export type ApiOrigin = 'management' | 'kubernetes';
export interface ApiDefinitionContext {
  origin: ApiOrigin;
}

export type ApiLifecycleState = 'CREATED' | 'PUBLISHED' | 'UNPUBLISHED' | 'DEPRECATED' | 'ARCHIVED';
