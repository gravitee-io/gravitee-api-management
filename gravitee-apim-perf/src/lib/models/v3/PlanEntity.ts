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
import { Rule } from '@lib/models/v3/Rule';

export interface PlanEntity {
  /**
   *
   * @type {string}
   * @memberof PlanEntity
   */
  api?: string;
  /**
   *
   * @type {Array<string>}
   * @memberof PlanEntity
   */
  characteristics?: Array<string>;
  /**
   *
   * @type {Date}
   * @memberof PlanEntity
   */
  closed_at?: Date;
  /**
   *
   * @type {string}
   * @memberof PlanEntity
   */
  comment_message?: string;
  /**
   *
   * @type {boolean}
   * @memberof PlanEntity
   */
  comment_required?: boolean;
  /**
   *
   * @type {Date}
   * @memberof PlanEntity
   */
  created_at?: Date;
  /**
   *
   * @type {string}
   * @memberof PlanEntity
   */
  crossId?: string;
  /**
   *
   * @type {string}
   * @memberof PlanEntity
   */
  description: string;
  /**
   *
   * @type {Array<string>}
   * @memberof PlanEntity
   */
  excluded_groups?: Array<string>;
  /**
   *
   * @type {Array<Flow>}
   * @memberof PlanEntity
   */
  flows: Array<Flow>;
  /**
   *
   * @type {string}
   * @memberof PlanEntity
   */
  general_conditions?: string;
  /**
   *
   * @type {string}
   * @memberof PlanEntity
   */
  id?: string;
  /**
   *
   * @type {string}
   * @memberof PlanEntity
   */
  name: string;
  /**
   *
   * @type {number}
   * @memberof PlanEntity
   */
  order?: number;
  /**
   *
   * @type {{ [key: string]: Array<Rule>; }}
   * @memberof PlanEntity
   */
  paths: { [key: string]: Array<Rule> };
  /**
   *
   * @type {Date}
   * @memberof PlanEntity
   */
  published_at?: Date;
  /**
   *
   * @type {PlanSecurityType}
   * @memberof PlanEntity
   */
  security: PlanSecurityType;
  /**
   *
   * @type {string}
   * @memberof PlanEntity
   */
  securityDefinition?: string;
  /**
   *
   * @type {string}
   * @memberof PlanEntity
   */
  selection_rule?: string;
  /**
   *
   * @type {PlanStatus}
   * @memberof PlanEntity
   */
  status: PlanStatus;
  /**
   *
   * @type {Array<string>}
   * @memberof PlanEntity
   */
  tags?: Array<string>;
  /**
   *
   * @type {PlanType}
   * @memberof PlanEntity
   */
  type: PlanType;
  /**
   *
   * @type {Date}
   * @memberof PlanEntity
   */
  updated_at?: Date;
  /**
   *
   * @type {PlanValidationType}
   * @memberof PlanEntity
   */
  validation: PlanValidationType;
}

export const enum PlanSecurityType {
  KEY_LESS = 'KEY_LESS',
  API_KEY = 'API_KEY',
  OAUTH2 = 'OAUTH2',
  JWT = 'JWT',
  SUBSCRIPTION = 'SUBSCRIPTION',
}

export const enum PlanStatus {
  STAGING = 'STAGING',
  PUBLISHED = 'PUBLISHED',
  CLOSED = 'CLOSED',
  DEPRECATED = 'DEPRECATED',
}

export const enum PlanType {
  API = 'API',
  CATALOG = 'CATALOG',
}

export const enum PlanValidationType {
  AUTO = 'AUTO',
  MANUAL = 'MANUAL',
}
