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
/*
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

export enum PlanType {
  API = 'API',
  CATALOG = 'CATALOG',
}

export enum MethodType {
  CONNECT = 'CONNECT',
  DELETE = 'DELETE',
  GET = 'GET',
  HEAD = 'HEAD',
  OPTIONS = 'OPTIONS',
  PATCH = 'PATCH',
  POST = 'POST',
  PUT = 'PUT',
  TRACE = 'TRACE',
  OTHER = 'OTHER',
}

export enum OperatorType {
  STARTS_WITH = 'STARTS_WITH',
  EQUALS = 'EQUALS',
}

export interface PathOperator {
  path: string;
  operator: OperatorType;
}

export interface Step {
  name: string;
  policy: string;
  description: string;
  configuration: string;
  enabled: boolean;
}

export enum ConsumerType {
  TAG = 'TAG',
}

export interface Consumer {
  consumerType: ConsumerType;
  consumerId: string;
}

export interface Flow {
  name: string;
  'path-operator': PathOperator;
  pre: Step[];
  post: Step[];
  enabled: boolean;
  methods: MethodType[];
  condition: string;
  consumers: Consumer[];
}

export interface NewPlanEntity {
  id: string;
  name: string;
  description: string;
  validation: PlanValidation;
  security: PlanSecurityType;
  securityDefinition: string;
  type: PlanType;
  status: PlanStatus;
  api: string;
  characteristics: string[];
  tags: string[];
  order: number;
  paths: object;
  flows: Flow[];
  excluded_groups: string[];
  comment_required: boolean;
  comment_message: string;
  selection_rule: string;
  general_conditions: string;
}

export interface Plan {
  id?: string;
  name: string;
  description: string;
  securityDefinition: PlanSecurityType;
  validation?: PlanValidation;
  created_at?: number;
  updated_at?: number;
  status?: PlanStatus;
}

export enum PlanValidation {
  AUTO = 'AUTO',
  MANUAL = 'MANUAL',
}

export enum PlanSecurityType {
  KEY_LESS = 'KEY_LESS',
  API_KEY = 'API_KEY',
  OAUTH2 = 'OAUTH2',
  JWT = 'JWT',
}

export enum PlanStatus {
  STAGING = 'STAGING',
  PUBLISHED = 'PUBLISHED',
  CLOSED = 'CLOSED',
  DEPRECATED = 'DEPRECATED',
}
