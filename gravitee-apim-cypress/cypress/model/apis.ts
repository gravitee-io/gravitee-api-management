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
export interface Api {
  id: string;
  contextPath: string;
  description: string;
  endpoint: string;
  name: string;
  version: string;
  state: ApiState;
  visibility: ApiVisibility;
  lifecycle_state: ApiLifecycleState;
  created_at: number;
  updated_at: number;
  owner: string;
  workflow_state: string;
  labels?: string[];
}

export interface ApiDefinition {
  id: string;
  description: string;
  proxy: ApiProxy;
  name: string;
  version: string;
  created_at: number;
  updated_at: number;
  owner: string;
}

export interface ApiProxy {
  context_path: string;
  endpoints: ApiEndpoint[];
}

export interface ApiEndpoint {
  name: string;
  target: string;
  inherit: boolean;
}

export interface PortalApi {
  id: string;
  description: string;
  name: string;
  version: string;
  running: boolean;
  public: boolean;
  draft: boolean;
  created_at: number;
  updated_at: number;
  labels: string[];
  entrypoints: string[];
  categories: string[];
}

export enum ApiErrorCodes {
  API_NOT_FOUND = 'errors.api.notFound',
}

export enum ApiVisibility {
  PUBLIC = 'PUBLIC',
  PRIVATE = 'PRIVATE',
}

export enum ApiLifecycleState {
  CREATED = 'CREATED',
  PUBLISHED = 'PUBLISHED',
}

export enum ApiWorkflowState {
  IN_REVIEW = 'IN_REVIEW',
  REVIEW_OK = 'REVIEW_OK',
}

export enum ApiState {
  STOPPED = 'STOPPED',
  STARTED = 'STARTED',
}

export enum ApiFlowMode {
  DEFAULT = 'DEFAULT',
  BEST_MATCH = 'BEST_MATCH',
}

export enum ApiFlowOperator {
  STARTS_WITH = 'STARTS_WITH',
  EQUALS = 'EQUALS',
}

export enum ApiPageType {
  ASCIIDOC = 'ASCIIDOC',
  ASYNCAPI = 'ASYNCAPI',
  MARKDOWN = 'MARKDOWN',
  MARKDOWN_TEMPLATE = 'MARKDOWN_TEMPLATE',
  SWAGGER = 'SWAGGER',
  FOLDER = 'FOLDER',
  LINK = 'LINK',
  ROOT = 'ROOT',
  SYSTEM_FOLDER = 'SYSTEM_FOLDER',
  TRANSLATION = 'TRANSLATION',
}

export enum ApiMetadataFormat {
  STRING = 'STRING',
  NUMERIC = 'NUMERIC',
  BOOLEAN = 'BOOLEAN',
  DATE = 'DATE',
  MAIL = 'MAIN',
  URL = 'URL',
}

export interface ApiMember {
  id: string;
  reference: string;
  role: string;
}

export interface ApiQualityMetrics {
  score: number;
  metrics_passed: Record<string, boolean>;
}

export interface ApiQualityRule {
  api: string;
  quality_rule: string;
  checked: boolean;
  created_at?: number;
  updated_at?: number;
}

export interface ApiRating {
  rate: number;
}
export interface ApiRatingResponse {
  id: string;
  rate: number;
  api: string;
  user: string;
  userDisplayName: string;
  createdAt: number;
  updateAt: number;
  answers: [];
}

export interface ApiDeployment {
  deploymentLabel: string;
}

export interface Subscription {}

export enum ApiPrimaryOwnerType {
  USER = 'USER',
  GROUP = 'GROUP',
}
