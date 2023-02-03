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
import { PlanSecurityType, PlanStatus, PlanType, PlanValidationType } from '@models/v3/PlanEntity';
import { Flow } from '@models/v3/Flow';
import { Rule } from '@models/v3/Rule';

export interface NewPlanEntity {
  /**
   *
   * @type {string}
   * @memberof NewPlanEntity
   */
  api?: string;
  /**
   *
   * @type {Array<string>}
   * @memberof NewPlanEntity
   */
  characteristics?: Array<string>;
  /**
   *
   * @type {string}
   * @memberof NewPlanEntity
   */
  comment_message?: string;
  /**
   *
   * @type {boolean}
   * @memberof NewPlanEntity
   */
  comment_required?: boolean;
  /**
   *
   * @type {string}
   * @memberof NewPlanEntity
   */
  crossId?: string;
  /**
   *
   * @type {string}
   * @memberof NewPlanEntity
   */
  description: string;
  /**
   *
   * @type {Array<string>}
   * @memberof NewPlanEntity
   */
  excluded_groups?: Array<string>;
  /**
   *
   * @type {Array<Flow>}
   * @memberof NewPlanEntity
   */
  flows: Array<Flow>;
  /**
   *
   * @type {string}
   * @memberof NewPlanEntity
   */
  general_conditions?: string;
  /**
   *
   * @type {string}
   * @memberof NewPlanEntity
   */
  id?: string;
  /**
   *
   * @type {string}
   * @memberof NewPlanEntity
   */
  name: string;
  /**
   *
   * @type {number}
   * @memberof NewPlanEntity
   */
  order?: number;
  /**
   *
   * @type {{ [key: string]: Array<Rule>; }}
   * @memberof NewPlanEntity
   */
  paths: { [key: string]: Array<Rule> };
  /**
   *
   * @type {PlanSecurityType}
   * @memberof NewPlanEntity
   */
  security: PlanSecurityType;
  /**
   *
   * @type {string}
   * @memberof NewPlanEntity
   */
  securityDefinition?: string;
  /**
   *
   * @type {string}
   * @memberof NewPlanEntity
   */
  selection_rule?: string;
  /**
   *
   * @type {PlanStatus}
   * @memberof NewPlanEntity
   */
  status: PlanStatus;
  /**
   *
   * @type {Array<string>}
   * @memberof NewPlanEntity
   */
  tags?: Array<string>;
  /**
   *
   * @type {PlanType}
   * @memberof NewPlanEntity
   */
  type: PlanType;
  /**
   *
   * @type {PlanValidationType}
   * @memberof NewPlanEntity
   */
  validation: PlanValidationType;
}
