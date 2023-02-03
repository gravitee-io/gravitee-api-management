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
import { PlanSecurityV4, PlanTypeV4, PlanValidationTypeV4 } from '@models/v4/PlanEntityV4';

export interface NewPlanEntityV4 {
  /**
   *
   * @type {string}
   * @memberof NewPlanEntityV4
   */
  apiId?: string;
  /**
   *
   * @type {Array<string>}
   * @memberof NewPlanEntityV4
   */
  characteristics?: Array<string>;
  /**
   *
   * @type {string}
   * @memberof NewPlanEntityV4
   */
  commentMessage?: string;
  /**
   *
   * @type {boolean}
   * @memberof NewPlanEntityV4
   */
  commentRequired?: boolean;
  /**
   *
   * @type {string}
   * @memberof NewPlanEntityV4
   */
  crossId?: string;
  /**
   *
   * @type {string}
   * @memberof NewPlanEntityV4
   */
  description: string;
  /**
   *
   * @type {Array<string>}
   * @memberof NewPlanEntityV4
   */
  excludedGroups?: Array<string>;
  /**
   *
   * @type {Array<FlowV4>}
   * @memberof NewPlanEntityV4
   */
  flows: Array<FlowV4>;
  /**
   *
   * @type {string}
   * @memberof NewPlanEntityV4
   */
  generalConditions?: string;
  /**
   *
   * @type {string}
   * @memberof NewPlanEntityV4
   */
  id?: string;
  /**
   *
   * @type {string}
   * @memberof NewPlanEntityV4
   */
  name: string;
  /**
   *
   * @type {number}
   * @memberof NewPlanEntityV4
   */
  order?: number;
  /**
   *
   * @type {PlanSecurityV4}
   * @memberof NewPlanEntityV4
   */
  security?: PlanSecurityV4;
  /**
   *
   * @type {string}
   * @memberof NewPlanEntityV4
   */
  selectionRule?: string;
  /**
   *
   * @type {string}
   * @memberof NewPlanEntityV4
   */
  status: NewPlanEntityV4StatusEnum;
  /**
   *
   * @type {Array<string>}
   * @memberof NewPlanEntityV4
   */
  tags?: Array<string>;
  /**
   *
   * @type {PlanTypeV4}
   * @memberof NewPlanEntityV4
   */
  type: PlanTypeV4;
  /**
   *
   * @type {PlanValidationTypeV4}
   * @memberof NewPlanEntityV4
   */
  validation: PlanValidationTypeV4;
}

export const enum NewPlanEntityV4StatusEnum {
  STAGING = 'STAGING',
  PUBLISHED = 'PUBLISHED',
  DEPRECATED = 'DEPRECATED',
  CLOSED = 'CLOSED',
}

export const enum PlanSecurityTypeV4 {
  KEY_LESS = 'key-less',
  API_KEY = 'api-key',
  OAUTH2 = 'oauth2',
  JWT = 'jwt',
  SUBSCRIPTION = 'subscription',
}
