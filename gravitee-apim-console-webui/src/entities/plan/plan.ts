import { Flow } from '../flow/flow';

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
export enum PlanSecurityType {
  KEY_LESS = 'KEY_LESS',
  API_KEY = 'API_KEY',
  OAUTH2 = 'OAUTH2',
  JWT = 'JWT',
}

export enum PlanValidation {
  AUTO = 'AUTO',
  MANUAL = 'MANUAL',
}

export enum PlanType {
  API = 'API',
  CATALOG = 'CATALOG',
}

export const PLAN_STATUS = ['STAGING', 'PUBLISHED', 'DEPRECATED', 'CLOSED'] as const;
export type PlanStatus = typeof PLAN_STATUS[number];

export interface NewPlan {
  name: string;
  description?: string;
  validation?: PlanValidation;
  security: PlanSecurityType;
  securityDefinition?: string;
  type?: PlanType;
  status?: PlanStatus;
  api?: string;
  characteristics?: string[];
  tags?: string[];
  order?: number;
  paths?: { [key: string]: Array<unknown> };
  flows?: Flow[];
  excluded_groups?: string[];
  comment_required?: boolean;
  comment_message?: string;
  selection_rule?: string;
  general_conditions?: string;
}

export interface Plan {
  id: string;
  name: string;
  description?: string;
  security: PlanSecurityType;
  securityDefinition?: string;
  api?: string;
  characteristics?: Array<string>;
  closed_at?: Date;
  comment_message?: string;
  comment_required?: boolean;
  created_at?: Date;
  crossId?: string;
  excluded_groups?: Array<string>;
  flows?: Array<Flow>;
  general_conditions?: string;
  order?: number;
  paths?: { [key: string]: Array<unknown> };
  published_at?: Date;
  selection_rule?: string;
  status?: PlanStatus;
  tags?: Array<string>;
  type?: PlanType;
  updated_at?: Date;
  validation?: PlanValidation;
}
