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

export const A2A_PROVIDER = 'A2A';

export interface IntegrationNavigationItem {
  routerLink: string;
  displayName: string;
  permissions?: string[];
  icon?: string;
  routerLinkActiveOptions?: { exact: boolean };
  disabled?: boolean;
  providerType: ('API' | 'A2A')[];
}

export interface IntegrationResponse {
  data: Integration[];
  pagination: Pagination;
}
export function isApiIntegration(integration: unknown): integration is ApiIntegration {
  return (integration as ApiIntegration).provider !== undefined && (integration as ApiIntegration).provider !== A2A_PROVIDER;
}

export function isA2aIntegration(integration: unknown): integration is A2aIntegration {
  return (integration as A2aIntegration).provider === A2A_PROVIDER;
}

export type Integration = ApiIntegration | A2aIntegration;

interface IntegrationBase {
  id: string;
  name: string;
  provider: string;
  description: string;
  primaryOwner?: { id: string; displayName: string; email: string };
  groups: string[];
}

export interface ApiIntegration extends IntegrationBase {
  agentStatus: AgentStatus;
  pendingJob?: AsyncJob;
}

export interface A2aIntegration extends IntegrationBase {
  provider: 'A2A';
  wellKnownUrls: { url: string }[];
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
  wellKnownUrls?: A2aIntegration['wellKnownUrls'];
}

export interface UpdateIntegrationPayload {
  name: string;
  description: string;
  groups: string[];
  wellKnownUrls?: A2aIntegration['wellKnownUrls'];
}

export interface IntegrationProvider {
  icon: string;
  value: string;
  apimDocsName: string;
}

export interface FederatedAPI {
  id: string;
  name: string;
  version: string;
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

export type IntegrationPreviewApi = FederatedAPI & {
  state: IntegrationPreviewApisState;
};

export interface IntegrationPreview {
  totalCount: number;
  newCount: number;
  updateCount: number;
  apis: IntegrationPreviewApi[];
}
