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
import { PlanValidation } from './planValidation';

import { DefinitionVersion } from '../api';

export interface CreateBasePlan {
  /**
   * @description Plan's crossId. Identifies plan across environments.
   * @example df83b2a4-cc3e-3f80-9f0d-c138c106c076
   */
  crossId?: string;
  characteristics?: string[];
  commentMessage?: string;
  commentRequired?: boolean;
  definitionVersion?: DefinitionVersion;
  /**
   * @description Plan's description. A short description of your Plan.
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
  /**
   * @description This field contains the UUID of the documentation page that is used as General Conditions.
   * @example 4e3de652-2301-48ba-bde6-522301e8ba3a
   */
  generalConditions?: string;
  /**
   * @description Plan's name. Duplicate names can exists.
   * @example My Api plan
   */
  name?: string;
  /**
   * @description Simple order that could be used by a front end to display plans in a certain order. To highlight a plan on the portal for instance.
   * @example 0
   */
  order?: number;
  security?: PlanSecurity;
  /** @description An optional EL expression that will be evaluated at request time to select this plan. */
  selectionRule?: string;
  /**
   * @description The list of sharding tags associated with this plan.
   * @example [
   *   "public",
   *   "private"
   * ]
   */
  tags?: string[];
  validation?: PlanValidation;
}
