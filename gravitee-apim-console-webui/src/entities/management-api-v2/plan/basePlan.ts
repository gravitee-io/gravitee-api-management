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
import { PlanSecurity } from './planSecurity';
import { PlanStatus } from './planStatus';
import { PlanType } from './planType';
import { PlanValidation } from './planValidation';

import { DefinitionVersion } from '../api';

export interface BasePlan {
  /**
   * @description Id of the API owning the plan.
   * @example 6c530064-0b2c-4004-9300-640b2ce0047b
   */
  apiId?: string;
  characteristics?: string[];
  /**
   * Format: date-time
   *
   * @description The datetime when the plan was closed.
   * @example "2023-05-25T12:40:46.184Z"
   */
  closedAt?: string;
  commentMessage?: string;
  commentRequired?: boolean;
  /**
   * Format: date-time
   *
   * @description The last datetime when the plan was created.
   * @example "2023-05-25T12:40:46.184Z"
   */
  createdAt?: string;
  /**
   * @description Plan's crossId. Identifies plan across environments.
   * @example df83b2a4-cc3e-3f80-9f0d-c138c106c076
   */
  crossId?: string;
  definitionVersion?: DefinitionVersion;
  /**
   * @description Plan's description. A short description of your plan.
   * @example I can use a hundred characters to describe this plan.
   */
  description?: string;
  /**
   * @description Groups of users which are not allowed to subscribe to this plan.
   * @example [
   *   "MY_GROUP1",
   *   "MY_GROUP2"
   * ]
   */
  excludedGroups?: string[];
  generalConditions?: string;
  /**
   * @description Plan's uuid.
   * @example 00f8c9e7-78fc-4907-b8c9-e778fc790750
   */
  id?: string;
  /**
   * @description Plan's name. Duplicate names can exists.
   * @example My Api plan
   */
  name?: string;
  order?: number;
  /**
   * Format: date-time
   *
   * @description The last datetime when the plan was published.
   * @example "2023-05-25T12:40:46.184Z"
   */
  publishedAt?: string;
  security?: PlanSecurity;
  selectionRule?: string;
  status?: PlanStatus;
  /**
   * @description The list of sharding tags associated with this plan.
   * @example [
   *   "public",
   *   "private"
   * ]
   */
  tags?: string[];
  type?: PlanType;
  /**
   * Format: date-time
   *
   * @description The last datetime when the plan was updated.
   * @example "2023-05-25T12:40:46.184Z"
   */
  updatedAt?: string;
  validation?: PlanValidation;
}
