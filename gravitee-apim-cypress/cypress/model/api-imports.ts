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
import { ApiVisibility, ApiPageType, ApiMetadataFormat, ApiPrimaryOwnerType } from '@model/apis';
import { PlanSecurityType, PlanStatus, PlanType, PlanValidation } from '@model/plan';
import { FlowMode, OperatorType } from '@model/api-flows';

export interface ApiImportMember {
  source: string;
  sourceId: string;
  role?: string;
  roles: string[];
}

export interface ApiImportFlow {
  name: string;
  path_operator: {
    path: string;
    operator: OperatorType;
  };
  condition: string;
  consumers: any[];
  methods: any[];
  pre: any[];
  post: any[];
  enabled: true;
}

export interface ApiImportPlan {
  id?: string;
  name: string;
  description: string;
  validation: PlanValidation;
  security: PlanSecurityType;
  type: PlanType;
  status: PlanStatus;
  order: number;
  characteristics: any[];
  created_at?: number;
  updated_at?: number;
  paths: any;
  flows: ApiImportFlow[];
  comment_required: boolean;
}

export interface ApiImportVirtualHost {
  path: string;
}

export interface ApiImportProxyEndpoint {
  backup: boolean;
  inherit: boolean;
  name: string;
  weight: number;
  type: string;
  target: string;
}

export interface ApiImportProxyHttpEndpoint {
  connectTimeout: number;
  idleTimeout: number;
  keepAlive: boolean;
  readTimeout: number;
  pipelining: boolean;
  maxConcurrentConnections: number;
  useCompression: boolean;
  followRedirects: boolean;
}

export enum ApiImportProxyGroupLoadBalancerType {
  ROUND_ROBIN = 'ROUND_ROBIN',
}

export interface ApiImportProxyGroup {
  name: string;
  endpoints: ApiImportProxyEndpoint[];
  load_balancing: {
    type: ApiImportProxyGroupLoadBalancerType;
  };
  http?: ApiImportProxyHttpEndpoint;
}

export interface ApiImportProxy {
  virtual_hosts: ApiImportVirtualHost[];
  strip_context_path: boolean;
  preserve_host: boolean;
  groups?: ApiImportProxyGroup[];
}

export interface ApiImportPrimaryOwner {
  id: string;
  type: ApiPrimaryOwnerType;
  displayName?: string;
  email?: string;
}

export interface ApiImport {
  id?: string;
  environment_id?: string;
  name: string;
  version: string;
  description: string;
  visibility: ApiVisibility;
  gravitee: string;
  flow_mode: FlowMode;
  resources: any[];
  properties: any[];
  groups: string[];
  members: ApiImportMember[];
  pages: ApiImportPage[];
  plans: ApiImportPlan[];
  metadata: ApiImportMetadata[];
  path_mappings: string[];
  proxy: ApiImportProxy;
  response_templates: any;
  owner?: ApiImportPrimaryOwner;
  primaryOwner?: ApiImportPrimaryOwner;
}

export interface ApiImportPage {
  id?: string;
  name: string;
  type: ApiPageType;
  content?: string;
  order: number;
  published: boolean;
  visibility: ApiVisibility;
  lastModificationDate?: number;
  lastContributor?: string;
  contentType: string;
  homepage: boolean;
  parentPath?: string;
  parentId?: string;
  excludedAccessControls: boolean;
  accessControls: any[];
  api?: string;
}

export interface ApiImportMetadata {
  key?: string;
  name: string;
  format: ApiMetadataFormat;
  value: string;
  defaultValue?: string;
  apiId?: string;
}
