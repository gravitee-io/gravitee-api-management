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
import { FlowV4 } from '@models/v4/FlowV4';

export interface PlanEntityV4 {
  /**
   *
   * @type {string}
   * @memberof PlanEntityV4
   */
  apiId?: string;
  /**
   *
   * @type {Array<string>}
   * @memberof PlanEntityV4
   */
  characteristics?: Array<string>;
  /**
   *
   * @type {Date}
   * @memberof PlanEntityV4
   */
  closedAt?: Date;
  /**
   *
   * @type {string}
   * @memberof PlanEntityV4
   */
  commentMessage?: string;
  /**
   *
   * @type {boolean}
   * @memberof PlanEntityV4
   */
  commentRequired?: boolean;
  /**
   *
   * @type {Date}
   * @memberof PlanEntityV4
   */
  createdAt?: Date;
  /**
   *
   * @type {string}
   * @memberof PlanEntityV4
   */
  crossId?: string;
  /**
   *
   * @type {string}
   * @memberof PlanEntityV4
   */
  description?: string;
  /**
   *
   * @type {Array<string>}
   * @memberof PlanEntityV4
   */
  excludedGroups?: Array<string>;
  /**
   *
   * @type {Array<FlowV4>}
   * @memberof PlanEntityV4
   */
  flows?: Array<FlowV4>;
  /**
   *
   * @type {string}
   * @memberof PlanEntityV4
   */
  generalConditions?: string;
  /**
   *
   * @type {string}
   * @memberof PlanEntityV4
   */
  id?: string;
  /**
   *
   * @type {string}
   * @memberof PlanEntityV4
   */
  name?: string;
  /**
   *
   * @type {number}
   * @memberof PlanEntityV4
   */
  order?: number;
  /**
   *
   * @type {PlanSecurityV4}
   * @memberof PlanEntityV4
   */
  planSecurity?: PlanSecurityV4;
  /**
   *
   * @type {string}
   * @memberof PlanEntityV4
   */
  planStatus?: PlanEntityV4PlanStatusEnum;
  /**
   *
   * @type {PlanValidationTypeV4}
   * @memberof PlanEntityV4
   */
  planValidation?: PlanValidationTypeV4;
  /**
   *
   * @type {Date}
   * @memberof PlanEntityV4
   */
  publishedAt?: Date;
  /**
   *
   * @type {PlanSecurityV4}
   * @memberof PlanEntityV4
   */
  security?: PlanSecurityV4;
  /**
   *
   * @type {string}
   * @memberof PlanEntityV4
   */
  selectionRule?: string;
  /**
   *
   * @type {string}
   * @memberof PlanEntityV4
   */
  status?: PlanEntityV4StatusEnum;
  /**
   *
   * @type {Array<string>}
   * @memberof PlanEntityV4
   */
  tags?: Array<string>;
  /**
   *
   * @type {PlanTypeV4}
   * @memberof PlanEntityV4
   */
  type?: PlanTypeV4;
  /**
   *
   * @type {Date}
   * @memberof PlanEntityV4
   */
  updatedAt?: Date;
  /**
   *
   * @type {PlanValidationTypeV4}
   * @memberof PlanEntityV4
   */
  validation?: PlanValidationTypeV4;
}

export interface PlanSecurityV4 {
  /**
   *
   * @type {any}
   * @memberof PlanSecurityV4
   */
  configuration?: any;
  /**
   *
   * @type {string}
   * @memberof PlanSecurityV4
   */
  type: string;
}

export const enum PlanEntityV4PlanStatusEnum {
  STAGING = 'STAGING',
  PUBLISHED = 'PUBLISHED',
  DEPRECATED = 'DEPRECATED',
  CLOSED = 'CLOSED',
}

export const enum PlanValidationTypeV4 {
  AUTO = 'AUTO',
  MANUAL = 'MANUAL',
}

export const enum PlanEntityV4StatusEnum {
  STAGING = 'STAGING',
  PUBLISHED = 'PUBLISHED',
  DEPRECATED = 'DEPRECATED',
  CLOSED = 'CLOSED',
}

export const enum PlanTypeV4 {
  API = 'API',
  CATALOG = 'CATALOG',
}
