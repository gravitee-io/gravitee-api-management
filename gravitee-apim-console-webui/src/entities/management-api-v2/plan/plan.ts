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

export interface Plan {
  /**
   * Id of the API owning the plan.
   */
  apiId?: string;
  characteristics?: string[];
  closedAt?: string;
  commentMessage?: string;
  commentRequired?: boolean;
  /**
   * The last date (as timestamp) when the API was created.
   */
  createdAt?: Date;
  /**
   * API's crossId. Identifies API across environments.
   */
  crossId?: string;
  /**
   * API's description. A short description of your API.
   */
  description?: string;
  /**
   * Groups of users which are not allowed to subscribe to this plan.
   */
  excludedGroups?: string[];
  generalConditions?: string;
  /**
   * Plan's uuid.
   */
  id?: string;
  /**
   * Plan's name. Duplicate names can exists.
   */
  name?: string;
  order?: number;
  /**
   * The last date (as timestamp) when the API was published.
   */
  publishedAt?: Date;
  security?: PlanSecurity;
  selectionRule?: string;
  status?: PlanStatus;
  /**
   * The list of sharding tags associated with this plan.
   */
  tags?: string[];
  type?: PlanType;
  /**
   * The last date (as timestamp) when the API was updated.
   */
  updatedAt?: Date;
  validation?: PlanValidation;
}
