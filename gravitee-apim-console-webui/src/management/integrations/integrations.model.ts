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

import { Pagination } from '../../entities/management-api-v2';

export interface IntegrationNavigationItem {
  routerLink: string;
  displayName: string;
  permissions?: string[];
  icon?: string;
  routerLinkActiveOptions?: { exact: boolean };
  disabled?: boolean;
}

export interface IntegrationResponse {
  data: Integration[];
  pagination: Pagination;
}

export interface Integration {
  agentStatus: AgentStatus;
  id: string;
  name: string;
  provider: string;
  description: string;
  pendingJob?: AsyncJob;
  primaryOwner?: { id: string; displayName: string; email: string };
  groups: string[];
}

export interface IntegrationIngestionRequest {
  apiIds: string[];
}

export interface IntegrationIngestionResponse {
  status: AsyncJobStatus;
  message?: string;
}

export interface AsyncJob {
  id: string;
  status: AsyncJobStatus;
  startedAt: string;
}

export enum AsyncJobStatus {
  SUCCESS = 'SUCCESS',
  PENDING = 'PENDING',
  ERROR = 'ERROR',
}

export enum AgentStatus {
  CONNECTED = 'CONNECTED',
  DISCONNECTED = 'DISCONNECTED',
}

export interface CreateIntegrationPayload {
  name: string;
  description: string;
  provider: string;
}

export interface UpdateIntegrationPayload {
  name: string;
  description: string;
  groups: string[];
}

export interface IntegrationProvider {
  icon: string;
  value: string;
}

export interface FederatedAPI {
  id: string;
  name: string;
}

export interface FederatedAPIsResponse {
  data: FederatedAPI[];
  pagination: Pagination;
}

export interface DeletedFederatedAPIsResponse {
  deleted: number;
  skipped: number;
  errors: number;
}

export enum IntegrationPreviewApisState {
  NEW = 'NEW',
  UPDATE = 'UPDATE',
}

export interface IntegrationPreviewApis {
  id: string;
  name: string;
  state: IntegrationPreviewApisState;
}

export interface IntegrationPreview {
  totalCount: number;
  newCount: number;
  updateCount: number;
  apis: IntegrationPreviewApis[];
}
