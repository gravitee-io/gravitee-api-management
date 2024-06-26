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
import { BasePlan } from '../basePlan';
import { Rule } from '../../api/v1';
import { FlowV2 } from '../../api/v2';

export interface PlanV2 extends BasePlan {
  definitionVersion: 'V2';
  selectionRule?: string;
  /**
   * @description The list of sharding tags associated with this plan.
   * @example [
   *   "public",
   *   "private"
   * ]
   */
  tags?: string[];
  flows?: FlowV2[];
  paths?: Record<string, Rule[] | undefined>;
}
