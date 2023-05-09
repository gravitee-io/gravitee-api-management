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

import { Consumer } from './consumer';
import { FlowStage } from './flowStage';
import { StepV2 } from './stepV2';
import { PathOperator } from './pathOperator';

import { HttpMethod } from '../httpMethod';

export interface FlowV2 {
  /**
   * Flow's uuid.
   */
  id?: string;
  /**
   * Flow's name.
   */
  name?: string;
  pathOperator?: PathOperator;
  pre?: StepV2[];
  post?: StepV2[];
  /**
   * Is the flow enabled.
   */
  enabled?: boolean;
  methods?: HttpMethod[];
  /**
   * The condition to evaluate to determine if the flow should be executed.
   */
  condition?: string;
  consumers?: Consumer[];
  stage?: FlowStage;
}
